package vue.role_classe;

import java.awt.*;
import javax.swing.JFrame;
import vue.PanneauDiagramme;

public class FenetreModifRole extends JFrame
{
    private PanneauDiagramme panDiag;
    private PanneauModifRole   PanneauModifRole;

    public FenetreModifRole(PanneauDiagramme panDiag) 
    {
        this.panDiag = panDiag;

        Toolkit   toolkit     = Toolkit.getDefaultToolkit();
        Dimension tailleEcran = toolkit.getScreenSize();

        int largeur = tailleEcran.width;
        int hauteur = tailleEcran.height;

        this.setTitle("Modifier Rôle");
        this.setSize(350, 200);
        this.setLocation(largeur/2, hauteur/2);


        this.PanneauModifRole = new PanneauModifRole(this.panDiag);
        this.add(this.PanneauModifRole, BorderLayout.CENTER);

        this.setVisible(true);

    }
}
