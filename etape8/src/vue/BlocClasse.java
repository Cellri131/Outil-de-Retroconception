package vue;

import java.util.UUID;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;
import metier.objet.Langage;

/**
* L'affichage des blocs de Classe dans le diagramme UML
* @author Jules
*/
public class BlocClasse 
{
	//--------------------------//
	//        ATTRIBUTS         //
	//--------------------------//

	private final UUID 	id					;

	private String       nom                ;
	private Langage      langage            = Langage.JAVA;
	private boolean      estInterface       ;
	private boolean      estSelectionne     ;
	private List<String> attributs          ;
	private List<String> methodes           ;

	private int          x                  ;
	private int          y                  ;
	private int          largeur            ;
	private int          hauteur            ;
	private int          hauteurCalculee    ;
	private boolean      affichagePleinEcran;

	// Constantes 
	private static final int PADDING           = 10;
	private static final int HAUTEUR_ENTETE    = 30;
	private static final int HAUTEUR_LIGNE     = 20;
	private static final int MAX_ATTRIBUTS     = 3 ;
	private static final int MAX_METHODES      = 2 ;
	private static final int MAX_PARAMETRES    = 2 ;
	private static final Color COULEUR_FOND    = new Color(230, 240, 250);
	private static final Color COULEUR_BORDURE = new Color(0, 0, 0);
	private static final Color COULEUR_ENTETE  = new Color(100, 150, 200);
	private static final Font POLICE_ENTETE = new Font("Arial", Font.BOLD, 12);
	private static final Font POLICE_MEMBRE = new Font("Arial", Font.PLAIN, 9);
	private static final FontRenderContext CONTEXTE_POLICE = new FontRenderContext(null, true, true);

	//-------------------------//
	//      CONSTRUCTEUR       //
	//-------------------------//
	
	public BlocClasse(String nom, int x, int y) 
	{
		this.id					 = UUID.randomUUID();

		this.nom                 = nom  ;
		this.x                   = x    ;
		this.y                   = y    ;
		this.largeur             = 200  ;
		this.hauteur             = 150  ;
		this.estInterface        = false;
		this.estSelectionne      = false;
		this.affichagePleinEcran = false;

		this.attributs = new ArrayList<String>();
		this.methodes  = new ArrayList<String>();
	}

	//----------------------//
	//      METHODES        //
	//----------------------//

	/**
	 * Formate le texte d'une méthode en limitant les paramètres en mode condensé
	 * @param methodeStr Chaîne de méthode au format: "visibilite nomMethode(param1, param2, ...) : typeRetour"
	 * @return Chaîne formatée avec max 2 paramètres si mode condensé
	 */
	private String formatMethode(String methodeStr) 
	{
		// En mode condensé, limiter les paramètres à 2
		if (!affichagePleinEcran)
		{
			int startParent = methodeStr.indexOf("(");
			int endParent = methodeStr.indexOf(")");
			
			if (startParent > 0 && endParent > startParent)
			{
				String avant      = methodeStr.substring(0, startParent + 1);
				String apres      = methodeStr.substring(endParent);
				String parametres = methodeStr.substring(startParent + 1, endParent);
				
				if (!parametres.trim().isEmpty())
				{
					String[] params = parametres.split(",");
					
					if (params.length > MAX_PARAMETRES)
					{
						StringBuilder result = new StringBuilder(avant);

						for (int i = 0; i < MAX_PARAMETRES; i++)
						{
							result.append(params[i].trim());
							if (i < MAX_PARAMETRES - 1) {
								result.append(", ");
							}
						}
						result.append(", ...");
						result.append(apres);
						return result.toString();
					}
				}
			}
		}
		
		return methodeStr;
	}

	/**
	 * Obtient la liste des attributs à afficher selon le mode d'affichage
	 */
	private List<String> getAttributsAffichage() 
	{
		if (affichagePleinEcran) {
			return attributs;
		}
		
		List<String> result = new ArrayList<>();
		
		if (attributs.size() <= MAX_ATTRIBUTS) {
			return attributs;
		}
		
		for (int i = 0; i < MAX_ATTRIBUTS; i++) {
			result.add(attributs.get(i));
		}
		result.add("...");
		
		return result;
	}

	/**
	 * Obtient la liste des méthodes à afficher selon le mode d'affichage
	 */
	private List<String> getMethodesAffichage() 
	{
		if (affichagePleinEcran)
			return methodes;
		
		List<String> result = new ArrayList<String>();
		
		if (methodes.size() <= MAX_METHODES)
		{
			// Formater les méthodes pour limiter les paramètres
			for (String methode : methodes)
				result.add(formatMethode(methode));

			return result;
		}
		
		for (int i = 0; i < MAX_METHODES; i++)
			result.add(formatMethode(methodes.get(i)));

		result.add("...");
		
		return result;
	}

