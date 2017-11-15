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
    val testExpression = Infix(InOp.plus, LInt(1), Prefix(PreOp.negate, Var("a")))
    assertEquals(TInt, typeInfer(mapOf("a" to TInt), testExpression))
    assertFails { typeInfer(mapOf("a" to TBool), testExpression) }

    //read file from args
    val exampleScript = try {
        File(args.last())
    } catch (e: NoSuchElementException) {
        throw IllegalArgumentException("No path was given")
    }

    val lines = try {
        exampleScript.readLines()
    } catch (e: FileNotFoundException) {
        throw IllegalArgumentException("${args.first()}: No such file or directory")
    }

    val debug = args.contains("-d")

    if (debug) println(lines)

    val parseResult = TommyParser().parseToEnd(exampleScript.inputStream())

    interpretProgram(parseResult)
}
//TODO all the dangling operators (from python reference, array access, etc)
// (look through example, make test case)