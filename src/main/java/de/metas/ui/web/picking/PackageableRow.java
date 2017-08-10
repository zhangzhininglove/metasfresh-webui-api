package de.metas.ui.web.picking;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import de.metas.inoutcandidate.model.I_M_Packageable_V;
import de.metas.ui.web.exceptions.EntityNotFoundException;
import de.metas.ui.web.view.IViewRow;
import de.metas.ui.web.view.IViewRowAttributes;
import de.metas.ui.web.view.IViewRowType;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.view.descriptor.annotation.ViewColumn;
import de.metas.ui.web.view.descriptor.annotation.ViewColumn.ViewColumnLayout;
import de.metas.ui.web.view.descriptor.annotation.ViewColumnHelper;
import de.metas.ui.web.view.json.JSONViewDataType;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.datatypes.LookupValue;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * Rows shown in {@link PackageableView}. Each row basically represents one {@link I_M_Packageable_V}.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@ToString(exclude = "_fieldNameAndJsonValues")
public final class PackageableRow implements IViewRow
{
	private final ViewId viewId;
	private final DocumentId id;
	private final IViewRowType type;
	private final boolean processed;
	private final DocumentPath documentPath;

	@ViewColumn(widgetType = DocumentFieldWidgetType.Lookup, captionKey = I_M_Packageable_V.COLUMNNAME_M_Warehouse_ID, layouts =
		{
				@ViewColumnLayout(when = JSONViewDataType.grid, seqNo = 10)
		})
	private final LookupValue warehouse;

	@ViewColumn(widgetType = DocumentFieldWidgetType.Lookup, captionKey = I_M_Packageable_V.COLUMNNAME_M_Product_ID, layouts =
		{
				@ViewColumnLayout(when = JSONViewDataType.grid, seqNo = 20)
		})
	private final LookupValue product;

	@ViewColumn(widgetType = DocumentFieldWidgetType.Quantity, captionKey = I_M_Packageable_V.COLUMNNAME_QtyToDeliver, layouts =
		{
				@ViewColumnLayout(when = JSONViewDataType.grid, seqNo = 30)
		})
	private final BigDecimal qtyToDeliver;

	@ViewColumn(widgetType = DocumentFieldWidgetType.Quantity, captionKey = I_M_Packageable_V.COLUMNNAME_QtyPickedPlanned, layouts =
		{
				@ViewColumnLayout(when = JSONViewDataType.grid, seqNo = 35)
		})
	private final BigDecimal qtyPickedPlanned;

	@ViewColumn(widgetType = DocumentFieldWidgetType.DateTime, captionKey = I_M_Packageable_V.COLUMNNAME_DeliveryDate, layouts =
		{
				@ViewColumnLayout(when = JSONViewDataType.grid, seqNo = 40)
		})
	private final java.util.Date deliveryDate;

	@ViewColumn(widgetType = DocumentFieldWidgetType.DateTime, captionKey = I_M_Packageable_V.COLUMNNAME_PreparationDate, layouts =
		{
				@ViewColumnLayout(when = JSONViewDataType.grid, seqNo = 50)
		})
	private final java.util.Date preparationDate;

	private final int shipmentScheduleId;

	private final ViewId includedViewId;

	private transient ImmutableMap<String, Object> _fieldNameAndJsonValues;

	public static PackageableRow cast(final IViewRow row)
	{
		return (PackageableRow)row;
	}

	@Builder
	private PackageableRow(
			@NonNull final DocumentId id,
			@NonNull final ViewId viewId,
			final IViewRowType type,
			final boolean processed,
			@NonNull final DocumentPath documentPath,

			final LookupValue warehouse,
			final LookupValue product,
			final BigDecimal qtyToDeliver,
			final BigDecimal qtyPickedPlanned,
			final Date deliveryDate,
			final Date preparationDate,
			final int shipmentScheduleId)
	{
		this.id = id;
		this.viewId = viewId;
		this.type = type;
		this.processed = processed;
		this.documentPath = documentPath;

		this.warehouse = warehouse;
		this.product = product;
		this.qtyToDeliver = qtyToDeliver;
		this.qtyPickedPlanned = qtyPickedPlanned != null ? qtyPickedPlanned : BigDecimal.ZERO;
		this.deliveryDate = deliveryDate;
		this.preparationDate = preparationDate;
		this.shipmentScheduleId = shipmentScheduleId;

		// create the included view's ID
		// note that despite all our packageable-rows have the same picking slots, the IDs still need to be individual per-row,
		// because we need to notify the picking slot view of this packageable-rows is selected at a given point in time
		this.includedViewId = PickingSlotViewsIndexStorage.createViewId(viewId, id);
	}

	@Override
	public DocumentId getId()
	{
		return id;
	}

	@Override
	public IViewRowType getType()
	{
		return type;
	}

	@Override
	public boolean isProcessed()
	{
		return processed;
	}

	@Override
	public DocumentPath getDocumentPath()
	{
		return documentPath;
	}

	@Override
	public Map<String, Object> getFieldNameAndJsonValues()
	{
		if (_fieldNameAndJsonValues == null)
		{
			_fieldNameAndJsonValues = ViewColumnHelper.extractJsonMap(this);
		}
		return _fieldNameAndJsonValues;
	}

	@Override
	public List<? extends IViewRow> getIncludedRows()
	{
		return ImmutableList.of();
	}

	@Override
	public boolean hasAttributes()
	{
		return false;
	}

	@Override
	public IViewRowAttributes getAttributes() throws EntityNotFoundException
	{
		throw new EntityNotFoundException("Row does not support attributes");
	}

	@Override
	public boolean hasIncludedView()
	{
		return true;
	}

	@Override
	public ViewId getIncludedViewId()
	{
		return includedViewId;
	}

	public int getShipmentScheduleId()
	{
		return shipmentScheduleId;
	}
}
