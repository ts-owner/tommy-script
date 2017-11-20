package interpreter

import core.*
import standard_library.stdLib
import java.lang.Math.pow

fun PreOp.eval(arg : Value) = when(this) {
    PreOp.plus -> arg as? VInt ?: throw IncorrectTypeException(wrongVal = arg)
    PreOp.negate -> if(arg is VInt) VInt(-arg.value) else throw IncorrectTypeException(wrongVal = arg)
    PreOp.not -> if(arg is VBool) VBool(!arg.value) else throw IncorrectTypeException(wrongVal = arg)
}

fun InOp.eval(lhs : Value, rhs : Value) : Value {
    val iLeft by lazy(LazyThreadSafetyMode.NONE) { (lhs as? VInt ?: throw IncorrectTypeException(wrongVal = lhs)).value }
    val iRight by lazy(LazyThreadSafetyMode.NONE) { (rhs as? VInt ?: throw IncorrectTypeException(wrongVal = rhs)).value }
    val bLeft by lazy(LazyThreadSafetyMode.NONE) { (lhs as? VBool ?: throw IncorrectTypeException(wrongVal = lhs)).value }
    val bRight by lazy(LazyThreadSafetyMode.NONE) { (rhs as? VBool ?: throw IncorrectTypeException(wrongVal = rhs)).value }
    val sLeft by lazy(LazyThreadSafetyMode.NONE) { (lhs as? VString ?: throw IncorrectTypeException(wrongVal = lhs)).value }
    val sRight by lazy(LazyThreadSafetyMode.NONE) { (rhs as? VString ?: throw IncorrectTypeException(wrongVal = rhs)).value }
    return when (this) {
        InOp.plus -> VInt(iLeft + iRight)
        InOp.subtract -> VInt(iLeft - iRight)
        InOp.times -> VInt(iLeft * iRight)
        InOp.div -> VInt(iLeft / iRight)
        InOp.mod -> VInt(iLeft % iRight)
        InOp.power -> VInt(pow(iLeft.toDouble(), iRight.toDouble()).toInt())
        InOp.eqInt -> VBool(iLeft == iRight)
        InOp.neq -> VBool(iLeft != iRight)
        InOp.lt -> VBool(iLeft < iRight)
        InOp.gt -> VBool(iLeft > iRight)
        InOp.leq -> VBool(iLeft <= iRight)
        InOp.geq -> VBool(iLeft >= iRight)
        InOp.and -> VBool(bLeft && bRight)
        InOp.or -> VBool(bLeft || bRight)
        InOp.concat -> VString(sLeft + sRight)
    }
}

fun retPass(function : TommyFunc) : TommyFunc {
    val (id, _, _, body, _) = function
    val retVar = Var("$id+")
    val retFlag = Var("$id&")
    val newBody : MutableList<Stmt> =
            mutableListOf(UntypedVarDef(retVar, LUnit), UntypedVarDef(retFlag, LBool(false)))
    fun rewriteReturn(stmt : Stmt) : Stmt {
        fun wrap(curr : Stmt) = IfStep(Prefix(PreOp.not, retFlag), listOf(curr), null)
        fun rewriteIf(ifS : If) : If = when(ifS) {
            is IfStep -> IfStep(ifS.cond, ifS.body.map(::rewriteReturn), ifS.next?.let { rewriteIf(it) })
            is Else -> Else(ifS.body.map(::rewriteReturn))
        }
        return when (stmt) {
            is Return -> {
                IfStep(Prefix(PreOp.not, retFlag),
                        listOf(VarReassign(retVar, stmt.toReturn), VarReassign(retFlag, LBool(true))), null)
            }
            is IfStep -> wrap(rewriteIf(stmt))
            is Else -> wrap(Else(stmt.body.map(::rewriteReturn)))
            is While -> wrap(While(stmt.cond, stmt.body.map(::rewriteReturn)))
            is For -> wrap(For(stmt.elemIdent, stmt.list, stmt.body.map(::rewriteReturn)))
            is EvalExpr, is UntypedVarDef, is VarDef, is VarReassign, is ArrayAssignment, is FunDef -> wrap(stmt)
        }
    }
    newBody.addAll(body.map(::rewriteReturn))
    return function.copy(statements = newBody)
}

