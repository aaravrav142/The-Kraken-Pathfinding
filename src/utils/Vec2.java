package utils;

import java.io.Serializable;

import permissions.Permission;
import permissions.ReadOnly;
import permissions.ReadWrite;


/**
 * Classe de calcul de vecteurs de dimension 2
 * @author pf
 *
 */

public class Vec2<T extends Permission> implements Serializable
{
	private static final long serialVersionUID = 1L;
	public static final int coeffDirection = 1024; // le coefficient 1024 est là pour éviter une division bas niveau
	public volatile int x;
	public volatile int y;
	
	public Vec2()
	{
		x = 0;
		y = 0;
	}
	
	public Vec2(double angle)
	{
		x = (int) (Math.cos(angle)*coeffDirection);	
		y = (int) (Math.sin(angle)*coeffDirection);
	}

	public Vec2(int longueur, double angle, boolean useless)
	{
		x = (int) (Math.cos(angle)*longueur);
		y = (int) (Math.sin(angle)*longueur);
	}

	public Vec2(Vec2<? extends Permission> model)
	{
		x = model.x;
		y = model.y;
	}

	public Vec2(int requestedX, int requestedY)
	{
		x = requestedX;
		y = requestedY;
	}
	
	public final static Vec2<ReadWrite> setAngle(Vec2<ReadWrite> out, double angle)
	{
		out.x = (int) (Math.cos(angle)*coeffDirection);
		out.y = (int) (Math.sin(angle)*coeffDirection);
		return out;
	}

	// Do not square a length, use squared length directly
	// to increase performances
	public final int squaredLength()
	{
		return x*x + y*y;
	}

	// Returns this vec2's magnitude
	public final double length()
	{
		return Math.hypot(x, y);
	}
	
	// dot product
	public final int dot(Vec2<? extends Permission> other)
	{
		return x*other.x + y*other.y;
	}

	// build a new Vec2 by summing the calling Vec2 and the one in args
	public final Vec2<ReadWrite> plusNewVector(Vec2<? extends Permission> other)
	{
		return new Vec2<ReadWrite>(x + other.x, y + other.y);
	}
	
	// build a new Vec2 with the value obtained by decrementing the
	// calling Vec2 by the provided Vec2 in args
	public final Vec2<ReadWrite> minusNewVector(Vec2<? extends Permission> other)
	{
		return new Vec2<ReadWrite>(x - other.x, y - other.y);
	}

	public final static Vec2<ReadWrite> plus(Vec2<ReadWrite> modified, Vec2<? extends Permission> other)
	{
		modified.x += other.x;
		modified.y += other.y;
		return modified;
	}
	
	public final static Vec2<ReadWrite> minus(Vec2<ReadWrite> modified, Vec2<? extends Permission> other)
	{
		modified.x -= other.x;
		modified.y -= other.y;
		return modified;
	}

	public final Vec2<ReadWrite> clone()
	{
		return new Vec2<ReadWrite>(this.x, this.y);
	}

	@SuppressWarnings("unchecked")
	public final Vec2<ReadOnly> getReadOnly()
	{
		return (Vec2<ReadOnly>) this;
	}

	public final int squaredDistance(Vec2<? extends Permission> other)
	{
		int tmp_x = x-other.x, tmp_y = y-other.y;
		return tmp_x*tmp_x + tmp_y*tmp_y;
	}

	public final double distance(Vec2<? extends Permission> other)
	{
		return Math.hypot(x-other.x, y-other.y);
	}
	
	public final String toString()
	{
		return "("+x+","+y+")";
	}
	
	public final boolean equals(Vec2<? extends Permission> other)
	{
		return x == other.x && y == other.y;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		else if (obj == null)
			return false;
		else if (!(obj instanceof Vec2))
			return false;
		@SuppressWarnings("unchecked")
		Vec2<ReadOnly> other = (Vec2<ReadOnly>) obj;
		if (x != other.x || (y != other.y))
			return false;
		return true;
	}

