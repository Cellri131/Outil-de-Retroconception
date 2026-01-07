# Présentation

- Ce projet est un Outil de rétroconception, il permet de génerer un Diagramme de classes   UML à partir d'un répertoire de classes Java.

- Attention : Lors du chargement d'un projet, les packages ne sont pas pris en compte, toutes les classes doivent être placées à la racine du répertoire selectionné.

# Besoin 
Ce projet a pour objectif de faciliter la compréhension et la prise en main rapide du code, en particulier lors de la reprise du projet par une personne qui n’en connaît pas le fonctionnement initial.
Il peut notamment être utilisé par des enseignants afin d’illustrer la transformation d’un projet Java en diagrammes UML, permettant ainsi une meilleure visualisation de la structure et de l’architecture du code.ni

# Utilisation

- Une fois le projet compilé et executé, une interface graphique s'ouvrira.

> Pour ajouter un projet, voir la barre du haut : Fichier -> Ouvrir un projet... et enfin ouvrir le répertoire contenant le projet Java votre choix.

> La barre latérale gauche affiche l'entièreté des projets sauvegardés. Cliquer sur un projet l'affichera sur le panneau principal, chargeant le diagramme.

### Sur le panneau principal, on peut :

- Déplacer les classes (clic gauche maintenu)
- Zoomer (ctrl + molette), et de se déplacer librement (clic droit maintenu)
- Ajouter des rôles aux associations (double clic gauche sur une classe -> Ajouter rôle)
- Modifier les rôles affichés (double clic gauche sur une classe -> Modifier rôle)
- Modifier les multiplicités affichées (double clic gauche sur une classe -> Modifier multiplicité)

### Fonctionnalités accessibles depuis la barre supérieure

- Ouvrir un nouveau projet Java  afficher dans la barre des projets (Fichier -> Ouvrir un projet...)
- Exporter en image ce qui est affiché sur le panneau de Diagramme (Fichier -> Exporter en image)
- Sauvegarder l'affichage actuel du schéma présent sur le panneau diagamme (Fichier -> Sauvegarder)
- Fermer l'interface (Fichier -> Quitter)
- Afficher ou non les attributs des classes (Affichage -> Afficher attributs)
- Afficher ou non les méthodes des classes (Affichage -> Afficher méthodes)
- Optimiser les positions des blocs et liaison (Affichage -> Optimiser les positions)
- Optimiser les liaisons seulement (Affichage -> Optimiser les liaisons uniquement)
- Avoir les détails des auteurs (Aide -> A propos)

La liste des projets sauvegardés et leur position est sauvegardée dans le fichier data/projets.xml sous la forme : «chemin absolu» (tabulation) «chemin avec le ficher de sauvegarde du projet en question»
Les diagrammes sont sauvegardés dans data/sauvegarde/ avec toutes les informations relatives aux classes et leurs liaisons.
Les données des diagrammes peuvent être changées directement sur les fichiers .xml dans data, via un éditeur de texte.

### Fonctionnalités de la section de projets
- Choisir le projet a afficher dans le panneau diagramme (clique gauche sur le projet voulu)
- Renommer un projet (clique droit sur le projet voulu -> Renommer projet)
- Supprimer un projet (clique droit sur le projet voulu -> Supprimer projet)

Un projet présent dans `projets.xml` mais inexistant sur l'ordinateur est ignoré dans la liste des projet lors du lancement.

## Structure

Le projet est organisé en modèle MVC : Métier-Vue-Controleur.
En MVC, le métier gère l'écriture, sauvegarde, le traitement de données. La vue gère l'affichage et l'interface Homme-Machine. Le controleur gère la liaison entre la vue et le métier.

