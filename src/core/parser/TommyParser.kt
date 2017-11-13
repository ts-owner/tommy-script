package core.parser

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.*
import core.*

class TommyParser : Grammar<List<AST>>() {
    //Symbols
    private val LPAR by token("\\(")
    private val RPAR by token("\\)")
    private val LBRA by token("\\[")
    private val RBRA by token("\\]")
    private val EQU by token("==")
    private val NEQ by token("!=")
    private val LEQ by token("<=")
    private val GEQ by token(">=")
    private val LT by token("<")
    private val GT by token(">")
    private val COLON by token(":")
    private val LET by token("let\\b")
    private val EQUALS by token("=")
    private val CONCAT by token("\\+\\+")
    private val PLUS by token("\\+")
    private val MINUS by token("\\-")
    private val DIV by token("/")
    private val MOD by token("%")
    private val POW by token("\\*\\*")
    private val TIMES by token("\\*")
    private val OR by token("or")
    private val AND by token("and\\b")
    private val COMMA by token(",")

    //keywords
    private val NOT by token("not\\b") //@Brendan maybe not should require a space after/before it?
    private val RETURN by token("return\\b")
    private val END by token("end")
    private val IF by token("if\\b")
    private val THEN by token("then\\b")
    private val ELSE by token("else\\b")
    private val ELSEIF by token("elseif\\b")
    private val WHILE by token("while\\b")
    private val FOR by token("for\\b")
    private val DO by token("\\bdo")
    private val IN by token("in\\b")

    //Literals
    private val NUM by token("\\d+")
    private val STRING by token("\".*\"")
    private val TRUE by token("true")
    private val FALSE by token("false")

    //types
    private val stringSymbol by token("String")
    private val intSymbol by token("Int")
    private val boolSymbol by token("Bool")
    private val unitSymbol by token("Unit")

    private val id by token("\\w+")

    private val ws by token("\\s+", ignore = true)

    //LEXER OVER

    private val idParser = id use { text }

    private val arrayTypeParser = -LBRA and parser(this::typeParser) and -RBRA map { TArray }

    //Parses a string to a TString or an int to a TInt or a boolean to a TBool
    private val typeParser : Parser<Type> = stringSymbol.asJust(TString) or
            intSymbol.asJust(TInt) or
            boolSymbol.asJust(TBool) or
            unitSymbol.asJust(TUnit) or
            arrayTypeParser

    //Parses things like escape characters
    private val stringParser = STRING.map { match -> StringParser().tryParseToEnd(match.text) }
                                     .join().map(::LString)

    private val numParser = NUM use { LInt(text.toInt()) } //Parses numbers into LInts
    private val trueParser = TRUE.asJust(LBool(true)) //Parses "true" into a boolean (LBool) `true`
    private val falseParser = FALSE.asJust(LBool(false)) //Parses "false" into a boolean (LBool) `false`
    //TODO make this expr eventually

    private val preLiteral = stringParser or numParser or trueParser or falseParser
    private val literalParser = preLiteral

    //Any amount of non-white-space followed by a ( and any number of parameters (`acceptZero = true` means it can be no parameters) and a )
    //Maps the parameters (`args`) and function name (`name.text`) to a function call
    private val funCallParser = id and -LPAR and
                                separatedTerms(parser(this::expr), COMMA, acceptZero = true) and
                                -RPAR map { (name, args) -> FunCall(name.text, args) }

    //Maps any non-white-space to a variable
    private val varParser = idParser.map(::Var)

    private val arrayLiteralParser = -LBRA and separatedTerms(parser(this::preLiteral), COMMA, acceptZero = true) and -RBRA map { LArray(it.toMutableList()) }

    private val arrayGetParser :Parser<Expr> = varParser and -LBRA and parser(this::expr) and -RBRA map { (name, index) -> ArrayAccess(name, index) }

    //switch out preexper thing with parser(this::expr) later, if it works with preexpr
    private val preexpr = literalParser or funCallParser or arrayGetParser or varParser or arrayLiteralParser or
                          (-LPAR and parser(this::expr) and -RPAR)

    //TODO move this somewhere else
    private val whileParser = -WHILE and parser(this::expr) and
                                           -DO and zeroOrMore(parser(this::astParser)) and
                                           -END map{(cond, statement) -> While(cond, statement)}
    private val forParser = -FOR and idParser and -IN and parser(this::expr) and
            -DO and zeroOrMore(parser(this::astParser)) and
            -END map{(elem, list, body) -> For(elem, list, body)}

    //operators zone
    //The operators with the highest number in the operator chain happen first. Eg: power function > plus/minus
    //TODO make this not seizure material

    //make the levels general. maybe need to pull request/add something
    //make it so there can't be a space between

    //POWER FUNCTION (**)
    val lvFourteenOperatorChain: Parser<Expr> = leftAssociative(preexpr, POW) { l, o, r ->
        // use o for generalization
        Infix(InOp.power, l, r)
    }

    //PLUS AND MINUS INVERT EACH OTHER
    val lvThirteenOperatorChain: Parser<Expr> = (PLUS or MINUS) * lvFourteenOperatorChain map { (a, b) ->
        Prefix(if (a.type == PLUS) PreOp.plus else PreOp.negate, b)
    }

