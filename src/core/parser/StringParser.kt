package core.parser

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap

fun<K : Any, V : Any> biMapOf(vararg pairs : Pair<K, V>) : BiMap<K, V> {
    val ret = HashBiMap<K, V>(pairs.size)
    pairs.forEach { (k, v) -> ret[k] = v }
    return ret
}
val escapeChars = biMapOf('t' to '\t', 'n' to '\n', '\"' to '\"', '\\' to '\\')
internal const val BACKSLASH = """\\"""
// This is used to generate the regex which matches escape chars
private val escapeSet = escapeChars.keys.joinToString("") { if(it == '\\') BACKSLASH else "$it" }

// Parser for strings which escapes escape characters (e.g. the text \t becomes a tab)
internal object StringParser : Grammar<String>() {
    private val ESCAPESEQUENCE by token("$BACKSLASH[$escapeSet]")
    private val QUOTE by token("\"")
    private val CHAR by token("[^$BACKSLASH]")

    private val escapeParser = ESCAPESEQUENCE use { escapeChars[text[1]]!! }

    override val rootParser =
            -QUOTE and zeroOrMore(escapeParser or CHAR.use { text.first() }) and -QUOTE map {
                chars -> String(chars.toCharArray())
            }
}