C:.
│   compile.list
│   Documentation.md
│   Executer.md
│   run.bat
│   run.sh
│
├───class
│
├───data
│   ├───donnees
│   │       projets.xml
│   │
│   └───sauvegardes
├───src
│   ├───controleur
│   │       Controleur.java
│   │
│   ├───metier
│   │   ├───lecture
│   │   │       AnalyseurFichier.java
│   │   │       GenerateurAssociation.java
│   │   │       Lecture.java
│   │   │       ParseurJava.java
│   │   │       UtilitaireType.java
│   │   │
│   │   ├───objet
│   │   │       Association.java
│   │   │       Attribut.java
│   │   │       Classe.java
│   │   │       Heritage.java
│   │   │       Interface.java
│   │   │       Liaison.java
│   │   │       Methode.java
│   │   │       Multiplicite.java
│   │   │       Parametre.java
│   │   │
│   │   ├───sauvegarde
│   │   │       GestionSauvegarde.java
│   │   │
│   │   └───util
│   │       │   ConstantesChemins.java
│   │       │
│   │       └───test_structure_projet
│   │               ElementStructureProjet.java
│   │               TypeElement.java
│   │               VerificationStructureProjet.java
│   │
│   ├───res
│   │       uml_icon.png
│   │
│   └───vue
│       │   BarreMenus.java
│       │   BlocClasse.java
│       │   FenetrePrincipale.java
│       │   PanneauDiagramme.java
│       │   PanneauProjets.java
│       │
│       ├───liaison
│       │       CalculateurChemin.java
│       │       DetecteurObstacles.java
│       │       GestionnaireAncrage.java
│       │       GestionnaireIntersections.java
│       │       LiaisonVue.java
│       │       RenduLiaison.java
│       │
│       └───role_classe
│               FenetreChangementMultiplicite.java
│               FenetreModifRole.java
│               PanneauModif.java
│               PanneauModifRole.java
│
└───test_util
    └───animalerie
            Animal.java
            Chat.java
            Chien.java
            Collier.java
            IAccessoire.java
            je_suis_un_txt.txt

## Fonctionnement

### Chargement d'un projet depuis une liste de .java

Lorsque l'utilisateur charge une première fois un projet, le programme va récuperer le chemin absolu de celui ci et le au métier (en passant par le controlleur), plus précisément "Lecture".
Lecture et ses classes associées (AnalyseurFicher, GenerateurAssociation, ParseurJava et UtilitaireType) vont instancier différentes listes de Classes, Interfaces, Heritages et Associations.

La lecture se fait via un scanner qui analyse ligne par ligne les classes Java pour en extraire des objets tels que :
- **Classe**   : A un `nom`, un  `type` (*class*/*abstract*). Elle stocke si elle a un héritage avec `isHeritage`, une interface dans `nomInterface`. Elle stocke une liste d'`attributs` et de `méthodes`.
- **Attribut** : A un `nom`, un `type` , une `visibilité` (*private*/*public*/*protected*/*package*), une `portee` (*instance*/*classe). et `isConstant`
- **Methode**  : A un `nom`, une type de `retour`, une `visibilite` (*private*/*public*/*protected*/*package*), un booléen `isAbstract`, et une liste de **Parametre** (un paramètre à un `nom` et un `type`).

- **Liaison** : A une classe d'origine (`classeOrig`) et de destination (`classeDest`).
Différents types de "liaisons" entre 2 classes :
    - **Interface**   : Hérite de **Liaison**.
    - **Heritage**    : Hérite de **Liaison**.

    Si le type d'un attribut est le nom d'une autre Classe il est supprimé et changé en Association : 
    - **Association** : Hérite de **Liaison**, stocke un booléen si il est `unidirectionnel`, une multiplicité d'origine (`multiOrig`) et de destination (`multiDest`).

- **Multiplicite** : Contient deux int `debut` et `fin`. Exemple : (0..1) et si (0..*), alors "fin" est égal a Integer.MAX_INT


Une fois toute la lecture terminée, les informations sont transférées via le Controleur à la vue (**FenetrePrincipale**), convertit les **Classe** en **BlocClasse** et les **Liaison** en **LiaisonVue** :

- **BlocClasse** : A tout les attribut de **Classe**, mais aussi un booléen `estInterface`, les int `x` `y` de sa position et `largeur` et `hauteur`.
- **LiaisonVue** : A tout les atrtibuts de **Association** (et donc de tout les autres types de liaison), mais aussi deux **Point**s `ancrageOrigine` et `ancrageDestination`.

Ces instances de classes sont ensuite envoyées à la vue par le controleur, et sont affichées sur le Diagramme de classe par la vue.

### Sauvegarde d'un projet

Lorsque l'utilisateur sauvegarde son diagramme, son contenu sera enregistré dans `data/sauvegardes/«nom du projet».xml`. Voir la classe `GestionSauvegarde`, dans les méthodes `sauvegarderClasses`, `sauvegarderCoordProjet`, `sauvegarderLiaison`.

