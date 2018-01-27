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
public class SymbolTable {
    private final ClassLevelSymbolTable classLevelSymbolTable;
    private final MethodLevelSymbolTable methodLevelSymbolTable;
    
    public SymbolTable(){
        classLevelSymbolTable = new ClassLevelSymbolTable();
        methodLevelSymbolTable = new MethodLevelSymbolTable();
    }
    
    public void startSubroutine(){
        methodLevelSymbolTable.clearSymbolTable();
    }
    
    public void define(String name, String type, KIND kind){
        switch(kind){
            case STATIC:
                classLevelSymbolTable.declareStatic(name, type);
                break;
            case FIELD:
                classLevelSymbolTable.declareField(name, type);
                break;
            case ARG:
                methodLevelSymbolTable.declareArg(name, type);
                break;
            case VAR:
                methodLevelSymbolTable.declareVar(name, type);
                break;
            default:
                throw new RuntimeException("Attempting to declare variale: " + name + " with no kind");
        }
    }
    
    public int varCount(KIND kind){
        switch(kind){
            case STATIC:
                return classLevelSymbolTable.getStaticCount();
            case FIELD:
                return classLevelSymbolTable.getFieldCount();
            case ARG:
                return methodLevelSymbolTable.getArgCount();
            case VAR:
                return methodLevelSymbolTable.getVarCount();
            default:
                throw new RuntimeException("Attempting to get variable count with kind: " + kind.toString());
        }
    }
    
    public KIND kindOf(String name){
        if(methodLevelSymbolTable.isDeclared(name)){
            return methodLevelSymbolTable.kindOf(name);
        }else if(classLevelSymbolTable.isDeclared(name)){
            return classLevelSymbolTable.kindOf(name);
        }else{
            return KIND.NONE;
        }
    }
    
    public String typeOf(String name){
        if(methodLevelSymbolTable.isDeclared(name)){
            return methodLevelSymbolTable.typeOf(name);
        }else if(classLevelSymbolTable.isDeclared(name)){
            return classLevelSymbolTable.typeOf(name);
        }else{
            throw new RuntimeException("Failed to get the type of Variable : " + name + "was not declared in current scope");
        }
    }
    
    public int indexOf(String name){
        if(methodLevelSymbolTable.isDeclared(name)){
            return methodLevelSymbolTable.indexOf(name);
        }else if(classLevelSymbolTable.isDeclared(name)){
            return classLevelSymbolTable.indexOf(name);
        }else{
            throw new RuntimeException("Failed to get the index of Variable : " + name + "was not declared in current scope");
        }
    }
    
    //test
    public static void main(String args[]){
        SymbolTable st = new SymbolTable();
        //test plan
        //add static
        st.define("st0", "St", KIND.STATIC);
        //add staic
        st.define("st1", "St", KIND.STATIC);
        
        //add field
        st.define("fld0", "Fld", KIND.FIELD);
        //add field
        st.define("fld1", "Fld", KIND.FIELD);
        //print stat and field count
        assert st.varCount(KIND.FIELD) == 2;
        assert st.varCount(KIND.STATIC) == 2;
        
        //start method
        st.methodLevelSymbolTable.clearSymbolTable();
        //add arg
        st.define("arg0", "Arg", KIND.ARG);
        //add arg
        st.define("arg1", "Arg", KIND.ARG);
        //add var
        st.define("fld1", "Var", KIND.VAR);
        //print arg and var count
        assert st.varCount(KIND.ARG) == 2;
        assert st.varCount(KIND.VAR) == 1;
        //index of
        assert st.indexOf("fld1") == 0;
        assert st.indexOf("arg1") == 1;
        
        //clear method
        st.startSubroutine();
        //print arg and var count
        assert st.varCount(KIND.ARG) == 0;
        assert st.varCount(KIND.VAR) == 0;
        //index of
        assert st.indexOf("fld1") == 1;
        
        //add arg
        st.define("arg0", "Arg", KIND.ARG);
        //add arg
        st.define("arg1", "Arg", KIND.ARG);
        //add var
        st.define("fld1", "Var", KIND.VAR);
        //print arg and var count
        assert st.varCount(KIND.ARG) == 2;
        assert st.varCount(KIND.VAR) == 1;
        //index of
        assert st.indexOf("fld1") == 0;
        assert st.indexOf("arg1") == 1;
        
        //print arg and var count
        assert st.typeOf("fld1").equals("Var");
        assert st.typeOf("st0").equals("St");
        assert st.typeOf("fld0").equals("Fld");
        
        //print stat and field count
        assert st.varCount(KIND.FIELD) == 2;
        assert st.varCount(KIND.STATIC) == 2;
    }
}
