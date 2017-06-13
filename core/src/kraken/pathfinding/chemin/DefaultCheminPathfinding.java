/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
 * Distributed under the MIT License.
 */

package kraken.pathfinding.chemin;

import java.awt.Graphics;
import java.util.LinkedList;
import config.Config;
import graphic.Fenetre;
import graphic.AbstractPrintBuffer;
import graphic.printable.Layer;
import graphic.printable.Printable;
import kraken.ConfigInfoKraken;
import kraken.Couleur;
import kraken.obstacles.types.ObstacleCircular;
import kraken.robot.Cinematique;
import kraken.robot.ItineraryPoint;
import kraken.utils.Log;
import kraken.utils.XY;

/**
 * Faux chemin, sert à la prévision d'itinéraire
 * 
 * @author pf
 *
 */

public class DefaultCheminPathfinding implements CheminPathfindingInterface, Printable
{
	private static final long serialVersionUID = -9020733512144987231L;
	private LinkedList<ItineraryPoint> path;
	protected Log log;
	private AbstractPrintBuffer buffer;
	private boolean print;
	private ObstacleCircular[] aff = new ObstacleCircular[256];

	public DefaultCheminPathfinding(Log log, Config config, AbstractPrintBuffer buffer)
	{
		this.log = log;
		this.buffer = buffer;
		print = config.getBoolean(ConfigInfoKraken.GRAPHIC_TRAJECTORY_FINAL);
		if(print)
			buffer.add(this);
	}

	@Override
	public synchronized void addToEnd(LinkedList<ItineraryPoint> points)
	{
		path = points;
		if(print)
			synchronized(buffer)
			{
				buffer.notify();
			}
		notify();
	}

	@Override
	public void setUptodate()
	{}

	public LinkedList<ItineraryPoint> getPath()
	{
		LinkedList<ItineraryPoint> out = path;
		path = null;
		return out;
	}

	@Override
	public void print(Graphics g, Fenetre f)
	{
		int i = 0;
		if(path != null)
			for(ItineraryPoint c : path)
			{
				aff[i] = new ObstacleCircular(new XY(c.x, c.y), 8, Couleur.TRAJECTOIRE);
				buffer.addSupprimable(aff[i]);
				i++;
			}
	}

	@Override
	public int getLayer()
	{
		return Layer.FOREGROUND.ordinal();
	}

	@Override
	public boolean aAssezDeMarge()
	{
		return true;
	}

	@Override
	public boolean needStop()
	{
		return false;
	}

	@Override
	public Cinematique getLastValidCinematique()
	{
		return null;
	}

	public void clear()
	{
		path = null;
	}
}
