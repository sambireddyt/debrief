// Copyright MWC 1999, Debrief 3 Project
// $RCSfile$
// @author $Author$
// @version $Revision$
// $Log$
// Revision 1.3  2005-05-24 07:35:57  Ian.Mayo
// Ignore anti-alias bits, sort out text-writing in filling areas
//
// Revision 1.2  2005/05/20 15:34:44  Ian.Mayo
// Hey, practically working!
//
// Revision 1.1  2005/05/20 13:45:03  Ian.Mayo
// Start doing chart
//
//

package org.mwc.cmap.plotViewer.editors.chart;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.core.property_support.ColorHelper;
import org.mwc.cmap.core.property_support.FontHelper;

import MWC.Algorithms.PlainProjection;
import MWC.Algorithms.Projections.FlatProjection;
import MWC.GUI.CanvasType;
import MWC.GUI.Editable;
import MWC.GUI.Properties.BoundedInteger;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

/**
 * Swing implementation of a canvas.
 */
public class SWTCanvas implements CanvasType, Serializable, Editable
{

	// ///////////////////////////////////////////////////////////
	// member variables
	// //////////////////////////////////////////////////////////

	org.eclipse.swt.widgets.Canvas _myCanvas = null;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * the projection in use
	 */
	private PlainProjection _theProjection;

	/**
	 * our graphics object - only valid between 'start' and 'stop' paint events.
	 */
	private GC _theDest = null;

	/**
	 * the list of registered painters for this canvas.
	 */
	private Vector _thePainters;

	/**
	 * the dimensions of the canvas - we keep our own track of this in order to
	 * handle the number of resize messages we get.
	 */
	private java.awt.Dimension _theSize;

	/**
	 * our double-buffering safe copy.
	 */
	private transient Image _dblBuff;

	/**
	 * our tool tip handler.
	 */
	private CanvasType.TooltipHandler _tooltipHandler;

	/**
	 * our editor.
	 */
	transient private Editable.EditorType _myEditor;

	/**
	 * a list of the line-styles we know about.
	 */
	static private java.util.HashMap _myLineStyles = null;

	/**
	 * the current line width.
	 */
	private float _lineWidth;

	// ///////////////////////////////////////////////////////////
	// constructor
	// //////////////////////////////////////////////////////////

	/**
	 * default constructor.
	 */
	public SWTCanvas(Composite parent, int style)
	{
		_myCanvas = new Canvas(parent, style);

		// start with our background colour
		setBackgroundColor(java.awt.Color.red);

		// initialisation
		_thePainters = new Vector(0, 1);

		// create our projection
		_theProjection = new FlatProjection();

		// add handler to catch canvas resizes
		_myCanvas.addControlListener(new ControlAdapter()
		{

			public void controlResized(final ControlEvent e)
			{
				Point pt = _myCanvas.getSize();
				Dimension dim = new Dimension(pt.x, pt.y);
				setScreenSize(dim);
			}
		});

		// switch on tooltips for this panel
		_myCanvas.setToolTipText("blank");

		// setup our own painter
		_myCanvas.addPaintListener(new org.eclipse.swt.events.PaintListener()
		{

			public void paintControl(PaintEvent e)
			{				
				repaintMe(e);
			}
		});
	}

	protected void repaintMe(PaintEvent pe)
	{
		
		// get the graphics destination
		GC gc = pe.gc;
		
		// put double-buffering code in here.
		if (_dblBuff == null)
		{
			// ok, create the new image
			Point theSize = _myCanvas.getSize();

			if ((theSize.x == 0) || (theSize.y == 0))
				return;

			_dblBuff = new Image(Display.getCurrent(), theSize.x, theSize.y);
			GC theDest = new GC(_dblBuff);

			// and paint into it
			paintPlot(theDest);

		}

		// finally put the required bits of the target image onto the screen
		gc.drawImage(_dblBuff, pe.x, pe.y, pe.width, pe.height,pe.x, pe.y, pe.width, pe.height);

	}

