import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class LectureCoord {

    private Map<String, int[]> hashCoordonnees;
    

    public LectureCoord() 
    {
        this.hashCoordonnees = new HashMap<>();
    }

    //dossierFichSelec cette variable prend le dossier selctioner et le fichier
    //corespondant au fichier enregister por ces coordoner sauvegarder avant de fermer le programe
    public void lecture(String dossierFichSelec)
    {
        String cheminDossier = "donnees/sauvegardes/" + dossierFichSelec;

        try (BufferedReader reader = new BufferedReader(new FileReader(cheminDossier))) 
        {
            String ligne;
            String nomClass = "";

            while ((ligne = reader.readLine()) != null) 
            {
                if(ligne.contains("/"))
                {
                    nomClass = ligne.substring(ligne.lastIndexOf("/") + 1);

                    //retire le .java si ya à la fin
                    if (nomClass.endsWith(".java"))
                        nomClass = nomClass.substring(0, nomClass.length() - 5);

                }

                // Trouver la position des espaces
                int premierEspace = ligne.indexOf(' ');
                int deuxiemeEspace = ligne.indexOf(' ', premierEspace + 1);

                // Extraire les morceaux
                String nomMethode  = ligne.substring(0, premierEspace);
                String xStr        = ligne.substring(premierEspace + 1, deuxiemeEspace);
                String yStr        = ligne.substring(deuxiemeEspace + 1);

                int x = Integer.parseInt(xStr);
                int y = Integer.parseInt(yStr);

                this.hashCoordonnees.put(nomMethode, new int[]{x, y});
                
            }

        } catch (Exception e) 
        {
            e.printStackTrace();
        }

    }


    /*public Map<String, int[]> getCoordonnees() 
    {
        return coordonnees;
    }

    public void ajouterCoord(String nom, int x, int y) 
    {
        coordonnees.put(nom, new int[]{x, y});
    }*/

}
