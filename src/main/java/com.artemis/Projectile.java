public abstract class Projectile{

    protected final double mass;
    protected final double blast_radius;
    protected final double area;
    protected double gravity = 9.81;
    protected double x, y;
    protected double vx, vy;
    protected boolean has_landed = false;

    public Projectile(double mass, double blast_radius, double area){
        this.mass = mass;
        this.blast_radius = blast_radius;
        this.area = area;
    }
    public abstract void onImpact();
    public abstract String getDisplayName();
    public double getMass(){
        return mass;
    }

    public double getBlastRadius(){
        return blast_radius;
    }

    public double getArea(){
        return area;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public double getVx(){
        return vx;
    }

    public double getVy(){
        return vy;
    }

    public boolean hasLanded(){
        return has_landed;
    }

    public double getGravity(){
        return gravity;
    }

    public void setX(double x){
        this.x = x;
    }

    public void setY(double y){
        this.y = y;
    }

    public void setVx(double vx){
        this.vx = vx;
    }

    public void setVy(double vy){
        this.vy = vy;
    }

    public void setGravity(double g){
        this.gravity = g;
    }

    public void setPosition(double x, double y){
        this.x = x;
        this.y = y;
    }

    public void setVelocity(double vx, double vy){
        this.vx = vx;
        this.vy = vy;
    }

    public void setHasLanded(boolean v){
        this.has_landed = v;
    }
}
