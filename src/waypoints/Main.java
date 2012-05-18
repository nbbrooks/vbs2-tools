package waypoints;

///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package com.perc.simulationcontrol.vbs2.main;
//
//import com.perc.simulationcontrol.models.AssetInfo;
//import com.perc.simulationcontrol.models.AssetSystemInfo;
//import com.perc.simulationcontrol.models.AssetSystemInfo.AssetSystemType;
//import com.perc.simulationcontrol.models.GroundForceInfo;
//import com.perc.simulationcontrol.models.Location;
//import com.perc.simulationcontrol.models.Orientation;
//import com.perc.simulationcontrol.models.Position;
//import com.perc.simulationcontrol.models.UTMCoordinate;
//import com.perc.simulationcontrol.models.Waypoint;
//import com.perc.simulationcontrol.vbs2.VBS2SimulationControl;
//import com.perc.simulationcontrol.vbs2.Vbs2Scripts;
//import com.perc.simulationcontrol.vbs2.Vbs2Utils;
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Dimension;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//import javax.imageio.ImageIO;
//import javax.swing.JFrame;
//import javax.swing.border.LineBorder;
//import com.perc.simulationcontrol.vbs2.models.Pose4DOF;
//import com.perc.simulationcontrol.vbs2.gui.FrameManager;
//import com.perc.simulationcontrol.vbs2.gui.MapPanel;
//import com.perc.simulationcontrol.vbs2.models.Map;
//import com.perc.simulationcontrol.vbs2.connection.Vbs2Handler;
//import com.perc.simulationcontrol.vbs2.connection.Vbs2Link;
//
///**
// * @author nbb
// */
//public class Main {
//
//    // VBS2 INSTANCE MANAGEMENT VARIABLES
//    // In the case of multiple VBS2 instances, vbs2Hosts[0] must be the server
//    //  instance
//    String[] vbs2Hosts;
//    int[] vbs2Ports;
//    //
//    // GUI VARIABLES
//    FrameManager frameManager;
//    JFrame mapFrame;
//    MapPanel mapPanel;
//    GUIType guiType = GUIType.FEED;
//    //
//    // ASSET MANAGEMENT VARIABLES
//    public static String baseFolder = "VBS2SimulationIntegrator/";
//    String uavResourceName = "vbs2_us_scaneagle2";
//    /**
//     * VBS2 uses a right handed coordinate system where +x is East, +y is North,
//     *  +z is up, and heading is degrees clockwise from North.
//     * Start heading is not used because VBS2 has no reliable method that I
//     *  know of for changing the heading of a moving vehicle. So instead I
//     *  played around with the offset from the center location so that once the
//     *  UAVs get turned around they will be roughly evenly spaced around the
//     *  loiter waypoint that is assigned
//     */
//    public static final int UAV_FLY_IN_HEIGHT = 200; // in m
//    public static final Pose4DOF[] uavStartPoses = {
//        new Pose4DOF(1755 - 1100, 2513 - 1100, UAV_FLY_IN_HEIGHT, 0),
//        new Pose4DOF(2155 - 1100, 2563 - 1100, UAV_FLY_IN_HEIGHT, 0),
//        new Pose4DOF(2155 - 1100, 2113 - 1100, UAV_FLY_IN_HEIGHT, 0),
//        new Pose4DOF(1755 - 1100 + 550, 2513 + 1100, UAV_FLY_IN_HEIGHT, 90),
//        new Pose4DOF(2155 - 1100 + 550, 2563 + 1100, UAV_FLY_IN_HEIGHT, 90),
//        new Pose4DOF(2155 - 1100 + 550, 2113 + 1100, UAV_FLY_IN_HEIGHT, 90),
//        new Pose4DOF(1755 + 1100, 2513 + 1100 - 250, UAV_FLY_IN_HEIGHT, 180),
//        new Pose4DOF(2155 + 1100, 2563 + 1100 - 250, UAV_FLY_IN_HEIGHT, 180),
//        new Pose4DOF(2155 + 1100, 2113 + 1100 - 250, UAV_FLY_IN_HEIGHT, 180),
//        new Pose4DOF(1755 + 1100, 2513 - 1100, UAV_FLY_IN_HEIGHT, 270),
//        new Pose4DOF(2155 + 1100, 2563 - 1100, UAV_FLY_IN_HEIGHT, 270),
//        new Pose4DOF(2155 + 1100, 2113 - 1100, UAV_FLY_IN_HEIGHT, 270),
//        new Pose4DOF(1755 - 1100, 2513, UAV_FLY_IN_HEIGHT, 45),
//        new Pose4DOF(2155 - 1100, 2563, UAV_FLY_IN_HEIGHT, 45),
//        new Pose4DOF(2155 - 1100, 2113, UAV_FLY_IN_HEIGHT, 45),
//        new Pose4DOF(1755, 2513 + 1100, UAV_FLY_IN_HEIGHT, 135),
//        new Pose4DOF(2155, 2563 + 1100, UAV_FLY_IN_HEIGHT, 135),
//        new Pose4DOF(2155, 2113 + 1100, UAV_FLY_IN_HEIGHT, 135),
//        new Pose4DOF(1755 + 1100, 2513, UAV_FLY_IN_HEIGHT, 225),
//        new Pose4DOF(2155 + 1100, 2563, UAV_FLY_IN_HEIGHT, 225),
//        new Pose4DOF(2155 + 1100, 2113, UAV_FLY_IN_HEIGHT, 225),
//        new Pose4DOF(1755, 2513 - 1100, UAV_FLY_IN_HEIGHT, 315),
//        new Pose4DOF(2155, 2563 - 1100, UAV_FLY_IN_HEIGHT, 315),
//        new Pose4DOF(2155, 2113 - 1100, UAV_FLY_IN_HEIGHT, 315),};
//
//    /**
//     * Controls what GUI items are created/displayed
//     * FEED: Receives and displays video, displays 2D map
//     * SUAVE: Receives and displays video, integrates with SUAVE
//     * VIDEO_TEST: Receives and displays video
//     * RECORD: Receives and records video, but does not display it
//     * VIDEO_RECEIVE: Receives video, but does not display it
//     */
//    public enum GUIType {
//
//        FEED, SUAVE, VIDEO_TEST, RECORD, VIDEO_RECEIVE;
//    };
//
//    public static void main(String[] args) {
//        if (args.length == 0) {
//            // This is for recording from a single VBS2 machine using the first
//            //  set of waypoints
//            new Main();
//            // This will delete the tracking scripts so the TCP link isn't full of tracking messages
////            Vbs2Link link = new Vbs2Link();
////            link.connect("localhost");
////            Vbs2Scripts.deleteTracking(link);
//        }
//    }
//
//    public Main() {
//        List<Waypoint> wps;
//        Location location;
//        VBS2SimulationControl control = new VBS2SimulationControl("localhost");
//
//        // Create 2D map
//        //create2dMap(Map.mapToLocalCorners.get("warminster"), Map.mapToAerialMap.get("warminster"));
//        Vbs2Handler vbs2Handler = new Vbs2Handler(mapPanel);
//        control.getVbs2Sim().getServerLink().addMessageListener(vbs2Handler);
//
//        // UAV manipulation
//        AssetInfo asset1 = control.createAsset();
//        AssetInfo asset2 = control.createAsset();
//        AssetInfo asset3 = control.createAsset();
//        // Adding waypoints
//        location = Vbs2Scripts.localToUTMCoord(control.getVbs2Sim().getServerLink(), 2500, 1000, 500);
//        control.addAssetWaypoint(asset1.getAssetId(), new Waypoint(location));
//        location = Vbs2Scripts.localToUTMCoord(control.getVbs2Sim().getServerLink(), 2500, 1200, 1000);
//        control.addAssetWaypoint(asset1.getAssetId(), new Waypoint(location));
//        location = Vbs2Scripts.localToUTMCoord(control.getVbs2Sim().getServerLink(), 2500, 2500, 100);
//        control.addAssetWaypoint(asset2.getAssetId(), new Waypoint(location));
//        location = Vbs2Scripts.localToUTMCoord(control.getVbs2Sim().getServerLink(), 1000, 2500, 100);
//        control.addAssetWaypoint(asset3.getAssetId(), new Waypoint(location));
//        // Getting list of waypoints
//        wps = control.getAssetWaypoints(asset1.getAssetId());
//        System.out.println("There are " + wps.size() + " waypoints");
//        for (Waypoint wp : wps) {
//            System.out.println(wp);
//        }
//        wait(2500);
//        // Clearing waypoints, causing it to loiter about its current location
//        control.clearAssetWaypoint(asset1.getAssetId());
//        wps = control.getAssetWaypoints(asset1.getAssetId());
//        System.out.println("There are " + wps.size() + " waypoints");
//        for (Waypoint wp : wps) {
//            System.out.println(wp);
//        }
//
//        // Ground force manipulation
//        GroundForceInfo groundForce1 = control.createGroundForce(false);
//        location = Vbs2Scripts.localToUTMCoord(control.getVbs2Sim().getServerLink(), 2500, 2500, 0);
//        control.setGroundForceLocation(groundForce1.getGroundForceID(), location);
//
//        // Camera manipulation
//        // Creating cameras
//        // Create camera 1, switch cameras to RGB mode
//        AssetSystemInfo assetSystem1 = control.createAssetSystem(asset1.getAssetId(), AssetSystemType.RGB);
//        // Set camera position and orientation
//        // IMPORTANT: Do NOT set camera pitch to +-90, it gets confused about
//        //  the plane banking and the camera spins around rapidly. Use +-89 or +-91
//        Position position1 = new Position(0, 0, -5);
//        Orientation orientation1 = new Orientation(0, -89, 0);
//        control.setAssetSystemPosition(assetSystem1.getAssetSystemID(), position1);
//        control.setAssetSystemOrientation(assetSystem1.getAssetSystemID(), orientation1);
//        wait(2500);
//        // Create camera 2, switch cameras to night vision mode
//        AssetSystemInfo assetSystem2 = control.createAssetSystem(asset2.getAssetId(), AssetSystemType.NV);
//        Position position2 = new Position(0, 0, -5);
//        Orientation orientation2 = new Orientation(0, -89, 0);
//        control.setAssetSystemPosition(assetSystem2.getAssetSystemID(), position2);
//        control.setAssetSystemOrientation(assetSystem2.getAssetSystemID(), orientation2);
//        wait(2500);
//        // Create camera 3, switch cameras to thermal imaging (white hot) mode
//        AssetSystemInfo assetSystem3 = control.createAssetSystem(asset2.getAssetId(), AssetSystemType.TI_WHITE_HOT);
//        Position position3 = new Position(0, 0, -5);
//        Orientation orientation3 = new Orientation(0, 1, 0);
//        control.setAssetSystemPosition(assetSystem3.getAssetSystemID(), position3);
//        control.setAssetSystemOrientation(assetSystem3.getAssetSystemID(), orientation3);
//        // Get latest BufferedImage from camera 1
//        BufferedImage temp = control.getAssetSystemImagery(assetSystem1.getAssetSystemID());
//        // Disable camera 1 for 2.5 seconds
//        control.disableAssetSystem(assetSystem1.getAssetSystemID());
//        wait(2500);
//        control.enableAssetSystem(assetSystem1.getAssetSystemID());
//
//        // Weather manipulation
//        control.changeWeatherToRainy();
//        wait(2500);
//        control.changeWeatherToClear();
//    }
//
//    private void create2dMap(double[] bounds, String aerialMapLocation) {
//        // Bounds consisis of VBS2 coordiantes for
//        //  [ x-left, x-right, y-top, y-bottom ]
//        // aerialMap is file name plus extension of file in the src/icons folder
//        mapPanel = new MapPanel();
//        mapPanel.setPreferredSize(new Dimension(600, 600));
//        mapPanel.setBorder(new LineBorder(Color.BLACK));
//        mapFrame = new JFrame();
//        mapFrame.getContentPane().add(mapPanel, BorderLayout.CENTER);
//        mapFrame.pack();
//        mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        mapFrame.setVisible(true);
//
//        if (bounds.length != 4) {
//            System.err.print("Argument bounds is of length " + bounds.length
//                    + "; should be length 4.");
//            return;
//        }
//        try {
//            File imgFile = new File(aerialMapLocation);
//            BufferedImage img = ImageIO.read(imgFile);
//            mapPanel.setMapImage(img);
//            mapPanel.setMapRect(bounds[0], bounds[1], bounds[2], bounds[3]); // Warminster bounds
//            mapPanel.repaint();
//        } catch (IOException e) {
//            System.err.println("Failed to load aerial map at: " + aerialMapLocation);
//            String currentDir = new File(".").getAbsolutePath();
//            System.err.println("Current directory: " + currentDir);
//        }
//        while (mapPanel.isPaintingTile()) {
//            wait(1000);
//        }
//    }
//
//    public void wait(int ms) {
//        try {
//            Thread.sleep(ms);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void createFrameManager() {
//        frameManager = new FrameManager();
////        FrameManager.restoreLayout();
//        frameManager.setVisible(true);
//    }
//}
