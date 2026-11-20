package vue;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SpinnerNumberModel;
import metier.lecture.AnalyseurFonctionnement.Graphe;
import metier.lecture.AnalyseurFonctionnement.Lien;
import metier.lecture.AnalyseurFonctionnement.Nature;
import metier.lecture.AnalyseurFonctionnement.Noeud;
import metier.lecture.AnalyseurFonctionnement.Relation;

/**
 * Vue de navigation dans les relations issues de l'analyse statique.
 */
public class PanneauFonctionnement extends JPanel
{
	private static final String TOUT = "Tout le projet";
	private final Graphe graphe;
	private final JComboBox<Object> depart = new JComboBox<>();
	private final JSpinner profondeur = new JSpinner(new SpinnerNumberModel(8, 1, 30, 1));
	private final JTextArea details = new JTextArea();
	private final Diagramme diagramme;
	private record Groupe(Noeud origine, Noeud destination, Relation relation, List<Lien> occurrences) {}
	private record CleGroupe(Noeud origine, Noeud destination, Relation relation) {}

	public PanneauFonctionnement(Graphe graphe)
	{
		super(new BorderLayout());
		this.graphe = graphe;
		diagramme = new Diagramme(this::afficherDetails, this::selectionner);

		depart.addItem(TOUT);
		graphe.noeuds().stream().filter(n -> n.nature() != Nature.EXTERNE)
			.sorted(Comparator.comparing(Noeud::nom).thenComparing(Noeud::id)).forEach(depart::addItem);
		depart.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> liste, Object valeur, int index,
				boolean selection, boolean focus)
			{
				Object texte = valeur instanceof Noeud n ? n.nom() + " — " + n.fichier().getFileName() + ":" + n.ligne() : valeur;
				return super.getListCellRendererComponent(liste, texte, index, selection, focus);
			}
		});
		depart.addActionListener(e -> actualiser());
		profondeur.addChangeListener(e -> actualiser());
		JCheckBox afficherArguments = new JCheckBox("Arguments sur les flèches", false);
		afficherArguments.addActionListener(e -> diagramme.setAfficherArguments(afficherArguments.isSelected()));
		JPanel options = new JPanel();
		options.add(new JLabel("Point de départ :"));
		options.add(depart);
		options.add(new JLabel("Profondeur :"));
		options.add(profondeur);
		options.add(afficherArguments);
		add(options, BorderLayout.NORTH);

		details.setEditable(false);
		details.setLineWrap(true);
		details.setWrapStyleWord(true);
		details.setText("Sélectionnez un bloc ou une liaison pour afficher ses détails.\n"
			+ "Double-cliquez sur un bloc pour suivre ses relations.");
		JScrollPane defilement = new JScrollPane(diagramme);
		diagramme.activerDeplacement(defilement);
		JScrollPane panneauDetails = new JScrollPane(details);
		panneauDetails.setPreferredSize(new Dimension(340, 300));
		JSplitPane separation = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, defilement, panneauDetails);
		separation.setResizeWeight(1.0);
		add(separation, BorderLayout.CENTER);

		JLabel legende = new JLabel("Trait plein : appel / construction / valeur   |   Pointillés : référence   |   "
			+ "Tirets gris : contient   |   Java rouge · C# violet · Python bleu   |   Analyse statique approximative");
		legende.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
		add(legende, BorderLayout.SOUTH);
		Noeud pointEntree = trouverPointEntree(graphe);
		if (pointEntree != null) depart.setSelectedItem(pointEntree);
		actualiser();
	}

	private static boolean estPointEntree(Noeud noeud)
	{
		return noeud.nature() == Nature.METHODE && noeud.nom().matches("(?i).*\\.(awake|main)\\(.*");
	}

	private static Noeud trouverPointEntree(Graphe graphe)
	{
		for (Noeud noeud : graphe.noeuds())
			if (estPointEntree(noeud))
				return graphe.liens().stream()
					.filter(l -> l.relation() == Relation.CONTIENT && l.destination().equals(noeud))
					.map(Lien::origine).findFirst().orElse(noeud);
		return graphe.noeuds().stream()
			.filter(n -> n.nom().endsWith(" (initialisation)") && graphe.liens().stream()
				.anyMatch(l -> l.origine().equals(n) && l.relation() != Relation.CONTIENT))
			.findFirst().orElseGet(() -> graphe.noeuds().stream()
				.filter(n -> n.nature() == Nature.CLASSE && graphe.liens().stream()
					.noneMatch(l -> l.destination().equals(n) && l.relation() != Relation.CONTIENT))
				.findFirst().orElse(null));
	}

	private void selectionner(Noeud noeud)
	{
		if (noeud.nature() != Nature.EXTERNE) depart.setSelectedItem(noeud);
	}

	private void actualiser()
	{
		Object valeur = depart.getSelectedItem();
		diagramme.afficher(graphe, valeur instanceof Noeud ? (Noeud) valeur : null, (Integer) profondeur.getValue());
	}

	private void afficherDetails(Object objet)
	{
		StringBuilder texte = new StringBuilder();
		if (objet instanceof Groupe groupe)
		{
			texte.append(type(groupe.relation())).append('\n')
				.append(groupe.origine().nom()).append(" → ").append(groupe.destination().nom())
				.append("\nOccurrences : ").append(groupe.occurrences().size()).append("\n");
			for (Lien lien : groupe.occurrences())
			{
				texte.append("\nLigne ").append(lien.ligne()).append(" : ");
				if (lien.relation() == Relation.APPEL || lien.relation() == Relation.CONSTRUCTION)
					texte.append("arguments : ").append(lien.arguments().isEmpty() ? "(aucun)" : lien.arguments());
				else if (lien.relation() == Relation.AFFECTATION)
					texte.append("valeur : ").append(lien.arguments());
				else if (lien.relation() == Relation.RETOUR)
					texte.append("expression : ").append(lien.arguments().isEmpty() ? "(aucune)" : lien.arguments());
				else if (!lien.arguments().isEmpty()) texte.append(lien.arguments());
			}
		}
		else if (objet instanceof Noeud noeud)
		{
			texte.append(noeud.nom()).append("\n").append(noeud.nature()).append(" — ").append(noeud.langage());
			if (noeud.fichier() != null) texte.append("\n").append(noeud.fichier()).append(":").append(noeud.ligne());
			texte.append("\n\nRelations sortantes :\n");
			for (Lien lien : graphe.liens())
				if (lien.origine().equals(noeud))
				{
					texte.append("\n• ").append(type(lien.relation())).append(" → ").append(lien.destination().nom());
					if (lien.relation() == Relation.APPEL || lien.relation() == Relation.CONSTRUCTION)
						texte.append("\n  Arguments : ").append(lien.arguments().isEmpty() ? "(aucun)" : lien.arguments());
					else if (!lien.arguments().isEmpty()) texte.append(" (").append(lien.arguments()).append(")");
					texte.append(" — ligne ").append(lien.ligne()).append('\n');
				}
		}
		details.setText(texte.toString());
		details.setCaretPosition(0);
	}

	private static String type(Relation relation)
	{
		return switch (relation)
		{
			case CONTIENT -> "Contient";
			case APPEL -> "Appel";
			case REFERENCE -> "Référence de classe";
			case CONSTRUCTION -> "Construction";
			case AFFECTATION -> "Affectation";
			case RETOUR -> "Retour";
		};
	}

	static Map<Noeud, Integer> organiser(Graphe graphe, Noeud racine, int profondeurMax)
	{
		Map<Noeud, List<Lien>> suivants = new LinkedHashMap<>();
		for (Lien lien : graphe.liens())
			suivants.computeIfAbsent(lien.origine(), k -> new ArrayList<>()).add(lien);
		for (List<Lien> liens : suivants.values())
			liens.sort(Comparator.comparingInt((Lien l) ->
				l.relation() == Relation.CONTIENT && estPointEntree(l.destination()) ? 0 : 1)
				.thenComparingInt(Lien::ligne));
		Map<Noeud, Integer> niveaux = new LinkedHashMap<>();
		List<Noeud> racines = racine == null ? graphe.noeuds() : List.of(racine);
		record Visite(Noeud noeud, int niveau) {}
		for (Noeud depart : racines)
		{
			ArrayDeque<Visite> pile = new ArrayDeque<>();
			pile.push(new Visite(depart, 0));
			while (!pile.isEmpty())
			{
				Visite visite = pile.pop();
				if (niveaux.containsKey(visite.noeud())) continue;
				niveaux.put(visite.noeud(), visite.niveau());
				if (racine != null && visite.niveau() >= profondeurMax) continue;
				List<Lien> liens = suivants.getOrDefault(visite.noeud(), List.of());
				for (int i = liens.size() - 1; i >= 0; i--)
					pile.push(new Visite(liens.get(i).destination(), visite.niveau() + 1));
			}
		}
		return niveaux;
	}

	static boolean lienDescendant(Lien lien, Map<Noeud, Integer> niveaux)
	{
		Integer origine = niveaux.get(lien.origine()), destination = niveaux.get(lien.destination());
		return origine != null && destination != null && destination > origine
			&& (lien.relation() != Relation.CONTIENT || destination == origine + 1);
	}

	private static class Diagramme extends JPanel
	{
		private static final int LARGEUR = 250, HAUTEUR = 62, ECART_X = 390, ECART_Y = 100;
		private final Consumer<Object> details;
		private final Consumer<Noeud> focus;
		private final Map<Noeud, Rectangle> blocs = new LinkedHashMap<>();
		private final Map<Groupe, Shape> courbes = new LinkedHashMap<>();
		private List<Groupe> liens = List.of();
		private boolean afficherArguments;
		private Point dernier;

		Diagramme(Consumer<Object> details, Consumer<Noeud> focus)
		{
			this.details = details;
			this.focus = focus;
			setBackground(Color.WHITE);
			setToolTipText("");
			addMouseListener(new MouseAdapter()
			{
				@Override public void mousePressed(MouseEvent e)
				{
					if (javax.swing.SwingUtilities.isMiddleMouseButton(e)) dernier = e.getPoint();
				}
				@Override public void mouseReleased(MouseEvent e) { dernier = null; }
				@Override public void mouseClicked(MouseEvent e)
				{
					if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
					for (Map.Entry<Noeud, Rectangle> entree : blocs.entrySet())
						if (entree.getValue().contains(e.getPoint()))
						{
							details.accept(entree.getKey());
							if (e.getClickCount() == 2) focus.accept(entree.getKey());
							return;
						}
					for (Map.Entry<Groupe, Shape> entree : courbes.entrySet())
						if (new BasicStroke(9).createStrokedShape(entree.getValue()).contains(e.getPoint()))
						{
							details.accept(entree.getKey());
							return;
						}
				}
			});
		}

		void activerDeplacement(JScrollPane panneau)
		{
			addMouseMotionListener(new MouseAdapter()
			{
				@Override public void mouseDragged(MouseEvent e)
				{
					if (dernier == null) return;
					JViewport vue = panneau.getViewport();
					Point position = vue.getViewPosition();
					position.translate(dernier.x - e.getX(), dernier.y - e.getY());
					position.x = Math.max(0, Math.min(position.x, Math.max(0, getWidth() - vue.getWidth())));
					position.y = Math.max(0, Math.min(position.y, Math.max(0, getHeight() - vue.getHeight())));
					vue.setViewPosition(position);
				}
			});
		}

		void setAfficherArguments(boolean afficher) { afficherArguments = afficher; repaint(); }

		void afficher(Graphe graphe, Noeud racine, int profondeurMax)
		{
			Map<Noeud, Integer> niveaux = organiser(graphe, racine, profondeurMax);
			blocs.clear();
			Map<Integer, Integer> nombre = new HashMap<>();
			int maxX = 0, maxY = 0;
			for (Map.Entry<Noeud, Integer> entree : niveaux.entrySet())
			{
				int niveau = entree.getValue();
				int ligne = nombre.getOrDefault(niveau, 0);
				nombre.put(niveau, ligne + 1);
				int x = 35 + niveau * ECART_X, y = 35 + ligne * ECART_Y;
				blocs.put(entree.getKey(), new Rectangle(x, y, LARGEUR, HAUTEUR));
				maxX = Math.max(maxX, x + LARGEUR + 100);
				maxY = Math.max(maxY, y + HAUTEUR + 80);
			}
			Map<CleGroupe, List<Lien>> groupes = new LinkedHashMap<>();
			for (Lien lien : graphe.liens())
				if (lienDescendant(lien, niveaux))
					groupes.computeIfAbsent(new CleGroupe(lien.origine(), lien.destination(), lien.relation()),
						k -> new ArrayList<>()).add(lien);
			liens = groupes.entrySet().stream().map(e -> new Groupe(e.getKey().origine(), e.getKey().destination(),
				e.getKey().relation(), List.copyOf(e.getValue()))).toList();
			courbes.clear();
			setPreferredSize(new Dimension(Math.max(600, maxX), Math.max(400, maxY)));
			revalidate();
			repaint();
		}

		@Override protected void paintComponent(Graphics graphique)
		{
			super.paintComponent(graphique);
			Graphics2D g = (Graphics2D) graphique.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			courbes.clear();
			Map<Noeud, Integer> sorties = new HashMap<>(), entrees = new HashMap<>();
			Map<Noeud, Integer> indexSorties = new HashMap<>(), indexEntrees = new HashMap<>();
			for (Groupe lien : liens)
			{
				sorties.merge(lien.origine(), 1, Integer::sum);
				entrees.merge(lien.destination(), 1, Integer::sum);
			}
			for (Groupe lien : liens)
			{
				int sortie = indexSorties.merge(lien.origine(), 1, Integer::sum);
				int entree = indexEntrees.merge(lien.destination(), 1, Integer::sum);
				Rectangle a = blocs.get(lien.origine()), b = blocs.get(lien.destination());
				dessinerLien(g, lien, a, b, sortie, sorties.get(lien.origine()), entree, entrees.get(lien.destination()));
			}
			for (Map.Entry<Noeud, Rectangle> entree : blocs.entrySet()) dessinerBloc(g, entree.getKey(), entree.getValue());
			g.dispose();
		}

		private void dessinerLien(Graphics2D g, Groupe lien, Rectangle a, Rectangle b,
			int sortie, int nbSorties, int entree, int nbEntrees)
		{
			double sx, sy, ex, ey, cx, cy;
			Shape courbe;
			if (a == b)
			{
				sx = a.x + a.width * 0.35; sy = a.y;
				ex = a.x + a.width * 0.65; ey = a.y;
				cx = a.x + a.width / 2.0; cy = a.y - 65 - 14 * sortie;
				courbe = new CubicCurve2D.Double(sx, sy, sx - 40, cy, ex + 40, cy, ex, ey);
			}
			else if (a.x == b.x)
			{
				sx = a.x + a.width; sy = a.y + a.height * sortie / (nbSorties + 1.0);
				ex = b.x + b.width; ey = b.y + b.height * entree / (nbEntrees + 1.0);
				cx = sx + 60 + 10 * sortie; cy = (sy + ey) / 2;
				courbe = new QuadCurve2D.Double(sx, sy, cx, cy, ex, ey);
			}
			else
			{
				boolean droite = a.x < b.x;
				sx = droite ? a.x + a.width : a.x; sy = a.y + a.height * sortie / (nbSorties + 1.0);
				ex = droite ? b.x : b.x + b.width; ey = b.y + b.height * entree / (nbEntrees + 1.0);
				cx = (sx + ex) / 2; cy = (sy + ey) / 2;
				courbe = new QuadCurve2D.Double(sx, sy, cx, cy, ex, ey);
			}
			courbes.put(lien, courbe);
			g.setColor(lien.relation() == Relation.CONTIENT ? Color.GRAY : BlocClasse.couleurLangage(lien.origine().langage()));
			g.setStroke(lien.relation() == Relation.APPEL || lien.relation() == Relation.CONSTRUCTION ||
				lien.relation() == Relation.AFFECTATION || lien.relation() == Relation.RETOUR
				? new BasicStroke(1.5f) : new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_ROUND, 10, new float[] {6, 5}, 0));
			g.draw(courbe);
			double angle = Math.atan2(ey - cy, ex - cx);
			int x1 = (int)(ex - 10 * Math.cos(angle - 0.45));
			int y1 = (int)(ey - 10 * Math.sin(angle - 0.45));
			int x2 = (int)(ex - 10 * Math.cos(angle + 0.45));
			int y2 = (int)(ey - 10 * Math.sin(angle + 0.45));
			g.drawLine((int)ex, (int)ey, x1, y1);
			g.drawLine((int)ex, (int)ey, x2, y2);
			if ((afficherArguments && lien.relation() != Relation.CONTIENT) || lien.occurrences().size() > 1)
			{
				Lien premier = lien.occurrences().get(0);
				String etiquette = "";
				if (afficherArguments)
					etiquette = switch (lien.relation())
					{
						case REFERENCE -> "réf. " + premier.arguments();
						case AFFECTATION -> "= " + premier.arguments();
						case RETOUR -> "return " + premier.arguments();
						case CONSTRUCTION -> "new (" + premier.arguments() + ")";
						case APPEL -> "(" + premier.arguments() + ")";
						case CONTIENT -> "";
					};
				if (lien.occurrences().size() > 1) etiquette += " ×" + lien.occurrences().size();
				if (etiquette.length() > 34) etiquette = etiquette.substring(0, 31) + "...";
				g.setFont(getFont().deriveFont(10f));
				FontMetrics fm = g.getFontMetrics();
				int x = (int)((sx + 2 * cx + ex) / 4), y = (int)((sy + 2 * cy + ey) / 4);
				g.setColor(Color.WHITE);
				g.fillRect(x, y - fm.getAscent(), fm.stringWidth(etiquette) + 4, fm.getHeight());
				g.setColor(Color.DARK_GRAY);
				g.drawString(etiquette, x + 2, y);
			}
		}

		private void dessinerBloc(Graphics2D g, Noeud noeud, Rectangle r)
		{
			g.setColor(noeud.nature() == Nature.EXTERNE ? new Color(242, 242, 242) : new Color(230, 240, 250));
			g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
			g.setColor(BlocClasse.couleurLangage(noeud.langage()));
			g.setStroke(new BasicStroke(2));
			g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
			g.setColor(Color.BLACK);
			g.setFont(getFont().deriveFont(Font.BOLD, 12f));
			String nom = noeud.nom();
			FontMetrics fm = g.getFontMetrics();
			while (nom.length() > 1 && fm.stringWidth(nom) > r.width - 22) nom = nom.substring(0, nom.length() - 1);
			if (!nom.equals(noeud.nom())) nom += "...";
			g.drawString(nom, r.x + 10, r.y + 23);
			g.setFont(getFont().deriveFont(10f));
			String sousTitre = noeud.nature() == Nature.EXTERNE ? "Externe / non résolu"
				: (noeud.nature() == Nature.CLASSE ? "Classe" : noeud.nature() == Nature.VALEUR ? "Valeur" : "Méthode")
					+ " — " + noeud.fichier().getFileName() + ":" + noeud.ligne();
			g.drawString(sousTitre, r.x + 10, r.y + 46);
		}

		@Override public String getToolTipText(MouseEvent e)
		{
			for (Map.Entry<Noeud, Rectangle> entree : blocs.entrySet())
				if (entree.getValue().contains(e.getPoint())) return entree.getKey().nom();
			for (Map.Entry<Groupe, Shape> entree : courbes.entrySet())
				if (new BasicStroke(9).createStrokedShape(entree.getValue()).contains(e.getPoint()))
				{
					Groupe lien = entree.getKey();
					return type(lien.relation()) + " — " + lien.occurrences().size() + " occurrence(s), cliquer pour les détails";
				}
			return null;
		}
	}
}
