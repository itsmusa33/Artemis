
public interface PhysicsEngine {

    void computeStep(Projectile p, double dt);
    double totalFlightTime(LaunchConfig config);
    double getMaxRange(LaunchConfig config);
    double getMaxHeight(LaunchConfig config);
}

public class BasicPhysics implements PhysicsEngine {

    public void computeStep(Projectile p, double dt) {


        p.setX(p.getX() + p.getVx() * dt);
        p.setY(p.getY() + p.getVy() * dt);

        p.setVy(p.getVy() - p.getGravity() * dt);

        // Advance the timer
        p.setTime(p.getTime() + dt);
    }

    @Override
    public double totalFlightTime(LaunchConfig config) {
        double angleRad = Math.toRadians(config.getAngle());
        double vy = config.getInitialVelocity() * Math.sin(angleRad);
        return (2 * vy) / config.getGravity();
    }

    @Override
    public double getMaxRange(LaunchConfig config) {
        double angleRad = Math.toRadians(config.getAngle());
        double v = config.getInitialVelocity();
        return (v * v * Math.sin(2 * angleRad)) / config.getGravity();
    }

    @Override
    public double getMaxHeight(LaunchConfig config) {
        double angleRad = Math.toRadians(config.getAngle());
        double vy = config.getInitialVelocity() * Math.sin(angleRad);
        return (vy * vy) / (2 * config.getGravity());
    }
}