	// ///////////////////////////////////////////////////////////
	// member functions
	// //////////////////////////////////////////////////////////

	// ///////////////////////////////////////////////////////////
	// projection related
	// //////////////////////////////////////////////////////////
	/**
	 * update the projection.
	 */
	public final void setProjection(final PlainProjection theProjection)
	{
		_theProjection = theProjection;
	}

	/**
	 * switch anti-aliasing on or off.
	 * 
	 * @param val
	 *          yes/no
	 */
	private void switchAntiAliasOn(final boolean val)
	{
		// todo: sort out this bit..
		// // ignore this
		// final Graphics2D g2 = (Graphics2D) _theDest;
		//
		// if (g2 == null)
		// return;
		//

//		try
//		{
//			if (val)
//			{
//				 _theDest.setAntialias(SWT.ON);
//			}
//			else
//			{
//				 _theDest.setAntialias(SWT.OFF);
//			}
//		} catch (RuntimeException e)
//		{
////			CorePlugin.logError(Status.ERROR, "Graphics library not found", e);
//		}
	}

	/**
	 * get the current projection.
	 */
	public final PlainProjection getProjection()
	{
		return _theProjection;
	}

	/**
	 * convenience function.
	 */
	public final java.awt.Point toScreen(final WorldLocation val)
	{
		return _theProjection.toScreen(val);
	}

	/**
	 * convenience function.
	 */
	public final WorldLocation toWorld(final java.awt.Point val)
	{
		return _theProjection.toWorld(val);
	}

	/**
	 * re-determine the area of data we cover. then resize to cover it
	 */
	public final void rescale()
	{

		// get the data area for the current painters
		WorldArea theArea = null;
		final Enumeration enumer = _thePainters.elements();
		while (enumer.hasMoreElements())
		{
			final CanvasType.PaintListener thisP = (CanvasType.PaintListener) enumer
					.nextElement();
			final WorldArea thisArea = thisP.getDataArea();
			if (thisArea != null)
			{
				if (theArea == null)
					theArea = new WorldArea(thisArea);
				else
					theArea.extend(thisArea);
			}
		}

		// check we have found a valid area
		if (theArea != null)
		{
			// so, we now have the data area for everything which
			// wants to plot to it, give it to the projection
			_theProjection.setDataArea(theArea);

			// get the projection to refit-itself
			_theProjection.zoom(0.0);
		}

	}

	// public final void setSize(final int p1, final int p2)
	// {
	// // ok, store the dimension
	// _theSize = new Dimension(p1, p2);
	//
	// _myCanvas.setSize(p1, p2);
	//  	
	// // reset our double buffer, since we've changed size
	// _dblBuff = null;
	// }
	//  
	/**
	 * handler for a screen resize - inform our projection of the resize then
	 * inform the painters.
	 */
	private void setScreenSize(final java.awt.Dimension p1)
	{
		Dimension theDim = p1;
		// check if this is a real resize
		if ((_theSize == null) || (!_theSize.equals(p1)))
		{

			// ok, now remember it
			_theSize = p1;

			// and pass it onto the projection
			_theProjection.setScreenArea(p1);

			// inform our parent
			_myCanvas.setSize(p1.width, p1.height);

			// erase the double buffer, (if we have one)
			// since it is now invalid
			if (_dblBuff != null)
			{
				_dblBuff.dispose();
				_dblBuff = null;
			}

			// inform the listeners that we have resized
			final Enumeration enumer = _thePainters.elements();
			while (enumer.hasMoreElements())
			{
				final CanvasType.PaintListener thisPainter = (CanvasType.PaintListener) enumer
						.nextElement();
				thisPainter.resizedEvent(_theProjection, p1);
			}

		}
	}

