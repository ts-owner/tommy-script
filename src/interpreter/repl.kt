package interpreter

import java.io.InputStreamReader
import java.util.*

const val prelude: String = "ts> ";
const val quit: String = "--quit";

fun main(){
    var input: Scanner = Scanner(System.`in`)

    var eof = true
    var line: String = ""
    var depth: Int = 0
    while (input.hasNext()){
        line = input.nextLine()
        // why no eof :(
        if(line == quit){
            break
        } else if (line == "po") {
            depth += 1
            System.out.printf("    ".repeat(depth));
        }
        System.out.print(evaluate(line))
    }

}

fun evaluate(something:String): String {
    var output:String = "hey"
    return output
}

