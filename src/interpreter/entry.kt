package interpreter
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.Parser
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
        //Symbols
        val LPAR by token("\\(")
        val RPAR by token("\\)")
        val COLON by token(":")
        val LET by token("let\\b")
        val EQUALS by token("=")
        val PLUS by token("\\+")
        val MINUS by token("\\-")
        val DIV by token("/")
        val MOD by token("%")
        val TIMES by token("\\*")
        val OR by token("or")
        val AND by token("and\\b")
        val EQU by token("==")
        val NEQ by token("!=")
        val LEQ by token("<=")
        val GEQ by token(">=")
        val LT by token("<")
        val GT by token(">")
        val NOT by token("not\\b")
        val COMMA by token(",")
        val RETURN by token("return\\b")
        val END by token("end\\b")

        //Literals
        val NUM by token("\\d+")
        val STRING by token("\".*?\"") //TODO support escape characters
        val TRUE by token("true")
        val FALSE by token("false")

        //types
        val stringSymbol by token("String")
        val intSymbol by token("Int")
        val boolSymbol by token("Bool")

        val id by token("\\w+")

        val ws by token("\\s+",ignore = true)
        //LEXER OVER


        val idParser = id use { text }

        val type = stringSymbol or intSymbol or boolSymbol use {
            dirtyConverter[text]!!
        }

        val stringParser = STRING use { LString(text.substring(1,text.length-1))}
        val numParser = NUM use { LInt(text.toInt()) }
        val trueParser = TRUE use { LBool(true) }
        val falseParser = FALSE use { LBool(false) }
        val literalParser = stringParser or numParser or trueParser or falseParser
        val funCallParser = id and -LPAR and separatedTerms(parser(this::expr),COMMA,acceptZero = true) and -RPAR map {
            (name, args) -> FunCall(name.text,args)
        }
        val varParser = id use {Var(text)}

        val expr: Parser<Expr> = literalParser or funCallParser or varParser


        //TODO num should be expr
        val annotatedVarParser = idParser and -COLON and type map {
            (a,b)-> AnnotatedVar(a,b)
        }

        val varDefParser = -LET and annotatedVarParser and -EQUALS and expr map {
            (a,b)-> VarDef(a,b)
        }
        val untypedVarDefParser = -LET and varParser and -EQUALS and expr map {
            (a,b)->UntypedVarDef(a,b)
        }
        /*val funDefParser = -LET * idParser * -LPAR * separatedTerms(annotatedVarParser,
                COMMA, acceptZero = true) * -RPAR * -COLON * type * -EQUALS * zeroOrMore(parser(this::astParser)) * -END map {
            (a, b, c, d) -> FunDef(a,b,c,d)
        }*/

        val funDefParser = -LET * idParser *-LPAR * -separatedTerms(annotatedVarParser,COMMA,acceptZero = true) * -RPAR * -END map {
            FunDef(it, listOf(),TString,listOf())
        }
        //TODO if, fundef, return
        val statement : Parser<Statement> = varDefParser or untypedVarDefParser or funDefParser
        val astParser : Parser<AST> = expr or statement
        override val rootParser: Parser<List<AST>> = oneOrMore(astParser) //TODO make this correct
             //To change initializer of created properties use File | Settings | File Templates.
    }
    var result = TommyParser().tryParseToEnd(exampleScript.inputStream())
    TommyParser().parseToEnd(exampleScript.inputStream()).forEach {println(it)}

}
//TODO do all of the above todos, then do in/preops, then do control flow