package vue;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import metier.lecture.AnalyseurFonctionnement;
import metier.lecture.AnalyseurFonctionnement.Graphe;
import metier.lecture.AnalyseurFonctionnement.Lien;
import metier.lecture.AnalyseurFonctionnement.Noeud;
import metier.lecture.AnalyseurFonctionnement.Relation;

public class TestOrganisationFonctionnement
{
	@Test
	void exemplePluginSuitLaChaineSansRevenirAuxAncetres() throws Exception
	{
		Graphe graphe = new AnalyseurFonctionnement().analyser(
			Path.of("test_util", "back-to-beast", "Plugin.cs").toString());
		Noeud plugin = noeud(graphe, "Plugin");
		Noeud awake = noeud(graphe, "Plugin.Awake()");
		Noeud ajout = noeud(graphe, "Plugin.AddHandSigils()");
		Noeud info = noeud(graphe, "handOfBeastInfo");
		Noeud ability = noeud(graphe, "HandOfBeastAbility");
		Noeud reponse = graphe.noeuds().stream()
			.filter(n -> n.nom().startsWith("HandOfBeastAbility.RespondsToUpkeep(")).findFirst().orElseThrow();
		Noeud retour = graphe.liens().stream()
			.filter(l -> l.origine().equals(reponse) && l.relation() == Relation.RETOUR)
			.map(Lien::destination).findFirst().orElseThrow();
		Map<Noeud, Integer> niveaux = PanneauFonctionnement.organiser(graphe, plugin, 8);
		Noeud[] chemin = {plugin, awake, ajout, info, ability, reponse, retour};
		for (int i = 0; i < chemin.length - 1; i++)
		{
			Noeud origine = chemin[i], destination = chemin[i + 1];
			assertTrue(graphe.liens().stream().anyMatch(l -> l.origine().equals(origine)
				&& l.destination().equals(destination) && PanneauFonctionnement.lienDescendant(l, niveaux)),
				origine.nom() + " -> " + destination.nom());
		}
		Lien raccourciStructurel = graphe.liens().stream()
			.filter(l -> l.origine().equals(plugin) && l.destination().equals(ajout)
				&& l.relation() == Relation.CONTIENT).findFirst().orElseThrow();
		assertFalse(PanneauFonctionnement.lienDescendant(raccourciStructurel, niveaux));
		Lien retourAuPlugin = graphe.liens().stream()
			.filter(l -> l.destination().equals(plugin) && l.relation() == Relation.REFERENCE)
			.findFirst().orElseThrow();
		assertFalse(PanneauFonctionnement.lienDescendant(retourAuPlugin, niveaux));
		SwingUtilities.invokeAndWait(() -> {
			PanneauFonctionnement panneau = new PanneauFonctionnement(graphe);
			JComboBox<?> liste = (JComboBox<?>) ((javax.swing.JPanel) panneau.getComponent(0)).getComponent(1);
			assertEquals(plugin, liste.getSelectedItem());
		});
	}

	private Noeud noeud(Graphe graphe, String nom)
	{
		return graphe.noeuds().stream().filter(n -> n.nom().equals(nom)).findFirst().orElseThrow();
	}
}
