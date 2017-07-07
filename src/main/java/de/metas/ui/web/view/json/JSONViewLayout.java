package de.metas.ui.web.view.json;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import de.metas.ui.web.document.filter.json.JSONDocumentFilterDescriptor;
import de.metas.ui.web.view.descriptor.ViewLayout;
import de.metas.ui.web.window.datatypes.WindowId;
import de.metas.ui.web.window.datatypes.json.JSONDocumentLayoutElement;
import de.metas.ui.web.window.datatypes.json.JSONOptions;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;

/*
 * #%L
 * metasfresh-webui-api
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@SuppressWarnings("serial")
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public final class JSONViewLayout implements Serializable
{
	public static JSONViewLayout of(
			final ViewLayout gridLayout //
			, final JSONOptions jsonOpts //
	)
	{
		return new JSONViewLayout(gridLayout, jsonOpts);
	}

	@JsonProperty("viewId")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private String viewId;
	//
	/** i.e. AD_Window_ID */
	@JsonProperty("type")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Deprecated
	private final WindowId type;
	@JsonProperty("windowId")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final WindowId windowId;

	@JsonProperty("caption")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String caption;

	@JsonProperty("description")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String description;

	@JsonProperty("emptyResultText")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String emptyResultText;

	@JsonProperty("emptyResultHint")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String emptyResultHint;

	@JsonProperty("elements")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final List<JSONDocumentLayoutElement> elements;

	@JsonProperty("filters")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final List<JSONDocumentFilterDescriptor> filters;

	public static final String PROPERTY_supportAttributes = "supportAttributes";
	@JsonProperty(value = PROPERTY_supportAttributes)
	private boolean supportAttributes;
	//
	@JsonProperty("supportTree")
	private final boolean supportTree;
	@JsonProperty("collapsible")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Boolean collapsible;
	@JsonProperty("expandedDepth")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Integer expandedDepth;
	//
	@JsonProperty("supportIncludedView")
	private final boolean supportIncludedView;
	@JsonProperty("supportIncludedViewOnSelect")
	private final Boolean supportIncludedViewOnSelect;

	//
	// New record support
	@JsonProperty("supportNewRecord")
	private boolean supportNewRecord = false;
	@JsonProperty("newRecordCaption")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String newRecordCaption = null;

	private JSONViewLayout(final ViewLayout layout, final JSONOptions jsonOpts)
	{
		windowId = layout.getWindowId();
		type = windowId;

		final String adLanguage = jsonOpts.getAD_Language();
		caption = layout.getCaption(adLanguage);
		description = layout.getDescription(adLanguage);
		emptyResultText = layout.getEmptyResultText(adLanguage);
		emptyResultHint = layout.getEmptyResultHint(adLanguage);

		//
		// Elements
		List<JSONDocumentLayoutElement> elements = JSONDocumentLayoutElement.ofList(layout.getElements(), jsonOpts);
		final String idFieldName = layout.getIdFieldName();
		if (jsonOpts.isDebugShowColumnNamesForCaption() && idFieldName != null)
		{
			elements = ImmutableList.<JSONDocumentLayoutElement> builder()
					.add(JSONDocumentLayoutElement.debuggingField(idFieldName, DocumentFieldWidgetType.Text))
					.addAll(elements)
					.build();
		}
		this.elements = elements;

		this.filters = JSONDocumentFilterDescriptor.ofCollection(layout.getFilters(), jsonOpts);

		supportAttributes = layout.isAttributesSupport();
		
		supportIncludedView = layout.isIncludedViewSupport();
		supportIncludedViewOnSelect = layout.isIncludedViewOnSelectSupport() ? Boolean.TRUE : null;

		//
		// Tree
		supportTree = layout.isTreeSupport();
		if (supportTree)
		{
			collapsible = layout.isTreeCollapsible();
			if (collapsible)
			{
				expandedDepth = layout.getTreeExpandedDepth();
			}
			else
			{
				expandedDepth = null;
			}
		}
		else
		{
			collapsible = null;
			expandedDepth = null;
		}
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.add("AD_Window_ID", type)
				.add("caption", caption)
				.add("elements", elements.isEmpty() ? null : elements)
				.add("filters", filters.isEmpty() ? null : filters)
				.toString();
	}

	public String getCaption()
	{
		return caption;
	}

	public String getDescription()
	{
		return description;
	}

	public String getEmptyResultText()
	{
		return emptyResultText;
	}

	public String getEmptyResultHint()
	{
		return emptyResultHint;
	}

	public List<JSONDocumentLayoutElement> getElements()
	{
		return elements;
	}

	public boolean hasElements()
	{
		return !elements.isEmpty();
	}

	public List<JSONDocumentFilterDescriptor> getFilters()
	{
		return filters;
	}

	public boolean isSupportAttributes()
	{
		return supportAttributes;
	}

	public void setSupportAttributes(boolean supportAttributes)
	{
		this.supportAttributes = supportAttributes;
	}

	public boolean isSupportTree()
	{
		return supportTree;
	}

	public void enableNewRecord(final String newRecordCaption)
	{
		supportNewRecord = true;
		this.newRecordCaption = newRecordCaption;
	}

	public void setViewId(final String viewId)
	{
		this.viewId = viewId;
	}
}
