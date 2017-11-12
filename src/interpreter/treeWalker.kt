package interpreter

import core.*
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode
import java.lang.Math.pow
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
                               left as Int
                               right as Int

                               when(curr.op) {
                                   InOp.lt -> {
                                       var bool = true

                                       if(left > right)
                                           bool = true

                                       return bool
                                   }
                                   InOp.gt -> {
                                       return left < right
                                   }
                                   InOp.eqInt -> {
                                       return left == right
                                   }
                                   InOp.plus ->{
                                       return left + right
                                   }
                                   InOp.negate -> { //FLAGGED, ASK ABOUT THIS
                                       return left - right
                                   }
                                   InOp.mod -> {
                                       var result = left.rem(right)

                                      return result
                                   }
                                   InOp.times -> {
                                       return left.times(right)
                                   }
                                   InOp.div -> {
                                       return left/right
                                   }
                                   InOp.power -> { //FLAGGED, MAYBE ASK ABOUT THIS
                                       var power: Int = right
                                       var result = 1

                                       while(power > 0) {
                                           result *= left
                                           power -= 1
                                       }

                                       return pow( left as Double, right as Double)
                                   }
                                   InOp.leq -> {
                                       return left <= right
                                   }
                                   InOp.geq -> {
                                       return left >= right
                                   }
                                   InOp.neq -> {
                                       return left != right
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
                    //is

                }
            }
        }
    }
}