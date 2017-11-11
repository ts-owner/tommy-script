package interpreter

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.*
import java.util.regex.Pattern

class BindCombinator<T, R>(
        val innerParser: Parser<T>,
        val transform: (T) -> Parser<R>
) : Parser<R> {
    private fun Parsed<T>.sneakyGetRemainder() : Sequence<TokenMatch> {
        val ParsedClass = Parsed::class.java
        val remainderF = ParsedClass.getDeclaredField("remainder")
        remainderF.isAccessible = true
        return remainderF.get(this) as Sequence<TokenMatch>
    }

    override fun tryParse(tokens: Sequence<TokenMatch>): ParseResult<R> {
        val innerResult = innerParser.tryParse(tokens)
        return when (innerResult) {
            is ErrorResult -> innerResult
            is Parsed -> transform(innerResult.value).tryParse(innerResult.sneakyGetRemainder())
        }
    }
}

infix fun <A, T> Parser<A>.bind(transform: (A) -> Parser<T>) = BindCombinator(this, transform)

internal class FailCombinator(val err : ErrorResult) : Parser<Nothing> {
    override fun tryParse(tokens : Sequence<TokenMatch>) = err
}
internal fun fail(err : ErrorResult) = FailCombinator(err)
internal fun <T> pure(value : T) : Parser<T> = EmptyParser.asJust(value)

internal fun <T> Parser<ParseResult<T>>.join() : Parser<T> = this.bind { res ->
    when(res) {
        is Parsed -> pure(res.value)
        is ErrorResult -> fail(res)
    }
}

val escapeChars = mapOf('t' to '\t', 'n' to '\n', '\"' to '\"', '\\' to '\\')

internal class StringParser : Grammar<String>() {
    private val ESCAPESEQUENCE by token("\\[${escapeChars.keys.joinToString("")}]")
    private val QUOTE by token("\"")
    private val CHAR by token(".")

    private val escapeParser = ESCAPESEQUENCE.map { match ->
        escapeChars[match.text[1]]!! //Safe because match.text passes the ESCAPESEQUENCE pattern
    }

    override val rootParser =
            -QUOTE and zeroOrMore(escapeParser or CHAR.use { text.first() } ) and -QUOTE map {
                chars -> String(chars.toCharArray())
            }
}

class TommyParser : Grammar<List<AST>>() {
    //Symbols
    private val LPAR by token("\\(")
    private val RPAR by token("\\)")
    private val EQU by token("==")
    private val NEQ by token("!=")
    private val LEQ by token("<=")
    private val GEQ by token(">=")
    private val LT by token("<")
    private val GT by token(">")
    private val COLON by token(":")
    private val LET by token("let\\b")
    private val EQUALS by token("=")
    private val CONCAT by token("\\+\\+")
    private val PLUS by token("\\+")
    private val MINUS by token("\\-")
    private val DIV by token("/")
    private val MOD by token("%")
    private val POW by token("\\*\\*")
    private val TIMES by token("\\*")
    private val OR by token("or")
    private val AND by token("and\\b")
    private val COMMA by token(",")

    //keywords
    private val NOT by token("not\\b") //@Brendan maybe not should require a space after/before it?
    private val RETURN by token("return\\b")
    private val END by token("end\\b")
    private val IF by token("if\\b")
    private val THEN by token("then\\b")
    private val ELSE by token("else\\b")

    //Literals
    private val NUM by token("\\d+")
    private val STRING by token("\".*\"")
    private val TRUE by token("true")
    private val FALSE by token("false")

    //types
    private val stringSymbol by token("String")
    private val intSymbol by token("Int")
    private val boolSymbol by token("Bool")

    private val id by token("\\w+")

    private val ws by token("\\s+", ignore = true)

    //LEXER OVER

    private val preOpSymbols = NOT or PLUS or MINUS
    private val inOpSymbols = PLUS or MINUS or TIMES or DIV or CONCAT or AND or OR or EQU or LT or GT or LEQ or GEQ or NEQ

    private val idParser = id use { text }

    private val typeParser = stringSymbol.asJust(TString) or
                             intSymbol.asJust(TInt) or
                             boolSymbol.asJust(TBool)

    private val stringParser = STRING.map { match -> StringParser().tryParseToEnd(match.text) }
                                     .join().map(::LString)

