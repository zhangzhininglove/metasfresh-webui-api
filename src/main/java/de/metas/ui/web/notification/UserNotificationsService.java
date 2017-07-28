package de.metas.ui.web.notification;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.adempiere.util.Services;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import de.metas.event.Event;
import de.metas.event.IEventBus;
import de.metas.event.IEventBusFactory;
import de.metas.logging.LogManager;
import de.metas.ui.web.session.UserSession.LanguagedChangedEvent;
import de.metas.ui.web.websocket.WebsocketSender;

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

@Service
public class UserNotificationsService
{
	private static final Logger logger = LogManager.getLogger(UserNotificationsService.class);

	@Autowired
	private WebsocketSender websocketSender;

	private final ConcurrentHashMap<Integer, UserNotificationsQueue> adUserId2notifications = new ConcurrentHashMap<>();

	private final AtomicBoolean subscribedToEventBus = new AtomicBoolean(false);

	@EventListener
	private void onUserLanguageChanged(final LanguagedChangedEvent event)
	{
		final UserNotificationsQueue notificationsQueue = adUserId2notifications.get(event.getAdUserId());
		if(notificationsQueue != null)
		{
			notificationsQueue.setLanguage(event.getAdLanguage());
		}
	}

	private void subscribeToEventTopicsIfNeeded()
	{
		if (!subscribedToEventBus.getAndSet(true))
		{
			final IEventBusFactory eventBusFactory = Services.get(IEventBusFactory.class);
			eventBusFactory.getAvailableUserNotificationsTopics()
					.stream()
					.map(topic -> eventBusFactory.getEventBus(topic))
					.forEach(eventBus -> eventBus.subscribe(this::forwardEventToNotificationsQueues));
		}
	}

	public synchronized void enableForSession(final String sessionId, final int adUserId, final String adLanguage)
	{
		logger.trace("Enabling for sessionId={}, adUserId={}, adLanguage={}", sessionId, adUserId, adLanguage);

		final UserNotificationsQueue notificationsQueue = adUserId2notifications.computeIfAbsent(adUserId,
				theSessionId -> new UserNotificationsQueue(adUserId, adLanguage, websocketSender));
		notificationsQueue.addActiveSessionId(sessionId);

		subscribeToEventTopicsIfNeeded();
	}

	public synchronized void disableForSession(final String sessionId)
	{
		// TODO: implement
	}

	public String getWebsocketEndpoint(final int adUserId)
	{
		return getNotificationsQueue(adUserId).getWebsocketEndpoint();
	}

	private UserNotificationsQueue getNotificationsQueue(final int adUserId)
	{
		final UserNotificationsQueue notificationsQueue = adUserId2notifications.get(adUserId);
		if (notificationsQueue == null)
		{
			throw new IllegalArgumentException("No notifications queue found for AD_User_ID=" + adUserId);
		}
		return notificationsQueue;
	}

	public UserNotificationsList getNotifications(final int adUserId, final int limit)
	{
		return getNotificationsQueue(adUserId).getNotificationsAsList(limit);
	}

	private void forwardEventToNotificationsQueues(final IEventBus eventBus, final Event event)
	{
		logger.trace("Got event from {}: {}", eventBus, event);

		final UserNotification notification = UserNotification.of(event);
		if (event.isAllRecipients())
		{
			logger.trace("Sending event to ALL: {}", adUserId2notifications);
			adUserId2notifications.forEachValue(100, notificationsQueue -> notificationsQueue.addNotification(notification.copy()));
		}
		else
		{
			logger.trace("Sending event to event's recipients");
			for (final int recipientUserId : event.getRecipientUserIds())
			{
				final UserNotificationsQueue notificationsQueue = adUserId2notifications.get(recipientUserId);
				if (notificationsQueue == null)
				{
					logger.trace("No notification queue was found for recipientUserId={}", recipientUserId);
					continue;
				}

				notificationsQueue.addNotification(notification.copy());
			}
		}
	}

	public void markNotificationAsRead(final int adUserId, final String notificationId)
	{
		getNotificationsQueue(adUserId).markAsRead(notificationId);
	}

	public void markAllNotificationsAsRead(final int adUserId)
	{
		getNotificationsQueue(adUserId).markAllAsRead();
	}

	public int getNotificationsUnreadCount(final int adUserId)
	{
		return getNotificationsQueue(adUserId)
				.getUnreadCount();
	}

}
