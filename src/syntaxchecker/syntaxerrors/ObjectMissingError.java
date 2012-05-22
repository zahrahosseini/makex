/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package syntaxchecker.syntaxerrors;

/**
 *
 * @author snadi
 */
public class ObjectMissingError extends SyntaxError {

    private String objName;
    private static int counter = 0;

    public ObjectMissingError(String fileName, String objName, String directory) {
        super(fileName, directory);
        this.objName = objName;
        
    }

    public static int getCounter() {
        return counter;
    }

    @Override
    public String getMessage() {
        //return "Related object not found error: Object : " + objName + " related to file: " + fileName + " was not found in Makefile";
        return "Dir:" + directory + "::VNU:" + objName + ":" + fileName;// + " was not found in Makefile";
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ObjectMissingError){
            ObjectMissingError toCompare = (ObjectMissingError) obj;
            return toCompare.objName.equals(objName) && toCompare.directory.equals(directory);
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
        return "VNU";
    }


}
