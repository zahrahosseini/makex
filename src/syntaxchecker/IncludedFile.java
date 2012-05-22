/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package syntaxchecker;

/**
 *
 * @author snadi
 */
public class IncludedFile {

    private String includedFileName;

    private String includingFileName;

    public IncludedFile(String includedFileName, String includingFileName) {
        this.includedFileName = includedFileName;
        this.includingFileName = includingFileName;
    }

    public String getIncludedFileName() {
        return includedFileName;
    }

    public void setIncludedFileName(String includedFileName) {
        this.includedFileName = includedFileName;
    }

    public String getIncludingFileName() {
        return includingFileName;
    }

    public void setIncludingFileName(String includingFileName) {
        this.includingFileName = includingFileName;
    }

    @Override
    public boolean equals(Object obj) {
        return ((IncludedFile) obj).getIncludedFileName().equals(includedFileName);
    }



}
