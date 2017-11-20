package interpreter

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import core.parser.TommyParser
import standard_library.stdLib

const val prelude: String = "ts> "
const val quit: String = "--quit"

fun main(args : Array<String>) {
    val environment = Scope(local = mutableMapOf())
    val functionDefs = mutableMapOf<String, Func>("print" to Print, "len" to Len, "str" to Str, "push" to Push)
    stdLib.forEach { exec(it, environment, functionDefs) }
    while(true) {
        print(prelude)
        val line = readLine()
        if(line == null || line == quit) break
        val prog = TommyParser().parseToEnd(line)[0]
        interp(prog, environment, functionDefs)
        println()
    }
}
