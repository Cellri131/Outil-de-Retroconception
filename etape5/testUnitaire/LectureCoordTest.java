package testUnitaire;

import metier.LectureCoord;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Map;

public class LectureCoordTest 
{

    @Test
    public void testLecture(@TempDir Path tempDir) throws Exception 
    {

        // --- 1. Création d'un faux fichier de sauvegarde ---
        File dossierSauvegarde = tempDir.toFile();
        File fichierTest = new File(dossierSauvegarde, "testCoord.txt");

        try (FileWriter writer = new FileWriter(fichierTest)) 
        {
            writer.write("src/com/exemple/MaClasse.java\n");
            writer.write("methodeA 10 20\n");
            writer.write("methodeB 30 40\n");
        }

        // --- 2. Instanciation de ta classe ---
        LectureCoord lectureCoord = new LectureCoord();

        // On passe le chemin relatif utilisé dans la classe
        // mais ici on bypass "donnees/sauvegardes/"
        // donc on force un chemin réel :
        lectureCoord.lecture(fichierTest.getAbsolutePath());

        // --- 3. Vérifications ---
        Map<String, int[]> resultat = lectureCoord.gethashCoordonnees();

        assertEquals(2, resultat.size(), "La map doit contenir 2 éléments");

        assertArrayEquals(new int[]{10, 20}, resultat.get("methodeA"));
        assertArrayEquals(new int[]{30, 40}, resultat.get("methodeB"));
    }
}
