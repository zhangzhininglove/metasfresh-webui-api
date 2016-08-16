package de.metas.ui.web.window.datatypes;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import de.metas.printing.esb.base.util.Check;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public final class DocumentId
{
	public static final int NEW_ID = -1;
	public static final String NEW_ID_STRING = "NEW";
	public static final DocumentId NEW = new DocumentId(NEW_ID);

	private static final char TEMPORARY_ID_PREFIX = 'T';
	
	private static final AtomicInteger nextTemporaryId = new AtomicInteger(-1000);

	public static final DocumentId of(String idStr)
	{
		if (NEW_ID_STRING.equals(idStr))
		{
			return NEW;
		}

		if (idStr == null)
		{
			throw new NullPointerException("idStr shall not be null");
		}
		if (idStr.isEmpty())
		{
			throw new NullPointerException("idStr shall not be empty");
		}

		if (idStr.charAt(0) == TEMPORARY_ID_PREFIX)
		{
			idStr = "-" + idStr.substring(1);
		}

		final int idInt = Integer.parseInt(idStr);
		return of(idInt);
	}

	public static final DocumentId of(final int idInt)
	{
		if (idInt == NEW_ID)
		{
			return NEW;
		}

		return new DocumentId(idInt);
	}

	public static final DocumentId fromNullable(final String idStr)
	{
		if (Check.isEmpty(idStr, true))
		{
			return null;
		}
		return of(idStr.trim());
	}

	public static final DocumentId fromObject(final Object idObj)
	{
		if (idObj instanceof Integer)
		{
			return of((Integer)idObj);
		}
		else if (idObj instanceof String)
		{
			return of((String)idObj);
		}
		else
		{
			throw new IllegalArgumentException("Cannot convert " + idObj + " (" + (idObj == null ? null : idObj.getClass()) + ") to " + DocumentId.class);
		}
	}

	public static final boolean isNew(final int id)
	{
		return id == NEW_ID
				|| id < 0 // temporary id
				;
	}
	
	public static final int generateTemporaryId()
	{
		return nextTemporaryId.decrementAndGet();
	}

	private final int idInt;

	private DocumentId(final int idInt)
	{
		super();
		this.idInt = idInt;
	}

	@Override
	public String toString()
	{
		return toJson();
	}

	public String toJson()
	{
		if (idInt == NEW_ID)
		{
			return NEW_ID_STRING;
		}
		if (idInt < 0)
		{
			return TEMPORARY_ID_PREFIX + String.valueOf(-idInt);
		}
		return String.valueOf(idInt);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(idInt);
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
		{
			return true;
		}

		if (!(obj instanceof DocumentId))
		{
			return false;
		}

		final DocumentId other = (DocumentId)obj;
		return idInt == other.idInt;
	}

	public int toInt()
	{
		return idInt;
	}

	public boolean isNew()
	{
		return idInt == NEW_ID;
	}
}