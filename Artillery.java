public class Launcher {

    private double InitVelocity;
    private double Gravity;
    private double Angle;

    public Launcher(double InitVelocity, double Gravity, double Angle){

        this.InitVelocity = InitVelocity;
        this.Gravity = Gravity;
        this.Angle = Angle;
    }

    public double getInitVelocity(){
        return InitVelocity;
    }
    public double getGravity(){
        return Gravity;
    }
    public double getAngle(){
        return Angle;
    }

    public void setInitVelocity( double iv){
        this.InitVelocity = iv;
    }
    public void setGravity( double g){
        this.Gravity = g;
    }
    public void setAngle( double a){
        this.Angle = a;
    }
}

public interface PhysicsEngine {

    void computeStep(Projectile p, double dt);
    double totalFlightTime(Launcher lau);
    double getMaxRange(Launcher lau);
    double getMaxHeight(Launcher lau);
}

public class BasicPhysics implements PhysicsEngine {

    @Override
    public void computeStep(Projectile p, double dt) {

        p.setX(p.getX() + p.getVx() * dt);
        p.setY(p.getY() + p.getVy() * dt);

        p.setVy(p.getVy() - p.getGravity() * dt);

        p.setTime(p.getTime() + dt);
    }

    @Override
    public double totalFlightTime(Launcher lau) {
        double angleRad = Math.toRadians(lau.getAngle());
        double vy = lau.getInitVelocity() * Math.sin(angleRad);
        return (2 * vy) / lau.getGravity();
    }

    @Override
    public double getMaxRange(Launcher lau) {
        double angleRad = Math.toRadians(lau.getAngle());
        double v = lau.getInitVelocity();
        return (v * v * Math.sin(2 * angleRad)) / lau.getGravity();
    }

    @Override
    public double getMaxHeight(Launcher lau) {
        double angleRad = Math.toRadians(lau.getAngle());
        double vy = lau.getInitVelocity() * Math.sin(angleRad);
        return (vy * vy) / (2 * lau.getGravity());
    }
}
// Rendering starts here

package com.example.demo;

import javafx.scene.canvas.GraphicsContext;

public interface Renderer {
    void render(GraphicsContext gc, Projectile p, LaunchConfig config, ScaleMapper map);
}

package com.example.demo;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class TrajectoryRenderer implements Renderer {

    private static final int STEPS = 300;

    @Override
    public void render(GraphicsContext gc, Projectile p,
                       LaunchConfig config, ScaleMapper map) {

        double angleRad = Math.toRadians(config.getAngle());
        double v        = config.getInitialVelocity();
        double g        = config.getGravity();
        double vx       = v * Math.cos(angleRad);
        double vy       = v * Math.sin(angleRad);
        double tTotal   = (2 * vy) / g;

        gc.setLineWidth(2);
        for (int i = 1; i <= STEPS; i++) {
            double t1 = (i - 1) * tTotal / STEPS;
            double t2 = i       * tTotal / STEPS;

            double x1 = vx * t1;
            double y1 = vy * t1 - 0.5 * g * t1 * t1;
            double x2 = vx * t2;
            double y2 = vy * t2 - 0.5 * g * t2 * t2;

            double f = (double) i / STEPS;
            gc.setStroke(Color.rgb(
                    (int)(30 + 20 * f),
                    (int)(180 - 80 * f),
                    (int)(220 + 35 * f),
                    0.6
            ));

            gc.strokeLine(
                    map.toCanvasX(x1), map.toCanvasY(y1),
                    map.toCanvasX(x2), map.toCanvasY(y2)
            );
        }

        double tApex = vy / g;
        double xApex = vx * tApex;
        double yApex = vy * tApex - 0.5 * g * tApex * tApex;
        double apexPx = map.toCanvasX(xApex);
        double apexPy = map.toCanvasY(yApex);

        gc.setFill(Color.web("#ffdd66"));
        gc.fillOval(apexPx - 5, apexPy - 5, 10, 10);
        gc.setFont(Font.font("Monospace", 11));
        gc.fillText(String.format("%.1f m", yApex), apexPx + 8, apexPy - 2);

        double landingPx = map.toCanvasX(vx * tTotal);
        double landingPy = map.toCanvasY(0);

        gc.setFill(Color.web("#ff6655"));
        gc.fillOval(landingPx - 5, landingPy - 5, 10, 10);

        gc.setFill(Color.web("#66ff99"));
        gc.fillOval(map.toCanvasX(0) - 5, map.toCanvasY(0) - 5, 10, 10);
    }
}

package com.example.demo;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

public class ProjectileRenderer implements Renderer {

