package metier.objet;

import java.util.ArrayList;

/**
 * Représente une classe Java.
 *
 * Contient ses attributs, méthodes, type (class, interface, enum, record).
 */
public class Classe 
{
	private String  nom          ;
	private String  type		 ;
	private String  classeParente;
	private boolean isHeritage   ;
	private String  nomInterface ;
	private Langage langage = Langage.JAVA;
	public static class CleEtrangere
	{
		public final String table;
		public final String colonnesOrigine;
		public final String colonnesDestination;

		public CleEtrangere(String table, String colonnesOrigine, String colonnesDestination)
		{
			this.table = table;
			this.colonnesOrigine = colonnesOrigine;
			this.colonnesDestination = colonnesDestination;
		}
	}

	private ArrayList<CleEtrangere> clesEtrangeres = new ArrayList<>();

	private ArrayList<Attribut> lstAttribut;
	private ArrayList<Methode > lstMethode ;

	/**
	 * Constructeur complet d'une classe analysée.
	 *
	 * @param nom           nom de la classe
	 * @param classeParente nom de la classe héritée (si applicable)
	 * @param type    type de classe (class, abstract, ect)
	 * @param isHeritage    true si la classe hérite d'une autre
	 * @param nomInterface  nom de l'interface implémentée
	 * @param lstAttribut   liste d'attributs trouvés dans la classe
	 * @param lstMethode    liste de méthodes trouvées dans la classe
	 */
	public Classe(String              nom          ,
	              String              classeParente,
	              String              type   	   ,
	              boolean             isHeritage   ,
	              String              nomInterface ,
	              ArrayList<Attribut> lstAttribut  ,
	              ArrayList<Methode>  lstMethode     )
	{
		this.nom           = nom          ;
		this.classeParente = classeParente;
		this.type          = type         ;
		this.isHeritage    = isHeritage   ;
		this.nomInterface  = nomInterface ;
		this.lstAttribut   = lstAttribut  ;
		this.lstMethode    = lstMethode   ;
	}

	public String              getNom          () { return this.nom          ; }
	public Langage             getLangage      () { return this.langage      ; }
	public ArrayList<CleEtrangere> getClesEtrangeres() { return this.clesEtrangeres; }
	public void setLangage(Langage langage) { this.langage = java.util.Objects.requireNonNull(langage); }
	public String              getClasseParente() { return this.classeParente; }
	public void setNomInterface(String interfaces) { this.nomInterface = interfaces; }
	public boolean             getIsHeritage   () { return this.isHeritage   ; }
	public String              getTypeClasse   () { return this.type   		 ; }
	public String              getNomInterface () { return this.nomInterface ; }
	public ArrayList<Attribut> getLstAttribut  () { return this.lstAttribut  ; }
	public ArrayList<Methode > getLstMethode   () { return this.lstMethode   ; }

	public boolean isEnum     () { return this.type.contains("enum"     ); }
	public boolean isAbstract () { return this.type.contains("abstract" ); }
	public boolean isInterface() { return this.type.contains("interface"); }
	public boolean isRecord   () { return this.type.contains("record"   ); }
}