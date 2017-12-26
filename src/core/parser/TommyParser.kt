package core.parser

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.*
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.*
import core.*
import java.io.InputStream
import java.util.*

// Main parser for a program
object TommyParser : Grammar<List<Stmt>>() {
    // Symbols
    private val LPAR by token("\\(")
    private val RPAR by token("\\)")
    private val LBRA by token("\\[")
    private val RBRA by token("]")
    private val COLON by token(":")
    private val ARROW by token("->|\u2192") // 0x2192 is the charchode for latex's \rightarrow
    private val LAMBDA by token("[$BACKSLASH\u03bb]") // 0x03bb is the charchode for lambda
    private val LET by token("let\\b")

    // Operators
    private val EQOP by token("==")
    private val NEQ by token("!=")
    private val LEQ by token("<=")
    private val GEQ by token(">=")
    private val LT by token("<")
    private val GT by token(">")
    private val CONCAT by token("""\+\+""")
    private val PLUS by token("""\+""")
    private val MINUS by token("-")
    private val DIV by token("/")
    private val MOD by token("%")
    private val POW by token("""\*\*""")
    private val TIMES by token("""\*""")
    private val OR by token("""or\b""")
    private val AND by token("""and\b""")
    private val NOT by token("""not\b""")
    private val COMMA by token(",")
    private val EQUALS by token("=")

    // Literals
    private val NUM by token("\\d+")
    private val STRING by token("\"[^\"]*\"")
    private val BOOL by token("true|false")
    private val UNIT by token("unit")

    // Keywords
    private val RETURN by token("return\\b")
    private val END by token("end(?!\\w)")
    private val IF by token("if\\b")
    private val THEN by token("then\\b")
    private val ELSE by token("else\\b")
    private val ELSEIF by token("elseif\\b")
    private val WHILE by token("while\\b")
    private val FOR by token("for\\b")
    private val DO by token("\\bdo")
    private val IN by token("in\\b")

    // Types
    private val stringSymbol by token("String")
    private val intSymbol by token("Int")
    private val boolSymbol by token("Bool")
    private val unitSymbol by token("Unit")
    private val separator by token(";")

    private val id by token("\\w+")

    private val ws by token("\\s+", ignore = true)

    private val idParser = id use { text }
    private val varParser = idParser.map(::Var)
    private val separatorParser = oneOrMore(separator)

    private val arrayTypeParser = -LBRA and parser(this::typeParser) and -RBRA map { TArray }
    private val functionTypePatser = -LPAR and separatedTerms(parser(this::typeParser), COMMA
                                                             , acceptZero = true) and -RPAR and
                                     -ARROW and parser(this::typeParser) map { (dom, cod) -> TFunction(dom, cod) }

    private val typeParser : Parser<Type> = stringSymbol.asJust(TString) or
            intSymbol.asJust(TInt) or
            boolSymbol.asJust(TBool) or
            unitSymbol.asJust(TUnit) or
            arrayTypeParser or
            functionTypePatser

    private val annotatedVarParser = idParser and -COLON and typeParser map {
        (ident, type) -> AnnotatedVar(ident, type)
    }

    private object ExprParser : PrattParser() {
        private const val HIGHEST_PRECEDENCE = 100
        private const val COMMA_BP = 1
        private const val LITERAL_BP = -1

        private val prefixTokens =
                mapOf(MINUS to PreOp.negate, PLUS to PreOp.plus, NOT to PreOp.not)
        private val infixTokens =
                mapOf(PLUS to InOp.plus, MINUS to InOp.subtract, TIMES to InOp.times,
                      DIV to InOp.div, MOD to InOp.mod, POW to InOp.power, CONCAT to InOp.concat,
                      AND to InOp.and, OR to InOp.or, EQOP to InOp.eqInt, LT to InOp.lt,
                      GT to InOp.gt, LEQ to InOp.leq, GEQ to InOp.geq, NEQ to InOp.neq)

        private fun prefixNUD(op : PreOp) : NUD = { _, bp ->
            parser(bp).map { arg -> Prefix(op, arg) }
        }
        private fun infixLED(op : InOp) : LED = { _, left, bp ->
                parser(bp).map { right -> Infix(op, left, right) }
        }
        private fun literalNUD(mkLit : (String) -> Expr) : NUD =
                { match, _ -> pure(mkLit(match.text)) }

