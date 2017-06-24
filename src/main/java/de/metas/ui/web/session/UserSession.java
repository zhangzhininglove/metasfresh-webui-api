package de.metas.ui.web.session;

import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

import org.adempiere.ad.security.IUserRolePermissions;
import org.adempiere.ad.security.UserRolePermissionsKey;
import org.adempiere.util.Check;
import org.compiere.Adempiere;
import org.compiere.util.Env;
import org.compiere.util.Evaluatee;
import org.compiere.util.Evaluatees;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import de.metas.i18n.Language;
import de.metas.logging.LogManager;
import de.metas.ui.web.base.session.UserPreference;
import de.metas.ui.web.exceptions.DeprecatedRestAPINotAllowedException;
import de.metas.ui.web.login.exceptions.AlreadyLoggedInException;
import de.metas.ui.web.login.exceptions.NotLoggedInAsSysAdminException;
import de.metas.ui.web.login.exceptions.NotLoggedInException;
import de.metas.ui.web.websocket.WebSocketConfig;
import de.metas.ui.web.window.datatypes.json.JSONLookupValue;
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

/**
 * User Session service
 *
 * @author metas-dev <dev@metasfresh.com>
 */
@Service
public class UserSession
{
	/**
	 * Gets current {@link UserSession} if any
	 *
	 * NOTE: please use this method only if there is no other way to get the {@link UserSession}
	 *
	 * @return {@link UserSession} or null
	 */
	public static UserSession getCurrentOrNull()
	{
		//
		// Quickly check if the session scoped UserSession bean will be really available
		// NOTE: it's not about that the object will be null but if it's method calls will be really working
		if (RequestContextHolder.getRequestAttributes() == null)
		{
			return null;
		}

		//
		UserSession userSession = _staticUserSession;
		if (userSession == null)
		{
			synchronized (UserSession.class)
			{
				if (_staticUserSession == null)
				{
					userSession = _staticUserSession = Adempiere.getSpringApplicationContext().getBean(UserSession.class);
				}
			}
		}
		return userSession;
	}

	public static UserSession getCurrentIfMatchingOrNull(final int adUserId)
	{
		final UserSession userSession = getCurrentOrNull();
		if (userSession == null)
		{
			return null;
		}
		if (userSession.getAD_User_ID() != adUserId)
		{
			return null;
		}

		return userSession;
	}

	/**
	 * Gets current {@link UserSession}.
	 *
	 * NOTE: please use this method only if there is no other way to get the {@link UserSession}
	 *
	 * @return user session; never returns null
	 * @throws NotLoggedInException
	 */
	public static UserSession getCurrent() throws NotLoggedInException
	{
		final UserSession userSession = getCurrentOrNull();
		if (userSession == null)
		{
			throw new NotLoggedInException("no session found");
		}
		return userSession;
	}

	/**
	 * Gets current permissions.
	 *
	 * @return permissions; never returns null
	 * @throws NotLoggedInException
	 */
	public static IUserRolePermissions getCurrentPermissions()
	{
		return getCurrent().getUserRolePermissions();
	}

	// services
	static final transient Logger logger = LogManager.getLogger(UserSession.class);
	private final transient ApplicationEventPublisher eventPublisher;

	private static UserSession _staticUserSession = null;

	@Autowired
	private InternalUserSessionData data; // session scoped

	@Autowired
	public UserSession(final ApplicationEventPublisher eventPublisher)
	{
		this.eventPublisher = eventPublisher;
	}

	public String getSessionId()
	{
		return data.getSessionId();
	}

	/**
	 * @return session's context
	 * @see de.metas.ui.web.session.WebRestApiContextProvider
	 */
	public Properties getCtx()
	{
		return data.getCtx();
	}

	public Evaluatee toEvaluatee()
	{
		return Evaluatees.ofCtx(data.getCtx());
	}

	public UserPreference getUserPreference()
	{
		return data.getUserPreference();
	}

	public boolean isLoggedIn()
	{
		return data.isLoggedIn();
	}

	public void setLoggedIn(final boolean loggedIn)
	{
		final boolean currentlyLoggedIn = data.isLoggedIn();
		if (currentlyLoggedIn == loggedIn)
		{
			return;
		}

		if (loggedIn)
		{
			data.setLoggedIn(true);
			data.getUserPreference().loadPreference(data.getCtx());
			logger.trace("User session logged in: {}", data);
		}
		else
		{
			data.setLoggedIn(false);
			data.setUserPreference(new UserPreference());
			logger.trace("User session logged out: {}", data);
		}
	}

	public void assertLoggedIn()
	{
		if (!data.isLoggedIn())
		{
			throw new NotLoggedInException();
		}
	}
	
	public void assertLoggedInAsSysAdmin()
	{
		assertLoggedIn();
		
		final int adRoleId = data.getAD_Role_ID();
		if(adRoleId != IUserRolePermissions.SYSTEM_ROLE_ID)
		{
			throw new NotLoggedInAsSysAdminException();
		}
	}


	public void assertNotLoggedIn()
	{
		if (data.isLoggedIn())
		{
			throw new AlreadyLoggedInException();
		}
	}

