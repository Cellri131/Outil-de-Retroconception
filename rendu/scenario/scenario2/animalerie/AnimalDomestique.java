public class AnimalDomestique extends Animal
{
    private boolean vacciné;

    public AnimalDomestique(String nom, int age, boolean vacciné)
    {
        super(nom, age);
        this.vacciné = vacciné;
    }

    public boolean estVacciné()
    {
        return vacciné;
    }

    public void setVacciné(boolean vacciné)
    {
        this.vacciné = vacciné;
    }
}
