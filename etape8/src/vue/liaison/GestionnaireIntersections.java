package vue.liaison;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestion des intersections entre liaisons
 */
public class GestionnaireIntersections
{	
	public long longueurSegmentsCommuns(List<Point> chemin1, List<Point> chemin2)
	{
		long longueur = 0;
		for (int i = 1; i < chemin1.size(); i++)
			for (int j = 1; j < chemin2.size(); j++)
			{
				Point a = chemin1.get(i - 1), b = chemin1.get(i);
				Point c = chemin2.get(j - 1), d = chemin2.get(j);
				if (a.y == b.y && c.y == d.y && a.y == c.y)
					longueur += Math.max(0, Math.min(Math.max(a.x, b.x), Math.max(c.x, d.x))
						- Math.max(Math.min(a.x, b.x), Math.min(c.x, d.x)));
				else if (a.x == b.x && c.x == d.x && a.x == c.x)
					longueur += Math.max(0, Math.min(Math.max(a.y, b.y), Math.max(c.y, d.y))
						- Math.max(Math.min(a.y, b.y), Math.min(c.y, d.y)));
			}
		return longueur;
	}

	public int nombreCroisements(List<Point> chemin1, List<Point> chemin2)
	{
		java.util.Set<Point> croisements = new java.util.HashSet<>();
		for (int i = 1; i < chemin1.size(); i++)
			for (int j = 1; j < chemin2.size(); j++)
			{
				Point a = chemin1.get(i - 1), b = chemin1.get(i);
				Point c = chemin2.get(j - 1), d = chemin2.get(j);
				if (a.y == b.y && c.x == d.x && a.x != b.x && c.y != d.y &&
					c.x >= Math.min(a.x, b.x) && c.x <= Math.max(a.x, b.x) &&
					a.y >= Math.min(c.y, d.y) && a.y <= Math.max(c.y, d.y))
					croisements.add(new Point(c.x, a.y));
				else if (a.x == b.x && c.y == d.y && a.y != b.y && c.x != d.x &&
					a.x >= Math.min(c.x, d.x) && a.x <= Math.max(c.x, d.x) &&
					c.y >= Math.min(a.y, b.y) && c.y <= Math.max(a.y, b.y))
					croisements.add(new Point(a.x, c.y));
			}
		return croisements.size();
	}

	/**
	 * Détecte si deux segments orthogonaux se croisent
	 */
	public Point getIntersectionSegment(Point a1, Point a2, Point b1, Point b2)
	{
		boolean a_horizontal = (a1.y == a2.y);
		boolean a_vertical   = (a1.x == a2.x);
		boolean b_horizontal = (b1.y == b2.y);
		boolean b_vertical   = (b1.x == b2.x);
		
		if (!(a_horizontal && b_vertical) && !(a_vertical && b_horizontal))
			return null;
		
		Point h1, h2, v1, v2;
		if (a_horizontal)
		{
			h1 = a1; h2 = a2;
			v1 = b1; v2 = b2;
		}
		else
		{
			h1 = b1; h2 = b2;
			v1 = a1; v2 = a2;
		}
		
		int ix = v1.x;
		int iy = h1.y;
		
		int minHx = Math.min(h1.x, h2.x);
		int maxHx = Math.max(h1.x, h2.x);
		int minVy = Math.min(v1.y, v2.y);
		int maxVy = Math.max(v1.y, v2.y);
		
		if (ix >= minHx + 1 && ix <= maxHx - 1 && iy >= minVy + 1 && iy <= maxVy - 1)
			return new Point(ix, iy);
		
		return null;
	}
	
	/**
	 * Vérifie si deux chemins partagent des segments communs
	 */
	public boolean cheminsPartagentSegments(List<Point> chemin1, List<Point> chemin2)
	{
		if (chemin1.size() < 2 || chemin2.size() < 2) return false;
		
		for (int i = 0; i < chemin1.size() - 1; i++)
		{
			Point p1a = chemin1.get(i	);
			Point p1b = chemin1.get(i + 1);
			
			for (int j = 0; j < chemin2.size() - 1; j++)
			{
				Point p2a = chemin2.get(j	);
				Point p2b = chemin2.get(j + 1);
				
				if (segmentsSeChevauchent(p1a, p1b, p2a, p2b))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Vérifie si deux segments sont colinéaires et se chevauchent
	 */
	private boolean segmentsSeChevauchent(Point a1, Point a2, Point b1, Point b2)
	{
		if (a1.y == a2.y && b1.y == b2.y && a1.y == b1.y)
		{
			int minA = Math.min(a1.x, a2.x);
			int maxA = Math.max(a1.x, a2.x);
			int minB = Math.min(b1.x, b2.x);
			int maxB = Math.max(b1.x, b2.x);
			return maxA >= minB && maxB >= minA;
		}
		
		if (a1.x == a2.x && b1.x == b2.x && a1.x == b1.x)
		{
			int minA = Math.min(a1.y, a2.y);
			int maxA = Math.max(a1.y, a2.y);
			int minB = Math.min(b1.y, b2.y);
			int maxB = Math.max(b1.y, b2.y);
			return maxA >= minB && maxB >= minA;
		}
		
		return false;
	}
}