	/**
	 * Sets user preferred language.
	 *
	 * Fires {@link LanguagedChangedEvent}.
	 *
	 * @param adLanguage
	 * @return old AD_Language
	 */
	String setAD_Language(final String adLanguage)
	{
		Check.assumeNotEmpty(adLanguage, "adLanguage is not empty");
		final Language lang = Language.getLanguage(adLanguage);
		final String adLanguageOld = data.verifyLanguageAndSet(lang);
		final String adLanguageNew = data.getAdLanguage();
		logger.info("Changed AD_Language: {} -> {}, {}", adLanguageOld, adLanguageNew, lang);

		// Fire event
		if (!Objects.equals(adLanguageOld, adLanguageNew))
		{
			eventPublisher.publishEvent(new LanguagedChangedEvent(adLanguageNew, getAD_User_ID()));
		}

		return adLanguageOld;
	}

	public int getAD_Client_ID()
	{
		return data.getAD_Client_ID();
	}

	public String getAD_Language()
	{
		return data.getAdLanguage();
	}

	public Language getLanguage()
	{
		return data.getLanguage();
	}

	public JSONLookupValue getLanguageAsJson()
	{
		final Language language = getLanguage();
		return JSONLookupValue.of(language.getAD_Language(), language.getName());
	}

	public Locale getLocale()
	{
		return data.getLocale();
	}

	public boolean isUseHttpAcceptLanguage()
	{
		return data.isUseHttpAcceptLanguage();
	}

	public void setUseHttpAcceptLanguage(final boolean useHttpAcceptLanguage)
	{
		final boolean useHttpAcceptLanguageOld = data.isUseHttpAcceptLanguage();
		data.setUseHttpAcceptLanguage(useHttpAcceptLanguage);
		logSettingChanged("UseHttpAcceptLanguage", useHttpAcceptLanguage, useHttpAcceptLanguageOld);
	}

	public int getAD_User_ID()
	{
		return data.getAD_User_ID();
	}

	public String getUserName()
	{
		return data.getUserName();
	}

	public String getRoleName()
	{
		return data.getRoleName();
	}

	public UserRolePermissionsKey getUserRolePermissionsKey()
	{
		// TODO: cache the permissions key
		return UserRolePermissionsKey.of(data.getCtx());
	}

	public IUserRolePermissions getUserRolePermissions()
	{
		return Env.getUserRolePermissions(data.getCtx());
	}

	public String getAvatarId()
	{
		return data.getAvatarId();
	}

	public String getUserEmail()
	{
		return data.getUserEmail();
	}

	public String getUserFullname()
	{
		return data.getUserFullname();
	}

	String setAvatarId(final String avatarId)
	{
		final String avatarIdOld = data.getAvatarId();
		data.setAvatarId(avatarId);
		return avatarIdOld;
	}

	String setUserEmail(final String userEmail)
	{
		final String userEmailOld = data.getUserEmail();
		data.setUserEmail(userEmail);
		return userEmailOld;
	}

	String setUserFullname(final String userFullname)
	{
		final String userFullnameOld = data.getUserFullname();
		data.setUserFullname(userFullname);
		return userFullnameOld;
	}
	
	/** @return websocket notifications endpoint on which the frontend shall listen */
	public String getWebsocketEndpoint()
	{		
		return WebSocketConfig.buildUserSessionTopicName(getAD_User_ID());
	}

	public void assertDeprecatedRestAPIAllowed()
	{
		if (!data.isAllowDeprecatedRestAPI())
		{
			throw new DeprecatedRestAPINotAllowedException();
		}
	}

	private static final void logSettingChanged(final String name, final Object value, final Object valueOld)
	{
		UserSession.logger.warn("/*********************************************************************************************\\");
		UserSession.logger.warn("Setting changed: {} = {} (Old: {})", name, value, valueOld);
		UserSession.logger.warn("\\*********************************************************************************************/");
	}

	public boolean isAllowDeprecatedRestAPI()
	{
		return data.isAllowDeprecatedRestAPI();
	}

	public void setAllowDeprecatedRestAPI(final boolean allowDeprecatedRestAPI)
	{
		final boolean allowDeprecatedRestAPIOld = data.isAllowDeprecatedRestAPI();
		data.setAllowDeprecatedRestAPI(allowDeprecatedRestAPI);
		logSettingChanged("AllowDeprecatedRestAPI", allowDeprecatedRestAPI, allowDeprecatedRestAPIOld);
	}

	public boolean isShowColumnNamesForCaption()
	{
		return data.isShowColumnNamesForCaption();
	}

	public void setShowColumnNamesForCaption(final boolean showColumnNamesForCaption)
	{
		final boolean showColumnNamesForCaptionOld = data.isShowColumnNamesForCaption();
		data.setShowColumnNamesForCaption(showColumnNamesForCaption);
		logSettingChanged("ShowColumnNamesForCaption", showColumnNamesForCaption, showColumnNamesForCaptionOld);
	}

	public void setHttpCacheMaxAge(final int httpCacheMaxAge)
	{
		final int httpCacheMaxAgeOld = data.getHttpCacheMaxAge();
		data.setHttpCacheMaxAge(httpCacheMaxAge);
		logSettingChanged("HttpCacheMaxAge", httpCacheMaxAge, httpCacheMaxAgeOld);
	}

	public int getHttpCacheMaxAge()
	{
		return data.getHttpCacheMaxAge();
	}

	/**
	 * Event fired when the user language was changed.
	 * Usually it is user triggered.
	 *
	 * @author metas-dev <dev@metasfresh.com>
	 *
	 */
	@lombok.Value
	public static class LanguagedChangedEvent
	{
		@NonNull
		private final String adLanguage;
		private final int adUserId;
	}
}
