package standard_library

import core.*

// eventually this will be written in tommy script directly
// but we need to use TAny and Brendan hasn't made the type system good

private fun tprint(message : Expr) = EvalExpr(FunCall(Var("print"), listOf(message)))
private fun tpush(arr : Expr, elem : Expr) = EvalExpr(FunCall(Var("push"), listOf(arr, elem)))

private val defPrintln = FunDef(Var("println"), listOf(AnnotatedVar("message", TAny)), TUnit, listOf(
        tprint(Var("message")),
        tprint(LString("\n"))
))

private val defCons = FunDef(Var("cons"), listOf(AnnotatedVar("list", TArray), AnnotatedVar("elem", TAny)), TArray, listOf(
        VarDef(AnnotatedVar("newArr", TArray), LArray(mutableListOf())),
        For(Var("x"), Var("list"), listOf(tpush(Var("newArr"), Var("x")))),
        tpush(Var("newArr"), Var("elem")),
        Return(Var("newArr"))
))

private val defConcat =  FunDef(Var("concat"), listOf(AnnotatedVar("lhs", TArray), AnnotatedVar("rhs", TArray)), TArray, listOf(
        VarDef(AnnotatedVar("newArr", TArray), LArray(mutableListOf())),
        For(Var("x"), Var("lhs"), listOf(tpush(Var("newArr"), Var("x")))),
        For(Var("x"), Var("rhs"), listOf(tpush(Var("newArr"), Var("x")))),
        Return(Var("newArr"))
))

val stdLib = listOf<Statement>(defPrintln, defCons, defConcat)