package vue.role_classe;


import controleur.Controleur;
import vue.PanneauDiagramme;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.Toolkit;


public class FenetreChangementMultiplicite extends JFrame
{

    private PanelModif panelModif;
    private PanneauDiagramme panDiag;


    public FenetreChangementMultiplicite(PanneauDiagramme panDiag)
    {
        Toolkit   toolkit     = Toolkit.getDefaultToolkit();
        Dimension tailleEcran = toolkit.getScreenSize();
        
        int largeur = tailleEcran.width;
        int hauteur = tailleEcran.height;

        this.panDiag = panDiag;
        this.setTitle("Modification des multiplicités");
        this.setSize(350, 350);
        this.setLocation(largeur/2, hauteur/2);


        this.panelModif = new PanelModif(this.panDiag);
        this.add(this.panelModif, BorderLayout.CENTER);
        


        this.setVisible(true);

    }

}