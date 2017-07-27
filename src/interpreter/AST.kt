package interpreter

sealed class Type

object TInt : Type()
object TString : Type()
object TBool : Type()
object TUnit : Type()
data class TFunction(val dom : List<Type>, val cod : Type) : Type()

fun opType(n : Int, ty : Type) = TFunction(List(n, { ty }), ty)
fun relationOn(n : Int, ty : Type) = TFunction(List(n, { ty }), TBool)

enum class PreOp(val asText : String, val type : TFunction) {
     plus("+", relationOn(1, TInt)), negate("-", relationOn(1, TInt)),
     not("not", relationOn(1, TBool));

     override fun toString() = asText
}
enum class InOp(val asText : String, val type : TFunction) {
     plus("+", opType(2, TInt)), negate("-", opType(2, TInt)), times("*", opType(2, TInt)),
     div("/", opType(2, TInt)), power("**", opType(2, TInt)), concat("++", opType(2, TString)),
     and("and", opType(2, TBool)), or("or", opType(2, TBool)), eqInt("==", relationOn(2, TInt)),
     lt("<", relationOn(2, TInt)), gt(">", relationOn(2, TInt)), leq("<=", relationOn(2, TInt)),
     geq(">=", relationOn(2, TInt));

     override fun toString() = asText
}

data class AnnotatedVar(val id : String, val ty : Type)

sealed class AST

sealed class Expression : AST()
typealias Expr = Expression

data class Var(val id : String) : Expr()

// Recursive expression constructors
data class Prefix(val op : PreOp, val expr : Expr) : Expr()
data class Infix(val op : InOp, val lhs : Expr, val rhs : Expr) : Expr()
data class FunCall(val id : String, val args : List<String>) : Expr()

// Literals
sealed class Literal(val ty : Type) : Expr()

data class LInt(val value: Int) : Literal(TInt)
data class LString(val value: String) : Literal(TString)
data class LBool(val value: Boolean) : Literal(TBool)
object LUnit : Literal(TUnit)

sealed class Statement : AST()
typealias Body = List<Statement>

// Statements
data class If(val cond : Expr, val thenBranch : Body, val elseBranch : Body? = null) : Statement()
data class VarDef(val lhs : AnnotatedVar, val rhs : Expr) : Statement()
data class FunDef(val id : String, val args : List<AnnotatedVar>, val returnType : Type,
                  val statements : Body) : Statement()
data class Return(val toReturn : Expr) : Statement()