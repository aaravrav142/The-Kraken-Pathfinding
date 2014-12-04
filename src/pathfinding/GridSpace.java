package pathfinding;

import obstacles.ObstacleManager;
import container.Service;
import smartMath.Vec2;
import utils.Config;
import utils.Log;
import enums.NodesConnection;
import enums.PathfindingNodes;
import exceptions.GridSpaceException;

/**
 * Contient les informations sur le graphe utilisé par le pathfinding.
 * Intègre un mécanisme de cache afin d'accélérer les calculs.
 * @author pf
 *
 */

public class GridSpace implements Service {
	
	private Log log;
	private Config config;
	private ObstacleManager obstaclemanager;
		
	private int iterator, id_node_iterator;
	private PathfindingNodes nearestReachableNodeCache = null;
	
	// Rempli de ALWAYS_IMPOSSIBLE et UNKNOW. Ne change pas.
	private static NodesConnection[][] isConnectedModel = null;

	// Dynamique.
	private NodesConnection[][] isConnected = new NodesConnection[PathfindingNodes.values().length][PathfindingNodes.values().length];
	
	// Contient les distances entre chaque point de passage
	private static double[][] distances = new double[PathfindingNodes.values().length][PathfindingNodes.values().length];

	public GridSpace(Log log, Config config, ObstacleManager obstaclemanager)
	{
		this.log = log;
		this.config = config;
		this.obstaclemanager = obstaclemanager;
		
		check_pathfinding_nodes();
		
		// Il est très important de ne faire ce long calcul qu'une seule fois,
		// à la première initialisation
		if(isConnectedModel == null)
		{
			initStatic();
		}
	}

    public void check_pathfinding_nodes()
    {
    	for(PathfindingNodes i: PathfindingNodes.values())
    		if(obstaclemanager.is_obstacle_fixe_present_pathfinding(i.getCoordonnees()))
    			log.warning("Node "+i+" dans obstacle!", this);
    }
    

	private void initStatic()
	{
		log.debug("Calcul de isConnectedModel", this);
		isConnectedModel = new NodesConnection[PathfindingNodes.values().length][PathfindingNodes.values().length];

		for(PathfindingNodes i : PathfindingNodes.values())			
			for(PathfindingNodes j : PathfindingNodes.values())
			{
				if(i.ordinal() >= j.ordinal())
					continue;
				if(obstaclemanager.obstacle_fixe_dans_segment_pathfinding(i.getCoordonnees(), j.getCoordonnees()))
					isConnectedModel[i.ordinal()][j.ordinal()] = NodesConnection.ALWAYS_IMPOSSIBLE;
				else
					isConnectedModel[i.ordinal()][j.ordinal()] = null;
				isConnected[i.ordinal()][j.ordinal()] = isConnectedModel[i.ordinal()][j.ordinal()];
				isConnected[j.ordinal()][i.ordinal()] = isConnectedModel[i.ordinal()][j.ordinal()];
			}				

		for(PathfindingNodes i : PathfindingNodes.values())
			for(PathfindingNodes j : PathfindingNodes.values())
				if(isConnectedModel[i.ordinal()][j.ordinal()] != NodesConnection.ALWAYS_IMPOSSIBLE)
				{
					distances[i.ordinal()][j.ordinal()] = i.getCoordonnees().distance(j.getCoordonnees());
				}
	}
	
	/**
	 * Réinitialise l'état des liaisons.
	 * A faire quand les obstacles mobiles ont changé.
	 */
	public void reinitConnections(long date)
	{
		obstaclemanager.supprimerObstaclesPerimes(date);
		for(PathfindingNodes i : PathfindingNodes.values())
			for(PathfindingNodes j : PathfindingNodes.values())
				if(i.ordinal() < j.ordinal())
				{
					isConnected[i.ordinal()][j.ordinal()] = isConnectedModel[i.ordinal()][j.ordinal()];
					isConnected[j.ordinal()][i.ordinal()] = isConnectedModel[i.ordinal()][j.ordinal()];
				}
	}

