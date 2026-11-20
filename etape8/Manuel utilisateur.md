# Présentation

- Ce projet est un outil de rétroconception qui génère un diagramme UML à partir de fichiers Java, C#, Python, JavaScript, SQL, PHP et JSON, y compris dans un même projet.

- Les sous-dossiers sont parcourus ; les fichiers d'autres types sont ignorés. Les noms de classes et de tables doivent être uniques dans le projet, même entre langages et sous-dossiers.
- L'analyse Java existante est conservée. Pour les autres langages, l'analyse est simple : elle reconnaît les déclarations usuelles de classes, leurs membres typés, les associations correspondantes et l'héritage simple. En C#, les déclarations de variables de premier niveau sont regroupées dans un bloc par fichier ; il ne s'agit pas de véritables champs globaux C#. Les tables `CREATE TABLE` SQL sont affichées comme des blocs contenant leurs colonnes. Les clés étrangères déclarées dans `CREATE TABLE` ou `ALTER TABLE` relient les tables présentes et indiquent les colonnes, sans supposer de multiplicité. Les syntaxes complexes et les classes imbriquées ne sont pas entièrement prises en charge.
- Pour JSON, chaque objet est représenté par un bloc nommé d'après son fichier et son chemin, y compris les objets dans des tableaux. Les champs affichent leur type sans leur valeur ; les objets imbriqués ne sont pas reliés par des flèches. Les caractères spéciaux dans les noms de champs sont encodés en `%XXXX`.
- Couleur des cadres : Java rouge, C# violet foncé, Python bleu, JavaScript jaune, SQL rose, PHP violet clair et JSON gris. Une sélection épaissit le cadre sans masquer la couleur.
- Un projet déjà sauvegardé est chargé depuis son diagramme enregistré ; les modifications des fichiers source ne sont pas automatiquement réanalysées.

# Besoin 
Ce projet a pour objectif de faciliter la compréhension et la prise en main rapide du code, en particulier lors de la reprise du projet par une personne qui n’en connaît pas le fonctionnement initial.
Il peut notamment être utilisé par des enseignants afin d’illustrer la transformation d’un projet Java en diagrammes UML, permettant ainsi une meilleure visualisation de la structure et de l’architecture du code.

# Utilisation

- Une fois le projet compilé et executé, une interface graphique s'ouvrira.

> Pour ajouter un projet, voir la barre du haut : Fichier -> Ouvrir un projet... puis sélectionner un fichier source ou le répertoire du projet. Le sélecteur s'ouvre dans le dossier `etape8` lors du lancement depuis ce dossier ou depuis la racine du dépôt.

> La barre latérale gauche affiche l'entièreté des projets sauvegardés. Cliquer sur un projet l'affichera sur le panneau principal, chargeant le diagramme.

### Sur le panneau principal, on peut :

- Déplacer les classes (clic gauche maintenu)
- Zoomer (ctrl + molette) et déplacer la planche (clic molette maintenu, espace + clic gauche, ou clic droit maintenu sur le fond).
- Ajouter des rôles aux associations (double clic gauche sur une classe -> Ajouter rôle)
- Modifier les rôles affichés (double clic gauche sur une classe -> Modifier rôle)
- Modifier les multiplicités affichées (double clic gauche sur une classe -> Modifier multiplicité)

### Fonctionnalités accessibles depuis la barre supérieure

- Ouvrir un nouveau projet à afficher dans la barre des projets (Fichier -> Ouvrir un projet...)
- Exporter en image ce qui est affiché sur le panneau de Diagramme (Fichier -> Exporter en image)
- Sauvegarder l'affichage actuel du schéma présent sur le panneau diagamme (Fichier -> Sauvegarder)
- Fermer l'interface (Fichier -> Quitter)
- Afficher ou non les attributs des classes (Affichage -> Afficher attributs)
- Afficher ou non les méthodes des classes (Affichage -> Afficher méthodes)
- Optimiser les positions des blocs et liaison (Affichage -> Optimiser les positions)
- Optimiser les liaisons seulement (Affichage -> Optimiser les liaisons uniquement)
- Ouvrir la vue du fonctionnement interne du projet sélectionné (bouton « Fonctionnement du code »)
- L'optimisation rapproche les blocs liés et répartit les ancrages sur chaque côté. Les trajets tiennent compte des autres liaisons pour limiter leurs croisements et éviter les segments superposés.
- Les diagrammes sauvegardés gardent leurs positions et ancrages au chargement : utilisez « Optimiser les positions » ou « Optimiser les liaisons uniquement » pour appliquer le nouveau placement.
- Si aucun des trajets essayés ne permet d'éviter un bloc ou un segment superposé, la liaison est affichée en rouge : déplacer les blocs ou ses ancrages pour libérer un passage. L'optimisation est heuristique et ne garantit pas la disparition de tous les croisements.
- Avoir les détails des auteurs (Aide -> A propos)

### Vue du fonctionnement du code

Sélectionnez un projet puis cliquez sur « Fonctionnement du code ». Une fenêtre séparée analyse les fichiers Java, C# et Python du projet, y compris dans ses sous-dossiers. Les autres langages ne sont pas analysés dans cette vue. Les fichiers sources sont relus à chaque ouverture, indépendamment du diagramme UML sauvegardé.

- Un point de départ est proposé automatiquement lorsque l'analyse reconnaît notamment une méthode `Awake` ou `main`. Choisissez « Tout le projet » ou un autre point dans la liste si nécessaire. La profondeur limite les relations suivies depuis le point choisi ; double-cliquez sur un bloc pour en faire le nouveau point de départ.
- Cliquez sur un bloc ou une liaison pour afficher ses détails, les lignes sources et les arguments. Les occurrences identiques sont regroupées sur une flèche et détaillées individuellement. La case « Arguments sur les flèches » active leur affichage sur le diagramme.
- Déplacez la vue en maintenant le clic molette, ou utilisez les barres de défilement.
- Les cadres et flèches utilisent les couleurs des langages : Java rouge, C# violet foncé et Python bleu. Un trait plein indique un appel, une construction, une affectation ou un retour ; des pointillés indiquent une référence à une classe, et des tirets gris l'appartenance d'une méthode à une classe. Les éléments externes ou non résolus sont indiqués comme tels.
- Le graphe suit les relations vers les niveaux suivants : les flèches qui remonteraient vers un niveau précédent ou resteraient au même niveau ne sont pas dessinées. Les raccourcis d'appartenance qui sauteraient des niveaux sont également masqués. Ces relations restent consultables dans les détails du bloc source.

Il s'agit d'une analyse statique approximative, et non d'une trace d'exécution : une référence à une classe suivie de ses méthodes ne signifie pas que ces méthodes sont appelées immédiatement. Certaines affectations, appels, syntaxes complexes ou liaisons ambiguës peuvent manquer ou rester non résolus. Les arguments et valeurs sont les expressions présentes dans les sources, pas nécessairement leurs valeurs à l'exécution. La vue de tout le projet peut être dense ; choisissez un point de départ pour la clarifier.

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
