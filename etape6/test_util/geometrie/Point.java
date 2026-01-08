public class Point {
    private int x;
    private int y;
    private String nom;
    
    public Point(String nom, int x, int y) {
        this.nom = nom;
        this.x = x;
        this.y = y;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public String getNom() { return nom; }
    
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    
    public double distance(Point autre) {
        int dx = this.x - autre.x;
        int dy = this.y - autre.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
