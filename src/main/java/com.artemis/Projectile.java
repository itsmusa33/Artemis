package com.artemis;

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
