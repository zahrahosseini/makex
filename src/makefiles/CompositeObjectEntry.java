/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package makefiles;

/**
 *
 * @author snadi
 */
public class CompositeObjectEntry {
    
    private String objects;
    private String dependency;
    private String extension;

    @Override
    public String toString() {
        return "Variable value: " + objects + " depend on " + dependency;
    }


    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
    

    public CompositeObjectEntry(String objects, String dependency, String extension) {
        this.objects = objects;
        this.dependency = dependency;
        this.extension = extension;
    }

    public CompositeObjectEntry(String objects) {
        this.objects = objects;
        dependency = "";
    }

    public String getDependency() {
        return dependency;
    }

    public void setDependency(String dependency) {
        this.dependency = dependency;
    }

    public String getObjects() {
        return objects;
    }

    public void setObjects(String objects) {
        this.objects = objects;
    }
    
    


}
