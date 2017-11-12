package interpreter

import com.github.h0tk3y.betterParse.utils.Tuple2
import core.*
import java.lang.Math.pow


fun walkTree(debug : Boolean, tree : List<AST>) {
    if(debug) println("starting tree walk interpreter")
    runbody(tree, mutableMapOf())
}

fun runbody(body: List<AST>, environment: MutableMap<String, Tuple2<String?, Any>>):Any {
    //environment: variable name-> (type, value)
    //preserve hashmap for each one
    body.forEach {
        val ret = rec(it, environment)
        if (ret is ReturnBox) return ret.content
    }
    return Unit
}
//TODO properly use typeError, handle double/int properly
//
 fun rec(curr: AST, environment: MutableMap<String, Tuple2<String?, Any>>):Any {
        fun typeError(expr: Expr, what: String) {
            throw TypeCheckingException(wrongExpr = expr, msg=what)
        }
        fun nameError(what: String) {
            throw Exception(what) //TODO also when you try to define two of same function
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
                   is Var -> return environment[curr.id]!!.t2
                   is FunCall -> {
                       //TODO put std lib in other file
                       //std lib
                       if(curr.id=="print") println(rec(curr.args.first(),environment))

                       val storedData = environment[curr.id]
                       if (storedData != null) {
                           when (storedData.t2) {
                               is Tuple2<*,*> -> {
                                   //(args, statements)
                                   val storedTypeFunction=
                                           storedData as Tuple2<String?, Tuple2<List<AnnotatedVar>, Body>>
                                   val storedFunction = storedData.t2
                                   //if (matchArgs(ffun.t1, curr.args))
                                   if(storedFunction.t1.size == curr.args.size) {
                                       //set arguments in environment
                                       var new_environment = environment.toMap() as MutableMap
                                       //TODO not sure if ANYTHING from new environment persist ever
                                       storedFunction.t1.zip(curr.args) { a : AnnotatedVar, b: Expr ->
                                           val ret = rec(b,environment)
                                           if (ret is ReturnBox) return ret.content
                                           new_environment[a.id] = Tuple2("placeholder" as String?, ret)
                                       }
                                       // other function body, new environment including function args
                                       try {
                                           val result = runbody(storedFunction.t2, new_environment)
                                       } catch (e: ReturnBoxHackException) {
                                           return e.returnValue
                                       }
                                   } else nameError("arg count mismatch")

                               }
                               else -> nameError("${curr.id} is not a function, but its a variable")
                           }
                       }
                   }
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
                                          typeError(curr.expr, "should be bool")
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
                       //TODO make this less gross
                       when(curr.op.type.dom[0]) {
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
                               try {
                                   left as Int
                                   right as Int
                               } catch (e: ClassCastException) {
                                   throw ClassCastException("type mismatch $left and $right should be ints")
                               }

                               when(curr.op) {
                                   InOp.lt -> {
                                       return right > left
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
                                   InOp.power -> { //TODO deal with double/int
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
                                return runbody(curr.thenBranch, environment)
                            }
                            if (curr.elifs != null) {
                                curr.elifs.forEach { (a, b) ->
                                    var res = rec(a, environment)
                                    if (res is Boolean) {
                                        if (res) {
                                            return runbody(b, environment)
                                        }
                                    } else typeError(a, "should be bool")
                                }
                            }
                            if (curr.elseBranch != null) {
                                return runbody(curr.elseBranch, environment)
                            }
                        } else typeError(curr.cond, "should be bool")
                    }
                    is VarDef -> {
                        //TODO make it so you cant over-define things.
                        val res = rec(curr.rhs, environment)
                        environment[curr.lhs.id] = Tuple2(curr.lhs.ty.toString() as String?, res)
                    }
                    is UntypedVarDef -> {
                        val res = rec(curr.rhs, environment)
                        environment[curr.lhs.id] = Tuple2(null as String?, res)
                    }
                    is VarReassign -> {
                        //maybe assert that res is a value?
                        val res = rec(curr.rhs, environment)
                        val existingType = environment[curr.lhs.id]?.t1
                        //TODO there needs to be some type checking here
                        environment[curr.lhs.id] = Tuple2(existingType, res)
                    }
                    is FunDef -> {
                        environment[curr.id] = Tuple2(curr.returnType.toString() as String?, Tuple2(curr.args, curr.statements) as Any)
                    }
                    is Return -> {
                        val ret =(rec(curr.toReturn,environment))
                        throw ReturnBoxHackException(returnValue = ret)
                    }
                    is While -> {
                        //TODO fix cast sloppiness
                        while(rec(curr.cond,environment) as Boolean) {
                            var ret = runbody(curr.body, environment)
                            if (ret is ReturnBox) return ret.content
                        }
                    }
                //TODO while, closures
                }
            }
        }
    return Unit
}

data class ReturnBox(val content: Any)

class ReturnBoxHackException(val returnValue: Any , cause : Exception? = null)
    : RuntimeException("not error", cause) {
    override fun toString() = super.toString()
}
