package interpreter

import core.AST


fun walkTree(debug : Boolean, tree : List<AST>) {
    if(debug) println("starting tree walk interpreter")
    tree.forEach {
        println(it)
    }
}