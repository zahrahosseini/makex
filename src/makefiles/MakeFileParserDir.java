/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package makefiles;

import makefiles.Utilities.KEYWORDS;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author snadi
 */
public class MakeFileParserDir {

	private PrintWriter outputFile;
    private StringBuilder outputDefinitions;
    private StringBuilder outputClauses;
	private String arch;

	// for statistics
	private static Vector<String> allFiles;
	private static Vector<Integer> dependencySizeVector;
	private static HashMap<String, MakefileConfig> makeFileConfigs;

	private boolean changeOutputStyle;
	private static Vector<String> modules;



	static {
		allFiles = new Vector<String>();
		makeFileConfigs = new HashMap<String, MakefileConfig>();
		dependencySizeVector = new Vector<Integer>();
		modules = new Vector<String>();
	}

	// constructor
	public MakeFileParserDir(String architecture, boolean changeOutputStyle) {
		try {
			arch = architecture;
			this.changeOutputStyle = changeOutputStyle;
            outputClauses = new StringBuilder();
            outputDefinitions = new StringBuilder();
			outputFile = new PrintWriter(new FileOutputStream(new File(
					"models/" + architecture + ".makemodel")));

		} catch (FileNotFoundException ex) {
			Logger.getLogger(MakeFileParserDir.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	// parsing
	private void readLineWithDep(String path, String line,
			BufferedReader inputReader, String previousDependencies,
			String dependency,
			HashMap<String, Vector<CompositeObjectEntry>> variableMap,
			HashMap<String, String> dependencyMap) {

		if (dependency.trim().length() != 0
				&& previousDependencies.trim().length() != 0) {
			// there is a current dependency and a parent dependency
			readLine(path, line, inputReader, "(" + previousDependencies
					+ " && " + dependency + ")", variableMap, dependencyMap);
		} else if (dependency.trim().length() != 0) {
			// found a current dependency but no parent dependencies
			// System.out.println("should read line with dependency: " +
			// dependency);
			readLine(path, line, inputReader, dependency, variableMap,
					dependencyMap);
		} else if (previousDependencies.trim().length() != 0) {
			// found no current dependency but there is a parent dependency
			readLine(path, line, inputReader, previousDependencies,
					variableMap, dependencyMap);
		} else {
			// no parent dependencies & no current dependencies
			readLine(path, line, inputReader, "", variableMap, dependencyMap);
		}
	}


	private String flipDependency(String dependency) {
		if (dependency.startsWith("!")) {
			// System.out.println("flipping dependency: " + dependency + " to "+
			// dependency.substring(1));
			return dependency.substring(1);
		} else {
			return "!" + dependency;
		}
	}

	private void outputVariableMap(
			HashMap<String, Vector<CompositeObjectEntry>> variableMap) {
		System.out.println("VARIABLE MAP");
		Iterator it = variableMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			System.out.println("Key: " + pairs.getKey());
			System.out.println("--------------");
			Vector<CompositeObjectEntry> entries = (Vector<CompositeObjectEntry>) pairs
					.getValue();
			for (CompositeObjectEntry entry : entries) {
				System.out.println(entry);
			}
		}
	}

	private HashMap<String, Vector<CompositeObjectEntry>> constructVariableMap(
			String parentPath, String childPath, boolean checkKbuild,
			boolean isAbsolutePath) throws IOException {
		HashMap<String, Vector<CompositeObjectEntry>> variableMap = new HashMap<String, Vector<CompositeObjectEntry>>();

		BufferedReader inputReader = null;
		try {
			// String path = parentPath;
			String fileName = parentPath;
			if (!isAbsolutePath) {
				StringBuilder path = new StringBuilder();
				path.append(parentPath);
				fileName = path + "Makefile";
				if (checkKbuild) {
					fileName = getFileName(parentPath, childPath, path);
				}

				if (fileName == null) {

					// parentPath + " " + childPath);
					return variableMap;
				}
			}

			// System.out.println("file name in construct: " + fileName +
			// " and path became:  " + path);
			try {
				inputReader = new BufferedReader(new FileReader(fileName));
			} catch (IOException e) {

				// cannot open this file so return empty map without parsing
				return variableMap;
			}

			while (inputReader.ready()) {
				String line = inputReader.readLine();
				// ignore comments
				if (!line.startsWith("#")) {
					if (Utilities.isConditional(line)) {
						// need to fix this
						skipConditional(inputReader);
					} else {
						line = line.trim();

						// old pattern :
						// "[a-zA-Z0-9]+-(objs|y|(\\$\\(CONFIG_.*\\)))(-y)?\\s*[+=|:=|=].*"
						if (line.matches("[a-zA-Z0-9]+(-objs)?-(objs|y|(\\$\\(CONFIG_.*\\)))\\s*[+=|:=|=].*")) {
							// a -y takes precedence over a -objs coz sometimes
							// they both exist

							int assignmentIndex = line.indexOf(":=");
							int assignmentLength = 2;
							if (assignmentIndex == -1) {
								assignmentIndex = line.indexOf("+=");

								if (assignmentIndex == -1) {
									assignmentIndex = line.indexOf("=");
									assignmentLength = 1;
								}
							}

							String firstPart = line.substring(0,
									assignmentIndex);

							int dashIndex = firstPart.lastIndexOf("-y");

							if (dashIndex == -1) {
								dashIndex = firstPart.lastIndexOf("-$");
							}

							if (dashIndex == -1) {
								dashIndex = firstPart.lastIndexOf("-objs");
							}

							String variableBaseName = firstPart.substring(0,
									dashIndex);

							String variableSecondPart = firstPart.substring(
									dashIndex + 1).trim();

							String dependency = "";
							if (variableSecondPart.contains("$")) {
								dependency = variableSecondPart.substring(
										variableSecondPart.indexOf("(") + 1,
										variableSecondPart.indexOf(")"));
							}

							String variableValue = line
									.substring(assignmentIndex
											+ assignmentLength);
							variableValue = getNextLines(variableValue,
									inputReader);

							variableValue = variableValue.replace("$("
									+ variableBaseName + "-y)", "");
							variableValue = variableValue.replace("$("
									+ variableBaseName + "-m)", "");
							variableValue = variableValue.replace("$("
									+ variableBaseName + "-objs-y)", "");
							variableValue = variableValue.replace("$("
									+ variableBaseName + "-objs-m)", "");


							CompositeObjectEntry compositeObjectEntry = new CompositeObjectEntry(
									variableValue, dependency,
									variableSecondPart);
							if (variableMap.containsKey(variableBaseName)) {
								variableMap.get(variableBaseName).add(
										compositeObjectEntry);
							} else {
								Vector<CompositeObjectEntry> vector = new Vector<CompositeObjectEntry>();
								vector.add(compositeObjectEntry);
								variableMap.put(variableBaseName, vector);
							}
						}

					}
				}
			}

			// outputVariableMap(variableMap);
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
					if (Utilities.isConditional(line)) {

						skipConditional(inputReader);
					} else if (line.trim().startsWith("endif")
							|| line.trim().startsWith("endef")) {

						return;
					} else {
					}
				}
			}
			// System.out.println("ERROOOOOR NEVER FOUND CLOSING");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parseConditional(String path, String conditionalLine,
			BufferedReader inputReader, String previousDependencies,
			HashMap<String, Vector<CompositeObjectEntry>> variableMap,
			HashMap<String, String> dependencyMap) {

		try {

		    String dependency = "";


		    if(conditionalLine.startsWith(Utilities.IFNDEF)){
			if(!conditionalLine.contains("CONFIG_")){
			    skipConditional(inputReader);
			    return;
			}
			dependency = "!" + conditionalLine.substring(conditionalLine.indexOf("CONFIG_"));
		    }else if(conditionalLine.startsWith(Utilities.IFDEF)){

			if(!conditionalLine.contains("CONFIG_")){
			    skipConditional(inputReader);
			    return;
			}
			dependency = conditionalLine.substring(conditionalLine.indexOf("CONFIG_"));
		    }else{


			int dollarIndex = conditionalLine.indexOf("$");

			if (dollarIndex == -1) {
				return;
			}

			int bracketIndex = conditionalLine.indexOf(")", dollarIndex);
			dependency = conditionalLine.substring(dollarIndex + 2,
					bracketIndex).trim();

			// let's just focus on CONFIG variables for now
			if (!dependency.startsWith("CONFIG_")) {
				skipConditional(inputReader);
				return;
			}

			int commaIndex = conditionalLine.indexOf(",");
			bracketIndex = conditionalLine.indexOf(")", commaIndex);
			String condition = conditionalLine.substring(commaIndex + 1,
					bracketIndex).trim();

			if (conditionalLine.trim().startsWith(Utilities.IFEQ)) {

				// ifeq ($(dependency), ) ==> !dependency
				if (condition.isEmpty()) {
					dependency = "!" + dependency;
				}

			} else if (conditionalLine.trim().startsWith(Utilities.IFNEQ)) {
				if (condition.equals("y")) {
					dependency = "!" + dependency;
				}
			}
		    }

			while (inputReader.ready()) {
				String line = inputReader.readLine();

				if (!line.trim().equals(Utilities.ENDIF)) {

					if (line.equals(Utilities.ELSE)) {
						// System.out.println("line is else so flipped. Previous dep: "
						// + previousDependencies +" current: " + dependency);
						// read the next line to avoid parsing "else" again
						line = inputReader.readLine();
						readLineWithDep(path, line, inputReader,
								previousDependencies,
								flipDependency(dependency), variableMap,
								dependencyMap);
					} else {
						// line is not else so it is the right line for parsing
						readLineWithDep(path, line, inputReader,
								previousDependencies, dependency, variableMap,
								dependencyMap);
					}
				} else {
					break;
				}
			}
			// System.out.println("ERROOOOOR NEVER FOUND CLOSING");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parseMakeFile(String parentPath, String childPath,
			String previousDependencies, boolean checkKbuild) {
		try {
			HashMap<String, Vector<CompositeObjectEntry>> variableMap = constructVariableMap(
					parentPath, childPath, checkKbuild, false);
			evaluateMakeFile(parentPath, childPath, variableMap,
					previousDependencies, checkKbuild, false);
		} catch (IOException ex) {
			Logger.getLogger(MakeFileParserDir.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void parseMakeFileAbsolute(String makefilePath,
			String previousDependencies, boolean checkKbuild) {
		try {
			// makefiles included by include are absolute paths and we do not
			// need to figure out which makefile to parse in the directory
			HashMap<String, Vector<CompositeObjectEntry>> variableMap = constructVariableMap(
					makefilePath, "", checkKbuild, true);
			evaluateMakeFile(makefilePath, "", variableMap,
					previousDependencies, checkKbuild, true);
		} catch (IOException ex) {
			Logger.getLogger(MakeFileParserDir.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void evaluateMakeFile(String parentPath, String childPath,
			HashMap<String, Vector<CompositeObjectEntry>> variableMap,
			String previousDependencies, boolean checkKbuild, boolean isAbsPath)
			throws IOException {
		BufferedReader inputReader = null;
		// System.out.println("variable map: " + variableMap);
		HashMap<String, String> dependencyMap = new HashMap<String, String>();
		try {
			String fileName = parentPath;
			String path = "";

			if (!isAbsPath) {
				StringBuilder pathBuilder = new StringBuilder();
				pathBuilder.append(parentPath);
				pathBuilder.append(childPath);
				path = pathBuilder.toString();
				fileName = path + "Makefile";
				if (checkKbuild) {
					fileName = getFileName(parentPath, childPath, pathBuilder);
				}

				path = pathBuilder.toString();
			} else {

				//the -1 is because we want the / in the path
				if (parentPath.contains("Makefile"))
					path = parentPath.substring(0,
							parentPath.lastIndexOf("Makefile"));
				else if (parentPath.contains("Kbuild"))
					path = parentPath.substring(0,
							parentPath.lastIndexOf("Kbuild"));

			}

			if (fileName == null) {
				return;
			}

			try {
				inputReader = new BufferedReader(new FileReader(fileName));
			} catch (IOException e) {

				// cannot open this file so return without parsing
				return;
			}


			while (inputReader.ready()) {

				String line = inputReader.readLine();

				// ignore comments
				if (!line.startsWith("#")) {
					if (Utilities.isConditional(line)) {
						// skipConditional(inputReader);
						parseConditional(path.toString(), line, inputReader,
								previousDependencies, variableMap,
								dependencyMap);
					} else {
						readLine(path.toString(), line, inputReader,
								previousDependencies, variableMap,
								dependencyMap);
					}
				}
			}

			writeDependencies(path, dependencyMap);

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (inputReader != null) {
				inputReader.close();
			}
		}
	}



	private void readLine(String path, String line, BufferedReader inputReader,
			String previousDependencies,
			HashMap<String, Vector<CompositeObjectEntry>> variableMap,
			HashMap<String, String> dependencyMap) {
		try {
			// System.out.println("reading line: " + line);
			KEYWORDS keyword = KEYWORDS.NONE;
			if (line.trim().startsWith("include ")) {
				String includedFile = Utilities.getIncludedFile(line, arch);
				parseMakeFileAbsolute(includedFile, previousDependencies, true);
				return;
			}

			if (line.length() != 0) {
				keyword = Utilities.findKeyword(line);
			}
			if (keyword == KEYWORDS.NONE) {
				// no keyword, go to next line
				return;
			}

			parseLine(path.toString(), line, inputReader, previousDependencies,
					variableMap, dependencyMap, keyword);
		} catch (IOException ex) {
			Logger.getLogger(MakeFileParserDir.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void addFiles(String path, String toBeAdded, String dependency,
			HashMap<String, Vector<CompositeObjectEntry>> variableMap,
			HashMap<String, String> dependencyMap, KEYWORDS keyword) {

		StringTokenizer stringTokenizer = new StringTokenizer(toBeAdded);
		String token = "";

		while (stringTokenizer.hasMoreTokens()) {
			token = stringTokenizer.nextToken();

			token = Utilities.replaceVariables(token, arch);

			// check if token is a composite object
			if (variableMap.containsKey(token) && !Utilities.fileExists(token, path)) {

				addCompositeObjects(token, path, dependency, variableMap,
						dependencyMap, keyword);
				continue;
			}

			// if token ends with .o, check first if the part before the .o is a
			// composite object
			// otherwise replace .o with .c and continue processing
			if (token.endsWith(".o")) {
				String tokenName = token.substring(0, token.indexOf(".o"));
				// it is a comosite object
				if (variableMap.containsKey(tokenName)
						&& !Utilities.fileExists(tokenName, path)) {
					addCompositeObjects(tokenName, path, dependency,
							variableMap, dependencyMap, keyword);
					continue;
				}

				token = token.replace(".o", ".c"); // replace .o with .c

			}

			// if token is a variable then we need to look up the variable value
			// from
			// the variable map
			if (token.trim().startsWith("$")) {
				int dashIndex = token.indexOf("-y");

				if (dashIndex == -1) {
					dashIndex = token.indexOf("-$");
				}

				if (dashIndex == -1) {
					dashIndex = token.indexOf("-objs");
				}

				if (dashIndex != -1) {
					String variableName = token.substring(
							token.indexOf("$(") + 2, dashIndex);
					if (variableMap.containsKey(variableName)
							&& !Utilities.fileExists(variableName, path)) {
						// System.out.println("calling composite object with: "
						// + variableName);
						addCompositeObjects(variableName, path, dependency,
								variableMap, dependencyMap, keyword);
						continue;
					}
				}
			}

			token = Utilities.checkToken(token, keyword);

			// we have a final token (not a composite object) so start adding to
			// dependency map
			if (dependencyMap.containsKey(token)) {
				String existingDependency = dependencyMap.get(token);

				if (existingDependency.length() != 0
						&& dependency.length() != 0) {
					dependencyMap.put(token, "(" + existingDependency + " || "
							+ dependency + ")");
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

	private void parseLine(String path, String line,
			BufferedReader inputReader, String previousDependencies,
			HashMap<String, Vector<CompositeObjectEntry>> variableMap,
			HashMap<String, String> dependencyMap, KEYWORDS keyword)
			throws IOException {
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

		String variableSecondPart = line.substring(dashIndex + 1,
				assignmentIndex).trim();
		String dependency = "";
		if (variableSecondPart.contains("$")) {
			int index1 = variableSecondPart.indexOf("(");
			int index2 = variableSecondPart.indexOf(")");

			if (index1 != -1 && index2 != -1) {
				dependency = variableSecondPart.substring(index1 + 1, index2);
			}
		}

		String variableValue = line.substring(assignmentIndex
				+ assignmentLength);

		// avoid lines that set compiler options
		if (variableValue.startsWith("-")) {
			return;
		}

		variableValue = getNextLines(variableValue, inputReader);

		if(dependency.length() !=0 && modules.contains(dependency + "_MODULE")){
		    dependency= "(" + dependency + " || " + dependency + "_MODULE)"; 
		}

		if (dependency.length() != 0 && previousDependencies.length() != 0) {
			// System.out.println("should have both previous : " +
			// previousDependencies + " and current : " + dependency);
			// there is a current dependency and a parent dependency
			addFiles(path, variableValue, "(" + previousDependencies + " && "
					+ dependency +")", variableMap, dependencyMap, keyword);
		} else if (dependency.length() != 0) {
			// found a current dependency but no parent dependencies
			addFiles(path, variableValue, dependency, variableMap,
					dependencyMap, keyword);
		} else if (previousDependencies.length() != 0) {
			// found no current dependency but there is a parent dependency
			addFiles(path, variableValue, previousDependencies, variableMap,
					dependencyMap, keyword);
		} else {
			// no parent dependencies & no current dependencies
			addFiles(path, variableValue, "", variableMap, dependencyMap,
					keyword);

		}
	}

	private static void loadModules() {
	    try{
	    File file = new File("modules.txt");
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		while(reader.ready()){
		    modules.add(reader.readLine().trim());
		}
		
		
	    }catch(FileNotFoundException e){
		//if file is not found, output message to keep items available
		System.out.println("ERROR: Run undertaker-kconfigdump first to extract modules");
		System.exit(0);	
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();	
	    }
	}

	private void addCompositeObjects(String tokenName, String path,
			String dependency,
			HashMap<String, Vector<CompositeObjectEntry>> variableMap,
			HashMap<String, String> dependencyMap, KEYWORDS keyword) {
		// System.out.println("in composite objects: " + tokenName);
		Vector<CompositeObjectEntry> entries = variableMap.get(tokenName);

		for (CompositeObjectEntry entry : entries) {
			String dep = entry.getDependency();
			if (!dep.isEmpty() && !dependency.isEmpty()) {
				// System.out.println("calling add files with : " +
				// entry.getObjects());
				addFiles(path, entry.getObjects(), "(" + dependency + " && "
						+ dep + ")", variableMap, dependencyMap, keyword);
			} else if (!dep.isEmpty()) {
				// System.out.println("calling add files with : " +
				// entry.getObjects());
				addFiles(path, entry.getObjects(), dep, variableMap,
						dependencyMap, keyword);
			} else if (!dependency.isEmpty()) {
				// System.out.println("calling add files with : " +
				// entry.getObjects());
				addFiles(path, entry.getObjects(), dependency, variableMap,
						dependencyMap, keyword);
			} else {
				// System.out.println("calling add files with : " +
				// entry.getObjects());
				addFiles(path, entry.getObjects(), "", variableMap,
						dependencyMap, keyword);
			}

		}
	}

	private String getNextLines(String variableValue, BufferedReader inputReader)
			throws IOException {
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

    private String convertPathToName(String path){
        return path.replaceAll("/", "_").replaceAll("\\.", "_").replaceAll("-", "_");
    }

	// output models
	private void writeDependencies(String path, HashMap<String, String> dependencyMap) {
		String fileToBePrinted = "";
		Iterator it = dependencyMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			String key = (String) pairs.getKey();
			String dependency = (String) pairs.getValue();

			if (key.trim().endsWith("/")) {

                String directoryName = key.substring(0, key.length() -1);
                directoryName = "DIR_" + convertPathToName(path.substring(2) + directoryName);

                if(dependency.length() !=0){
                    outputClauses.append(directoryName + "->" + dependency + "\n")    ;
                    for(String dep: getConfigsFromDependency(dependency)){
                                 outputDefinitions.append("@" + dep + "\n");
                    }
                }


                outputDefinitions.append("@" + directoryName + "\n");
               // outputFile.println(directoryName + " \"" + dependency + "\"");
				parseMakeFile(path, key, directoryName, true);
			} else {

				if (!dependency.isEmpty()) {

					int currentNumOfConfigs = getNumOfConfigs(dependency);
					dependencySizeVector.add(currentNumOfConfigs);

					if (currentNumOfConfigs == 0) {
						System.out.println("strange: " + dependency);
					}

				}

				if (!key.trim().equals("\\")) {

					if (key.contains("$(BITS)")) {

						String newKey = key.replace(("$(BITS)"), "32");
						fileToBePrinted = path + newKey;
						if (changeOutputStyle&& fileToBePrinted.trim().startsWith("./")) {
							fileToBePrinted = convertPathToName(fileToBePrinted.replace("./","FILE_"));
						}




                        outputDefinitions.append("@" + fileToBePrinted + "\n");

						if (dependency.length() != 0) {

                            if (changeOutputStyle) {
                                //outputFile.print(fileToBePrinted);
                                outputClauses.append(fileToBePrinted +"->");
                            } else {
                                outputClauses.append("\"" + fileToBePrinted + "\"") ;
                                //outputFile.print("\"" + fileToBePrinted + "\"");
                            }

							getConfigsFromDependency(dependency, path + newKey,
									false);
							if (changeOutputStyle)
								//outputFile.print(" \"" + dependency + "\"");
                                outputClauses.append(" " + dependency + "\n") ;
							else
								//outputFile.print(" \"(" + dependency + ")\"");
                                outputClauses.append((" \"(" + dependency + ")\"\n"));

						}

						//outputFile.println();

						newKey = key.replace("$(BITS)", "64");

						fileToBePrinted = path + newKey;

						if (changeOutputStyle
								&& fileToBePrinted.trim().startsWith("./")) {
							fileToBePrinted = convertPathToName(fileToBePrinted.replace("./","FILE_"));
						}


                        outputDefinitions.append("@" + fileToBePrinted + "\n");

						if (dependency.length() != 0) {

                            if (changeOutputStyle) {
                                outputClauses.append(fileToBePrinted + "->");
                                // outputFile.print(fileToBePrinted);
                            } else {
                                //outputFile.print("\"" + fileToBePrinted + "\"");
                                outputClauses.append("\"" + fileToBePrinted + "\"");
                            }

							getConfigsFromDependency(dependency, path + newKey,
									false);

							if (changeOutputStyle)
								//outputFile.print(" \"" + dependency + "\"");
                                outputClauses.append(" " + dependency + "\n");
							else
								//outputFile.print(" \"(" + dependency + ")\"");
                                outputClauses.append(" \"(" + dependency + ")\"\n");

						}

						//outputFile.println();
					} else {

						fileToBePrinted = path + key;

						if (changeOutputStyle
								&& fileToBePrinted.trim().startsWith("./")) {

							fileToBePrinted = convertPathToName(fileToBePrinted.replace("./","FILE_"));
						}


                        outputDefinitions.append("@"+ fileToBePrinted + "\n");



						if (dependency.length() != 0) {

                            if (changeOutputStyle) {
                                //outputFile.print(fileToBePrinted);
                                outputClauses.append(fileToBePrinted +"->");
                            } else {
                                //outputFile.print("\"" + fileToBePrinted + "\"");
                                outputClauses.append("\"" + fileToBePrinted + "\"");

                            }

							getConfigsFromDependency(dependency, path + key,
									false);
							if (changeOutputStyle)
								//outputFile.print(" \"" + dependency + "\"");
                                outputClauses.append(" " + dependency + "\n");
							else
								//outputFile.print(" \"(" + dependency + ")\"");
                                outputClauses.append(" \"(" + dependency + ")\"\n");
						}

					//	outputFile.println();
					}
				}
			}
		}

	}

		private String getFileName(String parentPath, String childPath,
			StringBuilder finalPath) {

		String finalPathString = parentPath + childPath;
		String fileName = "";

		// System.out.println("trying "+ finalPath + "Kbuild");
		File file = new File(finalPathString + "Kbuild");

		if (!file.exists()) {
			// System.out.println("trying "+ finalPath + "Makefile");
			file = new File(finalPathString + "Makefile");

			if (!file.exists()) {

				finalPathString = "./" + childPath;
				// System.out.println("trying "+ finalPath + "Kbuild");
				file = new File(finalPathString + "Kbuild");

				if (!file.exists()) {
					// System.out.println("trying "+ finalPath + "Makefile");
					file = new File(finalPathString + "Makefile");

					if (!file.exists()) {
						// System.out.println("nothing");
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


    public void incrementExpressions(Vector<String> detectedConfigs, String dependency){
        for(String config: detectedConfigs){
            MakefileConfig makefileConfig = makeFileConfigs.get(config);

            if(makefileConfig != null){
                makefileConfig.addExpression(dependency)    ;
            }
        }
    }


	public void closeFile() {
		outputFile.close();
	}



	// statistics
	private void addFileToTotal(String fileName) {
		if (!allFiles.contains(fileName)) {
			allFiles.add(fileName);
		}
	}


	// statistics helpers
	private int getNumOfConfigs(String dependency) {

		Pattern pattern = Pattern.compile("CONFIG_[_a-zA-Z0-9]*");
		Matcher matcher = pattern.matcher(dependency);
		Vector<String> configsInDep = new Vector<String>();
		while (matcher.find()) {
			String config = dependency
					.substring(matcher.start(), matcher.end());
			if (!configsInDep.contains(config)) {
				configsInDep.add(config);
			}
		}

		return configsInDep.size();
	}

    private Vector<String> getConfigsFromDependency(String dependency) {

        Pattern pattern = Pattern.compile("CONFIG_[_a-zA-Z0-9]*");
        Matcher matcher = pattern.matcher(dependency);
        Vector<String> configsInDep = new Vector<String>();
        while (matcher.find()) {
            String config = dependency
                    .substring(matcher.start(), matcher.end());
            if (!configsInDep.contains(config)) {
                configsInDep.add(config);
            }
        }

        return configsInDep;
    }

	private void getConfigsFromDependency(String dependency, String fileOrDir,
			boolean isDirectory) {

		Pattern pattern = Pattern.compile("CONFIG_[_a-zA-Z0-9]*");
		Matcher matcher = pattern.matcher(dependency);
        Vector<String> detectedConfigs = new Vector<String>()    ;

		while (matcher.find()) {

			String config = dependency
					.substring(matcher.start(), matcher.end());


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

            //to avoid counting the same expression twice if it has the same feature appearing in it since we don't simplify
            if(!detectedConfigs.contains(config)){
                detectedConfigs.add(config);
            }
        }

        incrementExpressions(detectedConfigs, dependency);

	}

	public static void main(String args[]) {
		BufferedReader reader = null;
		try {
			Utilities.writeArchNames();

			// this changes the output style to be similar to golem so we can
			// compare
			boolean changeOutputStyle = false;

			if (args.length > 0) {
				System.out.println("arg:" + args[0]);
				changeOutputStyle = Boolean.valueOf(args[0]);
			}

			File file = new File("archNames.txt");
			reader = new BufferedReader(new FileReader(file));
			MakeFileParserDir.loadModules();
			while (reader.ready()) {
				String arch = reader.readLine().trim();
				System.out.println("parsing for arch: "+ arch);
				MakeFileParserDir constructor = new MakeFileParserDir(arch, changeOutputStyle);

				// if (args.length == 0) {
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
				// }// else {
				// constructor.parseMakeFile(args[0], "", "", false);
				// }
                constructor.writeOutput();
				constructor.closeFile();



			//	constructor.printIndivStatistics();

			}

		//	printOverallStatistics();

		} catch (IOException ex) {
			Logger.getLogger(MakeFileParserDir.class.getName()).log(Level.SEVERE,
					null, ex);
		} finally {
			try {
				reader.close();
			} catch (IOException ex) {
				Logger.getLogger(MakeFileParserDir.class.getName()).log(
						Level.SEVERE, null, ex);
			}
		}
	}

    private void writeOutput() {

        for (String config : makeFileConfigs.keySet()) {
            outputDefinitions.append("@" + config + "\n");
        }
        outputFile.println(outputDefinitions.toString());
        outputFile.println(outputClauses.toString());
    }
}
