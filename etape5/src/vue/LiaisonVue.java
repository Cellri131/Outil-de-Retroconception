package vue;

import java.awt.*;

public class LiaisonVue {

    private BlocClasse blocOrigine;
    private BlocClasse blocDestination;

    public LiaisonVue(BlocClasse blocOrigine, BlocClasse blocDestination) {
        this.blocOrigine = blocOrigine;
        this.blocDestination = blocDestination;
    }

    public void dessiner(Graphics2D g) {
        // Récupérer les points de connexion (centres des blocs)
        int x1 = blocOrigine.getX() + blocOrigine.getLargeur() / 2;
        int y1 = blocOrigine.getY() + 15; // En haut du bloc

        int x2 = blocDestination.getX() + blocDestination.getLargeur() / 2;
        int y2 = blocDestination.getY() + 15;

        // Trait plein simple pour association
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1));
        g.drawLine(x1, y1, x2, y2);
        dessinerFlecheSimple(g, x1, y1, x2, y2);
    }

    private void dessinerFlecheSimple(Graphics2D g, int x1, int y1, int x2, int y2) {
        // Calculer l'angle de la flèche
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int flecheSize = 10;

        // Points de la flèche
        int px2 = (int) (x2 - flecheSize * Math.cos(angle - Math.PI / 6));
        int py2 = (int) (y2 - flecheSize * Math.sin(angle - Math.PI / 6));
        int px3 = (int) (x2 - flecheSize * Math.cos(angle + Math.PI / 6));
        int py3 = (int) (y2 - flecheSize * Math.sin(angle + Math.PI / 6));

        g.drawLine(x2, y2, px2, py2);
        g.drawLine(x2, y2, px3, py3);
    }

    // Getters et Setters
    public BlocClasse getBlocOrigine() {
        return blocOrigine;
    }

    public void setBlocOrigine(BlocClasse blocOrigine) {
        this.blocOrigine = blocOrigine;
    }

    public BlocClasse getBlocDestination() {
        return blocDestination;
    }

    public void setBlocDestination(BlocClasse blocDestination) {
        this.blocDestination = blocDestination;
    }
}
