package vue.liaison;

import vue.BlocClasse;
import java.awt.Point;

/**
 * Gestion des points d'ancrage et positions sur les blocs de classes.
 * Permet de calculer les points pour les liaisons et les multiplicitées.
 */
public class GestionnaireAncrage
{
	/** Rayon autour du point d'ancrage pour le clic */
	private static final int ANCHOR_RADIUS = 10;


	/**
	 * Renvoie un point sur un côté d'un bloc.
	 * @param bloc Le bloc cible
	 * @param side Côté : 0=DROITE, 1=BAS, 2=GAUCHE, 3=HAUT
	 * @param posRel Position relative sur le côté (0.0 à 1.0)
	 * @return Point exact sur le côté
	 */
	public static Point getPointOnSide(BlocClasse bloc, int side, double posRel)
	{
		final int x = bloc.getX              ();
		final int y = bloc.getY              ();
		final int w = bloc.getLargeur        ();
		final int h = bloc.getHauteurCalculee();

		posRel = Math.max(0.0, Math.min(1.0, posRel));

		switch (side)
		{
			case 0: return new Point(x + w,                    y + (int)(h * posRel));
			case 1: return new Point(x + (int)(w * posRel),    y + h               );
			case 2: return new Point(x,                        y + (int)(h * posRel));
			case 3: return new Point(x + (int)(w * posRel),    y                   );
		}

		return new Point(x, y);
	}


	/**
	 * Renvoie le côté le plus proche d'un point donné.
	 * @param mouse Position de la souris
	 * @param bloc Bloc cible
	 * @return côté le plus proche (0=DROITE, 1=BAS, 2=GAUCHE, 3=HAUT)
	 */
	public static int getClosestSide(Point mouse, BlocClasse bloc)
	{
		int x = bloc.getX              ();
		int y = bloc.getY              ();
		int w = bloc.getLargeur        ();
		int h = bloc.getHauteurCalculee();

		int distDroite  = Math.abs(mouse.x - (x + w));
		int distGauche  = Math.abs(mouse.x - x      );
		int distBas     = Math.abs(mouse.y - (y + h));
		int distHaut    = Math.abs(mouse.y - y      );

		int minDist = Math.min(Math.min(distDroite, distGauche), Math.min(distBas, distHaut));

		if (minDist == distDroite) return 0;
		if (minDist == distBas   ) return 1;
		if (minDist == distGauche) return 2;
		return 3;
	}


	/**
	 * Calcule la position relative d'un point sur un côté du bloc.
	 * @param mouse Position de la souris
	 * @param bloc Bloc cible
	 * @param side Côté
	 * @return position relative normalisée [0.1,0.9]
	 */
	public static double getRelativePosFromMouse(Point mouse, BlocClasse bloc, int side)
	{
		int x = bloc.getX              ();
		int y = bloc.getY              ();
		int w = bloc.getLargeur        ();
		int h = bloc.getHauteurCalculee();

		double posRel = 0.5;

		switch (side)
		{
			case 0:
			case 2:
				posRel = (double)(mouse.y - y) / h;
				break;
			case 1:
			case 3:
				posRel = (double)(mouse.x - x) / w;
				break;
		}

		return Math.max(0.1, Math.min(0.9, posRel));
	}


	/**
	 * Vérifie si la position de la souris est sur un ancrage.
	 * @param ancrage Point d'ancrage
	 * @param mouse Position de la souris
	 * @param zoom Niveau de zoom
	 * @param panX Décalage horizontal
	 * @param panY Décalage vertical
	 * @param w Largeur de la vue
	 * @param h Hauteur de la vue
	 * @return true si la souris est sur l'ancrage
	 */
	public static boolean isOnAnchor(Point ancrage, Point mouse, double zoom, int panX, int panY, int w, int h)
	{
		if (ancrage == null) return false;

		if (zoom == 1.0 && panX == 0 && panY == 0)
			return mouse.distance(ancrage) <= ANCHOR_RADIUS;

		final double hw = w * 0.5;
		final double hh = h * 0.5;

		double dx = mouse.x - (ancrage.x * zoom + hw - hw + panX);
		double dy = mouse.y - (ancrage.y * zoom + hh - hh + panY);

		return (dx * dx + dy * dy) <= ANCHOR_RADIUS * ANCHOR_RADIUS;
	}


	/**
	 * Calcule la position de la multiplicité par rapport à un point.
	 * @param a Point de référence
	 * @param s Côté
	 * @param tw Largeur du texte
	 * @param th Hauteur du texte
	 * @return position calculée
	 */
	public static Point calculateMultiplicityPosition(Point a, int s, int tw, int th)
	{
		int[][] o = {{25, -5}, {5, 15}, {-tw - 25, -5}, {5, -25}};
		return s < 0 || s > 3 ? a : new Point(a.x + o[s][0], a.y + o[s][1]);
	}


	/**
	 * Détermine la priorité de centrage d'une position relative.
	 * @param pos Position relative
	 * @return priorité (0=centre exact, 1=quart, 2=octants, 1000=autre)
	 */
	public static int calculateCenterPriority(double pos)
	{
		final double TOLERANCE = 0.01;

		if (Math.abs(pos - 0.5  ) < TOLERANCE) return 0;
		if (Math.abs(pos - 0.25 ) < TOLERANCE || Math.abs(pos - 0.75 ) < TOLERANCE) return 1;
		if (Math.abs(pos - 0.125) < TOLERANCE || Math.abs(pos - 0.375) < TOLERANCE ||
			Math.abs(pos - 0.625) < TOLERANCE || Math.abs(pos - 0.875) < TOLERANCE) return 2;

		return 1000;
	}


	/**
	 * Calcule la position du rôle par rapport au bloc et à la multiplicité.
	 * Ne modifie pas le comportement gauche/droite.
	 * @param a Point de référence (ancrage)
	 * @param side Côté (0=DROITE, 1=BAS, 2=GAUCHE, 3=HAUT)
	 * @param tw Largeur du texte du rôle
	 * @param th Hauteur du texte du rôle
	 * @param multHeight Hauteur de la multiplicité
	 * @return position calculée pour le rôle
	 */
	public static Point calculateRolePosition(Point a, int side, int tw, int th, int multHeight)
	{
		final int GAP    = 6;   // écart vertical entre multiplicité et rôle
		final int OFFSET = 25;  // distance horizontale du bloc

		int x = a.x;
		int y = a.y;

		switch (side)
		{
			case 0: // DROITE (NE PAS TOUCHER)
				x += OFFSET;
				y += multHeight + GAP;
				break;

			case 2: // GAUCHE (NE PAS TOUCHER)
				x -= OFFSET + tw;
				y += multHeight + GAP;
				break;

			case 3: // HAUT (CORRIGÉ)
				x -= OFFSET + tw;
				y -= OFFSET;   // ⬅ même base que la multiplicité
				break;

			case 1: // BAS (CORRIGÉ)
				x -= OFFSET + tw;
				y += OFFSET;   // ⬅ même base que la multiplicité
				break;
		}

		return new Point(x, y);
	}
}
