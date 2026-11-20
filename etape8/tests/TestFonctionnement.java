import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import metier.lecture.AnalyseurFonctionnement;
import metier.lecture.AnalyseurFonctionnement.Graphe;
import metier.lecture.AnalyseurFonctionnement.Lien;
import metier.lecture.AnalyseurFonctionnement.Nature;
import metier.lecture.AnalyseurFonctionnement.Relation;
import vue.PanneauFonctionnement;

public class TestFonctionnement
{
	@TempDir Path dossier;

	private void ecrire(String nom, String contenu) throws Exception
	{
		Files.writeString(dossier.resolve(nom), contenu);
	}

	private List<Lien> liens(Graphe graphe, String methode)
	{
		return graphe.liens().stream().filter(l -> l.origine().nom().startsWith(methode + "(")).toList();
	}

	@Test
	void distingueAppelsReferencesEtArgumentsCSharp() throws Exception
	{
		ecrire("Plugin.cs", """
			class Plugin {
			    void Awake() { AddHandSigils("x", Config()); }
			    int Config() { return 1; }
			    void AddHandSigils(string nom, int nombre) {
			        // Ignorer faux()
			        AbilityManager.New(typeof(HandOfBeastAbility), nom);
			        HandOfBeastAbility.ability = nombre;
			    }
			}
			class HandOfBeastAbility {
			    void OnUpkeep() { Log.Write("vrai"); }
			}
			""");
		Graphe graphe = new AnalyseurFonctionnement().analyser(dossier.toString());
		List<Lien> awake = liens(graphe, "Plugin.Awake");
		assertTrue(awake.stream().anyMatch(l -> l.relation() == Relation.APPEL
			&& l.destination().nom().startsWith("Plugin.AddHandSigils(")
			&& l.arguments().equals("\"x\", Config()")));
		assertTrue(awake.stream().anyMatch(l -> l.destination().nom().startsWith("Plugin.Config(")));
		List<Lien> add = liens(graphe, "Plugin.AddHandSigils");
		assertTrue(add.stream().anyMatch(l -> l.relation() == Relation.REFERENCE
			&& l.destination().nom().equals("HandOfBeastAbility") && l.arguments().equals("typeof")));
		assertTrue(add.stream().anyMatch(l -> l.relation() == Relation.APPEL
			&& l.destination().nature() == Nature.EXTERNE && l.destination().nom().startsWith("AbilityManager.New")
			&& l.arguments().contains("typeof(HandOfBeastAbility)")));
		assertFalse(graphe.liens().stream().anyMatch(l -> l.destination().nom().contains("faux")));
		assertTrue(awake.stream().allMatch(l -> l.ligne() > 0 && l.origine().ligne() > 0));
	}

	@Test
	void suitLesAppelsJavaEntreFichiersSansConfondreLesTextes() throws Exception
	{
		ecrire("A.java", """
			class A {
			    void start() { B.run("appel(faux)", value()); }
			    int value() { return 1; }
			}
			""");
		ecrire("B.java", """
			class B {
			    static void run(String texte, int n) { System.out.println(texte); }
			}
			""");
		Graphe graphe = new AnalyseurFonctionnement().analyser(dossier.toString());
		List<Lien> start = liens(graphe, "A.start");
		assertTrue(start.stream().anyMatch(l -> l.destination().nom().startsWith("B.run(")
			&& l.arguments().equals("\"appel(faux)\", value()")));
		assertTrue(start.stream().anyMatch(l -> l.destination().nom().startsWith("A.value(")));
		assertFalse(graphe.liens().stream().anyMatch(l -> l.destination().nom().contains("faux")));
	}

