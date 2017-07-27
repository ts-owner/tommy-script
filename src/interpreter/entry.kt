import java.io.File

fun main(args: Array<String>) {
    val testScript : File = File("src/testscript.tom")

    if (testScript.isFile) {
        testScript.forEachLine { println(it) }
    } else {
        println(testScript.exists())
    }
}