package de.metas.ui.web.window.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.adempiere.util.Check;
import org.slf4j.Logger;

import com.google.common.collect.Maps;

import de.metas.logging.LogManager;
import de.metas.ui.web.window.datatypes.DocumentId;

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

class DocumentsCache
{
	private static final Logger logger = LogManager.getLogger(DocumentsCache.class);

	private final LinkedHashMap<DocumentId, Document> _documents;
	private final Set<DocumentId> _staleDocumentIds;

	DocumentsCache()
	{
		_documents = new LinkedHashMap<>();
		_staleDocumentIds = new HashSet<>();
	}

	/** copy constructor */
	private DocumentsCache(final DocumentsCache from, Function<Document, Document> documentCopyMethod)
	{
		_documents = new LinkedHashMap<>(Maps.transformValues(from._documents, document -> documentCopyMethod.apply(document)));
		_staleDocumentIds = new HashSet<>(from._staleDocumentIds);
	}

	public DocumentsCache copy(final Function<Document, Document> documentCopyMethod)
	{
		return new DocumentsCache(this, documentCopyMethod);
	}

	public int size()
	{
		return _documents.size();
	}

	/** @return cached document ids */
	public Set<DocumentId> getDocumentIds()
	{
		return _documents.keySet();
	}
	
	public Collection<Document> getDocuments()
	{
		return _documents.values();
	}

	@Nullable
	public Document getById(final DocumentId documentId)
	{
		return _documents.get(documentId);
	}

	public void put(final Document document)
	{
		Check.assumeNotNull(document, "Parameter document is not null");
		final DocumentId documentId = document.getDocumentId();
		_documents.put(documentId, document);
		markNotStale(documentId);
	}
	
	public void putAll(final Collection<Document> documentsToAdd)
	{
		if(documentsToAdd.isEmpty())
		{
			return;
		}
		
		for (final Document document : documentsToAdd)
		{
			final DocumentId documentId = document.getDocumentId();
			final Document documentExisting = _documents.put(documentId, document);
			if (documentExisting != null)
			{
				logger.warn("loadAll: Replacing for documentId={}: {} with {}", documentId, documentExisting, document);
			}
		}
	}

	public void removeById(final DocumentId documentId)
	{
		_documents.remove(documentId);
		markNotStale(documentId);
	}
	
	public void clearAllExceptNewDocuments()
	{
		logger.trace("Removing all documents, except the new ones from {}", this);
		for (final Iterator<Document> it = _documents.values().iterator(); it.hasNext();)
		{
			final Document document = it.next();

			// Skip new documents
			if (document.isNew())
			{
				continue;
			}

			it.remove();
			logger.trace("Removed document from internal map: {}", document);
		}
	}
	
	public boolean isStale()
	{
		return !_staleDocumentIds.isEmpty();
	}

	public boolean isStale(final DocumentId documentId)
	{
		return _staleDocumentIds.contains(documentId);
	}
	
	public final void markAllStale()
	{
		_staleDocumentIds.addAll(getDocumentIds());
	}
	
	public void markAllNotStale()
	{
		_staleDocumentIds.clear();
	}

	public void markNotStale(final DocumentId documentId)
	{
		if (documentId == null)
		{
			throw new NullPointerException("documentId cannot be null");
		}
		_staleDocumentIds.remove(documentId);
	}


}
