/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilationengine;

/**
 *
 * @author sid
 */
class LabelGenerator {
    private int ifCount = -1;
    private int whileCount = -1;
    
    public void prepareIfLabel(){
        ifCount++;
    }
    
    public String getIfTrue(){
        return "IF_TRUE" + ifCount;
    }
    
    public String getIfFalse(){
        return "IF_FALSE" + ifCount;
    }
    
    public String getIfEnd(){
        return "IF_END" + ifCount;
    }
    
    public void prepareWhileLabel(){
        whileCount++;
    }
    
    public String getWhileBegin(){
        return "WHILE_EXP" + whileCount;
    }
    
    public String getWhileEnd(){
        return "WHILE_END" + whileCount;
    }
}
