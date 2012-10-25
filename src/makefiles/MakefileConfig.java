/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package makefiles;

import java.util.Vector;

/**
 *
 * @author snadi
 */
public class MakefileConfig {

    private String configName;
    private boolean controlsDir;
    private boolean controlsFile;
    private Vector<String> directoriesControlled;
    private Vector<String> filesControlled;
    private Vector<String> expressions;

    public MakefileConfig(String configName) {
        this.configName = configName;
        controlsDir = false;
        controlsFile = false;
        directoriesControlled = new Vector<String>();
        filesControlled = new Vector<String>();
        expressions = new Vector<String>();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof String) {
            return ((String) obj).equals(configName);
        }


        final MakefileConfig other = (MakefileConfig) obj;
        if ((this.configName == null) ? (other.configName != null) : !this.configName.equals(other.configName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.configName != null ? this.configName.hashCode() : 0);
        return hash;
    }

    public void addControledFile(String file) {
        if (!filesControlled.contains(file)) {
            filesControlled.add(file);

        }
    }

    public void addControlledDir(String dir) {
        if (!directoriesControlled.contains(dir)) {
            directoriesControlled.add(dir);
        }
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public boolean isControlledDir() {
        return controlsDir;
    }

    public void setControlledDir(boolean controlsDir) {
        this.controlsDir = controlsDir;
    }

    public boolean isControlledFile() {
        return controlsFile;
    }

    public void setControlledFile(boolean controlsFile) {
        this.controlsFile = controlsFile;
    }

    public Vector<String> getDirectoriesControlled() {
        return directoriesControlled;
    }

    public void setDirectoriesControlled(Vector<String> directoriesControlled) {
        this.directoriesControlled = directoriesControlled;
    }

    public Vector<String> getFilesControlled() {
        return filesControlled;
    }

    public void setFilesControlled(Vector<String> filesControlled) {
        this.filesControlled = filesControlled;
    }

    public int getNumOfExpressions(){
        return expressions.size();
    }

    public void addExpression(String expression){
        if(!expressions.contains(expression))
            expressions.add(expression) ;
    }
}

