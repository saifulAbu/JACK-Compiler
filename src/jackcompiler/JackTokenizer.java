/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jackcompiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.io.FileUtils;

/**
 * @author sid
 */
public class JackTokenizer {

    private static String SPACE = " ";
    private char[] symbol = {'{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'};
    private int currentTokenIndex;
    private List<Token> tokens;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        JackTokenizer jtk = new JackTokenizer(new File("test.jack"));
        jtk.writeToFile();
    }

    public JackTokenizer(File inputFile) throws IOException {
        tokens = process(inputFile);
        currentTokenIndex = 0;
    }
    
    public boolean hasMoreTokens(){
        if(currentTokenIndex < (tokens.size() - 1)){
            return true;
        }else{
            return false;
        }
    }
    
    public void advance(){
        currentTokenIndex++;
        if(currentTokenIndex >= tokens.size()){
            throw new IllegalStateException("Advancing beyond number of tokens");
        }
    }
    
    public TOKEN_TYPE tokenType(){
        return tokens.get(currentTokenIndex).getTokenType();
    }

    public char symbol() {
        Token curToken = tokens.get(currentTokenIndex);
        if(curToken.getTokenType() != TOKEN_TYPE.SYMBOL){
            throw new ClassCastException("Current token is not of type: "+ TOKEN_TYPE.SYMBOL.toString());
        }
        return ((Symbol)(tokens.get(currentTokenIndex))).getSymbol();
    }

    public String identifier() {
        Token curToken = tokens.get(currentTokenIndex);
        if(curToken.getTokenType() != TOKEN_TYPE.IDENTIFIER){
            throw new ClassCastException("Current token is not of type: "+ TOKEN_TYPE.IDENTIFIER.toString());
        }
        return ((Identifier)(tokens.get(currentTokenIndex))).getIdentifier();
    }

    public int intVal() {
        Token curToken = tokens.get(currentTokenIndex);
        if(curToken.getTokenType() != TOKEN_TYPE.INT_CONST){
            throw new ClassCastException("Current token is not of type: "+ TOKEN_TYPE.INT_CONST.toString());
        }
        return ((IntVal)(tokens.get(currentTokenIndex))).getIntVal();
    }

    public String stringVal() {
        Token curToken = tokens.get(currentTokenIndex);
        if(curToken.getTokenType() != TOKEN_TYPE.STRING_CONST){
            throw new ClassCastException("Current token is not of type: "+ TOKEN_TYPE.STRING_CONST.toString());
        }
        return ((StringVal)(tokens.get(currentTokenIndex))).getStringVal();
    }
    
    /********************* Private Helper Methods And Classes *****************/

    private ArrayList<Token> process(File inputFile) throws IOException {
        //read all the content of the file
        String content = FileUtils.readFileToString(inputFile, Charset.defaultCharset());
        //remove comments
        content = replaceAllComments(content, SPACE);

        //insert a space before each symbol
        content = addBeforeAndAfter(content, symbol, SPACE);
        
        //replace multiple consecutive whitespaces with a single space
        //content = replaceAllConsqutiveWhiteSpace(content, SPACE);
        
        //split the string by space
        List<String> tokens = splitIgnoringBinder(content, '"');

        //create array list of size number of token
        ArrayList<Token> tokenlist = new ArrayList<>();

        //populate the tokens into token list
        for (String token : tokens) {
            tokenlist.add(getTokenWithString(token));
        }

        System.out.println("Tokenization done successfully");
        
        return tokenlist;
    }
    
    private List splitIgnoringBinder(String content, char binder){
        ArrayList<String> tokens = new ArrayList<>();
        int curIndex = 0;
        int docLen = content.length();
        while(curIndex < docLen){
            char curChar = content.charAt(curIndex);
            if(curChar == binder){
                int nextBinderIndex = content.indexOf(binder, curIndex+1);
                if(nextBinderIndex == -1){
                    throw new RuntimeException("provided content is not propery bounded with " + binder);
                }else{
                    String token = content.substring(curIndex, nextBinderIndex+1);
                    tokens.add(token);
                    curIndex = nextBinderIndex + 2; //eg. a = "my name is sa"
                }
            }else if(Character.isWhitespace(curChar)){
                curIndex++;
            }else{
                int nextSplitterIndex = nextWhiteSpace(content, curIndex+1);
                if(nextSplitterIndex == -1){
                    nextSplitterIndex = docLen;
                }
                String token = content.substring(curIndex, nextSplitterIndex);
                tokens.add(token);
                curIndex = nextSplitterIndex+1;
            }
        }
        return tokens;
    }
    
    private int nextWhiteSpace(String content, int startIndex){
        int curIndex = startIndex;
        while(curIndex < content.length()){
            if(Character.isWhitespace(content.charAt(curIndex))){
                return curIndex;
            }else{
                curIndex++;
            }
        }
        return -1;
    }

    private String replaceAllComments(String content, String replacer) {
        //remove all multiline comment
        String result = content.replaceAll("(?s)/\\*.*?\\*/", replacer);
        //remove single line comment
        result = result.replaceAll("\\/\\/.*\\n", replacer);
        return result;
    }

    private String replaceAllConsqutiveWhiteSpace(String content, String replacer) {
        return content.replaceAll("\\s{2,}", replacer).trim();
    }
    
    private String addBeforeAndAfter(String content, char[] symbolArr, String addString){
        HashSet<Character> symbols = new HashSet<>();
        for(char symbol : symbolArr){
            symbols.add(symbol);
        }
        StringBuffer result = new StringBuffer(content.length());
        for(char character : content.toCharArray()){
            if(symbols.contains(character)){
                result.append(addString + character + addString);
            }else{
                result.append(character);
            }
        }
        return result.toString();
    }
    
    private Token getTokenWithString(String token) {
        if (matchesKeyword(token)) {
            return new KeyWord(KEYWORD.valueOf(token.toUpperCase()));
        } else if (matchesSymbol(token)) {
            return new Symbol(token.charAt(0));
        } else if (matchesIntConst(token)) {
            return new IntVal(Integer.parseInt(token));
        } else if (matchesStringConst(token)) {
            return new StringVal(token.substring(1, token.length()-1));
        } else if (matchesIdentifier(token)) {
            return new Identifier(token);
        } else {
            throw new IllegalArgumentException("Unknown token " + token);
        }
    }

    private boolean matchesKeyword(String token) {
        final String regex = "^(class|constructor|function|method|field|static|var|int|char|boolean|void|true|false|null|this|let|do|if|else|while|return)$";
        return token.matches(regex);
    }

    private boolean matchesSymbol(String token) {
        final String regex = "^(\\{|\\}|\\(|\\)|\\[|\\]|\\.|,|;|\\+|\\-|\\*|\\/|&|\\||<|>|=|~)$";
        return token.matches(regex);
    }

    private boolean matchesIntConst(String token) {
        final String regex = "^0*([0-9]|[1-8][0-9]|9[0-9]|[1-8][0-9]{2}|9[0-8][0-9]|99[0-9]|[1-8][0-9]{3}|9[0-8][0-9]{2}|99[0-8][0-9]|999[0-9]|[12][0-9]{4}|3[01][0-9]{3}|32[0-6][0-9]{2}|327[0-5][0-9]|3276[0-7])$";
        return token.matches(regex);
    }

    private boolean matchesStringConst(String token) {
        final String regex = "^\"[^\"\\n]*\"$";
        return token.matches(regex);
    }

    private boolean matchesIdentifier(String token) {
        final String regex = "\\A[a-zA-Z_]+[a-zA-Z0-9_]*\\Z";
        boolean b = token.matches(regex);
        return token.matches(regex);
    }
    
    public void writeToFile(){
        StringBuilder content = new StringBuilder();
        content.append("<tokens>");
        appendLine(content);
        for(Token token : tokens){
            appendToken(content, token);
        }
        content.append("</tokens>");
        appendLine(content);
        System.out.println(content.toString());
    }
    
    private void appendToken(StringBuilder content, Token token){
        appendStartTokenType(content, token.getTokenType());
        appendTokenVal(content, token);
        appendEndTokenType(content, token.getTokenType());
        appendLine(content);
    }
    
    private void appendTokenVal(StringBuilder content, Token token) {
        String tokenVal = null;
        TOKEN_TYPE tokenType = token.getTokenType();
        if(tokenType == TOKEN_TYPE.IDENTIFIER){
            tokenVal = ((Identifier)token).getIdentifier();
        }else if(tokenType == TOKEN_TYPE.INT_CONST){
            tokenVal = Integer.toString(((IntVal)token).getIntVal());
        }else if(tokenType == TOKEN_TYPE.KEYWORD){
            tokenVal = ((KeyWord)token).getKeyword().toString().toLowerCase();
        }else if(tokenType == TOKEN_TYPE.STRING_CONST){
            tokenVal = ((StringVal)token).getStringVal();
        }else {
            tokenVal = Character.toString(((Symbol)token).getSymbol());
        }
        if(tokenVal == null){
            throw new RuntimeException("token does not contain value");
        }
        if(tokenVal.equals("<")){
            tokenVal = "&lt;";
        }else if(tokenVal.equals(">")){
            tokenVal = "&gt;";
        }else if(tokenVal.equals("&")){
            tokenVal = "&amp;";
        }
        content.append(" " + tokenVal + " ");
    }
    
    private void appendStartTokenType(StringBuilder content, TOKEN_TYPE tokenType){
        content.append("<" + getTokenTypString(tokenType) + ">");
    }
    
    private void appendEndTokenType(StringBuilder content, TOKEN_TYPE tokenType){
        content.append("</" + getTokenTypString(tokenType) + ">");
    }
    
    private String getTokenTypString(TOKEN_TYPE tokenType){
        String tokenTypeStr = tokenType.toString().toLowerCase();
        if(tokenTypeStr.equals("string_const")){
            tokenTypeStr = "stringConstant";
        }else if(tokenTypeStr.equals("int_const")){
            tokenTypeStr = "integerConstant";
        }
        return tokenTypeStr;
    }
    
    private void appendLine(StringBuilder content){
        content.append("\n");
    }

    /****************** private classes ************************/

    private class Token {

        private TOKEN_TYPE tokenType;

        Token(TOKEN_TYPE token_type) {
            this.tokenType = token_type;
        }

        public TOKEN_TYPE getTokenType() {
            return tokenType;
        }
    }

    private class KeyWord extends Token {

        KEYWORD keyword;

        public KeyWord(KEYWORD keyword) {
            super(TOKEN_TYPE.KEYWORD);
            this.keyword = keyword;
        }

        public KEYWORD getKeyword() {
            return keyword;
        }
    }

    private class Symbol extends Token {

        char symbol;

        public Symbol(char symbol) {
            super(TOKEN_TYPE.SYMBOL);
            this.symbol = symbol;
        }

        public char getSymbol() {
            return symbol;
        }
    }

    private class Identifier extends Token {

        String id;

        public Identifier(String id) {
            super(TOKEN_TYPE.IDENTIFIER);
            this.id = id;
        }

        public String getIdentifier() {
            return id;
        }
    }

    private class IntVal extends Token {

        int intVal;

        public IntVal(int intVal) {
            super(TOKEN_TYPE.INT_CONST);
            this.intVal = intVal;
        }

        public int getIntVal() {
            return intVal;
        }
    }

    private class StringVal extends Token {

        String stringVal;

        public StringVal(String stringVal) {
            super(TOKEN_TYPE.STRING_CONST);
            this.stringVal = stringVal;
        }

        public String getStringVal() {
            return stringVal;
        }
    }
}
