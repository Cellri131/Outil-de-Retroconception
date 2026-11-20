package metier.lecture;

import java.util.ArrayList;
import java.util.List;
import metier.objet.Attribut;
import metier.objet.Classe;
import metier.objet.Langage;
import metier.objet.Methode;
import metier.objet.Parametre;

final class OutilsParseur
{
	private OutilsParseur() {}

	static Classe nouvelleClasse(String nom, String type, String parent, Langage langage)
	{
		Classe classe = new Classe(nom, parent, type, parent != null, "",
			new ArrayList<Attribut>(), new ArrayList<Methode>());
		classe.setLangage(langage);
		return classe;
	}

	static void ajouterAttribut(Classe classe, String nom, String type, String visibilite, boolean statique, boolean constant)
	{
		for (Attribut attribut : classe.getLstAttribut())
			if (attribut.getNom().equals(nom)) return;
		classe.getLstAttribut().add(new Attribut(nom, type, visibilite, statique ? "classe" : "instance", constant));
	}

	static List<Parametre> parametres(String texte, boolean python)
	{
		List<Parametre> resultat = new ArrayList<>();
		for (String brut : texte.split(","))
		{
			String param = brut.trim().replaceAll("\\s*=.*$", "");
			if (param.isEmpty() || param.equals("self") || param.equals("cls")) continue;
			String nom;
			String type = "";
			if (python && param.contains(":"))
			{
				String[] parties = param.split(":", 2);
				nom = parties[0].trim();
				type = parties[1].trim();
			}
			else
			{
				String[] parties = param.split("\\s+");
				nom = parties[parties.length - 1].replace("$", "");
				if (parties.length > 1) type = parties[parties.length - 2];
			}
			resultat.add(new Parametre(nom, type));
		}
		return resultat;
	}
}
