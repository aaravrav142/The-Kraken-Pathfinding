/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package kraken.pathfinding.chemin;

import java.util.LinkedList;
import kraken.exceptions.PathfindingException;
import kraken.robot.Cinematique;
import kraken.robot.ItineraryPoint;

/**
 * Interface pour pouvoir interchanger le vrai chemin de pathfinding et le faux
 * 
 * @author pf
 *
 */

public interface CheminPathfindingInterface
{
	public void addToEnd(LinkedList<ItineraryPoint> points) throws PathfindingException;

	public void setUptodate();

	public boolean aAssezDeMarge();

	public boolean needStop();

	public Cinematique getLastValidCinematique();
}