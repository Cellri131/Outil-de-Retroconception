import java.util.ArrayList;
import java.util.List;

public class Bibliotheque {
    private String nom;
    private String adresse;
    private List<Document> catalogue;
    
    public Bibliotheque(String nom, String adresse) {
        this.nom = nom;
        this.adresse = adresse;
        this.catalogue = new ArrayList<>();
    }
    
    public void ajouterDocument(Document doc) {
        catalogue.add(doc);
    }
    
    public void retirerDocument(Document doc) {
        catalogue.remove(doc);
    }
    
    public int getNbDocuments() {
        return catalogue.size();
    }
    
    public List<Document> getCatalogue() {
        return new ArrayList<>(catalogue);
    }
}
