/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package makefiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author snadi
 */
public class MakeFileParser {

    private int totalNumOfFiles;
    private int filesWithDep;
    private PrintWriter outputFile;
    private String arch;
    private boolean printStatistics;
    //constants
    private static final String HEAD = "head-";
    private static final String INIT = "init-";
    private static final String CORE = "core-";
    private static final String LIBS = "libs-";
    private static final String DRIVERS = "drivers-";
    private static final String NET = "net-";
    private static final String OBJ = "obj-";
    private static final String PLAT = "plat-";
    private static final String MACHINE = "machine-";
    private static final String ARCH = "arch-";
    private static final String IFEQ = "ifeq";
    private static final String IFNEQ = "ifneq";
    private static final String ENDIF = "endif";
    private static final String IFDEF = "ifdef";
    private static final String ELSE = "else";
    //for statistics
    private static Vector<String> allFiles;
    private static Vector<String> conditionedFiles;
    private static Vector<String> parsedMakeFiles;
    private static Vector<String> parsedDirectories;
    private static Vector<String> conditionedDirectories;
    private static Vector<String> directoryConfigs;
    private static Vector<String> fileConfigs;
    private static Vector<Integer> dependencySizeVector;
    private static HashMap<String, MakefileConfig> makeFileConfigs;
    private static int numOfConfigConditions = 0;
    private static int totalNumOfConditions = 0;
    private static int minNumOfConfigConditions = Integer.MAX_VALUE;
    private static int maxNumOfConfigConditions = 0;

    private static enum KEYWORDS {

        HEAD, INIT, CORE, LIBS, DRIVERS, NET, OBJ, PLAT, MACHINE, ARCH, NONE
    };

    static {
        allFiles = new Vector<String>();
        conditionedFiles = new Vector<String>();
        parsedMakeFiles = new Vector<String>();
        conditionedDirectories = new Vector<String>();
        parsedDirectories = new Vector<String>();
        directoryConfigs = new Vector<String>();
        fileConfigs = new Vector<String>();
        makeFileConfigs = new HashMap<String, MakefileConfig>();
        dependencySizeVector = new Vector<Integer>();
    }

