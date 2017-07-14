package de.metas.ui.web.window.descriptor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.adempiere.ad.expression.api.ConstantLogicExpression;
import org.adempiere.ad.expression.api.IExpression;
import org.adempiere.ad.expression.api.ILogicExpression;
import org.adempiere.ad.expression.api.impl.LogicExpressionCompiler;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.compiere.util.DisplayType;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import de.metas.i18n.ITranslatableString;
import de.metas.i18n.ImmutableTranslatableString;
import de.metas.logging.LogManager;
import de.metas.ui.web.window.WindowConstants;
import de.metas.ui.web.window.datatypes.LookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.IntegerLookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.StringLookupValue;
import de.metas.ui.web.window.datatypes.json.JSONDate;
import de.metas.ui.web.window.datatypes.json.JSONLookupValue;
import de.metas.ui.web.window.descriptor.DocumentFieldDependencyMap.DependencyType;
import de.metas.ui.web.window.descriptor.DocumentLayoutElementFieldDescriptor.LookupSource;
import de.metas.ui.web.window.descriptor.LookupDescriptorProvider.LookupScope;
import de.metas.ui.web.window.model.IDocumentFieldValueProvider;
import de.metas.ui.web.window.model.lookup.LookupDataSource;
import de.metas.ui.web.window.model.lookup.LookupDataSourceFactory;
import de.metas.ui.web.window.model.lookup.LookupValueByIdSupplier;
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

@SuppressWarnings("serial")
public final class DocumentFieldDescriptor implements Serializable
{
	public static final Builder builder(final String fieldName)
	{
		return new Builder(fieldName);
	}

	private static final Logger logger = LogManager.getLogger(DocumentFieldDescriptor.class);

	/** Internal field name (aka ColumnName) */
	private final String fieldName;
	private final ITranslatableString caption;
	private final ITranslatableString description;
	/** Detail ID or null if this is a field in main sections */
	private final DetailId detailId;

	/** Is this the key field ? */
	private final boolean key;
	private final boolean parentLink;
	private final boolean calculated;

	private final DocumentFieldWidgetType widgetType;
	private final boolean allowShowPassword; // in case widgetType is Password
	private final ButtonFieldActionDescriptor buttonActionDescriptor;

	private final Class<?> valueClass;

	private final LookupDescriptorProvider lookupDescriptorProvider;
	private final boolean supportZoomInto;

	private final boolean virtualField;
	private final Optional<IDocumentFieldValueProvider> virtualFieldValueProvider;

	private final Optional<IExpression<?>> defaultValueExpression;
	private final ImmutableList<IDocumentFieldCallout> callouts;

	public static enum Characteristic
	{
		PublicField //
		, AdvancedField //
		, SideListField //
		, GridViewField //
		, AllowFiltering //
		//
		, SpecialField_DocumentNo //
		, SpecialField_DocStatus //
		, SpecialField_DocAction //
		// , SpecialField_DocumentSummary //
		;
	};

	private static final List<Characteristic> SPECIALFIELDS_ToExcludeFromLayout = ImmutableList.of(
			// Characteristic.SpecialField_DocumentNo // NOP, don't exclude it (see https://github.com/metasfresh/metasfresh-webui-api/issues/291 )
			Characteristic.SpecialField_DocStatus //
			, Characteristic.SpecialField_DocAction //
	// , SpecialField_DocumentSummary // NOP, don't exclude DocumentSummary because if it's layout it shall be editable at least when new (e.g. C_BPartner.Name)
	);

	private final Set<Characteristic> characteristics;

	private final ILogicExpression readonlyLogic;
	private final boolean alwaysUpdateable;
	private final ILogicExpression displayLogic;
	private final ILogicExpression mandatoryLogic;

	private final Optional<DocumentFieldDataBindingDescriptor> dataBinding;

	private final DocumentFieldDependencyMap dependencies;

	private DocumentFieldDescriptor(final Builder builder)
	{
		super();
		fieldName = Preconditions.checkNotNull(builder.fieldName, "name is null");
		caption = builder.getCaption();
		description = builder.getDescription();
		detailId = builder.getDetailId();

		key = builder.isKey();
		parentLink = builder.parentLink;
		calculated = builder.isCalculated();

		widgetType = builder.getWidgetType();
		allowShowPassword = builder.isAllowShowPassword();
		buttonActionDescriptor = builder.getButtonActionDescriptor();
		valueClass = builder.getValueClass();

		lookupDescriptorProvider = builder.getLookupDescriptorProvider();
		supportZoomInto = builder.isSupportZoomInto();

		defaultValueExpression = Preconditions.checkNotNull(builder.defaultValueExpression, "defaultValueExpression not null");

		virtualField = builder.isVirtualField();
		virtualFieldValueProvider = builder.getVirtualFieldValueProvider();

		characteristics = Sets.immutableEnumSet(builder.characteristics);
		readonlyLogic = builder.getReadonlyLogicEffective();
		alwaysUpdateable = builder.alwaysUpdateable;
		displayLogic = builder.displayLogic;
		mandatoryLogic = builder.getMandatoryLogicEffective();

		dataBinding = builder.getDataBinding();

		dependencies = builder.buildDependencies();

		callouts = builder.buildCallouts();
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.add("fieldName", fieldName)
				.add("detailId", detailId)
				.add("widgetType", widgetType)
				.add("characteristics", characteristics.isEmpty() ? null : characteristics)
				.add("fieldDataBinding", dataBinding)
				.toString();
	}

