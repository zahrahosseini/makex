/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package syntaxchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import syntaxchecker.syntaxerrors.FileMissingInMakeError;
import syntaxchecker.syntaxerrors.KConfigMissingError;
import syntaxchecker.syntaxerrors.ObjectMissingError;
import syntaxchecker.syntaxerrors.SyntaxError;

/**
 *
 * @author snadi
 */
public class SyntaxChecker {

    private String directoryPath;
    private Vector<SyntaxError> syntaxErrors;
    private static Vector<String> configEntries;
    private boolean directoryHasKbuild;
    private boolean directoryHasMakefile;
    private int numOfFiles;
    private HashMap<String, Vector<String>> includedFiles;
    public static int dodgedCount = 0;
    public static Vector<String> interestingIncludes = new Vector<String>();
    public static int filesChecked = 0;
    private Vector<String> foundFiles;
    private static int count = 0;
    //private static int includeCounter = 1;
    // public static int inAnotherDirectory = 0;
    //  public static int numOfIncludedFiles = 0;
    //public static int sumOfSize = 0;
    public static int numOfRemovedFiles = 0;
    public static int directoryWithNoMakeFile = 0;
    public static int noKbuildEither = 0;

    public static int getCount() {
        return count;
    }

    private Vector<String> getConfigsFromLine(String line, String previousConfig) {
        Vector<String> configs = new Vector<String>();


        int assignmentIndex = line.indexOf(":=");

        if (assignmentIndex == -1) {
            assignmentIndex = line.indexOf("+=");
        }

        if (assignmentIndex == -1) {
            assignmentIndex = line.indexOf("=");
        }


        String variablePart = line.substring(0, assignmentIndex).trim();

        String configName = variablePart.substring(variablePart.indexOf("$(") + 2, variablePart.indexOf(")"));

        configs.add(configName);

        int secondVar = variablePart.indexOf("$(", variablePart.indexOf("$(") + 2);
        if (secondVar != -1) {
            configName = variablePart.substring(secondVar + 2, variablePart.indexOf(")", variablePart.indexOf(")") + 1));
            //   System.out.println("ADDING SECOND config: " + configName);
            configs.add(configName);
        }

        if (previousConfig != null && previousConfig.length() != 0) {
            configs.add(previousConfig);
        }

        return configs;
    }

    private String substituteVariables(String line) {
        return line;//.replace(("$(CONFIG_WORD_SIZE)"), "64");
    }

    private void addSyntaxError(SyntaxError syntaxError) {
        if (syntaxError != null && !syntaxErrors.contains(syntaxError) && !syntaxError.getFileName().endsWith("asm-offsets.c")) {
            syntaxErrors.add(syntaxError);
            syntaxError.incrementCounter();
        }
    }

    private boolean checkVariablesInLine(String line, String fileName, Pattern pattern, Pattern cPattern, String makeFileName) throws IOException {
        String modifiedLine = "";

        Matcher matcher = null;
        Matcher cMatcher = null;


        if (line.contains("$(BITS")) {
            modifiedLine = line.replace("$(BITS)", "32");
            matcher = pattern.matcher(modifiedLine);
            cMatcher = cPattern.matcher(modifiedLine);

            if (!modifiedLine.trim().startsWith("#") && (matcher.find() || cMatcher.find())) {
                lookForErrors(modifiedLine, fileName, null, makeFileName);

                //if found match, no need to check the rest of the file
                return true;
            }
            modifiedLine = line.replace("$(BITS)", "64");
            matcher = pattern.matcher(modifiedLine);
            cMatcher = cPattern.matcher(modifiedLine);

            if (!modifiedLine.trim().startsWith("#") && (matcher.find() || cMatcher.find())) {
                lookForErrors(modifiedLine, fileName, null, makeFileName);

                //if found match, no need to check the rest of the file
                return true;
            }
        } else if (line.contains("$(CONFIG_WORD_SIZE")) {
            modifiedLine = line.replace("$(CONFIG_WORD_SIZE)", "32");
            matcher = pattern.matcher(modifiedLine);
            cMatcher = cPattern.matcher(modifiedLine);


            if (!modifiedLine.trim().startsWith("#") && (matcher.find() || cMatcher.find())) {
                lookForErrors(modifiedLine, fileName, null, makeFileName);

                //if found match, no need to check the rest of the file
                return true;
            }
            modifiedLine = line.replace("$(CONFIG_WORD_SIZE)", "64");
            matcher = pattern.matcher(modifiedLine);
            cMatcher = cPattern.matcher(modifiedLine);


            if (!modifiedLine.trim().startsWith("#") && (matcher.find() || cMatcher.find())) {
                lookForErrors(modifiedLine, fileName, null, makeFileName);

                //if found match, no need to check the rest of the file
                return true;
            }
        }

        return false;
    }

