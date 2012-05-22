/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ageanalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import syntaxchecker.Main;
import syntaxchecker.SyntaxChecker;
import syntaxchecker.syntaxerrors.FileMissingInMakeError;
import syntaxchecker.syntaxerrors.KConfigMissingError;
import syntaxchecker.syntaxerrors.ObjectMissingError;
import syntaxchecker.syntaxerrors.SyntaxError;

/**
 *
 * @author snadi
 */
public class AgeAnalyzer {

    private Vector<KernelRelease> releases;
    private Vector<SyntaxError> errors;

    public AgeAnalyzer() {
        releases = new Vector<KernelRelease>();
        errors = new Vector<SyntaxError>();
    }

    public static void main(String args[]) {
        AgeAnalyzer ageAnalyzer = new AgeAnalyzer();
        ageAnalyzer.analyze();
    }

    public void analyze() {
        File parentDirectory = new File("Releases");

        File[] files = parentDirectory.listFiles();

        for (File file : files) {
            if (!file.getName().startsWith(".") && file.getName().endsWith("log")) {
                readFile(file);
            }
        }

        analyzeErrors();

        printErrors();

    }

    private void printErrors() {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(new File("errorDesc-14.csv"));
            PrintWriter printWriter = new PrintWriter(fout);
            //printWriter.println("desc.,type,start, end, age");
            //id, desc,type, start, end, age
            int id = 501;
            for (SyntaxError error : errors) {

                if(error instanceof ObjectMissingError)
                    continue;
                
                if (error.getStartRelease().equals("2.6.12")) {
                    error.setStartRelease(null);
                }
                printWriter.println(id++ + "," + error.getMessage() + "," + error.getErrorCode() + "," + error.getStartRelease() + "," + error.getEndRelease() + "," +error.calculateAge());
            }
            printWriter.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AgeAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fout.close();
            } catch (IOException ex) {
                Logger.getLogger(AgeAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void analyzeErrors() {
        for (SyntaxError error : errors) {
            boolean markedStart = false;
            boolean markedEnd = false;
          
            //PROB: NOT IN RIGHT ORDER CAUSE OF HASHMAP BUT FOR TIME BEING, WILL TAKE ABSOLUTE VALUE OF AGE
            for (KernelRelease release : releases) {
                System.out.println("releasE: "+ release.getReleaseNumber());
                if (release.containsAnomaly(error)) {
                    if (!markedStart) {
                        error.setStartRelease(release.getReleaseNumber());
                        markedStart = true;
                    }
                } else {
                    //cannot find it in this release
                    if (markedStart && !markedEnd) {
                        error.setEndRelease(release.getReleaseNumber());
                        markedEnd = true;
                    }
                }
            }
        }

        findErrorReasons();
    }

    public boolean findInMake(String dirPath, String fileName) {
        try {            
            SyntaxChecker syntaxChecker = new SyntaxChecker(dirPath, Main.fillConfigEntries());
            return syntaxChecker.validateFile(fileName);
        } catch (Exception ex) {
            Logger.getLogger(AgeAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public void findErrorReasons() {
        
        for (SyntaxError error : errors) {
            String endRelease = error.getEndRelease();
            

            if(endRelease == null){
                continue;
            }
            int start = Integer.valueOf(error.getStartRelease().substring(error.getStartRelease().lastIndexOf(".") + 1));
            int end = Integer.valueOf(error.getEndRelease().substring(error.getEndRelease().lastIndexOf(".") + 1));

            String path = "../LinuxReleases/linux-" + endRelease + error.getDirectory().substring(1);
            if (error instanceof FileMissingInMakeError) {
                File file = new File(path + error.getFileName());

                if (!file.exists()) {                    
                    error.setReason(SyntaxError.REASON.removed_file);
                } else {
                    if (findInMake(path, error.getFileName())) {
                        error.setReason(SyntaxError.REASON.added_to_Makefile);
                    } else {
                        error.setReason(SyntaxError.REASON.added_to_C_file);
                    }
                }
            } else if (error instanceof KConfigMissingError) {
                KConfigMissingError kconfigError = (KConfigMissingError) error;
                String missingConfig = kconfigError.getKconfigDependency();
                KernelRelease release  = releases.get(end - 12);
                System.out.println("trying to get: " + error.getEndRelease() + " go: " + release.getReleaseNumber());
                if (release.containsConfig(missingConfig)) {
                    error.setReason(SyntaxError.REASON.added_defintion);
                } else {
                    File file = new File(path + error.getFileName());

                    if (!file.exists()) {                        
                        error.setReason(SyntaxError.REASON.removed_file);
                    }
                }
            }
        }
    }

    private boolean isStatistics(String line){
        return line.startsWith("Total") || line.startsWith("directories") || line.startsWith("Files");
    }

    private void readFile(File file) {
        try {            
            BufferedReader inputReader = new BufferedReader(new FileReader(file));
            String fileName = file.getName();
            fileName = fileName.substring(0, fileName.indexOf(".log"));
            KernelRelease kernelRelease = new KernelRelease(fileName);
//            kernelRelease.fillConfigEntries();

            while (inputReader.ready()) {
                String line = inputReader.readLine();

                if (!isStatistics(line)) {
                    SyntaxError error = getError(line);
                    kernelRelease.insertAnomaly(error);
                    if (!errors.contains(error)) {
                        errors.add(error);
                    }
                }
            }
            
            releases.add(kernelRelease);

        } catch (IOException ex) {
            Logger.getLogger(AgeAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private SyntaxError getError(String line) {
        SyntaxError error = null;
        String parts[] = line.split("::");
        String dir = parts[0].substring(4);

        if (parts[1].startsWith("FNU")) {
            String fileName = parts[1].substring(parts[1].indexOf(":") + 1);
            error = new FileMissingInMakeError(fileName, dir);
        } else if (parts[1].startsWith("FND")) {
            String feature = parts[1].substring(parts[1].indexOf(":") + 1, parts[1].lastIndexOf(":"));
            String fileName = parts[1].substring(parts[1].lastIndexOf(":") + 1);
            error = new KConfigMissingError(fileName, feature, dir);
        } else if (parts[1].startsWith("VNU")) {
            String variable = parts[1].substring(parts[1].indexOf(":") + 1, parts[1].lastIndexOf(":"));
            String fileName = parts[1].substring(parts[1].lastIndexOf(":") + 1);
            error = new ObjectMissingError(fileName, variable, dir);
        } else {
            System.out.println("ERROR: " + line);
        }

        return error;
    }
}
