package interpreter

import core.*

sealed class InterpreterException(msg : String = "", cause : Exception? = null)
    : RuntimeException(msg, cause)

class TopLevelException(msg : String = "", cause : Exception? = null, val top : AST)
    : InterpreterException(msg, cause) {
    override fun toString() = super.toString() + "\nOccurring in $top"
}

class InvalidPassException(msg : String = "", cause : Exception? = null, val wrongAST : AST, val pass : String)
    : InterpreterException(msg, cause) {
    override fun toString() = super.toString() + "\n$wrongAST didn't undergo $pass"
}

class UndefinedVariableException(msg : String = "", cause : Exception? = null, val wrongAST : AST, val wrongId : String)
    : InterpreterException(msg, cause) {
    override fun toString() = super.toString() + "\n$wrongId not defined in $wrongAST"
}

class IncorrectTypeException(msg : String = "", cause : Exception? = null, val wrongVal : Value)
    : InterpreterException(msg, cause) {
    override fun toString() = super.toString() + "\nError occurred in $wrongVal"
}

class IncorrectArgumentCountException(msg : String = "", cause : Exception? = null,
                                      val called : Func, val wrongCall : FunCall)
    : InterpreterException(msg, cause) {
    override fun toString() : String {
        val actual = wrongCall.args.size
        val expected = called.args.size
        return super.toString() + "\n$wrongCall gives $actual number of args, but ${called.id} expects $expected"
    }
}

class RedefineVariableException(msg : String = "", cause : Exception? = null, val wrongStmt : Statement, val wrongId : String)
    : InterpreterException(msg, cause) {
    override fun toString() = super.toString() + "\n$wrongStmt attempts to redefine $wrongId"
}