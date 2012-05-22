/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ageanalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import syntaxchecker.SyntaxChecker;

/**
 *
 * @author snadi
 */
public class ReleaseKconfig {

    private String releaseName;
    private Vector<String> configEntries;

    public ReleaseKconfig(String releaseName) {
        this.releaseName = releaseName;
        configEntries = new Vector<String>();
    }

    public void fillConfigEntries() {
        BufferedReader inputReader = null;
        BufferedReader kconfigReader = null;
        String path = "../LinuxReleases/linux-" + releaseName + "/";

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

                        if (!configEntries.contains(configName)) {
                            configEntries.add(configName);
                        }
                    }
                }

                kconfigReader.close();

            }



            //parse the common directories first

        } catch (IOException ex) {
            Logger.getLogger(SyntaxChecker.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                inputReader.close();
            } catch (IOException ex) {
                Logger.getLogger(SyntaxChecker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
