/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package syntaxchecker.syntaxerrors;

/**
 *
 * @author snadi
 */
public class KConfigMissingError extends SyntaxError {

    String kconfigDependency;
    static int counter = 0;

    public KConfigMissingError(String fileName, String kconfigDependency, String directory) {
        super(fileName, directory);
        this.kconfigDependency = kconfigDependency;
        
    }

    public String getKconfigDependency() {
        return kconfigDependency;
    }

    public void setKconfigDependency(String kconfigDependency) {
        this.kconfigDependency = kconfigDependency;
    }
    


     public static int getCounter() {
        return counter;
    }

    @Override
    public String getMessage() {
       //return  "Config missing in KConfig error: Cannot find " + kconfigDependency + " that is needed for file " + fileName + " in the makefile";
         return  "Dir:" + directory + "::FND:" + kconfigDependency + ":" + fileName;// + " in the makefile";
    }

     @Override
    public boolean equals(Object obj) {
        if(obj instanceof KConfigMissingError){
            KConfigMissingError toCompare = (KConfigMissingError) obj;
            return toCompare.kconfigDependency.equals(kconfigDependency) && toCompare.directory.equals(directory) && toCompare.fileName.equals(fileName);
        }

        return false;
    }

    @Override
    public void incrementCounter() {
        counter++;
    }

     @Override
    public void decrementCounter() {
        counter--;
    }

    @Override
    public String getErrorCode() {
      return "FND";
    }

}
