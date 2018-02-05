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
public class TestApi {
    public static void main(String args[]){
        String space = " ";
        String t = "ab cd e f g";
        int p1 = t.indexOf('a', 0);
        String sub1 = t.substring(3, p1);
        System.out.println("done");
    }
}