> Il est important de notifier que le format de fichier XML ne correspond pas au format du contenu, et que ces fichiers auraient du être signés en .data à la place.
> 
**Le contenu du diagramme sera sauvegardé sous format texte dans cette forme :** 

*\<lien du fichier\>*

*\---- Classes \----*

`nomBlocClasse`	`abcisse`    `ordonnee`	`largeur`	`hauteur`	`estInterface`
>Les données de classes sont traitées lors de la sauvegarde/chargement. Il peut y avoir autant de classes que possible dans un diagramme. Une classe peut contenir une liste de classes et méthodes.

`-/+/~` `nomAttribut` : `type` `{frozen/addOnly/requête}`

`-/+/~` `nomMethode`(`nomParam` : `typeParam`, ...) : `type` 

>Les attributs et méthodes sont traités comme des chaînes de caractère. Il peut y avoir autant d'attributs et méthodes dans une classe que possible.

*\---- Liaisons \----*

`typeLiaison`	`id`	`blocOrig`	`coteOrig`	`posRelOrig`	`blocDest`	`coteDest`	`posRelDest`	`roleOrig`	`roleDest`	`multiOrig`	`multiDest`

>Les données de liaisons sont traitées lors de la sauvegarde/chargement. Il peut y avoir autant de liaisons que possible dans un diagramme. Une classe peut contenir une liste de classes et méthodes.
>`typeLiaison` inclut `heritage`/`association_uni`/`association_bi`/`interface`
>`blocOrig` et `blocDest` correspondent aux noms des classes d'origine et de destination de la liaison.

---


Toutes les informations doivent être séparées par des tabulations, dessous des balise "\---- Classes \----" ou "\---- Liaisons \----".
Les lignes commencant par ``#`` ne sont pas prises en compte par le lecteur.


### Chargement d'un projet en format XML

Lorsque l'utilisateur sélectionne un projet dans la barre de gauche, si le projet est déjà existant dans `data/sauvegardes/«nom du projet».xml`, le chargement se fera depuis ce XML plutot que sur le projet de classes .java.
La lecture par l'application se basera sur les mêmes critère de que la sauvegarde (ci-dessus). Voir la classe `GestionSauvegarde`, dans la méthode `sauvegardeProjetXml`.
Ainsi, les classes seront chargées d'après le principe du premier chargement (**BlocClasse**, **LiaisonVue**...).

# Description détaillée des classes

## Métier

### Objet

#### Association

Cette classe permet de creer des objets d'Associations.
Herite de Liaison.

Attributs : nbAssoc(identifiant unique), multiOrig, multiDest, num

Méthodes : Getters, Setters, isUnidirectionnel(savoir si la classe est Unidirectionnel) et un toString.

#### Attribut

Cette classe permet de creer des objets d'Attributs. 
Elle contient des caractéristiques propres à un attribut.

Attributs : nom, type, visibilite, portee (instance ou classe)
Getters et Setters, ainsi qu'un toString propre.

#### Methode

Cette classe permet de creer des objets de méthodes.
Elle contient des caractéristiques propres à une méthode.

Attributs : nom, retour(type de retour), visibilite, isAbstract, lstParametre.

Méthodes : Getters, Setters, un toString et une méthode pour ajouter un paramètre à la liste

#### Parametre

Cette classe permet de creer des objets de parametres.
Elle contient des caractéristiques propres à un parametre.

Attributs : nom, type

Méthodes : Getters, Setters et un toString

#### Classe

Cette classe permet de creer des objets de Classe. Elle contient des caractéristiques propres à une classe.

Attributs : nom, classeParente(nom de la classe hérité), type, isHeritage, nomInterface, liste d'attributs et de méthode.

Méthodes : Getters et des méthodes booléennes qui nous serviront dans les fichiers du dossiers lecture pour détecter de quel type de classe il s'agit.

#### Liaison

Cette classe permet de creer un objet Liaison qui représente les relations entre deux classes comme par exemple l'heritage ou les interfaces.

Attributs : classeOrigine(classe source), classeSource(classe cible)

Méthodes : Getters

#### Heritage

Cette classe permet de creer un objet Heritage afin de déterminer quel classe est à l'origine de l'héritage et quel classe herite de la classe d'origine.
Herite de la classe Liaison.

Attributs : herite des attributs de Liaison.

Méthode : toString

#### Interface

