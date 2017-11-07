/**
 * Copyright (c) 2001, 2006, Paul James Mutton
 * Copyright (c) 2006,2009, Thomas Abeel
 * Copyright (c) 2007, Arnaud Blouin
 * Copyright (c) 2007, 2017, Meyer Sound Laboratories Inc.
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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.RenderableImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.util.Hashtable;
import java.util.Map;
import net.sf.epsgraphics.ColorMode;

/**
 * EpsGraphics2D is suitable for creating high quality EPS graphics for use in
 * documents and papers, and can be used just like a standard Graphics2D object.
 * Many Java programs use Graphics2D to draw stuff on the screen, and while it
 * is easy to save the output as a PNG or JPEG file, it is a little harder to
 * export it as an EPS for including in a document or paper.
 * This class makes the whole process extremely easy, because you can use it as
 * if it's a Graphics2D object. The only difference is that all of the
 * implemented methods create EPS output, which means the diagrams you draw can
 * be resized without leading to any of the jagged edges you may see when
 * resizing pixel-based images, such as JPEG and PNG files.
 * Example usage:
 * <pre>
 * Graphics2D g = new EpsGraphics2D();
 * g.setColor( Color.black );
 * // Line thickness 2.
 * g.setStroke( new BasicStroke( 2f ) );
 * // Draw a line.
 * g.drawLine( 10, 10, 50, 10 );
 * // Fill a rectangle in blue
 * g.setColor( Color.blue );
 * g.fillRect( 10, 0, 20, 20 );
 * // Get the EPS output.
 * String output = g.toString();
 * </pre>
 * You do not need to worry about the size of the canvas when drawing on a
 * EpsGraphics2D object. The bounding box of the EPS document will automatically
 * resize to accommodate new items that you draw.
 * Not all methods are implemented yet. Those that are not are clearly labeled.
 * @author Paul James Mutton
 * @version 1.5.0
 */
public class EpsGraphics2D extends Graphics2D {

	public static final String VERSION = "1.5.0";

	public static final String OUTPUT_ERROR_MSG = "Could not write to the output file: ";
	public static final String METHOD_NOT_SUPPORTED_MSG = "Method currently not supported by jlibeps version " + VERSION;
	public static final String INVERSE_MATRIX_ERROR_MSG = "Unable to get inverse of matrix: ";
	public static final String STROKE_CLASS_ERROR_MSG = "Stroke must be an instance of BasicStroke: ";

	private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(null, false, true);

	/**
	 * This method is called to indicate that a particular method is not
	 * supported yet. The stack trace is printed to the standard output.
	 * @since 0.1
	 */
	private static void methodNotSupported() {
		// Alert the client to an unsupported graphics method via a library
		// encapsulated exception, but print the stack trace and discard it as
		// the client may have no control over processing forwarded exceptions
		// that might get invoked from within Core Java.
		final EpsException e = new EpsException(METHOD_NOT_SUPPORTED_MSG);
		e.printStackTrace();
	}

	/**
	 * Returns a hex string that always contains two characters.
	 * @since 0.1
	 */
	private static String toHexString(final int n) {
		String result = Integer.toString(n, 16);

		while(result.length() < 2) {
			result = "0" + result;
		}

		return result;
	}

	private Color _color;
	private Color _backgroundColor;
	private Paint _paint;
	private Composite _composite;
	private BasicStroke _stroke;
	private Font _font;
	private Shape _clip;
	private AffineTransform _clipTransform;
	private AffineTransform _transform;
	private boolean _accurateTextMode;
	private ColorMode _colorMode;

	private EpsDocument _document;

	/**
	 * Default constructor, for in-memory usage.
	 * Constructs a new EPS document that is initially empty and can be drawn on
	 * like a Graphics2D object. The EPS document is stored in memory.
	 * @since 0.1
	 */
	@Deprecated
	public EpsGraphics2D() {
		this("Untitled");
	}

	/**
	 * Copy constructor.
	 * Constructs a new EpsGraphics2D instance that is a copy of the supplied
	 * argument and points at the same EpsDocument.
	 * @param epsGraphics The @EpsGraphics2D graphics context used to redirect graphics
	 * drawing command to the EPS document
	 * @since 0.1
	 */
	private EpsGraphics2D(final EpsGraphics2D epsGraphics) {
		_document = epsGraphics._document;

		_backgroundColor = epsGraphics._backgroundColor;
		_clip = epsGraphics._clip;
		_clipTransform = (AffineTransform) epsGraphics._clipTransform.clone();
		_transform = (AffineTransform) epsGraphics._transform.clone();
		_accurateTextMode = epsGraphics._accurateTextMode;
		_colorMode = epsGraphics._colorMode;

		setStroke(epsGraphics.getStroke());
		setColor(epsGraphics.getColor());
		setPaint(epsGraphics.getPaint());
		setComposite(epsGraphics.getComposite());
		setFont(epsGraphics.getFont());
	}

	/**
	 * Default constructor, for in-memory usage.
	 * Constructs a new EPS document that is initially empty and can be drawn on
	 * like a Graphics2D object. The EPS document is stored in memory.
	 * It is questionable how safe this approach is, given how many state
	 * dependencies there are and how difficult it is to maintain all of the
	 * related writer and buffer functions to be aware of so many possible
	 * intentions and origins. This particular approach may have been designed
	 * for generating an in-memory EPS representation that could be embedded in
	 * a PDF object during PDF export, but Adobe no longer officially supports
	 * EPS embedded in PDF, starting a few revisions ago of the PDF spec.
	 * @param title The title of the EPS Document
	 * @since 0.1
	 */
	@Deprecated
	public EpsGraphics2D(final String title) {
		_document = new EpsDocument(title);

		_colorMode = ColorMode.defaultValue();

		setDefaults();
	}

	/**
	 * Partially qualified constructor.
	 * Constructs a new EPS document that is initially empty and can be drawn on
	 * like a Graphics2D object. The EPS document is written to the file as it
	 * goes, which reduces memory usage. The bounding box of the document is
	 * fixed and specified at construction time by minX,minY,maxX,maxY. The file
	 * is flushed and closed when the close() method is called.
	 * This constructor should only be called if the EPS document is to be made
	 * responsible for creating its own writer. Usually, the client will use
	 * try-with-resources to make the writer and pass that to a different
	 * constructor, in which case the document self-closes once out of scope.
	 * @param title The title of the EPS Document
	 * @param fileName The EPS file to write the EPS content to
	 * @param minX The x-coordinate of the EPS content top left corner
	 * @param minY The y-coordinate of the EPS content top left corner
	 * @param maxX The x-coordinate of the EPS content bottom right corner
	 * @param maxY The y-coordinate of the EPS content bottom right corner
	 * @throws IOException
	 * @since 0.1
	 */
	@Deprecated
	public EpsGraphics2D(final String title, final File file, final double minX, final double minY, final double maxX, final double maxY) throws IOException {
		this(title, new FileOutputStream(file), minX, minY, maxX, maxY);
	}

