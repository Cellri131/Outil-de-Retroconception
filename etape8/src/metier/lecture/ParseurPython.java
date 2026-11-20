package metier.lecture;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import metier.objet.Classe;
import metier.objet.Langage;
import metier.objet.Methode;

public class ParseurPython
{
	private static final Pattern CLASSE = Pattern.compile("^class\\s+(\\w+)(?:\\((\\w+)[^)]*\\))?\\s*:");
	private static final Pattern METHODE = Pattern.compile(
		"^(?:async\\s+)?def\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:->\\s*([^:]+))?:");
	private static final Pattern ATTRIBUT = Pattern.compile(
		"^(?:self\\.)?(\\w+)\\s*(?::\\s*([\\w.\\[\\], ]+))?\\s*=");
	private static final Pattern INSTANCE = Pattern.compile("=\\s*([\\w.]+)\\s*\\(");

	public List<Classe> parser(String contenu, String nomFichier)
	{
		List<Classe> classes = new ArrayList<>();
		Classe courante = null;
		int indentationClasse = -1;
		int indentationMembres = -1;
		for (String ligne : contenu.split("\\R"))
		{
			String texte = ligne.trim();
			if (texte.isEmpty() || texte.startsWith("#")) continue;
			int indentation = ligne.length() - ligne.stripLeading().length();
			Matcher declaration = CLASSE.matcher(texte);
			if (declaration.find())
			{
				courante = OutilsParseur.nouvelleClasse(declaration.group(1), "class", declaration.group(2), Langage.PYTHON);
				classes.add(courante);
				indentationClasse = indentation;
				indentationMembres = -1;
				continue;
			}
			if (courante == null) continue;
			if (indentation <= indentationClasse) { courante = null; continue; }
			if (indentationMembres < 0 || indentation < indentationMembres)
				indentationMembres = indentation;
			Matcher methode = METHODE.matcher(texte);
			if (methode.find())
				courante.getLstMethode().add(new Methode(methode.group(1),
					methode.group(3) == null ? "" : methode.group(3).trim(),
					visibilite(methode.group(1)), false, OutilsParseur.parametres(methode.group(2), true)));
			else
			{
				Matcher attribut = ATTRIBUT.matcher(texte);
				if (attribut.find() && (texte.startsWith("self.") || indentation == indentationMembres))
				{
					String type = attribut.group(2) == null ? "" : attribut.group(2).trim();
					Matcher instance = INSTANCE.matcher(texte);
					if (type.isEmpty() && instance.find()) type = instance.group(1);
					OutilsParseur.ajouterAttribut(courante, attribut.group(1), type,
						visibilite(attribut.group(1)), !texte.startsWith("self."), false);
				}
			}
		}
		return classes;
	}

	private String visibilite(String nom)
	{
		return nom.startsWith("__") && !nom.endsWith("__") ? "private" :
			nom.startsWith("_") && !nom.startsWith("__") ? "protected" : "public";
	}
}
