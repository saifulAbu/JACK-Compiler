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
class SymbolTableEntry {

    private String type;
    private KIND kind;
    private int index;

    public SymbolTableEntry(String type, KIND kind, int index) {
        this.type = type;
        this.kind = kind;
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public KIND getKind() {
        return kind;
    }

    public int getIndex() {
        return index;
    }

}
