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
class ClassLevelSymbolTable {

    private int staticCount;
    private int fieldCount;
    private final GenericSymbolTable symbolTable;

    ClassLevelSymbolTable() {
        staticCount = 0;
        fieldCount = 0;
        symbolTable = new GenericSymbolTable();
    }

    void declareStatic(String name, String type) {
        symbolTable.add(name, type, KIND.STATIC, staticCount);
        staticCount++;
    }

    void declareField(String name, String type) {
        symbolTable.add(name, type, KIND.FIELD, fieldCount);
        fieldCount++;
    }

    public int getStaticCount() {
        return staticCount;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public boolean isDeclared(String name) {
        return symbolTable.isDeclared(name);
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
    
    public void print(){
        System.out.println("<--- class level ---->");
        symbolTable.print();
        System.out.println("<--- class level ---->");
        System.out.println();
    }
}
