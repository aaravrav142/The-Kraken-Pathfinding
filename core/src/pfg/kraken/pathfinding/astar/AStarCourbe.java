/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
 * Distributed under the MIT License.
 */

package pfg.kraken.pathfinding.astar;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Stack;
import config.Config;
import graphic.AbstractPrintBuffer;
import pfg.kraken.ConfigInfoKraken;
import pfg.kraken.exceptions.NoPathException;
import pfg.kraken.exceptions.NotFastEnoughException;
import pfg.kraken.exceptions.NotInitializedPathfindingException;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.exceptions.TimeoutException;
import pfg.kraken.memory.CinemObsPool;
import pfg.kraken.memory.NodePool;
import pfg.kraken.pathfinding.astar.arcs.ArcCourbe;
import pfg.kraken.pathfinding.astar.arcs.ArcManager;
import pfg.kraken.pathfinding.chemin.CheminPathfindingInterface;
import pfg.kraken.pathfinding.chemin.DefaultCheminPathfinding;
import pfg.kraken.pathfinding.dstarlite.DStarLite;
import pfg.kraken.pathfinding.dstarlite.gridspace.PointGridSpace;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.DefaultSpeed;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.robot.RobotState;
import pfg.kraken.robot.Speed;
import pfg.kraken.utils.Log;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.Log.Verbose;

/**
 * A* qui utilise le D* Lite comme heuristique pour fournir une trajectoire
 * courbe
 * 
 * @author pf
 *
 */

public class AStarCourbe
{
	protected Log log;
	private ArcManager arcmanager;
	private DStarLite dstarlite;
	private NodePool memorymanager;
	private AbstractPrintBuffer buffer;
	private AStarCourbeNode depart;
	private AStarCourbeNode trajetDeSecours;
	private CinemObsPool cinemMemory;
	private DefaultCheminPathfinding defaultChemin;
	private boolean graphicTrajectory, graphicDStarLite, graphicTrajectoryAll;
	private int dureeMaxPF;
	private Speed vitesseMax;
	// private int tailleFaisceau;
	private volatile boolean rechercheEnCours = false;

	/**
	 * Comparateur de noeud utilisé par la priority queue
	 * 
	 * @author pf
	 *
	 */
	private class AStarCourbeNodeComparator implements Comparator<AStarCourbeNode>
	{
		@Override
		public int compare(AStarCourbeNode arg0, AStarCourbeNode arg1)
		{
			// Ordre lexico : on compare d'abord first, puis second
			int tmp = (int) (arg0.f_score - arg1.f_score);
			if(tmp != 0)
				return tmp;
			return (int) (arg0.g_score - arg1.g_score);
		}
	}

	private final HashSet<AStarCourbeNode> closedset = new HashSet<AStarCourbeNode>();
	private final PriorityQueue<AStarCourbeNode> openset = new PriorityQueue<AStarCourbeNode>(PointGridSpace.NB_POINTS, new AStarCourbeNodeComparator());
	private Stack<ArcCourbe> pileTmp = new Stack<ArcCourbe>();
	private LinkedList<ItineraryPoint> trajectory = new LinkedList<ItineraryPoint>();

	// private HashSet<AStarCourbeNode> closedsetTmp = new
	// HashSet<AStarCourbeNode>();
	// private final PriorityQueue<AStarCourbeNode> opensetTmp = new
	// PriorityQueue<AStarCourbeNode>(100, new AStarCourbeNodeComparator());

