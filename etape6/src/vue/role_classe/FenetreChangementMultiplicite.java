package vue.role_classe;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JFrame;
import vue.PanneauDiagramme;


public class FenetreChangementMultiplicite extends JFrame
{

    private PanneauModif PanneauModif;
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


        this.PanneauModif = new PanneauModif(this.panDiag);
        this.add(this.PanneauModif, BorderLayout.CENTER);
        


        this.setVisible(true);

    }

}