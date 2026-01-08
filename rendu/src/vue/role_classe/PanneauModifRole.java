package vue.role_classe;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import vue.PanneauDiagramme;
import vue.liaison.LiaisonVue;

/**
 * Panel permettant de modifier le rôle d'une liaison pour un bloc sélectionné.
 * Affiche les associations et interfaces liées au bloc avec des radio-boutons séparés par type.
 */
public class PanneauModifRole extends JPanel implements ActionListener
{
    private final PanneauDiagramme panDiag;
    private final String           nomBlocSelectionne;

    private final JLabel           lblTitre;
    private final JLabel           lblTitreRole;
    private final JTextField       txtNomRole;

    private final JPanel           panelListeLiaisons;
    private final JScrollPane      scrollPane;
    private final JPanel           panelBoutons;

    private final JButton          btnValider;
    private final JButton          btnAnnuler;

    private final ButtonGroup      groupeRadio;
    private       LiaisonVue       liaisonSelectionnee;

    /**
     * Constructeur.
     *
     * Initialise le panel avec le bloc sélectionné, le champ de rôle, la liste des liaisons et les boutons.
     *
     * @param panDiag Panneau diagramme contenant le bloc cliqué et la liste des liaisons
     */
    public PanneauModifRole(PanneauDiagramme panDiag)
    {
        this.panDiag            = panDiag;
        this.nomBlocSelectionne = panDiag.getBlocClique().getNom();

        this.setLayout(new BorderLayout());

        // Panel en haut pour le titre + nom du bloc sélectionné
        JPanel panelHaut = new JPanel();
        panelHaut.setLayout(new BorderLayout());

        // Titre principal
        this.lblTitre = new JLabel("Modification des rôles : [ " + nomBlocSelectionne + " ]", JLabel.CENTER);
        this.lblTitre.setHorizontalAlignment(JLabel.CENTER);
        this.lblTitre.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelHaut.add(lblTitre, BorderLayout.NORTH);

        // Panel pour saisir le rôle
        JPanel panelRole = new JPanel(new FlowLayout(FlowLayout.CENTER));
        this.lblTitreRole = new JLabel("Définir rôle : ");
        this.txtNomRole   = new JTextField(20);
        panelRole.add(this.lblTitreRole);
        panelRole.add(this.txtNomRole);
        panelHaut.add(panelRole, BorderLayout.CENTER);

        // Ajouter le panelHaut en haut du panel principal
        this.add(panelHaut, BorderLayout.NORTH);

        // Panel scrollable pour sélectionner la liaison
        this.panelListeLiaisons   = new JPanel();
        this.panelListeLiaisons.setLayout(new BoxLayout(this.panelListeLiaisons, BoxLayout.Y_AXIS));
        this.scrollPane           = new JScrollPane(this.panelListeLiaisons);
        this.scrollPane.setPreferredSize(new Dimension(400, 200));
        this.add(scrollPane, BorderLayout.CENTER);

        // Boutons Valider / Annuler
        this.panelBoutons = new JPanel(new FlowLayout());
        this.btnValider   = new JButton("Valider");
        this.btnAnnuler   = new JButton("Annuler");
        this.panelBoutons.add(btnValider);
        this.panelBoutons.add(btnAnnuler);
        this.add(panelBoutons, BorderLayout.PAGE_END);

        // Groupe pour radio-boutons
        this.groupeRadio = new ButtonGroup();

        // Remplir la liste des liaisons avec séparation Associations / Interfaces
        remplirListeLiaisons();

        this.btnValider.addActionListener(this);
        this.btnAnnuler.addActionListener(this);
    }

    /**
     * Remplit la liste des liaisons avec radio-boutons.
     * Affiche séparément les associations et les interfaces.
     * Seules les liaisons liées au bloc sélectionné sont affichées.
     */
    private void remplirListeLiaisons()
    {
        // Titre Associations
        JLabel lblAssoc = new JLabel("Associations");
        lblAssoc.setFont(lblAssoc.getFont().deriveFont(Font.BOLD));
        this.panelListeLiaisons.add(lblAssoc);

        for (LiaisonVue l : this.panDiag.getLstLiaisons())
        {
            if (!l.getType().equals("association"))
            {
                continue;
            }

            ajouterLiaisonALaListe(l);
        }

        // Titre Interfaces
        JLabel lblInterface = new JLabel("Interfaces");
        lblInterface.setFont(lblInterface.getFont().deriveFont(Font.BOLD));
        this.panelListeLiaisons.add(lblInterface);

        for (LiaisonVue l : this.panDiag.getLstLiaisons())
        {
            if (!l.getType().equals("interface"))
            {
                continue;
            }

            ajouterLiaisonALaListe(l);
        }

        this.panelListeLiaisons.revalidate();
        this.panelListeLiaisons.repaint();
    }

    /**
     * Crée un radio-bouton pour une liaison et l'ajoute au panel.
     * @param l LiaisonVue à ajouter
     */
    private void ajouterLiaisonALaListe(LiaisonVue l)
    {
        boolean blocOrigine     = l.getBlocOrigine().equals(this.panDiag.getBlocClique());
        boolean blocDestination = l.getBlocDestination().equals(this.panDiag.getBlocClique());

        if (!blocOrigine && !blocDestination)
        {
            return;
        }

        // Création de la ligne
        JPanel ligne      = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String texteLigne = l.isUnidirectionnel() ?
                            l.getBlocOrigine().getNom() + " → " + l.getBlocDestination().getNom() :
                            l.getBlocOrigine().getNom() + " ↔ " + l.getBlocDestination().getNom();

        JRadioButton radio = new JRadioButton(texteLigne);
        radio.addActionListener(e ->
        {
            this.liaisonSelectionnee = l;

            if (blocOrigine)
            {
                this.txtNomRole.setText(l.getRoleOrig());
            }
            else if (blocDestination)
            {
                this.txtNomRole.setText(l.getRoleDest());
            }
        });

        this.groupeRadio.add(radio);
        ligne.add(radio);
        this.panelListeLiaisons.add(ligne);
    }

    /**
     * Gère les actions des boutons Valider et Annuler.
     *
     * @param e ActionEvent généré par le bouton
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == this.btnValider)
        {
            if (this.liaisonSelectionnee != null)
            {
                String role            = this.txtNomRole.getText();
                boolean modifieOrigine = this.panDiag.getBlocClique().equals(this.liaisonSelectionnee.getBlocOrigine());

                panDiag.modifierRole(this.liaisonSelectionnee.getId(), modifieOrigine, role);
				panDiag.rafraichirDiagramme();
                SwingUtilities.getWindowAncestor(this).dispose();
            }
        }
        else if (e.getSource() == this.btnAnnuler)
        {
            SwingUtilities.getWindowAncestor(this).dispose();
        }
    }
}
