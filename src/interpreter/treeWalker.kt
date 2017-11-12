package interpreter

import core.*

import java.lang.Math.pow


fun walkTree(debug : Boolean, tree : List<AST>) {
    if(debug) println("starting tree walk interpreter")
    runbody(tree, HashMap())
}

fun runbody(body: List<AST>, environment: HashMap<String, Tuple2<String?, Any>>) {
    //environment: variable name-> (type, value)
    //preserve hashmap for each one
    body.forEach { rec(it, environment) }

}
//TODO properly use typeError, handle double/int properly
//
 fun rec(curr: AST, environment: HashMap<String, Tuple2<String?, Any>>):Any {
        fun typeError(expr: Expr, what: String) {
            throw TypeCheckingException(wrongExpr = expr, msg=what)
        }
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
                        val result = rec(curr.cond, environment)
                        if (result is Boolean) {
                            var trueyet = false
                            if (result) {
                                //TODO handle environment properly
                                runbody(curr.thenBranch)
                            } else if (curr.elifs != null) {
                                curr.elifs.forEach { (a,b) ->
                                    var res = rec(a,environment)
                                    if(res is Boolean) {
                                        if (res) {
                                            runbody(b)
                                            trueyet = true
                                        }
                                    } else typeError(a, "should be bool")
                                }

                            } else if (curr.elseBranch != null && !trueyet) {
                                runbody(curr.elseBranch)
                            }
                        } else typeError(curr.cond, "should be bool")
                    }
                    is VarDef -> {
                        //TODO make it so you cant over-define things.
                        val res = rec(curr.rhs, environment)
                        environment[curr.lhs.id] = Tuple2("placeholder until we fix type parsing" as String?,res)
                    }
                    is UntypedVarDef -> {
                        val res = rec(curr.rhs, environment)
                        environment[curr.lhs.id] = Tuple2(null as String?, res)
                    }
                    is VarReassign -> {
                        //maybe assert that res is a value?
                        val res = rec(curr.rhs, environment)
                        val existingvar = environment[curr.lhs.id]!!
                        //TODO there needs to be some type checking here
                        environment[curr.lhs.id] = Tuple2("placeholder" as String?, res)
                    }
                    //TODO fundef, return, while, funcall, functions at all, closures, runbody
                }
            }
        }
    }
