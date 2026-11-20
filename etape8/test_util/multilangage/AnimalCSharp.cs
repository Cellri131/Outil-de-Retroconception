public class AnimalCSharp {
    private string nom;
    private int age;

    public AnimalCSharp(string nom, int age) {
        this.nom = nom;
        this.age = age;
    }

    public string GetNom() {
        return nom;
    }

    public int GetAge() {
        return age;
    }
}
