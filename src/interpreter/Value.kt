package interpreter

sealed class Value
data class VInt(val value : Int) : Value()
data class VString(val value : String) : Value()
data class VBool(val value : Boolean) : Value()
data class VArray(val value : MutableList<Value>) : Value()
data class VFunction(val value : Func) : Value()
object VUnit : Value() {
    override fun toString() = "VUnit"
}