    private val numParser = NUM use { LInt(text.toInt()) }
    private val trueParser = TRUE.asJust(LBool(true))
    private val falseParser = FALSE.asJust(LBool(false))
    private val literalParser = stringParser or numParser or trueParser or falseParser
    private val funCallParser = id and -LPAR and
                                separatedTerms(parser(this::expr), COMMA, acceptZero = true) and
                                -RPAR map { (name, args) -> FunCall(name.text, args) }
    private val varParser = idParser.map(::Var)

    //switch out preexper thing with parser(this::expr) later, if it works with preexpr
    private val preexpr = literalParser or funCallParser or varParser or
                          (-LPAR and parser(this::expr) and -RPAR)

    //operators zone
    //TODO make this not seizure material

    //make the levels general. maybe need to pull request/add something
    val lvThirteenOperatorChain: Parser<Expr> = leftAssociative(preexpr, POW) { l, o, r ->
        // use o for generalization
        Infix(InOp.power, l, r)
    }

    val lvTwelveOperatorChain: Parser<Expr> = (PLUS or MINUS) * lvThirteenOperatorChain map { (a, b) ->
        Prefix(if (a.type == PLUS) PreOp.plus else PreOp.negate, b)
    }

    //give alternative path around prefix operator
    val lvElevenOperatorChain: Parser<Expr> = leftAssociative(lvTwelveOperatorChain or lvThirteenOperatorChain, (DIV or TIMES)) { l, o, r ->
        Infix(if (o.type == DIV) InOp.div else InOp.times, l, r)
    }

    val lvTenOperatorChain: Parser<Expr> = leftAssociative(lvElevenOperatorChain, (PLUS or MINUS)) { l, o, r ->
        Infix(if (o.type == PLUS) InOp.plus else InOp.negate, l, r)
    }

    val lvNineOperatorSymbols = EQU or NEQ or LEQ or GEQ or LT or GT
    private val tokenToOperator = mapOf(
            EQU to InOp.eqInt, NEQ to InOp.neq, LEQ to InOp.leq, GEQ to InOp.geq, LT to InOp.lt, GT to InOp.gt
    )
    val lvNineOperatorChain: Parser<Expr> = leftAssociative(lvTenOperatorChain, lvNineOperatorSymbols) { l, o, r ->
        Infix(tokenToOperator[o.type]!!, l, r)
    }

    val lvSixOperatorChain: Parser<Expr> = NOT * lvNineOperatorChain map { (a, b) ->
        Prefix(PreOp.not, b)
    }
    //give alternative path around prefix operator
    val lvFiveOperatorChain: Parser<Expr> = leftAssociative(lvSixOperatorChain or lvNineOperatorChain, AND) { l, _, r ->
        Infix(InOp.and, l, r);
    }

    val lvFourOperatorChain: Parser<Expr> = leftAssociative(lvFiveOperatorChain, OR) { l, _, r ->
        Infix(InOp.or, l, r);
    }


    private val expr = lvFourOperatorChain
    //end operators zone

    private val annotatedVarParser = idParser and -COLON and typeParser map { (a, b) ->
        AnnotatedVar(a, b)
    }

    private val varDefParser = -LET and annotatedVarParser and -EQUALS and expr map { (a, b) ->
        VarDef(a, b)
    }

    private val untypedVarDefParser = -LET and varParser and -EQUALS and expr map { (a, b) ->
        UntypedVarDef(a, b)
    }

    private val varReassignParser = varParser and -EQUALS and expr map { (a, b) ->
        VarReassign(a, b)
    }

    private val funDefParser = -LET and idParser and
            -LPAR and separatedTerms(annotatedVarParser, COMMA, acceptZero = true) and -RPAR and
            -COLON and typeParser and -EQUALS and
            zeroOrMore(parser(this::astParser)) and
            -END map { (funName, args, retType, children) ->
        FunDef(funName, args, retType, children)
    }

    private val returnParser = -RETURN and expr.map(::Return)

    private val ifParser = -IF and expr and -THEN and zeroOrMore(parser(this::astParser)) and
            optional(-ELSE and zeroOrMore(parser(this::astParser))) and
            -END map { (cond, body, elsebody) -> If(cond, body, elsebody) }

    private val statement : Parser<Statement> = returnParser or varDefParser or untypedVarDefParser or funDefParser or varReassignParser or ifParser
    private val astParser = statement or expr //order matters here for assignment!
    override val rootParser = oneOrMore(astParser) //TODO make this correct
}