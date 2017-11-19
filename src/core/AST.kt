package core

sealed class Type

object TInt : Type() {
    override fun toString() = "Int"
}

object TString : Type() {
    override fun toString() = "String"
}

object TBool : Type() {
    override fun toString() = "Bool"
}

object TUnit : Type() {
    override fun toString() = "Unit"
}

object TArray : Type() {
    override fun toString() : String {
        return "[TODO]"
    }
}

data class TFunction(val dom : List<Type>, val cod : Type) : Type() {
    override fun toString() = "(${dom.joinToString()}) → $cod"
}

object TAny : Type() {
    override fun toString() = "Any"
} // Hella unsafe!!!!!!!!!

fun opType(n : Int, ty : Type) = TFunction(List(n, { ty }), ty)
fun relationOn(n : Int, ty : Type) = TFunction(List(n, { ty }), TBool)

enum class PreOp(val asText : String, val type : TFunction, val precedence : Int) {
    plus("+", opType(1, TInt), 12), negate("-", opType(1, TInt), 12),
    not("not", opType(1, TBool), 6);

    override fun toString() = asText
}

enum class InOp(val asText : String, val type : TFunction, val precedence : Int) {
    plus("+", opType(2, TInt), 10), subtract("-", opType(2, TInt), 10),
    times("*", opType(2, TInt), 11), div("/", opType(2, TInt), 11), mod("%", opType(2, TInt), 11),
    power("**", opType(2, TInt), 13), concat("++", opType(2, TString), 13),
    and("and", opType(2, TBool), 5), or("or", opType(2, TBool), 4),
    eqInt("==", relationOn(2, TInt), 9), lt("<", relationOn(2, TInt), 9),
    gt(">", relationOn(2, TInt), 9), leq("<=", relationOn(2, TInt), 9),
    geq(">=", relationOn(2, TInt), 9), neq("!=", relationOn(2, TInt), 9);
    //not sure about precedence level of ++, should be pretty low, work out examples

    override fun toString() = asText
}

data class AnnotatedVar(val id : String, val ty : Type)

sealed class Expression
typealias Expr = Expression

data class Var(val id : String) : Expr()

// Recursive expression constructors
data class Prefix(val op : PreOp, val arg : Expr) : Expr()

data class Infix(val op : InOp, val lhs : Expr, val rhs : Expr) : Expr()
data class FunCall(val id : Var, val args : List<Expr>) : Expr()
data class ArrayAccess(val name : Var, val index : Expr) : Expr()

// Literals
sealed class Literal(val ty : Type) : Expr()

data class LInt(val value : Int) : Literal(TInt)
data class LString(val value : String) : Literal(TString)
data class LBool(val value : Boolean) : Literal(TBool)
data class LArray(val value : List<Expr>) : Literal(TArray)
object LUnit : Literal(TUnit)

sealed class Statement
typealias Body = List<Statement>
typealias Stmt = Statement

// Statements
data class EvalExpr(val expr : Expr) : Stmt()

sealed class If : Stmt()
data class IfStep(val cond : Expr, val body : Body, val next : If? = null) : If()
data class Else(val body : Body) : If()

data class VarDef(val lhs : AnnotatedVar, val rhs : Expr) : Stmt()
data class UntypedVarDef(val lhs : Var, val rhs : Expr) : Stmt() //TODO @Brendan this is ugly maybe there's a cleaner way
data class VarReassign(val lhs : Var, val rhs : Expr) : Stmt()
data class ArrayAssignment(val lhs : Var, val index : Expr, val rhs : Expr) : Stmt()
data class FunDef(val id : Var, val args : List<AnnotatedVar>, val returnType : Type, val statements : Body) : Stmt()
data class Return(val toReturn : Expr) : Stmt()
data class While(val cond : Expr, val body : Body) : Stmt()
data class For(val elemIdent : Var, val list : Expr, val body : Body) : Stmt()