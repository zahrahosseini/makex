/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package syntaxchecker.syntaxerrors;

/**
 *
 * @author snadi
 */
public abstract class SyntaxError {

    protected String fileName;
    protected String startRelease;
    protected String endRelease;
    protected String directory;
    protected REASON reason;
    public enum REASON {removed_file, added_to_Makefile, added_to_C_file, added_defintion, removed_feature_dependency, new_feature_dependency};
    protected String extraInfo;
    protected int errorID;

    public int getErrorID() {
        return errorID;
    }

    public void setErrorID(int errorID) {
        this.errorID = errorID;
    }

    

    public abstract void incrementCounter();

    public abstract void decrementCounter();

    public abstract String getErrorCode();

    public SyntaxError(String fileName, String directory) {
        this.fileName = fileName;
        this.directory = directory;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public REASON getReason() {
        return reason;
    }

    public void setReason(REASON reason) {
        this.reason = reason;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }



    public int getStatus(){

        //if we know both the start & end then we observed this anomaly die
        //i.e. dead at end of study
        if (startRelease != null && endRelease != null) {
            return 1;
        }else if(startRelease != null && endRelease == null){
            //if we know the start release, but dunno the end, then this is right censored
            //i.e. alive at end of study
            return 0;
        }

        return -1;
    }
    public int calculateAge() {
        int age = -1;
        if (startRelease != null && endRelease != null) {            
            int start = Integer.valueOf(startRelease.substring(startRelease.lastIndexOf(".") + 1));
            int end = Integer.valueOf(endRelease.substring(endRelease.lastIndexOf(".") + 1));            
            age = end - start;
            if(age < 0){
                System.out.println("STRAAANGE: START: " + start + " end: " + end);
            }
        }
//        }else if(startRelease != null && endRelease == null){
//            int start = Integer.valueOf(startRelease.substring(startRelease.lastIndexOf(".") + 1));
//
//            //if they are still alive at time of study, then we know that their age is at least till
//            //the next release after 2.6.39 i.e. if introduced in 2.6.37 ad it is still alive in 2.6.39
//            //then it is at least going to be 3 releases old when it dies
//
//            //scratch that -- shouldn't add the 1
//            age = 39 - start;
//        }

        return age;
        
    }

    public String getEndRelease() {
        return endRelease;
    }

    public void setEndRelease(String endRelease) {
        this.endRelease = endRelease;
    }

    public String getStartRelease() {
        return startRelease;
    }

    public void setStartRelease(String startRelease) {
        this.startRelease = startRelease;
    }

    public abstract String getMessage();
    

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStartReleaseCode() {
        return startRelease.substring(startRelease.lastIndexOf(".") + 1);
    }

    public String getEndReleaseCode() {
        return endRelease.substring(endRelease.lastIndexOf(".") + 1);
    }
}
