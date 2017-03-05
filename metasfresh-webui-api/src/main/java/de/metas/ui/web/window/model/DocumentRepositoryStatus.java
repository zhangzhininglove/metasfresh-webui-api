package de.metas.ui.web.window.model;

import java.util.Objects;

import com.google.common.base.MoreObjects;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public class DocumentRepositoryStatus
{
	private final DocumentsRepository documentsRepository;
	private boolean staled;
	private String version;

	public DocumentRepositoryStatus(final DocumentsRepository documentsRepository)
	{
		this.documentsRepository = documentsRepository;
		
		staled = false; // initially not staled
		version = null; // unknown
	}

	/** copy constructor */
	private DocumentRepositoryStatus(final DocumentRepositoryStatus from)
	{
		documentsRepository = from.documentsRepository;
		staled = from.staled;
		version = from.version;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("staled", staled)
				.add("version", version)
				.toString();
	}
	
	public DocumentRepositoryStatus copy()
	{
		return new DocumentRepositoryStatus(this);
	}

	public boolean isStaled()
	{
		return staled;
	}

	public boolean checkStaled(final Document document)
	{
		if (staled)
		{
			return true;
		}

		if (document.isNew())
		{
			return false;
		}

		final String versionNow = documentsRepository.retrieveVersion(document.getEntityDescriptor(), document.getDocumentIdAsInt());
		if (Objects.equals(version, versionNow))
		{
			return false;
		}

		staled = true;
		return true;
	}

	public void markStaled()
	{
		staled = true;
	}

	public void markNotStaled(final String version)
	{
		staled = false;
		this.version = version;
	}

}
