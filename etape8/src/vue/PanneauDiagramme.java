package vue;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.*;
import vue.liaison.LiaisonVue;
import vue.liaison.RouteurLiaisons;
import vue.role_classe.*;



/**
 * Panneau principal affichant le diagramme de classes.
 * Gère les blocs de classes, les liaisons, le zoom, le pan, et les interactions utilisateur.
 */
public class PanneauDiagramme extends JPanel 
{
        //--------------------------//
    //        ATTRIBUTS         //
    //--------------------------//

    // Listes des blocs et liaisons du diagramme
    private List<BlocClasse>    lstBlocsClasses;
    private List<LiaisonVue>    lstLiaisons; 
    private RouteurLiaisons routeurLiaisons;

    // Fenêtre principale associée (contrôle et chargement des projets)
    private FenetrePrincipale   fenetrePrincipale;

    // Chemin du projet actuellement chargé
    private String              cheminProjetCourant;

    // Bloc actuellement déplacé par drag
    private BlocClasse          blocEnDeplacement;
    private Point               pointDernier;

    // Affichage des détails du bloc
    private boolean             afficherAttributs = true;
    private boolean             afficherMethodes  = true;
    private boolean             sauvegardeAuto    = false;

    // Pour le drag des points d'ancrage des liaisons
    private LiaisonVue          liaisonEnDeplacement;
    private boolean             draggingOriginAnchor;
    private boolean             draggingDestinationAnchor;

    // Zoom et limites
    private double              zoomLevel = 1.0;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 0.1;
    private boolean             afficherTextZoom = true;

    // Pan (déplacement du diagramme)
    private int                 panOffsetX = 0;
    private int                 panOffsetY = 0;
    private boolean             isPanning = false;
    private boolean             espaceEnfonce = false;

    // Bloc affiché temporairement en plein écran
    private BlocClasse          blocPleinEcranTemporaire = null;

    // Pour modification des rôles via menu contextuel
    private BlocClasse          blocClique;
    private UUID                idLiaison;
    private boolean             estOrigineLiaison = true;   

    // Menu contextuel pour changer multiplicité ou rôle
    private JPopupMenu          menuModif; 
    private JMenuItem           menuChangerMultiplicite;
    private JMenuItem           menuModifRole;



    //-------------------------//
    //      CONSTRUCTEUR       //
    //-------------------------//

