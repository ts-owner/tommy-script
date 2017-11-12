package interpreter

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import core.parser.TommyParser
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails
import core.*
import java.io.FileNotFoundException

fun main(args: Array<String>) {
    //test the AST
    var testExpression = Infix(InOp.plus, LInt(1), Prefix(PreOp.negate, Var("a")))
    assertEquals(TInt, typeInfer(mapOf("a" to TInt), testExpression))
    assertFails { typeInfer(mapOf("a" to TBool), testExpression) }

    var exampleScript:File?
    var lines:List<String>?

    //read file from args
    try {
        exampleScript = File(args.first())
    } catch (e: NoSuchElementException) {
        throw IllegalArgumentException("no path was given")
    }

    try {
        lines = exampleScript.readLines()
    } catch (e: FileNotFoundException) {
        throw IllegalArgumentException(args.first() + ": No such file or directory")
    }


    //hardcode that second arg is debug
    var debug = args.contains("debug")

    if (debug) println(lines)

    var parseResult = TommyParser().parseToEnd(exampleScript.inputStream())
    walkTree(debug, parseResult)


}
//TODO all the dangling operators (from python reference, array access, etc)
// (look through example, make test case)