package metier.lecture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import metier.objet.Classe;
import metier.objet.Langage;

/**
 * Lit les objets JSON et représente leurs champs sans exposer leurs valeurs.
 */
public class ParseurJson
{
	private static final Pattern NOMBRE = Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");
	private static final int PROFONDEUR_MAX = 256;
	private String contenu;
	private int position;

	private static class Valeur
	{
		final String type;
		final Map<String, Valeur> champs = new LinkedHashMap<>();
		final List<Valeur> elements = new ArrayList<>();

		Valeur(String type) { this.type = type; }
	}

	public List<Classe> parser(String contenu, String nomFichier)
	{
		this.contenu = contenu;
		this.position = !contenu.isEmpty() && contenu.charAt(0) == '\uFEFF' ? 1 : 0;
		try
		{
			Valeur racine = lireValeur(0);
			espaces();
			if (position != contenu.length()) throw erreur("contenu après la valeur JSON");
			List<Classe> classes = new ArrayList<>();
			String nom = "JSON " + nomFichier;
			if (racine.type.equals("objet"))
				ajouterObjet(classes, nom, racine);
			else
			{
				Classe bloc = OutilsParseur.nouvelleClasse(nom, "class", null, Langage.JSON);
				OutilsParseur.ajouterAttribut(bloc, "valeur", racine.type, "public", false, false);
				classes.add(bloc);
				ajouterEnfants(classes, nom, racine);
			}
			return classes;
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("JSON invalide dans " + nomFichier + " : " + e.getMessage(), e);
		}
	}

	private void ajouterObjet(List<Classe> classes, String nom, Valeur objet)
	{
		Classe bloc = OutilsParseur.nouvelleClasse(nom, "class", null, Langage.JSON);
		classes.add(bloc);
		for (Map.Entry<String, Valeur> champ : objet.champs.entrySet())
		{
			String cle = encoderCle(champ.getKey());
			OutilsParseur.ajouterAttribut(bloc, cle, champ.getValue().type, "public", false, false);
			ajouterEnfants(classes, nom + "." + cle, champ.getValue());
		}
	}

	private void ajouterEnfants(List<Classe> classes, String nom, Valeur valeur)
	{
		if (valeur.type.equals("objet")) ajouterObjet(classes, nom, valeur);
		else if (valeur.type.equals("tableau"))
			for (int i = 0; i < valeur.elements.size(); i++)
				ajouterEnfants(classes, nom + "[" + i + "]", valeur.elements.get(i));
	}

	private String encoderCle(String cle)
	{
		if (cle.isEmpty()) return "%VIDE";
		StringBuilder resultat = new StringBuilder();
		for (int i = 0; i < cle.length(); i++)
		{
			char c = cle.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '_' || c == '-') resultat.append(c);
			else resultat.append(String.format("%%%04X", (int) c));
		}
		return resultat.toString();
	}

	private Valeur lireValeur(int profondeur)
	{
		if (profondeur > PROFONDEUR_MAX) throw erreur("imbrication trop profonde");
		espaces();
		if (position >= contenu.length()) throw erreur("valeur attendue");
		char c = contenu.charAt(position);
		if (c == '{') return lireObjet(profondeur + 1);
		if (c == '[') return lireTableau(profondeur + 1);
		if (c == '"') { lireChaine(); return new Valeur("chaine"); }
		if (c == 't') { lireMot("true"); return new Valeur("booleen"); }
		if (c == 'f') { lireMot("false"); return new Valeur("booleen"); }
		if (c == 'n') { lireMot("null"); return new Valeur("null"); }
		Matcher nombre = NOMBRE.matcher(contenu);
		nombre.region(position, contenu.length());
		if (nombre.lookingAt())
		{
			position = nombre.end();
			return new Valeur("nombre");
		}
		throw erreur("valeur attendue");
	}

	private Valeur lireObjet(int profondeur)
	{
		Valeur objet = new Valeur("objet");
		position++;
		espaces();
		if (consommer('}')) return objet;
		do
		{
			espaces();
			if (position >= contenu.length() || contenu.charAt(position) != '"')
				throw erreur("nom de champ attendu");
			String cle = lireChaine();
			espaces();
			if (!consommer(':')) throw erreur("':' attendu");
			Valeur valeur = lireValeur(profondeur);
			if (objet.champs.putIfAbsent(cle, valeur) != null)
				throw erreur("champ en double : " + cle);
			espaces();
			if (consommer('}')) return objet;
			if (!consommer(',')) throw erreur("',' ou '}' attendu");
		} while (true);
	}

	private Valeur lireTableau(int profondeur)
	{
		Valeur tableau = new Valeur("tableau");
		position++;
		espaces();
		if (consommer(']')) return tableau;
		do
		{
			tableau.elements.add(lireValeur(profondeur));
			espaces();
			if (consommer(']')) return tableau;
			if (!consommer(',')) throw erreur("',' ou ']' attendu");
		} while (true);
	}

	private String lireChaine()
	{
		position++;
		StringBuilder texte = new StringBuilder();
		while (position < contenu.length())
		{
			char c = contenu.charAt(position++);
			if (c == '"') return texte.toString();
			if (c < 0x20) throw erreur("caractère de contrôle dans une chaîne");
			if (c != '\\') { texte.append(c); continue; }
			if (position >= contenu.length()) throw erreur("échappement incomplet");
			char echappe = contenu.charAt(position++);
			switch (echappe)
			{
				case '"': case '\\': case '/': texte.append(echappe); break;
				case 'b': texte.append('\b'); break;
				case 'f': texte.append('\f'); break;
				case 'n': texte.append('\n'); break;
				case 'r': texte.append('\r'); break;
				case 't': texte.append('\t'); break;
				case 'u':
					if (position + 4 > contenu.length()) throw erreur("échappement Unicode incomplet");
					int code = 0;
					for (int i = 0; i < 4; i++)
					{
						char hex = contenu.charAt(position++);
						int chiffre = (hex >= '0' && hex <= '9') ? hex - '0' :
							(hex >= 'a' && hex <= 'f') ? hex - 'a' + 10 :
							(hex >= 'A' && hex <= 'F') ? hex - 'A' + 10 : -1;
						if (chiffre < 0) throw erreur("échappement Unicode invalide");
						code = code * 16 + chiffre;
					}
					texte.append((char) code);
					break;
				default: throw erreur("échappement invalide");
			}
		}
		throw erreur("chaîne non terminée");
	}

	private void lireMot(String mot)
	{
		if (!contenu.startsWith(mot, position)) throw erreur("valeur attendue");
		position += mot.length();
	}

	private void espaces()
	{
		while (position < contenu.length())
		{
			char c = contenu.charAt(position);
			if (c != ' ' && c != '\t' && c != '\r' && c != '\n') break;
			position++;
		}
	}

	private boolean consommer(char attendu)
	{
		if (position < contenu.length() && contenu.charAt(position) == attendu)
		{
			position++;
			return true;
		}
		return false;
	}

	private IllegalArgumentException erreur(String message)
	{
		return new IllegalArgumentException(message + " (position " + (position + 1) + ")");
	}
}
