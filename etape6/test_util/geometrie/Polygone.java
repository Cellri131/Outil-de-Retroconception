import java.util.List;
import java.util.ArrayList;

public class Polygone {
    private List<Point> sommets;
    private String nom;
    
    public Polygone(String nom) {
        this.nom = nom;
        this.sommets = new ArrayList<>();
    }
    
    public void ajouterSommet(Point p) {
        sommets.add(p);
    }
    
    public int getNbSommets() {
        return sommets.size();
    }
    
    public double calculerPerimetre() {
        if (sommets.size() < 2) return 0;
        
        double perimetre = 0;
        for (int i = 0; i < sommets.size(); i++) {
            Point p1 = sommets.get(i);
            Point p2 = sommets.get((i + 1) % sommets.size());
            perimetre += p1.distance(p2);
        }
        return perimetre;
    }
}
