package vue.liaison;

import java.awt.Point;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vue.BlocClasse;

/**
 * Calcule les trajets dans l'ordre des liaisons et les conserve tant que la géométrie ne change pas.
 */
public class RouteurLiaisons
{
	private final List<BlocClasse> blocs;
	private final List<LiaisonVue> liaisons;
	private final Map<LiaisonVue, List<Point>> chemins = new IdentityHashMap<>();
	private List<Object> geometrie = List.of();

	public RouteurLiaisons(List<BlocClasse> blocs, List<LiaisonVue> liaisons)
	{
		this.blocs = blocs;
		this.liaisons = liaisons;
	}

	private void actualiser()
	{
		List<Object> etat = new ArrayList<>();
		for (BlocClasse bloc : blocs)
		{
			etat.add(bloc);
			etat.add(bloc.getX());
			etat.add(bloc.getY());
			etat.add(bloc.getLargeur());
			etat.add(bloc.getHauteurCalculee());
		}
		for (LiaisonVue liaison : liaisons)
		{
			etat.add(liaison);
			etat.add(liaison.getBlocOrigine());
			etat.add(liaison.getBlocDestination());
			etat.add(liaison.getCoteOrigine());
			etat.add(liaison.getCoteDestination());
			etat.add(liaison.getNivOrig());
			etat.add(liaison.getNivDest());
		}
		if (etat.equals(geometrie)) return;

		chemins.clear();
		List<List<Point>> parcours = new ArrayList<>();
		List<LiaisonVue> ordre = new ArrayList<>(liaisons);
		ordre.sort((a, b) ->
		{
			String ao = a.getBlocOrigine().getNom(), ad = a.getBlocDestination().getNom();
			String bo = b.getBlocOrigine().getNom(), bd = b.getBlocDestination().getNom();
			String a1 = ao.compareTo(ad) <= 0 ? ao : ad, a2 = ao.compareTo(ad) <= 0 ? ad : ao;
			String b1 = bo.compareTo(bd) <= 0 ? bo : bd, b2 = bo.compareTo(bd) <= 0 ? bd : bo;
			int comparaison = a1.compareTo(b1);
			if (comparaison == 0) comparaison = a2.compareTo(b2);
			if (comparaison != 0) return comparaison;
			boolean memeCoteA = a.getCoteOrigine() == a.getCoteDestination();
			boolean memeCoteB = b.getCoteOrigine() == b.getCoteDestination();
			if (memeCoteA != memeCoteB) return memeCoteA ? -1 : 1;
			if (!memeCoteA) return 0;
			comparaison = Integer.compare(a.getCoteOrigine(), b.getCoteOrigine());
			if (comparaison != 0) return comparaison;
			double positionA = ao.compareTo(ad) <= 0 ? a.getNivOrig() : a.getNivDest();
			double positionB = bo.compareTo(bd) <= 0 ? b.getNivOrig() : b.getNivDest();
			return Double.compare(positionB, positionA);
		});
		for (LiaisonVue liaison : ordre)
		{
			List<Point> chemin = liaison.calculerChemin(parcours);
			chemins.put(liaison, chemin);
			parcours.add(chemin);
		}
		geometrie = etat;
	}

	public List<Point> getChemin(LiaisonVue liaison)
	{
		actualiser();
		List<Point> chemin = chemins.get(liaison);
		return chemin != null ? chemin : liaison.calculerChemin(List.of());
	}

	public boolean aChevauchement(LiaisonVue liaison)
	{
		actualiser();
		List<Point> chemin = chemins.get(liaison);
		if (chemin == null) return false;
		GestionnaireIntersections detecteur = new GestionnaireIntersections();
		for (Map.Entry<LiaisonVue, List<Point>> autre : chemins.entrySet())
			if (autre.getKey() != liaison && detecteur.longueurSegmentsCommuns(chemin, autre.getValue()) > 0)
				return true;
		return false;
	}
}
