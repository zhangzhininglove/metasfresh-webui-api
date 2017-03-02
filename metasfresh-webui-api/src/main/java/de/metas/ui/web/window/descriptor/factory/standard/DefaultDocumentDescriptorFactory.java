package de.metas.ui.web.window.descriptor.factory.standard;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.adempiere.util.Check;
import org.compiere.model.I_AD_Window;
import org.compiere.util.CCache;
import org.springframework.stereotype.Service;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.datatypes.DocumentType;
import de.metas.ui.web.window.descriptor.DocumentDescriptor;
import de.metas.ui.web.window.descriptor.DocumentEntityDescriptor;
import de.metas.ui.web.window.descriptor.factory.DocumentDescriptorFactory;
import de.metas.ui.web.window.exceptions.DocumentLayoutBuildException;

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

@Service
public class DefaultDocumentDescriptorFactory implements DocumentDescriptorFactory
{
	private final CCache<Integer, DocumentDescriptor> documentDescriptorsByWindowId = new CCache<>(I_AD_Window.Table_Name + "#DocumentDescriptor", 50);
	private final CCache<String, Set<DocumentEntityDescriptor>> entityDescriptorsByTableName = new CCache<>(I_AD_Window.Table_Name + "#EntityDescriptorsByTableName", 50);

	/* package */ DefaultDocumentDescriptorFactory()
	{
		super();
	}

	@Override
	public DocumentDescriptor getDocumentDescriptor(final int AD_Window_ID)
	{
		try
		{
			return documentDescriptorsByWindowId.getOrLoad(AD_Window_ID, () -> {
				final DocumentDescriptor descriptor = new DefaultDocumentDescriptorLoader(AD_Window_ID).load();

				final String tableName = descriptor.getEntityDescriptor().getTableNameOrNull();
				if (tableName != null)
				{
					entityDescriptorsByTableName.remove(tableName);
				}

				return descriptor;
			});
		}
		catch (final Exception e)
		{
			throw DocumentLayoutBuildException.wrapIfNeeded(e);
		}
	}

	private Set<DocumentEntityDescriptor> getEntityDescriptorsForTableName(final String tableName)
	{
		Check.assumeNotEmpty(tableName, "tableName is not empty");

		return entityDescriptorsByTableName.getOrLoad(tableName, () -> documentDescriptorsByWindowId.values()
				.stream()
				.map(descriptor -> descriptor.getEntityDescriptor())
				.filter(entityDescriptor -> Objects.equal(tableName, entityDescriptor.getTableNameOrNull()))
				.collect(ImmutableSet.toImmutableSet()));
	}

	@Override
	public List<DocumentPath> getDocumentPaths(final String tableName, final int documentIdInt, final String includedTableName, final int includedDocumentIdInt)
	{
		final DocumentId documentId = DocumentId.of(documentIdInt);

		return getEntityDescriptorsForTableName(tableName)
				.stream()
				.flatMap(entityDescriptor -> {
					final DocumentType documentType = entityDescriptor.getDocumentType();
					final DocumentId documentTypeId = entityDescriptor.getDocumentTypeId();
					if (includedTableName == null)
					{
						return Stream.of(DocumentPath.rootDocumentPath(documentType, documentTypeId, documentId));
					}
					else
					{
						final DocumentId rowId = DocumentId.of(includedDocumentIdInt);
						return entityDescriptor.getIncludedEntitiesByTableName(includedTableName)
								.map(includedEntityDescriptor -> DocumentPath.includedDocumentPath(documentType, documentTypeId, documentId, includedEntityDescriptor.getDetailId(), rowId));
					}
				})
				.collect(ImmutableList.toImmutableList());
	}
}
