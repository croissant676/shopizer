package com.salesmanager.shop.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesmanager.core.business.services.catalog.category.CategoryService;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.business.services.customer.CustomerService;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.business.services.system.MerchantConfigurationService;
import com.salesmanager.core.business.utils.CacheUtils;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.category.CategoryDescription;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.common.UserContext;
import com.salesmanager.core.model.content.Content;
import com.salesmanager.core.model.content.ContentDescription;
import com.salesmanager.core.model.content.ContentType;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.system.MerchantConfig;
import com.salesmanager.core.model.system.MerchantConfiguration;
import com.salesmanager.core.model.system.MerchantConfigurationType;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.catalog.category.ReadableCategory;
import com.salesmanager.shop.model.catalog.category.ReadableCategoryList;
import com.salesmanager.shop.model.customer.AnonymousCustomer;
import com.salesmanager.shop.model.customer.address.Address;
import com.salesmanager.shop.model.shop.Breadcrumb;
import com.salesmanager.shop.model.shop.BreadcrumbItem;
import com.salesmanager.shop.model.shop.BreadcrumbItemType;
import com.salesmanager.shop.model.shop.PageInformation;
import com.salesmanager.shop.populator.catalog.ReadableCategoryPopulator;
import com.salesmanager.shop.store.controller.category.facade.CategoryFacade;
import com.salesmanager.shop.utils.GeoLocationUtils;
import com.salesmanager.shop.utils.LabelUtils;
import com.salesmanager.shop.utils.LanguageUtils;
import com.salesmanager.shop.utils.WebApplicationCacheUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servlet Filter implementation class StoreFilter
 */

