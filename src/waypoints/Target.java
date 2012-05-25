/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package waypoints;

/**
 *
 * @author nbb
 */
public class Target {

    String key;
    boolean validated = false;
    boolean highlighted = false;
    boolean selected = false;
    double x;
    double y;
    double theta;
    double lat;
    double lon;

    public Target() {
    }

    public Target(double x, double y, double theta, boolean validated) {
        this.x = x;
        this.y = y;
        this.theta = theta;
        this.validated = validated;
        key = "";
    }

    public void setPos(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public String toString() {
        return "[" + x + ", " + y + ", " + theta + ", " + (theta * 180.0 / Math.PI) + ", " + validated + "]";
    }
}