	@Test
	void suitLesMethodesEtFonctionsPythonEntreModules() throws Exception
	{
		ecrire("main.py", """
			import util
			def helper(x):
			    return x
			class Service:
			    def start(self):
			        self.save("abc")
			        helper(1)
			        util.work("valeur")
			    def save(self, data):
			        print(data)
			if __name__ == "__main__":
			    Service().start()
			""");
		ecrire("util.py", """
			def work(data):
			    return data
			""");
		Graphe graphe = new AnalyseurFonctionnement().analyser(dossier.toString());
		List<Lien> start = liens(graphe, "Service.start");
		assertTrue(start.stream().anyMatch(l -> l.destination().nom().startsWith("Service.save(")
			&& l.arguments().equals("\"abc\"")));
		assertTrue(start.stream().anyMatch(l -> l.destination().nom().startsWith("main.py.helper(")));
		assertTrue(start.stream().anyMatch(l -> l.destination().nom().startsWith("util.py.work(")));
		assertTrue(liens(graphe, "Service.save").stream().anyMatch(l -> l.destination().nature() == Nature.EXTERNE
			&& l.destination().nom().startsWith("print")));
		assertTrue(graphe.liens().stream().anyMatch(l -> l.origine().nom().equals("main.py (initialisation)")
			&& l.relation() == Relation.CONSTRUCTION && l.destination().nom().equals("Service")));
		assertFalse(graphe.liens().stream().anyMatch(l -> l.origine().nom().equals("main.py (initialisation)")
			&& l.destination().nom().startsWith("print")));
	}

	@Test
	void objetPythonNonImporteNeDevientPasUnModuleParSonNom() throws Exception
	{
		ecrire("main.py", """
			def start(util):
			    util.work("valeur")
			""");
		ecrire("util.py", """
			def work(data):
			    return data
			""");
		Graphe graphe = new AnalyseurFonctionnement().analyser(dossier.toString());
		assertTrue(liens(graphe, "main.py.start").stream().anyMatch(l ->
			l.destination().nature() == Nature.EXTERNE && l.destination().nom().startsWith("util.work")));
	}

	@Test
	void lexemplePluginRelieAwakeAAddHandSigilsPuisAuTypeDeLaCarte()
	{
		Path exemple = Path.of("test_util", "back-to-beast", "Plugin.cs");
		assertTrue(Files.isRegularFile(exemple));
		Graphe graphe = new AnalyseurFonctionnement().analyser(exemple.toString());
		assertTrue(liens(graphe, "Plugin.Awake").stream().anyMatch(l -> l.destination().nom().equals("Plugin.AddHandSigils()")));
		assertTrue(liens(graphe, "Plugin.AddHandSigils").stream().anyMatch(l ->
			l.relation() == Relation.AFFECTATION && l.destination().nom().equals("handOfBeastInfo")));
		assertTrue(graphe.liens().stream().anyMatch(l ->
			l.origine().nom().equals("handOfBeastInfo") && l.relation() == Relation.REFERENCE
				&& l.destination().nom().equals("HandOfBeastAbility") && l.arguments().equals("typeof")));
		assertTrue(graphe.liens().stream().anyMatch(l ->
			l.origine().nom().equals("HandOfBeastAbility") && l.relation() == Relation.CONTIENT
				&& l.destination().nom().startsWith("HandOfBeastAbility.RespondsToUpkeep(")));
		assertTrue(liens(graphe, "HandOfBeastAbility.RespondsToUpkeep").stream().anyMatch(l ->
			l.relation() == Relation.RETOUR && l.arguments().contains("playerUpkeep")));
		assertTrue(liens(graphe, "Plugin.Awake").stream().anyMatch(l ->
			l.destination().nom().startsWith("PatchAll") && l.destination().nature() == Nature.EXTERNE));
	}

	@Test
	void surchargeAmbigueResteNonResolueEtLesFonctionsImbriqueesNeSontPasAttribueesAuParent() throws Exception
	{
		ecrire("A.java", """
			class A {
			    void go() { run("valeur"); }
			    void run(String valeur) {}
			    void run(Object valeur) {}
			}
			""");
		ecrire("nested.py", """
			def outer():
			    def inner():
			        print("interne")
			    inner()
			""");
		Graphe graphe = new AnalyseurFonctionnement().analyser(dossier.toString());
		assertTrue(liens(graphe, "A.go").stream().anyMatch(l -> l.destination().nature() == Nature.EXTERNE
			&& l.destination().nom().contains("non résolu")));
		assertTrue(liens(graphe, "nested.py.outer").stream().anyMatch(l -> l.destination().nom().startsWith("nested.py.inner(")));
		assertFalse(liens(graphe, "nested.py.outer").stream().anyMatch(l -> l.destination().nom().startsWith("print")));
	}

