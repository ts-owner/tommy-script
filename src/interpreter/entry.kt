package interpreter
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.lexer.Lexer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
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

    val dirtyConverter = mapOf("String" to TString,
            "Int" to TInt,
            "Bool" to TBool,
            "Unit" to TUnit)

    class TommyParser : Grammar<List<AST>>() {
        val num by token("\\d+")

        val COLON by token(":")
        val LET by token("let")
        val EQUALS by token("=")

        //types
        val stringSymbol by token("String")
        val intSymbol by token("Int")
        val boolSymbol by token("Bool")

        val id by token("\\w+")

        val ws by token("\\s+",ignore = true)

        val type = stringSymbol or intSymbol or boolSymbol use {
            dirtyConverter[text]!!
        }

        val numParser = num use { LInt(text.toInt()) }
        val idParser = id use { text }

        //TODO num should be expr
        val annotatedVarParser = idParser and -COLON and type map {
            (a,b)-> AnnotatedVar(a,b)
        }

        val varDefParser = -LET and annotatedVarParser and -EQUALS and numParser map {
            (a,b)-> VarDef(a,b)
        }
        override val rootParser: Parser<List<AST>> = oneOrMore(varDefParser)
             //To change initializer of created properties use File | Settings | File Templates.
    }
    var result = TommyParser().tryParseToEnd(exampleScript.inputStream())
    println(result)

}