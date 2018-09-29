package core.parser

import com.github.h0tk3y.betterParse.combinators.asJust
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.*

internal class BindCombinator<T, out R>(
        val innerParser : Parser<T>,
        val transform : (T) -> Parser<R>
) : Parser<R> {
    override fun tryParse(tokens: Sequence<TokenMatch>): ParseResult<R> {
        val innerResult = innerParser.tryParse(tokens)
        return when (innerResult) {
            is ErrorResult -> innerResult
            is Parsed -> transform(innerResult.value).tryParse(innerResult.remainder)
        }
    }
}

internal infix fun<A, B> Parser<A>.bind(transform : (A) -> Parser<B>) = BindCombinator(this, transform)

internal class FailCombinator(val err : ErrorResult) : Parser<Nothing> {
    override fun tryParse(tokens : Sequence<TokenMatch>) = err
}
internal fun fail(err : ErrorResult) = FailCombinator(err)
internal fun<T> pure(value : T) : Parser<T> = EmptyParser.asJust(value)

internal data class GuardFailure<T>(val failing : T, val test : (T) -> Boolean) : ErrorResult()
internal class GuardCombinator<T>(val innerParser: Parser<T>, val test : (T) -> Boolean) : Parser<T> {
    override fun tryParse(tokens : Sequence<TokenMatch>) : ParseResult<T> {
        val result = innerParser.tryParse(tokens)
        return when(result) {
            is ErrorResult -> result
            is Parsed -> if(test(result.value)) result else GuardFailure(result.value, test)
        }
    }
}
internal infix fun<T> Parser<T>.guard(test : (T) -> Boolean) = GuardCombinator(this, test)