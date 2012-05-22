/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ageanalyzer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import syntaxchecker.syntaxerrors.GeneralAnomaly;

/**
 *
 * @author snadi
 */
public class CommonAnomalyFinder {
    
    private Vector<GeneralAnomaly> newDataSet;
    private Vector<GeneralAnomaly> originalDataSet;

    public static void main(String args[]){
        CommonAnomalyFinder finder = new CommonAnomalyFinder();
        finder.fillNewDataSet("errorDesc-27.csv");
        finder.fillOriginalDataSet("errorDesc-14.csv");


        finder.findCommonAnomalies();
        finder.dumpDataSets();
    }

    public CommonAnomalyFinder(){
        newDataSet = new Vector<GeneralAnomaly>();
        originalDataSet = new Vector<GeneralAnomaly>();
    }

    public void fillNewDataSet(String fileName){
         try {
            BufferedReader inputReader = new BufferedReader(new FileReader(fileName));

            while (inputReader.ready()) {
                String line = inputReader.readLine();
                String parts[] = line.split(",");
                GeneralAnomaly anomaly = new GeneralAnomaly(parts[1],parts[2],parts[3], parts[4],Integer.valueOf(parts[5]));
                anomaly.setId(Integer.valueOf(parts[0]));

                newDataSet.add(anomaly);
             }

            System.out.println("new sizE: " + newDataSet.size());

        }catch(IOException e){
            e.printStackTrace();

        }
    }

    public void fillOriginalDataSet(String fileName){
         try {
            BufferedReader inputReader = new BufferedReader(new FileReader(fileName));

            while (inputReader.ready()) {
                String line = inputReader.readLine();
                String parts[] = line.split(",");
                GeneralAnomaly anomaly = new GeneralAnomaly(parts[1],parts[2],parts[3], parts[4],Integer.valueOf(parts[5]));
                anomaly.setId(Integer.valueOf(parts[0]));

                originalDataSet.add(anomaly);
             }

            System.out.println("original sizE: " + originalDataSet.size());
        }catch(IOException e){
            e.printStackTrace();

        }
    }

    private void findCommonAnomalies() {
        for(GeneralAnomaly anomaly : originalDataSet){
            if(newDataSet.contains(anomaly)){
                int index = newDataSet.indexOf(anomaly);
                anomaly.setId(newDataSet.get(index).getId());
                newDataSet.get(index).setInSmallerDataSet(true);
            }
        }

        System.out.println("now new: " + newDataSet.size());
        System.out.println("now original: " + originalDataSet.size());
    }

    private void dumpDataSets(){
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(new FileOutputStream("OriginalDataSet-14rel.csv"));
            for (GeneralAnomaly anomaly : originalDataSet) {
                printWriter.println(anomaly.getId() + "," + anomaly.getType() + "," + anomaly.getStart() + "," + anomaly.getEnd() + "," + anomaly.getAge());
            }

            printWriter.close();

             printWriter = new PrintWriter(new FileOutputStream("NewDataSet-27rel.csv"));
            for (GeneralAnomaly anomaly : newDataSet) {
                printWriter.println(anomaly.getId() + "," + anomaly.getType() + "," + anomaly.getStart() + "," + anomaly.getEnd() + "," + anomaly.getAge() + "," + anomaly.isInSmallerDataSet());
            }

            printWriter.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CommonAnomalyFinder.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            printWriter.close();
        }
    }

}
