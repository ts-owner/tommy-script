package compiler

import core.*
import core.parser.escapeChars
import stdlib.stdLib
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

private fun String.escaped() : String {
    val builder = StringBuilder()
    for(c in this@escaped) {
        val unescaped = escapeChars.inverse[c] ?: c
        builder.append(unescaped)
    }
    return builder.toString()
}

// Turns a tommy-script expression into its python representation
private fun compile(expr : Expr) : String = when(expr) {
    is Var -> expr.id // TODO: Name scrambling?
    is Prefix -> "(${expr.op.toPython()} ${compile(expr.arg)})"
    is Infix -> "(${compile(expr.lhs)} ${expr.op.toPython()} ${compile(expr.rhs)})"
    is FunCall -> "${compile(expr.function)}(${expr.args.joinToString(", ", transform = ::compile)})"
    is LInt -> "${expr.value}"
    is LString -> "\"${expr.value.escaped()}\""
    is LBool -> if(expr.value) "True" else "False"
    is LArray -> expr.value.joinToString(prefix = "[", postfix = "]", transform = ::compile)
    is LFunction -> "lambda ${expr.args.joinToString { it.id }}: ${compile(expr.body)}"
    is LUnit -> "None" // TODO: Choose a better representation?
    is ArrayAccess -> "${compile(expr.array)}[${compile(expr.index)}]"
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
        is ArrayAssignment -> "${compile(stmt.lhs)}[${compile(stmt.index)}] = ${compile(stmt.rhs)}"
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

val builtinFunctions = """
import sys
def tommy_print(x):
    sys.stdout.write(x)
    sys.stdout.flush()
__builtins__['print'] = tommy_print
def push(arr, x):
    arr.append(x)

"""

fun execByPy(prog : List<Stmt>) {
    val progStr = prog.joinToString(separator = "\n", transform = ::compile)
    val fileName = "tommygen${UUID.randomUUID().toString().replace("-", "")}.py"
    val pyFile = File(fileName)
    pyFile.writeText(builtinFunctions)
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