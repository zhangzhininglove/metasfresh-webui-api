package de.metas.ui.web.window.model;

import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;

import de.metas.ui.web.exceptions.OperationNotAllowedException;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.descriptor.DocumentEntityDescriptor;
import de.metas.ui.web.window.exceptions.DocumentNotFoundException;
import de.metas.ui.web.window.model.Document.CopyMode;

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

/* package */class HighVolumeIncludedDocumentsCollection implements IIncludedDocumentsCollection
{
	private final DocumentEntityDescriptor entityDescriptor;
	private final Document parentDocument;

	public HighVolumeIncludedDocumentsCollection(final Document parentDocument, final DocumentEntityDescriptor entityDescriptor)
	{
		this.parentDocument = Preconditions.checkNotNull(parentDocument);
		this.entityDescriptor = Preconditions.checkNotNull(entityDescriptor);
	}

	@Override
	public IIncludedDocumentsCollection copy(final Document parentDocumentCopy, final CopyMode copyMode)
	{
		return this;
	}

	@Override
	public List<Document> getDocuments()
	{
		return DocumentQuery.builder(entityDescriptor)
				.setParentDocument(parentDocument)
				.retriveDocuments();
	}

	@Override
	public Document getDocumentById(final DocumentId documentId)
	{
		final Document document = DocumentQuery.builder(entityDescriptor)
				.setParentDocument(parentDocument)
				.setRecordId(documentId)
				.retriveDocumentOrNull();
		if (document == null)
		{
			final DocumentPath documentPath = parentDocument
					.getDocumentPath()
					.createChildPath(entityDescriptor.getDetailId(), documentId);
			throw new DocumentNotFoundException(documentPath);
		}
		return document;
	}

	@Override
	public void assertNewDocumentAllowed()
	{
		throw new OperationNotAllowedException("creating new document is not allowed");
	}

	@Override
	public Document createNewDocument()
	{
		throw new OperationNotAllowedException("creating new document is not allowed");
	}

	@Override
	public void deleteDocuments(final Set<DocumentId> documentIds)
	{
		throw new OperationNotAllowedException("deleting documents is not allowed");
	}

	@Override
	public DocumentValidStatus checkAndGetValidStatus()
	{
		// always valid
		return DocumentValidStatus.valid();
	}

	@Override
	public boolean hasChangesRecursivelly()
	{
		return false;
	}

	@Override
	public void saveIfHasChanges()
	{
		// do nothing
	}

	@Override
	public void markStaleAll()
	{
		// do nothing
	}

	@Override
	public int getNextLineNo()
	{
		throw new OperationNotAllowedException("getting next LineNo is not supported");
	}

}