    @Override
    public void render(GraphicsContext gc, Projectile p,
                       LaunchConfig config, ScaleMapper map) {

        if (p.getTime() <= 0) return;

        double px = map.toCanvasX(p.getX());
        double py = map.toCanvasY(p.getY());

        RadialGradient glow = new RadialGradient(
                0, 0, px, py, 20, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4db8ff55")),
                new Stop(1, Color.TRANSPARENT)
        );
        gc.setFill(glow);
        gc.fillOval(px - 20, py - 20, 40, 40);

        RadialGradient ball = new RadialGradient(
                0.3, 0.3, px - 3, py - 3, 9, false, CycleMethod.NO_CYCLE,
                new Stop(0,   Color.web("#ffffff")),
                new Stop(0.4, Color.web("#4db8ff")),
                new Stop(1,   Color.web("#1a5580"))
        );
        gc.setFill(ball);
        gc.fillOval(px - 9, py - 9, 18, 18);

        double vx      = p.getVx();
        double vyNow   = p.getVy();
        double vAngle  = Math.atan2(vyNow, vx);
        double arrowLen = Math.min(Math.sqrt(vx * vx + vyNow * vyNow) * 0.8, 60);

        double ax   = px + arrowLen * Math.cos(vAngle);
        double ay   = py - arrowLen * Math.sin(vAngle);
        double head = Math.PI / 7;
        double hLen = 10;

        gc.setStroke(Color.web("#ff9966aa"));
        gc.setLineWidth(1.5);
        gc.strokeLine(px, py, ax, ay);
        gc.strokeLine(ax, ay,
                ax - hLen * Math.cos(vAngle - head),
                ay + hLen * Math.sin(vAngle - head));
        gc.strokeLine(ax, ay,
                ax - hLen * Math.cos(vAngle + head),
                ay + hLen * Math.sin(vAngle + head));
    }
}


public abstract class Projectile {

    //Physical constants
    protected final double mass; //mass in kg
    protected final double blastRadius; //metres
    protected final double dragCoeff; // dimensionless Cd
    protected final double area; //m²

    //Runtime state (updated by PhysicsEngine every tick)
    protected double x, y; //position (metres)
    protected double vx, vy; //velocity(m/s)
    protected boolean hasLanded = false;

    public Projectile(double mass, double blastRadius, double dragCoeff, double area) {
        this.mass = mass;
        this.blastRadius = blastRadius;
        this.dragCoeff = dragCoeff;
        this.area = area;
    }

    //Called by PhysicsEngine on ground contact.
    public abstract void onImpact();

    // Display name shown in the UI dropdown.
    public abstract String getDisplayName();

    //Getters
    public double getMass(){
        return mass;
    }
    public double getBlastRadius(){
        return blastRadius;
    }
    public double getDragCoeff() {
        return dragCoeff;
    }
    public double getArea() {
        return area;
    }
    public double getX(){
        return x;
    }
    public double getY() {
        return y;
    }
    public double getVx() {
        return vx;
    }
    public double getVy() {
        return vy;
    }
    public boolean hasLanded() {
        return hasLanded;
    }

    //Setters
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public void setVelocity(double vx, double vy) {
        this.vx = vx;
        this.vy = vy;
    }
    public void setHasLanded(boolean v) {
        this.hasLanded = v;
    }
}
public class Main{
    public static void main(String[] args){
        System.out.println("Artemis is alive.");
        PhysicsEngine physicsEngine = new PhysicsEngine();
        Launcher launcher = new Launcher(0, 0, 45, 100);
        Projectile projectile = new Projectile(10, 5, 0.47, 0.01) {
            @Override
            public void onImpact(){
                System.out.println("Impact! Blast radius: " + blastRadius + " metres");
            }

            @Override
            public String getDisplayName(){
                return "Standard Shell";
            }
        };
        projectile.setPosition(launcher.getPositionX(), launcher.getPositionY());
        projectile.setVelocity(
                launcher.getInitialVelocity() * Math.cos(Math.toRadians(launcher.getAngle())),
                launcher.getInitialVelocity() * Math.sin(Math.toRadians(launcher.getAngle())));
        while (!projectile.hasLanded()) {
            System.out.printf("Projectile at (%.2f, %.2f) with velocity (%.2f, %.2f)%n",
                    projectile.getX(), projectile.getY(), projectile.getVx(), projectile.getVy());
            physicsEngine.update(projectile, 0.1);
        }
        System.out.printf("Final position: (%.2f, %.2f)%n", projectile.getX(), projectile.getY());
    }
}

// Artillery

public abstract class Artillery {

    private final String name;
    private final double initialVelocity;
    private final double blastRadius;
    private final double damage;
    private final double mass;

    public Artillery(String name, double initialVelocity,
                     double blastRadius, double damage, double mass) {

        this.name            = name;
        this.initialVelocity = initialVelocity;
        this.blastRadius     = blastRadius;
        this.damage          = damage;
        this.mass            = mass;
    }

    public abstract String getDescription();

    public String getName()            { return name; }
    public double getInitialVelocity() { return initialVelocity; }
    public double getBlastRadius()     { return blastRadius; }
    public double getDamage()          { return damage; }
    public double getMass()            { return mass; }
}

public class Mortar extends Artillery {

