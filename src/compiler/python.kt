package compiler

import core.*
import core.parser.escapeChars

fun PreOp.toPython() = when(this) {
    PreOp.plus -> ""
    PreOp.negate -> "-"
    PreOp.not -> "not"
}

fun InOp.toPython() = when(this) {
    InOp.plus -> "+"
    InOp.subtract -> "-"
    InOp.times -> "*"
    InOp.div -> "/"
    InOp.power -> "**"
    InOp.concat -> "+"
    InOp.and -> "and"
    InOp.or -> "or"
    InOp.eqInt -> "=="
    InOp.lt -> "<"
    InOp.gt -> ">"
    InOp.leq -> "<="
    InOp.geq -> ">="
    InOp.neq -> "!="
}

fun String.escaped() : String {
    val builder = StringBuilder()
    for(c in this) {
        val unescaped = escapeChars.entries.find { it.value == c }?.key
        when(unescaped) {
            null -> builder.append(c)
            else -> builder.append("\\$unescaped")
        }
    }
    return builder.toString()
}

private fun compile(expr : Expr) : String = when(expr) {
    is Var -> expr.id // TODO: Name scrambling?
    is Prefix -> "(${expr.op.toPython()} ${compile(expr.arg)})"
    is Infix -> "(${compile(expr.lhs)} ${expr.op.toPython()} ${compile(expr.rhs)})"
    is FunCall -> "${expr.id}(${expr.args.joinToString("", transform = ::compile)})"
    is LInt -> "${expr.value}"
    is LString -> "\"${expr.value.escaped()}\""
    is LBool -> if(expr.value) "True" else "False"
}

private val indentStr = "    "

private fun compile(ast : AST, indent : String) : String = when(ast) {
    is Expr -> compile(ast)
    is Statement -> {
        val stmt : Statement = ast
        val newIndent = "$indent$indentStr"
        val end = "\n${newIndent}pass"
        fun List<AST>.joinBody() = joinToString("\n") { compile(it, newIndent) }
        when(stmt) {
            is If -> {
                val (cond, thenBranch, elseBranch) = stmt
                val thenStr = thenBranch.joinBody()
                val elseStr = elseBranch?.joinBody().orEmpty()
                "${indent}if ${compile(cond)}:\n$thenStr$end\nelse:$elseStr$end"
            }
            is VarDef -> "$indent${stmt.lhs.id} = ${compile(stmt.rhs)}"
            is UntypedVarDef -> "$indent${stmt.lhs.id} = ${compile(stmt.rhs)}"
            is VarReassign -> "$indent${stmt.lhs.id} = ${compile(stmt.rhs)}"
            is FunDef -> {
                val (id, args, _, statements) = stmt
                val argStr = args.joinToString(transform = AnnotatedVar::id)
                val bodyStr = statements.joinBody()
                "${indent}def $id($argStr):\n$bodyStr$end"
            }
            is Return -> "$indent${compile(stmt.toReturn)}"
        }
    }
}

fun compile(ast : AST) = compile(ast, "")

fun main(args: Array<String>) {
    val example : Statement = If(cond = Prefix(PreOp.not, Infix(InOp.eqInt, LInt(6), LInt(9))),
                                thenBranch = listOf(UntypedVarDef(Var("x"), LInt(42)),
                                                    VarReassign(Var("x"),
                                                                Infix(InOp.times, Var("x"), LInt(10))),
                                                    Return(LString("a\nb"))))
    println(compile(example))
}