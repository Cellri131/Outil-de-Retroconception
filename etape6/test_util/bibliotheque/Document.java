public abstract class Document {

    private static int nbDocuments = 0;

    protected String titre;
    protected String auteur;
    protected int annee;

    public Document(String titre, String auteur, int annee) {
        this.titre = titre;
        this.auteur = auteur;
        this.annee = annee;
        nbDocuments++;
    }
    
    public abstract String getType();
    
    public String getTitre() { return titre; }
    public String getAuteur() { return auteur; }
    public int getAnnee() { return annee; }
    
    public static int getNbDocuments() { return nbDocuments; }
}