	/**
	 * Constructeur du AStarCourbe
	 */
	public AStarCourbe(Log log, DefaultCheminPathfinding defaultChemin, DStarLite dstarlite, ArcManager arcmanager, NodePool memorymanager, CinemObsPool rectMemory, AbstractPrintBuffer buffer, RobotState chrono, Config config)
	{
		this.defaultChemin = defaultChemin;
		this.log = log;
		this.arcmanager = arcmanager;
		this.memorymanager = memorymanager;
		this.dstarlite = dstarlite;
		this.cinemMemory = rectMemory;
		this.buffer = buffer;
		graphicTrajectory = config.getBoolean(ConfigInfoKraken.GRAPHIC_TRAJECTORY);
		graphicTrajectoryAll = config.getBoolean(ConfigInfoKraken.GRAPHIC_TRAJECTORY_ALL);
		graphicDStarLite = config.getBoolean(ConfigInfoKraken.GRAPHIC_D_STAR_LITE_FINAL);
		dureeMaxPF = config.getInt(ConfigInfoKraken.DUREE_MAX_RECHERCHE_PF);
		// tailleFaisceau = config.getInt(ConfigInfo.TAILLE_FAISCEAU_PF);
		int demieLargeurNonDeploye = config.getInt(ConfigInfoKraken.LARGEUR_NON_DEPLOYE) / 2;
		int demieLongueurArriere = config.getInt(ConfigInfoKraken.DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE);
		int demieLongueurAvant = config.getInt(ConfigInfoKraken.DEMI_LONGUEUR_NON_DEPLOYE_AVANT);
		this.depart = new AStarCourbeNode(chrono, demieLargeurNonDeploye, demieLongueurArriere, demieLongueurAvant);
		depart.setIndiceMemoryManager(-1);
	}

	public LinkedList<ItineraryPoint> process() throws PathfindingException
	{
		process(defaultChemin, false);
		return defaultChemin.getPath();
	}
	
