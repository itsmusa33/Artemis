public abstract class Artillery {

    private final String name;
    private final double initialVelocity;
    private final double blastRadius;
    private final double damage;
    private final double mass;

    public Artillery(String name, double initialVelocity,
                     double blastRadius, double damage, double mass) {

        this.name            = name;
        this.initialVelocity = initialVelocity;
        this.blastRadius     = blastRadius;
        this.damage          = damage;
        this.mass            = mass;
    }

    public abstract String getDescription();

    public String getName()            { return name; }
    public double getInitialVelocity() { return initialVelocity; }
    public double getBlastRadius()     { return blastRadius; }
    public double getDamage()          { return damage; }
    public double getMass()            { return mass; }
}

public class Mortar extends Artillery {

    public Mortar() {
        super("Mortar", 35.0, 18.0, 75.0, 4.2);
    }

    @Override
    public String getDescription() {
        return "Short range, high arc. Wide blast area.";
    }
}

public class Howitzer extends Artillery {

    public Howitzer() {
        super("Howitzer", 50.0, 12.0, 90.0, 15.0);
    }

    @Override
    public String getDescription() {
        return "Balanced range and power. Standard field artillery.";
    }
}

public class Cannon extends Artillery {

    public Cannon() {
        super("Cannon", 80.0, 6.0, 120.0, 25.0);
    }

    @Override
    public String getDescription() {
        return "Long range, high velocity. Precise but narrow blast zone.";
    }
}

public classs Self_propelled extends Artillery{
    public Self_propelled(){
        super("Self_propelled Artillery",160.0,10.0,150.0,30.0);

        @Override
            public String getDescription(){
                return "Quick, effective. Long range with high damage");
        }
    }

