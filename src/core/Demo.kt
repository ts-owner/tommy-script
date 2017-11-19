package core

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import core.parser.TommyParser
import interpreter.interpretProgram
import java.io.File

fun main(args: Array<String>) {
    println("------ ACKERMANN ------")
    runFile("examples/ackermann.tom")
    println()
    println("------ ARRAY2 ------")
    runFile("examples/array2.tom")
    println()
    println("------ ARRAY DEMO ------")
    runFile("examples/arraydemo.tom")
    println()
    println("------ FIBONACCI ------")
    runFile("examples/fib.tom")
    println()
    println("------ GCD ------")
    runFile("examples/gcd.tom")
    println()
    println("------ MERGE SORT ------")
    runFile("examples/merge_sort.tom")
    println()
}

fun runFile(path : String) {
    val script = File(path)

    val parseResult = TommyParser().parseToEnd(script.inputStream())

    interpretProgram(parseResult)
}