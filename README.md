### Compilation et exécution

#### Windows
./run.bat

#### Linux
./run.sh

### Compiler uniquement
javac -d class @compile.list

### Exécuter uniquement
java -cp class controleur.Controleur

# Présentation

- Ce projet est un Outil de rétroconception, il permet de génerer un Diagramme de classes   UML à partir d'un répertoire de classes Java.

- Attention : Lors du chargement d'un projet, les packages ne sont pas pris en compte, toutes les classes doivent être placées à la racine du répertoire selectionné.

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

# Manuel développeur

## Structure

Le projet est organisé en modèle MVC : Métier-Vue-Controleur.
En MVC, le métier gère l'écriture, sauvegarde, le traitement de données. La vue gère l'affichage et l'interface Homme-Machine. Le controleur gère la liaison entre la vue et le métier.

## Fonctionnement

### Chargement d'un projet depuis une liste de .java

Lorsque l'utilisateur charge une première fois un projet, le programme va récuperer le chemin absolu de celui ci et le au métier (en passant par le controlleur), plus précisément "Lecture".
Lecture et ses classes associées (AnalyseurFicher, GenerateurAssociation, ParseurJava et UtilitaireType) vont instancier différentes listes de Classes, Interfaces, Heritages et Associations.

La lecture se fait via un scanner qui analyse ligne par ligne les classes Java pour en extraire des objets tels que :
- **Classe**   : A un `nom`, un  `type` (*class*/*abstract*). Elle stocke si elle a un héritage avec `isHeritage`, une interface dans `nomInterface`. Elle stocke une liste d'`attributs` et de `méthodes`.
- **Attribut** : A un `nom`, un `type` , une `visibilité` (*private*/*public*/*protected*/*package*), une `portee` (*instance*/*classe). et `isConstant`
- **Methode**  : A un `nom`, une type de `retour`, une `visibilite` (*private*/*public*/*protected*/*package*), un booléen `isAbstract`, et une liste de **Parametre** (un paramètre a un `nom` et un `type`).

- **Liaison** : A une classe d'origine (`classeOrig`) et de destination (`classeDest`).
Différents types de "liaisons" entre 2 classes :
    - **Interface**   : Hérite de **Liaison**.
    - **Heritage**    : Hérite de **Liaison**.

    Si le type d'un attribut est le nom d'une autre Classe il est supprimé et changé en Association : 
    - **Association** : Hérite de **Liaison**, stocke un booléen si il est `unidirectionnel`, une multiplicité d'origine (`multiOrig`) et de destination (`multiDest`).

- **Multiplicite** : Contient deux int `debut` et `fin`. Exemple : (0..1) et si (0..*), alors "fin" est égal a Integer.MAX_INT


Une fois toute la lecture terminée, les informations sont transférées via le Controleur à la vue (**FenetrePrincipale**), convertit les **Classe** en **BlocClasse** et les **Liaison** en **LiaisonVue** :

- **BlocClasse** : A tout les attribut de **Classe**, mais aussi un booléen `estInterface`, les int `x` `y` de sa positions et `largeur` et `hauteur`.
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

\
\
*Projet académique – IUT du Havre*

SAE 3.01 – Outil de rétroconception Java-UML

Romain BARUCHELLO, Jules BOUQUET, Pierre COIGNARD, Paul NOEL, Thibault PADOIS, Hugo VARAO GOMES DA SILVA