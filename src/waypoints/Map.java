/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package waypoints;

import java.util.Hashtable;

/**
 *
 * @author nbb
 */
public class Map {
    public static String baseFolder = "";

    // GPS <-> LOCAL CONVERSION VARIABLES
    public static final Hashtable<String, double[]> mapToGPSCorners = new Hashtable<String, double[]>();
    static {
        // Format is { Longitude start, Longitude end, Latitude start, Latitude end }
        mapToGPSCorners.put("warminster", new double[] {51.2056236267, 51.2500648499, -2.1940853596, -2.1216654778});
    }
    public static final Hashtable<String, double[]> mapToLocalCorners = new Hashtable<String, double[]>();
    static {
        // Format is { x start, x end, y start, y end }
        // x start and y start should be 0.0
        mapToLocalCorners.put("warminster", new double[] {0.0, 5000.0, 0.0, 5000.0});
    }
    public static final Hashtable<String, String> mapToAerialMap = new Hashtable<String, String>();
    static {
        mapToAerialMap.put("warminster", baseFolder + "media/warminster-danger-small.jpg");
    }

    // @TODO: These methods are exactly the same, except they use different constants.
    // The different constants are ALMOST exactly the same, except the '2' set has END_WEST
    // and START_WEST  the opposite of the non '2' set.  Yay.  For what it's worth, I think the '2'
    // set is what is actually being used.  
    public static void gpsToLocal(double[] telemetry, double[] eastNorth, String mapName) {
        // telemetry[0] is position +x (north) or longitude depending on mode
        // telemetry[1] is position +y (east) or latitude depending on mode
        // Assuming we are in longitude/latitude mode,
        double[] gpsCorners = mapToGPSCorners.get(mapName);
        double[] localCorners = mapToLocalCorners.get(mapName);
        if(gpsCorners == null || localCorners == null) {
            System.out.println("Could not find corners for mapname '" + mapName + "'");
            return;
        }
        eastNorth[0] = (telemetry[0] - gpsCorners[0]) / (gpsCorners[1] - gpsCorners[0]) * (localCorners[1] - localCorners[0]) + localCorners[0];
        eastNorth[1] = (telemetry[1] - gpsCorners[2]) / (gpsCorners[3] - gpsCorners[2]) * (localCorners[3] - localCorners[2]) + localCorners[2];
    }

    public static void localToGps(double[] eastNorth, double[] telemetry, String mapName) {
        double[] gpsCorners = mapToGPSCorners.get(mapName);
        double[] localCorners = mapToLocalCorners.get(mapName);
        if(gpsCorners == null || localCorners == null) {
            System.out.println("Could not find corners for mapname '" + mapName + "'");
            return;
        }
        telemetry[0] = (((eastNorth[0] - localCorners[0]) / (localCorners[1] - localCorners[0])) * (gpsCorners[1] - gpsCorners[0])) + gpsCorners[0];
        telemetry[1] = (((eastNorth[1] - localCorners[2]) / (localCorners[3] - localCorners[2])) * (gpsCorners[3] - gpsCorners[2])) + gpsCorners[2];
    }
}