	public void dessiner(Graphics2D g, boolean afficherAttributs, boolean afficherMethodes)
	{
		largeur = calculerLargeur();
		hauteurCalculee = calculerHauteur();
		
		g.setColor(COULEUR_FOND);
		g.fillRect(x, y, largeur, hauteurCalculee);

		g.setColor(COULEUR_ENTETE);
		g.fillRect(x, y, largeur, HAUTEUR_ENTETE); 

		g.setColor(Color.WHITE);
		g.setFont(POLICE_ENTETE);
		FontMetrics fm = g.getFontMetrics();
		
		int textY = y + HAUTEUR_ENTETE - (HAUTEUR_ENTETE - fm.getAscent()) / 2;
		
		// Si c'est une interface, afficher <<interface>> au-dessus du nom
		if (estInterface)
		{
			g.setFont(new Font("Arial", Font.ITALIC, 10));
			FontMetrics fmSmall = g.getFontMetrics();
			String interfaceLabel = "<<interface>>";
			int textXInterface = x + (largeur - fmSmall.stringWidth(interfaceLabel)) / 2;
			g.drawString(interfaceLabel, textXInterface, textY - 8);
			
			// Afficher le nom en-dessous
			g.setFont(new Font("Arial", Font.BOLD, 12));
			fm = g.getFontMetrics();
			textY = y + HAUTEUR_ENTETE - (HAUTEUR_ENTETE - fm.getAscent()) / 2 + 5;
		}
		
		int textX = x + (largeur - fm.stringWidth(nom)) / 2;
		g.drawString(nom, textX, textY);

		int yActuel = y + HAUTEUR_ENTETE + PADDING;

		// Utiliser les méthodes de formatage pour l'affichage
		List<String> attributsAffichage = getAttributsAffichage();
		List<String> methodesAffichage  = getMethodesAffichage ();

		if (!attributsAffichage.isEmpty())
		{
			g.setColor(Color.BLACK);
			g.setFont(POLICE_MEMBRE);
			FontMetrics fmAttributs = g.getFontMetrics();

			if (afficherAttributs)
			{
				for (String att : attributsAffichage) 
				{
					// pour gérer les soulignement
					// Vérifier si l'attribut contient un code ANSI de soulignement
					boolean isSouligne = att.contains("\u001B[4m");
					
					// Retirer les codes ANSI pour l'affichage
					String displayText = att.replace("\u001B[4m", "").replace("\u001B[0m", "");
					
					
					g.drawString(displayText, x + PADDING, yActuel);
					
					// Si l'attribut doit être souligné, tracer une ligne sous le texte
					if (isSouligne)
					{
						int textWidth = fmAttributs.stringWidth(displayText);
						int underlineY = yActuel + 2;
						g.drawLine(x + PADDING, underlineY, x + PADDING + textWidth, underlineY);
					}
					
					yActuel += HAUTEUR_LIGNE;
				}	
			}
			else 
			{
				g.drawString("...", x + PADDING, yActuel);
				yActuel += HAUTEUR_LIGNE;
			}
		}

		if (!attributsAffichage.isEmpty() && !methodesAffichage.isEmpty()) 
		{
			g.setColor(COULEUR_BORDURE);
			g.drawLine(x + PADDING, yActuel - 5, x + largeur - PADDING, yActuel - 5);
			yActuel += 5;
		}

		if (!methodesAffichage.isEmpty()) 
		{
			g.setColor(Color.BLACK);
			g.setFont(POLICE_MEMBRE);

			if (afficherMethodes)
			{
				for (String met : methodesAffichage) 
				{
					// Afficher sur plusieurs lignes si la méthode contient des retours à la ligne
					String[] lines = met.split("\n");
					for (String line : lines)
					{
						g.drawString(line, x + PADDING, yActuel);
						yActuel += HAUTEUR_LIGNE;
					}

				}
			}
			else 
			{
				g.drawString("...", x + PADDING, yActuel);
				yActuel += HAUTEUR_LIGNE;
			}
		}
		g.setColor(couleurLangage(langage));
		g.setStroke(new BasicStroke(estSelectionne ? 4 : 2));
		g.drawRect(x, y, largeur, hauteurCalculee);
		g.setStroke(new BasicStroke(1));
	}

