package core.parser

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar

private val escapeChars = mapOf('t' to '\t', 'n' to '\n', '\"' to '\"', '\\' to '\\')

internal class StringParser : Grammar<String>() {
    private val ESCAPESEQUENCE by token("\\[${escapeChars.keys.joinToString("")}]")
    private val QUOTE by token("\"")
    private val CHAR by token(".")

    private val escapeParser = ESCAPESEQUENCE.map { match ->
        escapeChars[match.text[1]]!! //Safe because match.text passes the ESCAPESEQUENCE pattern
    }

    override val rootParser =
            -QUOTE and zeroOrMore(escapeParser or CHAR.use { text.first() }) and -QUOTE map {
                chars -> String(chars.toCharArray())
            }
}