Cette classe permet de creer un objet Interface afin de déterminer quel classe est l'interface implémentée et quel classe implémente l'interface.
Herite de la classe Liaison.

Attributs : herite des attributs de Liaison.

Méthode : toString

#### Multiplicite

Cette classe permet de creer des objets de multiplicite, qui représente la multiplicité d'une association ou d'une relation entre deux classes.

Attributs : debuts, fin

Méthodes : Deux constructeurs, setters et un toString.

Constructeur 1 : Verifie que les valeurs des multiplicités sont correctes.

Constructeur 2 : Verifie si la valeur est un '*' auquel cas on lui affecte ce caractere.

<br>

## Lecture

## ParseurJava

Cette classe permet d'analyser un fichier .java, et de creer une classe.
Elle analyse bien sur les attributs, les méthodes etc. C'est ce qui va 
creer la classe.

### parser(Scanner scFic, String nomFichierAvExt)

C'est la methode principale de cette classe qui permet 
d'analyser le fichier, de creer la classe et de la retourner.

> Elle fait appelle a des sous méthodes pour gérer les cas des attributs,
des méthodes, des recordes etc.

### ParseurJava (constructeur)

Le constructeur initialise les attributs d'instance

### GenerateurAssociation

### AnalyseurFichier

### UtilitaireType

<br><br>

## Vue

### FenetrePrincipale

Cette classe représente la **fenêtre principale de l’application UML**.  
Elle structure l’interface graphique en regroupant les panneaux projets et diagramme.  
Elle assure le **rôle de point central entre l’IHM et le contrôleur**.

Attributs : panneauProjets, panneauDiagramme, controleur.

---

#### FenetrePrincipale (constructeur)

Initialise la fenêtre, crée les panneaux principaux, configure le layout et installe la barre de menus.

---

#### ouvrirProjet

Charge un projet UML dans le panneau diagramme, déclenche la sauvegarde initiale via le contrôleur et force le recalcul graphique des liaisons.

---

#### exporterImageDiagramme

Capture le diagramme affiché dans le panneau diagramme et l’exporte au format PNG en gérant temporairement le zoom et l’affichage du texte.

---

#### Méthodes secondaires

- affichageAttributs(...) : appelle `panneauDiagramme.setAfficherAttributs(...)` pour afficher ou masquer les attributs.  
- affichageMethodes(...) : appelle `panneauDiagramme.setAfficherMethodes(...)` pour afficher ou masquer les méthodes visuellement.  
- optimiserPositionsClasses() : appelle `panneauDiagramme.optimiserPositionsClasses()` pour recalculer la position des classes.  
- optimiserPositionsLiaisons() : appelle `panneauDiagramme.optimiserPositionsLiaisons()` pour recalculer la position des liaisons.  
- setSauvegardeAuto(...) : appelle `panneauDiagramme.setSauvegardeAuto(...)` pour activer ou désactiver la sauvegarde automatique.  
- actionSauvegarder() : appelle `panneauDiagramme.actionSauvegarder()` pour sauvegarder l’état courant du diagramme.  
- chargerProjet(...) : délègue l’appel à `controleur.chargerProjet(...)`.  
- sauvegarderClasses(...) : délègue la sauvegarde des classes et liaisons à `controleur.sauvegarderClasses(...)`.  
- viderDiagramme() : appelle `panneauDiagramme.viderDiagramme()` pour réinitialiser l’affichage.

<br><br>

#### PanneauProjets

Cette classe représente le **panneau latéral de gestion des projets** de l’application.  
Elle affiche la liste des projets enregistrés, permet leur sélection et leur gestion (renommer, supprimer).  
Elle agit comme une **vue dédiée**, en interaction directe avec `FenetrePrincipale`.

Attributs : CHEMIN_SAUVEGARDES, fenetrePrincipale, cheminDossiers, panelProjets.

---

#### PanneauProjets (constructeur)

Initialise le panneau, configure l’interface graphique, charge la liste des projets et installe les actions utilisateur.

---

#### actualiser

Vide le panneau des projets puis recharge la liste depuis le fichier de configuration et met à jour l’affichage.

---

#### chargerProjets

Lit le fichier contenant tous les chemin des projet (projets.xml), valide les chemins, récupére les intitulés et crée dynamiquement les boutons correspondant aux projets existants.

---

#### Méthodes secondaires

