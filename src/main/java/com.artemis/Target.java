//public class Target {
//
//    double X;
//    static double Width = 10;
//    static double Height =10;
//    Boolean IsHit;
//
//    public Target(double X) {
//        this.X = X;
//        this.IsHit  = false;
//    }
//
//    public void markHit(){
//        this.IsHit = true;
//    }
//    public boolean isHit(){
//        return IsHit;
//    }
//    public double getX(){
//        return X;
//    }
//}

public class Target{

    public static final double width  = 6.0;
    public static final double height = 8.0;

    private double  x;
    private boolean moving;
    private double  speed;

    public Target(double x){
        this.x = x;
        this.moving = false;
        this.speed  = 0;
    }

    public Target(double x, double speed){
        this.x = x;
        this.moving = true;
        this.speed = speed;
    }

    public boolean step(double dt){
        if (!moving) return false;
        x -= speed * dt;
        return x <= 0;
    }

    public double  getX(){
        return x;
    }
    public boolean isMoving(){
        return moving;
    }
    public double  getSpeed(){
        return speed;
    }
}