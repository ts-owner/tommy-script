package core.parser

import com.github.h0tk3y.betterParse.combinators.asJust
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.*

internal class BindCombinator<T, R>(
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

internal infix fun <A, T> Parser<A>.bind(transform: (A) -> Parser<T>) = BindCombinator(this, transform)

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