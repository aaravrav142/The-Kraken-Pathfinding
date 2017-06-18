/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
 * Distributed under the MIT License.
 */

package pfg.kraken.pathfinding.astar.arcs.vitesses;

import pfg.kraken.pathfinding.astar.DirectionStrategy;
import pfg.kraken.robot.Cinematique;

/**
 * Les différentes vitesses de courbure qu'on peut suivre
 * 
 * @author pf
 *
 */

public interface VitesseCourbure
{
	public boolean isAcceptable(Cinematique c, DirectionStrategy directionstrategyactuelle, double courbureMax);

	public int getNbArrets();
}