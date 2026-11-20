package metier.lecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import metier.objet.Classe;
import metier.objet.Langage;

public class AnalyseurFichier
{
	private HashMap<String, Classe> hashMapClasses = new HashMap<>();
	private ArrayList<String> lstNomFichier = new ArrayList<>();

	public HashMap<String, Classe> analyser(String cheminFichier)
	{
		hashMapClasses.clear();
		lstNomFichier.clear();
		Path chemin = Path.of(cheminFichier);
		if (!Files.exists(chemin))
			throw new IllegalArgumentException("Fichier ou dossier introuvable : " + chemin);

		try (Stream<Path> chemins = Files.walk(chemin))
		{
			List<Path> fichiers = chemins.filter(Files::isRegularFile)
				.filter(p -> Langage.depuisNomFichier(p.getFileName().toString()) != null)
				.filter(p -> !p.getFileName().toString().equalsIgnoreCase("package-info.java")
					&& !p.getFileName().toString().equalsIgnoreCase("module-info.java"))
				.sorted().collect(Collectors.toList());
			if (fichiers.isEmpty())
				throw new IllegalArgumentException("Aucun fichier source pris en charge dans : " + chemin);

			for (Path fichier : fichiers)
			{
				Langage langage = Langage.depuisNomFichier(fichier.getFileName().toString());
				List<Classe> classes;
				if (langage == Langage.JAVA)
				{
					try (Scanner scanner = new Scanner(fichier, StandardCharsets.UTF_8))
					{
						classes = List.of(new ParseurJava().parser(scanner, fichier.getFileName().toString()));
					}
				}
				else
				{
					String contenu = Files.readString(fichier, StandardCharsets.UTF_8);
					String nomFichier = fichier.getFileName().toString();
					classes = switch (langage)
					{
						case CSHARP -> new ParseurCSharp().parser(contenu, nomFichier);
						case PYTHON -> new ParseurPython().parser(contenu, nomFichier);
						case JAVASCRIPT -> new ParseurJavaScript().parser(contenu, nomFichier);
						case SQL -> new ParseurSql().parser(contenu, nomFichier);
						case PHP -> new ParseurPhp().parser(contenu, nomFichier);
						case JSON -> new ParseurJson().parser(contenu, nomFichier);
						default -> throw new IllegalStateException("Langage non pris en charge : " + langage);
					};
				}

				for (Classe classe : classes)
				{
					classe.setLangage(langage);
					if (hashMapClasses.containsKey(classe.getNom()))
						throw new IllegalArgumentException("Nom de classe ou table en double : " + classe.getNom()
							+ " (fichier " + fichier + "). Renommez les éléments avant l'ouverture.");
					hashMapClasses.put(classe.getNom(), classe);
				}
				lstNomFichier.add(fichier.getFileName().toString());
			}
			for (Path fichier : fichiers)
				if (Langage.depuisNomFichier(fichier.getFileName().toString()) == Langage.SQL)
					new ParseurSql().lireAlterSql(Files.readString(fichier, StandardCharsets.UTF_8), hashMapClasses);
			if (hashMapClasses.isEmpty())
				throw new IllegalArgumentException("Aucune classe ou table reconnue dans : " + chemin);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Erreur lors de la lecture de " + chemin + " : " + e.getMessage(), e);
		}
		return hashMapClasses;
	}

	public HashMap<String, Classe> getHashMapClasses() { return hashMapClasses; }
	public ArrayList<String> getLstNomFichier() { return lstNomFichier; }
}