    private boolean checkVariablesInLine(String line, String fileName, String objName, Pattern pattern, String makeFileName) throws IOException {
        String modifiedLine = "";

        Matcher matcher = null;


        if (line.contains("$(BITS")) {
            modifiedLine = line.replace("$(BITS)", "32");
            matcher = pattern.matcher(modifiedLine);

            if (!modifiedLine.trim().startsWith("#") && (matcher.find() || line.contains("$(" + objName + "-y)") || line.contains("$(" + objName + "-y-y)"))) {
                lookForErrors(modifiedLine, fileName, null, makeFileName);

                //if found match, no need to check the rest of the file
                return true;
            }
            modifiedLine = line.replace("$(BITS)", "64");
            matcher = pattern.matcher(modifiedLine);

            if (!modifiedLine.trim().startsWith("#") && (matcher.find() || line.contains("$(" + objName + "-y)") || line.contains("$(" + objName + "-y-y)"))) {
                lookForErrors(modifiedLine, fileName, null, makeFileName);

                //if found match, no need to check the rest of the file
                return true;
            }
        } else if (line.contains("$(CONFIG_WORD_SIZE")) {
            modifiedLine = line.replace("$(CONFIG_WORD_SIZE)", "32");
            matcher = pattern.matcher(modifiedLine);


            if (!modifiedLine.trim().startsWith("#") && (matcher.find() || line.contains("$(" + objName + "-y)") || line.contains("$(" + objName + "-y-y)"))) {
                lookForErrors(modifiedLine, fileName, null, makeFileName);

                //if found match, no need to check the rest of the file
                return true;
            }
            modifiedLine = line.replace("$(CONFIG_WORD_SIZE)", "64");
            matcher = pattern.matcher(modifiedLine);

            if (!modifiedLine.trim().startsWith("#") && (matcher.find() || line.contains("$(" + objName + "-y)") || line.contains("$(" + objName + "-y-y)"))) {
                lookForErrors(modifiedLine, fileName, null, makeFileName);

                //if found match, no need to check the rest of the file
                return true;
            }
        }
        return false;
    }

    private String removeCommentsFromName(String input) {
        String includedFile = input;
        if (includedFile.contains(" ")) {
            includedFile = includedFile.substring(0, includedFile.indexOf(" "));
            includedFile = removeCommentsFromName(includedFile);
        } else if (includedFile.contains("\t")) {
            includedFile = includedFile.substring(0, includedFile.indexOf("\t"));
            includedFile = removeCommentsFromName(includedFile);
        }
        return includedFile;
    }

    private enum LineFormat {

        OBJ_Y_ENTRY, OBJ_CONFIG_ENTRY, COMPOSITE_CONFIG_ENTRY, UNKNOWN_ENTRY, HEAD_Y_ENTRY, INIT_Y_ENTRY, LIB_Y_ENTRY, DRIVERS_Y_ENTRY, NET_Y_ENTRY, CORE_Y_ENTRY, LIBS_Y_ENTRY, HOSTPROGS_Y_ENTRY, HOSTPROGS_CONFIG_ENTRY, COMPOSITE_Y_ENTRY, EXECUTABLE_ENTRY, OBJS_ENTRY
    };

