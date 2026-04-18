public class LaunchConfig {

  private double InitVelocity;
  private double Gravity;
  private double Angle;

    public LaunchConfig(double InitVelocity, double Gravity, double Angle){

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
