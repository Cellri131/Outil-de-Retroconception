ceci est une aide de compréhension du fonctionnement du dossier liaison
1. DÉCLENCHEMENT
   └─> Controleur charge projet
       └─> Parse fichiers Java
           └─> Crée objets métier (Association, Heritage...)

2. CRÉATION LIAISONS
   └─> new LiaisonVue(origine, destination, type)
       └─> Initialise helpers
           └─> chooseBestSides() ◄── CŒUR DE L'ALGORITHME

3. CALCUL OPTIMAL
   └─> Teste TOUTES les combinaisons côtés + positions
       ├─> Calcule chemin orthogonal pour chaque combo
       ├─> Vérifie obstacles
       └─> Sélectionne meilleur chemin (moins segments + court)

4. GÉNÉRATION CHEMIN
   └─> CalculateurChemin.creerCheminOrthogonal()
       └─> Crée List<Point> avec angles droits
           └─> Évite coins et obstacles

5. AFFICHAGE
   └─> PanneauDiagramme.paintComponent()
       └─> liaison.dessiner(g2d)
           ├─> Recalcule ancrages si nécessaire
           ├─> Génère chemin actuel
           ├─> Détecte intersections
           ├─> Dessine segments avec ponts
           ├─> Dessine flèche appropriée
           └─> Affiche multiplicités/rôles

6. MISE À JOUR (si déplacement)
   └─> Bloc déplacé → repaint()
       └─> Retour à l'étape 5 (recalcul automatique)

1.
**Création d'une LiaisonVue:**
```java
LiaisonVue liaison = new LiaisonVue(
    blocOrigine,        // BlocClasse source
    blocDestination,    // BlocClasse cible
    "association",      // Type: association/heritage/interface
    unidirectionnel,    // true/false
    multOrig,          // Multiplicité origine (ex: "1", "0..*")
    multDest           // Multiplicité destination
);

2.
┌──────────────────────────────────────────────────────┐
│ new LiaisonVue(origine, destination, type, ...)      │
│   │                                                  │
│   ├─> initializeHelpers()                            │
│   │     ├─> DetecteurObstacles     (détecte blocs)   │
│   │     ├─> CalculateurChemin      (calcul chemins)  │
│   │     ├─> GestionnaireIntersections (gère ponts)   │
│   │     └─> RenduLiaison           (dessine liaison) │
│   │                                                  │
│   └─> chooseBestSides() ◄─────── ALGORITHME CLÉ      │
└──────────────────────────────────────────────────────┘
```
3.
┌─────────────────────────────────────────────────────────────┐
│ chooseBestSides() - Trouve les meilleurs points d'ancrage   │
│                                                             │
│ ÉTAPE 1: Regrouper les liaisons similaires                  │
│   ├─> Chercher toutes les liaisons avec même origine/dest   │
│   └─> Optimiser ensemble pour éviter superposition          │
│                                                             │
│ ÉTAPE 2: Tester toutes les combinaisons possibles           │
│   ├─> Pour chaque côté origine (HAUT/DROITE/BAS/GAUCHE)     │
│   │   └─> Pour chaque côté destination                      │
│   │       └─> Pour chaque position (0.0 à 1.0)              │
│   │           │                                             │
│   │           ├─> GestionnaireAncrage.getPointSurCote()     │
│   │           │     └─> Calcule point exact sur le côté     │
│   │           │                                             │
│   │           ├─> CalculateurChemin.creerCheminOrthogonal() │
│   │           │     └─> Génère chemin avec segments         │
│   │           │                                             │
│   │           ├─> Vérifier collisions (obstacles)           │
│   │           │     └─> DetecteurObstacles.aUnObstacle()    │
│   │           │                                             │
│   │           └─> Compter segments + calculer distance      │
│   │                                                         │
│   └─> Sélectionner le chemin avec:                          │
│       • Moins de segments (préférence ligne droite)         │
│       • Plus court en distance                              │
│       • Paires de côtés "naturelles" (bonus)                │
│                                                             │
│ ÉTAPE 3: Appliquer le meilleur chemin                       │
│   └─> Définir: sideOrigine, sideDestination,                │
│                posRelOrigine, posRelDestination             │
└─────────────────────────────────────────────────────────────┘

système des côtés :
        0 (HAUT)
        ↑
    3 ←─┼─→ 1  (GAUCHE / DROITE)
        ↓
        2 (BAS)

