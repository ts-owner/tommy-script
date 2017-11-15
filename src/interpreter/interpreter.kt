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
    val iLeft by lazy { (lhs as? VInt ?: throw IncorrectTypeException(wrongVal = lhs)).value }
    val iRight by lazy { (rhs as? VInt ?: throw IncorrectTypeException(wrongVal = rhs)).value }
    val bLeft by lazy { (lhs as? VBool ?: throw IncorrectTypeException(wrongVal = lhs)).value }
    val bRight by lazy { (rhs as? VBool ?: throw IncorrectTypeException(wrongVal = rhs)).value }
    val sLeft by lazy { (lhs as? VString ?: throw IncorrectTypeException(wrongVal = lhs)).value }
    val sRight by lazy { (rhs as? VString ?: throw IncorrectTypeException(wrongVal = rhs)).value }
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
    val newBody : MutableList<AST> =
            mutableListOf(UntypedVarDef(retVar, LUnit), UntypedVarDef(retFlag, LBool(false)))
    fun rewriteReturn(ast : AST) : AST {
        fun wrap(curr : AST) = IfStep(Prefix(PreOp.not, retFlag), listOf(curr), null)
        fun rewriteIf(ifS : If) : If = when(ifS) {
            is IfStep -> IfStep(ifS.cond, ifS.body.map(::rewriteReturn), ifS.next?.let { rewriteIf(it) })
            is Else -> Else(ifS.body.map(::rewriteReturn))
        }
        return when (ast) {
            is Return -> {
                IfStep(Prefix(PreOp.not, retFlag),
                        listOf(VarReassign(retVar, ast.toReturn), VarReassign(retFlag, LBool(true))), null)
            }
            is IfStep -> wrap(rewriteIf(ast))
            is Else -> wrap(Else(ast.body.map(::rewriteReturn)))
            is While -> wrap(While(ast.cond, ast.body.map(::rewriteReturn)))
            is For -> wrap(For(ast.elemIdent, ast.list, ast.body.map(::rewriteReturn)))
            is Expr, is UntypedVarDef, is VarDef, is VarReassign, is ArrayAssignment, is FunDef -> wrap(ast)
        }
    }
    newBody.addAll(body.map(::rewriteReturn))
    return function.copy(statements = newBody)
}

fun eval(expr : Expr, environment : MutableMap<String, Value>,
             functionDefs : MutableMap<String, Func>) : Value = when (expr) {
    is LInt -> VInt(expr.value)
    is LString -> VString(expr.value)
    is LBool -> VBool(expr.value)
    is LArray -> VArray(expr.value.mapTo(mutableListOf()) { eval(it, environment, functionDefs) })
    is LUnit -> VUnit
    is Prefix -> expr.op.eval(eval(expr.arg, environment, functionDefs))
    is Infix -> expr.op.eval(eval(expr.lhs, environment, functionDefs), eval(expr.rhs, environment, functionDefs))
    is Var -> environment[expr.id] ?: throw UndefinedVariableException(wrongAST = expr, wrongId = expr.id)
    is FunCall -> {
        val func = functionDefs[expr.id.id]
                ?: throw UndefinedVariableException(wrongAST = expr, wrongId = expr.id.id)
        if(func.args.size != expr.args.size) throw IncorrectArgumentCountException(wrongCall = expr, called = func)
        val evaledArgs = expr.args.map { argExpr -> eval(argExpr, environment, functionDefs) }
        when(func) {
            is TommyFunc -> {
                val (id, argAnnIds, _, body, functionEnv) = func
                val argIds = argAnnIds.map { (argId, _) -> argId }
                val localCtx = functionEnv.toMutableMap()
                localCtx.putAll(argIds.zip(evaledArgs))
                body.forEach { interp(it, localCtx, functionDefs) }
                localCtx["$id+"] ?: throw InvalidPassException(wrongAST = expr, pass = "return desugaring")
            }
            is BuiltIn -> func(evaledArgs)
        }
    }
    is ArrayAccess -> {
        val (name, indexExpr) = expr
        val lhs = environment[name.id] ?: throw UndefinedVariableException(wrongAST = expr, wrongId = name.id)
        val arr = (lhs as? VArray ?: throw IncorrectTypeException(wrongVal = lhs)).value
        val index = eval(indexExpr, environment, functionDefs).let { evaled ->
            evaled as? VInt ?: throw IncorrectTypeException(wrongVal = evaled)
        }.value
        arr[index]
    }
}

fun exec(stmt : Statement, environment : MutableMap<String, Value>, functionDefs : MutableMap<String, Func>) {
    when(stmt) {
        is IfStep -> {
            val (condExpr, body, next) = stmt
            val cond = eval(condExpr, environment, functionDefs).let { evaled ->
                evaled as? VBool ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            if(cond) body.forEach { interp(it, environment, functionDefs) }
            else next?.let { exec(it, environment, functionDefs) }
        }
        is Else -> stmt.body.forEach { interp(it, environment, functionDefs) }
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
            if(id !in environment) throw UndefinedVariableException(wrongId = id, wrongAST = stmt)
            val rhs = eval(stmt.rhs, environment, functionDefs)
            environment[id] = rhs
        }
        is ArrayAssignment -> {
            val (lhs, indexExpr, rhsExpr) = stmt
            if(lhs.id !in environment) throw UndefinedVariableException(wrongId = lhs.id, wrongAST = stmt)
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
            functionDefs[id.id] = retPass(TommyFunc(id.id, args, retTy, body, environment))
        }
        is Return -> throw InvalidPassException(wrongAST = stmt, pass = "return desugaring")
        is While -> {
            val (condExpr, body) = stmt
            fun currCond() = eval(condExpr, environment, functionDefs).let { evaled ->
                evaled as? VBool ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            while(currCond()) {
                body.forEach { interp(it, environment, functionDefs) }
            }
        }
        is For -> {
            val (id, listExpr, body) = stmt
            if(id.id in environment) throw RedefineVariableException(wrongId = id.id, wrongStmt = stmt)
            val list = eval(listExpr, environment, functionDefs).let { evaled ->
                evaled as? VArray ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            for(x in list) {
                environment[id.id] = x
                body.forEach { interp(it, environment, functionDefs) }
                environment.remove(id.id)
            }
        }
    }
}

fun interp(ast : AST, environment : MutableMap<String, Value>,
           functionDefs : MutableMap<String, Func>) {
    try {
        when(ast) {
            is Expr -> eval(ast, environment, functionDefs)
            is Statement -> exec(ast, environment, functionDefs)
        }
    } catch(e : InterpreterException) {
        throw TopLevelException(cause = e, top = ast)
    }
}

fun interpretProgram(prog : List<AST>) {
    val environment = mutableMapOf<String, Value>()
    val functionDefs = mutableMapOf<String, Func>("print" to Print, "len" to Len, "str" to Str, "push" to Push)
    stdLib.forEach { exec(it, environment, functionDefs) }
    prog.forEach { interp(it, environment, functionDefs) }
}