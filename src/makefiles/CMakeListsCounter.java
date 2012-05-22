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
import java.util.HashMap;

/**
 *
 * @author snadi
 */
public class CMakeListsCounter {

    private int conditionalLineCount;
    private HashMap<String,Integer> featureToFileCount;
    private static final String IF = "IF";
    private static final String ENDIF = "ENDIF ()";

    public CMakeListsCounter() {
        conditionalLineCount = 0;
        featureToFileCount = new HashMap<String,Integer>();
    }

    public static void main(String[] args) {
        CMakeListsCounter conditionalLineCounter = new CMakeListsCounter();
        conditionalLineCounter.count();        
        System.out.println("number of conditional lines in Makefiles:" + conditionalLineCounter.getConditionalCount());
        conditionalLineCounter.printMap();
    }

    private void printMap(){
        for(String feature: featureToFileCount.keySet()){
            System.out.println("Feature " + feature  + " includes " + featureToFileCount.get(feature) + " files");
        }
    }

    private int countInFile(String fileName){
        BufferedReader reader = null;
        int conditionalCount = 0;
        try {
            File file = new File(fileName);
            reader = new BufferedReader(new FileReader(file));

            while (reader.ready()) {
                String line = reader.readLine();


                if(line != null && line.trim().startsWith(IF + " (ENABLE_")){
                    String feature = line.substring(4, line.length() - 1);
                    int lineCountForFeature = 0;
                    do{
                        line = reader.readLine();

                        if(line != null && line.startsWith("LIST"))
                            continue;

                        //count non-empty lines which are not comments and are not endif
                        if(line != null && !line.trim().isEmpty() && !line.trim().startsWith("#") && !line.trim().equals(ENDIF))
                            lineCountForFeature++;
                        
                    }while(line != null && !line.trim().equals(ENDIF));
                    
                    conditionalCount += lineCountForFeature;

                    if(featureToFileCount.containsKey(feature)){
                        featureToFileCount.put(feature, featureToFileCount.get(feature) + lineCountForFeature);
                    }else{
                        featureToFileCount.put(feature, lineCountForFeature);
                    }

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
            File file = new File("cmakefiles.txt");
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
