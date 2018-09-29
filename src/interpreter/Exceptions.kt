package interpreter

import core.*

sealed class InterpreterException(msg : String = "", cause : Exception? = null)
    : RuntimeException(msg, cause)

class TopLevelException(msg : String = "", cause : Exception? = null, val top : Stmt)
    : InterpreterException(msg, cause) {
    override fun toString() = super.toString() + "\nOccurring in $top"
}

class InvalidPassException(msg : String = "", cause : Exception? = null, val wrongStmt : Stmt, val pass : String)
    : InterpreterException(msg, cause) {
    override fun toString() = super.toString() + "\n$wrongStmt didn't undergo $pass"
}

class UndefinedVariableException(msg : String = "", cause : Exception? = null, val wrongExpr : Expr
                                 , val wrongId : String, val scope: Scope)
    : InterpreterException(msg, cause) {
    override fun toString() = super.toString() + "\n$wrongId not defined in $wrongExpr with scope $scope"
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
        return super.toString() + "\n$wrongCall gives $actual number of args, but $called expects $expected"
    }
}

class RedefineVariableException(msg : String = "", cause : Exception? = null
                                , val wrongStmt : Statement, val wrongId : String
                                , val scope: Scope) : InterpreterException(msg, cause) {
    override fun toString() = super.toString() + "\n$wrongStmt attempts to redefine $wrongId in scope $scope"
}