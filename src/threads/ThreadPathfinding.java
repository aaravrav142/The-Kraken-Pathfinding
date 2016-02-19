package threads;

import java.util.BitSet;

import container.Service;
import exceptions.PathfindingException;
import pathfinding.astarCourbe.AStarCourbe;
import pathfinding.dstarlite.GridSpace;
import utils.Config;
import utils.Log;

/**
 * Thread qui recalcule l'itinéraire à emprunter. Surveille ObstaclesMemory.
 * @author pf
 *
 */

public class ThreadPathfinding extends Thread implements Service
{
	protected Log log;
	private AStarCourbe pathfinding;
	private GridSpace gridspace;
	private boolean urgence = false;

	public ThreadPathfinding(Log log, AStarCourbe pathfinding, GridSpace gridspace)
	{
		this.log = log;
		this.pathfinding = pathfinding;
		this.gridspace = gridspace;
	}

	@Override
	public void run()
	{
		while(true)
		{
			synchronized(gridspace)
			{
				try {
					gridspace.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			BitSet xor = gridspace.getWhatChanged();
			
			if(!xor.isEmpty()) // si y'a du changement
			{
				try {
					notify(); // on prévient le thread d'évitement qu'un nouveau chemin est en calcul
					pathfinding.updatePath(xor);
				} catch (PathfindingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

//		log.debug("Fermeture de ThreadPathfinding");
	}

	@Override
	public void updateConfig(Config config)
	{}

	@Override
	public void useConfig(Config config)
	{}

	public final boolean isUrgence()
	{
		return urgence;
	}
	
}