	// ///////////////////////////////////////////////////////////
	// graphics plotting related
	// //////////////////////////////////////////////////////////
	/**
	 * find out the current metrics.
	 * 
	 * @param theFont
	 *          the font to try
	 * @return the metrics object
	 */
	// public final java.awt.FontMetrics getFontMetrics(final java.awt.Font
	// theFont)
	// {
	// java.awt.FontMetrics res = null;
	//
	// if (_theDest != null)
	// {
	// if (theFont != null)
	// res = _theDest.getFontMetrics(theFont);
	// else
	// res = _theDest.getFontMetrics();
	// }
	//
	// return res;
	// }
	public final int getStringHeight(final java.awt.Font theFont)
	{
		int res = 0;
		// final java.awt.FontMetrics fm = getFontMetrics(theFont);
		// if (fm != null)
		// res = fm.getHeight();

		res = _theDest.getFontMetrics().getHeight();

		return res;
	}

	public final int getStringWidth(final java.awt.Font theFont,
			final String theString)
	{
		int res = 0;

		res = _theDest.getFontMetrics().getAverageCharWidth() * theString.length();

		// final java.awt.FontMetrics fm = getFontMetrics(theFont);
		// if (fm != null)
		// res = fm.stringWidth(theString);

		return res;
	}

	/**
	 * ONLY USE THIS FOR NON-PERSISTENT PLOTTING
	 */
	public final java.awt.Graphics getGraphicsTemp()
	{
		System.err.println("graphics temp not implemented...");
		java.awt.Graphics res = null;
		// /** if we are in a paint operation already,
		// * return the graphics object, since it may
		// * be a double-buffering image
		// */
		// if (_theDest != null)
		// {
		// res = _theDest.create(); // return a copy, so the user can dispose it
		// }
		// else
		// {
		// if (_dblBuff != null)
		// {
		// res = _dblBuff.getGraphics();
		// }
		// else
		// {
		// }
		// }
		//
		return res;
	}

	public final void setFont(final java.awt.Font theFont)
	{
		// super.setFont(theFont);
	}

	public final boolean drawImage(final java.awt.Image img, final int x,
			final int y, final int width, final int height,
			final ImageObserver observer)
	{
		if (_theDest == null)
			return true;

		// return _theDest.drawImage(img, x, y, width, height, observer);

		return false;

	}

	public final void drawLine(final int x1, final int y1, final int x2,
			final int y2)
	{
		if (_theDest == null)
			return;

		// doDecide whether to anti-alias this line
		this.switchAntiAliasOn(SWTCanvas.antiAliasThisLine(this.getLineWidth()));

		// check that the points are vaguely plottable
		if ((Math.abs(x1) > 9000) || (Math.abs(y1) > 9000) || (Math.abs(x2) > 9000)
				|| (Math.abs(y2) > 9000))
		{
			return;
		}

		// double-check
		if (_theDest == null)
			return;

		_theDest.drawLine(x1, y1, x2, y2);
	}

	/**
	 * draw a filled polygon
	 * 
	 * @param xPoints
	 *          list of x coordinates
	 * @param yPoints
	 *          list of y coordinates
	 * @param nPoints
	 *          length of list
	 */
	public final void fillPolygon(final int[] xPoints, final int[] yPoints,
			final int nPoints)
	{
		if (_theDest == null)
			return;

		// translate the polygon to SWT format
		int[] poly = getPolygonArray(xPoints, yPoints, nPoints);

		_theDest.fillPolygon(poly);
	}

	private int[] getPolygonArray(int[] xPoints, int[] yPoints, int nPoints)
	{
		int[] poly = new int[nPoints * 2];

		for (int i = 0; i < nPoints; i++)
		{
			poly[i] = xPoints[i];
			poly[i + 1] = yPoints[i];
		}

		return poly;
	}

