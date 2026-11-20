import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import metier.lecture.Lecture;
import metier.objet.Association;
import metier.objet.Classe;
import metier.objet.Langage;
import vue.BlocClasse;
import vue.PanneauDiagramme;
import vue.liaison.LiaisonVue;
import vue.liaison.CalculateurChemin;
import vue.liaison.DetecteurObstacles;
import vue.liaison.GestionnaireAncrage;
import vue.liaison.GestionnaireIntersections;

public class TestLiaisonsEtNavigation
{
	@TempDir Path dossier;

	private void ecrire(String nom, String contenu) throws Exception
	{
		Files.writeString(dossier.resolve(nom), contenu);
	}

	@Test
	void clesSqlMultiplesEtAlterDansUnAutreFichier() throws Exception
	{
		ecrire("a.sql", "CREATE TABLE users (id INT PRIMARY KEY);\n"
			+ "CREATE TABLE games (id INT, player1 INT REFERENCES users(id), player2 INT,"
			+ " FOREIGN KEY (player2) REFERENCES users(id));");
		ecrire("b.sql", "ALTER TABLE games ADD CONSTRAINT fk FOREIGN KEY (id) REFERENCES users(id);");
		Lecture lecture = new Lecture(dossier.toString());
		List<Association> associations = lecture.getLstAssociation();
		assertEquals(3, associations.size());
		assertEquals(List.of("id", "player1", "player2"),
			associations.stream().map(Association::getRoleOrig).sorted().toList());
		for (Association association : associations)
		{
			assertEquals("games", association.getClasseOrig().getNom());
			assertEquals("users", association.getClasseDest().getNom());
			assertEquals("id", association.getRoleDest());
			assertNull(association.getMultOrig());
		}
		assertEquals(3, lecture.getHashMapClasses().get("games").getLstAttribut().size());
	}

	@Test
	void liaisonsCSharpEtVariablesDePremierNiveau() throws Exception
	{
		ecrire("Exemple.cs", "int compteur = 1;\n"
			+ "interface IAnimal {\n void Agir();\n}\n"
			+ "class Animal {\n}\n"
			+ "class Groupe : Animal, IAnimal {\n"
			+ " private List<Animal> animaux;\n public Animal? chef { get; set; }\n}\n");
		Lecture lecture = new Lecture(dossier.toString());
		Classe variables = lecture.getHashMapClasses().get("Variables globales (Exemple.cs)");
		assertNotNull(variables);
		assertEquals("compteur", variables.getLstAttribut().get(0).getNom());
		assertEquals(1, lecture.getLstHeritage().size());
		assertEquals(1, lecture.getLstInterface().size());
		assertEquals(1, lecture.getLstAssociation().size());
		assertEquals("Groupe", lecture.getLstAssociation().get(0).getClasseOrig().getNom());
	}

	@Test
	void liaisonsParTypeDansLesAutresLangages() throws Exception
	{
		ecrire("python.py", "class Oiseau:\n    pass\nclass Nid:\n    def __init__(self):\n        self.oiseau: Oiseau = None\n");
		ecrire("php.php", "<?php\nclass Chat {\n}\nclass Maison {\n private ?Chat $chat;\n}\n");
		ecrire("js.js", "class Vehicule {\n}\nclass Voiture extends Vehicule {\n constructor() {\n this.vehicule = new Vehicule();\n }\n}\n");
		Lecture lecture = new Lecture(dossier.toString());
		assertEquals(3, lecture.getLstAssociation().size());
		assertEquals(1, lecture.getLstHeritage().size());
	}

	@Test
	void objetsJsonEtTableauxSontRepresentesSansLeursValeurs() throws Exception
	{
		ecrire("donnees.json", "{\"nom\":\"secret\", \"profil\":{\"age\":30},"
			+ "\"liste\":[{\"id\":1},{\"id\":2},null], \"cle/\":true}");
		Lecture lecture = new Lecture(dossier.toString());
		var classes = lecture.getHashMapClasses();
		assertEquals(4, classes.size());
		Classe racine = classes.get("JSON donnees.json");
		assertEquals(Langage.JSON, racine.getLangage());
		assertEquals(List.of("nom", "profil", "liste", "cle%002F"),
			racine.getLstAttribut().stream().map(a -> a.getNom()).toList());
		assertEquals(List.of("chaine", "objet", "tableau", "booleen"),
			racine.getLstAttribut().stream().map(a -> a.getType()).toList());
		assertNotNull(classes.get("JSON donnees.json.profil"));
		assertNotNull(classes.get("JSON donnees.json.liste[0]"));
		assertNotNull(classes.get("JSON donnees.json.liste[1]"));
		assertFalse(racine.getLstAttribut().toString().contains("secret"));
	}

