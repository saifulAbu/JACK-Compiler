/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jackcompiler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import symboltable.KIND;
import symboltable.SymbolTable;
import vmwriter.ArithmaticCommand;
import vmwriter.Segment;
import vmwriter.VMWriter;

/**
 *
 * @author sid
 */
public class CompilationEngine {

    private final JackTokenizer tokenizer;
    private final Set<Character> symbols;
    private final StringBuilder content = new StringBuilder();
    private final VMWriter vmWriter;
    private final SymbolTable symbolTable;
    private String thisClassName;
    private final String thisObj = "this";
    private final LabelGenerator labelGen; 

    private final String STRING_NEW = "String.new";
    private final int STRING_NEW_N_ARGS = 1;

    public static void main(String[] args) throws IOException {
        CompilationEngine cengn = new CompilationEngine(new File("Main.jack"));
        cengn.beginCompilation();
        cengn.vmWriter.print();
    }

    public CompilationEngine(File input) throws IOException {
        this.symbols = new HashSet<>(Arrays.asList('{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'));
        tokenizer = new JackTokenizer(input);
        vmWriter = new VMWriter(getFileNameWithoutExtension(input));
        symbolTable = new SymbolTable();
        thisClassName = getFileNameWithoutExtension(input);
        labelGen = new LabelGenerator();
    }

    public void beginCompilation() {
        compileClass();
    }

    //*** public tag creator methods ***
    public void compileClass() {
        keyword(KEYWORD.CLASS);
        className();
        symbol('{');
        classVarDec_zero_more();
        subroutineDec_zero_more();
        symbol('}');
        //delete this
        //symbolTable.printClassLevelSymTable();
    }

    private void classVarDec_zero_more() {
        while (matchesClassVarDecBegin()) {
            compileClassVarDec();
        }
    }

    private void subroutineDec_zero_more() {
        while (matchesSubroutineDecBegin()) {
            symbolTable.startSubroutine();
            compileSubroutine();
            //symbolTable.printMethodLevelSymTable();
        }
    }

    public void compileClassVarDec() {
        final String type;
        final KIND kind;
        String varName;

        String tokenName = "classVarDec";
        writeBegin(tokenName);

        //static or field
        if (matchesKeyWord(KEYWORD.STATIC)) {
            keyword(KEYWORD.STATIC);
            kind = KIND.STATIC;
        } else {
            keyword(KEYWORD.FIELD);
            kind = KIND.FIELD;
        }
        type = type();
        varName = varName();
        symbolTable.define(varName, type, kind);

        //comma varname zero or more
        char comma = ',';
        while (matchesSymbol(comma)) {
            symbol(comma);
            varName = varName();
            symbolTable.define(varName, type, kind);
        }

        symbol(';');

        writeEnd(tokenName);
    }

