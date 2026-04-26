public class Launcher{

    private double X;
    private double Y;
    private double InitialVelocity;
    private double Gravity;
    private double Angle;
    private double Wind = 0.0;

    public Launcher(double X, double Y, double Angle, double InitialVelocity){
        this.X = X;
        this.Y = Y;
        this.Angle = Angle;
        this.InitialVelocity = InitialVelocity;
        this.Gravity = 9.81;
    }

    public double getX(){ 
        return X; 
    }
    public double getY(){ 
        return Y; 
    }
    public double getAngle(){ 
        return Angle; 
    }
    public double getInitialVelocity(){
        return InitialVelocity;
    }
    public double getGravity(){
        return Gravity; 
    }
    public double getWind(){
        return Wind; 
    }
    public void setAngle(double a){ 
        this.Angle = a; 
    }
    public void setInitialVelocity(double v){
        this.InitialVelocity = v;
    }
    public void setGravity(double g){
        this.Gravity = g; 
    }
    public void setWind(double w){ 
        this.Wind = w; 
    }
}
