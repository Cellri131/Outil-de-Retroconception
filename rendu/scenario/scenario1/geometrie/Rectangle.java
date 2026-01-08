public class Rectangle {
    private Point coinSupGauche;
    private int largeur;
    private int hauteur;
    
    public Rectangle(Point coinSupGauche, int largeur, int hauteur) {
        this.coinSupGauche = coinSupGauche;
        this.largeur = largeur;
        this.hauteur = hauteur;
    }
    
    public double calculerAire() {
        return largeur * hauteur;
    }
    
    public double calculerPerimetre() {
        return 2 * (largeur + hauteur);
    }
}
