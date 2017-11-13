package interpreter

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import core.*
import core.parser.TommyParser
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFails

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
    val lines = try {
        script.readLines()
    } catch (e: FileNotFoundException) {
        throw IllegalArgumentException("${script.absolutePath}: No such file or directory")
    }

    var parseResult = TommyParser().parseToEnd(script.inputStream())

    walkTree(false, parseResult)
}