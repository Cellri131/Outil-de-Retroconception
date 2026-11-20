package metier.lecture;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import metier.objet.Classe;
import metier.objet.Langage;

public class ParseurCSharp extends ParseurAccolades
{
	protected Langage langage() { return Langage.CSHARP; }

	protected void apresDeclaration(String texte, Classe classe)
	{
		if (texte.contains(":"))
		{
			String bases = texte.substring(texte.indexOf(':') + 1).split("\\{", 2)[0].trim();
			String[] noms = bases.split(",");
			if (noms.length > 1)
				classe.setNomInterface(String.join(",", Arrays.copyOfRange(noms, 1, noms.length)));
		}
	}

	protected void horsClasse(String texte, int profondeur, String fichier, List<Classe> classes)
	{
		if (profondeur != 0 || !texte.endsWith(";") || texte.contains("(") || texte.startsWith("using ")) return;
		Matcher variable = ATTRIBUT.matcher(texte);
		if (!variable.matches() || variable.group(3) == null) return;
		String nom = "Variables globales (" + fichier + ")";
		Classe variables = classes.stream().filter(c -> c.getNom().equals(nom)).findFirst().orElse(null);
		if (variables == null)
		{
			variables = OutilsParseur.nouvelleClasse(nom, "class", null, langage());
			classes.add(variables);
		}
		OutilsParseur.ajouterAttribut(variables, variable.group(4), variable.group(3),
			"public", false, variable.group(2).contains("const"));
	}
}
