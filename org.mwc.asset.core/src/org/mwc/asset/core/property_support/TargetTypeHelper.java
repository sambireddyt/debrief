/*
 *    Debrief - the Open Source Maritime Analysis Application
 *    http://debrief.info
 *
 *    (C) 2000-2014, PlanetMayo Ltd
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the Eclipse Public License v1.0
 *    (http://www.eclipse.org/legal/epl-v10.html)
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 */
package org.mwc.asset.core.property_support;

import java.text.DecimalFormat;
import java.util.*;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.List;
import org.mwc.cmap.core.property_support.EditorHelper;

import ASSET.Models.Decision.TargetType;
import ASSET.Participants.Category;

public class TargetTypeHelper extends EditorHelper
{

	/**
	 * remember how to format items on line
	 */
	protected static DecimalFormat _floatFormat = new DecimalFormat("0.0000");

	/**
	 * constructor. just declare our object type
	 */
	public TargetTypeHelper()
	{
		super(TargetType.class);
	}

	/**
	 * we define a custom cell editor just to get the "Paste" button
	 * 
	 * @param parent
	 * @return
	 */
	public CellEditor getCellEditorFor(final Composite cellParent)
	{
		final DialogCellEditor res = new TargetTypeCellEditor(cellParent);

		return res;
	}

	public ILabelProvider getLabelFor(final Object currentValue)
	{
		final ILabelProvider label1 = new LabelProvider()
		{
			public String getText(final Object element)
			{
				final TargetType val = (TargetType) element;
				return val.toString();
			}

			public Image getImage(final Object element)
			{
				return null;
			}

		};
		return label1;
	}

	/**
	 * custom cell editor which re-purposes button used to open dialog as a paste
	 * button
	 * 
	 * @author ian.mayo
	 */
	protected static class TargetTypeDialog extends org.eclipse.jface.dialogs.Dialog
	{

		/**
		 * and the drop-down units bit
		 */
		List _myForce;

		/**
		 * and the drop-down units bit
		 */
		List _myEnvironment;

		/**
		 * and the drop-down units bit
		 */
		List _myType;

		final TargetType _current;

		/**
		 * our tooltips
		 */
		final private String _forceTip = "the force of the subject participant(s)";

		final private String _environmentTip = "the environment of the subject participant(s)";

		final private String _typeTip = "the type of the subject participant(s)";

		private TargetType _newResult;

		public TargetTypeDialog(final TargetType current, final Shell parent)
		{
			super(parent);
			_current = current;
		}
		
	   protected void configureShell(final Shell newShell) {
	      super.configureShell(newShell);
	      newShell.setText("Select Target Types");
	   }

		/**
		 * @param parent
		 * @return the contents of the dialog (our controls)
		 */
		protected Control createDialogArea(final Composite parent)
		{
			final Composite composite = (Composite) super.createDialogArea(parent);

			final GridLayout thisLayout = new GridLayout();
			thisLayout.numColumns = 3;
			thisLayout.makeColumnsEqualWidth = true;

			composite.setLayout(thisLayout);
			final Label label1 = new Label(composite, SWT.NONE);
			label1.setText("Force");

			final Label label2 = new Label(composite, SWT.NONE);
			label2.setText("Environment");

			final Label label3 = new Label(composite, SWT.NONE);
			label3.setText("Type");

			_myForce = new List(composite, SWT.MULTI);
			_myForce.setToolTipText(_forceTip);
			final GridData forceListLData = new GridData();
			forceListLData.grabExcessHorizontalSpace = true;
			forceListLData.verticalAlignment = GridData.BEGINNING;
			forceListLData.horizontalAlignment = GridData.FILL;
			_myForce.setLayoutData(forceListLData);

			_myType = new List(composite, SWT.MULTI);
			_myType.setToolTipText(_typeTip);
			final GridData typeListLData = new GridData();
			typeListLData.verticalAlignment = GridData.BEGINNING;
			typeListLData.horizontalAlignment = GridData.FILL;
			_myType.setLayoutData(typeListLData);

			_myEnvironment = new List(composite, SWT.MULTI);
			_myEnvironment.setToolTipText(_environmentTip);
			final GridData envListLData = new GridData();
			envListLData.grabExcessHorizontalSpace = true;
			envListLData.verticalAlignment = GridData.BEGINNING;
			envListLData.horizontalAlignment = GridData.FILL;
			_myEnvironment.setLayoutData(envListLData);

			_myForce.setItems(getForces());
			_myType.setItems(getTypes());
			_myEnvironment.setItems(getEnvironments());
			
			// ok, select the right items
			setCurrentValues();

			return composite;
		}