fun eval(expr : Expr, environment : Scope, functionDefs : MutableMap<String, Func>) : Value = when (expr) {
    is LInt -> VInt(expr.value)
    is LString -> VString(expr.value)
    is LBool -> VBool(expr.value)
    is LArray -> VArray(expr.value.mapTo(mutableListOf()) { eval(it, environment, functionDefs) })
    is LUnit -> VUnit
    is Prefix -> expr.op.eval(eval(expr.arg, environment, functionDefs))
    is Infix -> expr.op.eval(eval(expr.lhs, environment, functionDefs), eval(expr.rhs, environment, functionDefs))
    is Var -> environment[expr.id] ?: throw UndefinedVariableException(wrongExpr = expr, wrongId = expr.id).apply {
        println("scope is $environment when getting the variable ${expr.id}")
    }
    is FunCall -> {
        val func = functionDefs[expr.id.id]
                ?: throw UndefinedVariableException(wrongExpr = expr, wrongId = expr.id.id)
        if(func.args.size != expr.args.size) throw IncorrectArgumentCountException(wrongCall = expr, called = func)
        val evaledArgs = expr.args.map { argExpr -> eval(argExpr, environment, functionDefs) }
        when(func) {
            is TommyFunc -> {
                val (id, argAnnIds, _, body, closure) = func
                val argIds = argAnnIds.map { (argId, _) -> argId }
                val argBindings = mutableMapOf<String, Value>().apply { putAll(argIds.zip(evaledArgs)) }
                val localCtx = Scope(local = argBindings, parent = closure)
                body.forEach { exec(it, localCtx, functionDefs) }
                localCtx["$id+"] ?: throw InvalidPassException(wrongStmt = EvalExpr(expr), pass = "return desugaring")
            }
            is BuiltIn -> func(evaledArgs)
        }
    }
    is ArrayAccess -> {
        val (name, indexExpr) = expr
        val lhs = environment[name.id] ?: throw UndefinedVariableException(wrongExpr = expr, wrongId = name.id)
        val arr = (lhs as? VArray ?: throw IncorrectTypeException(wrongVal = lhs)).value
        val index = eval(indexExpr, environment, functionDefs).let { evaled ->
            evaled as? VInt ?: throw IncorrectTypeException(wrongVal = evaled)
        }.value
        arr[index]
    }
}

fun exec(stmt : Statement, environment : Scope, functionDefs : MutableMap<String, Func>) {
    when(stmt) {
        is EvalExpr -> eval(stmt.expr, environment, functionDefs)
        is IfStep -> {
            val (condExpr, body, next) = stmt
            val cond = eval(condExpr, environment, functionDefs).let { evaled ->
                evaled as? VBool ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            val bodyScope = Scope(parent = environment)
            if(cond) body.forEach { exec(it, bodyScope, functionDefs) }
            else next?.let { exec(it, environment, functionDefs) }
        }
        is Else -> {
            val bodyScope = Scope(parent = environment)
            stmt.body.forEach { exec(it, bodyScope, functionDefs) }
        }
        is VarDef -> {
            val id = stmt.lhs.id
            if(id in environment) throw RedefineVariableException(wrongId = id, wrongStmt = stmt)
            val rhs = eval(stmt.rhs, environment, functionDefs)
            environment[id] = rhs
        }
        is UntypedVarDef -> {
            val id = stmt.lhs.id
            if(id in environment) throw RedefineVariableException(wrongId = id, wrongStmt = stmt)
            val rhs = eval(stmt.rhs, environment, functionDefs)
            environment[id] = rhs
        }
        is VarReassign -> {
            val id = stmt.lhs.id
            if(id !in environment) throw UndefinedVariableException(wrongId = id, wrongExpr = Var(id))
            val rhs = eval(stmt.rhs, environment, functionDefs)
            environment[id] = rhs
        }
        is ArrayAssignment -> {
            val (lhs, indexExpr, rhsExpr) = stmt
            if(lhs.id !in environment) throw UndefinedVariableException(wrongId = lhs.id, wrongExpr = lhs)
            val arr = environment[lhs.id]!!.let { evaled ->
                evaled as? VArray ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            val index = eval(indexExpr, environment, functionDefs).let { evaled ->
                evaled as? VInt ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            val rhs = eval(rhsExpr, environment, functionDefs)
            arr[index] = rhs
        }
        is FunDef -> {
            val (id, args, retTy, body) = stmt
            if(id.id in functionDefs) throw RedefineVariableException(wrongId = id.id, wrongStmt = stmt)
            val funcScope = Scope(parent = environment)
            functionDefs[id.id] = retPass(TommyFunc(id.id, args, retTy, body, funcScope))
        }
        is Return -> throw InvalidPassException(wrongStmt = stmt, pass = "return desugaring")
        is While -> {
            val (condExpr, body) = stmt
            fun currCond() = eval(condExpr, environment, functionDefs).let { evaled ->
                evaled as? VBool ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            while(currCond()) {
                val currScope = Scope(parent = environment)
                body.forEach { exec(it, currScope, functionDefs) }
            }
        }
        is For -> {
            val (id, listExpr, body) = stmt
            if(id.id in environment) throw RedefineVariableException(wrongId = id.id, wrongStmt = stmt)
            val list = eval(listExpr, environment, functionDefs).let { evaled ->
                evaled as? VArray ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            for(x in list) {
                val currScope = Scope(local = mutableMapOf(id.id to x), parent = environment)
                body.forEach { exec(it, currScope, functionDefs) }
            }
        }
    }
}

fun interp(stmt : Stmt, environment : MutableMap<String, Value>, functionDefs : MutableMap<String, Func>) {
    try {
        val baseScope = Scope(local = environment)
        exec(stmt, baseScope, functionDefs)
    } catch(e : InterpreterException) {
        throw TopLevelException(cause = e, top = stmt)
    }
}
fun interp(stmt : Stmt, environment : Scope, functionDefs : MutableMap<String, Func>) {
    try {
        exec(stmt, environment, functionDefs)
    } catch(e : InterpreterException) {
        throw TopLevelException(cause = e, top = stmt)
    }
}

fun interpretProgram(prog : List<Stmt>) {
    val progScope = Scope()
    val functionDefs = mutableMapOf<String, Func>("print" to Print, "len" to Len, "str" to Str, "push" to Push)
    stdLib.forEach { exec(it, progScope, functionDefs) }
    prog.forEach { interp(it, progScope, functionDefs) }
}