	/**
	 * Partially qualified constructor.
	 * Constructs a new EPS document that is initially empty and can be drawn on
	 * like a Graphics2D object. The EPS document is written to the output
	 * stream as it goes, which reduces memory usage. The bounding box of the
	 * document is fixed and specified at construction time by minX, minY, maxX,
	 * maxY. The output stream is flushed when the close() method is called.
	 * This constructor should only be called if the EPS document is to be made
	 * responsible for creating its own writer. Usually, the client will use
	 * try-with-resources to make the writer and pass that to a different
	 * constructor, in which case the document self-closes once out of scope.
	 * @param title The title of the EPS Document
	 * @param outputStream The @OutputStream to channel the EPS content to
	 * @param minX The x-coordinate of the EPS content top left corner
	 * @param minY The y-coordinate of the EPS content top left corner
	 * @param maxX The x-coordinate of the EPS content bottom right corner
	 * @param maxY The y-coordinate of the EPS content bottom right corner
	 * @throws IOException
	 * @since 0.1
	 */
	@Deprecated
	public EpsGraphics2D(final String title, final OutputStream outputStream, final double minX, final double minY, final double maxX, final double maxY)
		throws IOException {
		this(title, outputStream, minX, minY, maxX, maxY, ColorMode.defaultValue());
	}

	/**
	 * Partially qualified constructor.
	 * Constructs a new EPS document that is initially empty and can be drawn on
	 * like a Graphics2D object. The EPS document is written to the output
	 * stream as it goes, which reduces memory usage. The bounding box of the
	 * document is fixed and specified at construction time by minX, minY, maxX,
	 * maxY. The output stream is flushed when the close() method is called.
	 * This constructor should only be called if the EPS document is to be made
	 * responsible for creating its own writer. Usually, the client will use
	 * try-with-resources to make the writer and pass that to a different
	 * constructor, in which case the document self-closes once out of scope.
	 * @param title The title of the EPS Document
	 * @param outputStream The @OutputStream to channel the EPS content to
	 * @param minX The x-coordinate of the EPS content top left corner
	 * @param minY The y-coordinate of the EPS content top left corner
	 * @param maxX The x-coordinate of the EPS content bottom right corner
	 * @param maxY The y-coordinate of the EPS content bottom right corner
	 * @param colorMode The color mode to be used for all EPS graphics
	 * @throws IOException
	 * @since 0.1
	 */
	@Deprecated
	public EpsGraphics2D(final String title, final OutputStream outputStream, final double minX, final double minY, final double maxX, final double maxY,
						 final ColorMode colorMode) throws IOException {
		_document = new EpsDocument(title, outputStream, minX, minY, maxX, maxY);

		_colorMode = colorMode;

		setDefaults();
	}

	/**
	 * Partially qualified constructor.
	 * Constructs a new EPS document that is initially empty and can be drawn on
	 * like a Graphics2D object. The EPS document is written to the writer as it
	 * goes, which reduces memory usage. The bounding box of the document is
	 * fixed and specified at construction time by minX, minY, maxX, maxY. The
	 * writer is flushed when the finish() method is called.
	 * @param writer The wrapped @Writer to channel the EPS content to
	 * @param title The title of the EPS Document
	 * @param minX The x-coordinate of the EPS content top left corner
	 * @param minY The y-coordinate of the EPS content top left corner
	 * @param maxX The x-coordinate of the EPS content bottom right corner
	 * @param maxY The y-coordinate of the EPS content bottom right corner
	 * @param colorMode The color mode to be used for all EPS graphics
	 * @throws IOException
	 * @since 1.1.5
	 */
	public EpsGraphics2D(final Writer writer, final String title, final double minX, final double minY, final double maxX, final double maxY, final ColorMode
		colorMode) throws IOException {
		_document = new EpsDocument(writer, title, minX, minY, maxX, maxY);

		_colorMode = colorMode;

		setDefaults();
	}

	/**
	 * Adds rendering hints. These are ignored by EpsGraphics2D.
	 * @since 0.1
	 */
	@Override
	public void addRenderingHints(final Map<?, ?> hints) {
		// Do nothing.
	}

	/**
	 * Appends a line to the EpsDocument.
	 * @param line The new content line to write to the EPS document
	 * @see org.sourceforge.jlibeps.epsgraphics.EpsDocument
	 * @since 0.1
	 */
	private void append(final String line) {
		try {
			_document.append(this, line);
		}catch(final Exception e) {
			// Re-cast the exception using library encapsulation, but let the
			// client choose whether to print the stack trace or not.
			throw new EpsException(OUTPUT_ERROR_MSG + e.getLocalizedMessage());
		}
	}

	/**
	 * Appends a stroke to the @EpsDocument.
	 * This function only operates @BasicStroke objects (or subclasses
	 * of @BasicStroke); otherwise it is a no-op.
	 * @see java.awt.BasicStroke
	 * @since 0.1
	 */
	public void appendStroke() {
		append(_stroke.getLineWidth() + " setlinewidth");
		float miterLimit = _stroke.getMiterLimit();

		if(miterLimit < 1f) {
			miterLimit = 1;
		}

		append(miterLimit + " setmiterlimit");
		append(_stroke.getLineJoin() + " setlinejoin");
		append(_stroke.getEndCap() + " setlinecap");

		final StringBuilder dashes = new StringBuilder();
		dashes.append("[ ");
		final float[] dashArray = _stroke.getDashArray();

		if(dashArray != null) {
			for(final float element : dashArray) {
				dashes.append(element).append(" ");
			}
		}

		dashes.append("]");
		append(dashes.toString() + " 0 setdash");
	}

	/**
	 * Clears a rectangle with top-left corner placed at (x,y) using the current
	 * background color.
	 * @since 0.1
	 */
	@Override
	public void clearRect(final int x, final int y, final int width, final int height) {
		final Color originalColor = getColor();

		setColor(getBackground());
		final Shape shape = new Rectangle(x, y, width, height);
		draw(shape, "fill");

		setColor(originalColor);
	}