	/**
	 * drawPolyline
	 * 
	 * @param xPoints
	 *          list of x coordinates
	 * @param yPoints
	 *          list of y coordinates
	 * @param nPoints
	 *          length of list
	 */
	public final void drawPolyline(final int[] xPoints, final int[] yPoints,
			final int nPoints)
	{
		if (_theDest == null)
			return;

		// doDecide whether to anti-alias this line
		this.switchAntiAliasOn(SWTCanvas.antiAliasThisLine(this.getLineWidth()));

		// translate the polygon to SWT format
		int[] poly = getPolygonArray(xPoints, yPoints, nPoints);

		_theDest.drawPolyline(poly);
	}

	/**
	 * drawPolygon.
	 * 
	 * @param xPoints
	 *          list of x coordinates
	 * @param yPoints
	 *          list of y coordinates
	 * @param nPoints
	 *          length of list
	 */
	public final void drawPolygon(final int[] xPoints, final int[] yPoints,
			final int nPoints)
	{
		if (_theDest == null)
			return;

		// doDecide whether to anti-alias this line
		this.switchAntiAliasOn(SWTCanvas.antiAliasThisLine(this.getLineWidth()));

		// translate the polygon to SWT format
		int[] poly = getPolygonArray(xPoints, yPoints, nPoints);

		_theDest.drawPolygon(poly);
	}

	public final void drawOval(final int x, final int y, final int width,
			final int height)
	{
		if (_theDest != null)
			this.switchAntiAliasOn(SWTCanvas.antiAliasThisLine(this.getLineWidth()));

		if (_theDest != null)
			_theDest.drawOval(x, y, width, height);
	}

	public final void fillOval(final int x, final int y, final int width,
			final int height)
	{
		if (_theDest != null)
			_theDest.fillOval(x, y, width, height);
		// else
		// MWC.Utilities.Errors.Trace.trace("Graphics object not available when
		// painting oval - occasionally happens in first pass", false);
	}

	public final void setColor(final java.awt.Color theCol)
	{
		if (_theDest == null)
			return;

		// transfer the color
		Color newCol = ColorHelper.getColor(theCol);

		_theDest.setForeground(newCol);
	}

	static public java.awt.BasicStroke getStrokeFor(final int style)
	{
		if (_myLineStyles == null)
		{
			_myLineStyles = new java.util.HashMap(5);
			_myLineStyles.put(new Integer(MWC.GUI.CanvasType.SOLID),
					new java.awt.BasicStroke(1, java.awt.BasicStroke.CAP_BUTT,
							java.awt.BasicStroke.JOIN_MITER, 1, new float[] { 5, 0 }, 0));
			_myLineStyles.put(new Integer(MWC.GUI.CanvasType.DOTTED),
					new java.awt.BasicStroke(1, java.awt.BasicStroke.CAP_BUTT,
							java.awt.BasicStroke.JOIN_MITER, 1, new float[] { 2, 6 }, 0));
			_myLineStyles.put(new Integer(MWC.GUI.CanvasType.DOT_DASH),
					new java.awt.BasicStroke(1, java.awt.BasicStroke.CAP_BUTT,
							java.awt.BasicStroke.JOIN_MITER, 1, new float[] { 4, 4, 12, 4 },
							0));
			_myLineStyles.put(new Integer(MWC.GUI.CanvasType.SHORT_DASHES),
					new java.awt.BasicStroke(1, java.awt.BasicStroke.CAP_BUTT,
							java.awt.BasicStroke.JOIN_MITER, 1, new float[] { 6, 6 }, 0));
			_myLineStyles.put(new Integer(MWC.GUI.CanvasType.LONG_DASHES),
					new java.awt.BasicStroke(1, java.awt.BasicStroke.CAP_BUTT,
							java.awt.BasicStroke.JOIN_MITER, 1, new float[] { 12, 6 }, 0));
			_myLineStyles.put(new Integer(MWC.GUI.CanvasType.UNCONNECTED),
					new java.awt.BasicStroke(1));
		}

		return (java.awt.BasicStroke) _myLineStyles.get(new Integer(style));
	}