	public String getFieldName()
	{
		return fieldName;
	}

	public ITranslatableString getCaption()
	{
		return caption;
	}

	public ITranslatableString getDescription()
	{
		return description;
	}

	public DetailId getDetailId()
	{
		return detailId;
	}

	public boolean isKey()
	{
		return key;
	}

	public boolean isParentLink()
	{
		return parentLink;
	}

	public boolean isVirtualField()
	{
		return virtualField;
	}

	public Optional<IDocumentFieldValueProvider> getVirtualFieldValueProvider()
	{
		return virtualFieldValueProvider;
	}

	public boolean isCalculated()
	{
		return calculated;
	}

	public DocumentFieldWidgetType getWidgetType()
	{
		return widgetType;
	}
	
	public boolean isAllowShowPassword()
	{
		return allowShowPassword;
	}

	public ButtonFieldActionDescriptor getButtonActionDescriptor()
	{
		return buttonActionDescriptor;
	}

	public Class<?> getValueClass()
	{
		return valueClass;
	}

	public boolean isSupportZoomInto()
	{
		return supportZoomInto;
	}

	public LookupDescriptor getLookupDescriptor(final LookupScope scope)
	{
		return lookupDescriptorProvider.provideForScope(scope);
	}

	public LookupSource getLookupSourceType()
	{
		final LookupDescriptor lookupDescriptor = lookupDescriptorProvider.provideForScope(LookupScope.DocumentField);
		return lookupDescriptor == null ? null : lookupDescriptor.getLookupSourceType();
	}

	public Optional<String> getLookupTableName()
	{
		return extractLookupTableName(lookupDescriptorProvider);
	}

	private static final Optional<String> extractLookupTableName(final LookupDescriptorProvider lookupDescriptorProvider)
	{
		final LookupDescriptor lookupDescriptor = lookupDescriptorProvider.provideForScope(LookupScope.DocumentField);
		return lookupDescriptor == null ? Optional.empty() : lookupDescriptor.getTableName();
	}

	@Nullable
	public LookupDataSource createLookupDataSource(final LookupScope scope)
	{
		final LookupDescriptor lookupDescriptor = getLookupDescriptor(scope);
		if (lookupDescriptor == null)
		{
			return null;
		}

		return LookupDataSourceFactory.instance.getLookupDataSource(lookupDescriptor);

	}

	public Optional<IExpression<?>> getDefaultValueExpression()
	{
		return defaultValueExpression;
	}

	public boolean hasCharacteristic(final Characteristic c)
	{
		return characteristics.contains(c);
	}

	public ILogicExpression getReadonlyLogic()
	{
		return readonlyLogic;
	}

	public boolean isAlwaysUpdateable()
	{
		return alwaysUpdateable;
	}

	public ILogicExpression getDisplayLogic()
	{
		return displayLogic;
	}

	public ILogicExpression getMandatoryLogic()
	{
		return mandatoryLogic;
	}

	/**
	 * @return field data binding info
	 */
	public Optional<DocumentFieldDataBindingDescriptor> getDataBinding()
	{
		return dataBinding;
	}

	public <T extends DocumentFieldDataBindingDescriptor> T getDataBindingNotNull(final Class<T> bindingClass)
	{
		@SuppressWarnings("unchecked")
		final T dataBindingCasted = (T)dataBinding.orElseThrow(() -> new IllegalStateException("No databinding defined for " + this));
		return dataBindingCasted;
	}

	public DocumentFieldDependencyMap getDependencies()
	{
		return dependencies;
	}

	public Object convertToValueClass(final Object value, final LookupValueByIdSupplier lookupDataSource)
	{
		return convertToValueClass(fieldName, value, widgetType, valueClass, lookupDataSource);
	}

	/**
	 * Converts given value to target class.
	 *
	 * @param value value to be converted
	 * @param targetType target type
	 * @param widgetType optional widget type
	 * @param lookupDataSource optional Lookup data source, if needed
	 * @return converted value
	 */
	public <T> T convertToValueClass(final Object value, final DocumentFieldWidgetType widgetType, final Class<T> targetType, final LookupValueByIdSupplier lookupDataSource)
	{
		return convertToValueClass(fieldName, value, widgetType, targetType, lookupDataSource);
	}