	/**
	 * Intersects the current clip with the interior of the specified Shape and
	 * sets the clip to the resulting intersection.
	 * @since 0.1
	 */
	@Override
	public void clip(final Shape shape) {
		if(_clip == null) {
			setClip(shape);
		}else {
			final Area area = new Area(_clip);
			area.intersect(new Area(shape));
			setClip(area);
		}
	}

	/**
	 * Intersects the current clip with the specified rectangle.
	 * @since 0.1
	 */
	@Override
	public void clipRect(final int x, final int y, final int width, final int height) {
		clip(new Rectangle(x, y, width, height));
	}

	/**
	 * Closes the EPS file being output to the underlying OutputStream. The
	 * OutputStream is automatically flushed before being closed. If you forget
	 * to do this, the file may be incomplete.
	 * This function should only be called if the EPS document was made
	 * responsible for creating its own writer. Usually, the client will use
	 * try-with-resources to make the writer and pass that to the EPS document,
	 * in which case the document self-closes once out of scope.
	 * @throws IOException
	 * @see org.sourceforge.jlibeps.epsgraphics.EpsDocument
	 * @since 0.1
	 */
	@Deprecated
	public void close() throws IOException {
		flush();

		_document.close();
	}

	/**
	 * <b><i><font color="red">Not implemented</font></i></b> - performs no
	 * action.
	 * @since 0.1
	 */
	@Override
	public void copyArea(final int x, final int y, final int width, final int height, final int dx, final int dy) {
		methodNotSupported();
	}

	/**
	 * Returns a new Graphics object that is identical to this EpsGraphics2D.
	 * @since 0.1
	 */
	@Override
	public Graphics create() {
		return new EpsGraphics2D(this);
	}

	/**
	 * Returns an EpsGraphics2D object based on this Graphics object, but with a
	 * new translation and clip area.
	 * @since 0.1
	 */
	@Override
	public Graphics create(final int x, final int y, final int width, final int height) {
		final Graphics graphics = create();

		graphics.translate(x, y);
		graphics.clipRect(0, 0, width, height);

		return graphics;
	}

	/**
	 * Disposes of all resources used by this EpsGraphics2D object. If this is
	 * the only remaining EpsGraphics2D instance pointing at a EpsDocument
	 * object, then the EpsDocument object shall become eligible for garbage
	 * collection.
	 * @see org.sourceforge.jlibeps.epsgraphics.EpsDocument
	 * @since 0.1
	 */
	@Override
	public void dispose() {
		_document = null;
	}

	/**
	 * Draws a Shape on the EPS document.
	 * @since 0.1
	 */
	@Override
	public void draw(final Shape shape) {
		draw(shape, "stroke");
	}

	/**
	 * Appends the commands required to draw a shape on the EPS document.
	 * @see org.sourceforge.jlibeps.epsgraphics.EpsDocument
	 * @since 0.1
	 */
	private void draw(final Shape shape, final String action) {
		if(shape == null) {
			return;
		}

		// The Stroke needs to be appended each time we draw a shape.
		appendStroke();

		// Avoid creeping numeric inaccuracy if identity transform.
		final Shape transformedShape = _transform.isIdentity() ? shape : _transform.createTransformedShape(shape);

		// Update the bounds.
		if(!action.equals("clip")) {
			final Rectangle2D shapeBounds = transformedShape.getBounds2D();
			Rectangle2D visibleBounds = shapeBounds;

			if(_clip != null) {
				final Rectangle2D clipBounds = _clip.getBounds2D();
				visibleBounds = shapeBounds.createIntersection(clipBounds);
			}

			final float lineRadius = 0.5f * _stroke.getLineWidth();
			final float minX = (float) visibleBounds.getMinX() - lineRadius;
			final float minY = (float) visibleBounds.getMinY() - lineRadius;
			final float maxX = (float) visibleBounds.getMaxX() + lineRadius;
			final float maxY = (float) visibleBounds.getMaxY() + lineRadius;
			_document.updateBounds(minX, -minY);
			_document.updateBounds(maxX, -maxY);
		}

		append("newpath");
		final float[] coords = new float[6];
		final PathIterator pathIterator = transformedShape.getPathIterator(null);
		float x0 = 0;
		float y0 = 0;
		// int count = 0;

		while(!pathIterator.isDone()) {
			final int segmentType = pathIterator.currentSegment(coords);
			final float x1 = coords[0];
			final float y1 = -coords[1];
			final float x2 = coords[2];
			final float y2 = -coords[3];
			final float x3 = coords[4];
			final float y3 = -coords[5];

			switch(segmentType) {
				case PathIterator.SEG_MOVETO:
					append(x1 + " " + y1 + " moveto");
					// count++;
					x0 = x1;
					y0 = y1;
					break;
				case PathIterator.SEG_LINETO:
					append(x1 + " " + y1 + " lineto");
					// count++;
					x0 = x1;
					y0 = y1;
					break;
				case PathIterator.SEG_CUBICTO:
					append(x1 + " " + y1 + " " + x2 + " " + y2 + " " + x3 + " " + y3 + " curveto");
					// count++;
					x0 = x3;
					y0 = y3;
					break;
				case PathIterator.SEG_QUADTO:
					// Convert the quad curve into a cubic.
					final float _x1 = x0 + ((2 / 3f) * (x1 - x0));
					final float _y1 = y0 + ((2 / 3f) * (y1 - y0));
					final float _x2 = x1 + ((1 / 3f) * (x2 - x1));
					final float _y2 = y1 + ((1 / 3f) * (y2 - y1));
					final float _x3 = x2;
					final float _y3 = y2;
					append(_x1 + " " + _y1 + " " + _x2 + " " + _y2 + " " + _x3 + " " + _y3 + " curveto");
					// count++;
					x0 = _x3;
					y0 = _y3;
					break;
				case PathIterator.SEG_CLOSE:
					append("closepath");
					// count++;
					break;
				default:
					break;
			}

			pathIterator.next();
		}

		append(action);
		append("newpath");
	}

