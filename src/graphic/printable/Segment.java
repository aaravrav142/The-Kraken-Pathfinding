/*
Copyright (C) 2013-2017 Pierre-François Gimenez

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

package graphic.printable;

import java.awt.Graphics;

import graphic.Fenetre;
import robot.RobotReal;
import utils.Vec2RO;

/**
 * Un segment affichable
 * @author pf
 *
 */

public class Segment implements Printable
{
	private Vec2RO a, b;
	private Layer l;

	public Segment(Vec2RO a, Vec2RO b, Layer l)
	{
		this.a = a;
		this.b = b;
		this.l = l;
	}

	@Override
	public void print(Graphics g, Fenetre f, RobotReal robot)
	{
		g.drawLine(f.XtoWindow(a.getX()), f.YtoWindow(a.getY()), f.XtoWindow(b.getX()), f.YtoWindow(b.getY()));
	}

	@Override
	public Layer getLayer()
	{
		return l;
	}

}
