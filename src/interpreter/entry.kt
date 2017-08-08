package interpreter
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.Lexer
import com.github.h0tk3y.betterParse.lexer.Token
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
    var exampleScript = File("interpreter/easy.tom")
    var lines = exampleScript.readLines()
    println(lines)

    val id = Token("identifier", pattern= "\\w+")
    val number = Token("integer", pattern= "\\d+")
    val ws = Token("whitespace", pattern="\\s+")

    val lexer = Lexer(listOf(number,id))
    val tokenMatches = lexer.tokenize(exampleScript.inputStream())
    tokenMatches.forEach { println(it) }
}