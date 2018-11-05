package protocol;

import java.io.Serializable;

/**
 * Defines a tuple that can store two arbitrary objects
 * @author ---
 *
 * @param <X>
 * @param <Y>
 */
public class Tuple<X, Y>  implements Serializable
{ 
	private static final long serialVersionUID = 1;
	
	public X x;
	public Y y;
  
	/**
	 * Creates a tuple
	 * @param x object to store
	 * @param y object to store
	 */
	public Tuple(X x, Y y) 
	{ 
		this.x = x; 
		this.y = y; 
	}
	
	@Override
	public String toString()
	{
		return x.toString() + " " + y.toString();
	}
	
	/**
	 * Compares if an objects is equal to this tuple
	 * If it is not a tuple they are not equal.
	 * If it is a tuple both x and both y need to be equal with their respective equals method.
	 * @param object to test
	 * @return true if objects match, false otherwise.
	 */
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Tuple))
		{
			return false;
		} else
		{
			boolean equalsX = this.x.equals(((Tuple)o).x);
			boolean equalsY = this.y.equals(((Tuple)o).y);
			return equalsX && equalsY;
		}
	}
	
	/**
	 * HashCode for tuple by adding HashCodes of its objects.
	 */
	@Override
	public int hashCode()
	{
		return (this.x.hashCode() + this.y.hashCode()) % Integer.MAX_VALUE;
	}
} 