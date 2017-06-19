package de.metas.ui.web.dashboard.interceptor;

import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.ad.modelvalidator.annotations.ModelChange;
import org.compiere.model.ModelValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.metas.ui.web.base.model.I_WEBUI_Dashboard;
import de.metas.ui.web.dashboard.UserDashboardPermissionsRepository;

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

@Interceptor(I_WEBUI_Dashboard.class)
@Component
public class WEBUI_Dashboard
{
	@Autowired
	private UserDashboardPermissionsRepository permissionsRepo;

	@ModelChange(timings = ModelValidator.TYPE_AFTER_NEW)
	public void afterCreate(final I_WEBUI_Dashboard dashboard)
	{
		permissionsRepo.addPermission(dashboard.getWEBUI_Dashboard_ID(), dashboard.getCreatedBy(), true);
	}

	@ModelChange(timings = ModelValidator.TYPE_BEFORE_DELETE)
	public void beforeDelete(final I_WEBUI_Dashboard dashboard)
	{
		permissionsRepo.deleteAllPermissionsForDashboard(dashboard.getWEBUI_Dashboard_ID());
	}
}