	/**
	 * Surcouche de isConnected qui gère le cache
	 * @return
	 */
	public boolean isTraversable(PathfindingNodes i, PathfindingNodes j)
	{
		if(isConnected[i.ordinal()][j.ordinal()] != null)
			return isConnected[i.ordinal()][j.ordinal()].isTraversable();
		else if(obstaclemanager.obstacle_proximite_dans_segment(i.getCoordonnees(), j.getCoordonnees()))
			isConnected[i.ordinal()][j.ordinal()] = NodesConnection.TMP_IMPOSSIBLE;
		else
			isConnected[i.ordinal()][j.ordinal()] = NodesConnection.POSSIBLE;
		isConnected[j.ordinal()][i.ordinal()] = isConnected[i.ordinal()][j.ordinal()];
		return isConnected[i.ordinal()][j.ordinal()].isTraversable();
	}
	
	/**
	 * Retourne le point de passage le plus proche et accessible en ligne droite
	 * Attention, peut renvoyer "null" si aucun point de passage n'est atteignable en ligne droite.
	 * @param point
	 * @return
	 * @throws GridSpaceException 
	 */
	public PathfindingNodes nearestReachableNode(Vec2 point) throws GridSpaceException
	{
		if(nearestReachableNodeCache != null)
			return nearestReachableNodeCache;
		PathfindingNodes indice_point_depart = null;
		float distance_min = Float.MAX_VALUE;
		for(PathfindingNodes i : PathfindingNodes.values())
		{
			float tmp = point.squaredDistance(i.getCoordonnees());
			if(tmp < distance_min && !obstaclemanager.obstacle_proximite_dans_segment(point, i.getCoordonnees()))
			{
				distance_min = tmp;
				indice_point_depart = i;
			}
		}
		if(indice_point_depart == null)
			throw new GridSpaceException();

		nearestReachableNodeCache = indice_point_depart;
		return nearestReachableNodeCache;
	}
	
	@Override
	public void updateConfig() {
		// TODO Auto-generated method stub
		
	}

	public void copy(GridSpace other, long date)
	{
		obstaclemanager.copy(other.obstaclemanager);
		// On détruit le cache car le robot aura bougé
		other.nearestReachableNodeCache = null;
		other.reinitConnections(date);
	}
	
	public GridSpace clone(long date)
	{
		GridSpace cloned_gridspace = new GridSpace(log, config, obstaclemanager);
		copy(cloned_gridspace, date);
		return cloned_gridspace;
	}
    
	/**
	 * Utilisé uniquement pour les tests
	 * @return
	 */
    public int nbObstaclesMobiles()
    {
    	return obstaclemanager.nbObstaclesMobiles();
    }

    /** 
     * A utiliser entre un node et un point quelconque (l'arrivée)
     * @param pointA
     * @param pointB
     * @return
     */
    public boolean isTraversable(Vec2 pointA, Vec2 pointB)
    {
    	// Evaluation paresseuse importante, car obstacle_proximite_dans_segment est bien plus rapide que obstacle_fixe_dans_segment
    	return !obstaclemanager.obstacle_proximite_dans_segment(pointA, pointB) && !obstaclemanager.obstacle_fixe_dans_segment_pathfinding(pointA, pointB);
    }

    public double getDistance(PathfindingNodes id1, PathfindingNodes id2)
    {
    	return distances[id1.ordinal()][id2.ordinal()];
    }
    
    public PathfindingNodes next()
    {
    	return PathfindingNodes.values()[iterator];
    }
    
    public boolean hasNext()
    {
    	do {
    		iterator++;
    	} while(iterator < PathfindingNodes.values().length && (iterator == id_node_iterator || !isTraversable(PathfindingNodes.values()[iterator], PathfindingNodes.values()[id_node_iterator])));
    	return iterator != PathfindingNodes.values().length;
    }
    
    public void reinitIterator(PathfindingNodes node)
    {
    	id_node_iterator = node.ordinal();
    	iterator = -1;
    }
    
}