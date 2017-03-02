package de.metas.ui.web.window.descriptor.factory;

import java.util.List;

import org.adempiere.util.lang.impl.TableRecordReference;

import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.descriptor.DetailId;
import de.metas.ui.web.window.descriptor.DocumentDescriptor;
import de.metas.ui.web.window.descriptor.DocumentEntityDescriptor;
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

public interface DocumentDescriptorFactory
{
	DocumentDescriptor getDocumentDescriptor(int AD_Window_ID) throws DocumentLayoutBuildException;

	default DocumentEntityDescriptor getDocumentEntityDescriptor(int AD_Window_ID)
	{
		return getDocumentDescriptor(AD_Window_ID).getEntityDescriptor();
	}

	default String getTableNameOrNull(int AD_Window_ID)
	{
		return getDocumentEntityDescriptor(AD_Window_ID).getTableName();
	}

	default String getTableNameOrNull(int AD_Window_ID, DetailId detailId)
	{
		DocumentEntityDescriptor descriptor = getDocumentEntityDescriptor(AD_Window_ID);
		if (detailId == null)
		{
			return descriptor.getTableName();
		}
		else
		{
			return descriptor.getIncludedEntityByDetailId(detailId).getTableName();
		}
	}
	
	default TableRecordReference getTableRecordReference(DocumentPath documentPath)
	{
		DocumentEntityDescriptor rootEntityDescriptor = getDocumentEntityDescriptor(documentPath.getAD_Window_ID());

		if (documentPath.isRootDocument())
		{
			String tableName = rootEntityDescriptor.getTableName();
			int recordId = documentPath.getDocumentId().toInt();
			return TableRecordReference.of(tableName, recordId);
		}

		DocumentEntityDescriptor includedEntityDescriptor = rootEntityDescriptor.getIncludedEntityByDetailId(documentPath.getDetailId());
		String tableName = includedEntityDescriptor.getTableName();
		int recordId = documentPath.getSingleRowId().toInt();
		return TableRecordReference.of(tableName, recordId);
	}

	List<DocumentPath> getDocumentPaths(String tableName, int documentIdInt, String includedTableName, int includedDocumentIdInt);

}
