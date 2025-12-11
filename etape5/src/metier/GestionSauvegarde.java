package metier;

import vue.BlocClasse;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class GestionSauvegarde {

    private Map<String, int[]> hashCoordonnees;
    private String cheminDossier;

    public GestionSauvegarde() {
        this.hashCoordonnees = new HashMap<String, int[]>();
    }

    // dossierFichSelec cette variable prend le dossier selctioner et le fichier
    // corespondant au fichier enregister por ces coordoner sauvegarder avant de
    // fermer le programe
    public void lecture(String dossierFichSelec) {
        // Ces ligne permet de s'adapter a n'importe quelle environement a partir du
        // chemin absolue
        String basePath = System.getProperty("user.dir");
        String cheminDossier = basePath + "/etape5/donnees/sauvegardes/" + dossierFichSelec;

        try (BufferedReader reader = new BufferedReader(new FileReader(cheminDossier))) {
            String ligne;

            while ((ligne = reader.readLine()) != null) {
                if (ligne.contains("/")) {
                    this.cheminDossier = ligne.trim();
                    continue;
                } else {
                    // Trouver la position des espaces
                    int premierEspace = ligne.indexOf(' ');
                    int deuxiemeEspace = ligne.indexOf(' ', premierEspace + 1);

                    // Extraire les morceaux
                    String nomClass = ligne.substring(0, premierEspace);
                    String xStr = ligne.substring(premierEspace + 1, deuxiemeEspace).trim();
                    String yStr = ligne.substring(deuxiemeEspace + 1).trim();

                    int x = Integer.parseInt(xStr);
                    int y = Integer.parseInt(yStr);

                    this.hashCoordonnees.put(nomClass, new int[] { x, y });
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void sauvegardeFichier() {

    }

    public Map<String, int[]> gethashCoordonnees() {
        return this.hashCoordonnees;
    }

    public String getCheminDossier() {
        return this.cheminDossier;
    }

    public void sauvegarderClasses(List<BlocClasse> blocClasses, String cheminProjet) {

    }
}
