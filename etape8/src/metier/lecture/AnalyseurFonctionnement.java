package metier.lecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import metier.objet.Langage;

/**
 * Analyse syntaxique approximative des appels et références, sans exécuter les sources.
 */
public class AnalyseurFonctionnement
{
	public enum Nature { CLASSE, METHODE, VALEUR, EXTERNE }
	public enum Relation { CONTIENT, APPEL, REFERENCE, CONSTRUCTION, AFFECTATION, RETOUR }

	public record Noeud(String id, String nom, Langage langage, Nature nature, Path fichier, int ligne) {}
	public record Lien(Noeud origine, Noeud destination, Relation relation, String arguments, int ligne) {}
	public record Graphe(List<Noeud> noeuds, List<Lien> liens) {}

	private record ClasseSource(Noeud noeud, int debut, int fin, int profondeur, String nom) {}
	private record MethodeSource(Noeud noeud, ClasseSource classe, String nom, int arite,
		int debutDeclaration, int debutCorps, int finCorps, String source, String masque) {}
	private record ExpressionSource(Noeud noeud, int debut, int fin) {}

	private static final Pattern CLASSE = Pattern.compile("\\b(?:class|interface|record|struct|enum)\\s+([A-Za-z_]\\w*)");
	private static final Pattern METHODE = Pattern.compile(
		"(?m)(?:^|(?<=[{};]))[ \\t]*(?:(?:public|private|protected|internal|static|abstract|virtual|override|async|final|"
		+ "synchronized|native|sealed|partial|extern|unsafe|new|default|strictfp)\\s+)*"
		+ "(?:[A-Za-z_][\\w.$<>\\[\\],?]*\\s+)?([A-Za-z_]\\w*)\\s*\\(");
	private static final Pattern CLASSE_PYTHON = Pattern.compile("(?m)^([ \\t]*)class\\s+([A-Za-z_]\\w*)\\b[^\\n]*:");
	private static final Pattern METHODE_PYTHON = Pattern.compile("(?m)^([ \\t]*)(?:async\\s+)?def\\s+([A-Za-z_]\\w*)\\s*\\(");
	private static final Pattern APPEL = Pattern.compile("(?<![\\w])(?:\\.\\s*)?([A-Za-z_]\\w*(?:\\s*\\.\\s*[A-Za-z_]\\w*)*)\\s*\\(");
	private static final Pattern REFERENCE = Pattern.compile("\\b([A-Za-z_]\\w*)\\s*\\.");
	private static final Pattern DECLARATION = Pattern.compile(
		"(?m)(?:^|[;{}\\n])[ \\t]*(?:(?:final|const|static|readonly)\\s+)*"
		+ "[A-Za-z_][\\w.$<>\\[\\],?]*\\s+([A-Za-z_]\\w*)\\s*=(?!=)");
	private static final Pattern AFFECTATION_PYTHON = Pattern.compile("(?m)^[ \\t]*([A-Za-z_]\\w*)[ \\t]*=(?!=)");
	private static final Pattern RETOUR = Pattern.compile("\\breturn\\b");
	private static final Set<String> MOTS_CLES = Set.of("if", "for", "while", "switch", "catch", "return",
		"throw", "assert", "synchronized", "using", "lock", "foreach", "with", "except", "match", "lambda",
		"def", "class", "sizeof", "nameof", "typeof", "default", "checked", "unchecked", "super", "this");

