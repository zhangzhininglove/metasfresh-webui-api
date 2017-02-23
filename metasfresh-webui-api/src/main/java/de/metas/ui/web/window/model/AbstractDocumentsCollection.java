package de.metas.ui.web.window.model;

import org.adempiere.ad.expression.api.IExpressionEvaluator.OnVariableNotFound;
import org.adempiere.ad.expression.api.ILogicExpression;
import org.adempiere.ad.expression.api.LogicExpressionResult;

import com.google.common.base.Preconditions;

import de.metas.ui.web.window.descriptor.DocumentEntityDescriptor;
import de.metas.ui.web.window.exceptions.InvalidDocumentStateException;

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

/* package */ abstract class AbstractDocumentsCollection implements IIncludedDocumentsCollection
{
	private static final LogicExpressionResult LOGICRESULT_FALSE_ParentDocumentProcessed = LogicExpressionResult.namedConstant("ParentDocumentProcessed", false);

	protected final DocumentEntityDescriptor entityDescriptor;
	private final Document parentDocument;

	/* package */ AbstractDocumentsCollection(final Document parentDocument, final DocumentEntityDescriptor entityDescriptor)
	{
		this.parentDocument = Preconditions.checkNotNull(parentDocument);
		this.entityDescriptor = Preconditions.checkNotNull(entityDescriptor);
	}
	
	protected Document getParentDocument()
	{
		return parentDocument;
	}

	protected final void assertWritable()
	{
		parentDocument.assertWritable();
	}

	@Override
	public void assertNewDocumentAllowed()
	{
		final LogicExpressionResult allowCreateNewDocument = getAllowCreateNewDocument();
		if (allowCreateNewDocument.isFalse())
		{
			throw new InvalidDocumentStateException(parentDocument, "Cannot create included document because it's not allowed."
					+ "\n AllowCreateNewDocument: " + allowCreateNewDocument
					+ "\n EntityDescriptor: " + entityDescriptor);
		}
	}

	private LogicExpressionResult getAllowCreateNewDocument()
	{
		if (parentDocument.isProcessed())
		{
			return LOGICRESULT_FALSE_ParentDocumentProcessed;
		}

		final ILogicExpression allowCreateNewLogic = entityDescriptor.getAllowCreateNewLogic();
		final LogicExpressionResult allowCreateNew = allowCreateNewLogic.evaluateToResult(parentDocument.asEvaluatee(), OnVariableNotFound.ReturnNoResult);
		return allowCreateNew;
	}
	
	@Override
	public Document createNewDocument()
	{
		assertWritable();
		assertNewDocumentAllowed();

		final DocumentsRepository documentsRepository = entityDescriptor.getDataBinding().getDocumentsRepository();
		final Document document = documentsRepository.createNewDocument(entityDescriptor, getParentDocument());
		return document;
	}


	protected void assertDeleteDocumentAllowed(final Document document)
	{
		final LogicExpressionResult allowDelete = getAllowDeleteDocument();
		if (allowDelete.isFalse())
		{
			throw new InvalidDocumentStateException(parentDocument, "Cannot delete included document because it's not allowed: " + allowDelete);
		}
	}

	private LogicExpressionResult getAllowDeleteDocument()
	{
		if (parentDocument.isProcessed())
		{
			return LOGICRESULT_FALSE_ParentDocumentProcessed;
		}

		final ILogicExpression allowDeleteLogic = entityDescriptor.getAllowDeleteLogic();
		final LogicExpressionResult allowDelete = allowDeleteLogic.evaluateToResult(parentDocument.asEvaluatee(), OnVariableNotFound.ReturnNoResult);
		return allowDelete;
	}
}
