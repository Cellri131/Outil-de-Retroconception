public class DVD extends Document implements IEmpruntable {
    private int duree;
    private String emprunteur;
    
    public DVD(String titre, String auteur, int annee, int duree) {
        super(titre, auteur, annee);
        this.duree = duree;
        this.emprunteur = null;
    }
    
    @Override
    public String getType() {
        return "DVD";
    }
    
    @Override
    public void emprunter(String emprunteur) {
        this.emprunteur = emprunteur;
    }
    
    @Override
    public void retourner() {
        this.emprunteur = null;
    }
    
    @Override
    public boolean estDisponible() {
        return emprunteur == null;
    }
    
    @Override
    public String getEmprunteur() {
        return emprunteur;
    }
    
    public int getDuree() { return duree; }
}
