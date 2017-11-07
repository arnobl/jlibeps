/**
 * Copyright (c) 2006, 2009, Thomas Abeel
 * Copyright (c) 2007, 2017, Meyer Sound Laboratories Inc.
 * All rights reserved.
 * This file is part of the EPS Graphics Library, expanded by Meyer Sound
 * Laboratories Inc.
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

import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.io.Writer;
import org.sourceforge.jlibeps.epsgraphics.EpsGraphics2D;

/**
 * Contains utility methods to create an EPS figure from an object that
 * implements the <code>Drawable</code> interface.
 * These functions are provided as a convenience, for clients whose needs are
 * quite simple. A more typical application will need to more tightly manage the
 * layout offsets of multiple sub-panels and thus will need its own interfaces
 * and functions to redirect the EPS graphics while compositing a single page of
 * EPS page layout. It is not possible to provide such examples within the
 * context of an open source library code base, unfortunately.
 * @author Thomas Abeel
 * @version 1.1.5
 */
public class EpsTools {
	/**
	 * Method to export a @Drawable object to an EPS file.
	 * This version of the function uses the old way of managing I/O, and is
	 * provided for backward-compatibility. Assuming a zero origin is not such a
	 * good idea for EPS as it generally causes clipping.
	 * @param drawable The @Drawable object
	 * @param fileName The file name of the EPS file
	 * @param width The width of the exported graphic
	 * @param height The height of the exported graphic
	 * @param colorMode The color mode to be used
	 * @return true when the export is successful, false in other cases
	 * @since 1.1.4
	 */
	@Deprecated
	public static boolean createFromDrawable(final Drawable drawable, final String fileName, final double width, final double height, final ColorMode
		colorMode) {
		try {
			final double x = 0d;
			final double y = 0d;
			final EpsGraphics2D epsGraphics = new EpsGraphics2D("EpsTools Drawable Export", new FileOutputStream(fileName + ".eps"), x, y, width, height,
				colorMode);
			drawable.draw(epsGraphics, new Rectangle2D.Double(x, y, width, height));
			epsGraphics.close();
			return true;
		}catch(final Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 * Method to export a @Drawable object to an EPS file.
	 * This version of the function uses the new way of managing I/O.
	 * @param drawable The @Drawable object
	 * @param writer The wrapped @Writer to channel the EPS content to
	 * @param title The title of the EPS Document
	 * @param x The x-coordinate of the exported graphic origin
	 * @param y The y-coordinate of the exported graphic origin
	 * @param width The width of the exported graphic
	 * @param height The height of the exported graphic
	 * @param colorMode The color mode to be used for all EPS graphics
	 * @return true when the export is successful, false in other cases
	 * @since 1.1.5
	 */
	public static boolean createFromDrawable(final Drawable drawable, final Writer writer, final String title, final double x, final double y, final double
		width, final double height, final ColorMode colorMode) {
		try {
			final EpsGraphics2D epsGraphics = new EpsGraphics2D(writer, title, x, y, width, height, colorMode);
			drawable.draw(epsGraphics, new Rectangle2D.Double(x, y, width, height));

			epsGraphics.finish();

			return true;
		}catch(final Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	/* This class should not be instantiated. */
	private EpsTools() {}
}