- creerBoutonProjet(...) : crée un bouton de projet avec menu contextuel et action d’ouverture via `fenetrePrincipale`.  
- renommerProjet(...) : demande un nouvel intitulé puis met à jour le projet dans le fichier et les sauvegardes associées.  
- supprimerProjet(...) : supprime un projet de la liste et efface sa sauvegarde après confirmation utilisateur.  
- intituleExiste(...) : vérifie si un intitulé de projet existe déjà dans le fichier des projets.  
- modifierProjetDansFichier(...) : modifie le fichier projets.xml pour renommer ou supprimer un projet et gère les fichiers de sauvegarde.

<br><br>

### PanneauDiagramme

Cette classe représente le **panneau central du diagramme UML**.  
Elle gère l’affichage des blocs de classes, des liaisons et toutes les interactions utilisateur (clic, drag, zoom, pan).  
Elle constitue le **cœur graphique et interactif** de l’application.

Attributs principaux : lstBlocsClasses, lstLiaisons, fenetrePrincipale, cheminProjetCourant, zoomLevel, panOffsetX/Y.

---

#### PanneauDiagramme (constructeur)

Initialise le panneau, configure le menu contextuel, les paramètres graphiques et installe tous les listeners d’interaction.

---

#### chargerProjet

Charge un projet UML, récupère les blocs et liaisons via `FenetrePrincipale`, initialise leurs dépendances et rafraîchit l’affichage.

---

#### optimiserPositionsClasses

Organise automatiquement les blocs en grille, recalcule les ancrages et ajuste les liaisons pour éviter les chevauchements.

---

#### optimiserPositionsLiaisons

Recalcule les ancrages de toutes les liaisons en utilisant la liste complète des liaisons puis redessine le diagramme.

---

#### Méthodes secondaires

- **ajouterListenersInteraction()** : installe les listeners souris pour clic, drag, zoom, pan et menus contextuels.  
- **organiserEnGrille()** : positionne les blocs de classes dans une grille régulière selon leur nombre et leur taille.  
- **optimiserAncragesPourLiaison(...)** : calcule dynamiquement les côtés d’ancrage optimaux entre deux blocs.  
- **determinerMeilleurCote(...) :** détermine le côté de sortie d’une liaison selon la position relative des blocs.  
- **determinerMeilleurCoteDestination(...) :** détermine le côté d’entrée opposé pour la destination d’une liaison.  
- **paintComponent(...) :** applique le zoom et le pan puis dessine les liaisons et les blocs du diagramme.  
- **dessinerLiaisons(...) :** appelle `liaison.dessiner(...)` pour chaque liaison du diagramme.  
- **afficherZoomPercentage(...) :** affiche le pourcentage de zoom dans l’interface graphique.  
- **modifieMultiplicite(...) :** modifie la multiplicité d’une liaison en fonction de son origine ou destination.  
- **modifierRole(...) :** modifie le rôle d’une liaison (origine ou destination) à partir de son identifiant.  
- **rafraichirDiagramme() :** force la mise à jour complète de l’affichage Swing.  
- **actionSauvegarder() :** délègue la sauvegarde du diagramme à `fenetrePrincipale.sauvegarderClasses(...)`.  
- **actionEffectuee() :** déclenche une sauvegarde automatique si l’option est activée.  
- **viderDiagramme() :** supprime tous les blocs et liaisons puis rafraîchit l’affichage.

<br><br>


### BlocClasse

Cette classe représente **l’affichage graphique d’une classe UML** dans le diagramme.  
Elle gère la représentation visuelle (nom, attributs, méthodes), les modes condensé / plein écran et les interactions géométriques.  
Elle constitue l’**unité visuelle de base** manipulée par le panneau de diagramme.

Attributs principaux : id, nom, attributs, methodes, x, y, largeur, hauteur, affichagePleinEcran.

---

#### BlocClasse (constructeur)

Initialise un bloc UML avec un identifiant unique, une position initiale, des dimensions par défaut et des listes vides d’attributs et de méthodes.

---

#### dessiner

Dessine intégralement le bloc de classe : fond, en-tête, nom, attributs, méthodes, séparateurs et styles selon l’état (interface, sélection, affichage).

---

#### calculerHauteur

Calcule dynamiquement la hauteur réelle du bloc en fonction du contenu affiché, du mode d’affichage et des retours à la ligne.

---

#### **Méthodes secondaires**

