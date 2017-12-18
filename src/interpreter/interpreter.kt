package interpreter

import core.*
import standard_library.stdLib
import java.lang.Math.pow

fun PreOp.evalOn(arg : Expr, environment : Scope) : Value = when(this) {
        PreOp.plus -> eval(arg, environment).let { v ->
            v as? VInt ?: throw IncorrectTypeException(wrongVal = v)
        }
        PreOp.negate -> eval(arg, environment).let { v ->
            if(v is VInt) VInt(-v.value) else throw IncorrectTypeException(wrongVal = v)
        }
        PreOp.not -> eval(arg, environment).let { v ->
            if(v is VBool) VBool(!v.value) else throw IncorrectTypeException(wrongVal = v)
        }
    }

// We pass in the args unevaluated so that `and` and `or` can be lazy.
fun InOp.evalOn(lhs : Expr, rhs : Expr, environment : Scope) = when (this) {
    InOp.plus -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(left + right)
    }
    InOp.subtract -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(left - right)
    }
    InOp.times -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(left * right)
    }
    InOp.div -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(left / right)
    }
    InOp.mod -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(left % right)
    }
    InOp.power -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VInt(pow(left.toDouble(), right.toDouble()).toInt())
    }
    InOp.eqInt -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left == right)
    }
    InOp.neq -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left == right)
    }
    InOp.lt -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left < right)
    }
    InOp.gt -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left > right)
    }
    InOp.leq -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left <= right)
    }
    InOp.geq -> {
        val left = eval(lhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VInt)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VBool(left >= right)
    }
    InOp.and -> {
        val left = eval(lhs, environment).let { v -> (v as? VBool)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        if(left) {
            val right = eval(rhs, environment).let { v -> (v as? VBool)?.value ?: throw IncorrectTypeException(wrongVal = v) }
            VBool(right)
        }
        else VBool(false)
    }
    InOp.or -> {
        val left = eval(lhs, environment).let { v -> (v as? VBool)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        if(left) VBool(true)
        else {
            val right = eval(rhs, environment).let { v -> (v as? VBool)?.value ?: throw IncorrectTypeException(wrongVal = v) }
            VBool(right)
        }
    }
    InOp.concat -> {
        val left = eval(lhs, environment).let { v -> (v as? VString)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        val right = eval(rhs, environment).let { v -> (v as? VString)?.value ?: throw IncorrectTypeException(wrongVal = v) }
        VString(left + right)
    }
}

fun eval(expr : Expr, environment : Scope) : Value = when (expr) {
    is LInt -> VInt(expr.value)
    is LString -> VString(expr.value)
    is LBool -> VBool(expr.value)
    is LArray -> VArray(expr.value.mapTo(mutableListOf()) { eval(it, environment) })
    is LUnit -> VUnit
    is Prefix -> expr.op.evalOn(expr.arg, environment)
    is Infix -> expr.op.evalOn(expr.lhs, expr.rhs, environment)
    is Var -> environment[expr.id] ?: throw UndefinedVariableException(wrongExpr = expr, wrongId = expr.id)
    is FunCall -> {
        val func = (environment[expr.id.id] as? VFunction)?.value
                ?: throw UndefinedVariableException(wrongExpr = expr, wrongId = expr.id.id)
        if(func.args.size != expr.args.size) throw IncorrectArgumentCountException(wrongCall = expr, called = func)
        val evaledArgs = expr.args.map { argExpr -> eval(argExpr, environment) }
        when(func) {
            is TommyFunc -> {
                val (_, argAnnIds, _, body, closure) = func
                val argBindings = mutableMapOf<String, Value>()
                for(i in argAnnIds.indices) {
                    val (argName, _) = argAnnIds[i]
                    argBindings[argName] = evaledArgs[i]
                }
                val localCtx = Scope(local = argBindings, parent = closure)
                try {
                    body.forEach { exec(it, localCtx) }
                    VUnit
                } catch(box : ReturnBox) { box.ret }
            }
            is BuiltIn -> func(evaledArgs)
        }
    }
    is ArrayAccess -> {
        val (name, indexExpr) = expr
        val lhs = environment[name.id] ?: throw UndefinedVariableException(wrongExpr = expr, wrongId = name.id)
        val arr = (lhs as? VArray ?: throw IncorrectTypeException(wrongVal = lhs)).value
        val index = eval(indexExpr, environment).let { evaled ->
            evaled as? VInt ?: throw IncorrectTypeException(wrongVal = evaled)
        }.value
        arr[index]
    }
}

fun exec(stmt : Statement, environment : Scope) {
    when(stmt) {
        is EvalExpr -> eval(stmt.expr, environment)
        is IfStep -> {
            val (condExpr, body, next) = stmt
            val cond = eval(condExpr, environment).let { evaled ->
                evaled as? VBool ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            val bodyScope = Scope(parent = environment)
            if(cond) body.forEach { exec(it, bodyScope) }
            else next?.let { exec(it, environment) }
        }
        is Else -> {
            val bodyScope = Scope(parent = environment)
            stmt.body.forEach { exec(it, bodyScope) }
        }
        is VarDef -> {
            val id = stmt.lhs.id
            if(id in environment.local) throw RedefineVariableException(wrongId = id, wrongStmt = stmt)
            val rhs = eval(stmt.rhs, environment)
            environment[id] = rhs
        }
        is UntypedVarDef -> {
            val id = stmt.lhs.id
            if(id in environment.local) throw RedefineVariableException(wrongId = id, wrongStmt = stmt)
            val rhs = eval(stmt.rhs, environment)
            environment[id] = rhs
        }
        is VarReassign -> {
            val id = stmt.lhs.id
            if(id !in environment) throw UndefinedVariableException(wrongId = id, wrongExpr = Var(id))
            val rhs = eval(stmt.rhs, environment)
            environment[id] = rhs
        }
        is ArrayAssignment -> {
            val (lhs, indexExpr, rhsExpr) = stmt
            if(lhs.id !in environment) throw UndefinedVariableException(wrongId = lhs.id, wrongExpr = lhs)
            val arr = environment[lhs.id]!!.let { evaled ->
                evaled as? VArray ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            val index = eval(indexExpr, environment).let { evaled ->
                evaled as? VInt ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            val rhs = eval(rhsExpr, environment)
            arr[index] = rhs
        }
        is FunDef -> {
            val (id, args, retTy, body) = stmt
            if(id.id in environment) throw RedefineVariableException(wrongId = id.id, wrongStmt = stmt)
            val funcScope = Scope(parent = environment)
            environment[id.id] = VFunction(TommyFunc(id.id, args, retTy, body, funcScope))
        }
        is Return -> {
            val evaled = eval(stmt.toReturn, environment)
            throw ReturnBox(ret = evaled)
        }
        is While -> {
            val (condExpr, body) = stmt
            fun currCond() = eval(condExpr, environment).let { evaled ->
                evaled as? VBool ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            while(currCond()) {
                val currScope = Scope(parent = environment)
                body.forEach { exec(it, currScope) }
            }
        }
        is For -> {
            val (id, listExpr, body) = stmt
            if(id.id in environment) throw RedefineVariableException(wrongId = id.id, wrongStmt = stmt)
            val list = eval(listExpr, environment).let { evaled ->
                evaled as? VArray ?: throw IncorrectTypeException(wrongVal = evaled)
            }.value
            for(x in list) {
                val currScope = Scope(local = mutableMapOf(id.id to x), parent = environment)
                body.forEach { exec(it, currScope) }
            }
        }
    }
}

fun interp(stmt : Stmt, environment : MutableMap<String, Value>) {
    try {
        val baseScope = Scope(local = environment)
        exec(stmt, baseScope)
    } catch(e : InterpreterException) {
        throw TopLevelException(cause = e, top = stmt)
    }
}
fun interp(stmt : Stmt, environment : Scope) {
    try {
        exec(stmt, environment)
    } catch(e : InterpreterException) {
        throw TopLevelException(cause = e, top = stmt)
    }
}

fun interpretProgram(prog : List<Stmt>) {
    val builtins = mutableMapOf<String, Value>("print" to VFunction(Print),
                                               "len" to VFunction(Len),
                                               "str" to VFunction(Str),
                                               "push" to VFunction(Push))
    val progScope = Scope(builtins)
    stdLib.forEach { exec(it, progScope) }
    prog.forEach { interp(it, progScope) }
}

private data class ReturnBox(val ret : Value) : Throwable()