    public void compileSubroutine() {
        KEYWORD subRoutineType;
        boolean isVoid = false;
        //constructor, function, or method
        if (matchesKeyWord(KEYWORD.CONSTRUCTOR)) {
            keyword(KEYWORD.CONSTRUCTOR);
            subRoutineType = KEYWORD.CONSTRUCTOR;
        } else if (matchesKeyWord(KEYWORD.FUNCTION)) {
            keyword(KEYWORD.FUNCTION);
            subRoutineType = KEYWORD.FUNCTION;
        } else {
            keyword(KEYWORD.METHOD);
            symbolTable.define(thisObj, thisClassName, KIND.ARG);
            subRoutineType = KEYWORD.METHOD;
        }
        //void or type
        if (matchesKeyWord(KEYWORD.VOID)) {
            keyword(KEYWORD.VOID);
            isVoid = true;
        } else {
            type();
        }

        //parameters
        String subroutineName = subroutineName();
        symbol('(');
        compileParameterList();
        symbol(')');

        //subroutineBody
        symbol('{');
        int varDecCount = varDec_zero_more();
        vmWriter.writeFunction(subroutineName, varDecCount);
        if (subRoutineType == KEYWORD.CONSTRUCTOR) {
            //allocate memory with number of field count and set this pointer (pointer 0)
            vmWriter.writePush(Segment.CONSTANT, symbolTable.varCount(KIND.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(Segment.POINTER, 0);
        } else if (subRoutineType == KEYWORD.METHOD) {
            //argument 0 contains reference to the object the method is called upon
            //push object's reference
            vmWriter.writePush(Segment.ARGUMENT, 0);//push argument 0
            //set this pointer
            vmWriter.writePop(Segment.POINTER, 0);//pop pointer 0
        }
        compileStatements();
        symbol('}');
    }

    public void compileParameterList() {
        String tokenName = "parameterList";
        writeBegin(tokenName);
        if (matchesTypeBegin()) {
            String type = type();
            String varName = varName();
            symbolTable.define(varName, type, KIND.ARG);
            //comma_type_varName_zero_more();
            char comma = ',';
            while (matchesSymbol(comma)) {
                symbol(comma);
                type = type();
                varName = varName();
                symbolTable.define(varName, type, KIND.ARG);
            }
        }
        writeEnd(tokenName);
    }

    public void compileVarDec() {
        final KIND kind = KIND.VAR;
        final String type;
        String varName;

        String tokenName = "varDec";
        writeBegin(tokenName);

        keyword(KEYWORD.VAR);
        type = type();
        varName = varName();
        symbolTable.define(varName, type, kind);

        //comma_varName_zero_more();
        char comma = ',';
        while (matchesSymbol(comma)) {
            symbol(comma);
            varName = varName();
            symbolTable.define(varName, type, kind);
        }

        symbol(';');

        writeEnd(tokenName);
    }

    public void compileStatements() {
        //statement zero or more
        if (matchesStatementBegin()) {
            String tokenName = "statements";
            writeBegin(tokenName);
            statement_one_more();
            writeEnd(tokenName);
        }
    }

    private void statement() {
        if (matchesKeyWord(KEYWORD.LET)) {
            compileLet();
        } else if (matchesKeyWord(KEYWORD.IF)) {
            compileIf();
        } else if (matchesKeyWord(KEYWORD.WHILE)) {
            compileWhile();
        } else if (matchesKeyWord(KEYWORD.DO)) {
            compileDo();
        } else {
            compileReturn();
        }
    }

    public void compileLet() {
        String tokenName = "letStatement";
        writeBegin(tokenName);

        keyword(KEYWORD.LET);
        String varName = varName();
        boolean isArray = false;
        //[expression] zero or one
        if (matchesSymbol('[')) {
            isArray = true;

            symbol('[');
            //pushes array offset to stack
            compileExpression();

            //push array base address to stack
            pushVariable(varName);

            //compute array index address, the result would be on stack
            vmWriter.writeArithmetic(ArithmaticCommand.ADD);
            symbol(']');
        }

        symbol('=');
        compileExpression();

        if (isArray) {
            popResultToArrayIndex();
        } else{
            popResultToVariable(varName);
        }

        symbol(';');

        writeEnd(tokenName);
    }

    private void pushVariable(String varName) {
        int index = symbolTable.indexOf(varName);
        switch (symbolTable.kindOf(varName)) {
            case STATIC:
                vmWriter.writePush(Segment.STATIC, index);
                break;
            case FIELD:
                vmWriter.writePush(Segment.THIS, index);
                break;
            case ARG:
                vmWriter.writePush(Segment.ARGUMENT, index);
                break;
            case VAR:
                vmWriter.writePush(Segment.LOCAL, index);
                break;
            default:
                throw new RuntimeException("A variable should have a kind of static, field, arg or var");
        };
    }

    private void popResultToArrayIndex() {
        vmWriter.writePop(Segment.TEMP, 0); //pop temp 0; pops right side value to temp
        vmWriter.writePop(Segment.POINTER, 1);//pop pointer 1; pops array index address to that pointer
        vmWriter.writePush(Segment.TEMP, 0);//push temp 0; pushes right side value to stack
        vmWriter.writePop(Segment.THAT, 0);//pop that 0; puts the result to desired address
    }
    
    private void popResultToVariable(String varName){
        int index = symbolTable.indexOf(varName);
        switch (symbolTable.kindOf(varName)) {
            case STATIC:
                vmWriter.writePop(Segment.STATIC, index);
                break;
            case FIELD:
                vmWriter.writePop(Segment.THIS, index);
                break;
            case ARG:
                vmWriter.writePop(Segment.ARGUMENT, index);
                break;
            case VAR:
                vmWriter.writePop(Segment.LOCAL, index);
                break;
            default:
                throw new RuntimeException("An variable should have a kind of static, field, arg or var");
        };
    }

    public void compileIf() {
        labelGen.prepareIfLabel();
        
        keyword(KEYWORD.IF);
        symbol('(');
        compileExpression();
        symbol(')');
        
        writeIfBegin();

        symbol('{');
        compileStatements();
        symbol('}');

        //else { statements } zero or one
        if (matchesKeyWord(KEYWORD.ELSE)) {
            keyword(KEYWORD.ELSE);
            
            writeElseBegin();
            
            symbol('{');
            compileStatements();
            symbol('}');
            
            vmWriter.writeLabel(labelGen.getIfEnd());
            
        }else{
            vmWriter.writeLabel(labelGen.getIfFalse());
        }
    }
    
    private void writeIfBegin(){
        String ifTrue = labelGen.getIfTrue();
        String ifFalse = labelGen.getIfFalse();
        
        vmWriter.writeIf(ifTrue);
        vmWriter.writeGoto(ifFalse);
        vmWriter.writeLabel(ifTrue);
    }
    
    private void writeElseBegin(){
        String ifFalse = labelGen.getIfFalse();
        String ifEnd = labelGen.getIfEnd();
        
        vmWriter.writeGoto(ifEnd);
        vmWriter.writeLabel(ifFalse);
    }

    public void compileWhile() {
        labelGen.prepareWhileLabel();
        String beginLabel = labelGen.getWhileBegin();
        String endLabel = labelGen.getWhileEnd();
        
        keyword(KEYWORD.WHILE);
        vmWriter.writeLabel(beginLabel);
        
        symbol('(');
        compileExpression();
        symbol(')');
        
        vmWriter.writeArithmetic(ArithmaticCommand.NOT);
        vmWriter.writeIf(endLabel);

        symbol('{');
        compileStatements();
        symbol('}');
        
        vmWriter.writeGoto(beginLabel);
        vmWriter.writeLabel(endLabel);
    }

    public void compileDo() {
        
        keyword(KEYWORD.DO);
        subroutineCall();
        symbol(';');
        
        //discard the result from the called function as it is not being assigned
        vmWriter.writePop(Segment.TEMP, 0);//pop temp 0; discard the result
    }

    public void compileReturn() {
        String tokenName = "returnStatement";
        writeBegin(tokenName);

        keyword(KEYWORD.RETURN);
        if (matchesExpressionBegin()) {
            compileExpression();
        } else {
            //this is a void function, so return 0 instead
            vmWriter.writePush(Segment.CONSTANT, 0);//push constant 0
        }
        symbol(';');
        vmWriter.writeReturn();
    }

    public void compileExpression() {
        String tokenName = "expression";
        writeBegin(tokenName);

        compileTerm();
        op_term_zero_more();
    }

    public void compileTerm() {
        String tokenName = "term";
        writeBegin(tokenName);

        if (matchesIntegerConstant()) {
            integerConstant();
        } else if (matchesStringConstant()) {
            stringConstant();
        } else if (matchesKeywordConstant()) {
            keywordConstant();
        } else if (matchesSymbol('(')) {
            symbol('(');
            compileExpression();
            symbol(')');
        } else if (matchesUnaryOp()) {
            char op = unaryOp();
            compileTerm();
            if (op == '-') {
                vmWriter.writeArithmetic(ArithmaticCommand.NEG);
            } else {
                vmWriter.writeArithmetic(ArithmaticCommand.NOT);
            }
        } else {
            char lookAhead = lookAhead().charAt(0);
            String varName = null;
            switch (lookAhead) {
                case '(':
                case '.':
                    subroutineCall();
                    break;
                case '[':
                    varName = varName();
                    symbol('[');
                    compileExpression();
                    //push the value from arr[index] to stack
                    pushVariable(varName);
                    vmWriter.writeArithmetic(ArithmaticCommand.ADD);
                    vmWriter.writePop(Segment.POINTER, 1);
                    vmWriter.writePush(Segment.THAT, 0);
                    
                    symbol(']');
                    break;
                default:
                    varName = varName();
                    pushVariable(varName);
                    break;
            }
        }

    }
    
    public int compileExpressionList() {
        int expressionCount = 0;
        if (matchesExpressionBegin()) {
            compileExpression();
            expressionCount = comma_expression_zero_more() + 1;
        }
        return expressionCount;
    }

    //*** token writer helpers ***/
    private void appendLine() {
        content.append("\n");
    }

    private void writeBegin(String tokenName) {
        content.append("<").append(tokenName).append(">");
        appendLine();
        //System.out.println("<" + tokenName + ">");
    }

    private void writeEnd(String tokenName) {
        content.append("</").append(tokenName).append(">");
        appendLine();
        //System.out.println("</" + tokenName + ">");
    }

    private void writeToken(String tokenType, String tokenVal) {
        if (tokenVal.equals("<")) {
            tokenVal = "&lt;";
        } else if (tokenVal.equals(">")) {
            tokenVal = "&gt;";
        } else if (tokenVal.equals("&")) {
            tokenVal = "&amp;";
        }
    }

    //*** tokenizer related helper ***/
    private String lookAhead() {
        return tokenizer.lookAhead();
    }

    //*** private compile helper methods ***/
    private void checkTokenType(TOKEN_TYPE tokType) {
        if (tokenizer.tokenType() != tokType) {
            throw new RuntimeException("Token type " + tokType + " did not match with actual found type " + tokenizer.tokenType());
        }
    }

    private String keyword(jackcompiler.KEYWORD keyword) {
        checkTokenType(TOKEN_TYPE.KEYWORD);
        KEYWORD curTok = tokenizer.keyWord();
        if (curTok != keyword) {
            throw new RuntimeException("Expected keyword " + keyword.toString() + " found " + curTok);
        }
        writeToken("keyword", keyword.toString().toLowerCase());
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
        return keyword.toString().toLowerCase();
    }

    private char symbol(char symbol) {
        checkTokenType(TOKEN_TYPE.SYMBOL);
        char curTok = tokenizer.symbol();
        if (curTok != symbol) {
            throw new RuntimeException("Expected symbol " + symbol + " found " + curTok);
        }

        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
        return curTok;
    }

    private String identifier() {
        checkTokenType(TOKEN_TYPE.IDENTIFIER);
        String curTok = tokenizer.identifier();
        writeToken("identifier", curTok);
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
        return curTok;
    }

    private void integerConstant() {
        checkTokenType(TOKEN_TYPE.INT_CONST);
        int curTok = tokenizer.intVal();
        vmWriter.writePush(Segment.CONSTANT, curTok);
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
    }

    private void stringConstant() {
        checkTokenType(TOKEN_TYPE.STRING_CONST);
        String curTok = tokenizer.stringVal();

        vmWriter.writePush(Segment.CONSTANT, curTok.length());
        vmWriter.writeCall("String.new", 1);
        for (char ch : curTok.toCharArray()) {
            vmWriter.writePush(Segment.CONSTANT, (int) ch);
            vmWriter.writeCall("String.appendChar", 2);
        }

        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
    }

    private void keywordConstant() {
        if (matchesKeyWord(KEYWORD.TRUE)) {
            keyword(KEYWORD.TRUE);
            vmWriter.writePush(Segment.CONSTANT, 1);
            vmWriter.writeArithmetic(ArithmaticCommand.NEG);
        } else if (matchesKeyWord(KEYWORD.FALSE)) {
            keyword(KEYWORD.FALSE);
            vmWriter.writePush(Segment.CONSTANT, 0);
        } else if (matchesKeyWord(KEYWORD.NULL)) {
            keyword(KEYWORD.NULL);
            vmWriter.writePush(Segment.CONSTANT, 0);
        } else {
            keyword(KEYWORD.THIS);
            vmWriter.writePush(Segment.POINTER, 0);
        }
    }

    private char op() {
        if (matchesSymbol('+')) {
            return symbol('+');
        } else if (matchesSymbol('-')) {
            return symbol('-');
        } else if (matchesSymbol('*')) {
            return symbol('*');
        } else if (matchesSymbol('/')) {
            return symbol('/');
        } else if (matchesSymbol('&')) {
            return symbol('&');
        } else if (matchesSymbol('|')) {
            return symbol('|');
        } else if (matchesSymbol('<')) {
            return symbol('<');
        } else if (matchesSymbol('>')) {
            return symbol('>');
        } else {
            return symbol('=');
        }
    }

    private char unaryOp() {
        if (matchesSymbol('-')) {
            return symbol('-');
        } else {
            return symbol('~');
        }
    }

    private String type() {
        if (matchesKeyWord(KEYWORD.INT)) {
            return keyword(KEYWORD.INT);
        } else if (matchesKeyWord(KEYWORD.CHAR)) {
            return keyword(KEYWORD.CHAR);
        } else if (matchesKeyWord(KEYWORD.BOOLEAN)) {
            return keyword(KEYWORD.BOOLEAN);
        } else {
            return className();
        }
    }

    private String varName() {
        return identifier();
    }

    private String className() {
        return identifier();
    }

    private String subroutineName() {
        return identifier();
    }

    private void subroutineBody() {
        String tokenName = "subroutineBody";
        writeBegin(tokenName);

        symbol('{');
        int varDecCount = varDec_zero_more();
        //!write function 
        vmWriter.writeFunction(tokenName, varDecCount);
        compileStatements();
        symbol('}');

        writeEnd(tokenName);
    }

    private void subroutineCall() {
        char lookAhead = lookAhead().charAt(0);
        if (lookAhead == '(') {
            String subroutineName = subroutineName();
            symbol('(');
            //push this 
            vmWriter.writePush(Segment.POINTER, 0);//push pointer 0
            int argCount = compileExpressionList();
            vmWriter.writeCall(thisClassName + "." + subroutineName, argCount+1);
            symbol(')');
        } else {
            boolean isClass = false;
            String className = null;
            int argCount = 0;
            String identifier = identifier();
            //the identifier name could not be found on symbol table, this indicates it is a class name
            if(symbolTable.kindOf(identifier) == KIND.NONE){
                isClass = true;
                className = identifier;
            }
            
            if(!isClass){
                argCount = 1;
                className = symbolTable.typeOf(identifier);
                pushVariable(identifier);
            }
            
            symbol('.');
            String subroutineName = subroutineName();
            symbol('(');
            argCount += compileExpressionList();
            vmWriter.writeCall(className + "." + subroutineName, argCount);
            symbol(')');
        }
    }

    //*** wild card occurance helpers ***/
    private void comma_varName_zero_more() {
        char comma = ',';
        while (matchesSymbol(comma)) {
            symbol(comma);
            varName();
        }
    }

    private void comma_type_varName_zero_more() {
        char comma = ',';
        while (matchesSymbol(comma)) {
            symbol(comma);
            type();
            varName();
        }
    }

    private int varDec_zero_more() {
        while (matchesVarDecBegin()) {
            compileVarDec();
        }
        return symbolTable.varCount(KIND.VAR);
    }

    private void op_term_zero_more() {
        while (matchesOp()) {
            char op = op();
            compileTerm();

            switch (op) {
                case '+':
                    vmWriter.writeArithmetic(ArithmaticCommand.ADD);
                    break;
                case '-':
                    vmWriter.writeArithmetic(ArithmaticCommand.SUB);
                    break;
                case '*':
                    vmWriter.writeCall("Math.multiply", 2);
                    break;
                case '/':
                    vmWriter.writeCall("Math.divide", 2);
                    break;
                case '&':
                    vmWriter.writeArithmetic(ArithmaticCommand.AND);
                    break;
                case '|':
                    vmWriter.writeArithmetic(ArithmaticCommand.OR);
                    break;
                case '<':
                    vmWriter.writeArithmetic(ArithmaticCommand.LT);
                    break;
                case '>':
                    vmWriter.writeArithmetic(ArithmaticCommand.GT);
                    break;
                default:
                    vmWriter.writeArithmetic(ArithmaticCommand.EQ);
            }

        }
    }

    private void statement_one_more() {
        do {
            statement();
        } while (matchesStatementBegin());
    }

    private int comma_expression_zero_more() {
        int expressionCount = 0;
        while (matchesSymbol(',')) {
            symbol(',');
            compileExpression();
            expressionCount++;
        }
        return expressionCount;
    }

    //*** matches helper ***/
    private boolean matchesKeyWord(jackcompiler.KEYWORD keyword) {
        return (tokenizer.tokenType() == TOKEN_TYPE.KEYWORD) && tokenizer.keyWord() == keyword;
    }

    private boolean matchesSymbol(char symbol) {
        return ((tokenizer.tokenType() == TOKEN_TYPE.SYMBOL) && tokenizer.symbol() == symbol);
    }

    private boolean matchesIdentifier() {
        return tokenizer.tokenType() == TOKEN_TYPE.IDENTIFIER;
    }

    private boolean matchesIntegerConstant() {
        return tokenizer.tokenType() == TOKEN_TYPE.INT_CONST;
    }

    private boolean matchesStringConstant() {
        return tokenizer.tokenType() == TOKEN_TYPE.STRING_CONST;
    }

    private boolean matchesKeywordConstant() {
        return (matchesKeyWord(KEYWORD.TRUE)
                || matchesKeyWord(KEYWORD.FALSE)
                || matchesKeyWord(KEYWORD.NULL)
                || matchesKeyWord(KEYWORD.THIS));
    }

    private boolean matchesUnaryOp() {
        return (matchesSymbol('-')
                || matchesSymbol('~'));
    }

    private boolean matchesTypeBegin() {
        return (matchesKeyWord(KEYWORD.INT)
                || matchesKeyWord(KEYWORD.CHAR)
                || matchesKeyWord(KEYWORD.BOOLEAN)
                || matchesClassNameBegin());
    }

    private boolean matchesClassNameBegin() {
        return matchesIdentifier();
    }

    private boolean matchesClassVarDecBegin() {
        return (matchesKeyWord(KEYWORD.STATIC)
                || matchesKeyWord(KEYWORD.FIELD));
    }

    private boolean matchesSubroutineDecBegin() {
        return (matchesKeyWord(KEYWORD.CONSTRUCTOR)
                || matchesKeyWord(KEYWORD.FUNCTION)
                || matchesKeyWord(KEYWORD.METHOD));
    }

    private boolean matchesVarDecBegin() {
        return matchesKeyWord(KEYWORD.VAR);
    }

    private boolean matchesStatementBegin() {
        return (matchesKeyWord(KEYWORD.LET)
                || matchesKeyWord(KEYWORD.IF)
                || matchesKeyWord(KEYWORD.WHILE)
                || matchesKeyWord(KEYWORD.DO)
                || matchesKeyWord(KEYWORD.RETURN));
    }

    private boolean matchesExpressionBegin() {
        return (matchesIntegerConstant()
                || matchesStringConstant()
                || matchesKeywordConstant()
                || matchesIdentifier()
                ||//varname, subroutine call 
                matchesSymbol('(')
                || matchesUnaryOp());
    }

    private boolean matchesOp() {
        return (matchesSymbol('+')
                || matchesSymbol('-')
                || matchesSymbol('*')
                || matchesSymbol('/')
                || matchesSymbol('&')
                || matchesSymbol('|')
                || matchesSymbol('<')
                || matchesSymbol('>')
                || matchesSymbol('='));
    }

    private String getFileNameWithoutExtension(File file) {
        return file.getName().replaceFirst("[.][^.]+$", "");
    }
}