	/**
	 * Draws a 3D rectangle outline. If it is raised, light appears to come from
	 * the top left.
	 * @since 0.1
	 */
	@Override
	public void draw3DRect(final int x, final int y, final int width, final int height, final boolean raised) {
		final Color originalColor = getColor();
		final Stroke originalStroke = getStroke();

		setStroke(new BasicStroke(1f));

		if(raised) {
			setColor(originalColor.brighter().brighter());
		}else {
			setColor(originalColor.darker().darker());
		}

		drawLine(x, y, x + width, y);
		drawLine(x, y, x, y + height);

		if(raised) {
			setColor(originalColor.darker().darker());
		}else {
			setColor(originalColor.brighter().brighter());
		}

		drawLine(x + width, y + height, x, y + height);
		drawLine(x + width, y + height, x + width, y);

		setColor(originalColor);
		setStroke(originalStroke);
	}

	/**
	 * Draws an arc.
	 * @since 0.1
	 */
	@Override
	public void drawArc(final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
		final Shape shape = new Arc2D.Float(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN);
		draw(shape);
	}

	/**
	 * Draws the specified bytes, starting from (x,y).
	 * @since 0.1
	 */
	@Override
	public void drawBytes(final byte[] data, final int offset, final int length, final int x, final int y) {
		final String string = new String(data, offset, length);
		drawString(string, x, y);
	}

	/**
	 * Draws the specified characters, starting from (x,y).
	 * @since 0.1
	 */
	@Override
	public void drawChars(final char[] data, final int offset, final int length, final int x, final int y) {
		final String string = new String(data, offset, length);
		drawString(string, x, y);
	}

	/**
	 * Draws a GlyphVector at (x,y).
	 * @since 0.1
	 */
	@Override
	public void drawGlyphVector(final GlyphVector g, final float x, final float y) {
		final Shape shape = g.getOutline(x, y);
		draw(shape, "fill");
	}

	/**
	 * Draws a BufferedImage on the EPS document.
	 * @since 0.1
	 */
	@Override
	public void drawImage(final BufferedImage img, final BufferedImageOp op, final int x, final int y) {
		final BufferedImage img1 = op.filter(img, null);
		drawImage(img1, new AffineTransform(1f, 0f, 0f, 1f, x, y), null);
	}

	/**
	 * Draws an Image on the EPS document.
	 * @since 0.1
	 */
	@Override
	public boolean drawImage(final Image img, final AffineTransform xform, final ImageObserver obs) {
		final AffineTransform at = getTransform();
		transform(xform);
		final boolean st = drawImage(img, 0, 0, obs);
		setTransform(at);
		return st;
	}

	/**
	 * Draws an image.
	 * @since 0.1
	 */
	@Override
	public boolean drawImage(final Image img, final int x, final int y, final Color bgcolor, final ImageObserver observer) {
		final int width = img.getWidth(null);
		final int height = img.getHeight(null);
		return drawImage(img, x, y, width, height, bgcolor, observer);
	}

	/**
	 * Draws an image.
	 * @since 0.1
	 */
	@Override
	public boolean drawImage(final Image img, final int x, final int y, final ImageObserver observer) {
		return drawImage(img, x, y, Color.WHITE, observer);
	}

	/**
	 * Draws an image.
	 * @since 0.1
	 */
	@Override
	public boolean drawImage(final Image img, final int x, final int y, final int width, final int height, final Color bgcolor, final ImageObserver observer) {
		return drawImage(img, x, y, x + width, y + height, 0, 0, width, height, bgcolor, observer);
	}

	/**
	 * Draws an image.
	 * @since 0.1
	 */
	@Override
	public boolean drawImage(final Image img, final int x, final int y, final int width, final int height, final ImageObserver observer) {
		return drawImage(img, x, y, width, height, Color.WHITE, observer);
	}