	/**
	 * Converts given value to target class.
	 *
	 * @param fieldName field name, needed only for logging purposes
	 * @param value value to be converted
	 * @param widgetType widget type (optional)
	 * @param targetType target type
	 * @param lookupDataSource optional Lookup data source, if needed
	 * @return converted value
	 */
	public static <T> T convertToValueClass( //
			final String fieldName //
			, final Object value //
			, final DocumentFieldWidgetType widgetType //
			, final Class<T> targetType //
			, final LookupValueByIdSupplier lookupDataSource //
	)
	{
		if (value == null)
		{
			return null;
		}

		final Class<?> fromType = value.getClass();

		try
		{
			// Corner case: we need to convert Timestamp(which extends Date) to strict Date because else all value changed comparing methods will fail
			if (java.util.Date.class.equals(targetType) && Timestamp.class.equals(fromType))
			{
				@SuppressWarnings("unchecked")
				final T valueConv = (T)JSONDate.fromTimestamp((Timestamp)value);
				return valueConv;
			}

			if (targetType.isAssignableFrom(fromType))
			{
				if (!targetType.equals(fromType))
				{
					logger.warn("Possible optimization issue: target type is assignable from source type, but they are not the same class."
							+ "\n In future we will disallow this case, so please check and fix it."
							+ "\n Field name: " + fieldName
							+ "\n Target type: " + targetType
							+ "\n Source type: " + fromType
							+ "\n Value: " + value
							+ "\n LookupDataSource: " + lookupDataSource);
				}

				@SuppressWarnings("unchecked")
				final T valueConv = (T)value;
				return valueConv;
			}

			if (String.class == targetType)
			{
				if (Map.class.isAssignableFrom(fromType))
				{
					// this is not allowed for consistency. let it fail.
				}
				// For any other case, blindly convert it to string
				else
				{
					@SuppressWarnings("unchecked")
					final T valueConv = (T)value.toString();
					return valueConv;
				}
			}
			else if (java.util.Date.class == targetType)
			{
				if (value instanceof String)
				{
					@SuppressWarnings("unchecked")
					final T valueConv = (T)JSONDate.fromJson((String)value, widgetType);
					return valueConv;
				}
			}
			else if (Integer.class == targetType || int.class == targetType)
			{
				if (value instanceof String)
				{
					final String valueStr = (String)value;
					if(valueStr.isEmpty())
					{
						return null;
					}
					
					final BigDecimal valueBD = new BigDecimal(valueStr); 
					@SuppressWarnings("unchecked")
					final T valueConv = (T)(Integer)valueBD.intValueExact();
					return valueConv;
				}
				else if (value instanceof Number)
				{
					@SuppressWarnings("unchecked")
					final T valueConv = (T)(Integer)((Number)value).intValue();
					return valueConv;
				}
				else if (value instanceof LookupValue)
				{
					@SuppressWarnings("unchecked")
					final T valueConv = (T)(Integer)((LookupValue)value).getIdAsInt();
					return valueConv;
				}
				else if (value instanceof Map)
				{
					@SuppressWarnings("unchecked")
					final Map<String, String> map = (Map<String, String>)value;
					final IntegerLookupValue lookupValue = JSONLookupValue.integerLookupValueFromJsonMap(map);
					@SuppressWarnings("unchecked")
					final T valueConv = (T)(Integer)lookupValue.getIdAsInt();
					return valueConv;
				}

			}
			else if (BigDecimal.class == targetType)
			{
				if (String.class == fromType)
				{
					final String valueStr = (String)value;
					@SuppressWarnings("unchecked")
					final T valueConv = (T)(valueStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(valueStr));
					return valueConv;
				}
				else if (Integer.class == fromType || int.class == fromType)
				{
					final int valueInt = (int)value;
					@SuppressWarnings("unchecked")
					final T valueConv = (T)BigDecimal.valueOf(valueInt);
					return valueConv;
				}
			}
			else if (Boolean.class == targetType)
			{
				final Object valueToConv;
				if (value instanceof StringLookupValue)
				{
					// If String lookup value then consider only the Key.
					// usage example 1: the Posted column which can be Y, N and some other error codes.
					// In this case we want to convert the "Y" to "true".
					// usage example 2: some column which is List and the reference is "_YesNo".
					valueToConv = ((StringLookupValue)value).getIdAsString();
				}
				else
				{
					valueToConv = value;
				}

				@SuppressWarnings("unchecked")
				final T valueConv = (T)DisplayType.toBoolean(valueToConv, Boolean.FALSE);
				return valueConv;
			}
			else if (IntegerLookupValue.class == targetType)
			{
				if (Map.class.isAssignableFrom(fromType))
				{
					@SuppressWarnings("unchecked")
					final Map<String, String> map = (Map<String, String>)value;
					final IntegerLookupValue lookupValue = JSONLookupValue.integerLookupValueFromJsonMap(map);

					if (Check.isEmpty(lookupValue.getDisplayName(), true) && lookupDataSource != null)
					{
						// corner case: the frontend sent a lookup value like '{ 1234567 : "" }'
						// => we need to resolve the name against the lookup
						// see https://github.com/metasfresh/metasfresh-webui/issues/230
						final LookupValue lookupValueResolved = lookupDataSource.findById(lookupValue.getId());
						return convertToValueClass(fieldName, lookupValueResolved, widgetType, targetType, /* lookupDataSource */null);
					}
					else
					{
						@SuppressWarnings("unchecked")
						final T valueConv = (T)lookupValue;
						return valueConv;
					}
				}
				else if (Number.class.isAssignableFrom(fromType))
				{
					final int valueInt = ((Number)value).intValue();
					if (lookupDataSource != null)
					{
						final LookupValue valueLookup = lookupDataSource.findById(valueInt);
						final T valueConv = convertToValueClass(fieldName, valueLookup, widgetType, targetType, /* lookupDataSource */null);
						// TODO: what if valueConv was not found?
						return valueConv;
					}
				}
				else if (String.class == fromType)
				{
					final String valueStr = (String)value;
					if (valueStr.isEmpty())
					{
						return null;
					}

					if (lookupDataSource != null)
					{
						final LookupValue valueLookup = lookupDataSource.findById(valueStr);
						final T valueConv = convertToValueClass(fieldName, valueLookup, widgetType, targetType, /* lookupDataSource */null);
						// TODO: what if valueConv was not found?
						return valueConv;
					}
				}
				else if (StringLookupValue.class == fromType)
				{
					// TODO: implement https://github.com/metasfresh/metasfresh-webui-api/issues/417
					final StringLookupValue stringLookupValue = (StringLookupValue)value;
					@SuppressWarnings("unchecked")
					final T valueConv = (T)IntegerLookupValue.of(stringLookupValue);
					return valueConv;
				}
			}
			else if (StringLookupValue.class == targetType)
			{
				if (Map.class.isAssignableFrom(fromType))
				{
					@SuppressWarnings("unchecked")
					final Map<String, String> map = (Map<String, String>)value;
					final StringLookupValue lookupValue = JSONLookupValue.stringLookupValueFromJsonMap(map);

					if (Check.isEmpty(lookupValue.getDisplayName(), true) && lookupDataSource != null)
					{
						// corner case: the frontend sent a lookup value like '{ "someKey" : "" }'
						// => we need to resolve the name against the lookup
						// see https://github.com/metasfresh/metasfresh-webui/issues/230
						final LookupValue lookupValueResolved = lookupDataSource.findById(lookupValue.getId());
						return convertToValueClass(fieldName, lookupValueResolved, widgetType, targetType, /* lookupDataSource */null);
					}
					else
					{
						@SuppressWarnings("unchecked")
						final T valueConv = (T)lookupValue;
						return valueConv;
					}
				}
				else if (String.class == fromType)
				{
					final String valueStr = (String)value;
					if (valueStr.isEmpty())
					{
						return null;
					}

					if (lookupDataSource != null)
					{
						final LookupValue valueLookup = lookupDataSource.findById(valueStr);
						final T valueConv = convertToValueClass(fieldName, valueLookup, widgetType, targetType, /* lookupDataSource */null);
						// TODO: what if valueConv was not found?
						return valueConv;
					}
				}
				else if (IntegerLookupValue.class == fromType)
				{
					final IntegerLookupValue lookupValueInt = (IntegerLookupValue)value;
					@SuppressWarnings("unchecked")
					final T valueConv = (T)StringLookupValue.of(lookupValueInt.getIdAsString(), lookupValueInt.getDisplayName());
					return valueConv;
				}
			}
		}
		catch (final Exception e)
		{
			throw new AdempiereException("Failed converting " + fieldName + "'s value '" + value + "' (" + fromType + ") to " + targetType
					+ "\n LookupDataSource: " + lookupDataSource //
					+ "\n Widget type: " + widgetType, e);
		}

		throw new AdempiereException("Cannot convert " + fieldName + "'s value '" + value + "' (" + fromType + ") to " + targetType
				+ "\n LookupDataSource: " + lookupDataSource //
		);
	}

