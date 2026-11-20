package metier.lecture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import metier.objet.Classe;
import metier.objet.Langage;

public class ParseurSql
{
	private static final Pattern TABLE = Pattern.compile(
		"(?i)\\bCREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[\\[`\"]?(\\w+)[\\]`\"]?\\s*\\(");
	private static final Pattern COLONNE = Pattern.compile(
		"(?i)^\\s*[\\[`\"]?(\\w+)[\\]`\"]?\\s+([\\w]+(?:\\s*\\([^)]*\\))?)");
	private static final Pattern REFERENCE = Pattern.compile(
		"(?i)\\bREFERENCES\\s+[\\[`\"]?(\\w+)[\\]`\"]?(?:\\s*\\.\\s*[\\[`\"]?(\\w+)[\\]`\"]?)?\\s*(?:\\(([^)]*)\\))?");
	private static final Pattern CLE = Pattern.compile("(?i)\\bFOREIGN\\s+KEY\\s*\\(([^)]*)\\)");

	public List<Classe> parser(String contenu, String nomFichier)
	{
		List<Classe> classes = new ArrayList<>();
		String propre = nettoyer(contenu);
		Matcher tables = TABLE.matcher(propre);
		while (tables.find())
		{
			Classe table = OutilsParseur.nouvelleClasse(tables.group(1), "class", null, Langage.SQL);
			classes.add(table);
			int debut = tables.end();
			int profondeur = 0;
			for (int i = debut; i < propre.length(); i++)
			{
				char c = propre.charAt(i);
				if (c == '(') profondeur++;
				else if (c == ')' && profondeur > 0) profondeur--;
				else if ((c == ',' || c == ')') && profondeur == 0)
				{
					String colonne = propre.substring(debut, i).trim();
					Matcher m = COLONNE.matcher(colonne);
					Matcher reference = REFERENCE.matcher(colonne);
					if (reference.find())
					{
						Matcher cle = CLE.matcher(colonne);
						String origine = cle.find() ? cle.group(1).trim() : (m.find() ? m.group(1) : "");
						ajouterReference(table, reference, origine);
						m.reset();
					}
					if (m.find() && !m.group(1).matches("(?i)PRIMARY|FOREIGN|CONSTRAINT|UNIQUE|CHECK|KEY|INDEX"))
						OutilsParseur.ajouterAttribut(table, m.group(1), m.group(2), "public", false, false);
					debut = i + 1;
					if (c == ')') break;
				}
			}
		}
		return classes;
	}

	private String nettoyer(String contenu)
	{
		return Pattern.compile("(?s)/\\*.*?\\*/|--[^\\r\\n]*|'(?:''|[^'])*'")
			.matcher(contenu).replaceAll(resultat -> resultat.group().replaceAll("[^\\r\\n]", " "));
	}

	private void ajouterReference(Classe table, Matcher reference, String origine)
	{
		String nom = reference.group(2) == null ? reference.group(1) : reference.group(2);
		String destination = reference.group(3) == null ? "" : reference.group(3).trim();
		for (Classe.CleEtrangere cle : table.getClesEtrangeres())
			if (cle.table.equalsIgnoreCase(nom) && cle.colonnesOrigine.equals(origine) &&
				cle.colonnesDestination.equals(destination)) return;
		table.getClesEtrangeres().add(new Classe.CleEtrangere(nom, origine, destination));
	}

	public void lireAlterSql(String contenu, Map<String, Classe> classes)
	{
		Matcher alterations = Pattern.compile(
			"(?is)\\bALTER\\s+TABLE\\s+[\\[`\"]?(\\w+)[\\]`\"]?(?:\\s*\\.\\s*[\\[`\"]?(\\w+)[\\]`\"]?)?\\s+(.*?);")
			.matcher(nettoyer(contenu));
		while (alterations.find())
		{
			String nom = alterations.group(2) == null ? alterations.group(1) : alterations.group(2);
			Classe table = classes.values().stream()
				.filter(c -> c.getLangage() == Langage.SQL && c.getNom().equalsIgnoreCase(nom))
				.findFirst().orElse(null);
			if (table == null)
			{
				System.err.println("Table SQL modifiée introuvable : " + nom);
				continue;
			}
			String clauses = alterations.group(3);
			Matcher cle = CLE.matcher(clauses);
			while (cle.find())
			{
				Matcher reference = REFERENCE.matcher(clauses);
				if (reference.find(cle.end()))
					ajouterReference(table, reference, cle.group(1).trim());
			}
		}
	}
}
