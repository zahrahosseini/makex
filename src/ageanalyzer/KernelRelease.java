/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ageanalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import syntaxchecker.syntaxerrors.SyntaxError;

/**
 *
 * @author snadi
 */
public class KernelRelease implements Comparator{

    private String releaseNumber;
    private Vector<SyntaxError> anomalies;
    private Vector<String> definedConfigs;

    public String getReleaseNumber() {
        return releaseNumber;
    }

    public void setReleaseNumber(String releaseNumber) {
        this.releaseNumber = releaseNumber;
    }

    public KernelRelease(String releaseNumber) {
        this.releaseNumber = releaseNumber;
        anomalies = new Vector<SyntaxError>();
        definedConfigs = new Vector<String>();
    }

    public boolean containsConfig(String config){
        return definedConfigs.contains(config);
    }

      public void fillConfigEntries() {
        BufferedReader inputReader = null;
        BufferedReader kconfigReader = null;
        String path = "../LinuxReleases/linux-" + releaseNumber + "/";

        try {
            File file = new File(path + "archNames.txt");
            //   System.out.println("Reading from: " +file.getAbsolutePath());
            inputReader = new BufferedReader(new FileReader(file));

            while (inputReader.ready()) {
                String arch = inputReader.readLine().trim();

                kconfigReader = new BufferedReader(new FileReader(path + "KConfigFiles/" + arch + "-kconfig.txt"));


                while (kconfigReader.ready()) {
                    String line = kconfigReader.readLine();



                    if (Pattern.matches("config\\s+.*", line.trim()) || line.trim().startsWith("menuconfig")) {
                        String parts[] = line.trim().split("\\s");

                        int count = 1;
                        while (count < parts.length) {

                            if (parts[count].trim().length() > 0) {
                                break;
                            }

                            count++;
                        }

                        String configName = parts[count].trim();

                        if (!definedConfigs.contains(configName)) {
                            definedConfigs.add(configName);
                        }
                    }
                }

                kconfigReader.close();

            }



            //parse the common directories first

        } catch (IOException ex) {
            Logger.getLogger(KernelRelease.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                inputReader.close();
            } catch (IOException ex) {
                Logger.getLogger(KernelRelease.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void insertAnomaly(SyntaxError error){
        anomalies.add(error);
    }

    public boolean containsAnomaly(SyntaxError error){
        return anomalies.contains(error);
    }

    public int compare(Object o1, Object o2) {
       KernelRelease release1 = (KernelRelease) o1;
       KernelRelease release2 = (KernelRelease) o2;

       return release1.releaseNumber.compareTo(release2.releaseNumber);

    }
}
