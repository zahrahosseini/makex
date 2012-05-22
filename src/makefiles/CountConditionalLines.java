/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package makefiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author snadi
 */
public class CountConditionalLines {

    private int conditionalLineCount;
    private static final String IFDEF = "ifdef";
    private static final String IFEQ = "ifeq";
    private static final String IFNEQ = "ifneq";
    private static final String ENDIF = "endif";

    public CountConditionalLines() {
        conditionalLineCount = 0;
    }

    public static void main(String[] args) {
        CountConditionalLines conditionalLineCounter = new CountConditionalLines();
        conditionalLineCounter.count();
        System.out.println("number of conditional lines in Makefiles:" + conditionalLineCounter.getConditionalCount());
    }

    private int countInFile(String fileName){
        BufferedReader reader = null;
        int conditionalCount = 0;
        try {
            File file = new File(fileName);
            reader = new BufferedReader(new FileReader(file));

            while (reader.ready()) {
                String line = reader.readLine();

                if(line.trim().startsWith(IFDEF) || line.trim().startsWith("ifeq ($(CONFIG_") || line.trim().startsWith("ifneq ($(CONFIG_")){
                    do{
                        line = reader.readLine();

                        //count non-empty lines which are not comments and are not endif
                        if(!line.trim().isEmpty() && !line.trim().startsWith("#") && !line.trim().equals(ENDIF))
                            conditionalCount++;
                        
                    }while(!line.trim().equals(ENDIF));
                }
            }

            System.out.println("File: " + fileName + " has conditional lines: " + conditionalCount);
            return conditionalCount;
        }catch (FileNotFoundException e) { 
            return 0;
        }catch( IOException e){
            e.printStackTrace();
            return -1;
        }
    }

    private void count() {
        BufferedReader reader = null;
        try {
            File file = new File("makefileNames.txt");
            reader = new BufferedReader(new FileReader(file));

            while (reader.ready()) {
                String fileName = reader.readLine().trim();
                conditionalLineCount += countInFile(fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getConditionalCount() {
        return conditionalLineCount;
    }
}