- **formatMethodeAvecLargeur(...)** : formate une méthode sur plusieurs lignes selon la largeur disponible et le mode d’affichage.  
- **formatMethode(...)** : limite le nombre de paramètres affichés en mode condensé.  
- **getAttributsAffichage()** : retourne la liste d’attributs à afficher selon le mode plein écran ou condensé.  
- **getMethodesAffichage()** : retourne la liste de méthodes à afficher avec formatage et limitation éventuelle.  
- **contient(int px, int py)** : vérifie si un point donné se situe à l’intérieur du bloc.  
- **chevaucheTexte(...)** : détecte si un rectangle de texte chevauche la zone du bloc.  
- **deplacer(int dx, int dy)** : déplace le bloc en modifiant ses coordonnées X et Y.


<br><br>

### BarreMenus

Cette classe représente la **barre de menu principale** de l’application.  
Elle fournit un **accès** aux **outils d’affichage**, d’**édition**, de **gestion des fichiers** et à l’**aide**.  
Hérite de `JMenuBar` pour être directement intégrée dans la fenêtre principale.
****
Attributs principaux : `fenetrePrincipale`, items de menu (`JCheckBoxMenuItem`) pour les options d’affichage et de sauvegarde.

---

#### BarreMenus (constructeur)

Initialise la barre de menu avec :
- Couleur de fond personnalisée,
- Menus principaux (`Fichier`, `Affichage`, `Aide`),
- Style graphique (couleurs, police, opacité),
- Liaison des actions aux événements des items de menu.

---

#### **Méthodes secondaires**

- **creerMenuFichier()** : crée le menu "Fichier" avec les options `Ouvrir projet`, `Exporter en image`, `Sauvegarder`, `Quitter`.  
- **creerMenuAffichage()** : crée le menu "Affichage" avec options `Afficher attributs`, `Afficher méthodes`, `Optimiser positions`.  
- **creerMenuAide()** : crée le menu "Aide" avec l’item `À propos`.  

- **actionOuvrirProjet()** : ouvre un dialogue pour sélectionner un projet et initie sa vérification et son chargement.  
- **verifierFichiersProjet(String cheminFichier)** : affiche un message d’avertissement si des fichiers invalides sont détectés.  
- **sauvegardeProjetXml(String cheminFichier)** : transmet le chemin au contrôleur pour charger le projet.  
- **actionAffichageAttributs()** : met à jour l’affichage des attributs dans la fenêtre principale.  
- **actionAffichageMethodes()** : met à jour l’affichage des méthodes dans la fenêtre principale.  
- **actionSauvegardeAuto()** : active ou désactive la sauvegarde automatique.  
- **actionOptimiser()** : optimise la position des classes et des liaisons dans le diagramme.  
- **actionOptimiserLiaisons()** : optimise uniquement la position des liaisons.  
- **actionSauvegarder()** : déclenche la sauvegarde manuelle du projet.  
- **actionAPropos()** : affiche une boîte de dialogue HTML avec les auteurs et informations sur le projet.


## package vue.role_classe

### FenetreChangementMultiplicite

Hérite de `JFrame`. Fenêtre pour modifier les multiplicités d’un diagramme UML.


### PanneauModif

Cette classe représente le **panneau de modification des multiplicités** d’une classe dans le diagramme UML.  
Elle permet à l’utilisateur de sélectionner une liaison, saisir les multiplicité minimum et maximum, puis de valider ou annuler les changements.  
Elle agit comme **interface entre l’IHM et les objets LiaisonVue**, en appliquant les changements directement sur le diagramme.

Attributs principaux : `blocSelectionne`, `panDiag`, `listeLiaisonsIHM`, `txtMultipliciteMin`, `txtMultipliciteMax`, `btnValider`, `btnAnnuler`.

---

#### PanneauModif (constructeur)

Initialise le panneau avec tous les composants Swing : labels, champs texte, boutons et panels.  
Récupère la classe sélectionnée et construit la liste des classes reliées (`listeClasseDest`).  
Configure le layout (GridLayout), installe les listeners pour la liste et les boutons, puis rend le panneau visible.

---

#### initListe

Parcourt toutes les liaisons du diagramme et remplit `listeClasseDest` avec les noms des classes reliées par des **associations**.  
Ne garde que les classes connectées à la classe sélectionnée.

---

#### valueChanged

