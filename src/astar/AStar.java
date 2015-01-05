package astar;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import astar.arc.Arc;
import astar.arc.PathfindingNodes;
import astar.arcmanager.ArcManager;
import astar.arcmanager.PathfindingArcManager;
import astar.arcmanager.StrategyArcManager;
import container.Service;
import exceptions.ArcManagerException;
import exceptions.FinMatchException;
import exceptions.GridSpaceException;
import exceptions.MemoryManagerException;
import exceptions.PathfindingException;
import exceptions.PathfindingRobotInObstacleException;
import exceptions.strategie.ScriptException;
import robot.RobotChrono;
import strategie.GameState;
import utils.Config;
import utils.ConfigInfo;
import utils.Log;
import utils.Vec2;

/**
 * AStar, fonctionnant avec un certain ArcManager
 * Le AStar avec le PathfindingArcManager donne une recherche de chemin
 * Le AStar avec le StrategyArcManager donne un planificateur de scripts
 * @author pf, Martial
 *
 */

public class AStar<AM extends ArcManager, A extends Arc> implements Service
{
	/**
	 * Les analogies sont:
	 * un noeud est un GameState<RobotChrono> dans les deux cas
	 * un arc est un script pour l'arbre des possibles,
	 *   un pathfindingnode pour le pathfinding
	 */
	
	private static final int nb_max_element = 100;
	
	private LinkedList<GameState<RobotChrono>> openset = new LinkedList<GameState<RobotChrono>>();	 // The set of tentative nodes to be evaluated
	private boolean[] closedset = new boolean[nb_max_element];
	private int[] came_from = new int[nb_max_element];
	@SuppressWarnings("unchecked")
	private A[] came_from_arc = (A[]) new Arc[nb_max_element];
	private int[] g_score = new int[nb_max_element];
	private int[] f_score = new int[nb_max_element];
	
	private PathfindingException pathfindingexception;
	
	protected Log log;
	private Config config;
	private AM arcmanager;
	private MemoryManager memorymanager;
	
	private int distanceEnnemiUrgence = 700;
	
	/**
	 * Constructeur du AStar de pathfinding ou de stratégie, selon AM
	 */
	public AStar(Log log, Config config, AM arcmanager, MemoryManager memorymanager)
	{
		this.log = log;
		this.config = config;
		this.arcmanager = arcmanager;
		this.memorymanager = memorymanager;
		updateConfig();
	}

	/**
	 * Les méthodes publiques sont "synchronized".
	 * Cela signifique que si un AStar calcule une recherche de chemin pour un thread, l'autre thread devra attendre.
	 * Par contre, on peut faire un AStar stratégique et un AStar de pathfinding simultanément.
	 * Normalement, ce n'est pas utile car tous appels à AStar devraient être fait par le StrategyThread
	 * @param state
	 * @return
	 * @throws FinMatchException
	 * @throws PathfindingException 
	 * @throws MemoryManagerException 
	 */
	
	public synchronized ArrayList<A> computeStrategyEmergency(GameState<RobotChrono> state) throws FinMatchException, PathfindingException, MemoryManagerException
	{
		if(!(arcmanager instanceof StrategyArcManager))
		{
			// Ne devrait jamais arriver.
			new Exception().printStackTrace();
			return null;
		}
		int distance_ennemie = distanceEnnemiUrgence; // il faut que cette distance soit au moins supérieure à la somme de notre rayon, du rayon de l'adversaire et d'une marge
		double orientation_actuelle = state.robot.getOrientation();
		Vec2 positionEnnemie = state.robot.getPosition().plusNewVector(new Vec2((int)(distance_ennemie*Math.cos(orientation_actuelle)), (int)(distance_ennemie*Math.sin(orientation_actuelle))));
		state.gridspace.createHypotheticalEnnemy(positionEnnemie, state.robot.getTempsDepuisDebutMatch());
		return computeStrategy(state);
	}

	public synchronized ArrayList<A> computeStrategyAfter(GameState<RobotChrono> state, A decision) throws FinMatchException, PathfindingException, MemoryManagerException
	{
		if(!(arcmanager instanceof StrategyArcManager))
		{
			new Exception().printStackTrace();
			return null;
		}
		try {
			// Exécution de la décision donnée, ce qui est fait par "distanceTo".
			arcmanager.distanceTo(state, decision);
		} catch (ScriptException e) {
			return new ArrayList<A>();
		}
//		log.debug("Après exécution, on est en "+state.robot.getPositionPathfinding(), this);
		return computeStrategy(state);
	}

