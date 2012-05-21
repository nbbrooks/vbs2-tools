/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package waypoints;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

/**
 * A display widget that can display a map background and icon overlays.  It
 * support mouse-based panning and zooming.
 * @author pkv
 */
public class MapPanel extends JPanel {

    private static final Logger logger = Logger.getLogger(MapPanel.class.getName());
    private static double MIN_DISTANCE = 100; //m
    private static double ANGLE_INCREMENT = Math.PI / 4; //rad
    private static final int TARGET_RADIUS = 100;
    private static final double TARGET_SELECT_RANGE_SQD = TARGET_RADIUS * TARGET_RADIUS;
    private static final int TARGET_LENGTH = 100;
    private static final Color TARGET_COLOR = Color.RED;
    private static final Color TARGET_SELECTED_COLOR = Color.WHITE;
    private static final Color TARGET_HIGHLIGHTED_COLOR = Color.YELLOW;
    public static final double SCALE_FACTOR = 1.2;
    private final LinkedHashMap<String, MapIcon> icons = new LinkedHashMap<String, MapIcon>();
    private Image background;
    private final Point2D mapOffset = new Point2D.Double(0.0, 0.0);
    private final Point2D mapScale = new Point2D.Double(1.0, 1.0);
    private final AffineTransform mapImageTransform = new AffineTransform();
    private Point currPoint;
    private Point downPoint;
    private final Point2D offset = new Point2D.Double(0.0, 0.0);
    private final Point2D scale = new Point2D.Double(1.0, 1.0);
    private final MapMouseListener mt = new MapMouseListener();
    private final MapComponentListener cl = new MapComponentListener();
    public static final double SET_ICON_SCALE = 2.0;
    private int targetCreationStep = 0;

    {
        this.addMouseListener(mt);
        this.addMouseMotionListener(mt);
        this.addMouseWheelListener(mt);
        this.addComponentListener(cl);
    }

    private class MapIcon {

        Image icon;
        final AffineTransform xform = new AffineTransform();
    }

    public void setMapRect(double left, double right, double top, double bottom) {
        mapOffset.setLocation(-(left + right) / 2, -(top + bottom) / 2);
        //mapScale.setLocation(1 / (right - left), 1 / (bottom - top));
        // Reverse this to fix difference between VBS2 coordinate frame and screen coordinate frame
        mapScale.setLocation(1 / (right - left), 1 / (top - bottom));
    }

    public void setMapImage(Image img) {
        background = img;

        int width = background.getWidth(this);
        int height = background.getHeight(this);
        if(width > 0 && height > 0) {
            setMapTransform(width, height);
        } else {
            mapImageTransform.setToIdentity();
            System.err.println("Need to use ImageObserver to set transform.");
        }
    }

    private void setMapTransform(int width, int height) {
        mapImageTransform.setToIdentity();
        mapImageTransform.scale(1 / (double) width, 1 / (double) height);
        mapImageTransform.translate(-width / 2, -height / 2);
    }

    public void setIcon(String name, Image img, double s, double x, double y, double th) {
        MapIcon mi = getIcon(name);
        mi.icon = img;
        setIconTransform(mi, x, y, th, s);
        this.repaint();
    }

    public void setIcon(String name, Image img, double x, double y) {
        setIcon(name, img, 1.0, x, y, 0.0);
    }

    public void setIcon(String name, double x, double y, double t) {
        MapIcon mi = getIcon(name);
        setIconTransform(mi, x, y, t, 1.0);
        this.repaint();
    }

    private MapIcon getIcon(String name) {
        synchronized(icons) {
            if(icons.containsKey(name)) {
                return icons.get(name);
            } else {
                MapIcon mi = new MapIcon();
                icons.put(name, mi);
                return mi;
            }
        }
    }

    private void setIconTransform(MapIcon mi, double x, double y, double theta, double scale) {
        int width = mi.icon.getWidth(null);
        int height = mi.icon.getHeight(null);

        mi.xform.setToIdentity();
        mi.xform.translate(x, y);
        mi.xform.rotate(theta);
        mi.xform.translate(-scale * width / 2, -scale * height / 2);
        mi.xform.scale(scale, scale);
    }