	/* package */List<IDocumentFieldCallout> getCallouts()
	{
		return callouts;
	}

	/**
	 * Builder
	 */
	public static final class Builder
	{
		private DocumentFieldDescriptor _fieldBuilt;

		private final String fieldName;
		private ITranslatableString caption;
		private ITranslatableString description;
		public DetailId _detailId;

		private boolean key = false;
		private boolean parentLink = false;
		private boolean virtualField;
		private Optional<IDocumentFieldValueProvider> virtualFieldValueProvider = Optional.empty();
		private boolean calculated;

		private DocumentFieldWidgetType _widgetType;
		private Class<?> _valueClass;
		private boolean _allowShowPassword = false; // in case widgetType is Password


		// Lookup
		private LookupDescriptorProvider lookupDescriptorProvider = LookupDescriptorProvider.NULL;

		private Optional<IExpression<?>> defaultValueExpression = Optional.empty();

		private final Set<Characteristic> characteristics = new TreeSet<>();
		private ILogicExpression _entityReadonlyLogic = ILogicExpression.FALSE;
		private ILogicExpression _readonlyLogic = ILogicExpression.FALSE;
		private ILogicExpression _readonlyLogicEffective = null;
		private boolean alwaysUpdateable = false;
		private ILogicExpression displayLogic = ILogicExpression.TRUE;
		private ILogicExpression _mandatoryLogic = ILogicExpression.FALSE;
		private ILogicExpression _mandatoryLogicEffective = null;

