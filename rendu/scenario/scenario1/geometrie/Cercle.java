public class Cercle {
    private Point centre;
    private double rayon;
    
    public Cercle(Point centre, double rayon) {
        this.centre = centre;
        this.rayon = rayon;
    }
    
    public double calculerAire() {
        return Math.PI * rayon * rayon;
    }
    
    public double calculerPerimetre() {
        return 2 * Math.PI * rayon;
    }
    
    public Point getCentre() { return centre; }
    public double getRayon() { return rayon; }
}