	/**
	 * Copie this dans other.
	 * @param other
	 */
	public static final void copy(Vec2<ReadOnly> v, Vec2<ReadWrite> other)
	{
	    other.x = v.x;
	    other.y = v.y;
	}

	public final Vec2<ReadWrite> middleNewVector(Vec2<? extends Permission> b)
	{
		return new Vec2<ReadWrite>((x+b.x)/2, (y+b.y)/2);
	}

	public static final Vec2<ReadWrite> scalar(Vec2<ReadWrite> v, double d)
	{
		v.x = (int)(d*v.x);
		v.y = (int)(d*v.y);
		return v;
	}

	public static final Vec2<ReadWrite> Ysym(Vec2<ReadWrite> v, boolean symetrie)
	{
		if(symetrie)
			v.y = -v.y;
		return v;
	}

	public final Vec2<ReadWrite> scalarNewVector(double d)
	{
		Vec2<ReadWrite> out = new Vec2<ReadWrite>(this);
		scalar(out, d);
		return out;
	}
	
	public final Vec2<ReadWrite> rotateNewVector(double angle, Vec2<? extends Permission> centreRotation)
	{
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new Vec2<ReadWrite>((int)(cos*(x-centreRotation.x)-sin*(y-centreRotation.y))+centreRotation.x,
		(int)(sin*(x-centreRotation.x)+cos*(y-centreRotation.y))+centreRotation.y);
	}

	public final Vec2<ReadWrite> rotateNewVector(double cos, double sin, Vec2<? extends Permission> centreRotation)
	{
		return new Vec2<ReadWrite>((int)(cos*(x-centreRotation.x)-sin*(y-centreRotation.y))+centreRotation.x,
		(int)(sin*(x-centreRotation.x)+cos*(y-centreRotation.y))+centreRotation.y);
	}

	public final static Vec2<ReadWrite> rotate(Vec2<ReadWrite> v, double cos, double sin, Vec2<? extends Permission> centreRotation)
	{
		int a = (int)(cos*(v.x-centreRotation.x)-sin*(v.y-centreRotation.y))+centreRotation.x;
		v.y = (int)(sin*(v.x-centreRotation.x)+cos*(v.y-centreRotation.y))+centreRotation.y;
		v.x = a;
		return v;
	}

	/**
	 * Rotation avec pour centre de rotation (0,0)
	 * @param d
	 * @return
	 */
	public final Vec2<ReadWrite> rotateNewVector(double angle)
	{
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new Vec2<ReadWrite>((int)(cos*x-sin*y), (int)(sin*x+cos*y));
	}

	public final static Vec2<ReadWrite> rotate(Vec2<ReadWrite> v, double angle)
	{
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		int old_x = v.x;
		v.x = (int)(cos*v.x-sin*v.y);
		v.y = (int)(sin*old_x+cos*v.y);
		return v;
	}

	public final static Vec2<ReadWrite> rotate(Vec2<ReadWrite> v, double cos, double sin)
	{
		int old_x = v.x;
		v.x = (int)(cos*v.x-sin*v.y);
		v.y = (int)(sin*old_x+cos*v.y);
		return v;
	}

	public final double getArgument()
	{
		return Math.atan2(y, x);
	}
	
	/**
	 * Fait le projeté de this sur other, puis renvoie le résultat dans un nouveau Vec2
	 * @param other
	 * @return
	 */
	public final Vec2<ReadWrite> projectOnNewVector(Vec2<? extends Permission> other)
	{
		return scalarNewVector(dot(other));
	}

	/**
	 * Tourne le vecteur de PI/2
	 * @return
	 */
	public final static Vec2<ReadWrite> rotateAngleDroit(Vec2<ReadWrite> v)
	{
		int tmp = v.x;
		v.x = -v.y;
		v.y = tmp;
		return v;
	}

	/**
	 * v devient -v
	 * @param v
	 * @return
	 */
	public final static Vec2<ReadWrite> oppose(Vec2<ReadWrite> v)
	{
		v.x = -v.x;
		v.y = -v.y;
		return v;
	}
	
}

