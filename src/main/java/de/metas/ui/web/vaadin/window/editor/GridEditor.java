package de.metas.ui.web.vaadin.window.editor;

import java.util.List;

import org.slf4j.Logger;

import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasChildMeasurementHint.ChildMeasurementHint;
import com.vaadin.ui.Table;

import de.metas.logging.LogManager;
import de.metas.ui.web.vaadin.window.PropertyDescriptor;
import de.metas.ui.web.vaadin.window.PropertyName;
import de.metas.ui.web.vaadin.window.shared.datatype.PropertyValuesDTO;

/*
 * #%L
 * de.metas.ui.web.vaadin
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@SuppressWarnings("serial")
public class GridEditor extends DocumentSectionEditorsContainer
{
	static final Logger logger = LogManager.getLogger(GridEditor.class);

	private final GridTableFieldFactory fieldFactory;
	private GridEditorDataContainer containerDataSource;


	public GridEditor(final PropertyDescriptor propertyDescriptor)
	{
		super(propertyDescriptor);

		final Table table = getContent();
		table.setEditable(true);

		this.fieldFactory = new GridTableFieldFactory(propertyDescriptor);
		table.setTableFieldFactory(fieldFactory);

		containerDataSource = new GridEditorDataContainer(propertyDescriptor);
		table.setContainerDataSource(containerDataSource);
		for (Object propertyId : containerDataSource.getContainerPropertyIds())
		{
			table.setColumnHeader(propertyId, containerDataSource.getHeader(propertyId));
			table.setColumnExpandRatio(propertyId, -1);
		}
		
		table.setChildMeasurementHint(ChildMeasurementHint.MEASURE_NEVER);
		table.setColumnCollapsingAllowed(true);
		table.setPageLength(5);
	}

	@Override
	protected Component createPanelContent()
	{
		final Table table = new Table();
		table.setChildMeasurementHint(ChildMeasurementHint.MEASURE_NEVER);
		return table;
	}

	@Override
	protected Table getContent()
	{
		return (Table)super.getContent();
	}

	@Override
	public void setEditorListener(final EditorListener listener)
	{
		super.setEditorListener(listener);
		fieldFactory.setEditorListener(listener);
		containerDataSource.setEditorListener(listener);
	}

	@Override
	public boolean isAddingChildEditorsAllowed()
	{
		return false;
	}
	
	@Override
	public void addChildEditor(final Editor editor)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Editor> getChildEditors()
	{
		return ImmutableList.of();
	}

	@Override
	public void setValue(final PropertyName propertyName, final Object value)
	{
		if (getPropertyName().equals(propertyName))
		{
			@SuppressWarnings("unchecked")
			final List<PropertyValuesDTO> rowValuesList = (List<PropertyValuesDTO>)value;
	
			// FIXME: this is NOT optimum at all
			{
				final GridEditorDataContainer containerDataSourceNew = new GridEditorDataContainer(getPropertyDescriptor());
				containerDataSourceNew.setContent(rowValuesList);
				containerDataSourceNew.setEditorListener(getEditorListener());
				//
				containerDataSource = containerDataSourceNew;
				getContent().setContainerDataSource(containerDataSourceNew);
	
				// containerDataSource.setContent(rowValuesList);
			}
		}
	}

	public void setValueAt(final Object rowId, final PropertyName propertyName, final Object value)
	{
		final GridRowItem row = containerDataSource.getItem(rowId);
		if (row == null)
		{
			// TODO
			return;
		}

		row.setValue(propertyName, value);
	}

	public void newRow(final Object rowId, final PropertyValuesDTO rowValues)
	{
		final GridRowItem row = containerDataSource.addItem(rowId);
		row.setValues(rowValues);
	}
}