    public Mortar() {
        super("Mortar", 35.0, 18.0, 75.0, 4.2);
    }

    @Override
    public String getDescription() {
        return "Short range, high arc. Wide blast area.";
    }
}

public class Howitzer extends Artillery {

    public Howitzer() {
        super("Howitzer", 50.0, 12.0, 90.0, 15.0);
    }

    @Override
    public String getDescription() {
        return "Balanced range and power. Standard field artillery.";
    }
}

public class Cannon extends Artillery {

    public Cannon() {
        super("Cannon", 80.0, 6.0, 120.0, 25.0);
    }

    @Override
    public String getDescription() {
        return "Long range, high velocity. Precise but narrow blast zone.";
    }
}


public class SimStats {

    private final LaunchConfig config;

    public SimStats(LaunchConfig config) {
        this.config = config;
    }

    // Total horizontal distance at landing
    public double getRange() {
        double angleRad = Math.toRadians(config.getAngle());
        double v        = config.getInitialVelocity();
        double g        = config.getGravity();
        return (v * v * Math.sin(2 * angleRad)) / g;
    }

    // Peak height reached at apex
    public double getMaxHeight() {
        double angleRad = Math.toRadians(config.getAngle());
        double vy       = config.getInitialVelocity() * Math.sin(angleRad);
        double g        = config.getGravity();
        return (vy * vy) / (2 * g);
    }

    // Total time in the air
    public double getFlightTime() {
        double angleRad = Math.toRadians(config.getAngle());
        double vy       = config.getInitialVelocity() * Math.sin(angleRad);
        double g        = config.getGravity();
        return (2 * vy) / g;
    }

    // Formatted strings ready to display in the UI
    public String getRangeFormatted()      { return String.format("%.1f m",  getRange()); }
    public String getMaxHeightFormatted()  { return String.format("%.1f m",  getMaxHeight()); }
    public String getFlightTimeFormatted() { return String.format("%.2f s",  getFlightTime()); }
}

// color scheme



public class ColorScheme {

    // Background colors
    public static final String CANVAS_BG      = "#0a1520";
    public static final String PLOT_BG        = "#0d1f30";
    public static final String CONTROL_BG     = "#0d1b2a";

    // Grid colors
    public static final String GRID_LINES     = "#1a3050";
    public static final String GRID_AXES      = "#2a5070";
    public static final String GRID_LABELS    = "#4a7090";
    public static final String AXIS_TITLES    = "#6890a8";

    // Trajectory marker colors
    public static final String APEX_DOT       = "#ffdd66";
    public static final String LANDING_DOT    = "#ff6655";
    public static final String LAUNCH_DOT     = "#66ff99";

    // Projectile colors
    public static final String BALL_GLOW      = "#4db8ff55";
    public static final String BALL_MID       = "#4db8ff";
    public static final String BALL_DARK      = "#1a5580";
    public static final String VELOCITY_ARROW = "#ff9966aa";

    // Angle arrow colors
    public static final String ANGLE_ARROW    = "#4db8ffaa";
    public static final String ANGLE_ARC      = "#4db8ff66";
    public static final String ANGLE_LABEL    = "#4db8ff";

    // UI label colors
    public static final String LABEL_RANGE    = "#66ffaa";
    public static final String LABEL_HEIGHT   = "#ffdd66";
    public static final String LABEL_TIME     = "#ff9966";
    public static final String LABEL_ANGLE    = "#e0f0ff";
    public static final String LABEL_TITLE    = "#88aacc";

    // Private constructor — no one should ever instantiate this class
    private ColorScheme() {}
}

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AngleRenderer implements Renderer {

    private final double padLeft, padTop, plotH;

    public AngleRenderer(double padLeft, double padTop, double plotH) {
        this.padLeft = padLeft;
        this.padTop  = padTop;
        this.plotH   = plotH;
    }

    @Override
    public void render(GraphicsContext gc, Projectile p,
                       LaunchConfig config, ScaleMapper map) {

        double deg = config.getAngle();
        double rad = Math.toRadians(deg);
        double ox  = padLeft;
        double oy  = padTop + plotH;

        // ── Direction arrow ───────────────────────────────────────────────
        gc.setStroke(Color.web(ColorScheme.ANGLE_ARROW));
        gc.setLineWidth(2);
        gc.strokeLine(ox, oy,
                ox + 55 * Math.cos(rad),
                oy - 55 * Math.sin(rad)
        );

        // ── Angle arc ─────────────────────────────────────────────────────
        gc.setStroke(Color.web(ColorScheme.ANGLE_ARC));
        gc.setLineWidth(1);
        gc.strokeArc(ox - 25, oy - 25, 50, 50, 0, deg, ArcType.OPEN);

        // ── Angle label ───────────────────────────────────────────────────
        gc.setFill(Color.web(ColorScheme.ANGLE_LABEL));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        gc.fillText(String.format("%.0f°", deg), ox + 30, oy - 10);
    }
}

