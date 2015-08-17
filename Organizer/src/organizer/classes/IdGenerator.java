
package organizer.classes;

/**
 *
 * @author Ilya
 */
public class IdGenerator {
    
    static int generate() {
        int id = (int) (Double.doubleToLongBits(Math.random() * 1000) ^ (System.currentTimeMillis()));
        if (id<0) id *= -1;
        return id;
    }
}
