package compiler

import core.*
import core.parser.escapeChars
import standard_library.stdLib
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

fun PreOp.toPython() = when(this) {
    PreOp.plus -> ""
    PreOp.negate -> "-"
    PreOp.not -> "not"
}

fun InOp.toPython() = when(this) {
    InOp.plus -> "+"
    InOp.subtract -> "-"
    InOp.times -> "*"
    InOp.div -> "//"
    InOp.mod -> "%"
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

fun handleSpecialFunctions(func : FunCall) = when(func.id.id) {
    "print" -> "print(${func.args.joinToString(", ", transform = ::compile)}, end='', flush=True)"
    "concat" -> "${compile(func.args[0])} + ${compile(func.args[1])}"
    "push" -> "${compile(func.args[0])}.append(${compile(func.args[1])})"
    else -> null
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
    is FunCall -> handleSpecialFunctions(expr) ?:
                "${expr.id.id}(${expr.args.joinToString(", ", transform = ::compile)})"
    is LInt -> "${expr.value}"
    is LString -> "\"${expr.value.escaped()}\""
    is LBool -> if(expr.value) "True" else "False"
    is LArray -> expr.value.joinToString(prefix = "[", postfix = "]", transform = ::compile)
    is LUnit -> "None"
    is ArrayAccess -> "${expr.name.id}[${compile(expr.index)}]"
}

private val indentStr = "    "

private fun compile(ast : AST, indent : String) : String = when(ast) {
    is Expr -> indent + compile(ast)
    is Statement -> {
        val stmt : Statement = ast
        val newIndent = "$indent$indentStr"
        val end = "\n${newIndent}pass"
        fun List<AST>.joinBody() = joinToString("\n") { compile(it, newIndent) }
        when(stmt) {
            is IfStep -> {
                val (cond, body, next) = stmt
                val thenStr = body.joinBody()
                val elseStr = when(next) {
                    is IfStep -> compile(next, indent).replaceFirst("if", "elif")
                    is Else -> compile(next, indent)
                    null -> ""
                }
                "${indent}if ${compile(cond)}:\n$thenStr$end\n$elseStr"
            }
            // TODO: Make sure we can't transpile a standalone else
            is Else -> "${indent}else:\n${stmt.body.joinBody()}$end"
            is VarDef -> "$indent${stmt.lhs.id} = ${compile(stmt.rhs)}"
            is UntypedVarDef -> "$indent${stmt.lhs.id} = ${compile(stmt.rhs)}"
            is VarReassign -> "$indent${stmt.lhs.id} = ${compile(stmt.rhs)}"
            is ArrayAssignment -> "$indent${stmt.lhs.id}[${compile(stmt.index)}] = ${compile(stmt.rhs)}"
            is FunDef -> {
                val (id, args, _, statements) = stmt
                val argStr = args.joinToString(transform = AnnotatedVar::id)
                val bodyStr = statements.joinBody()
                "${indent}def ${id.id}($argStr):\n$bodyStr$end"
            }
            is While -> {
                val bodyStr = stmt.body.joinBody()
                "${indent}while ${compile(stmt.cond)}:\n$bodyStr$end"
            }
            is For -> {
                val (id, list, body) = stmt
                val bodyStr = body.joinBody()
                "${indent}for ${id.id} in ${compile(list)}:\n$bodyStr$end"
            }
            is Return -> "${indent}return ${compile(stmt.toReturn)}"
        }
    }
}

fun compile(ast : AST) = compile(ast, "")

fun execByPy(prog : List<AST>) {
    val progStr = prog.joinToString(separator = "\n", transform = ::compile)
    val fileName = "tommygen${UUID.randomUUID().toString().replace("-", "")}.py"
    val pyFile = File(fileName)
    pyFile.writeText(stdLib.joinToString("\n", transform = ::compile))
    pyFile.appendText("\n")
    pyFile.appendText(progStr)
    Thread.sleep(1000)
    ProcessBuilder("python", fileName)
            .directory(File(System.getProperty("user.dir")))
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(1, TimeUnit.HOURS)
    pyFile.delete()
}