    //constructor
    public MakeFileParser(String architecture, boolean printStatistics) {
        try {
            arch = architecture;
            totalNumOfFiles = 0;
            filesWithDep = 0;
            this.printStatistics = printStatistics;
            outputFile = new PrintWriter(new FileOutputStream(new File("models/" + architecture + ".makemodel")));

        } catch (FileNotFoundException ex) {
            Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //parsing
    private void readLineWithDep(String path, String line, BufferedReader inputReader, String previousDependencies, String dependency, HashMap<String, Vector<CompositeObjectEntry>> variableMap, HashMap<String, String> dependencyMap) {
        if (dependency.trim().length() != 0 && previousDependencies.trim().length() != 0) {
            //there is a current dependency and a parent dependency
            readLine(path, line, inputReader, "(" + previousDependencies + " && " + dependency + ")", variableMap, dependencyMap);
        } else if (dependency.trim().length() != 0) {
            //found a current dependency but no parent dependencies
        //	System.out.println("should read line with dependency: " + dependency);
            readLine(path, line, inputReader, dependency, variableMap, dependencyMap);
        } else if (previousDependencies.trim().length() != 0) {
            //found no current dependency but there is a parent dependency
            readLine(path, line, inputReader, previousDependencies, variableMap, dependencyMap);
        } else {
            //no parent dependencies & no current dependencies
            readLine(path, line, inputReader, "", variableMap, dependencyMap);
        }
    }

    KEYWORDS findKeyword(String line) {

        int index = -1;

        //look for the keywords, and make sure they are at the beginning of the line
        if (line.startsWith(HEAD)) {
            index = HEAD.length();
            return KEYWORDS.HEAD;
        } else if ((index = line.indexOf(INIT)) != -1 && (index == 0)) {
            index = INIT.length();
            return KEYWORDS.INIT;
        } else if ((index = line.indexOf(CORE)) != -1 && (index == 0)) {
            index = CORE.length();
            return KEYWORDS.CORE;
        } else if ((index = line.indexOf(LIBS)) != -1 && (index == 0)) {
            index = LIBS.length();
            return KEYWORDS.LIBS;
        } else if ((index = line.indexOf(DRIVERS)) != -1 && (index == 0)) {
            index = DRIVERS.length();
            return KEYWORDS.DRIVERS;
        } else if ((index = line.indexOf(NET)) != -1 && (index == 0)) {
            index = NET.length();
            return KEYWORDS.NET;
        } else if ((index = line.indexOf(OBJ)) != -1 && (index == 0)) {
            index = OBJ.length();
            return KEYWORDS.OBJ;
        } else if ((index = line.indexOf(PLAT)) != -1 && (index == 0)) {
            index = PLAT.length();
            return KEYWORDS.PLAT;
        } else if ((index = line.indexOf(MACHINE)) != -1 && (index == 0)) {
            index = MACHINE.length();
            return KEYWORDS.MACHINE;
        } else if ((index = line.indexOf(ARCH)) != -1 && (index == 0)) {
            index = ARCH.length();
            return KEYWORDS.ARCH;
        }

        return KEYWORDS.NONE;
    }

    private String flipDependency(String dependency) {
        if (dependency.startsWith("!")) {
        	//System.out.println("flipping dependency: " + dependency + " to "+ dependency.substring(1));
            return dependency.substring(1);
        } else {
            return "!" + dependency;
        }
    }

    private void outputVariableMap(HashMap<String, Vector<CompositeObjectEntry>> variableMap) {
        System.out.println("VARIABLE MAP");
        Iterator it = variableMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            System.out.println("Key: " + pairs.getKey());
            System.out.println("--------------");
            Vector<CompositeObjectEntry> entries = (Vector<CompositeObjectEntry>) pairs.getValue();
            for (CompositeObjectEntry entry : entries) {
                System.out.println(entry);
            }
        }
    }

    private HashMap<String, Vector<CompositeObjectEntry>> constructVariableMap(String parentPath, String childPath, boolean checkKbuild) throws IOException {
        HashMap<String, Vector<CompositeObjectEntry>> variableMap = new HashMap<String, Vector<CompositeObjectEntry>>();

        BufferedReader inputReader = null;
        try {
            // String path = parentPath;
            StringBuilder path = new StringBuilder();
            path.append(parentPath);
            String fileName = path + "Makefile";
            if (checkKbuild) {
                fileName = getFileName(parentPath, childPath, path);
            }

            if (fileName == null) {
                // System.out.println("could not open fileName in path: " + parentPath + " " + childPath);
                return variableMap;
            }

            //  System.out.println("file name in construct: " + fileName + " and path became:  " + path);
            inputReader = new BufferedReader(new FileReader(fileName));

            while (inputReader.ready()) {
                String line = inputReader.readLine();
                //ignore comments
                if (!line.startsWith("#")) {
                    if (isConditional(line)) {
                    	//need to fix this
                        skipConditional(inputReader);
                    } else {
                        line = line.trim();


                        //old pattern : "[a-zA-Z0-9]+-(objs|y|(\\$\\(CONFIG_.*\\)))(-y)?\\s*[+=|:=|=].*"
                        if (line.matches("[a-zA-Z0-9]+(-objs)?-(objs|y|(\\$\\(CONFIG_.*\\)))\\s*[+=|:=|=].*")) {
                            //a -y takes precedence over a -objs coz sometimes they both exist

                            int assignmentIndex = line.indexOf(":=");
                            int assignmentLength = 2;
                            if (assignmentIndex == -1) {
                                assignmentIndex = line.indexOf("+=");

                                if (assignmentIndex == -1) {
                                    assignmentIndex = line.indexOf("=");
                                    assignmentLength = 1;
                                }
                            }
                            //     System.out.println("line: " + line + " assignment index: " + assignmentIndex);
                            String firstPart = line.substring(0, assignmentIndex);
                            //   System.out.println("looing at first part: " + firstPart);
                            int dashIndex = firstPart.lastIndexOf("-y");

                            if (dashIndex == -1) {
                                //        System.out.println("couldn't find -y");
                                dashIndex = firstPart.lastIndexOf("-$");
                            }

                            if (dashIndex == -1) {
                                //      System.out.println("couldn't find -$");
                                dashIndex = firstPart.lastIndexOf("-objs");
                            }



                            String variableBaseName = firstPart.substring(0, dashIndex);
                            //        System.out.println("variable base name: "+ variableBaseName);

                            String variableSecondPart = firstPart.substring(dashIndex + 1).trim();

                            String dependency = "";
                            if (variableSecondPart.contains("$")) {
                                dependency = variableSecondPart.substring(variableSecondPart.indexOf("(") + 1, variableSecondPart.indexOf(")"));
                            }


                            String variableValue = line.substring(assignmentIndex + assignmentLength);
                            variableValue = getNextLines(variableValue, inputReader);

                            variableValue = variableValue.replace("$(" + variableBaseName + "-y)", "");
                            variableValue = variableValue.replace("$(" + variableBaseName + "-m)", "");
                            variableValue = variableValue.replace("$(" + variableBaseName + "-objs-y)", "");
                            variableValue = variableValue.replace("$(" + variableBaseName + "-objs-m)", "");



                            // System.out.println("variable base name :" + variableBaseName + " variable second part: " + variableSecondPart + " variable value: " + variableValue + " dependency: " + dependency + " variable second part: " + variableSecondPart);
                            CompositeObjectEntry compositeObjectEntry = new CompositeObjectEntry(variableValue, dependency, variableSecondPart);
                            if (variableMap.containsKey(variableBaseName)) {
                                variableMap.get(variableBaseName).add(compositeObjectEntry);
                            } else {
                                Vector<CompositeObjectEntry> vector = new Vector<CompositeObjectEntry>();
                                vector.add(compositeObjectEntry);
                                variableMap.put(variableBaseName, vector);
                            }
                        }

                    }
                }
            }

            //   outputVariableMap(variableMap);
            return variableMap;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (inputReader != null) {
                inputReader.close();
            }
        }
    }

    private void skipConditional(BufferedReader inputReader) {

        try {
            while (inputReader.ready()) {
                String line = inputReader.readLine();

                if (!line.trim().startsWith("#")) {
                    if (isConditional(line)) {

                        skipConditional(inputReader);
                    } else if (line.trim().startsWith("endif") || line.trim().startsWith("endef")) {

                        return;
                    } else {
                    }
                }
            }
            //   System.out.println("ERROOOOOR NEVER FOUND CLOSING");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseConditional(String path, String conditionalLine, BufferedReader inputReader, String previousDependencies, HashMap<String, Vector<CompositeObjectEntry>> variableMap, HashMap<String, String> dependencyMap) {

        try {

            String dependency = "";

            int dollarIndex = conditionalLine.indexOf("$");

            if (dollarIndex == -1) {
                return;
            }

            int bracketIndex = conditionalLine.indexOf(")", dollarIndex);
            dependency = conditionalLine.substring(dollarIndex + 2, bracketIndex).trim();

            //let's just focus on CONFIG variables for now
            if (!dependency.startsWith("CONFIG_")) {
                skipConditional(inputReader);
                return;
            }

            int commaIndex = conditionalLine.indexOf(",");
            bracketIndex = conditionalLine.indexOf(")", commaIndex);
            String condition = conditionalLine.substring(commaIndex + 1, bracketIndex).trim();

            if (conditionalLine.trim().startsWith(IFEQ)) {

                //ifeq ($(dependency), ) ==> !dependency
                if (condition.isEmpty()) {
                    dependency = "!" + dependency;
                }

            } else if (conditionalLine.trim().startsWith(IFNEQ)) {
                if (condition.equals("y")) {
                    dependency = "!" + dependency;
                }
            }

            while (inputReader.ready()) {
                String line = inputReader.readLine();

                if (!line.trim().equals(ENDIF)) {

                    if (line.equals(ELSE)) {
                    	//System.out.println("line is else so flipped. Previous dep: " + previousDependencies +" current: " + dependency);
                    	//read the next line to avoid parsing "else" again
                    	line = inputReader.readLine();
                        readLineWithDep(path, line, inputReader, previousDependencies, flipDependency(dependency), variableMap, dependencyMap);
                    } else {
                    	//line is not else so it is the right line for parsing
                        readLineWithDep(path, line, inputReader, previousDependencies, dependency, variableMap, dependencyMap);
                    }
                } else {
                    break;
                }
            }
            //   System.out.println("ERROOOOOR NEVER FOUND CLOSING");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseMakeFile(String parentPath, String childPath, String previousDependencies, boolean checkKbuild) {
        try {
            HashMap<String, Vector<CompositeObjectEntry>> variableMap = constructVariableMap(parentPath, childPath, checkKbuild);
            evaluateMakeFile(parentPath, childPath, variableMap, previousDependencies, checkKbuild);
        } catch (IOException ex) {
            Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void evaluateMakeFile(String parentPath, String childPath, HashMap<String, Vector<CompositeObjectEntry>> variableMap, String previousDependencies, boolean checkKbuild) throws IOException {
        //  System.out.println("evaluating file: " + parentPath + "  " + childPath);
        BufferedReader inputReader = null;
        //  System.out.println("variable map: " + variableMap);
        HashMap<String, String> dependencyMap = new HashMap<String, String>();
        try {
            StringBuilder path = new StringBuilder();
            path.append(parentPath);
            path.append(childPath);
            String fileName = path + "Makefile";
            if (checkKbuild) {
                fileName = getFileName(parentPath, childPath, path);
            }

            if (fileName == null) {
                //   System.out.println("COULD NOT open" + fileName + " in path: " + parentPath + "  " + childPath);
                return;
            }

            // System.out.println("file name in evaluate: " + fileName + " and path became:  " + path);
            inputReader = new BufferedReader(new FileReader(fileName));

            if (!parsedMakeFiles.contains(fileName)) {
                parsedMakeFiles.add(fileName);
            }

            while (inputReader.ready()) {

                String line = inputReader.readLine();
                // System.out.println("Read line: " + line);


                //ignore comments
                if (!line.startsWith("#")) {
                    if (isConditional(line)) {
                        // skipConditional(inputReader);
                        parseConditional(path.toString(), line, inputReader, previousDependencies, variableMap, dependencyMap);
                    } else {
                        readLine(path.toString(), line, inputReader, previousDependencies, variableMap, dependencyMap);
                    }
                }
            }

            writeDependencies(path.toString(), dependencyMap);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (inputReader != null) {
                inputReader.close();
            }
        }
    }

    private void readLine(String path, String line, BufferedReader inputReader, String previousDependencies, HashMap<String, Vector<CompositeObjectEntry>> variableMap, HashMap<String, String> dependencyMap) {
        try {
        	//System.out.println("reading line: " + line);
            KEYWORDS keyword = KEYWORDS.NONE;
            if (line.trim().startsWith("include ")) {
                return;
                //   System.out.println("FOUND include: " + line);
            }
            if (line.length() != 0) {
                keyword = findKeyword(line);
            }
            if (keyword == KEYWORDS.NONE) {
                //no keyword, go to next line
                return;
            }

            parseLine(path.toString(), line, inputReader, previousDependencies, variableMap, dependencyMap, keyword);
        } catch (IOException ex) {
            Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addFiles(String path, String toBeAdded, String dependency, HashMap<String, Vector<CompositeObjectEntry>> variableMap, HashMap<String, String> dependencyMap, KEYWORDS keyword) {

        StringTokenizer stringTokenizer = new StringTokenizer(toBeAdded);
        String token = "";

        while (stringTokenizer.hasMoreTokens()) {
            token = stringTokenizer.nextToken();

            token = replaceVariables(token, variableMap);

            //check if token is a composite object
            if (variableMap.containsKey(token) && !fileExists(token, path)) {

                addCompositeObjects(token, path, dependency, variableMap, dependencyMap, keyword);
                continue;
            }

            //if token ends with .o, check first if the part before the .o is a composite object
            //otherwise replace .o with .c and continue processing
            if (token.endsWith(".o")) {
                String tokenName = token.substring(0, token.indexOf(".o"));
                //it is a comosite object
                if (variableMap.containsKey(tokenName) && !fileExists(tokenName, path)) {
                    addCompositeObjects(tokenName, path, dependency, variableMap, dependencyMap, keyword);
                    continue;
                }

                token = token.replace(".o", ".c"); //replace .o with .c

            }

            //if token is a variable then we need to look up the variable value from
            //the variable map
            if (token.trim().startsWith("$")) {
                int dashIndex = token.indexOf("-y");

                if (dashIndex == -1) {
                    dashIndex = token.indexOf("-$");
                }

                if (dashIndex == -1) {
                    dashIndex = token.indexOf("-objs");
                }

                if (dashIndex != -1) {
                    String variableName = token.substring(token.indexOf("$(") + 2, dashIndex);
                    if (variableMap.containsKey(variableName) && !fileExists(variableName, path)) {
                        //        System.out.println("calling composite object with: " + variableName);
                        addCompositeObjects(variableName, path, dependency, variableMap, dependencyMap, keyword);
                        continue;
                    }
                }
            }

            token = checkToken(token, keyword);

            //we have a final token (not a composite object) so start adding to dependency map
            if (dependencyMap.containsKey(token)) {
                String existingDependency = dependencyMap.get(token);

                if (existingDependency.length() != 0 && dependency.length() != 0) {
                    dependencyMap.put(token, "(" + existingDependency + " || " + dependency + ")");
                } else if (existingDependency.length() != 0) {
                    dependencyMap.put(token, existingDependency);
                } else {
                    dependencyMap.put(token, dependency);
                }

            } else {
                dependencyMap.put(token, dependency);
            }
        }
    }

    private void parseLine(String path, String line, BufferedReader inputReader, String previousDependencies, HashMap<String, Vector<CompositeObjectEntry>> variableMap, HashMap<String, String> dependencyMap, KEYWORDS keyword) throws IOException {
        line = line.trim();

        int dashIndex = line.indexOf("-");

        if (dashIndex == -1) {
            return;
        }

        int assignmentIndex = line.indexOf(":=");
        int assignmentLength = 2;
        if (assignmentIndex == -1) {
            assignmentIndex = line.indexOf("+=");

            if (assignmentIndex == -1) {
                assignmentIndex = line.indexOf("=");
                assignmentLength = 1;
            }
        }

        if ((assignmentIndex == -1) || (dashIndex + 1 > assignmentIndex)) {
            return;
        }

        String variableSecondPart = line.substring(dashIndex + 1, assignmentIndex).trim();
        String dependency = "";
        if (variableSecondPart.contains("$")) {
            int index1 = variableSecondPart.indexOf("(");
            int index2 = variableSecondPart.indexOf(")");

            if (index1 != -1 && index2 != -1) {
                dependency = variableSecondPart.substring(index1 + 1, index2);
            }
        }

        String variableValue = line.substring(assignmentIndex + assignmentLength);

        //avoid lines that set compiler options
        if (variableValue.startsWith("-")) {
            return;
        }

        variableValue = getNextLines(variableValue, inputReader);

//        //dealing with modules
//        if(!dependency.isEmpty() && !dependency.endsWith("MODULE")){
//            dependency= "(" + dependency + " || " + dependency+ "_MODULE)";
//        }
        //System.out.println("line : " + line);

        if (dependency.length() != 0 && previousDependencies.length() != 0) {
        	//System.out.println("should have both previous : " + previousDependencies + " and current : " + dependency);
            //there is a current dependency and a parent dependency
            addFiles(path, variableValue, "(" + previousDependencies + " && " + dependency + ")", variableMap, dependencyMap, keyword);
        } else if (dependency.length() != 0) {
            //found a current dependency but no parent dependencies
            addFiles(path, variableValue, dependency, variableMap, dependencyMap, keyword);
        } else if (previousDependencies.length() != 0) {
            //found no current dependency but there is a parent dependency
            addFiles(path, variableValue, previousDependencies, variableMap, dependencyMap, keyword);
        } else {
            //no parent dependencies & no current dependencies
            addFiles(path, variableValue, "", variableMap, dependencyMap, keyword);

        }
    }

    private void addCompositeObjects(String tokenName, String path, String dependency, HashMap<String, Vector<CompositeObjectEntry>> variableMap, HashMap<String, String> dependencyMap, KEYWORDS keyword) {
        //  System.out.println("in composite objects: " + tokenName);
        Vector<CompositeObjectEntry> entries = variableMap.get(tokenName);

        for (CompositeObjectEntry entry : entries) {
            String dep = entry.getDependency();
            if (!dep.isEmpty() && !dependency.isEmpty()) {
                //  System.out.println("calling add files with : " + entry.getObjects());
                addFiles(path, entry.getObjects(), "(" + dependency + " && " + dep + ")", variableMap, dependencyMap, keyword);
            } else if (!dep.isEmpty()) {
                //  System.out.println("calling add files with : " + entry.getObjects());
                addFiles(path, entry.getObjects(), dep, variableMap, dependencyMap, keyword);
            } else if (!dependency.isEmpty()) {
                //   System.out.println("calling add files with : " + entry.getObjects());
                addFiles(path, entry.getObjects(), dependency, variableMap, dependencyMap, keyword);
            } else {
                //   System.out.println("calling add files with : " + entry.getObjects());
                addFiles(path, entry.getObjects(), "", variableMap, dependencyMap, keyword);
            }

        }
    }

    private String getNextLines(String variableValue, BufferedReader inputReader) throws IOException {
        String newVariableValue = variableValue;

        if (newVariableValue.trim().endsWith("\\")) {
            newVariableValue = newVariableValue.trim();
            newVariableValue = newVariableValue.replaceAll("\\\\", "");

            if (inputReader.ready()) {
                String nextLine = inputReader.readLine();
                newVariableValue += nextLine;
                newVariableValue = getNextLines(newVariableValue, inputReader);
            }
        }

        return newVariableValue;
    }

    //output models
    private void writeDependencies(String path, HashMap<String, String> dependencyMap) {

        Iterator it = dependencyMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            String key = (String) pairs.getKey();
            String dependency = (String) pairs.getValue();

            if (key.trim().endsWith("/")) {
                if (!parsedDirectories.contains(path + key)) {
                    parsedDirectories.add(path + key);
                }

                if (!dependency.isEmpty()) {

                    if (!conditionedDirectories.contains(path + key)) {
                        conditionedDirectories.add(path + key);
                    }

                    getConfigsFromDependency(dependency, path + key, true);
                    getConfigsFromDependency(dependency, directoryConfigs);

                }

                parseMakeFile(path, key, dependency, true);
            } else {

                if (!dependency.isEmpty()) {
                    totalNumOfConditions++;
                    int currentNumOfConfigs = getNumOfConfigs(dependency);

                    
                    numOfConfigConditions += currentNumOfConfigs;
                    dependencySizeVector.add(currentNumOfConfigs);

                    if (currentNumOfConfigs == 0) {
                        System.out.println("strange: " + dependency);
                    }
                    if (currentNumOfConfigs < minNumOfConfigConditions) {
                        minNumOfConfigConditions = currentNumOfConfigs;
                    }

                    if (currentNumOfConfigs > maxNumOfConfigConditions) {
                        maxNumOfConfigConditions = currentNumOfConfigs;
                    }

                }

                getConfigsFromDependency(dependency, fileConfigs);

                if (!key.trim().equals("\\")) {


                    if (key.contains("$(BITS)")) {

                        String newKey = key.replace(("$(BITS)"), "32");


                        outputFile.print("\"" + path + newKey + "\"");

                        totalNumOfFiles++;

                        addFileToTotal(path + newKey);


                        if (dependency.length() != 0) {
                            getConfigsFromDependency(dependency, path + newKey, false);
                            outputFile.print(" \"(" + dependency + ")\"");

                            addConditionedFile(path + newKey);

                            filesWithDep++;
                        }

                        outputFile.println();

                        newKey = key.replace("$(BITS)", "64");

                        outputFile.print("\"" + path + newKey + "\"");

                        totalNumOfFiles++;
                        addFileToTotal(path + newKey);

                        if (dependency.length() != 0) {
                            getConfigsFromDependency(dependency, path + newKey, false);
                            outputFile.print(" \"(" + dependency + ")\"");

                            addConditionedFile(path + newKey);

                            filesWithDep++;
                        }

                        outputFile.println();
                    } else {
                        outputFile.print("\"" + path + key + "\"");

                        totalNumOfFiles++;
                        addFileToTotal(path + key);

                        if (dependency.length() != 0) {
                            getConfigsFromDependency(dependency, path + key, false);
                            outputFile.print(" \"(" + dependency + ")\"");

                            addConditionedFile(path + key);

                            filesWithDep++;
                        }

                        outputFile.println();
                    }
                }
            }
        }


    }

    //parsing helpers
    private boolean isConditional(String line) {
        line = line.trim();

        return (line.startsWith("ifdef") || line.startsWith("ifndef") || line.startsWith("ifeq") || line.startsWith("ifneq") || line.startsWith("define"));
    }

    private String checkToken(String token, KEYWORDS keyword) {

        if (keyword == KEYWORDS.PLAT) {
            token = "plat-" + token + "/";
        } else if (keyword == KEYWORDS.MACHINE) {
            token = "mach-" + token + "/";
        } else if (keyword == KEYWORDS.ARCH) {
            token = "arch-" + token + "/";
        }

        return token;
    }

    private boolean fileExists(String entry, String path) {
        String fileName = entry.trim();

        File file = new File(path + fileName + ".c");

        if (file.exists()) {
            //     System.out.println("Returning true");
            return true;
        } else {
            file = new File(path + fileName + ".S");

            if (file.exists()) {
                return true;
            }
        }

        return false;
    }

    private String replaceVariables(String token, HashMap<String, Vector<CompositeObjectEntry>> variableMap) {
        String newToken = token.replaceAll("\\$\\(ARCH\\)", arch);
        newToken = newToken.replaceAll("\\$\\(ARCH_DIR\\)", "arch/" + arch);


        return newToken;
    }

    private String getFileName(String parentPath, String childPath, StringBuilder finalPath) {

        String finalPathString = parentPath + childPath;
        String fileName = "";

        //   System.out.println("trying "+ finalPath + "Kbuild");
        File file = new File(finalPathString + "Kbuild");

        if (!file.exists()) {
            //  System.out.println("trying "+ finalPath + "Makefile");
            file = new File(finalPathString + "Makefile");

            if (!file.exists()) {

                finalPathString = "./" + childPath;
                //      System.out.println("trying "+ finalPath + "Kbuild");
                file = new File(finalPathString + "Kbuild");

                if (!file.exists()) {
                    //       System.out.println("trying "+ finalPath + "Makefile");
                    file = new File(finalPathString + "Makefile");

                    if (!file.exists()) {
                        //      System.out.println("nothing");
                        return null;
                    } else {
                        fileName = finalPathString + "Makefile";
                    }

                } else {
                    fileName = finalPathString + "Kbuild";
                }
            } else {
                fileName = finalPathString + "Makefile";
            }

        } else {
            fileName = finalPathString + "Kbuild";
        }
        finalPath.delete(0, finalPath.length());
        finalPath.append(finalPathString);

        return fileName;
    }

    private void getConfigsFromDependency(String dependency, Vector<String> configVector) {

        Pattern pattern = Pattern.compile("CONFIG_[_a-zA-Z0-9]*");
        Matcher matcher = pattern.matcher(dependency);

        while (matcher.find()) {

            String config = dependency.substring(matcher.start(), matcher.end());
            if (!configVector.contains(config)) {
                configVector.add(config);
            }
        }


    }

    public void closeFile() {
        outputFile.close();
    }

    public static void writeArchNames() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter((new File("archNames.txt")));
            File archDir = new File("./arch/");
            FileFilter directoryFilter = new FileFilter() {

                public boolean accept(File pathname) {
                    return pathname.isDirectory() && !pathname.getName().startsWith(".");
                }
            };
            File[] directories = archDir.listFiles(directoryFilter);
            if (directories == null) {
                return;
            }
            for (File dir : directories) {
                String archName = dir.getName().trim();
                //  System.out.println("archName: " + archName);
                writer.println(archName);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            writer.close();
        }
    }

    //statistics
    private void addFileToTotal(String fileName) {
        if (!allFiles.contains(fileName)) {
            allFiles.add(fileName);
        }
    }

    private void addConditionedFile(String fileName) {
        if (!conditionedFiles.contains(fileName)) {
            conditionedFiles.add(fileName);
        }

    }
    
    private static void printStatisticsInFile(){
    	 try {
             PrintWriter fileWriter = new PrintWriter(new FileOutputStream(new File("statistics.txt")));

             fileWriter.println(parsedMakeFiles.size() + "," + allFiles.size() + "," + parsedDirectories.size() + conditionedFiles.size() + "," + conditionedDirectories.size() + "," + makeFileConfigs.size());
             fileWriter.close();
         } catch (IOException ex) {
             Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
         }
    }

    private static void printParsedFiles() {
        try {
            PrintWriter fileWriter = new PrintWriter(new FileOutputStream(new File("parsedMakefiles.txt")));

            for (String file : parsedMakeFiles) {
                fileWriter.println(file);
            }
        } catch (IOException ex) {
            Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    private static void printDirectoryConfigs() {
        try {
            PrintWriter fileWriter = new PrintWriter(new FileOutputStream(new File("directoryConfigs.txt")));

            for (String config : directoryConfigs) {
                fileWriter.println(config);
            }

            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void printAllConfigs() {
        try {
            PrintWriter fileWriter = new PrintWriter(new FileOutputStream(new File("makeFileConfigs.txt")));
            int dirCount = 0;
            int fileCount = 0;
            int total = makeFileConfigs.size();
            int controlDir = 0;
            int controlFile = 0;
            int fileOnly = 0;
            int dirOnly = 0;


            for (String config : makeFileConfigs.keySet()) {
                MakefileConfig makefileConfig = makeFileConfigs.get(config);
                dirCount += makefileConfig.getDirectoriesControlled().size();
                fileCount += makefileConfig.getFilesControlled().size();

                if (makefileConfig.isControlledDir()) {
                    controlDir++;
                }

                if (makefileConfig.isControlledFile()) {
                    controlFile++;
                }

                if (makefileConfig.isControlledDir() && !makefileConfig.isControlledFile()) {
                    dirOnly++;
                }

                if (makefileConfig.isControlledFile() && !makefileConfig.isControlledDir()) {
                    fileOnly++;
                }

                fileWriter.println(config + "," + makefileConfig.getDirectoriesControlled().size() + "," + makefileConfig.getFilesControlled().size());
            }

            System.out.println("Avg # of files a config controls: " + (fileCount / total));
            System.out.println("Avg # of dirs a config controls: " + (dirCount / total));
            System.out.println("# controlling dir: " + controlDir);
            System.out.println("# controlling file: " + controlFile);
            System.out.println("# controlling dir only: " + dirOnly);
            System.out.println("# controlling file only:" + fileOnly);
            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void printFileConfigs() {
        try {
            PrintWriter fileWriter = new PrintWriter(new FileOutputStream(new File("fileConfigs.txt")));

            for (String config : fileConfigs) {
                fileWriter.println(config);
            }

            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void printDependencySize() {
        try {
            PrintWriter fileWriter = new PrintWriter(new FileOutputStream(new File("dependencySize.csv")));

            for (Integer size : dependencySizeVector) {
                fileWriter.println(size);
            }

            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void printIndivStatistics() {
        if (!printStatistics) {
            return;
        }

        System.out.println("-----------------------------------");
        System.out.println("Arch: " + arch);
        System.out.println("total number of files in Make for arch: " + arch + " = " + totalNumOfFiles);
        System.out.println("total number of files in Make WITH DEP for arch: " + arch + " = " + filesWithDep);
        System.out.println("-----------------------------------");

    }

    private static void printOverallStatistics() {

        System.out.println("Total number of build files processed: " + parsedMakeFiles.size());
        System.out.println("Total .c Files Processed: " + allFiles.size());
        System.out.println("Total Conditional .c Files Processed: " + conditionedFiles.size());
        System.out.println("Total directories processed: " + parsedDirectories.size());
        System.out.println("Total conditional directories processed: " + conditionedDirectories.size());
        System.out.println("Total CONFIGS seen during parsing: " + makeFileConfigs.size());
        System.out.println("Avg number of CONFIGs in dependency" + (numOfConfigConditions / totalNumOfConditions));
        System.out.println("Max. num of CONFIGS in dependencies: " + maxNumOfConfigConditions);
        System.out.println("Min num of CONFIGS in dependencies: " + minNumOfConfigConditions);
        
      printStatisticsInFile();
        printParsedFiles();
        printFileConfigs();
        printDirectoryConfigs();
        printAllConfigs();
        printDependencySize();
    }

    //statistics helpers
    private int getNumOfConfigs(String dependency) {

        Pattern pattern = Pattern.compile("CONFIG_[_a-zA-Z0-9]*");
        Matcher matcher = pattern.matcher(dependency);
        Vector<String> configsInDep = new Vector<String>();        
        while (matcher.find()) {
            String config = dependency.substring(matcher.start(), matcher.end());
            if (!configsInDep.contains(config)) {
                configsInDep.add(config);
            }
        }

        return configsInDep.size();
    }

    private void getConfigsFromDependency(String dependency, String fileOrDir, boolean isDirectory) {

        Pattern pattern = Pattern.compile("CONFIG_[_a-zA-Z0-9]*");
        Matcher matcher = pattern.matcher(dependency);

        while (matcher.find()) {

            String config = dependency.substring(matcher.start(), matcher.end());
            if (makeFileConfigs.containsKey(config)) {

                if (isDirectory) {
                    makeFileConfigs.get(config).setControlledDir(true);
                    makeFileConfigs.get(config).addControlledDir(fileOrDir);
                } else {
                    makeFileConfigs.get(config).setControlledFile(true);
                    makeFileConfigs.get(config).addControledFile(fileOrDir);
                }
            } else {
                MakefileConfig makefileConfig = new MakefileConfig(config);
                if (isDirectory) {
                    makefileConfig.setControlledDir(true);
                    makefileConfig.addControlledDir(fileOrDir);
                } else {
                    makefileConfig.setControlledFile(true);
                    makefileConfig.addControledFile(fileOrDir);
                }

                makeFileConfigs.put(config, makefileConfig);
            }
        }


    }

    public static void main(String args[]) {
        BufferedReader reader = null;
        try {        	
            writeArchNames();
            File file = new File("archNames.txt");
            reader = new BufferedReader(new FileReader(file));

            while (reader.ready()) {
                String arch = reader.readLine().trim();
                MakeFileParser constructor = new MakeFileParser(arch, true);
                //constructor.parseMakeFile("./drivers/net/chelsio/", "", "", true);

                if (args.length == 0) {
                    constructor.parseMakeFile("./init/", "", "", true);
                    constructor.parseMakeFile("./kernel/", "", "", true);
                    constructor.parseMakeFile("./mm/", "", "", true);
                    constructor.parseMakeFile("./fs/", "", "", true);
                    constructor.parseMakeFile("./ipc/", "", "", true);
                    constructor.parseMakeFile("./security/", "", "", true);
                    constructor.parseMakeFile("./crypt/", "", "", true);
                    constructor.parseMakeFile("./block/", "", "", true);
                    constructor.parseMakeFile("./usr/", "", "", true);
                    constructor.parseMakeFile("./lib/", "", "", true);
                    constructor.parseMakeFile("./drivers/", "", "", true);
                    constructor.parseMakeFile("./sound/", "", "", true);
                    constructor.parseMakeFile("./firmware/", "", "", true);
                    constructor.parseMakeFile("./net/", "", "", true);

                    constructor.parseMakeFile("./arch/" + arch + "/", "", "", false);
                } else {
                    constructor.parseMakeFile(args[0], "", "", false);
                }
                constructor.closeFile();



                constructor.printIndivStatistics();

            }

            printOverallStatistics();


        } catch (IOException ex) {
            Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(MakeFileParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