    public ArrayList<Target> getWaypointList() {
        return waypointList;
    }

    public void removeIcon(String name) {
        synchronized(icons) {
            icons.remove(name);
            this.repaint();
        }
    }

    private void buildTransform1(Graphics2D g2) {
        // Apply transform from frame coords to image coords
        g2.translate(this.getWidth() / 2, this.getHeight() / 2);
        if(downPoint != null && currPoint != null) {
            g2.translate(currPoint.getX() - downPoint.getX(),
                    currPoint.getY() - downPoint.getY());
        }
        g2.scale(scale.getX(), scale.getY());
        g2.translate(offset.getX(), offset.getY());
    }

    private void buildTransform2(Graphics2D g2) {
        // Apply transform from image coords to map coords
        g2.scale(mapScale.getX(), mapScale.getY());
        g2.translate(mapOffset.getX(), mapOffset.getY());
    }

    private AffineTransform mapToScreen() {
        AffineTransform at = new AffineTransform();
        // Apply transform from frame coords to image coords
        at.translate(this.getWidth() / 2, this.getHeight() / 2);
        if(downPoint != null && currPoint != null) {
            at.translate(currPoint.getX() - downPoint.getX(),
                    currPoint.getY() - downPoint.getY());
        }
        at.scale(scale.getX(), scale.getY());
        at.translate(offset.getX(), offset.getY());

        // Apply transform from image coords to map coords
        at.scale(mapScale.getX(), mapScale.getY());
        at.translate(mapOffset.getX(), mapOffset.getY());
        return at;
    }

    private AffineTransform screenToMap() {
        AffineTransform at = mapToScreen();
        try {
            at = at.createInverse();
        } catch(NoninvertibleTransformException e) {
            System.err.println("MapPanel.screenToMap:  NON INVERTIBLE mapToScreen transform! e=" + e);
            e.printStackTrace();
        }
        return at;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        buildTransform1(g2);

        // Draw the map in the window
        drawMap(g2);

        buildTransform2(g2);

        // Draw overlays in the window
        synchronized(icons) {
            for(MapIcon mi : icons.values()) {
                drawIcon(mi, g2);
            }
        }

        drawTargets(g2);

        g2.dispose();
    }

    private void drawMap(Graphics2D g) {
        if(background == null) {
            return;
        }
        if(!mapImageTransform.isIdentity()) {
            g.drawImage(background, mapImageTransform, this);
        }
    }

    private void drawIcon(MapIcon mi, Graphics2D g) {
        if(mi.icon == null) {
            return;
        }
        if(!mi.xform.isIdentity()) {
            g.drawImage(mi.icon, mi.xform, this);
        }
    }

    private void drawTarget(Graphics2D g, int x, int y, double theta, int radius, int length, Color color) {
        g.setColor(color);
        g.drawOval(x - (radius / 2), y - (radius / 2), radius, radius);
        g.drawLine(x, y, (int) (x + Math.cos(theta) * length), (int) (y + Math.sin(theta) * length));
    }

    private void drawLine(Graphics2D g, int startX, int startY, int endX, int endY, Color color) {
        color = Color.BLACK;
        g.setColor(color);
        g.drawLine(startX, startY, endX, endY);
    }

    public void start() {
        new Thread(new Updater()).run();
    }
    int markerCounter = 0;
    String currentMarkerKey = null;
    Target currentMarker = null;
    private ArrayList<Target> waypointList = new ArrayList<Target>();

    // @TODO: Make this actually work.
    private void toMap(double x, double y, double[] xyz) {
        // bleahhhhhh convert from screen/frame coordinates to VBS2 coordinates.  When the icons are drawn they
        // are drawn starting with VBS2 coordinates.   A lot of crap happens in setIconTransform() but that mostly
        // resolves down to nothing since scale is always passed as 1.0.  All it's really doing is
        // translating. (and adjusting for the icon width which we don't need to do.)
        //
        // ok, so drawIcon is basically just drawing using whatever transform is already put in place by paintComponent().
        AffineTransform at = screenToMap();
        double[] screenxyz = new double[]{x, y, 0};
        at.transform(screenxyz, 0, xyz, 0, 1);
    }

