/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package configs;

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
public class FeatureHierarchy {

    public static void main(String args[]) {

        BufferedReader reader = null;
        int notFound = 0;
        try {

            HashMap<String, Integer> hierarchyMap = createMap();
            File file = new File("onlyInMake.txt");
            reader = new BufferedReader(new FileReader(file));

            while (reader.ready()) {
                String line = reader.readLine();
                if(hierarchyMap.containsKey(line)){
                    System.out.println(line + "," + hierarchyMap.get(line));
                }else{
                    notFound++;
                }
            }

            System.out.println("Not found: " + notFound);
        }catch (FileNotFoundException e) {            
        }catch( IOException e){
            e.printStackTrace();            
        }
    }

    private static HashMap createMap() {
             BufferedReader reader = null;
        try {
            HashMap<String,Integer> hashMap = new HashMap<String,Integer>();

            File file = new File("hierarchy.csv");
            reader = new BufferedReader(new FileReader(file));

            while (reader.ready()) {
                String line = reader.readLine();
                String parts[] = line.split(",");
                hashMap.put(parts[0], Integer.parseInt(parts[1]));
            }

            return hashMap;
        }catch (FileNotFoundException e) {
            return null;
        }catch( IOException e){
            e.printStackTrace();
            return null;
        }
    }
}
