package core

import com.github.h0tk3y.betterParse.utils.Tuple2

sealed class Type

object TInt : Type() { override fun toString() = "Int" }
object TString : Type() { override fun toString() = "String" }
object TBool : Type() { override fun toString() = "Bool" }
object TUnit : Type() { override fun toString() = "Unit" }
object TArray : Type() {
    override fun toString(): String {
        return "[TODO]"
    }
}
data class TFunction(val dom : List<Type>, val cod : Type) : Type() {
     override fun toString() = "(${dom.joinToString()}) → $cod"
}

fun opType(n : Int, ty : Type) = TFunction(List(n, { ty }), ty)
fun relationOn(n : Int, ty : Type) = TFunction(List(n, { ty }), TBool)

enum class PreOp(val asText : String, val type : TFunction, val precedence: Int) {
     plus("+", opType(1, TInt),13), negate("-", opType(1, TInt), 13),
     not("not", opType(1, TBool),6);

     override fun toString() = asText
}

enum class InOp(val asText : String, val type : TFunction, val precedence: Int) {
     plus("+", opType(2, TInt),10), negate("-", opType(2, TInt),10), mod("%", opType(2, TInt),12), times("*", opType(2, TInt),11),
     div("/", opType(2, TInt),11), power("**", opType(2, TInt),14), concat("++", opType(2, TString),14),
     and("and", opType(2, TBool),5), or("or", opType(2, TBool),4), eqInt("==", relationOn(2, TInt),9),
     lt("<", relationOn(2, TInt),9), gt(">", relationOn(2, TInt),9), leq("<=", relationOn(2, TInt),9),
     geq(">=", relationOn(2, TInt),9), neq("!=", relationOn(2, TInt),9);
     //not sure about precedence level of ++, should be pretty low, work out examples
     //TODO @brendan MOD operator

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

data class ArrayAccess(val name : Var, val index : Expr) : Expr()

// Literals
sealed class Literal(val ty : Type) : Expr()

data class LInt(val value: Int) : Literal(TInt)
data class LString(val value: String) : Literal(TString)
data class LBool(val value: Boolean) : Literal(TBool)
data class LArray(val value: MutableList<Literal>): Literal(TArray)
object LUnit : Literal(TUnit) { override fun toString() = "unit" }

sealed class Statement : AST()
typealias Body = List<AST> //potentially temporary change to make parser work


// Statements
data class If(val cond : Expr, val thenBranch : Body, val elseBranch : Body? = null, val elifs : List<Tuple2<Expr, Body>>? = null) : Statement()
data class VarDef(val lhs : AnnotatedVar, val rhs : Expr) : Statement()
data class UntypedVarDef(val lhs : Var, val rhs : Expr) : Statement() //TODO @Brendan this is ugly maybe there's a cleaner way
data class VarReassign(val lhs : Var, val rhs: Expr) : Statement()
data class ArrayAssignment(val lhs : Var, val index: Expr, val rhs: Expr) : Statement()
data class FunDef(val id : String, val args : List<AnnotatedVar>, val returnType : Type,
                  val statements : Body) : Statement()
data class Return(val toReturn : Expr) : Statement()
data class While(val cond : Expr, val body : Body): Statement()