    public SyntaxChecker(String directoryPath, Vector<String> configEntries) {
        this.directoryPath = directoryPath;
        syntaxErrors = new Vector<SyntaxError>();
        this.configEntries = configEntries;
        numOfFiles = 0;

        File file = new File(directoryPath + "Kbuild");

        directoryHasKbuild = file.exists();

        file = new File(directoryPath + "Makefile");

        directoryHasMakefile = file.exists();

        includedFiles = new HashMap<String, Vector<String>>();
        foundFiles = new Vector<String>();

    }

    public void setConfigEntries(Vector<String> configEntries) {
        this.configEntries = configEntries;

    }

    //this function is added for the age analyzer to check reasons
    public boolean validateFile(String fileName) throws Exception {
        return checkInMakeFile(fileName, "Makefile");

    }

    public void validateDirectory() throws Exception {

        FileFilter fileFilter = new FileFilter() {

            //only accept .c files
            public boolean accept(File file) {
                return file.isFile() && (file.getName().endsWith(".c"));// || file.getName().endsWith(".S"));
            }
        };


        File parentDirectory = new File(directoryPath);


        File[] files = parentDirectory.listFiles(fileFilter);

        if (files.length != 0) {
            if (!directoryHasMakefile) {

                directoryWithNoMakeFile++;
                //       System.out.println("No Makefile: " + directoryPath);
                if (!directoryHasKbuild) {
                    //          System.out.println("No Kbuild either: " + directoryPath);
                    noKbuildEither++;
                }

            }
        }

        if (files == null) {
            return;
        } else if (directoryPath.contains("Documentation")) {
            //ignore documentation directory
            return;

        } else {

//            for (int i = 0; i < files.length; i++) {
//                String filename = files[i].getName().trim();
//                getIncludedFiles(filename);
//            }

            for (int i = 0; i < files.length; i++) {
                String filename = files[i].getName().trim();
                filesChecked++;
                getIncludedFiles(filename);
                if (directoryHasKbuild || directoryHasMakefile) {
                    checkFile(filename);
                }
            }
        }

        numOfFiles = files.length;
        removeIncludedFiles();
    }

    private boolean checkIncludedFile(String missingFileName, Vector<String> originalFiles) {

        //keep track of all files checked before in original files to avoid circular includes resulting
        //in infinite calls
        if (includedFiles.containsKey(missingFileName)) {

            Vector<String> includingFiles = includedFiles.get(missingFileName);
            for (String includingFile : includingFiles) {

                if (foundFiles.contains(includingFile)) {
                    return true;
                } else if (!originalFiles.contains(includingFile)) {
                    originalFiles.add(missingFileName);
                    return checkIncludedFile(includingFile, originalFiles);
                }
            }
        } else {
            String headerFile = missingFileName.replace(".c", ".h");
            //  System.out.println("checking header file: "+ headerFile);
            if (includedFiles.containsKey(headerFile)) {
                Vector<String> includingFiles = includedFiles.get(headerFile);
                for (String includingFile : includingFiles) {
                    if (!includingFile.equals(missingFileName) && foundFiles.contains(includingFile)) {
                        return true;
                    } else if (!includingFile.equals(missingFileName) && !originalFiles.contains(includingFile)) {
                        originalFiles.add(missingFileName);
                        return checkIncludedFile(includingFile, originalFiles);
                    }
                }
            }
        }
        //   System.out.println("returning false for file: " + missingFileName);
        return false;
    }

    private void removeIncludedFiles() {
        int counter = 0;
        for (SyntaxError syntaxError : syntaxErrors) {
            if (syntaxError instanceof FileMissingInMakeError) {
                FileMissingInMakeError missingError = (FileMissingInMakeError) syntaxError;
                String missingFileName = missingError.getFileName();
                if (checkIncludedFile(missingFileName, new Vector<String>())) {
                    syntaxError.decrementCounter();
                    syntaxErrors.set(counter, null);
                    numOfRemovedFiles++;
                }
            }
            counter++;
        }
    }