    // @TODO: Make this actually work.
    private void toFrame(double x, double y, double[] xy) {
        xy[0] = x;
        xy[1] = y;
    }

    private void drawTargets(Graphics2D g) {
        float strokeSize = 10.0f;
        float strokeScale = (float) (600 / scale.getY());
        if(strokeScale < 0.0) {
            strokeScale = 1.0f;
        }
//        g.setStroke(new BasicStroke((float) (1.0 * -1*mapScale.getY()/this.getHeight())));
//        g.setStroke(new BasicStroke(10.0f));
        g.setStroke(new BasicStroke(strokeScale * strokeSize));

        Target t;
        for(int i = 0; i < waypointList.size(); i++) {
            t = waypointList.get(i);
            // Draw path
            if(i > 0) {
                Target oldT = waypointList.get(i - 1);
                if(t.selected || oldT.selected) {
                    drawLine(g, (int) t.x, (int) t.y, (int) oldT.x, (int) oldT.y, TARGET_SELECTED_COLOR);
                } else if(t.highlighted || oldT.highlighted) {
                    drawLine(g, (int) t.x, (int) t.y, (int) oldT.x, (int) oldT.y, TARGET_HIGHLIGHTED_COLOR);
                } else {
                    drawLine(g, (int) t.x, (int) t.y, (int) oldT.x, (int) oldT.y, TARGET_COLOR);
                }
            }
            // Draw waypoint
            if(t.selected) {
                drawTarget(g, (int) t.x, (int) t.y, t.theta, TARGET_RADIUS, TARGET_LENGTH, TARGET_SELECTED_COLOR);
            } else if(t.highlighted) {
                drawTarget(g, (int) t.x, (int) t.y, t.theta, TARGET_RADIUS, TARGET_LENGTH, TARGET_HIGHLIGHTED_COLOR);
            } else {
                drawTarget(g, (int) t.x, (int) t.y, t.theta, TARGET_RADIUS, TARGET_LENGTH, TARGET_COLOR);
            }
        }
    }

    private Target findTarget(double x, double y, double rangeSqd, boolean highlighted, boolean selected) {
        Target found = null;
        double bestDistSqd = Double.MAX_VALUE;
        Target[] targetAry = waypointList.toArray(new Target[1]);
        if(null == targetAry) {
            return null;
        }
        if(targetAry.length <= 0) {
            return null;
        }
        for(int loopi = 0; loopi < targetAry.length; loopi++) {
            Target target = targetAry[loopi];
            if(null == target) {
                continue;
            }
            if(highlighted) {
                target.highlighted = false;
            }
            if(selected) {
                target.selected = false;
            }
            double xdist = target.x - x;
            double ydist = target.y - y;
            double curDistSqd = xdist * xdist + ydist * ydist;
            if(curDistSqd < rangeSqd) {
                if(curDistSqd < bestDistSqd) {
                    bestDistSqd = curDistSqd;
                    found = target;
                }
            }
        }
        return found;
    }

