package interpreter

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import core.parser.TommyParser
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails
import core.*

fun main(args: Array<String>) {
    //test the AST
    var testExpression = Infix(InOp.plus, LInt(1), Prefix(PreOp.negate, Var("a")))
    assertEquals(TInt, typeInfer(mapOf("a" to TInt), testExpression))
    assertFails { typeInfer(mapOf("a" to TBool), testExpression) }

    //read file from args
    var exampleScript = File(args.first())
    var lines = exampleScript.readLines()

    //hardcode that second arg is debug
    var debug = args.contains("debug")

    if (debug) println(lines)

    var parseResult = TommyParser().parseToEnd(exampleScript.inputStream())
    //walkTree(debug, parseResult)


}
//TODO all the dangling operators (from python reference, array access, etc)
// (look through example, make test case)