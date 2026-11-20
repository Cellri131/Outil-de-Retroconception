package metier.objet;

import java.util.Locale;

public enum Langage
{
	JAVA, CSHARP, PYTHON, JAVASCRIPT, SQL, PHP, JSON;

	public static Langage depuisNomFichier(String nom)
	{
		String minuscule = nom.toLowerCase(Locale.ROOT);
		if (minuscule.endsWith(".java")) return JAVA;
		if (minuscule.endsWith(".cs"))   return CSHARP;
		if (minuscule.endsWith(".py"))   return PYTHON;
		if (minuscule.endsWith(".js"))   return JAVASCRIPT;
		if (minuscule.endsWith(".sql"))  return SQL;
		if (minuscule.endsWith(".php"))  return PHP;
		if (minuscule.endsWith(".json")) return JSON;
		return null;
	}
}
