package de.metas.ui.web.view;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.adempiere.util.lang.MutableInt;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.Adempiere;
import org.compiere.util.DB;
import org.compiere.util.Util.ArrayKey;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import de.metas.logging.LogManager;
import de.metas.ui.web.base.model.I_T_WEBUI_ViewSelection;
import de.metas.ui.web.base.model.I_T_WEBUI_ViewSelectionLine;
import de.metas.ui.web.exceptions.EntityNotFoundException;
import de.metas.ui.web.menu.MenuNode;
import de.metas.ui.web.menu.MenuTreeRepository;
import de.metas.ui.web.session.UserSession;
import de.metas.ui.web.view.descriptor.ViewLayout;
import de.metas.ui.web.view.json.JSONFilterViewRequest;
import de.metas.ui.web.view.json.JSONViewDataType;
import de.metas.ui.web.window.controller.DocumentPermissionsHelper;
import de.metas.ui.web.window.datatypes.WindowId;
import lombok.NonNull;

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

@Repository
public class ViewsRepository implements IViewsRepository
{
	private static final Logger logger = LogManager.getLogger(ViewsRepository.class);

	private final ConcurrentHashMap<ArrayKey, IViewFactory> factories = new ConcurrentHashMap<>();
	@Autowired
	private SqlViewFactory defaultFactory;

	@Autowired
	private MenuTreeRepository menuTreeRepo;

	@Value("${metasfresh.webui.view.truncateOnStartUp:true}")
	private boolean truncateSelectionOnStartUp;

	private final ConcurrentHashMap<WindowId, IViewsIndexStorage> viewsIndexStorages = new ConcurrentHashMap<>();
	private final IViewsIndexStorage defaultViewsIndexStorage = new DefaultViewsRepositoryStorage();

	/**
	 * 
	 * @param neededForDBAccess not used in here, but we need to cause spring to initialize it <b>before</b> this component can be initialized.
	 *            So, if you clean this up, please make sure that the webui-API still starts up ^^.
	 */
	public ViewsRepository(@NonNull final Adempiere neededForDBAccess)
	{
	}

	@PostConstruct
	private void truncateTempTablesIfAllowed()
	{
		if (truncateSelectionOnStartUp)
		{
			truncateTable(I_T_WEBUI_ViewSelection.Table_Name);
			truncateTable(I_T_WEBUI_ViewSelectionLine.Table_Name);
		}
	}

	private static void truncateTable(final String tableName)
	{
		final Stopwatch stopwatch = Stopwatch.createStarted();
		try
		{
			final int no = DB.executeUpdateEx("DELETE FROM " + tableName, ITrx.TRXNAME_NoneNotNull);
			logger.info("Deleted {} records(all) from table {} (Took: {})", no, tableName, stopwatch);
		}
		catch (final Exception ex)
		{
			logger.warn("Failed deleting all from {} (Took: {})", tableName, stopwatch, ex);
		}
	}

	@Autowired
	private void registerAnnotatedFactories(final Collection<IViewFactory> viewFactories)
	{
		for (final IViewFactory factory : viewFactories)
		{
			final ViewFactory annotation = factory.getClass().getAnnotation(ViewFactory.class);
			if (annotation == null)
			{
				logger.info("Skip registering {} because it's not annotated with {}", factory, ViewFactory.class);
				continue;
			}

			final WindowId windowId = WindowId.fromJson(annotation.windowId());
			JSONViewDataType[] viewTypes = annotation.viewTypes();
			if (viewTypes.length == 0)
			{
				viewTypes = JSONViewDataType.values();
			}

			for (final JSONViewDataType viewType : viewTypes)
			{
				factories.put(mkFactoryKey(windowId, viewType), factory);
				logger.info("Registered {} for windowId={}, viewType={}", factory, windowId, viewTypes);
			}
		}

	}

	private final IViewFactory getFactory(final WindowId windowId, final JSONViewDataType viewType)
	{
		IViewFactory factory = factories.get(mkFactoryKey(windowId, viewType));
		if (factory != null)
		{
			return factory;
		}

		factory = factories.get(mkFactoryKey(windowId, null));
		if (factory != null)
		{
			return factory;
		}

		return defaultFactory;
	}

	private static final ArrayKey mkFactoryKey(final WindowId windowId, final JSONViewDataType viewType)
	{
		return ArrayKey.of(windowId, viewType);
	}

	@Autowired
	private void registerViewsIndexStorages(final Collection<IViewsIndexStorage> viewsIndexStorages)
	{
		if (viewsIndexStorages.isEmpty())
		{
			logger.info("No {} discovered", IViewsIndexStorage.class);
			return;
		}

		for (final IViewsIndexStorage viewsIndexStorage : viewsIndexStorages)
		{
			if (viewsIndexStorage instanceof DefaultViewsRepositoryStorage)
			{
				logger.warn("Skipping {} because it shall not be in spring context", viewsIndexStorage);
				continue;
			}

			final WindowId windowId = viewsIndexStorage.getWindowId();
			Check.assumeNotNull(windowId, "Parameter windowId is not null");

			viewsIndexStorage.setViewsRepository(this);

			this.viewsIndexStorages.put(windowId, viewsIndexStorage);
			logger.info("Registered {} for windowId={}", viewsIndexStorage, windowId);
		}
	}

