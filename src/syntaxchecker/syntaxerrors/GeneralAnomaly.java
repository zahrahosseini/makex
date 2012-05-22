/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package syntaxchecker.syntaxerrors;

/**
 *
 * @author snadi
 */
public class GeneralAnomaly {

    private int id;
    private String type;
    private String start;
    private String end;
    private int age;
    private boolean inSmallerDataSet;
    private String description;

    public GeneralAnomaly(String description, String type, String start, String end, int age){
        this.description = description;
        this.type = type;
        this.age = age;
        this.start = start;
        this.end = end;
        inSmallerDataSet = false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeneralAnomaly other = (GeneralAnomaly) obj;
        if ((this.description == null) ? (other.description != null) : !this.description.equals(other.description)) {
            return false;
        }
        return true;
    }




    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isInSmallerDataSet() {
        return inSmallerDataSet;
    }

    public void setInSmallerDataSet(boolean inSmallerDataSet) {
        this.inSmallerDataSet = inSmallerDataSet;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }



}
