package de.metas.ui.web.dashboard;

import java.util.Set;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.ad.security.permissions.Access;
import org.adempiere.ad.security.permissions.ElementPermission;
import org.adempiere.ad.security.permissions.ElementPermissions;
import org.adempiere.ad.security.permissions.ElementResource;
import org.adempiere.ad.security.permissions.PermissionsBuilder.CollisionPolicy;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Services;
import org.compiere.util.CCache;
import org.compiere.util.CacheMgt;
import org.compiere.util.Env;
import org.springframework.stereotype.Component;

import de.metas.ui.web.base.model.I_WEBUI_Dashboard;
import de.metas.ui.web.base.model.I_WEBUI_Dashboard_Access;

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

@Component
public class UserDashboardPermissionsRepository
{
	private final CCache<Integer, ElementPermissions> userPermissionsCache = CCache.newCache(I_WEBUI_Dashboard_Access.Table_Name + "#ElementPermissions", 20, 60);

	public void assertCanRead(final int dashboardId, final int adUserId)
	{
		getPermissionsForUser(adUserId).assertAccess(dashboardId, Access.READ, () -> new AdempiereException("No read permissions"));
	}

	public void assertCanWrite(final int dashboardId, final int adUserId)
	{
		getPermissionsForUser(adUserId).assertAccess(dashboardId, Access.WRITE, () -> new AdempiereException("No write permissions"));
	}

	public boolean canWrite(final int dashboardId, final int adUserId)
	{
		return getPermissionsForUser(adUserId).hasAccess(dashboardId, Access.WRITE);
	}

	public Set<Integer> getReadableUserDashboardIds(final int adUserId)
	{
		return getPermissionsForUser(adUserId).getElementIds();
	}

	private ElementPermissions getPermissionsForUser(final int adUserId)
	{
		return userPermissionsCache.getOrLoad(adUserId, () -> retrievePermissionsForUser(adUserId));
	}

	private ElementPermissions retrievePermissionsForUser(final int adUserId)
	{
		final ElementPermissions.Builder permissions = ElementPermissions.builder()
				.setElementTableName(I_WEBUI_Dashboard.Table_Name);

		queryDashboardAccessForUser(adUserId)
				.create()
				.stream(I_WEBUI_Dashboard_Access.class)
				.map(access -> createPermission(access))
				.forEach(permission -> permissions.addPermission(permission, CollisionPolicy.Merge));

		return permissions.build();
	}

	private IQueryBuilder<I_WEBUI_Dashboard_Access> queryDashboardAccessForUser(final int adUserId)
	{
		final IQueryBL queryBL = Services.get(IQueryBL.class);
		final IQueryBuilder<I_WEBUI_Dashboard_Access> queryBuilder = queryBL.createQueryBuilder(I_WEBUI_Dashboard_Access.class)
				.addOnlyActiveRecordsFilter();

		queryBuilder.addCompositeQueryFilter()
				.setJoinOr()
				.addEqualsFilter(I_WEBUI_Dashboard_Access.COLUMN_IsAllUsers, true)
				.addEqualsFilter(I_WEBUI_Dashboard_Access.COLUMN_AD_User_ID, adUserId);

		return queryBuilder;
	}

	private static ElementPermission createPermission(final I_WEBUI_Dashboard_Access access)
	{
		final int dashboardId = access.getWEBUI_Dashboard_ID();
		return ElementPermission.of(ElementResource.of(I_WEBUI_Dashboard.Table_Name, dashboardId), access.isReadWrite());
	}

	public void deleteAllPermissionsForDashboard(final int dashboardId)
	{
		Services.get(IQueryBL.class)
				.createQueryBuilder(I_WEBUI_Dashboard_Access.class)
				.addEqualsFilter(I_WEBUI_Dashboard_Access.COLUMN_WEBUI_Dashboard_ID, dashboardId)
				.create()
				.deleteDirectly();

		CacheMgt.get().reset(I_WEBUI_Dashboard_Access.Table_Name);
	}

	public void addPermission(final int dashboardId, final int adUserId, final boolean readWrite)
	{
		final I_WEBUI_Dashboard_Access access = InterfaceWrapperHelper.newInstance(I_WEBUI_Dashboard_Access.class);
		access.setWEBUI_Dashboard_ID(dashboardId);
		access.setAD_Org_ID(Env.CTXVALUE_AD_Org_ID_Any);
		access.setAD_User_ID(adUserId);
		access.setIsAllUsers(false);
		access.setIsReadWrite(true);
		InterfaceWrapperHelper.save(access);
	}

}