	@Test
	void reconnaitLesMethodesCompactesEtLesParametresGeneriques() throws Exception
	{
		ecrire("Compact.java", """
			class Compact { void first() { second(null, 4); } void second(java.util.Map<String, Integer> valeurs, int n) {} }
			""");
		Graphe graphe = new AnalyseurFonctionnement().analyser(dossier.toString());
		assertTrue(liens(graphe, "Compact.first").stream().anyMatch(l ->
			l.destination().nom().startsWith("Compact.second(") && l.arguments().equals("null, 4")));
	}

	@Test
	void affectationsEtRetoursRelientLeursExpressionsEnJavaEtPython() throws Exception
	{
		ecrire("A.java", """
			class A {
			    int start() { int valeur = calcul(); return calcul(); }
			    int calcul() { return 1; }
			}
			""");
		ecrire("b.py", """
			def start():
			    valeur = calcul()
			    return calcul()
			def calcul():
			    return 1
			""");
		Graphe graphe = new AnalyseurFonctionnement().analyser(dossier.toString());
		for (String nom : List.of("A.start", "b.py.start"))
		{
			Lien affectation = liens(graphe, nom).stream().filter(l -> l.relation() == Relation.AFFECTATION)
				.findFirst().orElseThrow();
			Lien retour = liens(graphe, nom).stream().filter(l -> l.relation() == Relation.RETOUR)
				.findFirst().orElseThrow();
			assertEquals("valeur", affectation.destination().nom());
			assertEquals("calcul()", affectation.arguments());
			assertEquals("calcul()", retour.arguments());
			assertTrue(graphe.liens().stream().anyMatch(l -> l.origine().equals(affectation.destination())
				&& l.relation() == Relation.APPEL && l.destination().nom().contains(".calcul(")));
			assertTrue(graphe.liens().stream().anyMatch(l -> l.origine().equals(retour.destination())
				&& l.relation() == Relation.APPEL && l.destination().nom().contains(".calcul(")));
		}
	}

	@Test
	void vueAfficheLesDetailsEtPermetDeSuivreUnPointDeDepart() throws Exception
	{
		ecrire("A.java", """
			class A {
			    void start() { next("donnee"); next("autre"); }
			    void next(String valeur) {}
			}
			""");
		Graphe graphe = new AnalyseurFonctionnement().analyser(dossier.toString());
		var depart = graphe.noeuds().stream().filter(n -> n.nom().equals("A.start()")).findFirst().orElseThrow();
		SwingUtilities.invokeAndWait(() -> {
			PanneauFonctionnement panneau = new PanneauFonctionnement(graphe);
			JComboBox<?> liste = trouver(panneau, JComboBox.class);
			assertNotNull(liste);
			liste.setSelectedItem(depart);
			JSplitPane split = trouver(panneau, JSplitPane.class);
			JPanel dessin = (JPanel) ((JScrollPane) split.getLeftComponent()).getViewport().getView();
			dessin.setSize(dessin.getPreferredSize());
			BufferedImage image = new BufferedImage(dessin.getWidth(), dessin.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			dessin.paint(g);
			g.dispose();
			dessin.dispatchEvent(new MouseEvent(dessin, MouseEvent.MOUSE_CLICKED, 1, 0,
				50, 50, 1, false, MouseEvent.BUTTON1));
			JTextArea details = trouver(panneau, JTextArea.class);
			assertTrue(details.getText().contains("A.next"));
			assertTrue(details.getText().contains("\"donnee\""));
			dessin.dispatchEvent(new MouseEvent(dessin, MouseEvent.MOUSE_CLICKED, 2, 0,
				355, 66, 1, false, MouseEvent.BUTTON1));
			assertTrue(details.getText().contains("Occurrences : 2"));
			assertTrue(details.getText().contains("\"donnee\""));
			assertTrue(details.getText().contains("\"autre\""));
		});
	}

	private static <T extends Component> T trouver(Container parent, Class<T> type)
	{
		for (Component composant : parent.getComponents())
		{
			if (type.isInstance(composant)) return type.cast(composant);
			if (composant instanceof Container enfant)
			{
				T resultat = trouver(enfant, type);
				if (resultat != null) return resultat;
			}
		}
		return null;
	}
}