	/**
	 * Le calcul du AStarCourbe
	 * 
	 * @param depart
	 * @return
	 * @throws PathfindingException
	 * @throws InterruptedException
	 * @throws MemoryPoolException
	 */
	private final synchronized void process(CheminPathfindingInterface chemin, boolean replanif) throws PathfindingException
	{
		if(!rechercheEnCours)
			throw new NotInitializedPathfindingException("updatePath appelé alors qu'aucune recherche n'est en cours !");

		log.debug("Recherche de chemin.", Verbose.PF.masque);
		trajetDeSecours = null;
		depart.parent = null;
		depart.cameFromArcDynamique = null;
		depart.g_score = 0;

		Double heuristique = arcmanager.heuristicCostCourbe((depart.robot).getCinematique());

		if(heuristique == null)
		{
			throw new NoPathException("Aucun chemin trouvé par le D* Lite !");
		}

		depart.f_score = heuristique / vitesseMax.getMaxForwardSpeed(0);
		openset.clear();
		openset.add(depart); // Les nœuds à évaluer
		closedset.clear();

		long debutRecherche = System.currentTimeMillis();

		AStarCourbeNode current, successeur;
		do
		{
			current = openset.poll();

			/**
			 * needStop ne concerne que la replanif
			 */
			if(replanif && chemin.needStop())
				throw new NotFastEnoughException("On a vu l'obstacle trop tard, on n'a pas assez de marge. Il faut s'arrêter.");

			// On vérifie régulièremet s'il ne faut pas fournir un chemin
			// partiel
			Cinematique cinemRestart = chemin.getLastValidCinematique();
			boolean assezDeMarge = chemin.aAssezDeMarge();

			if(cinemRestart != null || !assezDeMarge)
			{
				if(!assezDeMarge)
				{
					log.debug("Reconstruction partielle demandée !");
					depart.robot.setCinematique(partialReconstruct(current, chemin, 2));
					if(!chemin.aAssezDeMarge()) // toujours pas assez de marge
												// : on doit arrêter
						throw new NotFastEnoughException("Pas assez de marge même après envoi.");
				}
				else // il faut partir d'un autre point
				{
					log.debug("On reprend la recherche à partir de " + cinemRestart, Verbose.REPLANIF.masque);
					depart.init();
					depart.robot.setCinematique(cinemRestart);
				}/*
				if(suppObsFixes)
				{
					CinematiqueObs obsDepart = cinemMemory.getNewNode();
					Cinematique cinemDepart = depart.state.robot.getCinematique();
					obsDepart.updateReel(cinemDepart.getPosition().getX(), cinemDepart.getPosition().getY(), cinemDepart.orientationReelle, cinemDepart.enMarcheAvant, cinemDepart.courbureReelle);
					arcmanager.disableObstaclesFixes(symetrie, obsDepart);
				}*/

				trajetDeSecours = null;
				depart.parent = null;
				depart.cameFromArcDynamique = null;
				depart.g_score = 0;
				heuristique = arcmanager.heuristicCostCourbe((depart.robot).getCinematique());

				if(heuristique == null)
				{
					throw new NoPathException("Aucun chemin trouvé par le D* Lite !");
				}

				depart.f_score = heuristique / vitesseMax.getMaxForwardSpeed(0);

				memorymanager.empty();
				cinemMemory.empty();
				closedset.clear();
				openset.clear();

				current = depart;
			}

			// if(current.getArc() != null)
			// log.debug("Meilleur : "+current.getArc().vitesse);

			// si on a déjà fait ce point ou un point très proche…
			// exception si c'est un point d'arrivée
			if(!closedset.add(current) && !arcmanager.isArrived(current))
			{
				if(graphicTrajectory || graphicTrajectoryAll)
					current.setDead();
				if(current != depart)
					destroy(current);
				continue;
			}

			// ce calcul étant un peu lourd, on ne le fait que si le noeud a été
			// choisi, et pas à la sélection des voisins (dans hasNext par
			// exemple)
			if(!arcmanager.isReachable(current))
			{
				if(current != depart)
					destroy(current);
				continue; // collision mécanique attendue. On passe au suivant !
			}

			// affichage
			if(graphicTrajectory && !graphicTrajectoryAll)
				buffer.addSupprimable(current);

			// Si current est la trajectoire de secours, ça veut dire que cette
			// trajectoire de secours est la meilleure possible, donc on a fini
			if(current == trajetDeSecours)
			{
				// log.debug("On est arrivé !");
				chemin.setUptodate();
				partialReconstruct(current, chemin);
				memorymanager.empty();
				cinemMemory.empty();
				return;
			}

			long elapsed = System.currentTimeMillis() - debutRecherche;

			if(!rechercheEnCours)
			{
				chemin.setUptodate();
				log.warning("La recherche de chemin a été annulée");
				return;
			}

			if(!replanif && elapsed > dureeMaxPF) // étant donné
																// qu'il peut
																// continuer
																// jusqu'à
																// l'infini...
			{
				memorymanager.empty();
				cinemMemory.empty();
				if(trajetDeSecours != null) // si on a un trajet de secours, on
											// l'utilise
				{
					log.debug("Utilisation du trajet de secours !");
					chemin.setUptodate();
					partialReconstruct(trajetDeSecours, chemin);
					return;
				}
				throw new TimeoutException("Timeout AStarCourbe !");
			}

			// On parcourt les voisins de current
			// opensetTmp.clear();
			// closedsetTmp.clear();
			arcmanager.reinitIterator(current);
			while(arcmanager.hasNext())
			{
				successeur = memorymanager.getNewNode();
				successeur.cameFromArcDynamique = null;

				// S'il y a un problème, on passe au suivant (interpolation
				// cubique impossible par exemple)
				if(!arcmanager.next(successeur))
				{
					destroy(successeur);
					continue;
				}
				successeur.parent = current;
				successeur.g_score = current.g_score + arcmanager.distanceTo(successeur, vitesseMax);

				// on a déjà visité un point proche?
				// ceci est vraie seulement si l'heuristique est monotone. C'est
				// normalement le cas.
				if(closedset.contains(successeur))
				{
					destroy(successeur);
					continue;
				}

				// affichage
				if(graphicTrajectoryAll)
					buffer.addSupprimable(successeur);

				heuristique = arcmanager.heuristicCostCourbe(successeur.robot.getCinematique());
				if(heuristique == null)
					heuristique = arcmanager.heuristicDirect(successeur.robot.getCinematique());

				successeur.f_score = successeur.g_score + heuristique / vitesseMax.getMaxForwardSpeed(0);

				// noeud d'arrivé
				if(arcmanager.isArrived(successeur) && arcmanager.isReachable(successeur) && (trajetDeSecours == null || trajetDeSecours.f_score > successeur.f_score))
				{
					if(trajetDeSecours != null)
						destroy(trajetDeSecours);
					// log.debug("Arrivée trouvée !");
					trajetDeSecours = successeur;
				}

				openset.add(successeur);
				// opensetTmp.add(successeur);
				// log.debug(successeur.getArc().vitesse+"
				// "+successeur.g_score+"
				// "+(successeur.f_score-successeur.g_score));
			}

			// On ajoute que les meilleurs
			/*
			 * int nbAjoutes = 0;
			 * while(nbAjoutes < tailleFaisceau)
			 * {
			 * AStarCourbeNode n = opensetTmp.poll();
			 * if(n == null) // pas assez de nœuds ? Tant pis
			 * break;
			 * if(closedsetTmp.add(n)) // on vérifie que ce hash n'est pas déjà
			 * utilisé
			 * {
			 * openset.add(n);
			 * nbAjoutes++;
			 * }
			 * }
			 */

		} while(!openset.isEmpty());

		/**
		 * Plus aucun nœud à explorer
		 */
		memorymanager.empty();
		cinemMemory.empty();
		throw new NoPathException("Plus aucun nœud à explorer !");
	}

