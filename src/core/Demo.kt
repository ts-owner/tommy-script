package core

import com.github.h0tk3y.betterParse.parser.*
import core.parser.TommyParser
import interpreter.interpretProgram
import java.io.File
import kotlin.system.measureNanoTime

fun main(args : Array<String>) {
    println("------ ACKERMANN ------")
    runFileTimed("examples/ackermann.tom")
    println()
    println("------ ARRAY2 ------")
    runFileTimed("examples/array2.tom")
    println()
    println("------ ARRAY DEMO ------")
    runFileTimed("examples/arraydemo.tom")
    println()
    println("------ FIBONACCI ------")
    runFileTimed("examples/fib.tom")
    println()
    println("------ GCD ------")
    runFileTimed("examples/gcd.tom")
    println()
    println("------ MERGE SORT ------")
    runFileTimed("examples/merge_sort.tom")
    println()
}

fun runFile(path : String) {
    val script = File(path)

    val parseResult = TommyParser(script.inputStream())

    interpretProgram(parseResult)
}

fun runFileTimed(path : String) {
    val script = File(path)
    var parseResult : ParseResult<List<Stmt>>? = null
    val timeToParse = measureNanoTime {
        parseResult = TommyParser.tryParseToEnd(TommyParser.tokenizer.tokenize(script.inputStream()))
    }
    val program = (parseResult as? Parsed)?.value ?: kotlin.run {
            println("Parsing failed in ${timeToParse.toDouble() * 1e-9} seconds.")
            println(parseResult)
            return@runFileTimed
        }
    println("Parsing completed in ${timeToParse.toDouble() * 1e-9} seconds\n")
    val timeToExecute = measureNanoTime {
        interpretProgram(program)
    }
    println("\n")
    println("Execution completed in ${timeToExecute.toDouble() * 1e-9} seconds")
}