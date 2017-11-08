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
        val COMMA by token(",")

        //keywords
        val NOT by token("not\\b")
        val RETURN by token("return\\b")
        val END by token("end\\b")
        val IF by token("if\\b")
        val THEN by token("then\\b")
        val ELSE by token("else\\b")

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

        val typeParser = stringSymbol or intSymbol or boolSymbol use {
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
        val varParser = idParser map {Var(it)}


        val expr: Parser<Expr> = literalParser or funCallParser or varParser

        val annotatedVarParser = idParser and -COLON and typeParser map {
            (a,b) -> AnnotatedVar(a,b)
        }

        val varDefParser = -LET and annotatedVarParser and -EQUALS and expr map {
            (a,b) -> VarDef(a,b)
        }

        val untypedVarDefParser = -(LET) and varParser and -EQUALS and expr map {
            (a,b) -> UntypedVarDef(a,b)
        }

        val varReassignParser = varParser and -EQUALS and expr map {
            (a,b) -> VarReassign(a,b)
        }

        val funDefParser = -LET * idParser *-LPAR * separatedTerms(annotatedVarParser, COMMA, acceptZero = true) * -RPAR * -COLON * typeParser * -EQUALS * zeroOrMore(parser(this::astParser))* -END map {
            (funname, args, rettype, children) -> FunDef(funname, args, rettype, children)
        }

        val returnParser = -RETURN and expr map {
            a -> Return(a)
        }

        val ifParser = -IF and expr and -THEN and zeroOrMore(parser(this::astParser)) and optional(-ELSE and zeroOrMore(parser(this::astParser))) and -END map {
            (cond,body, elsebody)-> If(cond,body,elsebody)
        }
        //TODO if, fundef, return
        val statement : Parser<Statement> = returnParser or varDefParser or untypedVarDefParser or funDefParser or varReassignParser or ifParser
        val astParser : Parser<AST> = statement or expr //order matters here for assignment!
        override val rootParser: Parser<List<AST>> = oneOrMore(astParser) //TODO make this correct
             //To change initializer of created properties use File | Settings | File Templates.
    }
    var result = TommyParser().tryParseToEnd(exampleScript.inputStream())
    result.toString()
    TommyParser().parseToEnd(exampleScript.inputStream()).forEach {println(it)}

}
//TODO do all of the above todos, then do in/preops, then do control flow