	public static Color couleurLangage(Langage langage)
	{
		switch (langage)
		{
			case JAVA: return new Color(234, 45, 45);
			case CSHARP: return new Color(103, 33, 122);
			case PYTHON: return new Color(0, 118, 188);
			case JAVASCRIPT: return new Color(253, 205, 0);
			case SQL: return new Color(220, 99, 133);
			case PHP: return new Color(153, 121, 191);
			case JSON: return new Color(199, 199, 199);
			default: throw new IllegalStateException("Langage non reconnu : " + langage);
		}
	}

	private int calculerHauteur() 
	{
		List<String> attributsAffichage = getAttributsAffichage();
		List<String> methodesAffichage  = getMethodesAffichage ();

		int h = HAUTEUR_ENTETE + PADDING;

		// Ajouter de l'espace pour <<interface>> si nécessaire
		if (estInterface)
			h += 10;

		h += attributsAffichage.size() * HAUTEUR_LIGNE;

		if (!attributsAffichage.isEmpty() && !methodesAffichage.isEmpty()) 
			h += 10;

		// Calculer la hauteur des méthodes en tenant compte des retours à la ligne
		for (String methode : methodesAffichage)
		{
			int nbLignes = 1;

			for (char c : methode.toCharArray())
			{
				if (c == '\n') nbLignes++;
			}

			h += nbLignes * HAUTEUR_LIGNE;
		}
		
		h += PADDING;

		return Math.max(h, 80);
	}

	private int calculerLargeur()
	{
		double max = POLICE_ENTETE.getStringBounds(nom, CONTEXTE_POLICE).getWidth();
		for (String texte : getAttributsAffichage())
			for (String ligne : texte.replace("\u001B[4m", "").replace("\u001B[0m", "").split("\n"))
				max = Math.max(max, POLICE_MEMBRE.getStringBounds(ligne, CONTEXTE_POLICE).getWidth());
		for (String texte : getMethodesAffichage())
			for (String ligne : texte.split("\n"))
				max = Math.max(max, POLICE_MEMBRE.getStringBounds(ligne, CONTEXTE_POLICE).getWidth());
		return Math.max(200, (int) Math.ceil(max) + 2 * PADDING + 8);
	}

	// Vérifie si le point donné est dans le bloc
	public boolean contient(int px, int py) 
	{
		int hauteurCalculee = calculerHauteur();

		return px >= x                    &&
			   px <= x + calculerLargeur() &&
			   py >= y                    &&
			   py <= y + hauteurCalculee;
	}

	// Vérifie si un rectangle de texte chevauchent le bloc
	public boolean chevaucheTexte(int textX, int textY, int textWidth, int textHeight) 
	{
		int hauteurCalculee = calculerHauteur();
		
		// Vérifier si le rectangle du texte chevauche le rectangle du bloc
		return !(textX     + textWidth < x   || 
				 textX > x + calculerLargeur() ||
				 textY > y + hauteurCalculee || 
				 textY     - textHeight < y  );
	}

	public void deplacer(int dx, int dy) 
	{
		this.x += dx;
		this.y += dy;
	}

	// Getters et Setters
	public UUID         getId             () {return this.id;}
	public int          getX              () { return x              ; }
	public int          getY              () { return y              ; }
	public int          getLargeur        () { return calculerLargeur(); }
	public int          getHauteur        () { return hauteur        ; }
	public int          getHauteurCalculee() { return calculerHauteur(); }
	public String       getNom            () { return nom            ; }
	public Langage      getLangage        () { return langage        ; }
	public List<String> getAttributs      () { return attributs      ; }
	public List<String> getMethodes       () { return methodes       ; }
	

	public boolean estSelectionne       () { return estSelectionne     ; }
	public boolean estInterface         () { return estInterface       ; }
	public boolean isAffichagePleinEcran() { return affichagePleinEcran; }

	public void setX(int x) { this.x = x; }
	public void setLangage(Langage langage) { this.langage = java.util.Objects.requireNonNull(langage); }
	public void setY(int y) { this.y = y; }

	public void setInterface          (boolean estInterface       ) { this.estInterface        = estInterface       ; }
	public void setSelectionne        (boolean selectionne        ) { this.estSelectionne      = selectionne        ; }
	public void setAttributsAffichage (List<String> attributs     ) { this.attributs           = attributs          ; }
	public void setMethodesAffichage  (List<String> methodes      ) { this.methodes            = methodes           ; }
	public void setAffichagePleinEcran(boolean affichagePleinEcran) { this.affichagePleinEcran = affichagePleinEcran; }
}