		private Optional<DocumentFieldDataBindingDescriptor> _dataBinding = Optional.empty();

		private final List<IDocumentFieldCallout> callouts = new ArrayList<>();

		private ButtonFieldActionDescriptor buttonActionDescriptor = null;

		private Builder(final String fieldName)
		{
			super();
			Check.assumeNotEmpty(fieldName, "fieldName is not empty");
			this.fieldName = fieldName;
		}

		@Override
		public String toString()
		{
			return MoreObjects.toStringHelper(this)
					.omitNullValues()
					.add("name", fieldName)
					.add("detailId", _detailId)
					.add("widgetType", _widgetType)
					.add("characteristics", characteristics.isEmpty() ? null : characteristics)
					.toString();
		}

		public DocumentFieldDescriptor getOrBuild()
		{
			if (_fieldBuilt == null)
			{
				_fieldBuilt = new DocumentFieldDescriptor(this);
			}
			return _fieldBuilt;
		}

		private final void assertNotBuilt()
		{
			if (_fieldBuilt != null)
			{
				throw new IllegalStateException("Already built: " + this);
			}
		}

		public String getFieldName()
		{
			return fieldName;
		}

		public Builder setCaption(final Map<String, String> captionTrls, final String defaultCaption)
		{
			caption = ImmutableTranslatableString.ofMap(captionTrls, defaultCaption);
			return this;
		}

		public Builder setCaption(final ITranslatableString caption)
		{
			this.caption = caption;
			return this;
		}

		public Builder setCaption(final String caption)
		{
			this.caption = ImmutableTranslatableString.constant(caption);
			return this;
		}

		public ITranslatableString getCaption()
		{
			if (caption == null)
			{
				return ImmutableTranslatableString.constant(fieldName);
			}

			return caption;
		}

		public Builder setDescription(final Map<String, String> descriptionTrls, final String defaultDescription)
		{
			description = ImmutableTranslatableString.ofMap(descriptionTrls, defaultDescription);
			return this;
		}

		public Builder setDescription(final ITranslatableString description)
		{
			this.description = description;
			return this;
		}

		public Builder setDescription(final String description)
		{
			this.description = ImmutableTranslatableString.constant(description);
			return this;
		}

		public ITranslatableString getDescription()
		{
			if (description == null)
			{
				return ImmutableTranslatableString.constant("");
			}
			return description;
		}

		/* package */Builder setDetailId(final DetailId detailId)
		{
			assertNotBuilt();
			_detailId = detailId;
			return this;
		}

		private DetailId getDetailId()
		{
			return _detailId;
		}

		/**
		 * @return true if included entity (i.e. detail tab)
		 */
		private boolean isDetail()
		{
			return getDetailId() != null;
		}

		public Builder setKey(final boolean key)
		{
			assertNotBuilt();
			this.key = key;
			return this;
		}

		public boolean isKey()
		{
			return key;
		}

		public Builder setParentLink(final boolean parentLink)
		{
			assertNotBuilt();
			this.parentLink = parentLink;
			return this;
		}

		public boolean isParentLinkEffective()
		{
			return parentLink && isDetail();
		}

		public Builder setVirtualField(final boolean virtualField)
		{
			assertNotBuilt();
			this.virtualField = virtualField;
			virtualFieldValueProvider = Optional.empty();
			return this;
		}

		public Builder setVirtualField(@NonNull final IDocumentFieldValueProvider virtualFieldValueProvider)
		{
			assertNotBuilt();
			virtualField = true;
			this.virtualFieldValueProvider = Optional.of(virtualFieldValueProvider);
			return this;
		}

		public boolean isVirtualField()
		{
			return virtualField;
		}

