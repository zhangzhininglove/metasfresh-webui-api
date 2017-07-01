package de.metas.ui.web.board.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;

import de.metas.ui.web.document.filter.json.JSONDocumentFilterDescriptor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

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

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@Builder
@Value
public class JSONNewCardsViewLayout
{
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String caption;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String description;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String emptyResultText;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String emptyResultHint;

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@Singular
	private final List<JSONDocumentFilterDescriptor> filters;
	
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@Singular
	private final List<JSONBoardCardOrderBy> orderBys;
}
