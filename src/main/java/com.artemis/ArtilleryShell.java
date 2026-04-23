package com.artemis;

public class ArtilleryShell extends Projectile{
//these are constants 
    private static final double MASS = 10.0;
    private static final double DRAG_COEFF = 0.47;
    private static final double AREA = 0.01;
    private static final double BOMB_FACTOR = 1.15;

    public ArtilleryShell(){
        this(DEFAULT_MASS, DEFAULT_DRAG_COEFF, DEFAULT_AREA);
    }

    public ArtilleryShell(double mass, double dragCoeff, double area){
        super(mass, calcBlastRadius(mass, BOMB_FACTOR), dragCoeff, area);
    }

    @Override
    public void onImpact(){
        System.out.println("Shell impact at (" + getX() + ", " + getY() + ") with blast radius " + getBlastRadius());
        setHasLanded(true);
    }

    @Override
    public String getDisplayName(){
        return "Artillery Shell";
    }
}