	public final void setLineStyle(final int style)
	{
		_theDest.setLineStyle(style);
		// final java.awt.BasicStroke stk = getStrokeFor(style);
		// final java.awt.Graphics2D g2 = (java.awt.Graphics2D) _theDest;
		// g2.setStroke(stk);
	}

	/**
	 * set the width of the line, in pixels
	 */
	public final void setLineWidth(float width)
	{
		// check we've got a valid width
		width = Math.max(width, 0);

		_lineWidth = width;

		// are we currently in a plot operation?
		if (_theDest != null)
		{
			// create the stroke
			// final java.awt.BasicStroke stk = new BasicStroke(width);
			// final java.awt.Graphics2D g2 = (java.awt.Graphics2D) _theDest;
			// g2.setStroke(stk);
			_theDest.setLineWidth((int) width);
		}
	}

	/**
	 * get the width of the line, in pixels
	 */
	public final float getLineWidth()
	{
		final float res;

		// are we currently in a plot operation?
		if (_theDest != null)
		{
			// create the stroke
			res = _theDest.getLineWidth();
			// final java.awt.Graphics2D g2 = (java.awt.Graphics2D) _theDest;
			// final BasicStroke bs = (BasicStroke) g2.getStroke();
			// res = bs.getLineWidth();
		}
		else
		{
			res = _lineWidth;
		}

		return res;
	}

	public final void drawArc(final int x, final int y, final int width,
			final int height, final int startAngle, final int arcAngle)
	{
		if (_theDest != null)
		{
			// doDecide whether to anti-alias this line
			this.switchAntiAliasOn(SWTCanvas.antiAliasThisLine(this.getLineWidth()));
		}

		if (_theDest != null)
		{
			_theDest.drawArc(x, y, width, height, startAngle, arcAngle);
		}
	}

	public final void fillArc(final int x, final int y, final int width,
			final int height, final int startAngle, final int arcAngle)
	{
		if (_theDest != null)
			_theDest.fillArc(x, y, width, height, startAngle, arcAngle);
		// else
		// MWC.Utilities.Errors.Trace.trace("Graphics object not available when
		// painting oval - occasionally happens in first pass", false);

	}

	public final void startDraw(final Object theVal)
	{
		_theDest = (GC) theVal;

		// set the thickness
		// final BasicStroke bs = new BasicStroke(_lineWidth);
		// final Graphics2D g2 = (Graphics2D) _theDest;
		// g2.setStroke(bs);
	}

	public final void endDraw(final Object theVal)
	{
		_theDest = null;
	}

	public void drawText(final String theStr, final int x, final int y)
	{
		if (_theDest == null)
			return;

		drawText(theStr, x, y);
	}

	public void drawText(final java.awt.Font theFont, final String theStr,
			final int x, final int y)
	{
		if (_theDest == null)
			return;

		// todo: use the font information

		// doDecide the anti-alias
		this.switchAntiAliasOn(SWTCanvas.antiAliasThis(theFont));

		org.eclipse.swt.graphics.Font swtFont = FontHelper.convertFont(theFont);
		_theDest.setFont(swtFont);
		_theDest.drawString(theStr, x, y, true);
	}

	public final void drawRect(final int x1, final int y1, final int wid,
			final int height)
	{
		if (_theDest == null)
			return;

		// doDecide whether to anti-alias this line
		this.switchAntiAliasOn(SWTCanvas.antiAliasThisLine(this.getLineWidth()));

		if (_theDest == null)
			return;

		_theDest.drawRectangle(x1, y1, wid, height);
	}

	public final void fillRect(final int x, final int y, final int wid,
			final int height)
	{
		if (_theDest == null)
			return;

		fillOn();
		
		_theDest.fillRectangle(x, y, wid, height);
		
		fillOff();
	}
	
	private static Color _theOldColor;
	
