public interface IEmpruntable {
    void emprunter(String emprunteur);
    void retourner();
    boolean estDisponible();
    String getEmprunteur();
}
