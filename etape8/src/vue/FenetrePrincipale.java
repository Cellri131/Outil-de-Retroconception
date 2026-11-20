package vue;

import controleur.Controleur;
import java.awt.BorderLayout;
import java.awt.Dimension; 
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import metier.lecture.AnalyseurFonctionnement;
import metier.lecture.AnalyseurFonctionnement.Graphe;
import vue.liaison.LiaisonVue;

/**
* Fenêtre principale de l'IHM du générateur de diagramme UML.
* Gère également la liasion avec le controleur.
* @author Jules
*/
public class FenetrePrincipale extends JFrame 
{
    //--------------------------//
    //        ATTRIBUTS         //
    //--------------------------//

    private PanneauProjets   panneauProjets  ;
    private PanneauDiagramme panneauDiagramme;
    private Controleur       controleur      ;
    private boolean analyseFonctionnementEnCours;

    //-------------------------//
    //      CONSTRUCTEUR       //
    //-------------------------//
    
    public FenetrePrincipale(Controleur controleur) 
    {
        setTitle("Générateur de diagramme UML");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setMinimumSize(new Dimension(700,400));
        setLocationRelativeTo(null);
        setResizable(true);

        panneauProjets   = new PanneauProjets  (this);
        panneauDiagramme = new PanneauDiagramme(this);
        panneauProjets.setBackground(new java.awt.Color(230, 230, 250)); // lavande clair);
        panneauDiagramme.setBackground(new java.awt.Color(230, 230, 250));
        
        this.controleur  = controleur;

        setLayout(new BorderLayout());
        
        // La ligne de division entre panel projet et panel diagramme
        JSplitPane splitPane = new JSplitPane
        (
            JSplitPane.HORIZONTAL_SPLIT,
            true,
            panneauProjets,
            panneauDiagramme
        );

        splitPane.setDividerLocation(250);
        splitPane.setOneTouchExpandable(true);

        this.add(splitPane           , BorderLayout.CENTER);
        this.add(new BarreMenus(this), BorderLayout.NORTH );

    }

    //----------------------//
    //      METHODES        //
    //----------------------//