	protected void fillOn()
	{
		_theOldColor = _myCanvas.getBackground();
		Color theForeColor = _myCanvas.getForeground();
		_myCanvas.setBackground(theForeColor);
	}
	
	protected void fillOff()
	{
		_myCanvas.setBackground(_theOldColor);
		_theOldColor = null;
	}

	/**
	 * get the current background colour
	 */
	public final java.awt.Color getBackgroundColor()
	{
		Color swtCol = _myCanvas.getBackground();
		java.awt.Color theCol = ColorHelper.convertColor(swtCol.getRGB());
		// convert to java-color
		return theCol;
	}

	/**
	 * set the current background colour, and trigger a screen update
	 */
	public final void setBackgroundColor(final java.awt.Color theColor)
	{
		Color swtCol = ColorHelper.getColor(theColor);

		// set the colour in the parent
		_myCanvas.setBackground(swtCol);

		// invalidate the screen
		updateMe();
	}

	public final BoundedInteger getLineThickness()
	{
		return new BoundedInteger((int) this.getLineWidth(), 0, 4);
	}

	public final void setLineThickness(final BoundedInteger val)
	{
		setLineWidth(val.getCurrent());
	}

	// /////////////////////////////////////////////////////////
	// handle tooltip stuff
	// /////////////////////////////////////////////////////////
	public final void setTooltipHandler(final CanvasType.TooltipHandler handler)
	{
		_tooltipHandler = handler;
	}

	/**
	 * get a string describing the current screen & world location
	 */
	public final String getToolTipText(final MouseEvent p1)
	{
		String res = null;
		if (_tooltipHandler != null)
		{

			final java.awt.Point pt = p1.getPoint();
			// check we have a valid projection
			final java.awt.Dimension dim = getProjection().getScreenArea();
			if (dim != null)
			{
				if (dim.width > 0)
				{
					final WorldLocation loc = toWorld(pt);
					if (loc != null)
						res = _tooltipHandler.getString(loc, pt);
				}
			}
		}

		return res;
	}

	// //////////////////////////////////////////////////////////
	// painter handling
	// //////////////////////////////////////////////////////////
	public final void addPainter(final CanvasType.PaintListener listener)
	{
		_thePainters.addElement(listener);
	}

	public final void removePainter(final CanvasType.PaintListener listener)
	{
		_thePainters.removeElement(listener);
	}

	public final Enumeration getPainters()
	{
		return _thePainters.elements();
	}

	// ////////////////////////////////////////////////////
	// screen redraw related
	// ////////////////////////////////////////////////////
	//
	// public final void paint(final java.awt.Graphics p1)
	// {
	// // paint code moved to Update function
	// update(p1);
	// }
	//
	// /**
	// * screen redraw, just repaint the buffer
	// */
	// public void update(final java.awt.Graphics p1)
	// {
	// // this is a screen redraw, we can just paint in the buffer
	// // (although we may have to redraw it first)
	//
	// if (_dblBuff == null)
	// {
	// paintPlot();
	// }
	//
	// // // and paste the image
	// // p1.drawImage(_dblBuff, 0, 0, this);
	//
	// }

	/**
	 * method to produce the buffered image - we paint this buffered image when we
	 * get one of the numerous Windows repaint calls
	 */
	private void paintPlot(GC g1)
	{
		System.out.println("doing paint.");

		// prepare the ground (remember the graphics dest for a start)
		startDraw(g1);

		// erase background
		final Dimension sz = this.getSize();

//		g1.setForeground(_myCanvas.getBackground());
//		g1.fillRectangle(0, 0, sz.width, sz.height);

		// do the actual paint
		paintIt(this);

		System.out.println("repainting");

		// all finished, close it now
		endDraw(null);

		// and dispose
		// g1.dispose();

		// put the image back in our buffer
		// _dblBuff = tmpBuff;

	}

