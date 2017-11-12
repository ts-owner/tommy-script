package interpreter

import core.*
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode
import kotlin.test.assertTrue


fun walkTree(debug : Boolean, tree : List<AST>) {
    if(debug) println("starting tree walk interpreter")
    fun rec(curr: AST, environment: Map<String, Any>):Any {
        when(curr) {
            is Literal -> {
                when(curr) {
                    is LInt -> return curr.value
                    is LString -> return curr.value
                    is LBool -> return curr.value
                }
            }
            is Expression -> {
               when(curr) {
                   is Var -> return environment[curr.id]!!
                   is Prefix -> {
                       when(curr.op.type.cod) {
                           is TBool -> {
                              //this is a boolean thing, only operator is NOT
                              when (curr.op) {
                                  PreOp.not -> {
                                      //if curr is boolean
                                      val exp = rec(curr.expr, environment)
                                      if (exp is Boolean) {
                                          return  !exp
                                      } else {
                                          //type error
                                      }
                                  }
                              }
                           }
                           is TInt -> {
                              //negate or plus
                               when(curr.op) {
                                   PreOp.negate -> {
                                       //make sure this is number
                                       val exp = rec(curr.expr, environment)
                                       if (exp is Int) return -exp
                                       if (exp is Double) return -exp
                                   }
                                   PreOp.plus -> rec(curr.expr, environment)
                               }
                           }
                           is TString -> {
                               //no preops for string
                           }
                       }
                   }
                   is Infix -> {
                       val left = rec(curr.lhs, environment)
                       val right = rec(curr.rhs, environment)
                       when(curr.op.type.cod) {
                           is TBool -> {
                               //make sure/assert that left and right are booleans
                               left as Boolean
                               right as Boolean
                               when(curr.op) {
                                   InOp.and -> {
                                       return left && right
                                   }
                                   InOp.or -> {
                                       return left || right
                                   }
                               }
                           }
                           is TInt -> {
                               left as Number
                               right as Number
                               when(curr.op) {
                                   InOp.lt -> {
                                       return left > right
                                   }
                                   InOp.gt -> {
                                       return left < right
                                   }
                               }
                           }
                       }
                   }
                }
            }
            is Statement -> {
                when(curr) {
                    is If -> {
                        //make sure it is boolean, maybe
                        //TODO after bools are implemented
                        rec(curr.cond, environment)
                    }
                    is

                }
            }
        }
    }
}