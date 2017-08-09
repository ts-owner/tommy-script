package interpreter

sealed class Type

object TInt : Type() { override fun toString() = "Int" }
object TString : Type() { override fun toString() = "String" }
object TBool : Type() { override fun toString() = "Bool" }
object TUnit : Type() { override fun toString() = "Unit" }
data class TFunction(val dom : List<Type>, val cod : Type) : Type() {
     override fun toString() = "(${dom.joinToString()}) → $cod"
}

fun opType(n : Int, ty : Type) = TFunction(List(n, { ty }), ty)
fun relationOn(n : Int, ty : Type) = TFunction(List(n, { ty }), TBool)

enum class PreOp(val asText : String, val type : TFunction) {
     plus("+", opType(1, TInt)), negate("-", opType(1, TInt)),
     not("not", opType(1, TBool));

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
data class FunCall(val id : String, val args : List<Expr>) : Expr()

// Literals
sealed class Literal(val ty : Type) : Expr()

data class LInt(val value: Int) : Literal(TInt)
data class LString(val value: String) : Literal(TString)
data class LBool(val value: Boolean) : Literal(TBool)
object LUnit : Literal(TUnit) { override fun toString() = "unit" }

sealed class Statement : AST()
//typealias Body = List<Statement> //TODO @Brendan should't this be List<AST> so you can have funcalls in body
typealias Body = List<AST> //potentially temporary change to make parser work


// Statements
data class If(val cond : Expr, val thenBranch : Body, val elseBranch : Body? = null) : Statement()
data class VarDef(val lhs : AnnotatedVar, val rhs : Expr) : Statement() //TODO annotations should be optional
data class UntypedVarDef(val lhs : Var, val rhs : Expr) : Statement() //TODO @Brendan this is ugly maybe there's a cleaner way
data class FunDef(val id : String, val args : List<AnnotatedVar>, val returnType : Type,
                  val statements : Body) : Statement()
data class Return(val toReturn : Expr) : Statement()