/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jackcompiler;

import compilationengine.CompilationEngine;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import org.apache.commons.io.filefilter.FileFileFilter;

/**
 * Entry point for the jack compiler
 * @author sid
 */
public class Main {
    public static void main(String[] args) throws IOException {
        String inputpath = "/home/sid/Desktop/Bucket-List/1.build-a-computer/JACKCompiler/jacksrc";
        File inputFile = new File(inputpath);
        if(inputFile.isFile() && inputFile.getName().endsWith(".jack")){
            CompilationEngine cengn = new CompilationEngine(inputFile, inputFile.getParent());
            cengn.run();
        }
        
        if(inputFile.isDirectory()){
            String jackSrc[] =  inputFile.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jack");
                }
            });
            for(String jackSrcFile : jackSrc){
                CompilationEngine cengn = new CompilationEngine(new File(inputpath + "/" + jackSrcFile), inputpath);
                cengn.run();
            }
        }
        
    }
}
