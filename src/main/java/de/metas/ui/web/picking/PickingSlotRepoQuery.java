package de.metas.ui.web.picking;
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

import de.metas.handlingunits.model.I_M_Picking_Candidate;
import de.metas.picking.model.I_M_PickingSlot;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;

/**
 * Used in the repo services, to specify which data we want to be retrieved.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@Data
@Builder
public class PickingSlotRepoQuery
{
	public static PickingSlotRepoQuery of(final int shipmentScheduleId)
	{
		return builder().shipmentScheduleId(shipmentScheduleId).build();
	}

	@NonNull
	final Integer shipmentScheduleId;

	public enum PickingCandidate
	{
		/**
		 * Only retrieve picking slot items that have a processed {@link I_M_Picking_Candidate} assigned to their underlying {@link I_M_PickingSlot}.
		 */
		ONLY_PROCESSED,

		/**
		 * Only retrieve picking slot items that have an unprocessed {@link I_M_Picking_Candidate} assigned to their underlying {@link I_M_PickingSlot}.
		 */
		ONLY_UNPROCESSED,
		
		/**
		 * Retrieve all picking slot items, no matter whether they have a {@link I_M_Picking_Candidate} assigned to their underlying {@link I_M_PickingSlot} or not.
		 */
		DONT_CARE
	}

	/**
	 * Optional; a <code>null</code> value means "return both with and without"
	 */
	@Default
	final PickingCandidate pickingCandidates = PickingCandidate.DONT_CARE;
}
