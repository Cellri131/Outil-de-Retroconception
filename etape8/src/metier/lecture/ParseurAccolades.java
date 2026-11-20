package metier.lecture;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import metier.objet.Classe;
import metier.objet.Langage;
import metier.objet.Methode;

abstract class ParseurAccolades
{
	private static final Pattern DECLARATION = Pattern.compile(
		"\\b(class|interface|enum)\\s+(\\w+)(?:\\s+extends\\s+(\\w+)|\\s*:\\s*(\\w+))?");
	private static final Pattern METHODE = Pattern.compile(
		"^(?:(public|private|protected|internal)\\s+)?(?:(?:static|abstract|virtual|override|async|final|readonly|function)\\s+)*(?:(\\w+(?:\\[\\])?\\??)\\s+)?(\\w+)\\s*\\(([^)]*)\\)\\s*(?::\\s*(\\??\\w+))?.*");
	protected static final Pattern ATTRIBUT = Pattern.compile(
		"^(?:(public|private|protected|internal)\\s+)?((?:(?:static|readonly|const)\\s+)*)(?:(\\??[\\w.]+(?:\\s*<\\s*[\\w.]+\\s*>)?(?:\\[\\])?\\??)\\s+)?(\\$?\\w+)\\s*(?:[=;{].*)$");
	private static final Pattern MASQUE = Pattern.compile(
		"(?s)/\\*.*?\\*/|//[^\\r\\n]*|#[^\\r\\n]*|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'");
	private static final Pattern INSTANCE = Pattern.compile("=\\s*new\\s+([\\w.]+)\\s*\\(");

	protected abstract Langage langage();

	public List<Classe> parser(String contenu, String nomFichier)
	{
		List<Classe> classes = new ArrayList<>();
		Classe courante = null;
		int profondeur = 0;
		int profondeurClasse = -1;
		boolean attendAccolade = false;
		String propre = MASQUE.matcher(contenu).replaceAll(
			resultat -> resultat.group().replaceAll("[^\\r\\n]", " "));
		for (String ligne : propre.split("\\R"))
		{
			String texte = ligne.trim();
			if (courante == null)
			{
				Matcher declaration = DECLARATION.matcher(texte);
				if (declaration.find())
				{
					String parent = declaration.group(3) != null ? declaration.group(3) : declaration.group(4);
					courante = OutilsParseur.nouvelleClasse(declaration.group(2), declaration.group(1), parent, langage());
					apresDeclaration(texte, courante);
					classes.add(courante);
					profondeurClasse = profondeur + 1;
					attendAccolade = !texte.contains("{");
				}
				else
					horsClasse(texte, profondeur, nomFichier, classes);
			}
			else
			{
				if (attendAccolade && texte.contains("{")) attendAccolade = false;
				else if (!attendAccolade && profondeur == profondeurClasse)
					lireMembre(texte, courante);
				dansClasse(texte, courante);
			}
			for (char caractere : ligne.toCharArray())
			{
				if (caractere == '{') profondeur++;
				if (caractere == '}') profondeur--;
			}
			if (courante != null && !attendAccolade && profondeur < profondeurClasse)
				courante = null;
		}
		return classes;
	}

	protected void apresDeclaration(String texte, Classe classe) {}
	protected void horsClasse(String texte, int profondeur, String fichier, List<Classe> classes) {}
	protected void dansClasse(String texte, Classe classe) {}

	private void lireMembre(String texte, Classe classe)
	{
		Matcher methode = METHODE.matcher(texte);
		if (methode.matches())
		{
			String nom = methode.group(3);
			if (nom.equals("if") || nom.equals("for") || nom.equals("while")) return;
			String retour = methode.group(5) != null ? methode.group(5) : methode.group(2);
			classe.getLstMethode().add(new Methode(nom, retour == null ? "" : retour,
				visibilite(methode.group(1)), texte.contains("abstract"),
				OutilsParseur.parametres(methode.group(4), false)));
			return;
		}
		Matcher attribut = ATTRIBUT.matcher(texte);
		if (attribut.matches() && !texte.startsWith("return "))
		{
			String nom = attribut.group(4).replace("$", "");
			String type = attribut.group(3) == null ? "" : attribut.group(3);
			Matcher instance = INSTANCE.matcher(texte);
			if (type.isEmpty() && instance.find()) type = instance.group(1);
			OutilsParseur.ajouterAttribut(classe, nom, type, visibilite(attribut.group(1)),
				attribut.group(2).contains("static"), attribut.group(2).contains("const"));
		}
	}

	protected void ajouterAttributInstance(String texte, Classe classe, Pattern avecType, Pattern sansType)
	{
		Matcher matcher = avecType.matcher(texte);
		if (matcher.find())
			OutilsParseur.ajouterAttribut(classe, matcher.group(1), matcher.group(2), "public", false, false);
		else
		{
			matcher = sansType.matcher(texte);
			if (matcher.find())
				OutilsParseur.ajouterAttribut(classe, matcher.group(1), "", "public", false, false);
		}
	}

	private String visibilite(String texte)
	{
		return "private".equals(texte) || "protected".equals(texte) ? texte : "public";
	}
}
