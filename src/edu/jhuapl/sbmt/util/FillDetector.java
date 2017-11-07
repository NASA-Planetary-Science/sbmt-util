package edu.jhuapl.sbmt.util;

/**
 * Abstraction representing something that can detect when a value of some type is "fill", i.e. should be
 * discarded or handled in some special way.
 */
public interface FillDetector<T>
{
	/**
	 * Return true if the supplied argument represents a "fill" value.
	 * @param value the object to be evaluated.
	 */
    boolean isFill(T value);
}