		/**
		 * 
		 */
		private void setCurrentValues()
		{
			final Vector<String> forces = new Vector<String>(0, 1);
			final Vector<String> types = new Vector<String>(0, 1);
			final Vector<String> envs = new Vector<String>(0, 1);

			// ok, sort out the forces
			final Collection<String> targetTypes = _current.getTargets();
			for (final Iterator<String> iter = targetTypes.iterator(); iter.hasNext();)
			{
				final String type = (String) iter.next();

				// is this a force?
				if (Category.getForces().contains(type))
				{
					forces.add(type);
				}
				else if (Category.getTypes().contains(type))
				{
					types.add(type);
				}
				else if (Category.getEnvironments().contains(type))
				{
					envs.add(type);
				}
			}

			final String[] template = new String[] { "" };
			if (forces.size() > 0)
			{
				final String[] vals = (String[]) forces.toArray(template);
				_myForce.setSelection(vals);
			}
			if (types.size() > 0)
			{
				final String[] vals = (String[]) types.toArray(template);
				_myType.setSelection(vals);
			}
			if (envs.size() > 0)
			{
				final String[] vals = (String[]) envs.toArray(template);
				_myEnvironment.setSelection(vals);
			}
		}

		/**
		 * 
		 */
		protected void okPressed()
		{
			// hey, store the data
			_newResult = new TargetType();

			setTypes(_myForce, _newResult);
			setTypes(_myType, _newResult);
			setTypes(_myEnvironment, _newResult);

			super.okPressed();
		}

		protected void setTypes(final List holder, final TargetType tt)
		{
			final int[] forces = holder.getSelectionIndices();
			for (int thisI = 0; thisI < forces.length; thisI++)
			{
				final int index = forces[thisI];
				final String f = holder.getItem(index);
				tt.addTargetType(f);
			}
		}

		protected TargetType getResult()
		{

			return _newResult;
		}

		/**
		 * @return our list of values
		 */
		protected String[] getForces()
		{
			String[] res = new String[] { null };
			res = (String[]) Category.getForces().toArray(res);
			return res;
		}

		/**
		 * @return our list of values
		 */
		protected String[] getEnvironments()
		{
			String[] res = new String[] { null };
			res = (String[]) Category.getEnvironments().toArray(res);
			return res;
		}

		/**
		 * @return our list of values
		 */
		protected String[] getTypes()
		{
			String[] res = new String[] { null };
			res = (String[]) Category.getTypes().toArray(res);
			return res;
		}

	}

	private static class TargetTypeCellEditor extends DialogCellEditor
	{
		/**
		 * constructor - just pass on to parent
		 * 
		 * @param cellParent
		 */
		public TargetTypeCellEditor(final Composite cellParent)
		{
			super(cellParent);
		}

		/**
		 * override operation triggered when button pressed. We should strictly be
		 * opening a new dialog, instead we're looking for a valid location on the
		 * clipboard. If one is there, we paste it.
		 * 
		 * @param cellEditorWindow
		 *          the parent control we belong to
		 * @return
		 */
		protected Object openDialogBox(final Control cellEditorWindow)
		{
			TargetType res = null;

			final TargetTypeDialog dialog = new TargetTypeDialog((TargetType) super.getValue(),
					cellEditorWindow.getShell());

			final int response = dialog.open();

			if (response == org.eclipse.jface.dialogs.Dialog.OK)
			{
				res = dialog.getResult();
			}

			// ok - create our popup window
			//			
			// LatLongPropertySource output = null;
			//
			// // right, see what's on the clipboard
			// // right, copy the location to the clipboard
			// Clipboard clip = CorePlugin.getDefault().getClipboard();
			// Object val = clip.getContents(TextTransfer.getInstance());
			// if (val != null)
			// {
			// String txt = (String) val;
			// if (CorePlugin.isLocation(txt))
			// {
			// // cool, get the text
			// WorldLocation loc = CorePlugin.fromClipboard(txt);
			//
			// // create the output value
			// output = new LatLongPropertySource(loc);
			// }
			// else
			// {
			// CorePlugin.showMessage("Paste location",
			// "Sorry the clipboard text is not in the right format." + "\nContents:"
			// + txt);
			// }
			// }
			// else
			// {
			// CorePlugin.showMessage("Paste location",
			// "Sorry, there is no suitable text on the clipboard");
			// }

			return res;
		}

		/**
		 * Creates the button for this cell editor under the given parent control.
		 * <p>
		 * The default implementation of this framework method creates the button
		 * display on the right hand side of the dialog cell editor. Subclasses may
		 * extend or reimplement.
		 * </p>
		 * 
		 * @param parent
		 *          the parent control
		 * @return the new button control
		 */
		protected Button createButton(final Composite parent)
		{
			final Button result = super.createButton(parent);
			result.setText("...");
			return result;
		}
	}
}