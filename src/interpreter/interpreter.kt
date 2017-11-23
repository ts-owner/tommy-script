package interpreter

import core.*
import standard_library.stdLib
import java.lang.Math.pow

fun PreOp.evalOn(arg : Expr, environment : Scope, functionDefs : MutableMap<String, Func>) : Value = when(this) {
        PreOp.plus -> eval(arg, environment, functionDefs).let { v ->
            v as? VInt ?: throw IncorrectTypeException(wrongVal = v)
        }
        PreOp.negate -> eval(arg, environment, functionDefs).let { v ->
            if(v is VInt) VInt(-v.value) else throw IncorrectTypeException(wrongVal = v)
        }
        PreOp.not -> eval(arg, environment, functionDefs).let { v ->
            if(v is VBool) VBool(!v.value) else throw IncorrectTypeException(wrongVal = v)
        }
    }

fun InOp.evalOn(lhs : Expr, rhs : Expr, environment : Scope, functionDefs : MutableMap<String, Func>) = when (this) {
    InOp.plus -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(left + right)
    }
    InOp.subtract -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(left - right)
    }
    InOp.times -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(left * right)
    }
    InOp.div -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(left / right)
    }
    InOp.mod -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(left % right)
    }
    InOp.power -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(pow(left.toDouble(), right.toDouble()).toInt())
    }
    InOp.eqInt -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left == right)
    }
    InOp.neq -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left == right)
    }
    InOp.lt -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left < right)
    }
    InOp.gt -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left > right)
    }
    InOp.leq -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left <= right)
    }
    InOp.geq -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left >= right)
    }
    InOp.and -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VBool)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        if(left) {
            val right = eval(rhs, environment, functionDefs).let { v -> (v as? VBool)?.value ?: throw IncorrectTypeException(wrongVal = v) }
            VBool(right)
        }
        else VBool(false)
    }
    InOp.or -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VBool)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        if(left) VBool(true)
        else {
            val right = eval(rhs, environment, functionDefs).let { v -> (v as? VBool)?.value ?: throw IncorrectTypeException(wrongVal = v) }
            VBool(right)
        }
    }
    InOp.concat -> {
        val left = eval(lhs, environment, functionDefs).let { v -> (v as? VString)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment, functionDefs).let { v -> (v as? VString)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VString(left + right)
    }
}

fun eval(expr : Expr, environment : Scope, functionDefs : MutableMap<String, Func>) : Value = when (expr) {
    is LInt -> VInt(expr.value)
    is LString -> VString(expr.value)
    is LBool -> VBool(expr.value)
    is LArray -> VArray(expr.value.mapTo(mutableListOf()) { eval(it, environment, functionDefs) })
    is LUnit -> VUnit
    is Prefix -> expr.op.evalOn(expr.arg, environment, functionDefs)
    is Infix -> expr.op.evalOn(expr.lhs, expr.rhs, environment, functionDefs)
    is Var -> environment[expr.id] ?: throw UndefinedVariableException(wrongExpr = expr, wrongId = expr.id)
    is FunCall -> {
        val func = functionDefs[expr.id.id]
                ?: throw UndefinedVariableException(wrongExpr = expr, wrongId = expr.id.id)
        if(func.args.size != expr.args.size) throw IncorrectArgumentCountException(wrongCall = expr, called = func)
        val evaledArgs = expr.args.map { argExpr -> eval(argExpr, environment, functionDefs) }
        val ret = when(func) {
            is TommyFunc -> {
                val (id, argAnnIds, _, body, closure) = func
                val argIds = argAnnIds.map { (argId, _) -> argId }
                val argBindings = mutableMapOf<String, Value>().apply { putAll(argIds.zip(evaledArgs)) }
                val localCtx = Scope(local = argBindings, parent = closure)
                try {
                    body.forEach {
                        exec(it, localCtx, functionDefs)
                    }
                    VUnit
                } catch(box : ReturnBox) {
                    box.ret
                }
            }
            is BuiltIn -> func(evaledArgs)
        }
        ret
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
            functionDefs[id.id] = TommyFunc(id.id, args, retTy, body, funcScope)
        }
        is Return -> {
            val evaled = eval(stmt.toReturn, environment, functionDefs)
            throw ReturnBox(ret = evaled)
        }
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

private data class ReturnBox(val ret : Value) : Throwable()