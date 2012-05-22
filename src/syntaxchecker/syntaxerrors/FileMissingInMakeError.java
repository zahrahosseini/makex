/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package syntaxchecker.syntaxerrors;

/**
 *
 * @author snadi
 */
public class FileMissingInMakeError extends SyntaxError{

    private static int counter = 0;
    private REASON reason;

    public FileMissingInMakeError(String fileName, String directory){
        super(fileName, directory);
        
        
    }



    public static int getCounter() {
        return counter;
    }

    public static void setCounter(int counter) {
        FileMissingInMakeError.counter = counter;
    }

    @Override
    public String getMessage() {
        //return "File missing in makefile error: Cannot find " + fileName + " in Makefile";
         return "Dir:" + directory + "::FNU:" + fileName ;//+ " in Makefile";
    }

     @Override
    public boolean equals(Object obj) {
        if(obj instanceof FileMissingInMakeError){
            FileMissingInMakeError toCompare = (FileMissingInMakeError) obj;
            return toCompare.fileName.equals(fileName) && toCompare.directory.equals(directory);
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
       return "FNU";
    }
}
