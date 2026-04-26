import java.util.ArrayList;
import java.util.List;

public class CraterManager{

    private final List<Crater> Craters = new ArrayList<>();

    public void addCrater(double X, Projectile p){
        Craters.add(new Crater(X, p.getBlastRadius()));
    }
    public void clearCraters(){
        Craters.clear();
    }
    public List<Crater> getCraters(){
        return Craters;
    }
}