	private void destroy(AStarCourbeNode n)
	{
		if(n.cameFromArcDynamique != null)
			cinemMemory.destroyNode(n.cameFromArcDynamique);
		memorymanager.destroyNode(n);
	}

	private final Cinematique partialReconstruct(AStarCourbeNode best, CheminPathfindingInterface chemin)
	{
		return partialReconstruct(best, chemin, 500);
	}

	/**
	 * Reconstruit le chemin. Il peut reconstruire le chemin même si celui-ci
	 * n'est pas fini.
	 * 
	 * @param best
	 * @param last
	 * @throws PathfindingException
	 */
	private final Cinematique partialReconstruct(AStarCourbeNode best, CheminPathfindingInterface chemin, int profondeurMax)
	{
		AStarCourbeNode noeudParent = best;
		ArcCourbe arcParent = best.getArc();

		while(noeudParent.parent != null)
		{
			pileTmp.push(arcParent);
			noeudParent = noeudParent.parent;
			arcParent = noeudParent.getArc();
		}
		trajectory.clear();

		Cinematique last = null;
		// chemin.add fait des copies des points
		while(!pileTmp.isEmpty() && profondeurMax > 0)
		{
			ArcCourbe a = pileTmp.pop();
			log.debug(a.vitesse + " (" + a.getNbPoints() + " pts)", Verbose.PF.masque);
			for(int i = 0; i < a.getNbPoints(); i++)
			{
				last = a.getPoint(i);
				trajectory.add(new ItineraryPoint(last));
			}
			assert trajectory.size() < 255 : "Overflow du trajet";
			profondeurMax--;
		}

		pileTmp.clear();
		chemin.addToEnd(trajectory);

		return last;
	}

