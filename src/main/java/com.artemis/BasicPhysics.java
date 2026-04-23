public interface PhysicsEngine {

    void computeStep(Projectile p, double dt);
    double totalFlightTime(Launcher lau);
    double getMaxRange(Launcher lau);
    double getMaxHeight(Launcher lau);
}

public class BasicPhysics implements PhysicsEngine {

    public void computeStep(Projectile p, double dt) {

        p.setX(p.getX() + p.getVx() * dt);
        p.setY(p.getY() + p.getVy() * dt);

        p.setVy(p.getVy() - p.getGravity() * dt);

        p.setTime(p.getTime() + dt);
    }
    @Override
    public double totalFlightTime(Launcher lau) {
        double angleRad = Math.toRadians(lau.getAngle());
        double vy = lau.getInitialVelocity() * Math.sin(angleRad);
        return (2 * vy) / lau.getGravity();
    }
    @Override
    public double getMaxRange(Launcher lau) {
        double angleRad = Math.toRadians(lau.getAngle());
        double v = lau.getInitialVelocity();
        return (v * v * Math.sin(2 * angleRad)) / lau.getGravity();
    }
    @Override
    public double getMaxHeight(Launcher lau) {
        double angleRad = Math.toRadians(lau.getAngle());
        double vy = lau.getInitialVelocity() * Math.sin(angleRad);
        return (vy * vy) / (2 * lau.getGravity());
    }
}
