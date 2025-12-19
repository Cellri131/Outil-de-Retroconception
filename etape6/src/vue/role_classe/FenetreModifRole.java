package vue.role_classe;

import vue.PanneauDiagramme;
import controleur.Controleur;
import java.awt.BorderLayout;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class FenetreModifRole extends JFrame
{
    private PanneauDiagramme panDiag;
    private PanelModifRole   panelModifRole;

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


        this.panelModifRole = new PanelModifRole(this.panDiag);
        this.add(this.panelModifRole, BorderLayout.CENTER);

        this.setVisible(true);

    }
}
