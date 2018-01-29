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

    public static void main(String[] args) throws IOException {
        CompilationEngine cengn = new CompilationEngine(new File("test.jack"));
        cengn.beginCompilation();
    }

    public CompilationEngine(File input) throws IOException {
        this.symbols = new HashSet<>(Arrays.asList('{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'));
        tokenizer = new JackTokenizer(input);
        vmWriter = new VMWriter();
        symbolTable = new SymbolTable();
    }

    public void beginCompilation() {
        compileClass();
        //System.out.println(content.toString());
    }

    //*** public tag creator methods ***
    public void compileClass() {
        String tokenName = "class";
        writeBegin(tokenName);

        keyword(KEYWORD.CLASS);
        thisClassName = className();
        symbol('{');
        classVarDec_zero_more();
        subroutineDec_zero_more();
        symbol('}');
        //delete this
        symbolTable.printClassLevelSymTable();
        writeEnd(tokenName);
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
            symbolTable.printMethodLevelSymTable();
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
        String tokenName = "subroutineDec";
        writeBegin(tokenName);

        //constructor, function, or method
        if (matchesKeyWord(KEYWORD.CONSTRUCTOR)) {
            keyword(KEYWORD.CONSTRUCTOR);
        } else if (matchesKeyWord(KEYWORD.FUNCTION)) {
            keyword(KEYWORD.FUNCTION);
        } else {
            keyword(KEYWORD.METHOD);
            //! add "this" to symbol table
            symbolTable.define(thisObj, thisClassName, KIND.ARG);
        }
        //void or type
        if (matchesKeyWord(KEYWORD.VOID)) {
            keyword(KEYWORD.VOID);
        } else {
            type();
        }
        subroutineName();
        symbol('(');
        compileParameterList();
        symbol(')');
        subroutineBody();

        writeEnd(tokenName);
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
        varName();

        //[expression] zero or one
        if (matchesSymbol('[')) {
            symbol('[');
            compileExpression();
            symbol(']');
        }

        symbol('=');
        compileExpression();
        symbol(';');

        writeEnd(tokenName);
    }

    public void compileIf() {
        String tokenName = "ifStatement";
        writeBegin(tokenName);

        keyword(KEYWORD.IF);
        symbol('(');
        compileExpression();
        symbol(')');

        symbol('{');
        compileStatements();
        symbol('}');

        //else { statements } zero or one
        if (matchesKeyWord(KEYWORD.ELSE)) {
            keyword(KEYWORD.ELSE);
            symbol('{');
            compileStatements();
            symbol('}');
        }

        writeEnd(tokenName);
    }

    public void compileWhile() {
        String tokenName = "whileStatement";
        writeBegin(tokenName);

        keyword(KEYWORD.WHILE);
        symbol('(');
        compileExpression();
        symbol(')');

        symbol('{');
        compileStatements();
        symbol('}');

        writeEnd(tokenName);
    }

    public void compileDo() {
        String tokenName = "doStatement";
        writeBegin(tokenName);

        keyword(KEYWORD.DO);
        subroutineCall();
        symbol(';');
        writeEnd(tokenName);
    }

    public void compileReturn() {
        String tokenName = "returnStatement";
        writeBegin(tokenName);

        keyword(KEYWORD.RETURN);
        if (matchesExpressionBegin()) {
            compileExpression();
        }
        symbol(';');

        writeEnd(tokenName);
    }

    public void compileExpression() {
        String tokenName = "expression";
        writeBegin(tokenName);

        compileTerm();
        op_term_zero_more();

        writeEnd(tokenName);
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
            unaryOp();
            compileTerm();
        } else {
            char lookAhead = lookAhead().charAt(0);
            switch (lookAhead) {
                case '(':
                case '.':
                    subroutineCall();
                    break;
                case '[':
                    varName();
                    symbol('[');
                    compileExpression();
                    symbol(']');
                    break;
                default:
                    varName();
                    break;
            }
        }

        writeEnd(tokenName);
    }

    public void compileExpressionList() {
        String tokenName = "expressionList";
        writeBegin(tokenName);
        if (matchesExpressionBegin()) {
            compileExpression();
            comma_expression_zero_more();
        }
        writeEnd(tokenName);
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

        content.append("<").append(tokenType).append(">");
        content.append(" ").append(tokenVal).append(" ");
        content.append("</").append(tokenType).append(">");
        appendLine();

        //System.out.print("<" + tokenType + ">");
        //System.out.print(" " + tokenVal + " ");
        //System.out.println("</" + tokenType + ">");
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

    private void symbol(char symbol) {
        checkTokenType(TOKEN_TYPE.SYMBOL);
        char curTok = tokenizer.symbol();
        if (curTok != symbol) {
            throw new RuntimeException("Expected symbol " + symbol + " found " + curTok);
        }
        writeToken("symbol", Character.toString(symbol));
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
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
        writeToken("integerConstant", Integer.toString(curTok));
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
    }

    private void stringConstant() {
        checkTokenType(TOKEN_TYPE.STRING_CONST);
        String curTok = tokenizer.stringVal();
        writeToken("stringConstant", curTok);
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
    }

    private void keywordConstant() {
        if (matchesKeyWord(KEYWORD.TRUE)) {
            keyword(KEYWORD.TRUE);
        } else if (matchesKeyWord(KEYWORD.FALSE)) {
            keyword(KEYWORD.FALSE);
        } else if (matchesKeyWord(KEYWORD.NULL)) {
            keyword(KEYWORD.NULL);
        } else {
            keyword(KEYWORD.THIS);
        }
    }

    private void op() {
        if (matchesSymbol('+')) {
            symbol('+');
        } else if (matchesSymbol('-')) {
            symbol('-');
        } else if (matchesSymbol('*')) {
            symbol('*');
        } else if (matchesSymbol('/')) {
            symbol('/');
        } else if (matchesSymbol('&')) {
            symbol('&');
        } else if (matchesSymbol('|')) {
            symbol('|');
        } else if (matchesSymbol('<')) {
            symbol('<');
        } else if (matchesSymbol('>')) {
            symbol('>');
        } else {
            symbol('=');
        }
    }

    private void unaryOp() {
        if (matchesSymbol('-')) {
            symbol('-');
        } else {
            symbol('~');
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

    private void subroutineName() {
        identifier();
    }

    private void subroutineBody() {
        String tokenName = "subroutineBody";
        writeBegin(tokenName);

        symbol('{');
        varDec_zero_more();
        compileStatements();
        symbol('}');

        writeEnd(tokenName);
    }

    private void subroutineCall() {
        char lookAhead = lookAhead().charAt(0);
        if (lookAhead == '(') {
            subroutineName();
            symbol('(');
            compileExpressionList();
            symbol(')');
        } else {
            if (matchesClassNameBegin()) {
                className();
            } else {
                varName();
            }
            symbol('.');
            subroutineName();
            symbol('(');
            compileExpressionList();
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

    private void varDec_zero_more() {
        while (matchesVarDecBegin()) {
            compileVarDec();
        }
    }

    private void op_term_zero_more() {
        while (matchesOp()) {
            op();
            compileTerm();
        }
    }

    private void statement_one_more() {
        do {
            statement();
        } while (matchesStatementBegin());
    }

    private void comma_expression_zero_more() {
        while (matchesSymbol(',')) {
            symbol(',');
            compileExpression();
        }
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
}
