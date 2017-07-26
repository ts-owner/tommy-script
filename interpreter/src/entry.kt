import java.io.File;
fun main(args: Array<String>) {
    var testscript = File("src/testscript.tom")
    if (testscript.isFile) {
        var lines = testscript.readLines()
    } else {
        println(testscript.exists())
    }

}