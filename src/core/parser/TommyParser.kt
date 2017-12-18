package core.parser

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.*
import com.sun.org.apache.xpath.internal.operations.And
import core.*
import java.util.*

data class NoSeparator(val separated : Separated<*, *>) : ErrorResult()
data class MixedAssociativity(val expr : Separated<Expr, InOp>) : ErrorResult()

// Main parser for a program
class TommyParser : Grammar<List<Stmt>>() {
    // Symbols
    private val LPAR by token("\\(")
    private val RPAR by token("\\)")
    private val LBRA by token("\\[")
    private val RBRA by token("]")
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
    private val MINUS by token("-")
    private val DIV by token("/")
    private val MOD by token("%")
    private val POW by token("\\*\\*")
    private val TIMES by token("\\*")
    private val OR by token("or")
    private val AND by token("and\\b")
    private val COMMA by token(",")

    // Keywords
    private val NOT by token("not\\b") //@Brendan maybe not should require a space after/before it?
    private val RETURN by token("return\\b")
    private val END by token("end")
    private val IF by token("if\\b")
    private val THEN by token("then\\b")
    private val ELSE by token("else\\b")
    private val ELSEIF by token("elseif\\b")
    private val WHILE by token("while\\b")
    private val FOR by token("for\\b")
    private val DO by token("\\bdo")
    private val IN by token("in\\b")

    // Literals
    private val NUM by token("\\d+")
    private val STRING by token("\".*\"")
    private val TRUE by token("true")
    private val FALSE by token("false")
    private val UNIT by token("unit")

    // Types
    private val stringSymbol by token("String")
    private val intSymbol by token("Int")
    private val boolSymbol by token("Bool")
    private val unitSymbol by token("Unit")

    private val id by token("\\w+")

    private val ws by token("\\s+", ignore = true)

    private val idParser = id use { text }

    private val arrayTypeParser = -LBRA and parser(this::typeParser) and -RBRA map { TArray }

    private val typeParser : Parser<Type> = stringSymbol.asJust(TString) or
            intSymbol.asJust(TInt) or
            boolSymbol.asJust(TBool) or
            unitSymbol.asJust(TUnit) or
            arrayTypeParser

    private val stringParser = STRING.map { match -> StringParser().tryParseToEnd(match.text) }
                                     .join().map(::LString)
    private val numParser = NUM use { LInt(text.toInt()) }
    private val boolParser = TRUE.asJust(LBool(true)) or FALSE.asJust(LBool(false))
    private val unitParser = UNIT.asJust(LUnit)
    private val arrayParser = -LBRA and separatedTerms(parser(this::expr), COMMA, acceptZero = true) and -RBRA map(::LArray)
    private val literalParser = stringParser or numParser or boolParser or unitParser or arrayParser

    private val callsiteArgsParser = -LPAR and separatedTerms(parser(this::expr), COMMA, acceptZero = true) and -RPAR
    private val funCallParser = id and callsiteArgsParser map { (name, args) -> FunCall(Var(name.text), args) }

    private val varParser = idParser.map(::Var)

    private val arrayGetParser = varParser and -LBRA and parser(this::expr) and -RBRA map { (name, index) -> ArrayAccess(name, index) }
    private val arraySetParser = varParser and -LBRA and parser(this::expr) and -RBRA and -EQUALS and
            parser(this::expr) map { (name, index, newArr) -> ArrayAssignment(name, index, newArr) }

    private val whileParser = -WHILE and parser(this::expr) and
                                           -DO and zeroOrMore(parser(this::statement)) and
                                           -END map { (cond, statement) -> While(cond, statement) }
    private val forParser = -FOR and idParser and -IN and parser(this::expr) and
            -DO and zeroOrMore(parser(this::statement)) and
            -END map { (elem, list, body) -> For(Var(elem), list, body) }

    private val preexpr = literalParser or funCallParser or arrayGetParser or varParser or arrayParser or
            (-LPAR and parser(this::expr) and -RPAR)

    private val expr : Parser<Expr> = kotlin.run {
        val prefixTokens = biMapOf(PreOp.negate to MINUS, PreOp.plus to PLUS, PreOp.not to NOT)
        val infixTokens = biMapOf(InOp.plus to PLUS, InOp.subtract to MINUS, InOp.times to TIMES,
                                  InOp.div to DIV, InOp.mod to MOD, InOp.power to POW, InOp.concat to CONCAT,
                                  InOp.and to AND, InOp.or to OR, InOp.eqInt to EQU, InOp.lt to LT,
                                  InOp.gt to GT, InOp.leq to LEQ, InOp.geq to GEQ, InOp.neq to NEQ)
        val prefixOperators = PreOp.values().groupBy { it.precedence }.mapValues {
            (_, ops) -> ops.map { prefixTokens[it]!! }
        }
        val infixOperators = InOp.values().groupBy { it.precedence }.mapValues {
            (_, ops) -> ops.map { infixTokens[it]!! }
        }
        val precedences : SortedSet<Int> = TreeSet(reverseOrder())
        prefixOperators.mapTo(precedences) { (prec, _) -> prec }
        infixOperators.mapTo(precedences) { (prec, _) -> prec }
        precedences.fold(preexpr) { currExprParser, prec ->
            val prefixOpParser = OrCombinator(prefixOperators[prec].orEmpty()) map {
                prefixTokens.inverse[it.type]!!
            }
            val prefix = prefixOpParser and currExprParser map { (op, arg) -> Prefix(op, arg) }
            val infixOps = OrCombinator(infixOperators[prec].orEmpty()) map {
                infixTokens.inverse[it.type]!!
            }
            val infix = separated(currExprParser, infixOps).map {
                val associativity = it.separators.firstOrNull()?.associativity ?:
                        return@map NoSeparator(it)
                if(it.separators.any { op -> op.associativity != associativity })
                        return@map MixedAssociativity(it)
                Parsed(when(associativity) {
                    Associativity.LEFT -> it.reduce { lhs, op, rhs -> Infix(op, lhs, rhs) }
                    Associativity.RIGHT -> it.reduceRight { lhs, op, rhs -> Infix(op, lhs, rhs) }
                }, emptySequence())
            }.join()
            prefix or infix
        }
    }

    private val annotatedVarParser = idParser and -COLON and typeParser map { (a, b) -> AnnotatedVar(a, b) }
    private val varDefParser = -LET and annotatedVarParser and -EQUALS and expr map { (a, b) -> VarDef(a, b) }
    private val untypedVarDefParser = -LET and varParser and -EQUALS and expr map { (a, b) -> UntypedVarDef(a, b) }

    private val varReassignParser = varParser and -EQUALS and expr map { (a, b) -> VarReassign(a, b) }

    private val definitionArgsParser = -LPAR and separatedTerms(annotatedVarParser, COMMA, acceptZero = true) and -RPAR
    private val funDefParser = -LET and idParser and definitionArgsParser and -COLON and typeParser and -EQUALS and
            parser(this::bodyParser) and
            -END map { (funName, args, retType, children) -> FunDef(Var(funName), args, retType, children) }

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

    private val bodyParser = zeroOrMore(statement)
    override val rootParser = oneOrMore(statement)
}