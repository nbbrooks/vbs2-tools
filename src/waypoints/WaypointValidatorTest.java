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

        if (bounds.length != 4) {
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
        } catch (IOException e) {
            System.err.println("Failed to load aerial map at: " + aerialMapLocation);
            String currentDir = new File(".").getAbsolutePath();
            System.err.println("Current directory: " + currentDir);
        }
        while (mapPanel.isPaintingTile()) {
            wait(1000);
        }
    }

    public void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        WaypointValidatorTest waypointValidatorTest = new WaypointValidatorTest();
    }
}
