package interpreter

import core.*

fun prettyPrint(arg : Value) : String = when(arg) {
    is VInt -> arg.value.toString()
    is VString -> arg.value
    is VBool -> arg.value.toString()
    is VArray -> arg.value.joinToString(prefix = "[", postfix = "]", transform = ::prettyPrint)
    is VUnit -> "unit"
    is VFunction -> "<function>"
}

// Function objects
sealed class Func(val args : List<AnnotatedVar>, val returnType : Type) {
    override fun equals(other : Any?) =
            other is Func && args == other.args && returnType == returnType

    override fun hashCode() = 31 * args.hashCode() + returnType.hashCode()
    operator fun component1() = args
    operator fun component2() = returnType
}

// Anonymous functions
class LambdaFunc(args : List<Var>, val body : Expr, val closure : Scope)
    : Func(args as List<AnnotatedVar>, TAny) { // TODO: Fix this
    override fun equals(other : Any?) = other is LambdaFunc && super.equals(other) &&
            body == other.body && closure == other.closure

    override fun toString() = "Func(args=$args, body=$body)"

    override fun hashCode() = 31 * super.hashCode() + body.hashCode()
    operator fun component3() = body
    operator fun component4() = closure
    fun copy(args : List<Var> = this.args as List<Var>, body : Expr = this.body,
             environment : Scope = this.closure) =
            LambdaFunc(args, body, environment)
}

// User defined functions
class TommyFunc(val id : String, args : List<AnnotatedVar>, returnType : Type, val statements : Body,
                val closure : Scope)
    : Func(args, returnType) {
    override fun equals(other : Any?) = other is TommyFunc && super.equals(other) &&
            id == other.id && statements == other.statements

    override fun toString() = "Func(id=$id, args=$args, returnType=$returnType, " +
            "statements=$statements)"

    override fun hashCode() = 31 * super.hashCode() + statements.hashCode()
    operator fun component3() = id
    operator fun component4() = statements
    operator fun component5() = closure
    fun copy(id : String = this.id, args : List<AnnotatedVar> = this.args,
             returnType : Type = this.returnType, statements : Body = this.statements,
             environment : Scope = this.closure) =
            TommyFunc(id, args, returnType, statements, environment)
}

sealed class BuiltIn(val id : String, args : List<AnnotatedVar>, returnType : Type)
    : Func(args, returnType) {
    abstract operator fun invoke(argsActual : List<Value>) : Value
}

// Primitive functions
object Print : BuiltIn(id = "print", args = listOf(AnnotatedVar("message", TAny)), returnType = TUnit) {
    override fun invoke(argsActual : List<Value>) : Value {
        print(prettyPrint(argsActual[0]))
        return VUnit
    }
}

object Str : BuiltIn(id = "str", args = listOf(AnnotatedVar("obj", TAny)), returnType = TString) {
    override fun invoke(argsActual : List<Value>) = VString(prettyPrint(argsActual[0]))
}

object Len : BuiltIn(id = "len", args = listOf(AnnotatedVar("list", TArray)), returnType = TInt) {
    override fun invoke(argsActual : List<Value>) : Value {
        val arg = argsActual[0] as? VArray ?: throw IncorrectTypeException(wrongVal = argsActual[0])
        return VInt(arg.value.size)
    }
}

object Push : BuiltIn(id = "push", args = listOf(AnnotatedVar("lhs", TArray), AnnotatedVar("rhs", TAny)),
        returnType = TUnit) {
    override fun invoke(argsActual : List<Value>) : Value {
        val arr = argsActual[0] as? VArray ?: throw IncorrectTypeException(wrongVal = argsActual[0])
        arr.value.add(argsActual[1])
        return VUnit
    }
}