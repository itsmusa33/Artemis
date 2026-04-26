public class BasicPhysics implements PhysicsEngine{


    //Moves projectile and checks if it has landed
    @Override
    public void computeStep(Projectile p, double dt){
        p.setX(p.getX() + p.getVx() * dt);
        p.setY(p.getY() + p.getVy() * dt);
        p.setVy(p.getVy() - p.getGravity() * dt);

        if (p.getY() <= 0 && (p.getVx() != 0 || p.getVy() != 0)){
            p.setY(0);
            p.setHasLanded(true);
        }
    }

    //FLight time calc
    @Override
    public double getFlightTime(Launcher lau){
        double angleRad = Math.toRadians(lau.getAngle());
        double vy = lau.getInitialVelocity() * Math.sin(angleRad);
        return (2 * vy) / lau.getGravity();
    }

    //Max range calc
    @Override
    public double getMaxRange(Launcher lau){
        double angleRad = Math.toRadians(lau.getAngle());
        double v = lau.getInitialVelocity();
        return (v * v * Math.sin(2 * angleRad)) / lau.getGravity();
    }

    //Max height calc
    @Override
    public double getMaxHeight(Launcher lau){
        double angleRad = Math.toRadians(lau.getAngle());
        double vy = lau.getInitialVelocity() * Math.sin(angleRad);
        return (vy * vy) / (2 * lau.getGravity());
    }
}
