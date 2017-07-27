package interpreter

fun typeAgrees(context : Map<String, Type>, expr : Expr, expected : Type) : Boolean {
    fun rec(currExpr : Expr, currExpected : Type) : Boolean {
        when (currExpr) {
            is Var -> return context[currExpr.id] == currExpected
            is Literal -> return currExpected == currExpr.ty
            is Prefix -> {
                val (op, ex) = currExpr
                val (dom, cod) = op.type
                return rec(ex, dom.first()) && cod == currExpected
            }
            is Infix -> {
                val (op, lhs, rhs) = currExpr
                val (dom, cod) = op.type
                val (lTy, rTy) = dom
                return rec(lhs, lTy) && rec(rhs, rTy) && cod == currExpected
            }
            is FunCall -> {
                val (id, args) = currExpr
                val (dom, cod) = context[id] as? TFunction ?: return false
                val argTypes = args.map { context[it] ?: return false }
                return dom == argTypes && cod == context[id]
            }
        }
    }
    return rec(expr, expected)
}