	private ArrayList<A> computeStrategy(GameState<?> state) throws FinMatchException, PathfindingException, MemoryManagerException
	{
		GameState<RobotChrono> depart = memorymanager.getNewGameState();
		state.copy(depart);

		// à chaque nouveau calcul de stratégie, il faut réinitialiser les hash de gamestate
		// si on ne le fait pas, on consommera trop de mémoire.
		((StrategyArcManager)arcmanager).reinitHashes();
		ArrayList<A> cheminArc;
		
		cheminArc = process(depart, arcmanager, true);
		if(cheminArc.size() == 0)
		{
			if(pathfindingexception == null)
				pathfindingexception = new PathfindingException();
			throw pathfindingexception;
		}
		return cheminArc;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized ArrayList<A> computePath(GameState<RobotChrono> state, PathfindingNodes indice_point_arrivee, boolean shoot_game_element) throws PathfindingException, PathfindingRobotInObstacleException, FinMatchException, MemoryManagerException
	{
		if(!(arcmanager instanceof PathfindingArcManager))
		{
			new Exception().printStackTrace();
			return null;
		}
		try {
			state.gridspace.setAvoidGameElement(!shoot_game_element);

			PathfindingNodes pointDepart;
			if(state.robot.isAtPathfindingNodes())
				pointDepart = state.robot.getPositionPathfinding();
			else
				pointDepart = state.gridspace.nearestReachableNode(state.robot.getPosition(), state.robot.getTempsDepuisDebutMatch());

			GameState<RobotChrono> depart = memorymanager.getNewGameState();
			state.copy(depart);
			depart.robot.setPositionPathfinding(pointDepart);
			
			((PathfindingArcManager)arcmanager).chargePointArrivee(indice_point_arrivee);
			
//			log.debug("Recherche de chemin entre "+pointDepart+" et "+indice_point_arrivee, this);
			ArrayList<A> cheminArc = process(depart, arcmanager, false);
			
			// on n'a besoin de lisser que si on ne partait pas d'un pathfindingnode
			if(!state.robot.isAtPathfindingNodes())
			{
				// parce qu'on ne part pas du point de départ directement...
				cheminArc.add(0, (A)pointDepart);
				cheminArc = lissage(state.robot.getPosition(), state, cheminArc);
			}
			
//			log.debug("Recherche de chemin terminée", this);
			return cheminArc;
		} catch (GridSpaceException e1) {
			throw new PathfindingRobotInObstacleException();
		}
	}
	
	@Override
	public void updateConfig()
	{
		distanceEnnemiUrgence = config.getInt(ConfigInfo.DISTANCE_ENNEMI_URGENCE);
	}
	
	// Si le point de départ est dans un obstacle fixe, le lissage ne changera rien.
	private ArrayList<A> lissage(Vec2 depart, GameState<RobotChrono> state, ArrayList<A> chemin)
	{
		// ATTENTION! LE LISSAGE COÛTE TRÈS CHER!
		// si on peut sauter le premier point, on le fait
		while(chemin.size() >= 2 && state.gridspace.isTraversable(depart, ((PathfindingNodes)chemin.get(1)).getCoordonnees(), state.robot.getTempsDepuisDebutMatch()))
			chemin.remove(0);

		return chemin;
	}
	
	private ArrayList<A> process(GameState<RobotChrono> depart, ArcManager arcmanager, boolean shouldReconstruct) throws PathfindingException, MemoryManagerException
	{
		int hash_depart = arcmanager.getHashAndCreateIfNecessary(depart);
		// optimisation si depart == arrivee
		if(arcmanager.isArrive(hash_depart))
		{
			depart = memorymanager.destroyGameState(depart);
			return new ArrayList<A>();
		}

		// plus rapide que des arraycopy
		for(int i = 0; i < nb_max_element; i++)
		{
			closedset[i] = false;
			came_from_arc[i] = null;
			came_from[i] = -1;
			g_score[i] = 133700000;//= Integer.MAX_VALUE;
			f_score[i] = Integer.MAX_VALUE;
		}
		
		openset.clear();
		openset.add(depart);	// The set of tentative nodes to be evaluated, initially containing the start node
		g_score[hash_depart] = 0;	// Cost from start along best known path.
		// Estimated total cost from start to goal through y.
		f_score[hash_depart] = g_score[hash_depart] + arcmanager.heuristicCost(depart);
		
		GameState<RobotChrono> current = null;

		while (!openset.isEmpty())
		{
			ListIterator<GameState<RobotChrono>> iterator = openset.listIterator();
			int min_score = Integer.MAX_VALUE;
			int potential_min, index_min = 0, index = -1;
			int hash_current = -1;
			GameState<RobotChrono> tmp;
			while(iterator.hasNext())
			{
				index++;
				tmp = iterator.next();
				try {
					int tmp_hash = arcmanager.getHash(tmp);
					potential_min = f_score[tmp_hash];
					if(min_score >= potential_min)
					{
						min_score = potential_min;
						current = tmp;
						index_min = index;
						hash_current = tmp_hash;
					}
				} catch (ArcManagerException e) {
					e.printStackTrace();
				}
			}
			openset.remove(index_min);
			
			// élément déjà fait
			// cela parce qu'il y a des doublons dans openset
			if(closedset[hash_current])
			{
//				if(arcmanager instanceof StrategyArcManager)
//					log.debug("Destruction de: "+current.getIndiceMemoryManager(), this);
				current = memorymanager.destroyGameState(current);
				continue;
			}
			
			// Si on est arrivé, on reconstruit le chemin
			if(arcmanager.isArrive(hash_current))
			{
//				if(arcmanager instanceof StrategyArcManager)
//					log.debug("Destruction de: "+current.getIndiceMemoryManager(), this);
				current = memorymanager.destroyGameState(current);
				freeGameStateOpenSet(openset);
				return reconstruct(hash_current);
			}

			closedset[hash_current] = true;
		    
			arcmanager.reinitIterator(current);

			while(arcmanager.hasNext(current))
			{
				A voisin = arcmanager.next();

				GameState<RobotChrono> successeur;
				try {
					successeur = memorymanager.getNewGameState();
					current.copy(successeur);
				} catch (FinMatchException e1) {
					// ne devrait pas arriver!
					throw new PathfindingException();
				}
				
				// successeur est modifié lors du "distanceTo"
				// si ce successeur dépasse la date limite, on l'annule
				int tentative_g_score;
				try {
					tentative_g_score = g_score[hash_current] + arcmanager.distanceTo(successeur, voisin);
				} catch (ScriptException | FinMatchException e) {
					continue;
				}

				int hash_successeur = arcmanager.getHashAndCreateIfNecessary(successeur);

//				if(arcmanager instanceof StrategyArcManager)
//					log.debug(voisin+" donne "+hash_successeur, this);
				
				if(closedset[hash_successeur]) // si closedset contient ce hash
				{
//					if(arcmanager instanceof StrategyArcManager)
//						log.debug("Destruction de: "+successeur.getIndiceMemoryManager(), this);
					successeur = memorymanager.destroyGameState(successeur);
					continue;
				}
				
				if(tentative_g_score < g_score[hash_successeur])
				{
					came_from[hash_successeur] = hash_current;
					came_from_arc[hash_successeur] = voisin;
					g_score[hash_successeur] = tentative_g_score;
					f_score[hash_successeur] = tentative_g_score + arcmanager.heuristicCost(successeur);
					openset.add(successeur);
				}
				else
					successeur = memorymanager.destroyGameState(successeur);
			}
			
//			if(arcmanager instanceof StrategyArcManager)
//				log.debug("Destruction de: "+current.getIndiceMemoryManager(), this);
			current = memorymanager.destroyGameState(current);	
		}
		
		// Pathfinding terminé sans avoir atteint l'arrivée
		freeGameStateOpenSet(openset);
		
		// La stratégie renvoie un chemin partiel, pas le pathfinding qui lève une exception
		if(!shouldReconstruct)
		{
			if(pathfindingexception == null)
				pathfindingexception = new PathfindingException();
			throw pathfindingexception;
		}

//		log.debug("Reconstruction!", this);
		
		/**
		 * Même si on n'a pas atteint l'objectif, on reconstruit un chemin partiel
		 */
		int note_best = Integer.MIN_VALUE;
		int note_f_best = Integer.MAX_VALUE;
		int best = -1;
		// On maximise le nombre de points qu'on fait.
		// En cas d'égalité, on prend le chemin le plus rapide.
		
		for(int h = 0; h < nb_max_element; h++)
			if(closedset[h]) // si ce noeud a été parcouru (sinon getNoteReconstruct va paniquer)
			{
				int potentiel_note_best = arcmanager.getNoteReconstruct(h);
				int potentiel_note_f_best = f_score[h];
//				log.debug(potentiel_note_best+" en "+potentiel_note_f_best, this);
				if(potentiel_note_best > note_best || potentiel_note_best == note_best && potentiel_note_f_best < note_f_best)
				{
					best = h;
					note_best = potentiel_note_best;
					note_f_best = potentiel_note_f_best;
				}
			}
		
		// best est nécessairement non nul car closedset contient au moins le point de départ
		return reconstruct(best);
		
	}

	private ArrayList<A> reconstruct(int hash) {
		ArrayList<A> chemin = new ArrayList<A>();
		int noeud_parent = came_from[hash];
		A arc_parent = came_from_arc[hash];
		while (noeud_parent != -1)
		{
			chemin.add(0, arc_parent);
			arc_parent = came_from_arc[noeud_parent];
			noeud_parent = came_from[noeud_parent];
		}
		return chemin;	//  reconstructed path
	}

	public void freeGameStateOpenSet(LinkedList<GameState<RobotChrono>> openset2) throws MemoryManagerException
	{
		for(GameState<RobotChrono> state: openset2)
			memorymanager.destroyGameState(state);
	}
	
}