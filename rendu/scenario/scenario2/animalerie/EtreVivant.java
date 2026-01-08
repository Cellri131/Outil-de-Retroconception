public class EtreVivant
{
    protected boolean vivant;

    public EtreVivant()
    {
        this.vivant = true;
    }

    public boolean estVivant()
    {
        return vivant;
    }

    public void mourir()
    {
        this.vivant = false;
    }
}
