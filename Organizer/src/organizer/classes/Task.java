
package organizer.classes;

import java.io.Serializable;

/**
 *
 * @author Ilya
 */
public class Task implements Serializable {
    
    private String name;
    private int id;
    private String performanceNameUser;
    
    public Task (){
        name = null;
        id = IdGenerator.generate();
    }
    
    /**
     *
     * @param nameTask
     */
    public Task (String nameTask){
        this.name = nameTask;
        id = IdGenerator.generate();
        performanceNameUser = null;
    }
    
    /**
     *
     * @param id
     * @param nameTask
     */
    public Task (int id, String nameTask){
        this.id = id;
        this.name = nameTask;
        performanceNameUser = null;
    }
    
    /**
     *
     * @return
     */
    public String getName (){
        return name;
    }
    
    /**
     *
     * @return
     */
    public int getId (){
        return id;
    }

    /**
     *
     * @return
     */
    public String getPerformanceNameUser() {
        return performanceNameUser;
    }
    
    /**
     *
     * @param nameTask
     */
    public void setName (String nameTask){
        this.name = nameTask;
    }
    
    /**
     *
     * @param performanceUser
     */
    public void setPerformanceNameUser(String performanceUser) {
        this.performanceNameUser = performanceUser;
    }
    
    @Override
    public String toString(){
        return name;
    }
}
