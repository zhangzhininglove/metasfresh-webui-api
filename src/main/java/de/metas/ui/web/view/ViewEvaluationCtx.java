package de.metas.ui.web.view;

import java.util.Properties;

import org.adempiere.ad.security.UserRolePermissionsKey;
import org.adempiere.ad.security.impl.AccessSqlStringExpression;
import org.compiere.util.Env;
import org.compiere.util.Evaluatee;
import org.compiere.util.Evaluatees;

import com.google.common.base.MoreObjects;

import lombok.NonNull;

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

public class ViewEvaluationCtx
{
	public static final ViewEvaluationCtx of(final Properties ctx)
	{
		final String adLanguage = Env.getAD_Language(ctx);
		final UserRolePermissionsKey permissionsKey = UserRolePermissionsKey.of(ctx);
		return new ViewEvaluationCtx(ctx, adLanguage, permissionsKey);
	}

	private final Properties ctx; // needed for global context vars
	private final String adLanguage;
	private final UserRolePermissionsKey permissionsKey;
	
	private Evaluatee _evaluatee; // lazy

	private ViewEvaluationCtx(@NonNull final Properties ctx, @NonNull final String adLanguage, @NonNull final UserRolePermissionsKey permissionsKey)
	{
		this.ctx = ctx;
		this.adLanguage = adLanguage;
		this.permissionsKey = permissionsKey;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("adLanguage", adLanguage)
				.add("permissionsKey", permissionsKey)
				.toString();
	}
	
	public String getAD_Language()
	{
		return adLanguage;
	}

	public UserRolePermissionsKey getPermissionsKey()
	{
		return permissionsKey;
	}

	public Evaluatee toEvaluatee()
	{
		Evaluatee evaluatee = _evaluatee;
		
		if (evaluatee == null)
		{
			evaluatee = _evaluatee = Evaluatees.mapBuilder()
					.put(Env.CTXNAME_AD_Language, adLanguage)
					.put(AccessSqlStringExpression.PARAM_UserRolePermissionsKey.getName(), permissionsKey.toPermissionsKeyString())
					.build()
					// Fallback to global context
					// TODO: consider dropping the fallback because AFAIK only AD_Language and PermissionsKey is required
					.andComposeWith(Evaluatees.ofCtx(ctx));
			return evaluatee;
		}
		
		return evaluatee;
	}
}
