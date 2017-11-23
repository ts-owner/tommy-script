package compiler

import core.*
import core.parser.escapeChars
import standard_library.stdLib
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

private fun PreOp.toPython() = when(this) {
    PreOp.plus -> ""
    PreOp.negate -> "-"
    PreOp.not -> "not"
}

private fun InOp.toPython() = when(this) {
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

// Python names certain functions differently
private fun handleSpecialFunctions(func : FunCall) = when(func.id.id) {
    "print" -> "print(${func.args.joinToString(", ", transform = ::compile)}, end='', flush=True)"
    "concat" -> "${compile(func.args[0])} + ${compile(func.args[1])}"
    "push" -> "${compile(func.args[0])}.append(${compile(func.args[1])})"
    else -> null
}

private fun String.escaped() : String = buildString {
    for(c in this@escaped) {
        val unescaped = escapeChars.entries.find { it.value == c }?.key
        when(unescaped) {
            null -> append(c)
            else -> append("\\$unescaped")
        }
    }
}

// Turns a tommy-script expression into its python representation
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
    is LUnit -> "None" // TODO: Choose a better representation?
    is ArrayAccess -> "${expr.name.id}[${compile(expr.index)}]"
}

private val indentStr = "    "

// Turns a tommy-script statement into its python representation
private fun compile(stmt : Stmt, indent : String) : String {
    val newIndent = "$indent$indentStr"
    // Our ifs/function declarations/etc... allow for empty bodies, but python's don't
    // Thus we append a nop to each body
    fun Body.joinBody() = joinToString("\n") { compile(it, newIndent) } + "\n${newIndent}pass"
    return indent + when(stmt) {
        is EvalExpr -> compile(stmt.expr)
        is IfStep -> {
            val (cond, body, next) = stmt
            val thenStr = body.joinBody()
            val elseStr = when(next) {
                is IfStep -> compile(next, indent).replaceFirst("if", "elif")
                is Else -> compile(next, indent)
                null -> ""
            }
            "if ${compile(cond)}:\n$thenStr\n$elseStr"
        }
        // TODO: Make sure we can't transpile a standalone else
        is Else -> "else:\n${stmt.body.joinBody()}"
        is VarDef -> "${stmt.lhs.id} = ${compile(stmt.rhs)}"
        is UntypedVarDef -> "${stmt.lhs.id} = ${compile(stmt.rhs)}"
        is VarReassign -> "${stmt.lhs.id} = ${compile(stmt.rhs)}"
        is ArrayAssignment -> "${stmt.lhs.id}[${compile(stmt.index)}] = ${compile(stmt.rhs)}"
        is FunDef -> {
            val (id, args, _, statements) = stmt
            val argStr = args.joinToString(transform = AnnotatedVar::id)
            val bodyStr = statements.joinBody()
            "def ${id.id}($argStr):\n$bodyStr"
        }
        is While -> {
            val bodyStr = stmt.body.joinBody()
            "while ${compile(stmt.cond)}:\n$bodyStr"
        }
        is For -> {
            val (id, list, body) = stmt
            val bodyStr = body.joinBody()
            "for ${id.id} in ${compile(list)}:\n$bodyStr"
        }
        is Return -> "return ${compile(stmt.toReturn)}"
    }
}

// We only expose the t
fun compile(stmt : Stmt) = compile(stmt, "")

fun execByPy(prog : List<Stmt>) {
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