/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package symboltable;

/**
 *
 * @author sid
 */
class MethodLevelSymbolTable {

    private int argCount;
    private int varCount;
    private final GenericSymbolTable symbolTable;

    MethodLevelSymbolTable() {
        argCount = 0;
        varCount = 0;
        symbolTable = new GenericSymbolTable();
    }

    void declareArg(String name, String type) {
        symbolTable.add(name, type, KIND.ARG, argCount);
        argCount++;
    }

    void declareVar(String name, String type) {
        symbolTable.add(name, type, KIND.VAR, varCount);
        varCount++;
    }

    public int getArgCount() {
        return argCount;
    }

    public int getVarCount() {
        return varCount;
    }

    public KIND kindOf(String name) {
        return symbolTable.kindOf(name);
    }

    public String typeOf(String name) {
        return symbolTable.typeOf(name);
    }

    public int indexOf(String name) {
        return symbolTable.indexOf(name);
    }

    public void clearSymbolTable() {
        argCount = 0;
        varCount = 0;
        symbolTable.clear();
    }
    
    public boolean isDeclared(String name){
        return symbolTable.isDeclared(name);
    }

    void print() {
        System.out.println("#--- method level ----#");
        symbolTable.print();
        System.out.println("#--- end method level ---#");
        System.out.println();
    }
}