	/**
	 * Draws an image.
	 * @since 0.1
	 */
	@Override
	public boolean drawImage(final Image img, final int dx1, final int dy1, final int dx2, final int dy2, final int sx1, final int sy1, final int sx2, final
	int sy2, final Color bgcolor, final ImageObserver observer) {
		if(dx1 >= dx2) {
			throw new IllegalArgumentException("dx1 >= dx2");
		}

		if(sx1 >= sx2) {
			throw new IllegalArgumentException("sx1 >= sx2");
		}

		if(dy1 >= dy2) {
			throw new IllegalArgumentException("dy1 >= dy2");
		}

		if(sy1 >= sy2) {
			throw new IllegalArgumentException("sy1 >= sy2");
		}

		append("gsave");

		final int width = sx2 - sx1;
		final int height = sy2 - sy1;
		final int destWidth = dx2 - dx1;
		final int destHeight = dy2 - dy1;

		final int[] pixels = new int[width * height];
		final PixelGrabber pg = new PixelGrabber(img, sx1, sy1, sx2 - sx1, sy2 - sy1, pixels, 0, width);

		try {
			pg.grabPixels();
		}catch(final InterruptedException e) {
			return false;
		}

		AffineTransform matrix = new AffineTransform(_transform);
		matrix.translate(dx1, dy1);
		matrix.scale(destWidth / (double) width, destHeight / (double) height);
		final double[] m = new double[6];

		try {
			matrix = matrix.createInverse();
		}catch(final NoninvertibleTransformException nte) {
			// Alert the client to a non-invertible transform via a library
			// encapsulated exception, but let the client choose whether to
			// print the stack trace or not.
			throw new EpsException(INVERSE_MATRIX_ERROR_MSG + matrix);
		}

		matrix.scale(1, -1);
		matrix.getMatrix(m);
		final String bitsPerSample = "8";
		// :TODO: Not using proper imagemask function yet
		// if ( ColorMode.BLACK_AND_WHITE.equals( getColorDepth() ) ) {
		// bitsPerSample = "true";
		// }
		append(width + " " + height + " " + bitsPerSample + " [" + m[0] + " " + m[1] + " " + m[2] + " " + m[3] + " " + m[4] + " " + m[5] + "]");

		// Fill the background to update the bounding box.
		final Color oldColor = getColor();
		setColor(getBackground());
		fillRect(dx1, dy1, destWidth, destHeight);
		setColor(oldColor);

		final ColorMode colorMode = getColorMode();
		switch(colorMode) {
			case BLACK_AND_WHITE:
			case GRAYSCALE:
				// :TODO: Should really use imagemask.
				append("{currentfile " + width + " string readhexstring pop} bind");
				append("image");

				break;
			case COLOR_RGB:
			case COLOR_CMYK:
				// :NOTE: No difference between RGB and CMYK.
				append("{currentfile 3 " + width + " mul string readhexstring pop} bind");
				append("false 3 colorimage");

				break;
			default:
				break;
		}

		StringBuilder line = new StringBuilder();
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				final Color color = new Color(pixels[x + (width * y)]);

				switch(colorMode) {
					case BLACK_AND_WHITE:
						if((color.getRed() + color.getGreen() + color.getBlue()) > ((255 * 1.5) - 1)) {
							line.append("ff");
						}else {
							line.append("00");
						}

						break;
					case GRAYSCALE:
						line.append(toHexString((color.getRed() + color.getGreen() + color.getBlue()) / 3));

						break;
					case COLOR_RGB:
					case COLOR_CMYK:
						// :NOTE: No difference between RGB and CMYK.
						line.append(toHexString(color.getRed())).append(toHexString(color.getGreen())).append(toHexString(color.getBlue()));

						break;
					default:
						break;
				}

				if(line.length() > 64) {
					append(line.toString());
					line = new StringBuilder();
				}
			}
		}

		if(line.length() > 0) {
			append(line.toString());
		}

		append("grestore");

		return true;
	}

	/**
	 * Draws an image.
	 * @since 0.1
	 */
	@Override
	public boolean drawImage(final Image img, final int dx1, final int dy1, final int dx2, final int dy2, final int sx1, final int sy1, final int sx2, final
	int sy2, final ImageObserver observer) {
		return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, Color.WHITE, observer);
	}

	/**
	 * Draws a straight line from (x1,y1) to (x2,y2).
	 * @since 0.1
	 */
	@Override
	public void drawLine(final int x1, final int y1, final int x2, final int y2) {
		final Shape shape = new Line2D.Float(x1, y1, x2, y2);
		draw(shape);
	}

	/**
	 * Draws an oval.
	 * @since 0.1
	 */
	@Override
	public void drawOval(final int x, final int y, final int width, final int height) {
		final Shape shape = new Ellipse2D.Float(x, y, width, height);
		draw(shape);
	}

	/**
	 * Draws a polygon made with the specified points.
	 * @since 0.1
	 */
	@Override
	public void drawPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
		final Shape shape = new Polygon(xPoints, yPoints, nPoints);
		draw(shape);
	}

	/**
	 * Draws a polygon.
	 * @since 0.1
	 */
	@Override
	public void drawPolygon(final Polygon p) {
		draw(p);
	}

	/**
	 * Draws a polyline.
	 * @since 0.1
	 */
	@Override
	public void drawPolyline(final int[] xPoints, final int[] yPoints, final int nPoints) {
		if(nPoints > 0) {
			final GeneralPath path = new GeneralPath();
			path.moveTo(xPoints[0], yPoints[0]);

			for(int i = 1; i < nPoints; i++) {
				path.lineTo(xPoints[i], yPoints[i]);
			}

			draw(path);
		}
	}

	/**
	 * Draws a rectangle with top-left corner placed at (x,y).
	 * @since 0.1
	 */
	@Override
	public void drawRect(final int x, final int y, final int width, final int height) {
		final Shape shape = new Rectangle(x, y, width, height);
		draw(shape);
	}

	/**
	 * Draws a RenderableImage by invoking its createDefaultRendering method.
	 * @since 0.1
	 */
	@Override
	public void drawRenderableImage(final RenderableImage img, final AffineTransform xform) {
		drawRenderedImage(img.createDefaultRendering(), xform);
	}

	/**
	 * Draws a RenderedImage on the EPS document.
	 * @since 0.1
	 */
	@Override
	public void drawRenderedImage(final RenderedImage img, final AffineTransform xform) {
		final Hashtable<String, Object> properties = new Hashtable<>();
		final String[] names = img.getPropertyNames();

		for(final String name : names) {
			properties.put(name, img.getProperty(name));
		}

		final ColorModel cm = img.getColorModel();
		final WritableRaster wr = img.copyData(null);
		final BufferedImage bufferedImage = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), properties);
		final AffineTransform at = AffineTransform.getTranslateInstance(img.getMinX(), img.getMinY());
		at.preConcatenate(xform);
		drawImage(bufferedImage, at, null);
	}

	/**
	 * Draws a rounded rectangle.
	 * @since 0.1
	 */
	@Override
	public void drawRoundRect(final int x, final int y, final int width, final int height, final int arcWidth, final int arcHeight) {
		final Shape shape = new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight);
		draw(shape);
	}

	/**
	 * Draws the characters of an AttributedCharacterIterator, starting from
	 * (x,y).
	 * @since 0.1
	 */
	@Override
	public void drawString(final AttributedCharacterIterator iterator, final float x, final float y) {
		if(isAccurateTextMode()) {
			final TextLayout layout = new TextLayout(iterator, getFontRenderContext());
			final Shape shape = layout.getOutline(AffineTransform.getTranslateInstance(x, y));
			draw(shape, "fill");
		}else {
			append("newpath");
			final Point2D location = transform(x, y);
			append(location.getX() + " " + location.getY() + " moveto");
			final StringBuilder buffer = new StringBuilder();

			for(char ch = iterator.first(); ch != CharacterIterator.DONE; ch = iterator.next()) {
				if((ch == '(') || (ch == ')')) {
					buffer.append('\\');
				}

				buffer.append(ch);
			}

			append("(" + buffer.toString() + ") show");
		}
	}

	/**
	 * Draws the characters of an AttributedCharacterIterator, starting from
	 * (x,y).
	 * @since 0.1
	 */
	@Override
	public void drawString(final AttributedCharacterIterator iterator, final int x, final int y) {
		drawString(iterator, (float) x, (float) y);
	}

	/**
	 * Draws a string at (x,y).
	 * @since 0.1
	 */
	@Override
	public void drawString(final String str, final float x, final float y) {
		if((str != null) && !str.isEmpty()) {
			final AttributedString attributedString = new AttributedString(str);
			attributedString.addAttribute(TextAttribute.FONT, getFont());
			drawString(attributedString.getIterator(), x, y);
		}
	}

	/**
	 * Draws a string at (x,y).
	 * @since 0.1
	 */
	@Override
	public void drawString(final String str, final int x, final int y) {
		drawString(str, (float) x, (float) y);
	}

	/**
	 * Fills a Shape on the EPS document.
	 * @since 0.1
	 */
	@Override
	public void fill(final Shape shape) {
		draw(shape, "fill");
	}

	/**
	 * Fills a 3D rectangle. If raised, it has bright fill and light appears to
	 * come from the top left.
	 * @since 0.1
	 */
	@Override
	public void fill3DRect(final int x, final int y, final int width, final int height, final boolean raised) {
		final Color originalColor = getColor();

		if(raised) {
			setColor(originalColor.brighter().brighter());
		}else {
			setColor(originalColor.darker().darker());
		}

		draw(new Rectangle(x, y, width, height), "fill");
		setColor(originalColor);
		draw3DRect(x, y, width, height, raised);
	}

	/**
	 * Fills an arc.
	 * @since 0.1
	 */
	@Override
	public void fillArc(final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
		final Shape shape = new Arc2D.Float(x, y, width, height, startAngle, arcAngle, Arc2D.PIE);
		draw(shape, "fill");
	}

	/**
	 * Fills an oval.
	 * @since 0.1
	 */
	@Override
	public void fillOval(final int x, final int y, final int width, final int height) {
		final Shape shape = new Ellipse2D.Float(x, y, width, height);
		draw(shape, "fill");
	}

	/**
	 * Fills a polygon made with the specified points.
	 * @since 0.1
	 */
	@Override
	public void fillPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
		final Shape shape = new Polygon(xPoints, yPoints, nPoints);
		draw(shape, "fill");
	}

	/**
	 * Fills a polygon.
	 * @since 0.1
	 */
	@Override
	public void fillPolygon(final Polygon p) {
		draw(p, "fill");
	}

	/**
	 * Fills a rectangle with top-left corner placed at (x,y).
	 * @since 0.1
	 */
	@Override
	public void fillRect(final int x, final int y, final int width, final int height) {
		final Shape shape = new Rectangle(x, y, width, height);
		draw(shape, "fill");
	}

	/**
	 * Fills a rounded rectangle.
	 * @since 0.1
	 */
	@Override
	public void fillRoundRect(final int x, final int y, final int width, final int height, final int arcWidth, final int arcHeight) {
		final Shape shape = new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight);
		draw(shape, "fill");
	}

	/**
	 * Finalizes the object.
	 * @since 1.1.4
	 */
	@Override
	public void finalize() {
		super.finalize();
	}

	/**
	 * Finishes the EPS file being output to the underlying OutputStream. The
	 * OutputStream is automatically flushed before being closed. If you forget
	 * to do this, the file may be incomplete.
	 * @throws IOException
	 * @see org.sourceforge.jlibeps.epsgraphics.EpsDocument
	 * @since 0.1
	 */
	public void finish() throws IOException {
		flush();

		_document.finish();
	}

	/**
	 * Flushes the buffered contents of this EPS document to the underlying
	 * OutputStream it is being written to.
	 * @throws IOException
	 * @see org.sourceforge.jlibeps.epsgraphics.EpsDocument
	 * @since 0.1
	 */
	public void flush() throws IOException {
		_document.flush();
	}

	/**
	 * Returns whether accurate text mode is being used.
	 * This function is supplied for backward compatibility. Generally accepted
	 * coding guidelines say to use the "is" syntax when returning the value of
	 * a simple boolean. See the new isAccurateTextMode() function.
	 * @return true if Accurate Text Mode is set; false otherwise
	 * @since 0.1
	 */
	@Deprecated
	public boolean getAccurateTextMode() {
		return isAccurateTextMode();
	}

	/**
	 * Gets the background colour that is used by the clearRect method.
	 * @since 0.1
	 */
	@Override
	public Color getBackground() {
		return _backgroundColor;
	}

	/**
	 * Gets the current clipping area.
	 * @since 0.1
	 */
	@Override
	public Shape getClip() {
		if(_clip == null) {
			return null;
		}

		try {
			final AffineTransform transform = _transform.createInverse();
			transform.concatenate(_clipTransform);

			return transform.createTransformedShape(_clip);

		}catch(final NoninvertibleTransformException nte) {
			// Alert the client to a non-invertible transform via a library
			// encapsulated exception, but let the client choose whether to
			// print the stack trace or not.
			throw new EpsException(INVERSE_MATRIX_ERROR_MSG + _transform);
		}
	}

	/**
	 * Returns the bounding rectangle of the current clipping area.
	 * @see java.awt.Shape
	 * @since 0.1
	 */
	@Override
	public Rectangle getClipBounds() {
		if(_clip == null) {
			return null;
		}

		return getClip().getBounds();
	}

	/**
	 * Returns the bounding rectangle of the current clipping area.
	 * @see java.awt.Shape
	 * @since 0.1
	 */
	@Override
	public Rectangle getClipBounds(final Rectangle rectangle) {
		if(_clip == null) {
			return rectangle;
		}

		final Rectangle clipBounds = getClipBounds();
		rectangle.setLocation((int) clipBounds.getX(), (int) clipBounds.getY());
		rectangle.setSize((int) clipBounds.getWidth(), (int) clipBounds.getHeight());

		return rectangle;
	}

	/**
	 * Returns the current Color. This will be a default value (black) until it
	 * is changed using the setColor method.
	 * @since 0.1
	 */
	@Override
	public Color getColor() {
		return _color;
	}

	/**
	 * Returns the Color Mode used for all drawing operations.
	 * @return The Color Mode to use for the EPS document
	 * @since 1.1.4
	 */
	public ColorMode getColorMode() {
		return _colorMode;
	}

	/**
	 * returns the current Composite of the EpsGraphics2D object.
	 * @since 0.1
	 */
	@Override
	public Composite getComposite() {
		return _composite;
	}

	/**
	 * Returns the device configuration associated with this EpsGraphics2D
	 * object.
	 * @since 0.1
	 */
	@Override
	public GraphicsConfiguration getDeviceConfiguration() {
		final GraphicsConfiguration gc = null;
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final GraphicsDevice[] gds = ge.getScreenDevices();

		for(final GraphicsDevice gd : gds) {
			final GraphicsConfiguration[] gcs = gd.getConfigurations();

			if(gcs.length > 0) {
				return gcs[0];
			}
		}

		return gc;
	}

	/**
	 * Returns the Font currently being used.
	 * @since 0.1
	 */
	@Override
	public Font getFont() {
		return _font;
	}

	/**
	 * Gets the font metrics of the current font.
	 * @since 0.1
	 */
	@Override
	public FontMetrics getFontMetrics() {
		return getFontMetrics(getFont());
	}

	/**
	 * Gets the font metrics for the specified font.
	 * @since 0.1
	 */
	@Override
	public FontMetrics getFontMetrics(final Font font) {
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		final Graphics graphics = image.getGraphics();

		return graphics.getFontMetrics(font);
	}

	/**
	 * Returns the FontRenderContext.
	 * @since 0.1
	 */
	@Override
	public FontRenderContext getFontRenderContext() {
		return FONT_RENDER_CONTEXT;
	}

	/**
	 * Returns the current Paint of the EpsGraphics2D object.
	 * @since 0.1
	 */
	@Override
	public Paint getPaint() {
		return _paint;
	}

	/**
	 * Returns the value of a single preference for the rendering algorithms.
	 * Rendering hints are not used by EpsGraphics2D.
	 * @since 0.1
	 */
	@Override
	public Object getRenderingHint(final RenderingHints.Key hintKey) {
		return null;
	}

	/**
	 * Returns the preferences for the rendering algorithms.
	 * @since 0.1
	 */
	@Override
	public RenderingHints getRenderingHints() {
		return new RenderingHints(null);
	}

	/**
	 * Returns the Stroke currently used. Guaranteed to be an instance of
	 * BasicStroke.
	 * @since 0.1
	 */
	@Override
	public Stroke getStroke() {
		return _stroke;
	}

	/**
	 * Gets the AffineTransform used by this EpsGraphics2D.
	 * @since 0.1
	 */
	@Override
	public AffineTransform getTransform() {
		return new AffineTransform(_transform);
	}

	/**
	 * Checks whether or not the specified Shape intersects the specified
	 * Rectangle, which is in device space.
	 * @since 0.1
	 */
	@Override
	public boolean hit(final Rectangle rect, final Shape shape, final boolean onStroke) {
		return shape.intersects(rect);
	}

	/**
	 * Returns true if the specified rectangular area might intersect the
	 * current clipping area.
	 * @see java.awt.Shape
	 * @since 0.1
	 */
	@Override
	public boolean hitClip(final int x, final int y, final int width, final int height) {
		if(_clip == null) {
			return true;
		}

		final Rectangle rect = new Rectangle(x, y, width, height);
		return hit(rect, _clip, true);
	}

	/**
	 * Returns whether accurate text mode is being used.
	 * @return true if Accurate Text Mode is set; false otherwise
	 * @since 1.1.5
	 */
	public boolean isAccurateTextMode() {
		return _accurateTextMode;
	}

	/**
	 * Concatenates the current EpsGraphics2D Transform with a rotation
	 * transform.
	 * @since 0.1
	 */
	@Override
	public void rotate(final double theta) {
		rotate(theta, 0d, 0d);
	}

	/**
	 * Concatenates the current EpsGraphics2D Transform with a translated
	 * rotation transform.
	 * @since 0.1
	 */
	@Override
	public void rotate(final double theta, final double rotateX, final double rotateY) {
		transform(AffineTransform.getRotateInstance(theta, rotateX, rotateY));
	}

	/**
	 * Concatenates the current EpsGraphics2D Transform with a scaling
	 * transformation.
	 * @since 0.1
	 */
	@Override
	public void scale(final double scaleX, final double scaleY) {
		transform(AffineTransform.getScaleInstance(scaleX, scaleY));
	}

	/**
	 * Sets whether to use accurate text mode when rendering text in EPS. This
	 * is enabled (true) by default. When accurate text mode is used, all text
	 * will be rendered in EPS to appear exactly the same as it would do when
	 * drawn with a Graphics2D context. With accurate text mode enabled, it is
	 * not necessary for the EPS viewer to have the required font installed.
	 * Turning off accurate text mode will require the EPS viewer to have the
	 * necessary fonts installed. If you are using a lot of text, you will find
	 * that this significantly reduces the file size of your EPS documents.
	 * AffineTransforms can only affect the starting point of text using this
	 * simpler text mode - all text will be horizontal.
	 * @param accurateTextMode Set to true if Accurate Text Mode is desired; false otherwise
	 * @since 0.1
	 */
	public void setAccurateTextMode(final boolean accurateTextMode) {
		_accurateTextMode = accurateTextMode;

		if(!isAccurateTextMode()) {
			setFont(getFont());
		}
	}

	/**
	 * Sets the background colour to be used by the clearRect method.
	 * @since 0.1
	 */
	@Override
	public void setBackground(final Color color) {
		_backgroundColor = (color != null) ? color : Color.BLACK;
	}

	/**
	 * Sets the current clip to the rectangle specified by the given
	 * coordinates.
	 * @since 0.1
	 */
	@Override
	public void setClip(final int x, final int y, final int width, final int height) {
		setClip(new Rectangle(x, y, width, height));
	}

	/**
	 * Sets the current clipping area to an arbitrary clip shape.
	 * @see org.sourceforge.jlibeps.epsgraphics.EpsDocument
	 * @since 0.1
	 */
	@Override
	public void setClip(final Shape clip) {
		if(clip != null) {
			if(_document.isClipSet()) {
				append("grestore");
				append("gsave");
			}else {
				_document.setClipSet(true);
				append("gsave");
			}

			draw(clip, "clip");
			_clip = clip;
			_clipTransform = (AffineTransform) _transform.clone();
		}else {
			if(_document.isClipSet()) {
				append("grestore");
				_document.setClipSet(false);
			}

			_clip = null;
		}
	}

	/**
	 * Sets the Color to be used when drawing all future shapes, text, etc.
	 * @since 0.1
	 */
	@Override
	public void setColor(final Color color) {
		_color = (color != null) ? color : Color.BLACK;

		final ColorMode colorMode = getColorMode();
		switch(colorMode) {
			case BLACK_AND_WHITE:
				float bwValue = 0;

				if((_color.getRed() + _color.getGreen() + _color.getBlue()) > ((255 * 1.5) - 1)) {
					bwValue = 1;
				}

				append(bwValue + " setgray");

				break;
			case GRAYSCALE:
				final float grayValue = ((_color.getRed() + _color.getGreen() + _color.getBlue()) / (3 * 255f));

				append(grayValue + " setgray");

				break;
			case COLOR_RGB:
				append((_color.getRed() / 255f) + " " + (_color.getGreen() / 255f) + " " + (_color.getBlue() / 255f) + " setrgbcolor");

				break;
			case COLOR_CMYK:
				if(Color.BLACK.equals(_color)) {
					append("0.0 0.0 0.0 1.0 setcmykcolor");
				}else {
					final double c = 1 - (_color.getRed() / 255f);
					final double m = 1 - (_color.getGreen() / 255f);
					final double y = 1 - (_color.getBlue() / 255f);
					final double k = Math.min(Math.min(c, y), m);

					append(((c - k) / (1 - k)) + " " + ((m - k) / (1 - k)) + " " + ((y - k) / (1 - k)) + " " + k + " setcmykcolor");
				}

				break;
			default:
				break;
		}
	}

	/**
	 * Sets the Color Mode to use when drawing on the document.
	 * @param colorMode The Color Mode to use for the EPS document
	 * @since 1.1.4
	 */
	public void setColorMode(final ColorMode colorMode) {
		_colorMode = colorMode;
	}

	/**
	 * Sets the Composite to be used by this EpsGraphics2D. EpsGraphics2D does
	 * not make use of these.
	 * @since 0.1
	 */
	@Override
	public void setComposite(final Composite comp) {
		_composite = comp;
	}

	public void setDefaults() {
		_backgroundColor = Color.WHITE;
		_clip = null;
		_clipTransform = new AffineTransform();
		_transform = new AffineTransform();
		_accurateTextMode = true;

		setStroke(new BasicStroke());
		setColor(Color.BLACK);
		setPaint(Color.BLACK);
		setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
		setFont(Font.decode(null));
	}

	/**
	 * Sets the Font to be used in future text.
	 * @since 0.1
	 */
	@Override
	public void setFont(final Font font) {
		_font = (font != null) ? font : Font.decode(null);

		if(!isAccurateTextMode()) {
			append("/" + _font.getPSName() + " findfont " + _font.getSize() + " scalefont setfont");
		}
	}

	/**
	 * Sets the Paint attribute for the EpsGraphics2D object. Only Paint objects
	 * of type Color are respected by EpsGraphics2D.
	 * @since 0.1
	 */
	@Override
	public void setPaint(final Paint paint) {
		_paint = paint;

		if(paint instanceof Color) {
			setColor((Color) paint);
		}
	}

	/**
	 * Sets the paint mode of this EpsGraphics2D object to overwrite the
	 * destination EpsDocument with the current colour.
	 * @since 0.1
	 */
	@Override
	public void setPaintMode() {
		// Do nothing - paint mode is the only method supported anyway.
	}

	/**
	 * Sets a rendering hint. These are not used by EpsGraphics2D.
	 * @since 0.1
	 */
	@Override
	public void setRenderingHint(final RenderingHints.Key hintKey, final Object hintValue) {
		// Do nothing.
	}

	/**
	 * Sets the rendering hints. These are ignored by EpsGraphics2D.
	 * @since 0.1
	 */
	@Override
	public void setRenderingHints(final Map<?, ?> hints) {
		// Do nothing.
	}

	/**
	 * Sets the current stroke.
	 * @since 0.1
	 */
	@Override
	public void setStroke(final Stroke stroke) {
		if(stroke instanceof BasicStroke) {
			_stroke = (BasicStroke) stroke;
		}else {
			// Alert the client to an invalid stroke via a library encapsulated
			// exception, but let the client choose whether to print the stack
			// trace or not.
			throw new EpsException(STROKE_CLASS_ERROR_MSG + stroke);
		}
	}

	/**
	 * Sets the AffineTransform to be used by this EpsGraphics2D.
	 * @since 0.1
	 */
	@Override
	public void setTransform(final AffineTransform transform) {
		if(transform == null) {
			_transform = new AffineTransform();
		}else {
			_transform = new AffineTransform(transform);
		}

		// Need to update the stroke and font so they know the scale changed.
		setStroke(getStroke());
		setFont(getFont());
	}

	/**
	 * <b><i><font color="red">Not implemented</font></i></b> - performs no
	 * action.
	 * @since 0.1
	 */
	@Override
	public void setXORMode(final Color color) {
		methodNotSupported();
	}

	/**
	 * Concatenates the current EpsGraphics2D Transform with a shearing
	 * transform.
	 * @since 0.1
	 */
	@Override
	public void shear(final double shearX, final double shearY) {
		transform(AffineTransform.getShearInstance(shearX, shearY));
	}

	/**
	 * Returns the entire contents of the EPS document, complete with header,
	 * footer, and bounding box (but no content). The returned String is
	 * suitable for being written directly to disk as an EPS file.
	 * This function should probably not be used anymore, as the dependencies
	 * are not carefully managed and as it seems designed to be used only when
	 * making use of this library in "in-memory mode" vs. "direct-to-disc mode",
	 * even though the previous and current implementations do not enforce this
	 * rigorously.
	 * @see org.sourceforge.jlibeps.epsgraphics.EpsDocument
	 * @since 0.1
	 */
	@Deprecated
	@Override
	public String toString() {
		if(_document == null) {
			return null;
		}

		final StringWriter writer = new StringWriter();

		try {
			_document.write(writer);
			_document.flush();
			_document.close();
		}catch(final Exception e) {
			// Re-cast the exception using library encapsulation, but let the
			// client choose whether to print the stack trace or not.
			throw new EpsException(OUTPUT_ERROR_MSG + e.getLocalizedMessage());
		}

		return writer.toString();
	}

	/**
	 * Composes an AffineTransform object with the Transform in this
	 * EpsGraphics2D according to the rule last-specified-first-applied.
	 * @since 0.1
	 */
	@Override
	public void transform(final AffineTransform transform) {
		_transform.concatenate(transform);
		setTransform(getTransform());
	}

	/**
	 * Returns the point after it has been transformed by the transformation.
	 * @param x The x-coordinate of the point to transform to the page layout
	 * @param y The y-coordinate of the point to transform to the page layout
	 * @return The transformed point, which has applied the page layout offsets
	 * @see java.awt.geom.AffineTransform
	 * @since 0.1
	 */
	private Point2D transform(final float x, final float y) {
		Point2D result = new Point2D.Float(x, y);
		result = _transform.transform(result, result);
		result.setLocation(result.getX(), -result.getY());
		return result;
	}

	/**
	 * Concatenates the current EpsGraphics2D Transformation with a translation
	 * transform.
	 * @since 0.1
	 */
	@Override
	public void translate(final double translateX, final double translateY) {
		transform(AffineTransform.getTranslateInstance(translateX, translateY));
	}

	/**
	 * Translates the origin of the EpsGraphics2D context to the point (x,y) in
	 * the current coordinate system.
	 * @since 0.1
	 */
	@Override
	public void translate(final int translateX, final int translateY) {
		translate((double) translateX, (double) translateY);
	}
}