4.
┌───────────────────────────────────────────────────────────┐
│ Génère un chemin orthogonal (angles droits uniquement)    │
│                                                           │
│ INPUT:                                                    │
│   • Point début (ancrage origine)                         │
│   • Point fin (ancrage destination)                       │
│   • Côté début (0-3)                                      │
│   • Côté fin (0-3)                                        │
│                                                           │
│ ALGORITHME:                                               │
│   1. Ajouter point de début                               │
│                                                           │
│   2. Calculer point de sortie (décalé de 30px du bloc)    │
│      └─> calculerPointDecale(debut, coteDebut, 30)        │
│                                                           │
│   3. Calculer point d'entrée (décalé de 30px du bloc)     │
│      └─> calculerPointDecale(fin, coteFin, 30)            │
│                                                           │
│   4. Déterminer type de liaison:                          │
│      ├─> Côtés opposés alignés? → Ligne droite            │
│      ├─> Côtés opposés non alignés? → 1 point inter.      │
│      ├─> Même orientation? → 2 points inter.              │
│      └─> Orientation perpendiculaire? → 1 point inter.    │
│                                                           │
│   5. Éviter les coins (eviterCoin)                        │
│      └─> Ajouter petite marge pour ne pas toucher bloc    │
│                                                           │
│   6. Ajouter point d'entrée et point de fin               │
│                                                           │
│ OUTPUT: List<Point> représentant le chemin                │
└───────────────────────────────────────────────────────────┘

EXEMPLE DE CHEMIN:
┌─────┐
│  A  │─┐ (point sortie)
└─────┘ │
        │ (segment vertical)
        └──────────┐ (point intermédiaire)
                   │ (segment horizontal)
              ┌────┘ (point entrée)
              │
          ┌───┴──┐
          │  B   │
          └──────┘

5.
┌──────────────────────────────────────────────────────────┐
│ Vérifie que le chemin ne traverse pas d'autres blocs     │
│                                                          │
│ CalculateurChemin.cheminADesCollisions(chemin)           │
│   │                                                      │
│   ├─> Pour chaque segment du chemin:                     │
│   │     │                                                │
│   │     ├─> Si segment horizontal:                       │
│   │     │     └─> DetecteurObstacles                     │
│   │     │           .aObstacleHorizontalStrict()         │
│   │     │           └─> Vérifie intersection avec blocs  │
│   │     │                                                │
│   │     └─> Si segment vertical:                         │
│   │           └─> DetecteurObstacles                     │
│   │                 .aObstacleVerticalStrict()           │
│   │                 └─> Vérifie intersection avec blocs  │
│   │                                                      │
│   └─> Retourne true si collision détectée                │
└──────────────────────────────────────────────────────────┘

6.
┌───────────────────────────────────────────────────────────┐
│ PanneauDiagramme.paintComponent(g)                        │
│   │                                                       │
│   └─> Pour chaque LiaisonVue:                             │
│         liaison.dessiner(g2d, zoom, offset)               │
│           │                                               │
│           ├─> Recalculer les ancrages                     │
│           │     └─> GestionnaireAncrage.getPointSurCote() │
│           │                                               │
│           ├─> Générer le chemin actuel                    │
│           │     └─> CalculateurChemin                     │
│           │           .creerCheminOrthogonal()            │
│           │                                               │
│           ├─> Détecter intersections avec autres liaisons │
│           │     └─> GestionnaireIntersections             │
│           │           .trouverIntersections()             │
│           │                                               │
│           ├─> Dessiner les segments avec ponts            │
│           │     └─> RenduLiaison                          │
│           │           .dessinerLigneAvecPonts()           │
│           │           • Dessine arcs aux intersections    │
│           │           • Crée effet de "pont"              │
│           │                                               │
│           ├─> Dessiner la flèche à destination            │
│           │     ├─> Heritage/Interface:                   │
│           │     │     └─> RenduLiaison                    │
│           │     │           .dessinerFlecheVide()         │
│           │     │                                         │
│           │     └─> Association:                          │
│           │           └─> RenduLiaison                    │
│           │                 .dessinerFlecheAssociation()  │
│           │                                               │
│           └─> Dessiner multiplicités et rôles (si assoc.) │
│                 • Près de l'origine (multOrig, roleOrig)  │
│                 • Près de la destination (multDest, ...)  │
└───────────────────────────────────────────────────────────┘

LiaisonVue
    │
    ├──► DetecteurObstacles
    │      • Identifie les blocs sur le chemin
    │      • Vérifie collisions horizontal/vertical
    │      • Liste obstacles par ligne
    │
    ├──► CalculateurChemin
    │      • Crée chemins orthogonaux
    │      • Calcule longueur chemin
    │      • Vérifie collisions globales
    │      • Utilise DetecteurObstacles
    │
    ├──► GestionnaireIntersections
    │      • Détecte croisements entre liaisons
    │      • Trouve points d'intersection
    │      • Identifie segments partagés
    │
    ├──► GestionnaireAncrage
    │      • Calcule points sur côtés
    │      • Détermine côté le plus proche
    │      • Gère positions relatives [0.0-1.0]
    │      • Détecte clics sur ancrages
    │
    └──► RenduLiaison
           • Dessine lignes avec ponts
           • Dessine flèches (vide/association)
           • Gère intersections visuelles