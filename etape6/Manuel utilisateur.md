# Présentation

- Ce projet est un Outil de rétroconception, il permet de génerer un Diagramme de classes   UML à partir d'un répertoire de classes Java.

- Attention : Lors du chargement d'un projet, les packages ne sont pas pris en compte, toutes les classes doivent être placées à la racine du répertoire selectionné.

# Besoin 
Ce projet a pour objectif de faciliter la compréhension et la prise en main rapide du code, en particulier lors de la reprise du projet par une personne qui n’en connaît pas le fonctionnement initial.
Il peut notamment être utilisé par des enseignants afin d’illustrer la transformation d’un projet Java en diagrammes UML, permettant ainsi une meilleure visualisation de la structure et de l’architecture du code.

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




*Projet académique – IUT du Havre*

SAE 3.01 – Outil de rétroconception Java-UML

Romain BARUCHELLO, Jules BOUQUET, Pierre COIGNARD, Paul NOEL, Thibault PADOIS, Hugo VARAO GOMES DA SILVA