    public void updateTargetMarkers(MouseEvent e) {
        boolean finished = false;
        double xyz[] = new double[3];
        toMap(e.getX(), e.getY(), xyz);
        double[] gps = new double[3];
        Map.localToGps(xyz, gps, "warminster");
        if(!finished && (e.getID() == e.MOUSE_MOVED) && (currentMarker == null)) {
            Target target = findTarget(xyz[0], xyz[1], TARGET_SELECT_RANGE_SQD, true, false);
            if(null != target) {
                target.highlighted = true;
            }
            finished = true;
        }
//        double[] ogl = new double[]{se.hitPos[0], se.hitPos[1], se.hitPos[2]};
        double[] ogl = new double[3];
//        double[] xyz = new double[3];
//        origin.openGLToLvcs(ogl, lvcs);
//        origin.lvcsToGpsDegrees(lvcs, gps);

        // Deleting
        if(!finished && e.getButton() == e.BUTTON3 && e.getID() == e.MOUSE_PRESSED) {
            if(null == currentMarker && targetCreationStep == 0) {
                currentMarker = findTarget(xyz[0], xyz[1], TARGET_SELECT_RANGE_SQD, false, true);
                if(null != currentMarker) {
                    waypointList.remove(currentMarker);
                    currentMarker = null;
                }
            } else if(null != currentMarker && targetCreationStep == 1) {
                currentMarker = null;
                targetCreationStep = 0;
            }
            finished = true;
        }
        // Create (0) (set location) and selecting
        if(!finished && e.getButton() == e.BUTTON1 && e.getID() == e.MOUSE_PRESSED) {
            if(currentMarker == null) {
                currentMarker = findTarget(xyz[0], xyz[1], TARGET_SELECT_RANGE_SQD, false, true);
                if(null == currentMarker) {
                    currentMarkerKey = "TARGET_MARKER" + "_" + markerCounter++;
                    currentMarker = new Target();
                    currentMarker.key = currentMarkerKey;
                } else {
                }
                currentMarker.selected = true;
            } // Create (1) (set angle)
            else if(currentMarker != null && targetCreationStep == 1) {
                // Reverse order for y since we are working in pixel space
                currentMarker.theta = sanitize(Math.atan2(xyz[1] - currentMarker.y, xyz[0] - currentMarker.x));
                System.out.println("currentMarker.theta = " + currentMarker.theta + " " + rToD(currentMarker.theta));
                waypointList.add(currentMarker);
            } // Update location during create (0)
            else {
                currentMarker.setPos(xyz[0], xyz[1]);
            }
        } // Drop
        else if(!finished && e.getButton() == e.BUTTON1 && e.getID() == e.MOUSE_RELEASED && currentMarker != null) {
            if(targetCreationStep == 0) {
                targetCreationStep = 1;
                currentMarker.setPos(xyz[0], xyz[1]);
            } else {
                targetCreationStep = 0;
                currentMarker.selected = false;
                currentMarker = null;

                // Validate
                waypointList = validateWaypoints(waypointList);

            }
        } // Move
        else if(!finished && e.getID() == e.MOUSE_DRAGGED && currentMarker != null) {
            currentMarker.setPos(xyz[0], xyz[1]);
        }

        // Added this in because target interaction was not triggering a Map
        //  panel repaint for somre reason
        MapPanel.this.repaint();
    }

    private class MapComponentListener implements ComponentListener {

        public void componentResized(ComponentEvent e) {
            scale.setLocation(MapPanel.this.getWidth(), MapPanel.this.getHeight());
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }
    }

    private class MapMouseListener implements MouseInputListener, MouseWheelListener, MouseMotionListener {

        public void mouseDragged(MouseEvent e) {
            if(!e.isShiftDown()) {
                updateTargetMarkers(e);
                return;
            }
            currPoint = e.getPoint();
            MapPanel.this.repaint();
        }

        public void mouseMoved(MouseEvent e) {
            if(!e.isShiftDown()) {
                updateTargetMarkers(e);
                return;
            }
            currPoint = e.getPoint();
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            if(!e.isShiftDown()) {
                updateTargetMarkers(e);
                return;
            }
            double exp = Math.pow(SCALE_FACTOR, e.getWheelRotation());
            scale.setLocation(scale.getX() * exp, scale.getY() * exp);
            MapPanel.this.repaint();
        }

