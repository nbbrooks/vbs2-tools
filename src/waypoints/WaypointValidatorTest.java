/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package waypoints;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.border.LineBorder;

/**
 *
 * @author nbb
 */
public class WaypointValidatorTest {

    protected MapPanel mapPanel;
    protected JFrame mapFrame;

    public WaypointValidatorTest() {
        create2dMap(Map.mapToLocalCorners.get("warminster"), Map.mapToAerialMap.get("warminster"));
    }

    private void create2dMap(double[] bounds, String aerialMapLocation) {
        // Bounds consisis of VBS2 coordiantes for
        //  [ x-left, x-right, y-top, y-bottom ]
        // aerialMap is file name plus extension of file in the src/icons folder
        mapPanel = new MapPanel();
        mapPanel.setPreferredSize(new Dimension(600, 600));
        mapPanel.setBorder(new LineBorder(Color.BLACK));
        mapFrame = new JFrame();
        mapFrame.getContentPane().add(mapPanel, BorderLayout.CENTER);
        mapFrame.pack();
        mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mapFrame.setVisible(true);

        if(bounds.length != 4) {
            System.err.print("Argument bounds is of length " + bounds.length
                    + "; should be length 4.");
            return;
        }
        try {
            File imgFile = new File(aerialMapLocation);
            BufferedImage img = ImageIO.read(imgFile);
            mapPanel.setMapImage(img);
            mapPanel.setMapRect(bounds[0], bounds[1], bounds[2], bounds[3]); // Warminster bounds
            mapPanel.repaint();
        } catch(IOException e) {
            System.err.println("Failed to load aerial map at: " + aerialMapLocation);
            String currentDir = new File(".").getAbsolutePath();
            System.err.println("Current directory: " + currentDir);
        }
        while(mapPanel.isPaintingTile()) {
            wait(1000);
        }
    }

    public void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Interactive
        WaypointValidatorTest waypointValidatorTest = new WaypointValidatorTest();

        // Manual
//        MapPanel map = new MapPanel();
//        ArrayList<Target> targetList = new ArrayList<Target>();
//        ArrayList<Target> targetList2;
//        targetList.add(new Target(2500, 2500, Math.PI / 2, false));
//        targetList.add(new Target(2000, 3200, 7 * Math.PI / 4, false));
//        targetList2 = map.validateWaypoints(targetList);

//        double[] tS = {180, 0, 225, 90, 179, 315, 181, 315};
//        double[] tE = {0, 180, 90, 225, 315, 179, 315, 181};
//        for(int i = 0; i < tS.length; i++) {
//            //System.out.println(tS[i] + "\t" + tE[i] + "\t" + sanitize(tS[i] - 45 + (180 - (tE[i] - tS[i])) / 2));
//            //System.out.println(tS[i] + "\t" + tE[i] + "\t" + sanitize(90 + (180 - (tE[i] - tS[i])) / 2));
//            //System.out.println(tS[i] + "\t" + tE[i] + "\t" + sanitize(tS[i] - 45 + Math.signum(180 - Math.abs(tE[i] - tS[i])) * Math.abs(180 - Math.abs(tE[i] - tS[i])) / 2));
//            //System.out.println(tS[i] + "\t" + tE[i] + "\t" + sanitize(tS[i] - 45 + (180 - Math.abs(tE[i] - tS[i])) / 2));
//            //System.out.println(tS[i] + "\t" + tE[i] + "\t" + sanitize((sanitize(tS[i] + 180) + tE[i]) / 2 - 225));
////            System.out.println(tS[i] + "\t" + tE[i] + "\t" + sanitize((tS[i] + sanitize(tE[i] + 180)) / 2 - 45));
////            System.out.println(tS[i] + "\t" + tE[i] + "\t" + sanitize((tS[i] + tE[i] + 180) / 2 - 45));
//            System.out.println(tS[i] + "\t" + tE[i] + "\t" + (tS[i] + tE[i] + 180) + "\t" + firstLoop(tS[i], tE[i]));
//        }
    }
    
    private static double firstLoop(double t1, double t2) {
        if(t1 > t2) {
            t2 += 180;
        } else if( t1 < t2) {
            t2 -= 180;
        }
        return sanitize((t1 + t2) / 2 - 45);
    }

    private static double sanitize(double d) {
        while(d >= 360) {
            d -= 360;
        }
        while(d < 0) {
            d += 360;
        }
        return d;
    }
}
