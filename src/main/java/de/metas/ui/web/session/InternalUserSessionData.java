package de.metas.ui.web.session;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;
import java.util.Properties;

import org.compiere.util.Env;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.google.common.base.MoreObjects;

import de.metas.i18n.Language;
import de.metas.ui.web.base.session.UserPreference;

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
 * Internal {@link UserSession} data.
 * 
 * NOTE: it's here and not inside UserSession class because it seems spring could not discover it
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@Component
@Primary
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@lombok.Data
/* package */ class InternalUserSessionData implements Serializable, InitializingBean
{
	private static final long serialVersionUID = 4046535476486036184L;

	// ---------------------------------------------------------------------------------------------
	// NOTE: make sure none of those fields are "final" because this will prevent deserialization
	// ---------------------------------------------------------------------------------------------

	//
	// Actual session data
	private String sessionId = null;
	private UserPreference userPreference = null;
	private boolean loggedIn = false;
	private Locale locale = null;
	
	//
	// User info
	private String userFullname;
	private String userEmail;
	private String avatarId;

	//
	// Defaults
	@Value("${metasfresh.webui.debug.showColumnNamesForCaption:false}")
	private boolean defaultShowColumnNamesForCaption;
	private boolean showColumnNamesForCaption;
	//
	@Value("${metasfresh.webui.debug.allowDeprecatedRestAPI:false}")
	private boolean defaultAllowDeprecatedRestAPI;
	private boolean allowDeprecatedRestAPI;
	
	@Value("${metasfresh.webui.http.cache.maxAge:60}")
	private int defaultHttpCacheMaxAge;
	private int httpCacheMaxAge;
	
	// TODO: set default to "true" after https://github.com/metasfresh/metasfresh-webui-frontend/issues/819
	@Value("${metasfresh.webui.http.use.AcceptLanguage:false}")
	private boolean defaultUseHttpAcceptLanguage;
	private boolean useHttpAcceptLanguage;


	//
	public InternalUserSessionData()
	{
		final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		sessionId = requestAttributes.getSessionId();

		userPreference = new UserPreference();
		loggedIn = false;

		//
		// Set initial language
		try
		{
			final Locale locale = LocaleContextHolder.getLocale();
			final Language language = Language.getLanguage(locale);
			verifyLanguageAndSet(language);
		}
		catch (final Exception e)
		{
			UserSession.logger.warn("Failed setting the language, but moving on", e);
		}

		UserSession.logger.trace("User session created: {}", this);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		//
		// Set initial properties
		setShowColumnNamesForCaption(defaultShowColumnNamesForCaption);
		setAllowDeprecatedRestAPI(defaultAllowDeprecatedRestAPI);
		setHttpCacheMaxAge(defaultHttpCacheMaxAge);
		setUseHttpAcceptLanguage(defaultUseHttpAcceptLanguage);
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.add("sessionId", sessionId)
				.add("loggedIn", loggedIn)
				.add("locale", locale)
				.add("userPreferences", userPreference)
				.add("defaultUseHttpAcceptLanguage", defaultUseHttpAcceptLanguage)
				.toString();
	}

	private void writeObject(final java.io.ObjectOutputStream out) throws IOException
	{
		out.defaultWriteObject();

		UserSession.logger.trace("User session serialized: {}", this);
	}

	private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();

		UserSession.logger.trace("User session deserialized: {}", this);
	}
	
	Properties getCtx()
	{
		return Env.getCtx();
	}

	public int getAD_Client_ID()
	{
		return Env.getAD_Client_ID(getCtx());
	}

	public int getAD_User_ID()
	{
		return Env.getAD_User_ID(getCtx());
	}

	public int getAD_Role_ID()
	{
		return Env.getAD_Role_ID(getCtx());
	}

	public String getUserName()
	{
		return Env.getContext(getCtx(), Env.CTXNAME_AD_User_Name);
	}

	public String getRoleName()
	{
		return Env.getContext(getCtx(), Env.CTXNAME_AD_Role_Name);
	}

	String getAdLanguage()
	{
		return Env.getContext(getCtx(), Env.CTXNAME_AD_Language);
	}

	Language getLanguage()
	{
		return Env.getLanguage(getCtx());
	}

	String verifyLanguageAndSet(final Language lang)
	{
		final Properties ctx = getCtx();
		final String adLanguageOld = Env.getContext(ctx, Env.CTXNAME_AD_Language);

		//
		// Check the language (and update it if needed)
		Env.verifyLanguage(lang);

		//
		// Actual update
		final String adLanguageNew = lang.getAD_Language();
		Env.setContext(ctx, Env.CTXNAME_AD_Language, adLanguageNew);
		this.locale = lang.getLocale();
		UserSession.logger.info("Changed AD_Language: {} -> {}, {}", adLanguageOld, adLanguageNew, lang);

		return adLanguageOld;
	}
}
