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

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * An interface for an object that can draw itself within an rectangle using a
 * Graphics2D context.
 * @author Thomas Abeel
 * @version 1.1.4
 */
public interface Drawable {
	/**
	 * Draws the object in the rectangle using the provide graphics context.
	 * @param g2 The @EpsGraphics2D graphics context to draw on.
	 * @param area The bounds within which the @Drawable should be drawn.
	 * @since 1.1.4
	 */
	void draw(final Graphics2D g2, final Rectangle2D area);
}
