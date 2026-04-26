public abstract class Artillery{

    private final String Name;
    private final double InitialVelocity;
    private Ammunition Ammo = null;

    public Artillery(String Name, double InitialVelocity){
        this.Name            = Name;
        this.InitialVelocity = InitialVelocity;
    }

    //Ammunition selection basically
    public void loadAmmo(Ammunition ammo){
        this.Ammo = ammo;
    }

    public Ammunition getAmmo(){
        return Ammo;
    }
    public String GetName(){
        return Name;
    }
    public double GetInitialVelocity(){
        return InitialVelocity;
    }

    //Default is 50m
    public double GetEffectiveBlastRadius(){
        return (Ammo != null) ? Ammo.getBlastRadius() : new Shell50mm().getBlastRadius();
    }
    public double GetEffectiveMass() {
        return (Ammo != null) ? Ammo.getMass() : new Shell50mm().getMass();
    }
}