	@Test
	void jsonInvalideSignaleLeFichierEtLaPosition() throws Exception
	{
		ecrire("invalide.json", "{\"a\": [1,]}");
		IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
			() -> new Lecture(dossier.toString()));
		assertTrue(erreur.getMessage().contains("invalide.json"));
		assertTrue(erreur.getMessage().contains("position"));
	}

	@Test
	void tableauJsonRacineEtEchappements() throws Exception
	{
		ecrire("tableau.json", "[{\"a\\u002fb\": 1e-2}, true]");
		var classes = new Lecture(dossier.toString()).getHashMapClasses();
		assertEquals(2, classes.size());
		assertEquals("tableau", classes.get("JSON tableau.json").getLstAttribut().get(0).getType());
		assertEquals("a%002Fb", classes.get("JSON tableau.json[0]").getLstAttribut().get(0).getNom());
	}

	@Test
	void projetMixteUtiliseLeParseurDeChaqueFichier() throws Exception
	{
		ecrire("JavaTest.java", "public class JavaTest {\n private int nombre;\n}\n");
		ecrire("CSharp.cs", "class CSharp {\n private int nombre;\n}\n");
		ecrire("python.py", "class Python:\n    nombre = 1\n");
		ecrire("script.js", "class Javascript {\n nombre = 1;\n}\n");
		ecrire("tables.sql", "CREATE TABLE TableSql (nombre INT);");
		ecrire("php.php", "<?php\nclass Php {\n private int $nombre;\n}\n");
		ecrire("json.json", "\uFEFF{\"nombre\": 1}");
		var classes = new Lecture(dossier.toString()).getHashMapClasses();
		String[] noms = {"JavaTest", "CSharp", "Python", "Javascript", "TableSql", "Php", "JSON json.json"};
		Langage[] langages = {Langage.JAVA, Langage.CSHARP, Langage.PYTHON, Langage.JAVASCRIPT,
			Langage.SQL, Langage.PHP, Langage.JSON};
		assertEquals(noms.length, classes.size());
		for (int i = 0; i < noms.length; i++)
		{
			Classe classe = classes.get(noms[i]);
			assertNotNull(classe, noms[i]);
			assertEquals(langages[i], classe.getLangage());
			assertTrue(classe.getLstAttribut().stream().anyMatch(a -> a.getNom().equals("nombre")), noms[i]);
		}
	}

	@Test
	void texteLongResteDansLeCadre()
	{
		BlocClasse bloc = new BlocClasse("NomDeClasseExtremementLongPourLeDiagramme", 10, 10);
		bloc.setAttributsAffichage(List.of("- unAttributExtremementLong : UnTypeExtremementLong"));
		bloc.setMethodesAffichage(List.of("- Postfix(__instance: PlayableCard, __result: bool) : void"));
		assertTrue(bloc.getLargeur() > 200);
		BufferedImage image = new BufferedImage(bloc.getLargeur() + 50, bloc.getHauteurCalculee() + 50, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		bloc.dessiner(g, true, true);
		g.dispose();
		for (int x = bloc.getX() + bloc.getLargeur() + 3; x < image.getWidth(); x++)
			for (int y = 0; y < image.getHeight(); y++)
				assertEquals(Color.WHITE.getRGB(), image.getRGB(x, y), "Dessin hors cadre");
		assertTrue(bloc.contient(bloc.getX() + bloc.getLargeur() - 1, bloc.getY() + 40));
	}

	@Test
	void lesClesAvecLaMemeCibleRestentAffichables()
	{
		BlocClasse source = new BlocClasse("games", 50, 50);
		BlocClasse cible = new BlocClasse("users", 350, 50);
		LiaisonVue premiere = new LiaisonVue(source, cible, "association", true, "", "");
		LiaisonVue seconde = new LiaisonVue(source, cible, "association", true, "", "");
		premiere.setRoleOrig("player1");
		seconde.setRoleOrig("player2");
		List<LiaisonVue> liaisons = List.of(premiere, seconde);
		for (LiaisonVue liaison : liaisons)
		{
			liaison.setToutesLesLiaisons(liaisons);
			liaison.setTousLesBlocs(List.of(source, cible));
			liaison.recalculerAncrages();
		}
		BufferedImage image = new BufferedImage(600, 250, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		for (LiaisonVue liaison : liaisons) liaison.dessiner(g);
		g.dispose();
		boolean trace = false;
		for (int x = 255; x < 340; x++)
			for (int y = 0; y < image.getHeight(); y++)
				if (image.getRGB(x, y) != Color.WHITE.getRGB()) trace = true;
		assertTrue(trace, "Les liaisons doivent être dessinées entre les deux blocs");
	}

	@Test
	void contourneUnObstacleEtResteAttacheAuxCadres()
	{
		BlocClasse origine = new BlocClasse("A", 20, 100);
		BlocClasse obstacle = new BlocClasse("Obstacle", 320, 100);
		BlocClasse destination = new BlocClasse("B", 650, 100);
		DetecteurObstacles detecteur = new DetecteurObstacles(origine, destination, List.of(origine, obstacle, destination));
		CalculateurChemin calculateur = new CalculateurChemin(detecteur);
		Point debut = GestionnaireAncrage.getPointSurCote(origine, 1, 0.5);
		Point fin = GestionnaireAncrage.getPointSurCote(destination, 3, 0.5);
		List<Point> chemin = calculateur.creerCheminEviteObstacles(debut, fin, 1, 3);
		assertEquals(debut, chemin.get(0));
		assertEquals(fin, chemin.get(chemin.size() - 1));
		assertFalse(calculateur.cheminADesCollisions(chemin));
		for (int i = 1; i < chemin.size(); i++)
			assertTrue(chemin.get(i - 1).x == chemin.get(i).x || chemin.get(i - 1).y == chemin.get(i).y);
	}

	@Test
	void liaisonsMultiplesEtBoucleOntDesAncragesDistincts()
	{
		BlocClasse origine = new BlocClasse("A", 20, 100);
		BlocClasse destination = new BlocClasse("B", 450, 100);
		LiaisonVue premiere = new LiaisonVue(origine, destination, "association");
		LiaisonVue seconde = new LiaisonVue(origine, destination, "association");
		LiaisonVue boucle = new LiaisonVue(origine, origine, "association");
		LiaisonVue autreBoucle = new LiaisonVue(origine, origine, "association");
		List<LiaisonVue> groupe = List.of(premiere, seconde);
		for (LiaisonVue liaison : groupe)
		{
			liaison.setTousLesBlocs(List.of(origine, destination));
			liaison.setToutesLesLiaisons(groupe);
			liaison.recalculerAncrages();
		}
		assertNotEquals(premiere.getChemin().get(0), seconde.getChemin().get(0));
		assertNotEquals(premiere.getChemin().get(premiere.getChemin().size() - 1),
			seconde.getChemin().get(seconde.getChemin().size() - 1));
		assertNotEquals(boucle.getChemin().get(0), boucle.getChemin().get(boucle.getChemin().size() - 1));
		List<LiaisonVue> boucles = List.of(boucle, autreBoucle);
		boucle.setToutesLesLiaisons(boucles);
		autreBoucle.setToutesLesLiaisons(boucles);
		boucle.recalculerAncrages();
		assertNotEquals(boucle.getChemin().get(0), autreBoucle.getChemin().get(0));
		assertNotEquals(boucle.getChemin().get(boucle.getChemin().size() - 1),
			autreBoucle.getChemin().get(autreBoucle.getChemin().size() - 1));
		assertFalse(new CalculateurChemin(new DetecteurObstacles(origine, origine, List.of(origine)))
			.cheminADesCollisions(boucle.getChemin()));
	}

	@Test
	void placementRegroupeLesVoisinsSansChevauchementEtResteStable()
	{
		PanneauDiagramme panneau = new PanneauDiagramme(null);
		BlocClasse a = new BlocClasse("A", 0, 0);
		BlocClasse b = new BlocClasse("B", 0, 0);
		BlocClasse c = new BlocClasse("C", 0, 0);
		BlocClasse d = new BlocClasse("D", 0, 0);
		panneau.getBlocsClasses().addAll(List.of(d, c, b, a));
		panneau.getLstLiaisons().add(new LiaisonVue(a, d, "association"));
		panneau.optimiserPositionsClasses();
		assertEquals(List.of(a, d, b, c), panneau.getBlocsClasses());
		List<Point> positions = panneau.getBlocsClasses().stream().map(bloc -> new Point(bloc.getX(), bloc.getY())).toList();
		for (int i = 0; i < 4; i++)
			for (int j = i + 1; j < 4; j++)
			{
				BlocClasse x = panneau.getBlocsClasses().get(i), y = panneau.getBlocsClasses().get(j);
				assertFalse(new Rectangle(x.getX(), x.getY(), x.getLargeur(), x.getHauteurCalculee())
					.intersects(new Rectangle(y.getX(), y.getY(), y.getLargeur(), y.getHauteurCalculee())));
			}
		panneau.optimiserPositionsClasses();
		assertEquals(positions, panneau.getBlocsClasses().stream().map(bloc -> new Point(bloc.getX(), bloc.getY())).toList());
	}

	@Test
	void ancrageManuelSuitLeBlocEtLeDeplacementDeLaPlanche()
	{
		BlocClasse origine = new BlocClasse("A", 20, 100);
		BlocClasse destination = new BlocClasse("B", 450, 100);
		LiaisonVue liaison = new LiaisonVue(origine, destination, "association");
		liaison.setSideOrigine(1);
		liaison.setPosRelOrig(0.5);
		origine.setX(70);
		Point ancrage = GestionnaireAncrage.getPointSurCote(origine, 1, 0.5);
		assertTrue(liaison.isOnOriginAnchor(ancrage));
		liaison.dragOriginAnchor(new Point(origine.getX() + origine.getLargeur() + 40, origine.getY() + 20 + 30),
			1, 40, 30, 800, 600);
		assertEquals("RIGHT", liaison.getSideOrig());
		assertEquals(GestionnaireAncrage.getPointSurCote(origine, 1, liaison.getNivOrig()), liaison.getChemin().get(0));
	}

	@Test
	void optimisationDesLiaisonsEstStableEtRenduNeModifiePasLesAncrages()
	{
		PanneauDiagramme panneau = new PanneauDiagramme(null);
		BlocClasse a = new BlocClasse("A", 30, 30);
		BlocClasse b = new BlocClasse("B", 400, 30);
		panneau.getBlocsClasses().addAll(List.of(a, b));
		LiaisonVue l1 = new LiaisonVue(a, b, "association");
		LiaisonVue l2 = new LiaisonVue(b, a, "association");
		l1.setRoleOrig("cle1");
		l2.setRoleOrig("cle2");
		panneau.getLstLiaisons().addAll(List.of(l2, l1));
		panneau.optimiserPositionsLiaisons();
		List<Point> chemin1 = l1.getChemin();
		List<Point> chemin2 = l2.getChemin();
		panneau.optimiserPositionsLiaisons();
		assertEquals(chemin1, l1.getChemin());
		assertEquals(chemin2, l2.getChemin());
		l1.setSideOrigine(2);
		l1.setPosRelOrig(0.37);
		BufferedImage image = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		l1.dessiner(g);
		g.dispose();
		assertEquals("BOTTOM", l1.getSideOrig());
		assertEquals(0.37, l1.getNivOrig());
		assertEquals(GestionnaireAncrage.getPointSurCote(a, 2, 0.37), l1.getChemin().get(0));
	}

	@Test
	void plusieursSourcesVersUneMemeTableNePartagentNiAncrageNiSegment()
	{
		PanneauDiagramme panneau = new PanneauDiagramme(null);
		BlocClasse users = new BlocClasse("users", 700, 300);
		users.setAttributsAffichage(List.of("id : INT", "username : VARCHAR", "email : VARCHAR"));
		BlocClasse games = new BlocClasse("games", 50, 300);
		BlocClasse friends = new BlocClasse("friends", 50, 50);
		BlocClasse cards = new BlocClasse("cards", 50, 600);
		panneau.getBlocsClasses().addAll(List.of(users, games, friends, cards));
		for (BlocClasse source : List.of(games, friends, cards))
			for (int i = 0; i < 2; i++)
			{
				LiaisonVue liaison = new LiaisonVue(source, users, "association");
				liaison.setRoleOrig(source.getNom() + i);
				panneau.getLstLiaisons().add(liaison);
			}
		panneau.optimiserPositionsLiaisons();
		GestionnaireIntersections intersections = new GestionnaireIntersections();
		List<LiaisonVue> liaisons = panneau.getLstLiaisons();
		for (int i = 0; i < liaisons.size(); i++)
		{
			LiaisonVue liaison = liaisons.get(i);
			List<Point> chemin = liaison.getChemin();
			assertFalse(new CalculateurChemin(new DetecteurObstacles(liaison.getBlocOrigine(), users,
				panneau.getBlocsClasses())).cheminADesCollisions(chemin));
			for (int j = 0; j < i; j++)
			{
				List<Point> autre = liaisons.get(j).getChemin();
				assertNotEquals(chemin.get(chemin.size() - 1), autre.get(autre.size() - 1));
				assertEquals(0, intersections.longueurSegmentsCommuns(chemin, autre));
			}
		}
		List<List<Point>> avant = liaisons.stream().map(LiaisonVue::getChemin).toList();
		panneau.optimiserPositionsLiaisons();
		assertEquals(avant, liaisons.stream().map(LiaisonVue::getChemin).toList());
	}

	@Test
	void cadresAlignesDeHauteursDifferentesOntUneLiaisonDroite()
	{
		PanneauDiagramme panneau = new PanneauDiagramme(null);
		BlocClasse a = new BlocClasse("A", 50, 100);
		BlocClasse b = new BlocClasse("B", 400, 100);
		b.setAttributsAffichage(List.of("id", "un", "deux"));
		panneau.getBlocsClasses().addAll(List.of(a, b));
		LiaisonVue liaison = new LiaisonVue(a, b, "association");
		panneau.getLstLiaisons().add(liaison);
		panneau.optimiserPositionsLiaisons();
		List<Point> chemin = liaison.getChemin();
		assertEquals(2, chemin.size(), chemin.toString());
		assertEquals(chemin.get(0).y, chemin.get(1).y);
		assertEquals(a.getX() + a.getLargeur(), chemin.get(0).x);
		assertEquals(b.getX(), chemin.get(1).x);
	}

	@Test
	void rolesAuxDeuxBoutsDuneLiaisonCourteNeSeSuperposentPas()
	{
		BufferedImage image = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setFont(new Font("Arial", Font.PLAIN, 10));
		var fm = g.getFontMetrics();
		String texte = "part, variant";
		Point[] positions = GestionnaireAncrage.calculerPositionsRoles(
			new Point(100, 50), 1, texte, new Point(200, 50), 3, texte, fm);
		Rectangle premier = new Rectangle(positions[0].x, positions[0].y - fm.getAscent(), fm.stringWidth(texte), fm.getHeight());
		Rectangle second = new Rectangle(positions[1].x, positions[1].y - fm.getAscent(), fm.stringWidth(texte), fm.getHeight());
		g.dispose();
		assertFalse(premier.intersects(second));
	}

	@Test
	void faisceauDenseUtiliseDesTrajetsDistinctsSansCroisement()
	{
		PanneauDiagramme panneau = new PanneauDiagramme(null);
		BlocClasse a = new BlocClasse("A", 50, 100);
		BlocClasse b = new BlocClasse("B", 700, 100);
		panneau.getBlocsClasses().addAll(List.of(a, b));
		for (int i = 0; i < 9; i++)
		{
			LiaisonVue liaison = new LiaisonVue(a, b, "association");
			liaison.setRoleOrig("cle" + i);
			panneau.getLstLiaisons().add(liaison);
		}
		panneau.optimiserPositionsLiaisons();
		GestionnaireIntersections detecteur = new GestionnaireIntersections();
		List<LiaisonVue> liaisons = panneau.getLstLiaisons();
		for (int i = 0; i < liaisons.size(); i++)
		{
			List<Point> chemin = liaisons.get(i).getChemin();
			assertFalse(new CalculateurChemin(new DetecteurObstacles(a, b, panneau.getBlocsClasses()))
				.cheminADesCollisions(chemin));
			for (int j = 0; j < i; j++)
			{
				List<Point> autre = liaisons.get(j).getChemin();
				assertEquals(0, detecteur.longueurSegmentsCommuns(chemin, autre));
				assertEquals(0, detecteur.nombreCroisements(chemin, autre));
			}
		}
	}

	@Test
	void deplacerUnBlocRecalculeLesTrajetsSansChangerLesAncrages()
	{
		PanneauDiagramme panneau = new PanneauDiagramme(null);
		BlocClasse a = new BlocClasse("A", 50, 100);
		BlocClasse b = new BlocClasse("B", 400, 100);
		panneau.getBlocsClasses().addAll(List.of(a, b));
		LiaisonVue liaison = new LiaisonVue(a, b, "association");
		panneau.getLstLiaisons().add(liaison);
		panneau.optimiserPositionsLiaisons();
		List<Point> avant = liaison.getChemin();
		double position = liaison.getNivDest();
		b.setY(250);
		List<Point> apres = liaison.getChemin();
		assertNotEquals(avant, apres);
		assertEquals(position, liaison.getNivDest());
		assertEquals(GestionnaireAncrage.getPointSurCote(b, liaison.getCoteDestination(), position),
			apres.get(apres.size() - 1));
	}

	@Test
	void liaisonImpossibleAContournerEstSignaleeEnRouge()
	{
		BlocClasse origine = new BlocClasse("A", 50, 50);
		BlocClasse destination = new BlocClasse("B", 500, 50);
		BlocClasse obstacle = new BlocClasse("Obstacle", 50, 50);
		LiaisonVue liaison = new LiaisonVue(origine, destination, "association");
		liaison.setTousLesBlocs(List.of(origine, destination, obstacle));
		BufferedImage image = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		liaison.dessiner(g);
		g.dispose();
		boolean rouge = false;
		for (int x = 0; x < image.getWidth() && !rouge; x++)
			for (int y = 0; y < image.getHeight(); y++)
				if (image.getRGB(x, y) == Color.RED.getRGB()) { rouge = true; break; }
		assertTrue(rouge);
	}

	@Test
	void deplacementMoletteEtEspaceNeDeplacePasLesBlocs() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> {
			PanneauDiagramme panneau = new PanneauDiagramme(null);
			panneau.setSize(800, 600);
			BlocClasse bloc = new BlocClasse("Bloc", 100, 100);
			panneau.getBlocsClasses().add(bloc);
			glisser(panneau, MouseEvent.BUTTON2, 0, 150, 150, 200, 190);
			assertEquals(100, bloc.getX());
			assertEquals(100, bloc.getY());
			assertSame(bloc, cliquer(panneau, 210, 200));
			panneau.getActionMap().get("panEspaceDebut").actionPerformed(new ActionEvent(panneau, 0, ""));
			glisser(panneau, MouseEvent.BUTTON1, 0, 210, 200, 180, 160);
			panneau.getActionMap().get("panEspaceFin").actionPerformed(new ActionEvent(panneau, 0, ""));
			assertEquals(100, bloc.getX());
			assertEquals(100, bloc.getY());
			assertSame(bloc, cliquer(panneau, 180, 160));
		});
	}

	private static void glisser(PanneauDiagramme panneau, int bouton, int mod, int x, int y, int finX, int finY)
	{
		int masque = bouton == MouseEvent.BUTTON1 ? InputEvent.BUTTON1_DOWN_MASK : InputEvent.BUTTON2_DOWN_MASK;
		panneau.dispatchEvent(new MouseEvent(panneau, MouseEvent.MOUSE_PRESSED, 0, mod | masque, x, y, 1, false, bouton));
		panneau.dispatchEvent(new MouseEvent(panneau, MouseEvent.MOUSE_DRAGGED, 1, mod | masque, finX, finY, 0, false, MouseEvent.NOBUTTON));
		panneau.dispatchEvent(new MouseEvent(panneau, MouseEvent.MOUSE_RELEASED, 2, mod, finX, finY, 1, false, bouton));
	}

	private static BlocClasse cliquer(PanneauDiagramme panneau, int x, int y)
	{
		panneau.dispatchEvent(new MouseEvent(panneau, MouseEvent.MOUSE_PRESSED, 0,
			InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
		panneau.dispatchEvent(new MouseEvent(panneau, MouseEvent.MOUSE_RELEASED, 1, 0,
			x, y, 1, false, MouseEvent.BUTTON1));
		return panneau.getBlocClasseClic();
	}
}