@SuppressWarnings({"deprecation", "GrazieInspection", "DuplicatedCode"})
public class StoreFilter extends HandlerInterceptorAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(StoreFilter.class);

	private final static String STORE_REQUEST_PARAMETER = "store";

	@Inject
	private ContentService contentService;

	@Inject
	private CategoryService categoryService;

	@Inject
	private ProductService productService;

	@Inject
	private MerchantStoreService merchantService;

	@Inject
	private CustomerService customerService;

	@Inject
	private MerchantConfigurationService merchantConfigurationService;

	@Inject
	private LanguageService languageService;

	@Inject
	private LabelUtils messages;

	@Inject
	private LanguageUtils languageUtils;

	@Inject
	private CacheUtils cache;

	@Inject
	private WebApplicationCacheUtils webApplicationCache;

	@Inject
	private CategoryFacade categoryFacade;

	@Inject
	private CoreConfiguration coreConfiguration;

	private final static String SERVICES_URL_PATTERN = "/services";
	private final static String REFERENCE_URL_PATTERN = "/reference";

	/**
	 * Default constructor.
	 */
	public StoreFilter() {

	}

	@SuppressWarnings("NullableProblems")
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		request.setCharacterEncoding("UTF-8");
		if (request.getRequestURL().toString().toLowerCase().contains(SERVICES_URL_PATTERN)
				|| request.getRequestURL().toString().toLowerCase().contains(REFERENCE_URL_PATTERN)) {
			return true;
		}
		try {
			MerchantStore store = (MerchantStore) request.getSession().getAttribute(Constants.MERCHANT_STORE);
			String storeCode = request.getParameter(STORE_REQUEST_PARAMETER);
			// remove link set from controllers for declaring active - inactive
			// links
			request.removeAttribute(Constants.LINK_CODE);
			if (!StringUtils.isBlank(storeCode)) {
				if (store != null) {
					if (!store.getCode().equals(storeCode)) {
						store = setMerchantStoreInSession(request, storeCode);
					}
				} else { // when url sm-shop/shop is being loaded for first time
							// store is null
					store = setMerchantStoreInSession(request, storeCode);
				}
			}
			if (store == null) {
				store = setMerchantStoreInSession(request, MerchantStore.DEFAULT_STORE);
			}
			if(StringUtils.isBlank(store.getStoreTemplate())) {
				store.setStoreTemplate(Constants.DEFAULT_TEMPLATE);
			}
			request.setAttribute(Constants.MERCHANT_STORE, store);
			String ipAddress = GeoLocationUtils.getClientIpAddress(request);
			UserContext userContext = UserContext.create();
			userContext.setIpAddress(ipAddress);
			Customer customer = (Customer) request.getSession().getAttribute(Constants.CUSTOMER);
			if (customer != null) {
				if (customer.getMerchantStore().getId().intValue() != store.getId().intValue()) {
					request.getSession().removeAttribute(Constants.CUSTOMER);
				}
				if (!customer.isAnonymous()) {
					if (!request.isUserInRole("AUTH_CUSTOMER")) {
						request.removeAttribute(Constants.CUSTOMER);
					}
				}
				request.setAttribute(Constants.CUSTOMER, customer);
			}
			if (customer == null) {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				if (auth != null && request.isUserInRole("AUTH_CUSTOMER")) {
					customer = customerService.getByNick(auth.getName());
					if (customer != null) {
						request.setAttribute(Constants.CUSTOMER, customer);
					}
				}
			}
			AnonymousCustomer anonymousCustomer = (AnonymousCustomer) request.getSession()
					.getAttribute(Constants.ANONYMOUS_CUSTOMER);
			if (anonymousCustomer == null) {
				Address address = null;
				try {
					if(!StringUtils.isBlank(ipAddress)) {
						com.salesmanager.core.model.common.Address geoAddress = customerService.getCustomerAddress(store, ipAddress);
						if (geoAddress != null) {
							address = new Address();
							address.setCountry(geoAddress.getCountry());
							address.setCity(geoAddress.getCity());
							address.setZone(geoAddress.getZone());
							// address.setPostalCode(geoAddress.getPostalCode());
						}
					}
				} catch (Exception ce) {
					LOGGER.error("Cannot get geo ip component ", ce);
				}

				if (address == null) {
					address = new Address();
					address.setCountry(store.getCountry().getIsoCode());
					if (store.getZone() != null) {
						address.setZone(store.getZone().getCode());
					} else {
						address.setStateProvince(store.getStorestateprovince());
					}
					// address.setPostalCode(store.getStorepostalcode());
				}

				anonymousCustomer = new AnonymousCustomer();
				anonymousCustomer.setBilling(address);
				request.getSession().setAttribute(Constants.ANONYMOUS_CUSTOMER, anonymousCustomer);
			} else {
				request.setAttribute(Constants.ANONYMOUS_CUSTOMER, anonymousCustomer);
			}

			Language language = languageUtils.getRequestLanguage(request, response);
			request.setAttribute(Constants.LANGUAGE, language);

			Locale locale = languageService.toLocale(language, store);
			request.setAttribute(Constants.LOCALE, locale);
			// Locale locale = LocaleContextHolder.getLocale();
			LocaleContextHolder.setLocale(locale);
			setBreadcrumb(request, locale);
			// get from the cache first
			this.getContentObjects(store, language, request);

			this.getContentPageNames(store, language, request);

			// this.getTopCategories(store, language, request);
			this.setTopCategories(store, language, request);

			PageInformation pageInformation = new PageInformation();
			pageInformation.setPageTitle(store.getStorename());
			pageInformation.setPageDescription(store.getStorename());
			pageInformation.setPageKeywords(store.getStorename());

			@SuppressWarnings("unchecked")
			Map<String, ContentDescription> contents = (Map<String, ContentDescription>) request
					.getAttribute(Constants.REQUEST_CONTENT_OBJECTS);

			if (contents != null) {
				// for(String key : contents.keySet()) {
				// List<ContentDescription> contentsList = contents.get(key);
				// for(Content content : contentsList) {
				// if(key.equals(Constants.CONTENT_LANDING_PAGE)) {
				// List<ContentDescription> descriptions =
				// content.getDescriptions();
				ContentDescription contentDescription = contents.get(Constants.CONTENT_LANDING_PAGE);
				if (contentDescription != null) {
					// for(ContentDescription contentDescription : descriptions)
					// {
					// if(contentDescription.getLanguage().getCode().equals(language.getCode()))
					// {
					pageInformation.setPageTitle(contentDescription.getName());
					pageInformation.setPageDescription(contentDescription.getMetatagDescription());
					pageInformation.setPageKeywords(contentDescription.getMetatagKeywords());
					// }
				}
				// }
				// }
				// }
			}

			request.setAttribute(Constants.REQUEST_PAGE_INFORMATION, pageInformation);

			this.getMerchantConfigurations(store, request);

			String shoppingCarCode = (String) request.getSession().getAttribute(Constants.SHOPPING_CART);
			if (shoppingCarCode != null) {
				request.setAttribute(Constants.REQUEST_SHOPPING_CART, shoppingCarCode);
			}

		} catch (Exception e) {
			LOGGER.error("Error in StoreFilter", e);
		}

		return true;

	}

	@SuppressWarnings("unchecked")
	private void getMerchantConfigurations(MerchantStore store, HttpServletRequest request) throws Exception {
		// Convert configKey StringBuilder into String
		String configKey = store.getId() + "_" + Constants.CONFIG_CACHE_KEY;

		StringBuilder configKeyMissed = new StringBuilder();
		configKeyMissed.append(configKey).append(Constants.MISSED_CACHE_KEY);
		Map<String, Object> configs;

		if (store.isUseCache()) {
			// get from the cache
			configs = (Map<String, Object>) cache.getFromCache(configKey);
			if (configs == null) {
				// get from missed cache
				// Boolean missedContent =
				// (Boolean)cache.getFromCache(configKeyMissed.toString());

				// if( missedContent==null) {
				configs = this.getConfigurations(store);
				// put in cache

				if (configs != null) {
					cache.putInCache(configs, configKey);
				} else {
					// put in missed cache
					// May need to update this part..
				}
				// }
			}

		} else {
			configs = this.getConfigurations(store);
		}

		if (configs != null && configs.size() > 0) {
			request.setAttribute(Constants.REQUEST_CONFIGS, configs);
		}

	}

	@SuppressWarnings("unchecked")
	private void getContentPageNames(MerchantStore store, Language language, HttpServletRequest request)
			throws Exception {

		// build the key

		StringBuilder contentKey = new StringBuilder();
		contentKey.append(store.getId()).append("_").append(Constants.CONTENT_PAGE_CACHE_KEY).append("-")
				.append(language.getCode());

		// This SB is unused...
		StringBuilder contentKeyMissed = new StringBuilder();
		contentKeyMissed.append(contentKey).append(Constants.MISSED_CACHE_KEY);

		Map<String, List<ContentDescription>> contents = null;

		if (store.isUseCache()) {

			// get from the cache
			contents = (Map<String, List<ContentDescription>>) cache.getFromCache(contentKey.toString());

			if (contents == null) {
				// get from missed cache
				// Boolean missedContent =
				// (Boolean)cache.getFromCache(contentKeyMissed.toString());

				// if(missedContent==null) {

				contents = this.getContentPagesNames(store, language);

				if (contents != null) {
					// put in cache
					cache.putInCache(contents, contentKey.toString());

				} else {
					// put in missed cache
				}
				// }
			}
		} else {
			contents = this.getContentPagesNames(store, language);
		}

		if (contents != null && contents.size() > 0) {
			List<ContentDescription> descriptions = contents.get(contentKey.toString());

			if (descriptions != null) {
				request.setAttribute(Constants.REQUEST_CONTENT_PAGE_OBJECTS, descriptions);
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	private void getContentObjects(MerchantStore store, Language language, HttpServletRequest request)
			throws Exception {

		// build the key

		StringBuilder contentKey = new StringBuilder();
		contentKey.append(store.getId()).append("_").append(Constants.CONTENT_CACHE_KEY).append("-")
				.append(language.getCode());

		StringBuilder contentKeyMissed = new StringBuilder();
		contentKeyMissed.append(contentKey).append(Constants.MISSED_CACHE_KEY);

		Map<String, List<Content>> contents = null;

		if (store.isUseCache()) {

			// get from the cache
			contents = (Map<String, List<Content>>) cache.getFromCache(contentKey.toString());

			if (contents == null) {

				// get from missed cache
				// Boolean missedContent =
				// (Boolean)cache.getFromCache(contentKeyMissed.toString());

				// if(missedContent==null) {

				contents = this.getContent(store, language);
				if (contents != null && contents.size() > 0) {
					// put in cache
					cache.putInCache(contents, contentKey.toString());
				} else {
					// put in missed cache
				}
				// }

			}
		} else {

			contents = this.getContent(store, language);

		}

		if (contents != null && contents.size() > 0) {

			List<Content> contentByStore = contents.get(contentKey.toString());
			if (!CollectionUtils.isEmpty(contentByStore)) {
				Map<String, ContentDescription> contentMap = new HashMap<>();
				for (Content content : contentByStore) {
					if (content.isVisible()) {
						contentMap.put(content.getCode(), content.getDescription());
					}
				}
				request.setAttribute(Constants.REQUEST_CONTENT_OBJECTS, contentMap);
			}

		}

	}

	@SuppressWarnings("unchecked")
	private void setTopCategories(MerchantStore store, Language language, HttpServletRequest request) throws Exception {

		StringBuilder categoriesKey = new StringBuilder();
		categoriesKey.append(store.getId()).append("_").append(Constants.CATEGORIES_CACHE_KEY).append("-")
				.append(language.getCode());

		// This one is also unused.. (never queried)
		StringBuilder categoriesKeyMissed = new StringBuilder();
		categoriesKeyMissed.append(categoriesKey).append(Constants.MISSED_CACHE_KEY);

		// language code - List of category
		Map<String, List<ReadableCategory>> objects = null;
		List<ReadableCategory> loadedCategories = null;

		if (store.isUseCache()) {
			objects = (Map<String, List<ReadableCategory>>) webApplicationCache.getFromCache(categoriesKey.toString());

			if (objects == null) {
				// load categories
				ReadableCategoryList categoryList = categoryFacade.getCategoryHierarchy(store, null, 0, language, null,
						0, 200);// null
				loadedCategories = categoryList.getCategories();

				// filter out invisible category
				loadedCategories.stream().filter(cat -> cat.isVisible() == true).collect(Collectors.toList());

				objects = new ConcurrentHashMap<>();
				objects.put(language.getCode(), loadedCategories);
				webApplicationCache.putInCache(categoriesKey.toString(), objects);

			} else {
				loadedCategories = objects.get(language.getCode());
			}

		} else {

			ReadableCategoryList categoryList = categoryFacade.getCategoryHierarchy(store, null, 0, language, null, 0,
					200);// null // filter
			loadedCategories = categoryList.getCategories();
		}

		if (loadedCategories != null) {
			request.setAttribute(Constants.REQUEST_TOP_CATEGORIES, loadedCategories);
		}

	}

	private Map<String, List<ContentDescription>> getContentPagesNames(MerchantStore store, Language language)
			throws Exception {

		Map<String, List<ContentDescription>> contents = new ConcurrentHashMap<>();

		// Get boxes and sections from the database
		List<ContentType> contentTypes = new ArrayList<>();
		contentTypes.add(ContentType.PAGE);

		List<ContentDescription> contentPages = contentService.listNameByType(contentTypes, store, language);

		if (contentPages != null && contentPages.size() > 0) {

			// create a Map<String,List<Content>
			for (ContentDescription content : contentPages) {

				String key = new StringBuilder().append(store.getId()).append("_")
						.append(Constants.CONTENT_PAGE_CACHE_KEY).append("-").append(language.getCode()).toString();
				List<ContentDescription> contentList;
				if (contents == null || contents.size() == 0) {
					contents = new HashMap<>();
				}
				if (!contents.containsKey(key)) {
					contentList = new ArrayList<>();

					contents.put(key, contentList);
				} else {// get from key
					contentList = contents.get(key);
					if (contentList == null) {
						LOGGER.error("Cannot find content key in cache " + key);
						continue;
					}
				}
				contentList.add(content);
			}
		}
		return contents;
	}

	private Map<String, List<Content>> getContent(MerchantStore store, Language language) throws Exception {

		Map<String, List<Content>> contents = new ConcurrentHashMap<>();

		// Get boxes and sections from the database
		List<ContentType> contentTypes = new ArrayList<>();
		contentTypes.add(ContentType.BOX);
		contentTypes.add(ContentType.SECTION);

		List<Content> contentPages = contentService.listByType(contentTypes, store, language);

		if (contentPages != null && contentPages.size() > 0) {

			// create a Map<String,List<Content>
			for (Content content : contentPages) {
				if (content.isVisible()) {
					List<ContentDescription> descriptions = content.getDescriptions();
					for (ContentDescription contentDescription : descriptions) {
						Language lang = contentDescription.getLanguage();
						String key = new StringBuilder().append(store.getId()).append("_")
								.append(Constants.CONTENT_CACHE_KEY).append("-").append(lang.getCode()).toString();
						List<Content> contentList = null;
						if (contents.size() == 0) {
							contents = new HashMap<>();
						}
						if (!contents.containsKey(key)) {
							contentList = new ArrayList<>();

							contents.put(key, contentList);
						} else {// get from key
							contentList = contents.get(key);
							if (contentList == null) {
								LOGGER.error("Cannot find content key in cache " + key);
								continue;
							}
						}
						contentList.add(content);
					}
				}
			}
		}
		return contents;
	}

	/**
	 * 
	 * @param store
	 * @param language
	 * @return
	 * @throws Exception
	 */
	// private Map<String, List<Category>> getCategories(MerchantStore store,
	// Language language)
	// throws Exception {
	private Map<String, List<ReadableCategory>> getCategories(MerchantStore store, Language language) throws Exception {
		Map<String, List<ReadableCategory>> objects = new ConcurrentHashMap<>();
		List<Category> categories = categoryService.getListByDepth(store, 0, language);
		ReadableCategoryPopulator readableCategoryPopulator = new ReadableCategoryPopulator();
		Map<String, ReadableCategory> subs = new ConcurrentHashMap<>();
		if (categories != null && categories.size() > 0) {

			// create a Map<String,List<Content>
			for (Category category : categories) {
				if (category.isVisible()) {
					// if(category.getDepth().intValue()==0) {
					// ReadableCategory readableCategory = new
					// ReadableCategory();
					// readableCategoryPopulator.populate(category,
					// readableCategory, store, language);

					Set<CategoryDescription> descriptions = category.getDescriptions();
					for (CategoryDescription description : descriptions) {

						Language lang = description.getLanguage();

						ReadableCategory readableCategory = new ReadableCategory();
						readableCategoryPopulator.populate(category, readableCategory, store, language);

						String key = new StringBuilder().append(store.getId()).append("_")
								.append(Constants.CATEGORIES_CACHE_KEY).append("-").append(lang.getCode()).toString();

						if (category.getDepth().intValue() == 0) {

							// List<Category> cacheCategories = null;
							List<ReadableCategory> cacheCategories = null;
							if (objects == null || objects.size() == 0) {
								objects = new HashMap<>();
							}
							if (!objects.containsKey(key)) {
								// cacheCategories = new ArrayList<Category>();
								cacheCategories = new ArrayList<>();

								objects.put(key, cacheCategories);
							} else {
								cacheCategories = objects.get(key);
								if (cacheCategories == null) {
									LOGGER.error("Cannot find categories key in cache " + key);
									continue;
								}
							}
							// cacheCategories.add(category);
							cacheCategories.add(readableCategory);

						} else {
							subs.put(lang.getCode(), readableCategory);
						}
					}
				}
			}

		}
		return objects;
	}

	@SuppressWarnings("unused")
	private Map<String, Object> getConfigurations(MerchantStore store) {

		Map<String, Object> configs = new HashMap<>();
		try {

			List<MerchantConfiguration> merchantConfiguration = merchantConfigurationService
					.listByType(MerchantConfigurationType.CONFIG, store);

			// get social
			List<MerchantConfiguration> socialConfigs = merchantConfigurationService
					.listByType(MerchantConfigurationType.SOCIAL, store);

			if (!CollectionUtils.isEmpty(socialConfigs)) {
				if (CollectionUtils.isEmpty(merchantConfiguration)) {
					merchantConfiguration = new ArrayList<>();
				}
				merchantConfiguration.addAll(socialConfigs);
			}

			if (CollectionUtils.isEmpty(merchantConfiguration)) {
				return configs;
			}

			for (MerchantConfiguration configuration : merchantConfiguration) {
				configs.put(configuration.getKey(), configuration.getValue());
			}

			configs.put(Constants.SHOP_SCHEME, coreConfiguration.getProperty(Constants.SHOP_SCHEME));
			configs.put(Constants.FACEBOOK_APP_ID, coreConfiguration.getProperty(Constants.FACEBOOK_APP_ID));

			// get MerchantConfig
			MerchantConfig merchantConfig = merchantConfigurationService.getMerchantConfig(store);
			if (merchantConfig != null) {
				if (configs == null) {
					configs = new HashMap<>();
				}

				ObjectMapper m = new ObjectMapper();
				@SuppressWarnings("unchecked")
				Map<String, Object> props = m.convertValue(merchantConfig, Map.class);

				for (String key : props.keySet()) {
					configs.put(key, props.get(key));
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception while getting configurations", e);
		}

		return configs;

	}

	private void setBreadcrumb(HttpServletRequest request, Locale locale) {

		try {

			// breadcrumb
			Breadcrumb breadCrumb = (Breadcrumb) request.getSession().getAttribute(Constants.BREADCRUMB);
			Language language = (Language) request.getAttribute(Constants.LANGUAGE);
			if (breadCrumb == null) {
				breadCrumb = new Breadcrumb();
				breadCrumb.setLanguage(language);
				BreadcrumbItem item = this.getDefaultBreadcrumbItem(language, locale);
				breadCrumb.getBreadCrumbs().add(item);
			} else {

				// check language
				if (language.getCode().equals(breadCrumb.getLanguage().getCode())) {

					// rebuild using the appropriate language
					List<BreadcrumbItem> items = new ArrayList<>();
					for (BreadcrumbItem item : breadCrumb.getBreadCrumbs()) {
						// String#equals to a BreadcrumbItemType?
						if (item.getItemType().name().equals(BreadcrumbItemType.HOME)) {
							BreadcrumbItem homeItem = this.getDefaultBreadcrumbItem(language, locale);
							homeItem.setItemType(BreadcrumbItemType.HOME);
							homeItem.setLabel(messages.getMessage(Constants.HOME_MENU_KEY, locale));
							homeItem.setUrl(Constants.HOME_URL);
							items.add(homeItem);
						} else if (item.getItemType().name().equals(BreadcrumbItemType.PRODUCT)) {
							Product product = productService.getProductForLocale(item.getId(), language, locale);
							if (product != null) {
								BreadcrumbItem productItem = new BreadcrumbItem();
								productItem.setId(product.getId());
								productItem.setItemType(BreadcrumbItemType.PRODUCT);
								productItem.setLabel(product.getProductDescription().getName());
								productItem.setUrl(product.getProductDescription().getSeUrl());
								items.add(productItem);
							}
						} else if (item.getItemType().name().equals(BreadcrumbItemType.CATEGORY)) {
							Category category = categoryService.getOneByLanguage(item.getId(), language);
							if (category != null) {
								BreadcrumbItem categoryItem = new BreadcrumbItem();
								categoryItem.setId(category.getId());
								categoryItem.setItemType(BreadcrumbItemType.CATEGORY);
								categoryItem.setLabel(category.getDescription().getName());
								categoryItem.setUrl(category.getDescription().getSeUrl());
								items.add(categoryItem);
							}
						} else if (item.getItemType().name().equals(BreadcrumbItemType.PAGE)) {
							Content content = contentService.getByLanguage(item.getId(), language);
							if (content != null) {
								BreadcrumbItem contentItem = new BreadcrumbItem();
								contentItem.setId(content.getId());
								contentItem.setItemType(BreadcrumbItemType.PAGE);
								contentItem.setLabel(content.getDescription().getName());
								contentItem.setUrl(content.getDescription().getSeUrl());
								items.add(contentItem);
							}
						}
					}

					breadCrumb = new Breadcrumb();
					breadCrumb.setLanguage(language);
					breadCrumb.setBreadCrumbs(items);

				}

			}

			request.getSession().setAttribute(Constants.BREADCRUMB, breadCrumb);
			request.setAttribute(Constants.BREADCRUMB, breadCrumb);

		} catch (Exception e) {
			LOGGER.error("Error while building breadcrumbs", e);
		}

	}

	private BreadcrumbItem getDefaultBreadcrumbItem(Language language, Locale locale) {
		// set home page item
		BreadcrumbItem item = new BreadcrumbItem();
		item.setItemType(BreadcrumbItemType.HOME);
		item.setLabel(messages.getMessage(Constants.HOME_MENU_KEY, locale));
		item.setUrl(Constants.HOME_URL);
		return item;
	}

	/**
	 * Sets a MerchantStore with the given storeCode in the session.
	 * 
	 * @param request
	 * @param storeCode
	 *            The storeCode of the Merchant.
	 * @return the MerchantStore inserted in the session.
	 * @throws Exception
	 */
	private MerchantStore setMerchantStoreInSession(HttpServletRequest request, String storeCode) throws Exception {
		if (storeCode == null || request == null)
			return null;
		MerchantStore store = merchantService.getByCode(storeCode);
		if (store != null) {
			request.getSession().setAttribute(Constants.MERCHANT_STORE, store);
		}
		return store;
	}
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
		
		if (request.getRequestURL().toString().toLowerCase().contains(SERVICES_URL_PATTERN)
				|| request.getRequestURL().toString().toLowerCase().contains(REFERENCE_URL_PATTERN)) {
			return;
		}
		
		UserContext userContext = UserContext.getCurrentInstance();
		if(userContext!=null) {
			userContext.close();
		}
	}

}