	/**
	 * the real paint function, called when it's not satisfactory to just paint in
	 * our safe double-buffered image.
	 */
	public final void paintIt(final CanvasType canvas)
	{
		// go through our painters
		final Enumeration enumer = _thePainters.elements();
		while (enumer.hasMoreElements())
		{
			final CanvasType.PaintListener thisPainter = (CanvasType.PaintListener) enumer
					.nextElement();

			if (canvas == null)
			{
				System.out.println("Canvas not ready yet");
			}
			else
			{
				// check the screen has been defined
				final Dimension area = this.getProjection().getScreenArea();
				if ((area == null) || (area.getWidth() <= 0) || (area.getHeight() <= 0))
				{
					return;
				}

				// it must be ok
				thisPainter.paintMe(canvas);
			}

		}
	}

	/**
	 * first repaint the plot, then trigger a screen update
	 */
	public final void updateMe()
	{
		if (_dblBuff != null)
		{
			_dblBuff.dispose();
			_dblBuff = null;
		}

		_myCanvas.redraw();
	}

	// ////////////////////////////////////////////////////
	// bean/editable methods
	// ///////////////////////////////////////////////////
	public final Editable.EditorType getInfo()
	{
		if (_myEditor == null)
			_myEditor = new CanvasInfo(this);

		return _myEditor;
	}

	public final boolean hasEditor()
	{
		return true;
	}

	/**
	 * provide close method, clear elements.
	 */
	public final void close()
	{
		_thePainters.removeAllElements();
		_thePainters = null;
		_theProjection = null;
		_theDest = null;
		_theSize = null;
		_dblBuff = null;
		_tooltipHandler = null;
	}

	/**
	 * return our name (used in editing)
	 */
	public final String toString()
	{
		return "Appearance";
	}

	// ////////////////////////////////////////////////////
	// bean info for this class
	// ///////////////////////////////////////////////////
	public final class CanvasInfo extends Editable.EditorType
	{

		public CanvasInfo(final SWTCanvas data)
		{
			super(data, data.toString(), "");
		}

		public final PropertyDescriptor[] getPropertyDescriptors()
		{
			try
			{
				final PropertyDescriptor[] res = {
						prop("BackgroundColor", "the background color"),
						prop("LineThickness", "the line thickness"), };

				return res;

			} catch (IntrospectionException e)
			{
				return super.getPropertyDescriptors();
			}
		}
	}

	// ////////////////////////////////////////////////
	// methods to support anti-alias decisions
	// ////////////////////////////////////////////////

	/**
	 * do we anti-alias this font.
	 * 
	 * @param theFont
	 *          the font we are looking at
	 * @return yes/no decision
	 */
	private static boolean antiAliasThis(final Font theFont)
	{
		boolean res = false;

		final int size = theFont.getSize();
		final boolean isBold = theFont.isBold();

		if (size >= 14)
		{
			res = true;
		}
		else
		{
			if (isBold && (size >= 12))
			{
				res = true;
			}
		}

		return res;
	}

	/**
	 * doDecide whether this line thickness could be anti-aliased.
	 * 
	 * @param width
	 *          the line width setting
	 * @return yes/no
	 */
	private static boolean antiAliasThisLine(final float width)
	{
		boolean res = false;

		if (width > 1)
			res = true;

		return res;
	}

	public String getName()
	{
		// TODO Auto-generated method stub
		return "SWT Canvas";
	}

	public Dimension getSize()
	{
		return _theSize;
	}

	public void redraw(int x, int y, int width, int height, boolean b)
	{
		_myCanvas.redraw(x, y, width, height, b);
	}

	public void addControlListener(ControlAdapter adapter)
	{
		_myCanvas.addControlListener(adapter);
	}

	public void addMouseMoveListener(MouseMoveListener listener)
	{
		_myCanvas.addMouseMoveListener(listener);
	}

	public void addMouseListener(MouseListener listener)
	{
		_myCanvas.addMouseListener(listener);
	}

	public Control getCanvas()
	{
		return _myCanvas;
	}

}
