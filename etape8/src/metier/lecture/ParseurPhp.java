package metier.lecture;

import java.util.regex.Pattern;
import metier.objet.Classe;
import metier.objet.Langage;

public class ParseurPhp extends ParseurAccolades
{
	private static final Pattern AVEC_TYPE = Pattern.compile("^\\$this->(\\w+)\\s*=\\s*new\\s+([\\w.]+)\\s*\\(");
	private static final Pattern SANS_TYPE = Pattern.compile("^\\$this->(\\w+)\\s*=");

	protected Langage langage() { return Langage.PHP; }

	protected void dansClasse(String texte, Classe classe)
	{
		if (texte.startsWith("$this->")) ajouterAttributInstance(texte, classe, AVEC_TYPE, SANS_TYPE);
	}
}
