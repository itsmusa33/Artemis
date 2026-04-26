import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TargetManager {

    private final List<Target> targets = new ArrayList<>();

    public void addTarget(double x) {
        targets.add(new Target(x));
    }

    public void addMovingTarget(double x, double speed) {
        targets.add(new Target(x, speed));
    }

    public void clearTargets() {
        targets.clear();
    }

    public boolean checkImpact(double landingX, double blastRadius) {
        int before = targets.size();
        targets.removeIf(t -> Math.abs(t.getX() - landingX) <= blastRadius);
        return targets.size() < before;
    }

    public void generateRandomTargets(int count, double maxRange) {
        targets.clear();
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            double worldX = maxRange * (0.20 + rand.nextDouble() * 0.75);
            targets.add(new Target(worldX));
        }
    }

    public void generateMovingTargets(int count, double maxRange,
                                      double minSpeed, double maxSpeed) {
        targets.clear();
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            double worldX = maxRange * (0.40 + rand.nextDouble() * 0.55);
            double speed  = minSpeed + rand.nextDouble() * (maxSpeed - minSpeed);
            targets.add(new Target(worldX, speed));
        }
    }

    public int updateMovingTargets(double dt) {
        int breached = 0;
        List<Target> toRemove = new ArrayList<>();
        for (Target t : targets) {
            if (t.step(dt)) {
                toRemove.add(t);
                breached++;
            }
        }
        targets.removeAll(toRemove);
        return breached;
    }


    public List<Target> getTargets() { return targets; }

    public Target getNearestTarget() {
        Target nearest = null;
        for (Target t : targets) {
            if (nearest == null || t.getX() < nearest.getX())
                nearest = t;
        }
        return nearest;
    }
}