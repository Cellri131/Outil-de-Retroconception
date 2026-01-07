public class Livre extends Document implements IEmpruntable {
    private int nbPages;
    private String isbn;
    private String emprunteur;
    
    public Livre(String titre, String auteur, int annee, int nbPages, String isbn) {
        super(titre, auteur, annee);
        this.nbPages = nbPages;
        this.isbn = isbn;
        this.emprunteur = null;
    }
    
    @Override
    public String getType() {
        return "Livre";
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
    
    public int getNbPages() { return nbPages; }
    public String getIsbn() { return isbn; }
}
