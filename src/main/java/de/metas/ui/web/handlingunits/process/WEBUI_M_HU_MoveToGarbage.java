package de.metas.ui.web.handlingunits.process;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.adempiere.util.Services;
import org.compiere.util.Env;

import com.google.common.collect.ImmutableSet;

import de.metas.handlingunits.inventory.IHUInventoryBL;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.process.IProcessPrecondition;
import de.metas.process.ProcessPreconditionsResolution;

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

/**
 * Create Internal Use Inventory and destroy given HUs.
 *
 * @author metas-dev <dev@metasfresh.com>
 * @task initial task https://github.com/metasfresh/metasfresh-webui-api/issues/396
 */
public class WEBUI_M_HU_MoveToGarbage extends HUEditorProcessTemplate implements IProcessPrecondition
{
	private static final String MSG_NoSelectedHU = "NoHUSelected";
	
	private final transient IHUInventoryBL huInventoryBL = Services.get(IHUInventoryBL.class);

	private Set<Integer> huIdsDestroyed;

	@Override
	protected ProcessPreconditionsResolution checkPreconditionsApplicable()
	{
		final Set<Integer> huIds = getSelectedHUIds();
		if (huIds.isEmpty())
		{
			return ProcessPreconditionsResolution.reject(msgBL.getTranslatableMsgText(MSG_NoSelectedHU));
		}

		return ProcessPreconditionsResolution.accept();
	}

	@Override
	protected String doIt() throws Exception
	{
		final List<I_M_HU> husToDestroy = getSelectedHUs();
		final Timestamp movementDate = Env.getDate(getCtx());
		huInventoryBL.moveToGarbage(husToDestroy, movementDate);

		huIdsDestroyed = husToDestroy.stream().map(I_M_HU::getM_HU_ID).collect(ImmutableSet.toImmutableSet());

		return MSG_OK;
	}

	@Override
	protected void postProcess(final boolean success)
	{
		// Invalidate the view
		if (huIdsDestroyed != null && !huIdsDestroyed.isEmpty())
		{
			getView().removeHUIdsAndInvalidate(huIdsDestroyed);
		}
	}
}
