/* 
package metier.lecture;

import java.nio.file.*;
import java.util.*;


public class VerificationStructureProjet 

    private List<String> erreurs;

    public VerificationStructureProjet()
    {
        this.erreurs = new ArrayList<>();
    }

    public void verifierStructure() 
    {

        // Dossiers obligatoires
        verifierDossier("src", erreurs);
        verifierDossier("bin", erreurs);
        verifierDossier("data", erreurs);
        verifierDossier("donnees", erreurs);
        verifierDossier("donnees/sauvegardes", erreurs);
        verifierDossier("sauvegardes", erreurs);
        verifierDossier("sauvegardes/dossiers", erreurs);

        // Fichiers obligatoires
        verifierFichier("donnees/projets.xml", erreurs);
        verifierFichier("compile.list", erreurs);

        // Script de lancement
        if (!Files.exists(Path.of("run.sh")) && !Files.exists(Path.of("run.bat"))) {
            erreurs.add("Script de lancement manquant (run.sh ou run.bat)");
        }

        // Résultat
        if (!erreurs.isEmpty()) {
            System.err.println("=================================");
            System.err.println(" ERREUR : STRUCTURE PROJET INVALIDE");
            System.err.println("=================================");
            erreurs.forEach(e -> System.err.println("❌ " + e));
            System.exit(1);
        }
    }

    private void verifierDossier(String chemin, List<String> erreurs) 
    {
        if (!Files.isDirectory(Path.of(chemin))) 
        {
            erreurs.add("Dossier manquant : " + chemin);
        }
    }

    private void verifierFichier(String chemin, List<String> erreurs) 
    {
        if (!Files.isRegularFile(Path.of(chemin))) 
        {
            erreurs.add("Fichier manquant : " + chemin);
        }
    }
}
 
*/