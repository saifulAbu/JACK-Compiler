/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package symboltable;

import java.util.HashMap;

/**
 *
 * @author sid
 */
class GenericSymbolTable {

    private final HashMap<String, SymbolTableEntry> symbolTable;

    GenericSymbolTable() {
        symbolTable = new HashMap<>();
    }

    public boolean isDeclared(String name) {
        return symbolTable.containsKey(name);
    }

    public KIND kindOf(String name) {
        return symbolTable.get(name).getKind();
    }

    public String typeOf(String name) {
        return symbolTable.get(name).getType();
    }

    public int indexOf(String name) {
        return symbolTable.get(name).getIndex();
    }
    
    public void clear(){
        symbolTable.clear();
    }

    void add(String name, String type, KIND kind, int index) {
        symbolTable.put(name, new SymbolTableEntry(type, kind, index));
    }
    
    void print(){
        for(String key : symbolTable.keySet()){
            SymbolTableEntry val = symbolTable.get(key);
            String output = String.format("%s | %s | %s | %d", key, val.getType(), val.getKind().toString().toLowerCase(), val.getIndex());
            System.out.println(output);
        }
    }
}