        override fun makeExpressionParser() : Parser<Expr> {
            registerLeft(HIGHEST_PRECEDENCE, LPAR) { _, left, _ ->
                separated(this.parser(COMMA_BP), COMMA, acceptZero = true) and -RPAR map { FunCall(left, it.terms) }
            }
            registerLeft(HIGHEST_PRECEDENCE, LBRA) { _, left, _ ->
                parser() and -RBRA map { idx -> ArrayAccess(left, idx) }
            }
            prefixTokens.forEach { (tok, op) -> registerNull(op.precedence, tok, prefixNUD(op)) }
            infixTokens.forEach { (tok, op) ->
                when(op.associativity) {
                    Associativity.LEFT -> registerLeft(op.precedence, tok, infixLED(op))
                    Associativity.RIGHT -> registerRightAssoc(op.precedence, tok, infixLED(op))
                }
            }
            registerNull(0, LPAR) { _, bp -> parser(bp) and -RPAR }
            registerNull(LITERAL_BP, id, literalNUD { Var(it) })
            registerNull(LITERAL_BP, NUM, literalNUD { LInt(it.toInt()) })
            registerNull(LITERAL_BP, BOOL, literalNUD { LBool(it.toBoolean()) })
            registerNull(LITERAL_BP, UNIT, literalNUD { LUnit })
            registerNull(LITERAL_BP, STRING) { match, _ ->
                StringParser.tryParseToEnd(match.text).let {
                    when(it) {
                        is Parsed -> pure(LString(it.value))
                        is ErrorResult -> fail(it)
                    }
                }
            }
            registerNull(LITERAL_BP, LAMBDA) { _, bp ->
                separated(varParser, COMMA, acceptZero = true) and -ARROW and parser(bp) map {
                    (argSep, body) -> LFunction(argSep.terms, body)
                }
            }
            registerNull(LITERAL_BP, LBRA) { _, _ ->
                separated(parser(COMMA_BP), COMMA, acceptZero = true) and -RBRA map { LArray(it.terms) }
            }
            return parser()
        }
    }

    private val expr = ExprParser.makeExpressionParser()

    private val arraySetParser = expr guard { it is ArrayAccess } and -EQUALS and expr map {
        (arrayAccess, elem) -> (arrayAccess as ArrayAccess).let {
            (array, index) -> ArrayAssignment(array, index, elem)
        }
    }

    private val whileParser = -WHILE and expr and
            -DO and parser(this::bodyParser) and
            -END map { (cond, statement) -> While(cond, statement) }
    private val forParser = -FOR and idParser and -IN and expr and
            -DO and parser(this::bodyParser) and
            -END map { (elem, list, body) -> For(Var(elem), list, body) }

    private val varDefParser = -LET and annotatedVarParser and -EQUALS and expr map { (a, b) -> VarDef(a, b) }
    private val untypedVarDefParser = -LET and varParser and -EQUALS and expr map { (a, b) -> UntypedVarDef(a, b) }

    private val varReassignParser = varParser and -EQUALS and expr map { (a, b) -> VarReassign(a, b) }

    private val definitionArgsParser = -LPAR and separatedTerms(annotatedVarParser, COMMA, acceptZero = true) and -RPAR
    private val funDefParser = -LET and varParser and definitionArgsParser and -COLON and typeParser and -EQUALS and
            parser(this::bodyParser) and -END map { (funName, args, retType, children) ->
        FunDef(funName, args, retType, children)
    }

    private val returnParser = -RETURN and expr.map(::Return)

    private val elseifParser = -ELSEIF and expr and -THEN and parser(this::bodyParser)
    private val ifParser = -IF and expr and -THEN and parser(this::bodyParser) and
             zeroOrMore(elseifParser) and optional(-ELSE and parser(this::bodyParser)) and
            -END map { (cond, body, elifs, elsebody) ->
                val elseBranch : If? = elsebody?.let { Else(it) }
                val elseIfs = elifs.foldRight(elseBranch) { (currCond, currBody), next ->
                    IfStep(currCond, currBody, next)
                }
                IfStep(cond, body, elseIfs)
            }

    private val loneExprParser = expr.map(::EvalExpr)

    private val statement : Parser<Statement> = returnParser or varDefParser or untypedVarDefParser or funDefParser or
            varReassignParser or ifParser or whileParser or forParser or arraySetParser or loneExprParser

    private val bodyParser = (separatedTerms(statement, separatorParser) and -separatorParser) or
            pure(emptyList())
    override val rootParser = bodyParser

    operator fun invoke(tokens : Sequence<TokenMatch>) = parseToEnd(tokens)
    operator fun invoke(tokens : String) = parseToEnd(tokens)
    operator fun invoke(tokens : Scanner) = parseToEnd(tokens)
    operator fun invoke(tokens : Readable) = parseToEnd(tokens)
    operator fun invoke(tokens : InputStream) = parseToEnd(tokens)
}