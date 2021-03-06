package de.metas.ui.web.pporder;

import java.util.function.Function;

import org.adempiere.util.Services;
import org.compiere.util.Env;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.google.common.base.Preconditions;

import de.metas.process.IADProcessDAO;
import de.metas.process.RelatedProcessDescriptor;
import de.metas.ui.web.WebRestApiApplication;
import de.metas.ui.web.window.datatypes.WindowId;
import de.metas.ui.web.window.descriptor.DetailId;

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

@Configuration
@DependsOn(WebRestApiApplication.BEANNAME_Adempiere) // NOTE: we need Adempiere as parameter to make sure it was initialized. Else the DAOs will fail.
public class WebPPOrderConfig
{
	public static final String AD_WINDOW_ID_IssueReceipt_String = "540328"; // Manufacturing Issue/Receipt
	public static final WindowId AD_WINDOW_ID_IssueReceipt = WindowId.fromJson("540328"); // Manufacturing Issue/Receipt
	public static final WindowId AD_WINDOW_ID_PP_Order = WindowId.fromJson("53009"); // Manufacturing order standard window
	public static final DetailId TABID_ID_PP_Order_BOMLine = DetailId.fromTabNoOrNull(2); // Manufacturing order standard window - BOM line tab

	public WebPPOrderConfig()
	{
		final IADProcessDAO adProcessDAO = Services.get(IADProcessDAO.class);

		//
		// Manufacturing Issue / Receipt
		{
			final Function<Class<?>, RelatedProcessDescriptor.Builder> newDescriptorBuilder = (processClass) -> {
				final int processId = adProcessDAO.retriveProcessIdByClassIfUnique(Env.getCtx(), processClass);
				Preconditions.checkArgument(processId > 0, "No AD_Process_ID found for %s", processClass);

				return RelatedProcessDescriptor.builder()
						.processId(processId)
						.windowId(AD_WINDOW_ID_IssueReceipt.toInt())
						.anyTable()
						.webuiQuickAction(true);
			};
			//
			adProcessDAO.registerTableProcess(newDescriptorBuilder.apply(de.metas.ui.web.pporder.process.WEBUI_PP_Order_Receipt.class).build());
			adProcessDAO.registerTableProcess(newDescriptorBuilder.apply(de.metas.ui.web.pporder.process.WEBUI_PP_Order_ReverseCandidate.class).build());
			adProcessDAO.registerTableProcess(newDescriptorBuilder.apply(de.metas.ui.web.pporder.process.WEBUI_PP_Order_ChangePlanningStatus_Planning.class).build());
			adProcessDAO.registerTableProcess(newDescriptorBuilder.apply(de.metas.ui.web.pporder.process.WEBUI_PP_Order_ChangePlanningStatus_Review.class).build());
			adProcessDAO.registerTableProcess(newDescriptorBuilder.apply(de.metas.ui.web.pporder.process.WEBUI_PP_Order_ChangePlanningStatus_Complete.class).build());
		}
	}
}