        public void mouseClicked(MouseEvent e) {
            if(!e.isShiftDown()) {
                updateTargetMarkers(e);
                return;
            }
            Point2D mousePos = new Point2D.Double(e.getX(), e.getY());

            AffineTransform at = new AffineTransform();
            at.translate(MapPanel.this.getWidth() / 2, MapPanel.this.getHeight() / 2);
            if(downPoint != null && currPoint != null) {
                at.translate(currPoint.getX() - downPoint.getX(),
                        currPoint.getY() - downPoint.getY());
            }
            at.scale(scale.getX(), scale.getY());
            at.translate(offset.getX(), offset.getY());

            Point2D map = null;
            try {
                map = at.inverseTransform(mousePos, null);
                map = mapImageTransform.inverseTransform(map, null);
            } catch(NoninvertibleTransformException e2) {
            }

            at.scale(mapScale.getX(), mapScale.getY());
            at.translate(mapOffset.getX(), mapOffset.getY());

            Point2D world = null;
            try {
                world = at.inverseTransform(mousePos, null);
            } catch(NoninvertibleTransformException e2) {
            }

            System.out.println("Map: [" + map + "], "
                    + "World: [" + world + "]");
        }

        public void mousePressed(MouseEvent e) {
            if(!e.isShiftDown()) {
                updateTargetMarkers(e);
                return;
            }
            downPoint = e.getPoint();
        }

        public void mouseReleased(MouseEvent e) {
            if(!e.isShiftDown()) {
                updateTargetMarkers(e);
                return;
            }
            double dx = (e.getPoint().getX() - downPoint.getX()) / scale.getX();
            double dy = (e.getPoint().getY() - downPoint.getY()) / scale.getY();
            offset.setLocation(offset.getX() + dx, offset.getY() + dy);
            downPoint = null;
            MapPanel.this.repaint();
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }

    private class Updater implements Runnable {

        public void run() {
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
            }
        }
    }

    public ArrayList<Target> validateWaypoints(ArrayList<Target> waypoints) {
        if(waypoints.size() < 2) {
            for(Target t : waypoints) {
                System.out.println(t.toString());
            }
            return waypoints;
        }
        ArrayList<WP> newPair;
        ArrayList<Target> newList = new ArrayList<Target>();
        Target target1, target2;
        for(int i = 1; i < waypoints.size(); i++) {
            target1 = waypoints.get(i - 1);
            target2 = waypoints.get(i);
            if(!target1.validated || !target2.validated) {
                newPair = validatePair(waypoints.get(i - 1), waypoints.get(i));
                for(WP wp : newPair) {
                    newList.add(new Target(wp.x, wp.y, wp.t, true));
                }
            } else {
                newList.add(target1);
                newList.add(target2);
            }
        }
        newList.add(waypoints.get(waypoints.size() - 1));
        System.out.println("");
        for(Target t : newList) {
            System.out.println(t.toString());
        }

        return newList;
    }

