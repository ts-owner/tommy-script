package core.parser

import com.github.h0tk3y.betterParse.combinators.OrCombinator
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.*
import core.Expr
import kotlin.reflect.KProperty

internal typealias BindingPower = Int
internal typealias NUD = (TokenMatch, BindingPower) -> Parser<Expr>
internal typealias LED = (TokenMatch, Expr, BindingPower) -> Parser<Expr>

internal data class NullInfo(val nud : NUD, val bindingPower : BindingPower)
internal data class LeftInfo(val led : LED, val lbp : BindingPower, val rbp : BindingPower)

private data class RightBindingTooHard(val rbp : BindingPower, val lbp : BindingPower
                                      , val match : TokenMatch) : ErrorResult()
internal abstract class PrattParser {
    private val nullLookup : MutableMap<Token, NullInfo> = mutableMapOf()
    private val leftLookup : MutableMap<Token, LeftInfo> = mutableMapOf()

    protected fun registerNull(bindingPower : BindingPower, token : Token, nud : NUD) {
        nullLookup[token] = NullInfo(nud, bindingPower)
    }

    private fun registerLED(lbp : BindingPower, rbp : BindingPower, token : Token, led : LED) {
        leftLookup[token] = LeftInfo(led, lbp, rbp)
    }

    protected fun registerLeft(bindingPower : BindingPower, token : Token, led : LED) =
            registerLED(bindingPower, bindingPower, token, led)

    protected fun registerRightAssoc(bindingPower : BindingPower, token : Token, led : LED) =
            registerLED(bindingPower, bindingPower - 1, token, led)

    protected fun parser(rbp : BindingPower = 0) : Parser<Expr> {
        val nulls = OrCombinator(nullLookup.keys.toList())
        val lefts = OrCombinator(leftLookup.keys.toList())
        fun ledLoop(left : Expr) : Parser<Expr> = lefts.bind { match ->
            val (led, lbp, currRBP) = leftLookup[match.type]!!
            if(rbp >= lbp) fail(RightBindingTooHard(rbp = rbp, lbp = lbp, match = match))
            else led(match, left, currRBP).bind { newLeft -> ledLoop(newLeft) }
        } or pure(left)
        return nulls.bind { match ->
            val (nud, bp) = nullLookup[match.type]!!
            nud(match, bp)
        }.bind(::ledLoop)
    }

    abstract fun makeExpressionParser() : Parser<Expr>
}