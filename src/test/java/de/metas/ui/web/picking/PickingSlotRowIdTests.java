package de.metas.ui.web.picking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import de.metas.ui.web.picking.PickingSlotRow.PickingSlotRowId;

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

public class PickingSlotRowIdTests
{
	/**
	 * Create a picking slot row ID that has no actual picking slot
	 */
	@Test
	public void testNoPickingSlotId()
	{
		final PickingSlotRowId rowId = PickingSlotRowId.ofSourceHU(2);
		assertThat(rowId.getPickingSlotId()).isLessThan(0);
		assertThat(rowId.getHuId()).isEqualTo(2);
		assertThat(rowId.getHuStorageProductId()).isLessThan(0);
	}
	
	@Test
	public void testOPfPickedHU()
	{
		final PickingSlotRowId rowId = PickingSlotRowId.ofPickedHU(1, 2, 3);
		assertThat(rowId.getPickingSlotId()).isEqualTo(1);
		assertThat(rowId.getHuId()).isEqualTo(2);
		assertThat(rowId.getHuStorageProductId()).isEqualTo(3);
	}
}