    private ArrayList<WP> validatePair(Target target1, Target target2) {
        double x, x1, x2, x3, x4, y, y1, y2, y3, y4, denom, d1, d2, temp, t1, t2;
        Segment s1, s2;
        ArrayList<WP> wps = new ArrayList<WP>();

        // Add target1 to new waypoint list
        wps.add(new WP(target1.x, target1.y, target1.theta));

        System.out.println("target1.theta = " + target1.theta + " " + rToD(target1.theta));
        System.out.println("target2.theta = " + target2.theta + " " + rToD(target2.theta));

        // Round to nearest 45 deg to prevent some nasty edge cases
        t1 = sanitize(Math.round(target1.theta / ANGLE_INCREMENT) * ANGLE_INCREMENT);
        t2 = sanitize(Math.round(target2.theta / ANGLE_INCREMENT) * ANGLE_INCREMENT);

        // Add in MIN_DISTANCE
        s1 = new Segment(target1.x + MIN_DISTANCE * Math.cos(t1), target1.y + MIN_DISTANCE * Math.sin(t1),
                target1.x + 2 * MIN_DISTANCE * Math.cos(t1), target1.y + 2 * MIN_DISTANCE * Math.sin(t1),
                t1, true);
        s2 = new Segment(target2.x - MIN_DISTANCE * Math.cos(t2), target2.y - MIN_DISTANCE * Math.sin(t2),
                target2.x - 2 * MIN_DISTANCE * Math.cos(t2), target2.y - 2 * MIN_DISTANCE * Math.sin(t2),
                t2, true);
        Intersection intersection = getIntersection(s1, s2);

        // Add s1 to new waypoint list
        wps.add(new WP(s1.x1, s1.y1, t1));

        // Case 1: Intersection is in front of WP1 and behind WP2
        if(intersection.d1 >= 0 && intersection.d2 >= 0) {
            System.out.println("CASE 1");

            if(Math.abs(t2 - t1) > dToR(90) && Math.abs(t2 - t1) < dToR(270)) {
                System.out.println("\tloop");
                WP[] loopWPs = getLoop(intersection.x, intersection.y, t1, t2);
                for(int i = 0; i < loopWPs.length; i++) {
                    wps.add(loopWPs[i]);
                }
            } else {
                wps.add(new WP(intersection.x, intersection.y, t2));
            }
        }
        // Case 2: Intersection is in front of WP1 but in front of WP2
        if(intersection.d1 >= 0 && intersection.d2 < 0) {
            System.out.println("CASE 2");


        }
        // Case 3: Intersection is behind WP1 and behind WP2
        if(intersection.d1 < 0 && intersection.d2 >= 0) {
            System.out.println("CASE 3");

        }
        // Case 4: Intersection is behind WP1 but in front of WP2
        if(intersection.d1 < 0 && intersection.d2 < 0) {
            System.out.println("CASE 4");

            // Make a set of 2 lines which are orthogonal to wp1, one intersecting with s1 and one intersecting with s2
            double t1_1 = t1 + dToR(90);
            Segment s1_1a = new Segment(s1.x1, s1.y1,
                    s1.x1 + MIN_DISTANCE * Math.cos(t1_1), s1.y1 + MIN_DISTANCE * Math.sin(t1_1),
                    t1_1, false);
            Segment s1_1b = new Segment(s2.x1, s2.y1,
                    s2.x1 + MIN_DISTANCE * Math.cos(t1_1), s2.y1 + MIN_DISTANCE * Math.sin(t1_1),
                    t1_1, false);
            Intersection i1_1a = getIntersection(s1_1a, s2);
            Intersection i1_1b = getIntersection(s1, s1_1b);

            // If we have a valid intersection, add it along with any necessary looops
            //@todo: Choose best intersection
            if(i1_1a.valid) {
                System.out.println("\ti1_1a.valid");

                if(Math.abs(t2 - t1_1) > dToR(90) && Math.abs(t2 - t1_1) < dToR(270)) {
                    System.out.println("\tloop");
                    WP[] loopWPs = getLoop(i1_1a.x, i1_1a.y, t1_1, t2);
                    for(int i = 0; i < loopWPs.length; i++) {
                        wps.add(loopWPs[i]);
                    }
                } else {
                    wps.add(new WP(i1_1a.x, i1_1a.y, t2));
                }
            } else if(i1_1b.valid) {
                System.out.println("\ti1_1b.valid");

                if(Math.abs(t2 - t1_1) > dToR(90) && Math.abs(t2 - t1_1) < dToR(270)) {
                    System.out.println("\tloop");
                    WP[] loopWPs = getLoop(i1_1b.x, i1_1b.y, t1_1, t2);
                    for(int i = 0; i < loopWPs.length; i++) {
                        wps.add(loopWPs[i]);
                    }
                } else {
                    wps.add(new WP(i1_1b.x, i1_1b.y, t2));
                }
            } // If the first set of orthogonal lines did not have a valid intersection, we will need a second line of orthogonal lines, which are orthogonal to the first set
            else {
                System.out.println("\ti1_1 invalid");
                double t1_2 = sanitize(t1 + Math.PI);
                // Vector parallel to s1 but MIN_DISTANCE to its left
                Segment s1_2a = new Segment(
                        s1.x1 + MIN_DISTANCE * Math.cos(t1 + dToR(90)), s1.y1 + MIN_DISTANCE * Math.sin(t1 + dToR(90)),
                        target1.x + MIN_DISTANCE * Math.cos(t1 + dToR(90)), target1.y + MIN_DISTANCE * Math.sin(t1 + dToR(90)),
                        t1_2, true);
                // Vector parallel to s1 but MIN_DISTANCE to its right
                Segment s1_2b = new Segment(
                        s1.x1 + MIN_DISTANCE * Math.cos(t1 - dToR(90)), s1.y1 + MIN_DISTANCE * Math.sin(t1 - dToR(90)),
                        target1.x + MIN_DISTANCE * Math.cos(t1 - dToR(90)), target1.y + MIN_DISTANCE * Math.sin(t1 - dToR(90)),
                        t1_2, true);
                // Vector parallel to s1 starting at s2
                Segment s1_2c = new Segment(
                        s2.x1, s2.y1,
                        s2.x1 + MIN_DISTANCE * Math.cos(t1), s2.y1 + MIN_DISTANCE * Math.sin(t1),
                        t1_2, true);

                // Just use s1_1a for s1_1
                Intersection i1_2a = getIntersection(s1_2a, s2);
                Intersection i1_2b = getIntersection(s1_2b, s2);
                //  We don't actaully know if this intersection will allow a long enough segment on the s1_1a line, unless the above two intersections failed
                // Unless we made s1_1a into two vectors to eliminate that area....
                Intersection i1_2c = getIntersection(s1_1a, s1_2c);

                if(i1_2a.valid) {
                    System.out.println("\t\ti1_2a.valid");
                    // Add intersection between s1_1 and s1_2a
                    wps.add(new WP(s1_2a.x1, s1_2a.y1, t1_2));
                    // Do we need a loop at the intersection of s1_2a and s2?
                    if(Math.abs(t2 - t1_2) > dToR(90) && Math.abs(t2 - t1_2) < dToR(270)) {
                        System.out.println("\t\tloop");
                        WP[] loopWPs = getLoop(i1_2a.x, i1_2a.y, t1_2, t2);
                        for(int i = 0; i < loopWPs.length; i++) {
                            wps.add(loopWPs[i]);
                        }
                    } else {
                        wps.add(new WP(i1_2a.x, i1_2a.y, t2));
                    }
                } else if(i1_2b.valid) {
                    System.out.println("\t\ti1_2b.valid");
                    // Add intersection between s1_1 and s1_2b
                    wps.add(new WP(s1_2b.x1, s1_2b.y1, t1_2));
                    // Do we need a loop at the intersection of s1_2a and s2?
                    if(Math.abs(t2 - t1_2) > dToR(90) && Math.abs(t2 - t1_2) < dToR(270)) {
                        System.out.println("\t\tloop");
                        WP[] loopWPs = getLoop(i1_2b.x, i1_2b.y, t1_2, t2);
                        for(int i = 0; i < loopWPs.length; i++) {
                            wps.add(loopWPs[i]);
                        }
                    } else {
                        wps.add(new WP(i1_2a.x, i1_2a.y, t2));
                    }
                } else if(i1_2c.valid) {
                    System.out.println("\t\ti1_2c.valid");
                    // Add intersection between s1_1 and s1_2c
                    wps.add(new WP(i1_2c.x, i1_2c.y, t1_2));
                    // Do we need a loop at the intersection of s1_2c and s2?
                    if(Math.abs(t2 - t1_2) > dToR(90) && Math.abs(t2 - t1_2) < dToR(270)) {
                        System.out.println("\t\tloop");
                        WP[] loopWPs = getLoop(s1_2c.x1, s1_2c.y1, t1_2, t2);
                        for(int i = 0; i < loopWPs.length; i++) {
                            wps.add(loopWPs[i]);
                        }
                    } else {
                        wps.add(new WP(s1_2c.x1, s1_2c.y1, t2));
                    }
                }
            }
        }

        // Add s2 to new waypoint list
        wps.add(new WP(s2.x1, s2.y1, t2));

        return wps;
    }

