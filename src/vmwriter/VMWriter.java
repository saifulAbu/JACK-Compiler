/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vmwriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 *
 * @author sid
 */
public class VMWriter {

    private final String PUSH = "push";
    private final String POP = "pop";
    private final String LABEL = "label";
    private final String GOT0 = "goto";
    private final String RETURN = "return";
    private final String IF_GOTO = "if-goto";
    private final String FUNCTION = "function";
    private final String CALL = "call";

    private final StringBuffer vmCode;
    private String fileName;
    private String folderName;
    
    public VMWriter(String fileName, String outputDirectory) {
        vmCode = new StringBuffer();
        this.fileName = fileName;
        this.folderName = outputDirectory;
    }

    private void appendNewLine() {
        vmCode.append("\n");
    }

    private void appendAsLowerCase(String str) {
        vmCode.append(str.toLowerCase()).append(" ");
    }
    
    private void appendStr(String str) {
        vmCode.append(str).append(" ");
    }

    private void append(int index) {
        vmCode.append(index).append(" ");
    }

    private void append(Segment segment) {
        appendAsLowerCase(segment.toString());
    }

    private void append(String pushPop, Segment segment, int index) {
        appendAsLowerCase(pushPop);
        append(segment);
        append(index);
        appendNewLine();
    }

    public void writePush(Segment segment, int index) {
        append(PUSH, segment, index);
    }

    public void writePop(Segment segment, int index) {
        append(POP, segment, index);
    }

    public void writeArithmetic(ArithmaticCommand cmd) {
        appendAsLowerCase(cmd.toString());
        appendNewLine();
    }

    private void appendLabel(String labelType, String labelName) {
        appendAsLowerCase(labelType);
        appendStr(labelName);
        appendNewLine();
    }

    public void writeLabel(String labelName) {
        appendLabel(LABEL, labelName);
    }

    public void writeGoto(String labelName) {
        appendLabel(GOT0, labelName);
    }

    public void writeIf(String labelName) {
        appendLabel(IF_GOTO, labelName);
    }

    private void appendFunction(String commandName, String name, int count) {
        appendAsLowerCase(commandName);
        appendStr(fileName + "." + name);
        append(count);
        appendNewLine();
    }

    public void writeCall(String name, int numArgs) {
        appendAsLowerCase(CALL);
        appendStr(name);
        append(numArgs);
        appendNewLine();
    }

    public void writeFunction(String name, int numLocals) {
        appendFunction(FUNCTION, name, numLocals);
    }

    public void writeReturn() {
        appendAsLowerCase(RETURN);
        appendNewLine();
    }

    public void close() {
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(folderName + "/" + fileName + ".vm"));
            writer.write(vmCode.toString());
            writer.flush();
            writer.close();
            System.out.println("Compilation Succesful for " + fileName);
        }catch(Exception ex){
        
        }
        
    }

    public static void main(String args[]) {
        VMWriter wr = new VMWriter("test", "src");
        wr.writeFunction("Main.fibonacci", 0);
        wr.writePush(Segment.ARGUMENT, 0);
        wr.writePush(Segment.CONSTANT, 2);
        wr.writeArithmetic(ArithmaticCommand.LT);

        wr.writeIf("IF_TRUE");
        wr.writeGoto("IF_FALSE");
        wr.writeLabel("IF_TRUE");
        wr.writePush(Segment.ARGUMENT, 0);
        wr.writeReturn();

        wr.writeLabel("IF_FALSE");
        wr.writePush(Segment.ARGUMENT, 0);
        wr.writePush(Segment.CONSTANT, 2);
        wr.writeArithmetic(ArithmaticCommand.SUB);

        wr.writeCall("Main.fibonacci", 1);  // computes fib(n-2)
        wr.writePush(Segment.ARGUMENT, 0);
        wr.writePush(Segment.CONSTANT, 1);
        wr.writeArithmetic(ArithmaticCommand.SUB);
        wr.writeCall("Main.fibonacci", 1);  // computes fib(n-1)
        wr.writeArithmetic(ArithmaticCommand.ADD);
        wr.writeReturn();
        
        wr.close();
    }

    public void print() {
        System.out.print(vmCode.toString());
    }
}
