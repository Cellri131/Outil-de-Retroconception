package test;

import java.util.ArrayList;

public class Disque {
    private String nom;
    private ArrayList<Point> points;
    private int rayon;

    public Disque(String nom, int rayon) {
        this.nom = nom;
        this.rayon = rayon;
        this.points = new ArrayList<Point>();
    }

    public void ajouterPoint(Point p) {
        this.points.add(p);
    }

    public ArrayList<Point> getPoints() {
        return this.points;
    }

    public String getNom() {
        return this.nom;
    }

    public int getRayon() {
        return this.rayon;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setRayon(int rayon) {
        this.rayon = rayon;
    }
}