    //MODULUS (%)
    var lvTwelveOperatorChain: Parser<Expr> = leftAssociative(lvThirteenOperatorChain or lvFourteenOperatorChain, MOD) { l, o, r ->
        Infix(InOp.mod, l, r)
    }

    //give alternative path around prefix operator
    //MULTIPLICATION AND DIVISION (* and /)
    val lvElevenOperatorChain: Parser<Expr> = leftAssociative(lvThirteenOperatorChain or lvTwelveOperatorChain, (DIV or TIMES)) { l, o, r ->
        Infix(if (o.type == DIV) InOp.div else InOp.times, l, r)
    }

    //PLUS AND MINUS (+ and -)
    val lvTenOperatorChain: Parser<Expr> = leftAssociative(lvElevenOperatorChain, (PLUS or MINUS)) { l, o, r ->
        Infix(if (o.type == PLUS) InOp.plus else InOp.negate, l, r)
    }

    //Defining characters as their actual functions (the characters being == and != and <= and >= and < and > )
    val lvNineOperatorSymbols = EQU or NEQ or LEQ or GEQ or LT or GT
    private val tokenToOperator = mapOf(
            EQU to InOp.eqInt, NEQ to InOp.neq, LEQ to InOp.leq, GEQ to InOp.geq, LT to InOp.lt, GT to InOp.gt
    )
    //COMPARER TOKENS ( == and != and <= and >= and < and > )
    val lvNineOperatorChain: Parser<Expr> = leftAssociative(lvTenOperatorChain, lvNineOperatorSymbols) { l, o, r ->
        Infix(tokenToOperator[o.type]!!, l, r)
    }

    //NOT
    val lvSixOperatorChain: Parser<Expr> = NOT * lvNineOperatorChain map { (a, b) ->
        Prefix(PreOp.not, b)
    }
    //give alternative path around prefix operator
    //AND
    val lvFiveOperatorChain: Parser<Expr> = leftAssociative(lvSixOperatorChain or lvNineOperatorChain, AND) { l, _, r ->
        Infix(InOp.and, l, r);
    }

    //OR
    val lvFourOperatorChain: Parser<Expr> = leftAssociative(lvFiveOperatorChain, OR) { l, _, r ->
        Infix(InOp.or, l, r);
    }

    //CONCAT
    val lvThreeOperatorChain: Parser<Expr> = leftAssociative(lvFourOperatorChain, CONCAT) { l, _, r ->
        Infix(InOp.concat, l, r);
    }


    //The chain works by calling lvFourOperatorChain, which in turn calls lvFiveOperatorChain and then does its thing.
    // But lvFiveOperatorChain calls lvSixOperatorChain, etc.
    private val expr = lvThreeOperatorChain
    //end operators zone

    private val arraySetParser = varParser and -LBRA and parser(this::expr) and -RBRA and -EQUALS and
            parser(this::expr) map { (name, index, newObj) -> ArrayAssignment(name, index, newObj)}

    //Types a variable
    //x: String
    //x: Bool
    //x: Int
    private val annotatedVarParser = idParser and -COLON and typeParser map { (a, b) ->
        AnnotatedVar(a, b)
    }

    //Creates a variable with a type
    //let x: String = "test"
    //let x: Boolean = true
    //let x: Int = 3
    private val varDefParser = -LET and annotatedVarParser and -EQUALS and expr map { (a, b) ->
        VarDef(a, b)
    }

    //Creates a new variable without a type
    //let x = "test"
    //let x = false
    //let x = 3
    private val untypedVarDefParser = -LET and varParser and -EQUALS and expr map { (a, b) ->
        UntypedVarDef(a, b)
    }

    //Reassigns a variable to a new value
    //x = "test2"
    private val varReassignParser = varParser and -EQUALS and expr map { (a, b) ->
        VarReassign(a, b)
    }

    //Declares a function
    //let multiply (a: Int, b: Int):Int =
        //return (a * b)
        //end
    //let printWord (a: String):Unit =
        //print (a)
        //end
    private val funDefParser = -LET and idParser and
            -LPAR and separatedTerms(annotatedVarParser, COMMA, acceptZero = true) and -RPAR and
            -COLON and typeParser and -EQUALS and
            zeroOrMore(parser(this::astParser)) and
            -END map { (funName, args, retType, children) ->
        FunDef(funName, args, retType, children)
    }

    //Returns something
    //return (a * b)
    private val returnParser = -RETURN and expr.map(::Return)

    //An if (and optional else)
    //if (a == 3) then
        //return "yes"
    //else
        //return "no"
    //end
    val ifParser = -IF and expr and -THEN and zeroOrMore(parser(this::astParser)) and
             zeroOrMore(-ELSEIF and expr and -THEN and zeroOrMore(parser(this::astParser))) and optional(-ELSE and zeroOrMore(parser(this::astParser))) and -END map { (cond, body, elifs, elsebody) ->
        If(cond, body, elsebody, elifs)
    }

    //A statement is either return or a typed variable declaration or an untyped variable declaration or a function or reassigning a variable or an if
    //return (a * b)
    private val statement : Parser<Statement> = returnParser or varDefParser or untypedVarDefParser or funDefParser or
            varReassignParser or ifParser or whileParser or forParser or arraySetParser

            //An ast is an expression or a statement
    private val astParser = statement or expr //order matters here for assignment!
    //The root of the program is one or more asts (one or more expressions/statements)
    override val rootParser = oneOrMore(astParser) //TODO make this correct
}