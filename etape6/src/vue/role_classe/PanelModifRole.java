package vue.role_classe;

import vue.PanneauDiagramme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.*;
import vue.liaison.LiaisonVue;
import java.awt.event.*;

public class PanelModifRole extends JPanel implements  ActionListener
{
    private PanneauDiagramme panDiag;

    private JLabel     lblTitre;

    private JLabel     lblTitreRole;
    private JTextField txtNomRole;

    private JPanel panelTexte;
    private JPanel panelBoutons;

    private JButton btnValider;
    private JButton btnAnnuler;

    private List<LiaisonVue> listeLiaisons;

    public PanelModifRole(PanneauDiagramme panDiag)
    {
        this.panDiag = panDiag;

        this.setLayout(new BorderLayout());
        
        this.lblTitre = new JLabel("Modification : [ BLOC CLIQUE ] ",  JLabel.CENTER);

        this.lblTitreRole = new JLabel("Définir rôle : ");
        this.txtNomRole   = new JTextField(20);

        this.btnValider = new JButton("Valider");
        this.btnAnnuler = new JButton("Annuler");

        this.panelTexte   = new JPanel();
        this.panelBoutons = new JPanel();

        this.panelTexte.setLayout(new FlowLayout());
        this.panelBoutons.setLayout(new FlowLayout());

        //Ajout des éléments
        this.add(lblTitre, BorderLayout.NORTH);

        this.panelTexte.add(lblTitreRole);
        this.panelTexte.add(txtNomRole);
        this.add(panelTexte, BorderLayout.CENTER);

        this.panelBoutons.add(this.btnValider);
        this.panelBoutons.add(this.btnAnnuler);
        this.add(panelBoutons, BorderLayout.SOUTH);

        this.btnValider.addActionListener(this);
        this.btnAnnuler.addActionListener(this);

        this.setVisible(true);
    }


    @Override
    public void actionPerformed(ActionEvent e) 
    {
        if (e.getSource() == btnValider) 
        {
            String role = this.txtNomRole.getText();
            panDiag.modifierRole(panDiag.getIdLiaison(), panDiag.isOrigineLiaison() , role);
            SwingUtilities.getWindowAncestor(this).dispose();
        } 
        else if (e.getSource() == btnAnnuler) 
        {
            SwingUtilities.getWindowAncestor(this).dispose();
        }
    }
}