	public Graphe analyser(String cheminProjet)
	{
		Path chemin = Path.of(cheminProjet);
		if (!Files.exists(chemin)) throw new IllegalArgumentException("Projet introuvable : " + chemin);
		Map<String, Noeud> noeuds = new LinkedHashMap<>();
		List<Lien> liens = new ArrayList<>();
		List<MethodeSource> methodes = new ArrayList<>();
		List<ClasseSource> classes = new ArrayList<>();
		try (Stream<Path> chemins = Files.walk(chemin))
		{
			List<Path> fichiers = chemins.filter(Files::isRegularFile)
				.filter(p -> {
					Langage l = Langage.depuisNomFichier(p.getFileName().toString());
					return l == Langage.JAVA || l == Langage.CSHARP || l == Langage.PYTHON;
				}).sorted().toList();
			if (fichiers.isEmpty())
				throw new IllegalArgumentException("Aucun fichier Java, Python ou C# dans " + chemin);
			for (Path fichier : fichiers)
			{
				String source = Files.readString(fichier, StandardCharsets.UTF_8);
				Langage langage = Langage.depuisNomFichier(fichier.getFileName().toString());
				String masque = masquer(source, langage);
				if (langage == Langage.PYTHON)
					lirePython(fichier, source, masque, noeuds, classes, methodes, liens);
				else
					lireAccolades(fichier, langage, source, masque, noeuds, classes, methodes, liens);
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Impossible de lire les sources du projet : " + e.getMessage(), e);
		}

		Map<String, List<ClasseSource>> classesParNom = new HashMap<>();
		for (ClasseSource classe : classes)
			classesParNom.computeIfAbsent(cle(classe.noeud().langage(), classe.nom()), k -> new ArrayList<>()).add(classe);
		for (MethodeSource methode : methodes)
			lireRelations(methode, methodes, classesParNom, noeuds, liens);
		liens.sort(Comparator.comparing((Lien lien) -> lien.origine().id()).thenComparingInt(Lien::ligne));
		return new Graphe(List.copyOf(noeuds.values()), List.copyOf(liens));
	}

	private void lireAccolades(Path fichier, Langage langage, String source, String masque,
		Map<String, Noeud> noeuds, List<ClasseSource> classes, List<MethodeSource> methodes, List<Lien> liens)
	{
		int[] profondeurs = profondeurs(masque);
		List<ClasseSource> locales = new ArrayList<>();
		Matcher declaration = CLASSE.matcher(masque);
		while (declaration.find())
		{
			int ouverture = masque.indexOf('{', declaration.end());
			int pointVirgule = masque.indexOf(';', declaration.end());
			if (ouverture < 0 || (pointVirgule >= 0 && pointVirgule < ouverture)) continue;
			int fermeture = fermeture(masque, ouverture, '{', '}');
			if (fermeture < 0) continue;
			String nom = declaration.group(1);
			Noeud noeud = ajouterNoeud(noeuds, fichier, langage, Nature.CLASSE, nom, source, declaration.start());
			ClasseSource classe = new ClasseSource(noeud, ouverture, fermeture, profondeurs[ouverture] + 1, nom);
			locales.add(classe);
			classes.add(classe);
		}
		Matcher declarationMethode = METHODE.matcher(masque);
		while (declarationMethode.find())
		{
			int debut = declarationMethode.start();
			ClasseSource classe = classeContenante(locales, debut);
			if (classe == null || profondeurs[debut] != classe.profondeur()) continue;
			String nom = declarationMethode.group(1);
			if (MOTS_CLES.contains(nom)) continue;
			int ouverture = masque.indexOf('(', declarationMethode.start(1) + nom.length());
			int finParametres = fermeture(masque, ouverture, '(', ')');
			if (finParametres < 0) continue;
			int suivant = prochainNonBlanc(masque, finParametres + 1);
			if (suivant >= masque.length()) continue;
			int debutCorps, finCorps;
			if (masque.startsWith("throws ", suivant) || masque.startsWith("where ", suivant))
			{
				int accolade = masque.indexOf('{', suivant);
				int pointVirgule = masque.indexOf(';', suivant);
				suivant = accolade >= 0 && (pointVirgule < 0 || accolade < pointVirgule) ? accolade : pointVirgule;
				if (suivant < 0) continue;
			}
			if (masque.charAt(suivant) == '{')
			{
				int fin = fermeture(masque, suivant, '{', '}');
				if (fin < 0) continue;
				debutCorps = suivant + 1;
				finCorps = fin;
			}
			else if (masque.startsWith("=>", suivant))
			{
				int fin = masque.indexOf(';', suivant + 2);
				if (fin < 0) continue;
				debutCorps = suivant + 2;
				finCorps = fin;
			}
			else if (masque.charAt(suivant) == ';')
			{
				debutCorps = finCorps = suivant;
			}
			else continue;

			String parametres = source.substring(ouverture + 1, finParametres).trim().replaceAll("\\s+", " ");
			Noeud noeud = ajouterNoeud(noeuds, fichier, langage, Nature.METHODE,
				classe.nom() + "." + nom + "(" + parametres + ")", source, debut);
			methodes.add(new MethodeSource(noeud, classe, nom, compterParametres(masque.substring(ouverture + 1, finParametres)),
				debut, debutCorps, finCorps, source, masque));
			liens.add(new Lien(classe.noeud(), noeud, Relation.CONTIENT, "", noeud.ligne()));
		}
	}

	private void lirePython(Path fichier, String source, String masque, Map<String, Noeud> noeuds,
		List<ClasseSource> classes, List<MethodeSource> methodes, List<Lien> liens)
	{
		List<ClasseSource> locales = new ArrayList<>();
		List<int[]> plagesClasses = new ArrayList<>();
		String idModule = fichier.toAbsolutePath().normalize() + ":module";
		Noeud module = new Noeud(idModule, fichier.getFileName() + " (initialisation)",
			Langage.PYTHON, Nature.METHODE, fichier.toAbsolutePath().normalize(), 1);
		noeuds.put(idModule, module);
		Matcher declaration = CLASSE_PYTHON.matcher(masque);
		while (declaration.find())
		{
			int fin = finBlocPython(masque, declaration.end(), indentation(declaration.group(1)));
			String nom = declaration.group(2);
			Noeud noeud = ajouterNoeud(noeuds, fichier, Langage.PYTHON, Nature.CLASSE, nom, source, declaration.start());
			ClasseSource classe = new ClasseSource(noeud, declaration.end(), fin, indentation(declaration.group(1)), nom);
			locales.add(classe);
			classes.add(classe);
			plagesClasses.add(new int[] {declaration.start(), fin});
			liens.add(new Lien(module, noeud, Relation.CONTIENT, "", noeud.ligne()));
		}
		Matcher declarationMethode = METHODE_PYTHON.matcher(masque);
		while (declarationMethode.find())
		{
			int debut = declarationMethode.start();
			ClasseSource classe = classeContenante(locales, debut);
			String nom = declarationMethode.group(2);
			int ouverture = masque.indexOf('(', declarationMethode.start(2) + nom.length());
			int finParametres = fermeture(masque, ouverture, '(', ')');
			if (finParametres < 0) continue;
			int deuxPoints = masque.indexOf(':', finParametres + 1);
			int finLigne = masque.indexOf('\n', finParametres + 1);
			if (deuxPoints < 0 || (finLigne >= 0 && deuxPoints > finLigne)) continue;
			int fin = finBlocPython(masque, deuxPoints + 1, indentation(declarationMethode.group(1)));
			String parametres = source.substring(ouverture + 1, finParametres).trim().replaceAll("\\s+", " ");
			String prefixe = classe == null ? fichier.getFileName().toString() : classe.nom();
			Noeud noeud = ajouterNoeud(noeuds, fichier, Langage.PYTHON, Nature.METHODE,
				prefixe + "." + nom + "(" + parametres + ")", source, debut);
			int arite = compterArguments(masque.substring(ouverture + 1, finParametres));
			if (classe != null && parametres.matches("^(self|cls)(\\s*,.*)?$")) arite--;
			methodes.add(new MethodeSource(noeud, classe, nom, arite, debut, deuxPoints + 1, fin, source, masque));
			liens.add(new Lien(classe != null ? classe.noeud() : module, noeud, Relation.CONTIENT, "", noeud.ligne()));
		}
		char[] masqueModule = masque.toCharArray();
		for (int[] plage : plagesClasses)
			for (int i = plage[0]; i < plage[1]; i++)
				if (masqueModule[i] != '\n' && masqueModule[i] != '\r') masqueModule[i] = ' ';
		methodes.add(new MethodeSource(module, null, "<module>", 0, 0, 0, masque.length(), source, new String(masqueModule)));
	}

	private void lireRelations(MethodeSource methode, List<MethodeSource> methodes,
		Map<String, List<ClasseSource>> classes, Map<String, Noeud> noeuds, List<Lien> liens)
	{
		String corps = methode.masque().substring(methode.debutCorps(), methode.finCorps());
		List<ExpressionSource> expressions = lireExpressions(methode, methodes, noeuds, liens, corps);
		Matcher appel = APPEL.matcher(corps);
		while (appel.find())
		{
			String nom = appel.group(1).replaceAll("\\s+", "");
			if (MOTS_CLES.contains(nom) || nom.endsWith(".super") || nom.equals("new")) continue;
			int debutNom = methode.debutCorps() + appel.start(1);
			if (dansMethodeImbriquee(methode, debutNom, methodes)) continue;
			int ouverture = methode.debutCorps() + appel.end() - 1;
			int fin = fermeture(methode.masque(), ouverture, '(', ')');
			if (fin < 0 || fin > methode.finCorps()) continue;
			String arguments = methode.source().substring(ouverture + 1, fin).trim().replaceAll("\\s+", " ");
			int arite = compterArguments(methode.masque().substring(ouverture + 1, fin));
			boolean construction = avantEstNew(methode.masque(), debutNom);
			Noeud cible = resoudreAppel(methode, nom, arite, construction, methodes, classes, noeuds);
			liens.add(new Lien(origineExpression(methode, expressions, debutNom), cible,
				construction || cible.nature() == Nature.CLASSE ? Relation.CONSTRUCTION : Relation.APPEL,
				arguments, numeroLigne(methode.source(), debutNom)));
		}
		Matcher typeof = Pattern.compile("\\btypeof\\s*\\(\\s*([A-Za-z_]\\w*)\\s*\\)").matcher(corps);
		while (typeof.find())
			if (!dansMethodeImbriquee(methode, methode.debutCorps() + typeof.start(), methodes))
				ajouterReference(methode, expressions, typeof.group(1), "typeof", methode.debutCorps() + typeof.start(), true, classes, noeuds, liens);
		Matcher reference = REFERENCE.matcher(corps);
		while (reference.find())
			if (!dansMethodeImbriquee(methode, methode.debutCorps() + reference.start(), methodes))
				ajouterReference(methode, expressions, reference.group(1), "membre statique", methode.debutCorps() + reference.start(), false, classes, noeuds, liens);
	}

	private List<ExpressionSource> lireExpressions(MethodeSource methode, List<MethodeSource> methodes,
		Map<String, Noeud> noeuds, List<Lien> liens, String corps)
	{
		List<ExpressionSource> expressions = new ArrayList<>();
		boolean python = methode.noeud().langage() == Langage.PYTHON;
		Matcher declaration = (python ? AFFECTATION_PYTHON : DECLARATION).matcher(corps);
		while (declaration.find())
		{
			int position = methode.debutCorps() + declaration.start(1);
			if (dansMethodeImbriquee(methode, position, methodes)) continue;
			int debut = methode.debutCorps() + declaration.end();
			int fin = finExpression(methode.masque(), debut, methode.finCorps(), python);
			Noeud noeud = ajouterNoeud(noeuds, methode.noeud().fichier(), methode.noeud().langage(),
				Nature.VALEUR, declaration.group(1), methode.source(), position);
			String valeur = methode.source().substring(debut, fin).trim().replaceAll("\\s+", " ");
			liens.add(new Lien(methode.noeud(), noeud, Relation.AFFECTATION, valeur, numeroLigne(methode.source(), position)));
			expressions.add(new ExpressionSource(noeud, debut, fin));
		}
		Matcher retour = RETOUR.matcher(corps);
		while (retour.find())
		{
			int position = methode.debutCorps() + retour.start();
			if (dansMethodeImbriquee(methode, position, methodes)) continue;
			int debut = methode.debutCorps() + retour.end();
			int fin = finExpression(methode.masque(), debut, methode.finCorps(), python);
			String valeur = methode.source().substring(debut, fin).trim().replaceAll("\\s+", " ");
			boolean yield = motAvant(methode.masque(), position).equals("yield");
			Noeud noeud = ajouterNoeud(noeuds, methode.noeud().fichier(), methode.noeud().langage(),
				Nature.VALEUR, (yield ? "yield return" : "return") + (valeur.isEmpty() ? "" : " " + valeur),
				methode.source(), position);
			liens.add(new Lien(methode.noeud(), noeud, Relation.RETOUR, valeur, numeroLigne(methode.source(), position)));
			expressions.add(new ExpressionSource(noeud, debut, fin));
		}
		return expressions;
	}

	private static int finExpression(String masque, int debut, int limite, boolean python)
	{
		int profondeur = 0;
		for (int i = debut; i < limite; i++)
		{
			char c = masque.charAt(i);
			if (c == '(' || c == '[' || c == '{') profondeur++;
			else if (c == ')' || c == ']' || c == '}') profondeur--;
			else if (profondeur == 0 && (c == ';' || (python && c == '\n'))) return i;
		}
		return limite;
	}

	private static Noeud origineExpression(MethodeSource methode, List<ExpressionSource> expressions, int position)
	{
		return expressions.stream().filter(e -> e.debut() <= position && position < e.fin())
			.min(Comparator.comparingInt(e -> e.fin() - e.debut()))
			.map(ExpressionSource::noeud).orElse(methode.noeud());
	}

	private boolean dansMethodeImbriquee(MethodeSource origine, int position, List<MethodeSource> methodes)
	{
		for (MethodeSource autre : methodes)
			if (autre != origine && autre.noeud().fichier().equals(origine.noeud().fichier()) &&
				autre.debutDeclaration() >= origine.debutCorps() && autre.finCorps() <= origine.finCorps() &&
				position >= autre.debutDeclaration() && position < autre.finCorps()) return true;
		return false;
	}

	private Noeud resoudreAppel(MethodeSource origine, String nom, int arite, boolean construction,
		List<MethodeSource> methodes, Map<String, List<ClasseSource>> classes, Map<String, Noeud> noeuds)
	{
		String[] parties = nom.split("\\.");
		String nomMethode = parties[parties.length - 1];
		ClasseSource classe = null;
		if (construction || (origine.noeud().langage() == Langage.PYTHON && parties.length == 1))
		{
			List<ClasseSource> candidates = classes.getOrDefault(cle(origine.noeud().langage(), nomMethode), List.of());
			if (candidates.size() == 1) return candidates.get(0).noeud();
		}
		if (parties.length == 1 || (parties.length == 2 &&
			(parties[0].equals("this") || parties[0].equals("self") || parties[0].equals("cls"))))
			classe = origine.classe();
		else if (parties.length == 2)
		{
			List<ClasseSource> candidates = classes.getOrDefault(cle(origine.noeud().langage(), parties[0]), List.of());
			if (candidates.size() == 1) classe = candidates.get(0);
		}
		List<MethodeSource> candidates = new ArrayList<>();
		boolean appelSimple = parties.length == 1;
		for (MethodeSource methode : methodes)
			if ((classe != null || appelSimple) && methode.noeud().langage() == origine.noeud().langage() && methode.classe() == classe &&
				methode.nom().equals(nomMethode) && methode.arite() == arite &&
				(classe != null || methode.noeud().fichier().equals(origine.noeud().fichier())))
				candidates.add(methode);
		if (candidates.size() == 1) return candidates.get(0).noeud();
		if (candidates.isEmpty() && origine.noeud().langage() == Langage.PYTHON && appelSimple && origine.classe() != null)
		{
			for (MethodeSource methode : methodes)
				if (methode.classe() == null && methode.noeud().fichier().equals(origine.noeud().fichier()) &&
					methode.nom().equals(nomMethode) && methode.arite() == arite) candidates.add(methode);
			if (candidates.size() == 1) return candidates.get(0).noeud();
		}
		if (candidates.isEmpty() && origine.noeud().langage() == Langage.PYTHON && parties.length == 2 &&
			Pattern.compile("(?m)^[ \\t]*import[ \\t]+" + Pattern.quote(parties[0]) + "[ \\t]*(?:,|$)")
				.matcher(origine.masque()).find())
		{
			for (MethodeSource methode : methodes)
			{
				String fichier = methode.noeud().fichier().getFileName().toString();
				if (methode.classe() == null && fichier.equals(parties[0] + ".py") &&
					methode.nom().equals(nomMethode) && methode.arite() == arite) candidates.add(methode);
			}
			if (candidates.size() == 1) return candidates.get(0).noeud();
		}
		String cle = "externe:" + origine.noeud().langage() + ":" + nom;
		return noeuds.computeIfAbsent(cle, k -> new Noeud(k, nom + " [externe / non résolu]",
			origine.noeud().langage(), Nature.EXTERNE, null, 0));
	}

	private void ajouterReference(MethodeSource origine, List<ExpressionSource> expressions,
		String nom, String detail, int position, boolean inclureExterne,
		Map<String, List<ClasseSource>> classes, Map<String, Noeud> noeuds, List<Lien> liens)
	{
		List<ClasseSource> candidates = classes.getOrDefault(cle(origine.noeud().langage(), nom), List.of());
		if (candidates.size() != 1 && !inclureExterne) return;
		String cleExterne = "type-externe:" + origine.noeud().langage() + ":" + nom;
		Noeud cible = candidates.size() == 1 ? candidates.get(0).noeud()
			: noeuds.computeIfAbsent(cleExterne, k -> new Noeud(k, nom + " [type externe / non résolu]",
				origine.noeud().langage(), Nature.EXTERNE, null, 0));
		liens.add(new Lien(origineExpression(origine, expressions, position), cible, Relation.REFERENCE, detail,
			numeroLigne(origine.source(), position)));
	}

	private static String cle(Langage langage, String nom) { return langage + ":" + nom; }

	private static Noeud ajouterNoeud(Map<String, Noeud> noeuds, Path fichier, Langage langage,
		Nature nature, String nom, String source, int position)
	{
		String id = fichier.toAbsolutePath().normalize() + ":" + nature + ":" + position;
		Noeud noeud = new Noeud(id, nom, langage, nature, fichier.toAbsolutePath().normalize(), numeroLigne(source, position));
		noeuds.put(id, noeud);
		return noeud;
	}

	private static ClasseSource classeContenante(List<ClasseSource> classes, int position)
	{
		return classes.stream().filter(c -> c.debut() <= position && position < c.fin())
			.min(Comparator.comparingInt(c -> c.fin() - c.debut())).orElse(null);
	}

	private static int[] profondeurs(String source)
	{
		int[] profondeurs = new int[source.length() + 1];
		int profondeur = 0;
		for (int i = 0; i < source.length(); i++)
		{
			profondeurs[i] = profondeur;
			if (source.charAt(i) == '{') profondeur++;
			else if (source.charAt(i) == '}') profondeur--;
		}
		profondeurs[source.length()] = profondeur;
		return profondeurs;
	}

	private static int fermeture(String source, int debut, char ouvrant, char fermant)
	{
		if (debut < 0 || debut >= source.length() || source.charAt(debut) != ouvrant) return -1;
		int profondeur = 0;
		for (int i = debut; i < source.length(); i++)
		{
			if (source.charAt(i) == ouvrant) profondeur++;
			else if (source.charAt(i) == fermant && --profondeur == 0) return i;
		}
		return -1;
	}

	private static int compterArguments(String texte)
	{
		return compterElements(texte, false);
	}

	private static int compterParametres(String texte)
	{
		return compterElements(texte, true);
	}

	private static int compterElements(String texte, boolean generiques)
	{
		if (texte.trim().isEmpty()) return 0;
		int nombre = 1, profondeur = 0;
		for (int i = 0; i < texte.length(); i++)
		{
			char c = texte.charAt(i);
			if (c == '(' || c == '[' || c == '{' || (generiques && c == '<')) profondeur++;
			else if (c == ')' || c == ']' || c == '}' || (generiques && c == '>')) profondeur--;
			else if (c == ',' && profondeur == 0) nombre++;
		}
		return nombre;
	}

	private static boolean avantEstNew(String masque, int position)
	{
		return motAvant(masque, position).equals("new");
	}

	private static String motAvant(String masque, int position)
	{
		int i = position - 1;
		while (i >= 0 && Character.isWhitespace(masque.charAt(i))) i--;
		int fin = i + 1;
		while (i >= 0 && Character.isJavaIdentifierPart(masque.charAt(i))) i--;
		return masque.substring(i + 1, fin);
	}

	private static int prochainNonBlanc(String texte, int position)
	{
		while (position < texte.length() && Character.isWhitespace(texte.charAt(position))) position++;
		return position;
	}

	private static int indentation(String texte)
	{
		int indentation = 0;
		for (char c : texte.toCharArray()) indentation += c == '\t' ? 8 - indentation % 8 : 1;
		return indentation;
	}

	private static int finBlocPython(String masque, int debut, int indentation)
	{
		int position = masque.indexOf('\n', debut);
		if (position < 0) return masque.length();
		position++;
		while (position < masque.length())
		{
			int fin = masque.indexOf('\n', position);
			if (fin < 0) fin = masque.length();
			String ligne = masque.substring(position, fin);
			if (!ligne.isBlank())
			{
				int n = ligne.length() - ligne.stripLeading().length();
				if (indentation(ligne.substring(0, n)) <= indentation) return position;
			}
			position = fin + 1;
		}
		return masque.length();
	}

	private static int numeroLigne(String texte, int position)
	{
		int ligne = 1;
		for (int i = 0; i < position; i++) if (texte.charAt(i) == '\n') ligne++;
		return ligne;
	}

	private static String masquer(String source, Langage langage)
	{
		char[] resultat = source.toCharArray();
		for (int i = 0; i < source.length(); i++)
		{
			char c = source.charAt(i);
			if (langage == Langage.PYTHON && c == '#' ||
				langage != Langage.PYTHON && c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/')
			{
				while (i < source.length() && source.charAt(i) != '\n') resultat[i++] = ' ';
				i--;
			}
			else if (langage != Langage.PYTHON && c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*')
			{
				resultat[i++] = ' ';
				resultat[i] = ' ';
				while (++i < source.length())
				{
					if (source.charAt(i) == '*' && i + 1 < source.length() && source.charAt(i + 1) == '/')
					{
						resultat[i] = resultat[++i] = ' ';
						break;
					}
					if (source.charAt(i) != '\n' && source.charAt(i) != '\r') resultat[i] = ' ';
				}
			}
			else if (c == '"' || c == '\'')
			{
				char quote = c;
				boolean triple = i + 2 < source.length() && source.charAt(i + 1) == c && source.charAt(i + 2) == c;
				boolean verbatim = langage == Langage.CSHARP && i > 0 && source.charAt(i - 1) == '@';
				resultat[i] = quote;
				if (triple) { resultat[++i] = ' '; resultat[++i] = ' '; }
				while (++i < source.length())
				{
					c = source.charAt(i);
					if (c != '\n' && c != '\r') resultat[i] = ' ';
					if (!verbatim && c == '\\' && i + 1 < source.length())
					{
						if (source.charAt(++i) != '\n' && source.charAt(i) != '\r') resultat[i] = ' ';
					}
					else if (c == quote)
					{
						if (verbatim && i + 1 < source.length() && source.charAt(i + 1) == quote) resultat[++i] = ' ';
						else if (!triple) break;
						else if (i + 2 < source.length() && source.charAt(i + 1) == quote && source.charAt(i + 2) == quote)
						{
							resultat[++i] = ' ';
							resultat[++i] = ' ';
							break;
						}
					}
				}
			}
		}
		return new String(resultat);
	}
}
