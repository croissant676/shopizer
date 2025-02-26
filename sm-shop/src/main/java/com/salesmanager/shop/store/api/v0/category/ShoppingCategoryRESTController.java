package com.salesmanager.shop.store.api.v0.category;


import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.catalog.category.ReadableCategory;
import com.salesmanager.shop.store.controller.category.facade.CategoryFacade;
import com.salesmanager.shop.utils.LanguageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Rest services for category management
 *
 * @author Carl Samson
 */
@Controller
@RequestMapping("/services")
// Quick question, shouldn't these REST Controllers have the @RestController annotation?
public class ShoppingCategoryRESTController {


    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingCategoryRESTController.class);
    @Inject
    private MerchantStoreService merchantStoreService;
    @Inject
    private LanguageUtils languageUtils;
    @Inject
    private CategoryFacade categoryFacade;

    @RequestMapping(value = "/public/{store}/category/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ReadableCategory getCategory(@PathVariable final String store, @PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {


        try {

            /** default routine **/

            MerchantStore merchantStore = (MerchantStore) request.getAttribute(Constants.MERCHANT_STORE);
            if (merchantStore != null) {
                if (!merchantStore.getCode().equals(store)) {
                    merchantStore = null;
                }
            }

            if (merchantStore == null) {
                merchantStore = merchantStoreService.getByCode(store);
            }

            if (merchantStore == null) {
                LOGGER.error("Merchant store is null for code " + store);
                response.sendError(503, "Merchant store is null for code " + store);
                return null;
            }


            Language language = languageUtils.getRequestLanguage(request, response);

            /**
             Language language = merchantStore.getDefaultLanguage();

             Map<String,Language> langs = languageService.getLanguagesMap();


             if(!StringUtils.isBlank(request.getParameter(Constants.LANG))) {
             String lang = request.getParameter(Constants.LANG);
             if(lang!=null) {
             language = langs.get(language);
             }
             }

             if(language==null) {
             language = merchantStore.getDefaultLanguage();
             }
             **/


            /** end default routine **/


            ReadableCategory category = categoryFacade.getById(merchantStore, id, language);

            if (category == null) {
                response.sendError(503, "Invalid category id");
                return null;
            }


            return category;

        } catch (Exception e) {
            LOGGER.error("Error while saving category", e);
            try {
                response.sendError(503, "Error while saving category " + e.getMessage());
            } catch (Exception ignore) {
            }
            return null;
        }
    }


}
