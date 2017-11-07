/**
 * Copyright (c) 2006, 2009, Thomas Abeel
 * All rights reserved.
 * This file is part of the EPS Graphics Library
 * The EPS Graphics Library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * The EPS Graphics Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with the EPS Graphics Library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 * Project: http://sourceforge.net/projects/epsgraphics/
 */
package net.sf.epsgraphics;

/**
 * Enumeration of available Color Modes for EPS.
 * @author Thomas Abeel
 * @version 1.1.5
 */
public enum ColorMode {
	BLACK_AND_WHITE, GRAYSCALE, COLOR_RGB, COLOR_CMYK;

	/**
	 * This function returns the default Color Mode, for clients that may not
	 * know enough about the domain to know which one is the preferred default
	 * in the EPS context.
	 * @return The most common preferred default Color Mode
	 * @since 1.1.5
	 */
	public static ColorMode defaultValue() {
		return COLOR_RGB;
	}
}