    private WP[] getLoop(double x, double y, double tStart, double tEnd) {
        WP[] loop = new WP[5];
        double t1 = sanitize((sanitize(tStart + 180) + tEnd) / 2 - 225);
        loop[0] = new WP(x, y, t1);
        for(int i = 1; i < 4; i++) {
            loop[i] = new WP(
                    loop[i - 1].x + MIN_DISTANCE * Math.cos(loop[i - 1].t),
                    loop[i - 1].y + MIN_DISTANCE * Math.sin(loop[i - 1].t),
                    sanitize(loop[i - 1].t + Math.PI / 2));
        }
        loop[4] = new WP(x, y, tEnd);
        return loop;
    }

    private double dToR(double d) {
        return d * Math.PI / 180.0;
    }

    private double rToD(double r) {
        return r * 180.0 / Math.PI;
    }
    
    private double sanitize(double r) {
        while(r >= 2 * Math.PI) {
            r -= 2 * Math.PI;
        }
        while(r < 0) {
            r += 2 * Math.PI;
        }
        return r;
    }

    private Intersection getIntersection(Segment s1, Segment s2) {
        double x, y, t, denom, d1, d2;
        // Find point of intersection between the two lines
        denom = (s1.x1 - s1.x2) * (s2.y1 - s2.y2) - (s1.y1 - s1.y2) * (s2.x1 - s2.x2);
        if(denom == 0) {
            //Parallel lines
            return new Intersection(Double.NaN, Double.NaN, Double.NaN, Double.NaN, false);
        }
        x = ((s1.x1 * s1.y2 - s1.y1 * s1.x2) * (s2.x1 - s2.x2) - (s1.x1 - s1.x2) * (s2.x1 * s2.y2 - s2.y1 * s2.x2)) / denom;
        y = ((s1.x1 * s1.y2 - s1.y1 * s1.x2) * (s2.y1 - s2.y2) - (s1.y1 - s1.y2) * (s2.x1 * s2.y2 - s2.y1 * s2.x2)) / denom;

        // Find the distance from wp1 to intersection
        d1 = Math.sqrt(Math.pow(x - s1.x1, 2) + Math.pow(y - s1.y1, 2));
        // Find the distance from intersection to wp2
        d2 = Math.sqrt(Math.pow(s2.x1 - x, 2) + Math.pow(s2.y1 - y, 2));

        if(s1.isVector && (
                (Math.abs(x - s1.x1) > 1e-3 && Math.signum(x - s1.x1) != Math.signum(Math.cos(s1.t)) || 
                (Math.abs(y - s1.y1) > 1e-3 && Math.signum(y - s1.y1) != Math.signum(Math.sin(s1.t)))))) {
            d1 *= -1;
        }
        if(s2.isVector && (
                (Math.abs(s2.x1 - x) > 1e-3 && Math.signum(s2.x1 - x) != Math.signum(Math.cos(s2.t)) || 
                (Math.abs(s2.y1 - y) > 1e-3 && Math.signum(s2.y1 - y) != Math.signum(Math.sin(s2.t)))))) {
            d2 *= -1;
        }
        return new Intersection(x, y, d1, d2, (d1 >= 0 && d2 >= 0));
    }

    private class WP {

        public double x, y, t;

        public WP(double x, double y, double t) {
            this.x = x;
            this.y = y;
            this.t = t;
        }

        public String toString() {
            return "WP: " + x + " " + y + " " + t + " " + rToD(t);
        }
    }

    private class Segment {

        public boolean isVector;
        public double x1, y1, x2, y2, t;

        public Segment(double x1, double y1, double x2, double y2, double t, boolean isVector) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.t = t;
            this.isVector = isVector;
        }

        public String toString() {
            return "Segment: " + x1 + " " + y1 + " " + x2 + " " + y2 + " " + t + " " + rToD(t);
        }
    }

    private class Intersection {

        boolean valid;
        double x, y, d1, d2;

        public Intersection(double x, double y, double d1, double d2, boolean valid) {
            this.x = x;
            this.y = y;
            this.d1 = d1;
            this.d2 = d2;
            this.valid = valid;
        }

        public String toString() {
            return "Intersection: " + x + " " + y + " " + d1 + " " + d2 + " " + valid;
        }
    }
}