	/**
	 * Calcul de chemin classique
	 * 
	 * @param arrivee
	 * @param sens
	 * @param shoot
	 * @throws PathfindingException
	 * @throws InterruptedException
	 */
	public void initializeNewSearch(XYO start, XY arrival)
	{
		vitesseMax = DefaultSpeed.STANDARD;
		depart.init();
		depart.robot.setCinematique(new Cinematique(start));
		arcmanager.configureArcManager(DirectionStrategy.defaultStrategy, arrival);
/*
		if(suppObsFixes)
		{
			CinematiqueObs obsDepart = cinemMemory.getNewNode();
			Cinematique cinemDepart = depart.state.robot.getCinematique();
			obsDepart.updateReel(cinemDepart.getPosition().getX(), cinemDepart.getPosition().getY(), cinemDepart.orientationReelle, cinemDepart.enMarcheAvant, cinemDepart.courbureReelle);
			arcmanager.disableObstaclesFixes(symetrie, obsDepart);
		}*/

		dstarlite.computeNewPath(depart.robot.getCinematique().getPosition(), arrival);
		if(graphicDStarLite)
			dstarlite.itineraireBrut();
		rechercheEnCours = true;
	}

	/**
	 * Calcul de chemin avec arrivée sur un cercle
	 * 
	 * @param state
	 * @param endNode
	 * @param shoot_game_element
	 * @return
	 * @throws PathfindingException
	 * @throws InterruptedException
	 */
/*	public void initializeNewSearchToCircle(RobotState robot) throws PathfindingException, MemoryManagerException
	{
		vitesseMax = DefaultSpeed.STANDARD;
		depart.init();
		robot.copy(depart.robot);
		arcmanager.configureArcManagerWithCircle(DirectionStrategy.defaultStrategy);*/
/*
		if(suppObsFixes)
		{
			CinematiqueObs obsDepart = cinemMemory.getNewNode();
			Cinematique cinemDepart = depart.state.robot.getCinematique();
			obsDepart.updateReel(cinemDepart.getPosition().getX(), cinemDepart.getPosition().getY(), cinemDepart.orientationReelle, cinemDepart.enMarcheAvant, cinemDepart.courbureReelle);
			arcmanager.disableObstaclesFixes(symetrie, obsDepart);
		}*/
/*
		dstarlite.computeNewPath(depart.robot.getCinematique().getPosition(), cercle.arriveeDStarLite);
		if(graphicDStarLite)
			dstarlite.itineraireBrut();
		rechercheEnCours = true;
	}
*/
	/**
	 * Replanification. On conserve la même DirectionStrategy ainsi que le même
	 * SensFinal
	 * Par contre, si besoin est, on peut changer la politique de shootage
	 * d'éléments de jeu
	 * S'il n'y avait aucun recherche en cours, on ignore.
	 * 
	 * @param shoot
	 * @throws PathfindingException
	 * @throws InterruptedException
	 */
	public void updatePath(Cinematique lastValid) throws NotInitializedPathfindingException
	{
		if(!rechercheEnCours)
			throw new NotInitializedPathfindingException("updatePath appelé alors qu'aucune recherche n'est en cours !");

		log.debug("Replanification lancée", Verbose.REPLANIF.masque);

		depart.init();
// TODO		state.copyAStarCourbe(depart.state);

		/*
		 * Forcément, on utilise le vrai chemin ici
		 */

		closedset.clear();
		depart.robot.setCinematique(lastValid);

/*		if(suppObsFixes)
		{
			CinematiqueObs obsDepart = cinemMemory.getNewNode();
			Cinematique cinemDepart = depart.state.robot.getCinematique();
			obsDepart.updateReel(cinemDepart.getPosition().getX(), cinemDepart.getPosition().getY(), cinemDepart.orientationReelle, cinemDepart.enMarcheAvant, cinemDepart.courbureReelle);
			arcmanager.disableObstaclesFixes(symetrie, obsDepart);
		}*/

		// On met à jour le D* Lite
		dstarlite.updateStart(depart.robot.getCinematique().getPosition());
		dstarlite.updateObstaclesEnnemi();
		if(graphicDStarLite)
			dstarlite.itineraireBrut();

//		process(realChemin, true);
	}
	
/*	public boolean isArrivedAsser()
	{
		return arcmanager.isArrivedAsser(state.robot.getCinematique());
	}*/

}