		private Optional<IDocumentFieldValueProvider> getVirtualFieldValueProvider()
		{
			return virtualFieldValueProvider;
		}

		public Builder setCalculated(final boolean calculated)
		{
			assertNotBuilt();
			this.calculated = calculated;
			return this;
		}

		private boolean isCalculated()
		{
			if (isVirtualField())
			{
				return true;
			}
			return calculated;
		}

		public Builder setWidgetType(final DocumentFieldWidgetType widgetType)
		{
			assertNotBuilt();
			_widgetType = widgetType;
			return this;
		}

		public DocumentFieldWidgetType getWidgetType()
		{
			Preconditions.checkNotNull(_widgetType, "widgetType is null");
			return _widgetType;
		}
		
		public Builder setAllowShowPassword(boolean allowShowPassword)
		{
			this._allowShowPassword = allowShowPassword;
			return this;
		}
		
		private boolean isAllowShowPassword()
		{
			return _allowShowPassword;
		}

		public Builder setLookupDescriptorProvider(final LookupDescriptorProvider lookupDescriptorProvider)
		{
			Check.assumeNotNull(lookupDescriptorProvider, "Parameter lookupDescriptorProvider is not null");
			this.lookupDescriptorProvider = lookupDescriptorProvider;
			return this;
		}

		public Builder setLookupDescriptorProvider(@Nullable final LookupDescriptor lookupDescriptor)
		{
			final LookupDescriptorProvider provider = lookupDescriptor != null ? LookupDescriptorProvider.singleton(lookupDescriptor) : LookupDescriptorProvider.NULL;
			setLookupDescriptorProvider(provider);
			return this;
		}

		public Builder setLookupDescriptorProvider_None()
		{
			setLookupDescriptorProvider(LookupDescriptorProvider.NULL);
			return this;
		}

		private LookupDescriptorProvider getLookupDescriptorProvider()
		{
			return lookupDescriptorProvider;
		}

		public LookupSource getLookupSourceType()
		{
			final LookupDescriptor lookupDescriptor = lookupDescriptorProvider.provideForScope(LookupScope.DocumentField);
			return lookupDescriptor == null ? null : lookupDescriptor.getLookupSourceType();
		}

		public Optional<String> getLookupTableName()
		{
			return extractLookupTableName(lookupDescriptorProvider);
		}

		public Builder setValueClass(final Class<?> valueClass)
		{
			assertNotBuilt();
			_valueClass = valueClass;
			return this;
		}

		private Class<?> getValueClass()
		{
			if (_valueClass != null)
			{
				return _valueClass;
			}

			final DocumentFieldWidgetType widgetType = getWidgetType();
			return widgetType.getValueClass();
		}

		public Builder setDefaultValueExpression(final Optional<IExpression<?>> defaultValueExpression)
		{
			assertNotBuilt();
			this.defaultValueExpression = Preconditions.checkNotNull(defaultValueExpression);
			return this;
		}

		public Builder setDefaultValueExpression(final IExpression<?> defaultValueExpression)
		{
			assertNotBuilt();
			this.defaultValueExpression = Optional.of(defaultValueExpression);
			return this;
		}

		public Builder addCharacteristic(final Characteristic c)
		{
			assertNotBuilt();
			characteristics.add(c);
			return this;
		}

		public boolean hasCharacteristic(final Characteristic c)
		{
			return characteristics.contains(c);
		}

		public Builder addCharacteristicIfTrue(final boolean test, final Characteristic c)
		{
			if (test)
			{
				addCharacteristic(c);
			}

			return this;
		}

		public Builder removeCharacteristic(final Characteristic c)
		{
			assertNotBuilt();
			characteristics.remove(c);
			return this;
		}

		public boolean isSpecialFieldToExcludeFromLayout()
		{
			return !Collections.disjoint(characteristics, SPECIALFIELDS_ToExcludeFromLayout);
		}

		/* package */ void setEntityReadonlyLogic(final ILogicExpression entityReadonlyLogic)
		{
			_entityReadonlyLogic = entityReadonlyLogic;
		}

		private ILogicExpression getEntityReadonlyLogic()
		{
			return _entityReadonlyLogic;
		}

		public Builder setReadonlyLogic(final ILogicExpression readonlyLogic)
		{
			assertNotBuilt();
			_readonlyLogic = Preconditions.checkNotNull(readonlyLogic);
			return this;
		}

		public Builder setReadonlyLogic(final boolean readonly)
		{
			setReadonlyLogic(ConstantLogicExpression.of(readonly));
			return this;
		}

		private ILogicExpression getReadonlyLogic()
		{
			return _readonlyLogic;
		}

		private ILogicExpression getReadonlyLogicEffective()
		{
			if (_readonlyLogicEffective == null)
			{
				_readonlyLogicEffective = buildReadonlyLogicEffective();
			}
			return _readonlyLogicEffective;
		}

