package metier.lecture;
import metier.objet.*;

/**
 * Classe utilitaire pour manipuler les types Java.
 */
public class UtilitaireType
{
	/**
	 * Nettoie le type pour retirer List<>, Set<> ou [].
	 * @param type Le type à nettoyer
	 * @return     Le type nettoyé
	 */
	public static String nettoyerType(String type)
	{
		type = type.trim().replaceAll("\\s+", "");
		if (type.startsWith("?")) type = type.substring(1);
		if (type.endsWith("?")) type = type.substring(0, type.length() - 1);
		if (type.endsWith("[]")) type = type.substring(0, type.length() - 2);
		else if (type.matches("(?:List|Set|IList|ICollection|IEnumerable|HashSet)<[^<>]+>"))
			type = type.substring(type.indexOf('<') + 1, type.length() - 1);
		if (type.startsWith("Map<")  && type.endsWith(">")) 
		{
			// Pour les Map, extraire le type de la valeur
			String contenu = type.substring(4, type.length() - 1);
			int virgule = contenu.lastIndexOf(',');

			if (virgule != -1) 
			{
				type = contenu.substring(virgule + 1).trim();
			}
		}
		int point = type.lastIndexOf('.');
		return point >= 0 ? type.substring(point + 1) : type;
	}

	/**
	 * Retourne true si le type peut contenir plusieurs instances (tableau ou collection).
	 * @param type Le type à vérifier
	 * @return true si multi-instance, false sinon
	 */
	public static boolean estMultiInstance(String type)
	{
		type = type.trim().replaceAll("\\s+", "");
		if (type.endsWith("?")) type = type.substring(0, type.length() - 1);
		return type.endsWith("[]")     || type.startsWith("List<")    || 
		       type.startsWith("Set<") || type.startsWith("Map<") ||
		       type.startsWith("IList<") || type.startsWith("ICollection<") ||
		       type.startsWith("IEnumerable<") || type.startsWith("HashSet<");
	}
}
