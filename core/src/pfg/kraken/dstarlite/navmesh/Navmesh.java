/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
 * Distributed under the MIT License.
 */

package pfg.kraken.dstarlite.navmesh;

import pfg.config.Config;
import pfg.graphic.AbstractPrintBuffer;
import pfg.kraken.utils.XY;
import pfg.log.Log;
import pfg.kraken.ConfigInfoKraken;
import pfg.kraken.LogCategoryKraken;
import pfg.kraken.SeverityCategoryKraken;
import pfg.kraken.obstacles.container.StaticObstacles;

import java.io.IOException;

/**
 * A navmesh, used by the D* Lite.
 * It can load and save a navmesh. If necessary, it generate a new one.
 * 
 * @author pf
 *
 */

public class Navmesh
{
	protected Log log;
	public TriangulatedMesh mesh;
	
	public Navmesh(Log log, Config config, StaticObstacles obs, AbstractPrintBuffer buffer)
	{
		this.log = log;
		String filename = "navmesh-"+obs.hashCode()+"-"+config.getInt(ConfigInfoKraken.LARGEST_TRIANGLE_AREA_IN_NAVWESH)+".krk";
		try {
			log.write("D* NavMesh loading…", LogCategoryKraken.PF);
			mesh = TriangulatedMesh.loadNavMesh(filename);
		}
		catch(IOException | ClassNotFoundException | NullPointerException e)
		{
			log.write("The navmesh can't be loaded : generation of a new one.", SeverityCategoryKraken.WARNING, LogCategoryKraken.PF);
			NavmeshComputer computer = new NavmeshComputer(log, config);
			mesh = computer.generateNavMesh(obs);
/*			try {
				mesh.saveNavMesh(filename);
				log.write("Navmesh saved into "+filename, LogCategoryKraken.PF);
			}
			catch(IOException e1)
			{
				log.write("Error during navmesh save ! " + e, SeverityCategoryKraken.CRITICAL, LogCategoryKraken.PF);
			}*/
		}
		assert mesh != null;
		if(config.getBoolean(ConfigInfoKraken.GRAPHIC_NAVMESH))
			mesh.addToBuffer(buffer);
	}
	
	@Override
	public String toString()
	{
		return mesh.toString();
	}

	public double getDistance(NavmeshNode n1, NavmeshNode n2)
	{
		return 0;
	}
	
	public NavmeshNode getNearest(XY position)
	{
		NavmeshNode bestNode = null;
		double smallestDistance = 0;
		for(NavmeshNode n : mesh.nodes)
		{
			double candidateDistance = position.distance(n.position);
			if(bestNode == null || candidateDistance < smallestDistance)
			{
				bestNode = n;
				smallestDistance = candidateDistance;
			}
		}
		assert bestNode != null;
		return bestNode;
	}

}