	private IViewsIndexStorage getViewsStorageFor(@NonNull final ViewId viewId)
	{
		final IViewsIndexStorage viewIndexStorage = viewsIndexStorages.get(viewId.getWindowId());
		if (viewIndexStorage != null)
		{
			return viewIndexStorage;
		}

		return defaultViewsIndexStorage;
	}

	private Stream<IView> streamAllViews()
	{
		return Streams.concat(viewsIndexStorages.values().stream(), Stream.of(defaultViewsIndexStorage))
				.flatMap(IViewsIndexStorage::streamAllViews);
	}

	@Override
	public ViewLayout getViewLayout(final WindowId windowId, final JSONViewDataType viewDataType)
	{
		DocumentPermissionsHelper.assertWindowAccess(windowId, null, UserSession.getCurrentPermissions());

		final IViewFactory factory = getFactory(windowId, viewDataType);
		return factory.getViewLayout(windowId, viewDataType)
				// Enable AllowNew if we have a menu node to create new records
				.withAllowNewRecordIfPresent(menuTreeRepo.getUserSessionMenuTree()
						.getNewRecordNodeForWindowId(windowId)
						.map(MenuNode::getCaption));
	}

	@Override
	public List<IView> getViews()
	{
		return streamAllViews().collect(ImmutableList.toImmutableList());
	}

	@Override
	public IView createView(final CreateViewRequest request)
	{
		final WindowId windowId = request.getWindowId();
		final JSONViewDataType viewType = request.getViewType();
		final IViewFactory factory = getFactory(windowId, viewType);
		final IView view = factory.createView(request);
		if (view == null)
		{
			throw new AdempiereException("Failed creating view")
					.setParameter("request", request)
					.setParameter("factory", factory.toString());
		}

		getViewsStorageFor(view.getViewId()).put(view);

		return view;
	}

	@Override
	public IView filterView(final ViewId viewId, final JSONFilterViewRequest jsonRequest)
	{
		// Get current view
		final IView view = getView(viewId);

		//
		// Create the new view
		final IViewFactory factory = getFactory(view.getViewId().getWindowId(), view.getViewType());
		final IView newView = factory.filterView(view, jsonRequest);
		if (newView == null)
		{
			throw new AdempiereException("Failed filtering view")
					.setParameter("viewId", viewId)
					.setParameter("request", jsonRequest)
					.setParameter("factory", factory.toString());
		}

		//
		// Add the new view to our internal map
		// NOTE: avoid adding if the factory returned the same view.
		if (view != newView)
		{
			getViewsStorageFor(newView.getViewId()).put(newView);
		}

		// Return the newly created view
		return newView;
	}

	@Override
	public IView deleteStickyFilter(final ViewId viewId, final String filterId)
	{
		// Get current view
		final IView view = getView(viewId);

		//
		// Create the new view
		final IViewFactory factory = getFactory(view.getViewId().getWindowId(), view.getViewType());
		final IView newView = factory.deleteStickyFilter(view, filterId);
		if (newView == null)
		{
			throw new AdempiereException("Failed deleting sticky/static filter")
					.setParameter("viewId", viewId)
					.setParameter("filterId", filterId)
					.setParameter("factory", factory.toString());
		}

		//
		// Add the new view to our internal map
		// NOTE: avoid adding if the factory returned the same view.
		if (view != newView)
		{
			getViewsStorageFor(newView.getViewId()).put(newView);
		}

		// Return the newly created view
		return newView;
	}

	@Override
	public IView getView(@NonNull final String viewIdStr)
	{
		final ViewId viewId = ViewId.ofViewIdString(viewIdStr);
		final IView view = getViewIfExists(viewId);
		if (view == null)
		{
			throw new EntityNotFoundException("No view found for viewId=" + viewId);
		}
		return view;
	}

	@Override
	public IView getViewIfExists(final ViewId viewId)
	{
		final IView view = getViewsStorageFor(viewId).getByIdOrNull(viewId);
		if (view == null)
		{
			throw new EntityNotFoundException("No view found for viewId=" + viewId);
		}

		final String windowName = viewId.getViewId(); // used only for error reporting
		DocumentPermissionsHelper.assertWindowAccess(viewId.getWindowId(), windowName, UserSession.getCurrentPermissions());

		return view;
	}

	@Override
	public void deleteView(final ViewId viewId)
	{
		getViewsStorageFor(viewId).removeById(viewId);
	}

	@Override
	public void invalidateView(final ViewId viewId)
	{
		getViewsStorageFor(viewId).invalidateView(viewId);
	}

	@Override
	@Async
	public void notifyRecordsChanged(final Set<TableRecordReference> recordRefs)
	{
		if (recordRefs.isEmpty())
		{
			return;
		}

		final MutableInt notifiedCount = MutableInt.zero();
		streamAllViews()
				.forEach(view -> {
					view.notifyRecordsChanged(recordRefs);
					notifiedCount.incrementAndGet();
				});

		if (logger.isDebugEnabled())
		{
			logger.debug("Notified {} views about changed records: {}", notifiedCount, recordRefs);
		}

	}
}