Met à jour le titre du panneau pour indiquer la liaison sélectionnée dans la liste.  
Mémorise le nom sélectionné afin de retrouver l’objet `BlocClasse` correspondant.

---

#### estMultipliciteValide

Vérifie la validité des valeurs de multiplicité min et max (min ≤ max, max entier ou `*`).  
Renvoie `true` si les valeurs sont correctes et imprimé des erreurs sinon.

---

#### getBlocClasseSelectionne

Retourne l’objet `BlocClasse` correspondant au nom sélectionné dans la liste.  
Permet de récupérer la classe cible pour appliquer les modifications sur les liaisons.

---

#### valider

Applique la nouvelle multiplicité sur la liaison sélectionnée (origine ou destination).  
Met à jour les objets `LiaisonVue` associés et déclenche le rafraîchissement du diagramme.  
Gère le cas où la multiplicité est invalide en affichant une erreur.

---

#### Méthodes secondaires

- **caractereValideMultMin(String min)** : vérifie si min est un entier.  
- **caractereValideMultMax(String max)** : vérifie si max est un entier.  
- **actionPerformed(ActionEvent e)** : gère les clics sur les boutons valider et annuler.  
- **getLiaisonConnectees(BlocClasse blcClasse)** : renvoie la liste des liaisons connectées à la classe donnée.

### FenetreModifRole

Cette classe représente la **fenêtre de modification des rôles** dans le diagramme UML.  
Elle encapsule le panneau `PanneauModifRole` et sert de conteneur Swing pour permettre à l’utilisateur de modifier les rôles d’une liaison.  
Elle constitue la **fenêtre modale pour l’édition des rôles**, centrée sur l’écran et de taille fixe.

Attributs principaux : `panDiag`, `PanneauModifRole`.

---

#### FenetreModifRole (constructeur)

Initialise la fenêtre avec un titre, une taille fixe et une position centrée à l’écran.  
Crée le panneau `PanneauModifRole` et l’ajoute au centre de la fenêtre via `BorderLayout`.

<br><br>

### PanneauModifRole

Ce panel permet de **modifier le rôle d’une liaison** pour un bloc UML sélectionné.  
Il affiche les associations et interfaces liées au bloc avec des **radio-boutons séparés par type**, et propose un champ pour saisir ou modifier le nom du rôle.  
Il constitue l’**interface principale de modification de rôle** avant validation par l’utilisateur.

Attributs principaux : `panDiag`, `nomBlocSelectionne`, `liaisonSelectionnee`, `txtNomRole`, `groupeRadio`.

---

#### PanneauModifRole (constructeur)

Initialise le panel, configure le titre et le nom du bloc sélectionné, crée le champ de texte pour le rôle, installe les boutons Valider/Annuler et le panel scrollable contenant la liste des liaisons.  
Remplit la liste des liaisons avec séparation **Associations / Interfaces** et installe les listeners pour les boutons et radio-boutons.

---

#### remplirListeLiaisons

Parcourt toutes les liaisons du diagramme et ajoute dans le panel uniquement celles **liées au bloc sélectionné**, séparées en **Associations** et **Interfaces**.  
Chaque liaison est représentée par un **radio-bouton** qui remplit le champ du rôle lorsqu’il est sélectionné.

---

#### ajouterLiaisonALaListe

Crée une ligne avec un radio-bouton pour une liaison donnée.  
Si le bloc sélectionné est l’origine, affiche le rôle origine ; si le bloc sélectionné est la destination, affiche le rôle destination.  
Ajoute le radio-bouton à un `ButtonGroup` pour assurer une sélection unique.

---

#### actionPerformed

Gère les actions des boutons Valider et Annuler :  
- **Valider** : récupère le rôle saisi et modifie la liaison correspondante via `panDiag.modifierRole(...)`, puis rafraîchit le diagramme et ferme la fenêtre.  
- **Annuler** : ferme simplement la fenêtre sans modifier la liaison.

---

#### Méthodes secondaires

- **getLiaisonSelectionnee()** : retourne la liaison actuellement sélectionnée.  
- **rafraichirPanel()** : force la mise à jour graphique du panel et de ses composants Swing.



*Projet académique – IUT du Havre*

SAE 3.01 – Outil de rétroconception Java-UML

Romain BARUCHELLO, Jules BOUQUET, Pierre COIGNARD, Paul NOEL, Thibault PADOIS, Hugo VARAO GOMES DA SILVA