    /**
     * Initialise le panneau et ses listeners
     */
    public PanneauDiagramme(FenetrePrincipale fenetrePrincipale) 
    {
        this.lstBlocsClasses           = new ArrayList<>();
        this.lstLiaisons               = new ArrayList<>();
        this.routeurLiaisons           = new RouteurLiaisons(lstBlocsClasses, lstLiaisons);
        this.cheminProjetCourant       = null;
        this.fenetrePrincipale         = fenetrePrincipale;

        // Création du menu contextuel
        this.menuModif                    = new JPopupMenu();
        this.menuChangerMultiplicite      = new JMenuItem("Modifier multiplicité");
		this.menuModifRole                = new JMenuItem("Modifier rôle");
       
        // Action du menu double clic gauche pour changer multiplicité
        this.menuChangerMultiplicite.addActionListener(ActionEvent -> {
            FenetreChangementMultiplicite fenetreChangementMultiplicite = new FenetreChangementMultiplicite(this);
            fenetreChangementMultiplicite.setVisible(true);
        });

        // Action du menu double clic gauche pour modifier un rôle
		this.menuModifRole.addActionListener(ActionEvent -> 
		{
            FenetreModifRole fenetreModifRole = new FenetreModifRole(this);
            fenetreModifRole.setVisible(true);

            // Chercher la liaison correspondante au bloc cliqué
            for (LiaisonVue liaisonVue : lstLiaisons)
            {
                if (liaisonVue.getBlocOrigine().equals(PanneauDiagramme.this.blocClique))
                {
                    this.idLiaison = liaisonVue.getId();
                    this.estOrigineLiaison = true;

                    break;
                }
                else if (liaisonVue.getBlocDestination().equals(PanneauDiagramme.this.blocClique))
                {
                    this.idLiaison = liaisonVue.getId();
                    this.estOrigineLiaison = false;
                    break;
                }
            }
        });

        // Ajouter les éléments au menu
        this.menuModif.add(this.menuChangerMultiplicite);
		this.menuModif.add(this.menuModifRole);

        setLayout(null);
        setBackground(new Color(255, 255, 255));
        setBorder(BorderFactory.createTitledBorder("Diagramme de classe"));
        
        // Augmenter la taille de la police par défaut pour la bordure
        Font defaultFont = UIManager.getFont("TitledBorder.font");
        if (defaultFont != null)
            UIManager.put("TitledBorder.font", new Font(defaultFont.getName(), Font.PLAIN, 14));

        // Ajouter les listeners pour interactions
        ajouterListenersInteraction();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed SPACE"), "panEspaceDebut");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released SPACE"), "panEspaceFin");
        getActionMap().put("panEspaceDebut", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e) { espaceEnfonce = true; }
        });
        getActionMap().put("panEspaceFin", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e) { espaceEnfonce = false; }
        });
    }

    public BlocClasse getBlocClasseClic()
    {
        return this.blocClique;
    }

    //----------------------//
    //      METHODES        //
    //----------------------//

    /**
     * Charge un projet depuis un fichier XML et initialise les blocs et liaisons
     */
    public void chargerProjet(String cheminProjet) throws Exception
    {
        this.cheminProjetCourant = cheminProjet;

        this.lstBlocsClasses.clear();
        this.lstLiaisons    .clear();

        fenetrePrincipale.chargerProjet(cheminProjet);

        // Récupérer les blocs et liaisons
        List<BlocClasse> blocCharges = fenetrePrincipale.getBlocClasses();
        lstBlocsClasses.addAll(blocCharges);
        lstLiaisons.addAll(fenetrePrincipale.getLiaisons());

        // Passer la liste des blocs à toutes les lstLiaisons pour le contournement
        for (LiaisonVue liaison : lstLiaisons)
        {
            liaison.setTousLesBlocs(lstBlocsClasses);
            liaison.setToutesLesLiaisons(lstLiaisons);
            liaison.setRouteur(routeurLiaisons);
        }

        if (!fenetrePrincipale.estProjetChargeDepuisSauvegarde())
        {
            optimiserPositionsClasses();
            optimiserPositionsLiaisons();
        }

        rafraichirDiagramme();
    }

    /**
     * Ajoute les listeners pour gérer les clics, drags, zoom et pan
     */
    private void ajouterListenersInteraction()
    {
        addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isMiddleMouseButton(e) ||
                    (SwingUtilities.isLeftMouseButton(e) && espaceEnfonce))
                {
                    isPanning = true;
                    pointDernier = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }
                // Vérifier si on clique sur le texte de zoom pour le réinitialiser
                // Zone cliquable : bas-gauche de la fenêtre
                int padding    = 10;
                int textHeight = 25;

                if (e.getX() >= padding                            &&
                    e.getX() <= padding + 100                      && 
                    e.getY() >= getHeight() - padding - textHeight &&
                    e.getY() <= getHeight() - padding             )
                {
                    zoomLevel  = 1.0;
                    panOffsetX = 0  ;
                    panOffsetY = 0  ;
                    repaint();
                    return;
                }
                
				// Convertir les coordonnées écran en coordonnées logiques (avec zoom et pan)
				double logicalX = (e.getX() - panOffsetX - getWidth() / 2) / zoomLevel + getWidth() / (2 * zoomLevel);
				double logicalY = (e.getY() - panOffsetY - getHeight() / 2) / zoomLevel + getHeight() / (2 * zoomLevel);

				// Vérifier sur quel bloc on clique
				PanneauDiagramme.this.blocClique = null;
				for (BlocClasse bloc : lstBlocsClasses) 
				{
					if (bloc.contient((int) logicalX, (int) logicalY)) 
					{
                        //System.out.println("OOOOOOOOOOOOOOOOOOOOOOOO Bloc cliqué mis à jour : " + bloc.getNom());
						PanneauDiagramme.this.blocClique = bloc;
						break;
					}
				}

				// --- DOUBLE CLIC GAUCHE pour ouvrir le menu ---
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && blocClique != null) 
				{
					PanneauDiagramme.this.menuModif.show(e.getComponent(), e.getX(), e.getY());
					return; // on sort après avoir affiché le menu
				}

				// --- CLIC GAUCHE MAINTENU pour drag ---
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1 && blocClique != null) 
				{
					blocEnDeplacement = blocClique;
					blocClique.setSelectionne(true);
					pointDernier = e.getPoint();
				}

				// --- CLIC DROIT pour pan / plein écran ---
				if (SwingUtilities.isRightMouseButton(e)) 
				{
					if (blocClique != null) 
					{
						blocPleinEcranTemporaire = blocClique;
						blocClique.setAffichagePleinEcran(true);
						repaint();
						return;
					}
					isPanning = true;
					pointDernier = e.getPoint();
					return;
				}

				// Réinitialisation par défaut
				pointDernier = e.getPoint();
				liaisonEnDeplacement = null;
				draggingOriginAnchor = false;
				draggingDestinationAnchor = false;

                // Vérifier si on clique sur un point d'ancrage de liaison
                for (LiaisonVue liaison : lstLiaisons)
                {
                    if (liaison.isOnOriginAnchor(e.getPoint(), zoomLevel, panOffsetX, panOffsetY, getWidth(), getHeight()))
                    {
                        liaisonEnDeplacement = liaison;
                        draggingOriginAnchor = true   ;
                        return;
                    }
                    if (liaison.isOnDestinationAnchor(e.getPoint(), zoomLevel, panOffsetX, panOffsetY, getWidth(), getHeight()))
                    {
                        liaisonEnDeplacement      = liaison;
                        draggingDestinationAnchor = true   ;
                        return;
                    }
                }

                // Sinon, vérifier si on clique sur un bloc
                for (BlocClasse bloc : lstBlocsClasses)
                {
                    if (bloc.contient((int) logicalX, (int) logicalY))
                    {
                        blocEnDeplacement = bloc ;
                        bloc.setSelectionne(true);
                        break;
                    }
                }

                repaint();
            }

            public void mouseReleased(MouseEvent e)
            {
                if (isPanning)
                {
                    isPanning = false;
                    setCursor(Cursor.getDefaultCursor());

                    repaint();
                    return   ;
                }
                
                // Désactiver l'affichage plein écran temporaire
                if (blocPleinEcranTemporaire != null)
                {
                    blocPleinEcranTemporaire.setAffichagePleinEcran(false);
                    blocPleinEcranTemporaire = null                       ;
                    repaint();
                }
                
                if (blocEnDeplacement != null)
                    blocEnDeplacement.setSelectionne(false);

                blocEnDeplacement         = null ;
                liaisonEnDeplacement      = null ;
                draggingOriginAnchor      = false;
                draggingDestinationAnchor = false;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter()
        {
            public void mouseDragged(MouseEvent e)
            {
                // Pan avec clic droit
                if (isPanning)
                {
                    panOffsetX += e.getX() - pointDernier.x;
                    panOffsetY += e.getY() - pointDernier.y;
                    
                    pointDernier = e.getPoint();
                    repaint();
                    return;
                }

                // Drag d'un point d'ancrage de liaison
                if (liaisonEnDeplacement != null)
                {
                    if (draggingOriginAnchor)
                    {
                        liaisonEnDeplacement.dragOriginAnchor     (e.getPoint(), zoomLevel, panOffsetX, panOffsetY, getWidth(), getHeight());
                    }
                    else if (draggingDestinationAnchor)
                    {
                        liaisonEnDeplacement.dragDestinationAnchor(e.getPoint(), zoomLevel, panOffsetX, panOffsetY, getWidth(), getHeight());
                    }
                    repaint();
                    return;
                }

                // Drag d'un bloc
                if (blocEnDeplacement != null)
                {
                    // Convertir les déplacements en fonction du zoom
                    double dx = (e.getX() - pointDernier.x) / zoomLevel;
                    double dy = (e.getY() - pointDernier.y) / zoomLevel;

                    // Vérifier les limites pour éviter un déplacement hors zone
                    int newX = blocEnDeplacement.getX() + (int) dx;
                    int newY = blocEnDeplacement.getY() + (int) dy;

                    // Empêcher le déplacement hors de la zone de travail
                    if (newX >= 0 && newY >= 0)
                    {
                        blocEnDeplacement.deplacer((int) dx, (int) dy);
                        pointDernier = e.getPoint();
                    }
                    else if (newX >= 0 && newY < 0)
                    {
                        blocEnDeplacement.deplacer((int) dx, 0);
                        pointDernier = e.getPoint();
                    }
                    else if (newX < 0 && newY >= 0)
                    {
                        blocEnDeplacement.deplacer(0, (int) dy);
                        pointDernier = e.getPoint();
                    }

                    repaint();
                }
            }            
        });

        // Listener pour la molette de souris (zoom)
        addMouseWheelListener(new MouseWheelListener()
        {
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                if (e.isControlDown())
                {
                    double oldZoom = zoomLevel                                        ;
                    zoomLevel     -= e.getWheelRotation() * ZOOM_STEP                 ;
                    zoomLevel      = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomLevel));

                    if (zoomLevel != oldZoom)
                        repaint();
                }
            }
        });
    }

    //----------------------//
    //  OPTIMISATION LAYOUT //
    //----------------------//

     /**
     * Organise les blocs et liaisons pour que le diagramme soit lisible.
     * Regroupe les blocs liés et utilise une grille stable.
     */
    public void optimiserPositionsClasses() 
    {
        if (lstBlocsClasses.isEmpty()) 
            return;

        java.util.Map<BlocClasse, Integer> degres = new java.util.IdentityHashMap<>();
        for (BlocClasse bloc : lstBlocsClasses) degres.put(bloc, 0);
        for (LiaisonVue liaison : lstLiaisons)
        {
            degres.computeIfPresent(liaison.getBlocOrigine(), (b, d) -> d + 1);
            degres.computeIfPresent(liaison.getBlocDestination(), (b, d) -> d + 1);
        }
        java.util.Comparator<BlocClasse> ordreBlocs = java.util.Comparator
            .comparingInt((BlocClasse bloc) -> -degres.get(bloc))
            .thenComparing(BlocClasse::getNom);
        lstBlocsClasses.sort(ordreBlocs);
        List<BlocClasse> ordre = new ArrayList<>();
        java.util.Set<BlocClasse> visites = new java.util.HashSet<>();
        for (BlocClasse depart : lstBlocsClasses)
        {
            if (!visites.add(depart)) continue;
            java.util.ArrayDeque<BlocClasse> file = new java.util.ArrayDeque<>();
            file.add(depart);
            while (!file.isEmpty())
            {
                BlocClasse bloc = file.remove();
                ordre.add(bloc);
                List<BlocClasse> voisins = new ArrayList<>();
                for (LiaisonVue liaison : lstLiaisons)
                {
                    if (liaison.getBlocOrigine() == bloc) voisins.add(liaison.getBlocDestination());
                    else if (liaison.getBlocDestination() == bloc) voisins.add(liaison.getBlocOrigine());
                }
                voisins.sort(ordreBlocs);
                for (BlocClasse voisin : voisins)
                    if (lstBlocsClasses.contains(voisin) && visites.add(voisin)) file.add(voisin);
            }
        }
        lstBlocsClasses.clear();
        lstBlocsClasses.addAll(ordre);
        organiserEnGrille();
        rapprocherBlocsLies();

        // Rafraîchir l’affichage
        rafraichirDiagramme();
    }

    private void rapprocherBlocsLies()
    {
        long score = scorePlacement();
        int evaluations = 0;
        for (int passe = 0; passe < Math.min(lstBlocsClasses.size() * 2, 20) && evaluations < 4000; passe++)
        {
            BlocClasse meilleurA = null, meilleurB = null;
            long meilleurScore = score;
            recherche:
            for (int i = 0; i < lstBlocsClasses.size(); i++)
                for (int j = i + 1; j < lstBlocsClasses.size(); j++)
                {
                    if (evaluations++ >= 4000) break recherche;
                    BlocClasse a = lstBlocsClasses.get(i), b = lstBlocsClasses.get(j);
                    echangerPositions(a, b);
                    long candidat = scorePlacement();
                    echangerPositions(a, b);
                    if (candidat < meilleurScore)
                    {
                        meilleurScore = candidat;
                        meilleurA = a;
                        meilleurB = b;
                    }
                }
            if (meilleurA == null) break;
            echangerPositions(meilleurA, meilleurB);
            score = meilleurScore;
        }
    }

    private void echangerPositions(BlocClasse a, BlocClasse b)
    {
        int x = a.getX(), y = a.getY();
        a.setX(b.getX());
        a.setY(b.getY());
        b.setX(x);
        b.setY(y);
    }

    private long scorePlacement()
    {
        long score = 0;
        for (int i = 0; i < lstLiaisons.size(); i++)
        {
            LiaisonVue a = lstLiaisons.get(i);
            BlocClasse a1 = a.getBlocOrigine(), a2 = a.getBlocDestination();
            int x1 = a1.getX() + a1.getLargeur() / 2, y1 = a1.getY() + a1.getHauteurCalculee() / 2;
            int x2 = a2.getX() + a2.getLargeur() / 2, y2 = a2.getY() + a2.getHauteurCalculee() / 2;
            score += Math.abs((long)x2 - x1) + Math.abs((long)y2 - y1);
            for (int j = 0; j < i && lstLiaisons.size() <= 60; j++)
            {
                LiaisonVue b = lstLiaisons.get(j);
                BlocClasse b1 = b.getBlocOrigine(), b2 = b.getBlocDestination();
                if (a1 == b1 || a1 == b2 || a2 == b1 || a2 == b2) continue;
                if (java.awt.geom.Line2D.linesIntersect(x1, y1, x2, y2,
                    b1.getX() + b1.getLargeur() / 2, b1.getY() + b1.getHauteurCalculee() / 2,
                    b2.getX() + b2.getLargeur() / 2, b2.getY() + b2.getHauteurCalculee() / 2))
                    score += 300;
            }
        }
        return score;
    }


    /**
     * Organise les blocs en une grille régulière avec espacement optimal
     */
    private void organiserEnGrille()
    {
        int cols     = (int) Math.ceil(Math.sqrt(lstBlocsClasses.size()));
        int spacingX = 300;
        int spacingY = 300;                                         ;
        int startX   = 50                                             ;
        int startY   = 50                                             ;

        for (BlocClasse bloc : lstBlocsClasses)
        {
            //System.out.println(bloc.getNom() + " X=" + bloc.getX() + " Y=" + bloc.getY());
            spacingY = Math.max(spacingY, bloc.getHauteurCalculee() + 100);
            spacingX = Math.max(spacingX, bloc.getLargeur() + 100);
        }

        for (int i = 0; i < lstBlocsClasses.size(); i++)
        {
            BlocClasse bloc = lstBlocsClasses.get(i);

            int col  = i % cols               ;
            int row  = i / cols               ;
            int newX = startX + col * spacingX;
            int newY = startY + row * spacingY;

            bloc.setX(newX);
            bloc.setY(newY);
        }
    }

    public void optimiserPositionsLiaisons()
    {
        if (lstLiaisons.isEmpty())
            return;

        lstLiaisons.sort(java.util.Comparator
            .comparing((LiaisonVue l) -> l.getBlocOrigine().getNom())
            .thenComparing(l -> l.getBlocDestination().getNom())
            .thenComparing(LiaisonVue::getType, java.util.Comparator.nullsFirst(String::compareTo))
            .thenComparing(LiaisonVue::getRoleOrig, java.util.Comparator.nullsFirst(String::compareTo))
            .thenComparing(LiaisonVue::getRoleDest, java.util.Comparator.nullsFirst(String::compareTo)));

        for (LiaisonVue liaison : lstLiaisons)
        {
            liaison.setTousLesBlocs(lstBlocsClasses);
            liaison.setToutesLesLiaisons(lstLiaisons);
            liaison.setRouteur(routeurLiaisons);
        }
        
        // Choisir les côtés indépendamment de l'état de la précédente optimisation.
        for (LiaisonVue liaison : lstLiaisons)
        {
            liaison.setToutesLesLiaisons(List.of());
            liaison.recalculerAncrages();
        }
        for (LiaisonVue liaison : lstLiaisons)
            liaison.setToutesLesLiaisons(lstLiaisons);

        repartirAncrages();

        // Redessiner
        repaint();
    }

    private static class AncrageOptimise
    {
        final LiaisonVue liaison;
        final boolean origine;

        AncrageOptimise(LiaisonVue liaison, boolean origine)
        {
            this.liaison = liaison;
            this.origine = origine;
        }

        BlocClasse bloc() { return origine ? liaison.getBlocOrigine() : liaison.getBlocDestination(); }
        BlocClasse autre() { return origine ? liaison.getBlocDestination() : liaison.getBlocOrigine(); }
        int cote() { return origine ? liaison.getCoteOrigine() : liaison.getCoteDestination(); }
        void cote(int cote)
        {
            if (origine) liaison.setSideOrigine(cote);
            else liaison.setSideDestination(cote);
        }
        void position(double position)
        {
            if (origine) liaison.setPosRelOrig(position);
            else liaison.setPosRelDest(position);
        }
    }

    private void repartirAncrages()
    {
        java.util.Map<BlocClasse, List<List<AncrageOptimise>>> parBloc = new java.util.IdentityHashMap<>();
        for (BlocClasse bloc : lstBlocsClasses)
        {
            List<List<AncrageOptimise>> cotes = new ArrayList<>();
            for (int i = 0; i < 4; i++) cotes.add(new ArrayList<>());
            parBloc.put(bloc, cotes);
        }
        for (LiaisonVue liaison : lstLiaisons)
        {
            AncrageOptimise origine = new AncrageOptimise(liaison, true);
            AncrageOptimise destination = new AncrageOptimise(liaison, false);
            parBloc.get(origine.bloc()).get(origine.cote()).add(origine);
            parBloc.get(destination.bloc()).get(destination.cote()).add(destination);
        }
        for (BlocClasse bloc : lstBlocsClasses)
        {
            List<List<AncrageOptimise>> cotes = parBloc.get(bloc);
            for (int cote = 0; cote < 4; cote++)
            {
                List<AncrageOptimise> ancrages = cotes.get(cote);
                int taille = (cote == 1 || cote == 3) ? bloc.getHauteurCalculee() : bloc.getLargeur();
                int capacite = Math.max(1, (taille - 20) / 18);
                while (ancrages.size() > capacite)
                {
                    AncrageOptimise choisi = null;
                    int nouveauCote = -1;
                    long meilleureDistance = Long.MAX_VALUE;
                    for (AncrageOptimise ancrage : ancrages)
                        for (int adjacent : cote % 2 == 1 ? new int[] {2, 0} : new int[] {1, 3})
                        {
                            int tailleAdjacente = (adjacent == 1 || adjacent == 3)
                                ? bloc.getHauteurCalculee() : bloc.getLargeur();
                            if (cotes.get(adjacent).size() >= Math.max(1, (tailleAdjacente - 20) / 18))
                                continue;
                            BlocClasse autre = ancrage.autre();
                            int cx = autre.getX() + autre.getLargeur() / 2;
                            int cy = autre.getY() + autre.getHauteurCalculee() / 2;
                            int bx = bloc.getX() + bloc.getLargeur() / 2;
                            int by = bloc.getY() + bloc.getHauteurCalculee() / 2;
                            if (adjacent == 0) by = bloc.getY();
                            else if (adjacent == 1) bx = bloc.getX() + bloc.getLargeur();
                            else if (adjacent == 2) by = bloc.getY() + bloc.getHauteurCalculee();
                            else bx = bloc.getX();
                            long distance = Math.abs((long)cx - bx) + Math.abs((long)cy - by);
                            if (distance < meilleureDistance)
                            {
                                meilleureDistance = distance;
                                choisi = ancrage;
                                nouveauCote = adjacent;
                            }
                        }
                    if (choisi == null) break;
                    ancrages.remove(choisi);
                    choisi.cote(nouveauCote);
                    cotes.get(nouveauCote).add(choisi);
                }
            }
        }
        for (List<List<AncrageOptimise>> cotes : parBloc.values())
            for (int cote = 0; cote < 4; cote++)
            {
                final boolean vertical = cote == 1 || cote == 3;
                List<AncrageOptimise> ancrages = cotes.get(cote);
                ancrages.sort(java.util.Comparator.comparingInt(a -> vertical
                    ? a.autre().getY() + a.autre().getHauteurCalculee() / 2
                    : a.autre().getX() + a.autre().getLargeur() / 2));
                for (int i = 0; i < ancrages.size(); i++)
                    ancrages.get(i).position((i + 1.0) / (ancrages.size() + 1.0));
            }

        // Pour les faisceaux passant du même côté, inverser l'ordre à l'arrivée pour les imbriquer.
        java.util.Set<List<AncrageOptimise>> cotesTraites =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (LiaisonVue liaison : lstLiaisons)
        {
            int cote = liaison.getCoteOrigine();
            if (liaison.getBlocOrigine() == liaison.getBlocDestination() || cote != liaison.getCoteDestination())
                continue;
            List<AncrageOptimise> origines = parBloc.get(liaison.getBlocOrigine()).get(cote);
            List<AncrageOptimise> destinations = parBloc.get(liaison.getBlocDestination()).get(cote);
            if (origines.size() < 2 || origines.size() != destinations.size() ||
                cotesTraites.contains(origines) || cotesTraites.contains(destinations)) continue;
            boolean memeOrdre = true;
            for (int i = 0; i < origines.size(); i++)
                if (origines.get(i).liaison != destinations.get(i).liaison) memeOrdre = false;
            if (!memeOrdre) continue;
            for (int i = 0; i < destinations.size(); i++)
                destinations.get(i).position((destinations.size() - i) / (destinations.size() + 1.0));
            cotesTraites.add(origines);
            cotesTraites.add(destinations);
        }

        // Aligner les extrémités opposées quand les deux côtés ne servent qu'à cette paire.
        for (LiaisonVue liaison : lstLiaisons)
        {
            int coteOrig = liaison.getCoteOrigine(), coteDest = liaison.getCoteDestination();
            if (liaison.getBlocOrigine() == liaison.getBlocDestination() || (coteOrig + 2) % 4 != coteDest)
                continue;
            List<AncrageOptimise> origines = parBloc.get(liaison.getBlocOrigine()).get(coteOrig);
            List<AncrageOptimise> destinations = parBloc.get(liaison.getBlocDestination()).get(coteDest);
            if (origines.size() != destinations.size()) continue;
            boolean memeOrdre = true;
            for (int i = 0; i < origines.size(); i++)
                if (origines.get(i).liaison != destinations.get(i).liaison) memeOrdre = false;
            if (!memeOrdre) continue;

            BlocClasse a = liaison.getBlocOrigine(), b = liaison.getBlocDestination();
            boolean vertical = coteOrig == 1 || coteOrig == 3;
            int debutA = vertical ? a.getY() : a.getX();
            int debutB = vertical ? b.getY() : b.getX();
            int tailleA = vertical ? a.getHauteurCalculee() : a.getLargeur();
            int tailleB = vertical ? b.getHauteurCalculee() : b.getLargeur();
            int debut = Math.max(debutA, debutB) + 15;
            int fin = Math.min(debutA + tailleA, debutB + tailleB) - 15;
            if (fin <= debut) continue;
            for (int i = 0; i < origines.size(); i++)
            {
                double coordonnee = origines.size() == 1 ? (debut + fin) / 2.0
                    : debut + (fin - debut) * i / (origines.size() - 1.0);
                origines.get(i).position((coordonnee - debutA) / tailleA);
                destinations.get(i).position((coordonnee - debutB) / tailleB);
            }
        }
    }


    private void optimiserAncragesPourLiaison(LiaisonVue liaison)
    {
        BlocClasse orig = liaison.getBlocOrigine    ();
        BlocClasse dest = liaison.getBlocDestination();

        if (orig == null || dest == null)
            return;

        // Calculer les positions relatives
        int origX = orig.getX() + orig.getLargeur        () / 2;
        int origY = orig.getY() + orig.getHauteurCalculee() / 2;
        int destX = dest.getX() + dest.getLargeur        () / 2;
        int destY = dest.getY() + dest.getHauteurCalculee() / 2;

        // Déterminer le meilleur côté pour l'origine
        int sideOrigine = determinerMeilleurCote(origX, origY, destX, destY);
        liaison.setSideOrigine(sideOrigine);

        // Déterminer le meilleur côté pour la destination (généralement opposé)
        int sideDestination = determinerMeilleurCoteDestination(origX, origY, destX, destY);
        liaison.setSideDestination(sideDestination);
    }

    /**
     * Détermine le meilleur côté de sortie pour l'origine
     * 0=DROITE, 1=BAS, 2=GAUCHE, 3=HAUT
     */
    private int determinerMeilleurCote(int origX, int origY, int destX, int destY)
    {
        int dx = destX - origX;
        int dy = destY - origY;

        // Déterminer la direction principale
        if (Math.abs(dx) > Math.abs(dy))
        {
            // Direction horizontale
            return dx > 0 ? 0 : 2; // DROITE ou GAUCHE
        }
        else
        {
            // Direction verticale
            return dy > 0 ? 1 : 3; // BAS ou HAUT
        }
    }

    private int determinerMeilleurCoteDestination(int origX, int origY, int destX, int destY)
    {
        int dx = destX - origX;
        int dy = destY - origY;

        // Déterminer la direction principale et prendre le côté opposé
        if (Math.abs(dx) > Math.abs(dy))
        {
            // Direction horizontale
            return dx > 0 ? 2 : 0; // GAUCHE ou DROITE (opposé du départ)
        }
        else
        {
            // Direction verticale
            return dy > 0 ? 3 : 1; // HAUT ou BAS (opposé du départ)
        }
    }

    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Appliquer le pan offset
        g2d.translate(panOffsetX, panOffsetY);
        
        // Appliquer le zoom
        g2d.translate(getWidth() / 2, getHeight() / 2);
        g2d.scale(zoomLevel, zoomLevel);
        g2d.translate(-getWidth() / (2 * zoomLevel), -getHeight() / (2 * zoomLevel));

        // Mettre à jour la liste de toutes les lstLiaisons pour détecter les intersections
        for (LiaisonVue liaison : lstLiaisons)
        {
            liaison.setToutesLesLiaisons(lstLiaisons);
            liaison.setRouteur(routeurLiaisons);
        }
        
        // Dessiner les lstLiaisons
        dessinerLiaisons(g2d);

        // Dessiner les blocs
        for (BlocClasse bloc : lstBlocsClasses)
            bloc.dessiner(g2d, this.afficherAttributs, this.afficherMethodes);
        // Afficher le pourcentage de zoom
    }

    private void afficherZoomPercentage(Graphics2D g2d)
    {
        if (!afficherTextZoom)
            return;

        // Réinitialiser la transformation pour afficher le texte sans zoom
        g2d.setTransform(new java.awt.geom.AffineTransform());

        g2d.setColor(new Color(100, 100, 100));
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        FontMetrics fm = g2d.getFontMetrics();
        
        String zoomText = String.format("Zoom: %d%%", (int) (zoomLevel * 100));

        int textWidth  = fm.stringWidth(zoomText);
        int textHeight = fm.getAscent  ()        ;
        
        // Positionner le texte en bas à gauche avec du padding
        int padding = 10                   ;
        int x       = padding              ;
        int y       = getHeight() - padding;
        
        g2d.drawString(zoomText, x, y);
    }

    private void dessinerLiaisons(Graphics2D g2d)
    {
        for (LiaisonVue liaison : lstLiaisons)
            liaison.dessiner(g2d);
    }

    public double           getZoomLevel          () { return zoomLevel          ; }
    public boolean          isAfficherTextZoom    () { return afficherTextZoom   ; }

    public void modifieMultiplicite(String multiModifie, boolean estOrigine, UUID idLiaison)
    {
        for (LiaisonVue liaisonVue : this.lstLiaisons) 
        {
            if(liaisonVue.getId().equals(idLiaison))
            {
                if(estOrigine)
                    liaisonVue.setMultOrig(multiModifie);
                else
                    liaisonVue.setMultDest(multiModifie);
            }
        }

    }

	public void modifierRole(UUID id, boolean estOrigine, String role) 
	{
		for (LiaisonVue lv : lstLiaisons) 
		{
			if (lv.getId().equals(id))
			{
				if(estOrigine)
                {
					lv.setRoleOrig(role);
                }
				else
                {
					lv.setRoleDest(role);
                }
				return;
			}
		}
	}

    /**
     * Force un rafraîchissement complet du diagramme.
     * À appeler après toute modification du modèle.
     */
    public void rafraichirDiagramme()
    {
        revalidate();
        repaint();
        requestFocusInWindow();
    }

    public UUID             getIdLiaison          () { return idLiaison          ; }
    public List<LiaisonVue> getLstLiaisons        () { return lstLiaisons        ; }
    public boolean          isOrigineLiaison      () { return estOrigineLiaison  ; }
    public List<BlocClasse> getBlocsClasses       () { return lstBlocsClasses    ; }
    public BlocClasse       getBlocClique         () { return blocClique         ; }
    public String           getCheminProjetCourant() { return cheminProjetCourant; }

    public void setAfficherMethodes(boolean b)
    {
        this.afficherMethodes = b;
        this.repaint();
    }

    public void setAfficherAttributs(boolean b)
    {
        this.afficherAttributs = b;
        this.repaint();
    }

    public void setZoomLevel(double zoom)
    {
        this.zoomLevel = zoom;
        this.repaint();
    }

    public void setAfficherTextZoom(boolean afficher)
    {
        this.afficherTextZoom = afficher;
        this.repaint();
    }

    public void actionSauvegarder()
    {
        // Ne sauvegarder que si un projet a été chargé
        if (cheminProjetCourant != null && !cheminProjetCourant.isEmpty())
            this.fenetrePrincipale.sauvegarderClasses(this.lstBlocsClasses, this.lstLiaisons, cheminProjetCourant);
    }

    public void setSauvegardeAuto(boolean b) { this.sauvegardeAuto = b; }

    // Non utilisé
    public void actionEffectuee()
    {
        //System.out.println("Action effectuée.");

        if (this.sauvegardeAuto)
        {
            this.actionSauvegarder();
            //System.out.println("Sauvegarde auto effectuée !");
        }
    }

    public void viderDiagramme() 
    {
        this.lstBlocsClasses.clear();
        this.lstLiaisons.clear();
        this.repaint();
    }
}