    public boolean directoryHasMakefile() {
        return directoryHasMakefile;
    }

    public boolean directoryHasKbuild() {
        return directoryHasKbuild;
    }

    public void printErrors() {
        if (numOfFiles != 0 && !syntaxErrors.isEmpty()) {

            for (SyntaxError syntaxError : syntaxErrors) {
                if (syntaxError != null) {
                    // stringBuilder.append(syntaxError.getMessage()).append(NEW_LINE);
                    //System.out.println("Dir:" + directoryPath + "::" + syntaxError.getMessage());
                    //put dir path in message now
                    System.out.println(syntaxError.getMessage());
                    count++;
                }
            }
        }
    }

    private void getIncludedFiles(String fileName) {

        BufferedReader inputReader = null;
        try {
            inputReader = new BufferedReader(new FileReader(directoryPath + fileName));

            while (inputReader.ready()) {
                String line = inputReader.readLine();
                if (line.trim().startsWith("#include")) {

                    String includedFile = line.substring(line.indexOf("#include") + 8).trim();
                    includedFile = removeCommentsFromName(includedFile);

                    includedFile = includedFile.replaceAll("\"", "");
                    includedFile = includedFile.replaceAll("<", "");
                    includedFile = includedFile.replaceAll(">", "").trim();

                    if (includedFiles.containsKey(includedFile)) {
                        includedFiles.get(includedFile).add(fileName);
                    } else {
                        Vector<String> files = new Vector<String>();
                        files.add(fileName);
                        includedFiles.put(includedFile, files);
                    }

                    // }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Iterator it = includedFiles.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry pairs = (Map.Entry) it.next();
//            sumOfSize += ((Vector<String>) pairs.getValue()).size();
//            numOfIncludedFiles++;
//        }
    }

    private void checkFile(String fileName) throws IOException {

        checkInMakeFile(fileName, "Makefile");
    }

    private void checkInKconfig(Vector<String> configs, String fileName) {
        for (String config : configs) {
            checkInKconfig(config, fileName);
        }
    }

    private void checkInKconfig(String configName, String fileName) {
        configName = configName.substring(configName.indexOf("CONFIG_") + 7).trim();

        if (configEntries.contains(configName)) {
            return;
        } else {
            //workaround things that have FCOE:m=y
            if (!configName.contains("=")) {
                addSyntaxError(new KConfigMissingError(fileName, configName, directoryPath));
            }
        }
    }

    //line format executable-objs := fileName.o
    //need to find executable added to a hostprogs
    //or executable.o
    private void checkExecutableEntry(String line, String fileName, String makeFileName) throws IOException {

        String executableName = line.substring(0, line.indexOf("-objs")).trim();

        BufferedReader inputReader = null;
        try {

            inputReader = new BufferedReader(new FileReader(directoryPath + makeFileName));


            boolean found = false;

            while (inputReader.ready()) {
                String makeLine = inputReader.readLine().trim();

                makeLine = getNextLines(makeLine, inputReader);

                Pattern pattern = Pattern.compile("[^a-zA-Z0-9_-](" + executableName + "[\\s*|\\n]|" + executableName + "$)");
                Matcher matcher = pattern.matcher(makeLine);

                Pattern pattern2 = Pattern.compile("[^a-zA-Z0-9_-]" + executableName + ".o(\\s*|\\n)");
                Matcher matcher2 = pattern2.matcher(makeLine);

                if (!line.equals(makeLine) && (matcher.find() || matcher2.find())) {// || makeLine.contains("$(" + executableName + "-objs)"))) {

                    lookForErrors(makeLine, fileName, null, makeFileName);
                    found = true;
                    break;
                }
            }

            if (!found) {
                if (!makeFileName.equals("Kbuild") && directoryHasKbuild) {
                    checkExecutableEntry(line, fileName, "Kbuild");
                } else {
                    addSyntaxError(new ObjectMissingError(fileName, executableName, directoryPath));
                }
            }

        } catch (FileNotFoundException e) {
            if (!makeFileName.equals("Kbuild") && directoryHasKbuild) {
                checkExecutableEntry(line, fileName, "Kbuild");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputReader != null) {
                inputReader.close();
            }
        }

    }

    private boolean hasImplicitRule(String variableName) {
        //lib-y objects get linked into lib.a and extra-y objects are built but not included in built-in.o
        return variableName.equals("lib") || variableName.equals("extra");
    }

    //line of format executableName-y += fileName.o
    //need to make sure that executableName is later used somewhere as executableName.o or
    //$(executableName-y)
    private void checkCompositeYEntry(String line, String fileName, String makeFileName) throws IOException {

        String objName = line.substring(0, line.indexOf("-y")).trim();

        BufferedReader inputReader = null;
        try {

            inputReader = new BufferedReader(new FileReader(directoryPath + makeFileName));

            boolean found = false;
            while (inputReader.ready()) {
                String makeLine = inputReader.readLine();
                makeLine = getNextLines(makeLine, inputReader);

                Pattern pattern = Pattern.compile("[^a-zA-Z0-9_-]" + objName + "(.o\\s*|\\n)");
                Matcher matcher = pattern.matcher(makeLine);
                if (!line.equals(makeLine) && (matcher.find() || makeLine.contains("$(" + objName + "-y)") || makeLine.contains("$(" + objName + "-y-y)"))) {

                    lookForErrors(makeLine, fileName, null, makeFileName);
                    found = true;

                    break;
                }

                //if reached here then it did not find a match in this line
                //so we check if we can find a match if we substitute the variables
                if (!found && checkVariablesInLine(line, fileName, objName, pattern, makeFileName)) {
                    found = true;
                    break;
                }
            }

            if (!found && !hasImplicitRule(objName)) {
                if (!makeFileName.equals("Kbuild") && directoryHasKbuild) {
                    checkCompositeYEntry(line, fileName, "Kbuild");
                } else {
                    addSyntaxError(new ObjectMissingError(fileName, objName, directoryPath));
                }
            }
        } catch (FileNotFoundException e) {
            if (!makeFileName.equals("Kbuild") && directoryHasKbuild) {
                checkCompositeYEntry(line, fileName, "Kbuild");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputReader != null) {

                inputReader.close();
            }
        }


    }

    //line of format varName-$(CONFIG_x)
    private void checkCompositeInKconfig(String line, String fileName, String makeFileName) throws IOException {

        String objName = line.substring(0, line.indexOf("-$(")).trim();
        String configName = line.substring(line.indexOf("$(") + 2, line.indexOf(")")).trim();

        BufferedReader inputReader = null;
        boolean found = false;
        try {

            inputReader = new BufferedReader(new FileReader(directoryPath + makeFileName));




            while (inputReader.ready()) {
                String makeLine = inputReader.readLine();
                makeLine = getNextLines(makeLine, inputReader);
                Pattern pattern = Pattern.compile("[^a-zA-Z0-9_-]" + objName + ".o(\\s*|\\n)");
                Matcher matcher = pattern.matcher(makeLine);
                boolean matches = matcher.find();

                if (!line.equals(makeLine) && (matches || makeLine.contains("$(" + objName + "-y)") || makeLine.contains("$(" + objName + "-y-y)"))) {
                    lookForErrors(makeLine, fileName, configName, makeFileName);
                    found = true;
                    break;
                }

                //if reached here then it did not find a match in this line
                //so we check if we can find a match if we substitute the variables
                if (!found && checkVariablesInLine(line, fileName, objName, pattern, makeFileName)) {
                    found = true;
                    break;
                }
            }

            if (!found && !hasImplicitRule(objName)) {
                if (!makeFileName.equals("Kbuild") && directoryHasKbuild) {
                    checkCompositeInKconfig(line, fileName, "Kbuild");
                } else {
                    addSyntaxError(new ObjectMissingError(fileName, objName, directoryPath));
                }
            }


        } catch (FileNotFoundException e) {
            if (!makeFileName.equals("Kbuild") && directoryHasKbuild) {
                checkCompositeInKconfig(line, fileName, "Kbuild");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputReader != null) {
                inputReader.close();
            }
        }


    }

    private String getNextLines(String variableValue, BufferedReader inputReader) throws IOException {
        String newVariableValue = substituteVariables(variableValue);

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

    private boolean checkInMakeFile(String fileName, String makeFileName) throws IOException {

        BufferedReader inputReader = null;
        boolean found = false;
        try {
            inputReader = new BufferedReader(new FileReader(directoryPath + makeFileName));

            String objectName = "";
            String executableName = fileName.substring(0, fileName.indexOf(".c"));
            if (fileName.endsWith(".c")) {
                objectName = fileName.replace(".c", ".o").trim();
            } else if (fileName.endsWith(".S")) {
                objectName = fileName.replace(".S", ".o").trim();
            }


            while (inputReader.ready()) {
                String line = inputReader.readLine();
                line = getNextLines(line, inputReader);

                Pattern pattern = Pattern.compile("[^a-zA-Z0-9_-]" + objectName + "(\\s*|\\n)");

                Matcher matcher = pattern.matcher(line);

                Pattern cPattern = Pattern.compile("[^a-zA-Z0-9_-]" + fileName + "(\\s*|\\n)");
                Matcher cMatcher = cPattern.matcher(line);
                Pattern execPattern = Pattern.compile("[^a-zA-Z0-9_-](" + executableName + "[\\s*|\\n]|" + executableName + "$)");
                Matcher execMatcher = execPattern.matcher(line);

                if (!line.trim().startsWith("#") && (matcher.find() || cMatcher.find())) {
                    lookForErrors(line, fileName, null, makeFileName);

                    //if found match, no need to check the rest of the file
                    found = true;
                    //  break;
                } else if (execMatcher.find()) {
                    LineFormat format = checkLineFormat(line);
                    if (format.equals(LineFormat.HOSTPROGS_Y_ENTRY) || format.equals(LineFormat.HOSTPROGS_CONFIG_ENTRY)) {
                        lookForErrors(line, fileName, null, makeFileName);
                        found = true;
                        //   break;
                    }
                } else {
                    //check if this line includes another Makefile
                    //include ${srctree}/arch/sh/kernel/Makefile_32                    
                    if (line.trim().startsWith("include ")) {
                        String includedFile = line.substring(7);
                        int lastIndex = includedFile.lastIndexOf("/");
                        includedFile = includedFile.substring(lastIndex + 1);

                        if (!includedFile.contains("$")) {
                            boolean includedFind = checkIncludedFile(fileName, includedFile);
                            if (includedFind) {
                                found = true;
                            }
                        }
                    }
                }


                //if reached here then it did not find a match in this line
                //so we check if we can find a match if we substitute the variables
                if (!found && checkVariablesInLine(line, fileName, pattern, cPattern, makeFileName)) {
                    found = true;
                    //break;
                }

            }

            if (!found) {//&& !includedFiles.contains(fileName)) {
                //check also Kbuild file before reporting error
                //if null then it has been found in Kbuild
                //if not null then it has not been found in Kbuild
                if (directoryHasKbuild) {
                    checkInKbuild(fileName);
                } else {
                    addSyntaxError(new FileMissingInMakeError(fileName, directoryPath));
                }
            } else {
                //found
                foundFiles.add(fileName);
            }


        } catch (FileNotFoundException ex) {
            if (directoryHasKbuild) {
                return checkInKbuild(fileName);
            }
        } catch (IOException e) {
            return false;
        } finally {
            if (inputReader != null) {
                inputReader.close();
            }
        }

        return found;
    }

    private boolean checkIncludedFile(String fileName, String makeFileName) throws IOException {

        BufferedReader inputReader = null;
        boolean found = false;
        try {
            inputReader = new BufferedReader(new FileReader(directoryPath + makeFileName));



            String objectName = "";
            String executableName = fileName.substring(0, fileName.indexOf(".c"));
            if (fileName.endsWith(".c")) {
                objectName = fileName.replace(".c", ".o").trim();
            } else if (fileName.endsWith(".S")) {
                objectName = fileName.replace(".S", ".o").trim();
            }


            while (inputReader.ready()) {
                String line = inputReader.readLine();
                line = getNextLines(line, inputReader);
                //   System.out.println("line: " + line);



                Pattern pattern = Pattern.compile("[^a-zA-Z0-9_-]" + objectName + "(\\s*|\\n)");

                Matcher matcher = pattern.matcher(line);

                Pattern cPattern = Pattern.compile("[^a-zA-Z0-9_-]" + fileName + "(\\s*|\\n)");
                Matcher cMatcher = cPattern.matcher(line);
                Pattern execPattern = Pattern.compile("[^a-zA-Z0-9_-](" + executableName + "[\\s*|\\n]|" + executableName + "$)");
                Matcher execMatcher = execPattern.matcher(line);

                if (!line.trim().startsWith("#") && (matcher.find() || cMatcher.find())) {
                    lookForErrors(line, fileName, null, makeFileName);

                    //if found match, no need to check the rest of the file
                    found = true;
                    //  break;
                } else if (execMatcher.find()) {
                    LineFormat format = checkLineFormat(line);
                    if (format.equals(LineFormat.HOSTPROGS_Y_ENTRY) || format.equals(LineFormat.HOSTPROGS_CONFIG_ENTRY)) {

                        lookForErrors(line, fileName, null, makeFileName);
                        found = true;
                        //   break;
                    }
                } else {
                    //check if this line includes another Makefile
                    //include ${srctree}/arch/sh/kernel/Makefile_32
                    if (line.trim().startsWith("include")) {
                        String includedFile = line.substring(7);
                        int lastIndex = includedFile.lastIndexOf("/");
                        includedFile = includedFile.substring(lastIndex + 1);

                        if (!includedFile.contains("$")) {
                            boolean includedFind = checkInMakeFile(fileName, includedFile);
                            if (includedFind) {
                                found = true;
                                break;
                            }
                        }
                    }
                }


                //if reached here then it did not find a match in this line
                //so we check if we can find a match if we substitute the variables
                if (!found && checkVariablesInLine(line, fileName, pattern, cPattern, makeFileName)) {
                    found = true;
                    //break;
                }

            }

        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (inputReader != null) {
                inputReader.close();
            }
        }

        return found;
    }

    private boolean checkInKbuild(String fileName) throws IOException {


        BufferedReader inputReader = null;
        try {
            inputReader = new BufferedReader(new FileReader(directoryPath + "Kbuild"));
            boolean found = false;
            String objectName = fileName.replace(".c", ".o");

            while (inputReader.ready()) {
                String line = inputReader.readLine();
                line = getNextLines(line, inputReader);

                if (!line.trim().startsWith("#") && line.contains(objectName)) {
                    lookForErrors(line, fileName, null, "Kbuild");
                    found = true;
                }
            }

            if (!found) {// && !includedFiles.contains(fileName)) {

                addSyntaxError(new FileMissingInMakeError(fileName, directoryPath));
            } else {
                //found
                foundFiles.add(fileName);
            }
            return found;
        } catch (FileNotFoundException ex) {
            //  if (!includedFiles.contains(fileName)) {
            addSyntaxError(new FileMissingInMakeError(fileName, directoryPath));
            return false;
            //}
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (inputReader != null) {
                inputReader.close();
            }
        }
    }

    private void lookForErrors(String line, String fileName, String previousConfig, String makeFileName) throws IOException {
        switch (checkLineFormat(line)) {
            //Base case: fileName is being used (directly or indirectly) in one
            //of the variables in the implicit rules
            case OBJ_Y_ENTRY:
            case LIB_Y_ENTRY:
            case LIBS_Y_ENTRY:
            case HEAD_Y_ENTRY:
            case INIT_Y_ENTRY:
            case CORE_Y_ENTRY:
            case DRIVERS_Y_ENTRY:
            case NET_Y_ENTRY:
            case HOSTPROGS_Y_ENTRY:
            case OBJS_ENTRY:
                return;
            //varName-y += fileName.o
            //need to check that varName is being used somewhere
            case COMPOSITE_Y_ENTRY:

                checkCompositeYEntry(line, fileName, makeFileName);
                break;
            //varName-objs + fileName.o
            //need to check that varName is being used somewhere
            case EXECUTABLE_ENTRY:
                checkExecutableEntry(line, fileName, makeFileName);
                break;
            //obj-$(CONFIG_x) += fileName.o
            //need to check that CONFIG_X is in the Kconfig files
            case OBJ_CONFIG_ENTRY:
            case HOSTPROGS_CONFIG_ENTRY:
                Vector<String> configs = getConfigsFromLine(line, previousConfig);
                checkInKconfig(configs, fileName);
                break;
            //varName-$(CONFIG_X) += fileName.o
            //need to check that CONFIG_X exists and that varName is being
            //used somewhere
            case COMPOSITE_CONFIG_ENTRY:
                checkCompositeInKconfig(line, fileName, makeFileName);
                break;
            default:
                //if it is an unknown format then break for now & don't report error                
                break;
        }
    }

    private LineFormat checkLineFormat(String line) {
        int assignmentIndex = line.indexOf(":=");

        if (assignmentIndex == -1) {
            assignmentIndex = line.indexOf("+=");
        }

        if (assignmentIndex == -1) {
            assignmentIndex = line.indexOf("=");
        }

        if (assignmentIndex == -1) {
            return LineFormat.UNKNOWN_ENTRY;
        }
        String variable = line.substring(0, assignmentIndex).trim();
        if (variable.equals("obj-y")) {
            return LineFormat.OBJ_Y_ENTRY;
        } else if (variable.equals("head-y")) {
            return LineFormat.HEAD_Y_ENTRY;
        } else if (variable.equals("init-y")) {
            return LineFormat.INIT_Y_ENTRY;
        } else if (variable.equals("core-y")) {
            return LineFormat.CORE_Y_ENTRY;
        } else if (variable.equals("libs-y")) {
            return LineFormat.LIBS_Y_ENTRY;
        } else if (variable.equals("drivers-y")) {
            return LineFormat.DRIVERS_Y_ENTRY;
        } else if (variable.equals("net-y")) {
            return LineFormat.NET_Y_ENTRY;
        } else if (variable.equals("lib-y")) {
            return LineFormat.LIB_Y_ENTRY;
        } else if (variable.equals("hostprogs-y")) {
            return LineFormat.HOSTPROGS_Y_ENTRY;
        } else if (variable.matches("hostprogs-\\$\\(CONFIG_.*\\)")) {
            return LineFormat.HOSTPROGS_CONFIG_ENTRY;
        } else if (variable.matches("obj-\\$\\(CONFIG_.*\\)")) {
            return LineFormat.OBJ_CONFIG_ENTRY;
        } else if (variable.matches("[a-zA-Z0-9_]+-y")) {
            return LineFormat.COMPOSITE_Y_ENTRY;
        } else if (variable.matches("[a-zA-Z0-9_]+-objs")) {
            return LineFormat.EXECUTABLE_ENTRY;
        } else if (variable.matches("[a-zA-Z0-9_]+-\\$\\(CONFIG_.*\\)")) {
            return LineFormat.COMPOSITE_CONFIG_ENTRY;
        } else if (variable.equals("OBJS")) {
            return LineFormat.OBJS_ENTRY;
        } else {
            return LineFormat.UNKNOWN_ENTRY;
        }
    }
}