		private ILogicExpression buildReadonlyLogicEffective()
		{
			if (isParentLinkEffective())
			{
				return ILogicExpression.TRUE;
			}

			if (isVirtualField())
			{
				return ILogicExpression.TRUE;
			}

			if (isKey())
			{
				return ILogicExpression.TRUE;
			}

			// If the tab is always readonly, we can assume any field in that tab is readonly
			final ILogicExpression entityReadonlyLogic = getEntityReadonlyLogic();
			if (entityReadonlyLogic.isConstantTrue())
			{
				return ILogicExpression.TRUE;
			}

			// Case: DocumentNo/Value special field not be readonly
			if (hasCharacteristic(Characteristic.SpecialField_DocumentNo))
			{
				return ILogicExpression.FALSE;
			}

			// Case: DocAction
			if (hasCharacteristic(Characteristic.SpecialField_DocAction))
			{
				return ILogicExpression.FALSE;
			}

			final ILogicExpression fieldReadonlyLogic = getReadonlyLogic();
			if (fieldReadonlyLogic.isConstantTrue())
			{
				return ILogicExpression.TRUE;
			}

			final String fieldName = getFieldName();
			if (WindowConstants.FIELDNAMES_CreatedUpdated.contains(fieldName))
			{
				// NOTE: from UI perspective those are readonly (i.e. it will be managed by persistence layer)
				return ILogicExpression.TRUE;
			}

			if (hasCharacteristic(Characteristic.SpecialField_DocStatus))
			{
				// NOTE: DocStatus field shall always be readonly
				return ILogicExpression.TRUE;
			}

			ILogicExpression readonlyLogic = fieldReadonlyLogic;
			// FIXME: not sure if using tabReadonlyLogic here is OK, because the tab logic shall be applied to parent tab!
			if (!entityReadonlyLogic.isConstantFalse())
			{
				readonlyLogic = entityReadonlyLogic.or(fieldReadonlyLogic);
			}

			return readonlyLogic;
		}

		public Builder setAlwaysUpdateable(final boolean alwaysUpdateable)
		{
			assertNotBuilt();
			this.alwaysUpdateable = alwaysUpdateable;
			return this;
		}

		public boolean isAlwaysUpdateable()
		{
			return alwaysUpdateable;
		}

		public Builder setDisplayLogic(final ILogicExpression displayLogic)
		{
			assertNotBuilt();
			this.displayLogic = Preconditions.checkNotNull(displayLogic);
			return this;
		}

		public Builder setDisplayLogic(final boolean display)
		{
			setDisplayLogic(ConstantLogicExpression.of(display));
			return this;
		}

		public Builder setDisplayLogic(final String displayLogic)
		{
			setDisplayLogic(LogicExpressionCompiler.instance.compile(displayLogic));
			return this;
		}

		public ILogicExpression getDisplayLogic()
		{
			return displayLogic;
		}

		public boolean isPossiblePublicField()
		{
			// Always publish the key columns, else the client won't know what to talk about ;)
			if (isKey())
			{
				return true;
			}

			// If display logic is not constant then we don't know if this field will be ever visible
			// so we are publishing it
			if (!displayLogic.isConstant())
			{
				return true;
			}

			// Publish this field only if it's displayed
			return displayLogic.isConstantTrue();
		}

		public Builder setMandatoryLogic(final ILogicExpression mandatoryLogic)
		{
			assertNotBuilt();
			_mandatoryLogic = Preconditions.checkNotNull(mandatoryLogic);
			return this;
		}

		public Builder setMandatoryLogic(final boolean mandatory)
		{
			setMandatoryLogic(ConstantLogicExpression.of(mandatory));
			return this;
		}

		private ILogicExpression getMandatoryLogicEffective()
		{
			if (_mandatoryLogicEffective == null)
			{
				_mandatoryLogicEffective = buildMandatoryLogicEffective();
			}
			return _mandatoryLogicEffective;
		}

