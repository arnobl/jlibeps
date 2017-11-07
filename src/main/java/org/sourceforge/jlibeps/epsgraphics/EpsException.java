/**
 * Copyright (c) 2001, 2006, Paul James Mutton
 * Copyright (c) 2007, Arnaud Blouin
 * All rights reserved.
 * This file is part of jlibeps, merged with similar code from EPS Graphics, and
 * expanded by Meyer Sound Laboratories Inc.
 * jlibeps is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * jlibeps is distributed without any warranty; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * Project: http://www.jibble.org/
 */
package org.sourceforge.jlibeps.epsgraphics;

/**
 * This is an encapsulation of exceptions thrown by this library, for purposes
 * of clearly identifying library calls as the underlying cause.
 * @author Paul James Mutton
 * @version 0.1
 */
public class EpsException extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 2125920484279307991L;

	/**
	 * Fully qualified constructor for a library encapsulation of exceptions
	 * related to EPS handling. Generally these will be recaptures of Core Java
	 * exceptions, wrapped in a library class to better mark the cause.
	 * @param message The full pre-parsed string to include with the exception
	 * @since 0.1
	 */
	public EpsException(final String message) {
		super(message);
	}
}
