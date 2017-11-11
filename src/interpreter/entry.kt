package interpreter

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails

fun main(args: Array<String>) {
    //test the AST
    var testExpression = Infix(InOp.plus, LInt(1), Prefix(PreOp.negate, Var("a")))
    assertEquals(TInt, typeInfer(mapOf("a" to TInt), testExpression))
    assertFails { typeInfer(mapOf("a" to TBool), testExpression) }

    //TODO remove hardcoded filename
    //interpreter/testscript is a more complete example
    var exampleScript = File(args.first())
    var lines = exampleScript.readLines()
    println(lines)

    TommyParser().parseToEnd(exampleScript.inputStream()).forEach { println(it) }

}
//TODO elseif, all the dangling operators (from python reference, array access, etc)
//TODO: fix exception, test operators at all, makes ure leftAssociative falls through like I think it does ...
// (look through example, make test case)