		private final ILogicExpression buildMandatoryLogicEffective()
		{
			if (isParentLinkEffective())
			{
				return ILogicExpression.TRUE;
			}

			final String fieldName = getFieldName();
			if (WindowConstants.FIELDNAMES_CreatedUpdated.contains(fieldName))
			{
				// NOTE: from UI perspective those are not mandatory (i.e. it will be managed by persistence layer)
				return ILogicExpression.FALSE;
			}

			if (isVirtualField())
			{
				return ILogicExpression.FALSE;
			}

			// FIXME: hardcoded M_AttributeSetInstance_ID mandatory logic = false
			// Reason: even if we set it's default value to "0" some callouts are setting it to NULL,
			// and then the document saving API is failing because it considers this column as NOT filled.
			if (WindowConstants.FIELDNAME_M_AttributeSetInstance_ID.equals(fieldName))
			{
				return ILogicExpression.FALSE;
			}

			// Corner case:
			// e.g. C_Order.M_Shipper_ID has AD_Field.IsMandatory=Y, AD_Field.IsDisplayed=N, AD_Column.IsMandatory=N
			// => we need to NOT enforce setting it because it's not needed, user cannot change it and it might be no callouts to set it.
			// Else, we won't be able to save our document.
			final boolean publicField = hasCharacteristic(Characteristic.PublicField);
			final ILogicExpression mandatoryLogic = _mandatoryLogic;
			final boolean mandatory = mandatoryLogic.isConstantTrue();
			final DocumentFieldDataBindingDescriptor fieldDataBinding = getDataBinding().orElse(null);
			final boolean mandatoryDB = fieldDataBinding != null && fieldDataBinding.isMandatory();
			if (!publicField && mandatory && !mandatoryDB)
			{
				return ILogicExpression.FALSE;
			}

			// Case: DocumentNo special field shall always be mandatory
			if (hasCharacteristic(Characteristic.SpecialField_DocumentNo))
			{
				return ILogicExpression.TRUE;
			}

			if (mandatory)
			{
				return ILogicExpression.TRUE;
			}

			return mandatoryLogic;
		}

		public Builder setDataBinding(final DocumentFieldDataBindingDescriptor dataBinding)
		{
			assertNotBuilt();
			_dataBinding = Optional.ofNullable(dataBinding);
			return this;
		}

		private Optional<DocumentFieldDataBindingDescriptor> getDataBinding()
		{
			return _dataBinding;
		}

		private DocumentFieldDependencyMap buildDependencies()
		{
			final DocumentFieldDependencyMap.Builder dependencyMapBuilder = DocumentFieldDependencyMap.builder()
					.add(fieldName, getReadonlyLogicEffective().getParameters(), DependencyType.ReadonlyLogic)
					.add(fieldName, getDisplayLogic().getParameters(), DependencyType.DisplayLogic)
					.add(fieldName, getMandatoryLogicEffective().getParameters(), DependencyType.MandatoryLogic);

			final LookupDescriptor lookupDescriptor = getLookupDescriptorProvider().provideForScope(LookupScope.DocumentField);
			if (lookupDescriptor != null)
			{
				dependencyMapBuilder.add(fieldName, lookupDescriptor.getDependsOnFieldNames(), DependencyType.LookupValues);
			}

			final IDocumentFieldValueProvider virtualFieldValueProvider = getVirtualFieldValueProvider().orElse(null);
			if (virtualFieldValueProvider != null)
			{
				dependencyMapBuilder.add(fieldName, virtualFieldValueProvider.getDependsOnFieldNames(), DependencyType.FieldValue);
			}

			return dependencyMapBuilder.build();
		}

		public Builder addCallout(final IDocumentFieldCallout callout)
		{
			Check.assumeNotNull(callout, "Parameter callout is not null");

			if (callouts.contains(callout))
			{
				logger.warn("Skip adding {} because it was already added to {}", callout, this);
				return this;
			}
			callouts.add(callout);
			return this;
		}

		public Builder addCallout(final ILambdaDocumentFieldCallout lambdaCallout)
		{
			final LambdaDocumentFieldCallout callout = new LambdaDocumentFieldCallout(getFieldName(), lambdaCallout);
			addCallout(callout);
			return this;
		}

		private ImmutableList<IDocumentFieldCallout> buildCallouts()
		{
			return ImmutableList.copyOf(callouts);
		}

		public Builder setButtonActionDescriptor(final ButtonFieldActionDescriptor buttonActionDescriptor)
		{
			this.buttonActionDescriptor = buttonActionDescriptor;
			return this;
		}

		public ButtonFieldActionDescriptor getButtonActionDescriptor()
		{
			return buttonActionDescriptor;
		}

		public boolean isSupportZoomInto()
		{
			// Allow zooming into key column. It shall open precisely this record in a new window
			// (see https://github.com/metasfresh/metasfresh/issues/1687 to understand the use-case)
			// In future we shall think to narrow it down only to included tabs and only for those tables which also have a window where they are the header document.
			if(isKey())
			{
				return true;
			}
			
			final DocumentFieldWidgetType widgetType = getWidgetType();
			if (!widgetType.isSupportZoomInto())
			{
				return false;
			}

			final Class<?> valueClass = getValueClass();
			if (StringLookupValue.class.isAssignableFrom(valueClass))
			{
				return false;
			}

			final String lookupTableName = getLookupTableName().orElse(null);
			if (WindowConstants.TABLENAME_AD_Ref_List.equals(lookupTableName))
			{
				return false;
			}

			return true;
		}
	}
}