    public void ouvrirProjet(String cheminProjet) 
    {
        try
        {
            panneauDiagramme.chargerProjet(cheminProjet);
            // Enregistrer le projet au premier chargement
            controleur.sauvegardeProjetXml(cheminProjet);
            panneauProjets.actualiser();

            // Forcer le recalcul des liaisons après le layout Swing
            // pour eviter un mauvais placement des fléches
            SwingUtilities.invokeLater(() ->
            {
                    revalidate();
                    repaint();
            });

        }

        catch (Exception e)
        {
            String message = "Erreur lors du chargement du projet :\n";
            
            if (e instanceof java.io.FileNotFoundException)
            {
                message += "Fichier ou dossier introuvable ou accès refusé.\n";
                message += "Vérifiez que vous avez les permissions nécessaires.";
            }
            else
            {
                message += e.getMessage();
            }
            
            javax.swing.JOptionPane.showMessageDialog
            (
                this, 
                message, 
                "Erreur de chargement", 
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }

    public void afficherFonctionnement()
    {
        String chemin = panneauDiagramme.getCheminProjetCourant();
        if (chemin == null)
        {
            JOptionPane.showMessageDialog(this, "Ouvrez d'abord un projet.", "Fonctionnement", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (analyseFonctionnementEnCours)
        {
            JOptionPane.showMessageDialog(this, "L'analyse du fonctionnement est déjà en cours.", "Fonctionnement", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        analyseFonctionnementEnCours = true;
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        new SwingWorker<Graphe, Void>()
        {
            @Override protected Graphe doInBackground()
            {
                return new AnalyseurFonctionnement().analyser(chemin);
            }

            @Override protected void done()
            {
                try
                {
                    Graphe graphe = get();
                    if (graphe.noeuds().isEmpty())
                    {
                        JOptionPane.showMessageDialog(FenetrePrincipale.this,
                            "Aucune classe ou méthode reconnue dans les fichiers Java, Python ou C#.",
                            "Fonctionnement", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    JFrame fenetre = new JFrame("Fonctionnement du code — " + chemin);
                    fenetre.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    fenetre.setContentPane(new PanneauFonctionnement(graphe));
                    fenetre.setSize(1400, 800);
                    fenetre.setLocationRelativeTo(FenetrePrincipale.this);
                    fenetre.setVisible(true);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(FenetrePrincipale.this, "Analyse interrompue.",
                        "Fonctionnement", JOptionPane.ERROR_MESSAGE);
                }
                catch (ExecutionException e)
                {
                    Throwable cause = e.getCause();
                    JOptionPane.showMessageDialog(FenetrePrincipale.this,
                        "Impossible d'analyser le fonctionnement :\n" + cause.getMessage(),
                        "Fonctionnement", JOptionPane.ERROR_MESSAGE);
                    cause.printStackTrace();
                }
                finally
                {
                    analyseFonctionnementEnCours = false;
                    setCursor(java.awt.Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    public void exporterImageDiagramme()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY); // choisir un fichier
        chooser.setSelectedFile(new File("diagramme.png")); // nom par défaut

        int resultat = chooser.showSaveDialog(panneauDiagramme);

        if (resultat == JFileChooser.APPROVE_OPTION) 
        {
            File fichierSortie = chooser.getSelectedFile();
            if (!fichierSortie.getName().toLowerCase().endsWith(".png")) 
            {
                fichierSortie = new File(fichierSortie.getParentFile(), fichierSortie.getName() + ".png");
            }

            try 
            {
                // Sauvegarder le zoom actuel et le réinitialiser pour l'export
                double  zoomSauvegarde     = panneauDiagramme.getZoomLevel      ();
                boolean textZoomSauvegarde = panneauDiagramme.isAfficherTextZoom();

                panneauDiagramme.setAfficherTextZoom(false);

                BufferedImage image = new BufferedImage
                (
                    panneauDiagramme.getWidth(), panneauDiagramme.getHeight(), BufferedImage.TYPE_INT_ARGB
                );

                panneauDiagramme.printAll(image.createGraphics());
                ImageIO.write(image, "png", fichierSortie);

                String diagrammeSave = "Diagramme sauvegardé dans " + fichierSortie.getPath();
                JOptionPane.showMessageDialog(this, diagrammeSave, "Save Diagramme", JOptionPane.INFORMATION_MESSAGE);

                // Restaurer le zoom et l'affichage du texte
                panneauDiagramme.setZoomLevel       (zoomSauvegarde    );
                panneauDiagramme.setAfficherTextZoom(textZoomSauvegarde);
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
    }


    public void affichageAttributs(boolean b)
    {
        panneauDiagramme.setAfficherAttributs(b);
    }

    public void affichageMethodes(boolean b)
    {
        panneauDiagramme.setAfficherMethodes(b);
    }

    public void optimiserPositionsClasses()
    {
        panneauDiagramme.optimiserPositionsClasses();
    }

    public void optimiserPositionsLiaisons()
    {
        panneauDiagramme.optimiserPositionsLiaisons();
    }

    public void setSauvegardeAuto(boolean b)
    {
        panneauDiagramme.setSauvegardeAuto(b);
    }

    public void actionSauvegarder()
    {
        panneauDiagramme.actionSauvegarder();
    }

    /**
    * Méthodes passerelle au controleur
    */
    public void chargerProjet(String cheminProjet) 
    {
        controleur.chargerProjet(cheminProjet);
    }

    public boolean estProjetChargeDepuisSauvegarde()
    {
        return controleur.estProjetChargeDepuisSauvegarde();
    }

    public List<BlocClasse> getBlocClasses()
    {
        return controleur.getBlocClasses();
    }

    public List<LiaisonVue> getLiaisons()
    {
        return controleur.getLiaisons();
    }

    public ArrayList<String> getLstFichiersInvalides(String cheminProjet)
    {
        return this.controleur.getLstFichiersInvalides(cheminProjet);
    }

    public void sauvegarderClasses(List<BlocClasse> blocClasses, List<LiaisonVue> liaisonVues, String cheminProjet)
    {
        controleur.sauvegarderClasses(blocClasses, liaisonVues, cheminProjet);
    }

    public void viderDiagramme()
    {
        panneauDiagramme.viderDiagramme();
    }
}