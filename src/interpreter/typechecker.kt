package interpreter

class TypeCheckingException(msg : String = "", cause : Exception? = null, val wrongExpr : Expr)
    : RuntimeException(msg, cause) {
    override fun toString() = super.toString() + "\nError occurred in $wrongExpr"
}

fun typeInfer(context : Map<String, Type>, expr : Expr) : Type {
    // We wrap rec so that we can report an error as occurring on expr rather than some subterm
    fun rec(currExpr : Expr) : Type {
        fun err(msg : String) : Nothing = throw TypeCheckingException(wrongExpr = currExpr, msg = msg)
        when (currExpr) {
            is Var -> return context[currExpr.id] ?: err("${currExpr.id} is untyped")
            is Literal -> return currExpr.ty
            is Prefix -> {
                val (op, ex) = currExpr
                val (dom, cod) = op.type
                val argExpected = dom.first() // This is ok since prefix ops only have 1 arg
                val argActual = rec(ex)
                if(argExpected != argActual) err("Expected type was $argExpected but actual was $argActual")
                else return cod
            }
            is Infix -> {
                val (op, lhs, rhs) = currExpr
                val (dom, cod) = op.type
                val (lExpected, rExpected) = dom
                val lActual = rec(lhs)
                val rActual = rec(rhs)
                if(lExpected != lActual)
                    err("Expected type for the left arg was $lExpected but actual was $lActual")
                else if(rExpected != rActual)
                    err("Expected type for the right arg was $rExpected but actual was $rActual")
                else return cod
            }
            is FunCall -> {
                val (id, args) = currExpr
                val (dom, cod) = context[id] as? TFunction ?: err("Function $id not typed at callsite")

                if(args.size < dom.size) err("Not enough arguments provided to $id")
                else if(args.size > dom.size) err("Too many arguments provided to $id")
                args.zip(dom) { argEx, argExpected ->
                    val argActual = rec(argEx)
                    if(argActual != argExpected)
                        err("Expected type for parameter was $argExpected but actual was $argActual")
                }

                return cod
            }
        }
    }
    try {
        return rec(expr)
    } catch (e : TypeCheckingException) {
        throw TypeCheckingException("Type checking failed", e, expr)
    }
}