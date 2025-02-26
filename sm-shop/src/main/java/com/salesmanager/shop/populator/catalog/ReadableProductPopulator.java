package com.salesmanager.shop.populator.catalog;

import com.salesmanager.core.business.exception.ConversionException;
import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.utils.AbstractDataPopulator;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.attribute.ProductAttribute;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionDescription;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionValue;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionValueDescription;
import com.salesmanager.core.model.catalog.product.availability.ProductAvailability;
import com.salesmanager.core.model.catalog.product.description.ProductDescription;
import com.salesmanager.core.model.catalog.product.image.ProductImage;
import com.salesmanager.core.model.catalog.product.manufacturer.ManufacturerDescription;
import com.salesmanager.core.model.catalog.product.price.FinalPrice;
import com.salesmanager.core.model.catalog.product.price.ProductPrice;
import com.salesmanager.core.model.catalog.product.price.ProductPriceDescription;
import com.salesmanager.core.model.catalog.product.type.ProductType;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.catalog.category.ReadableCategory;
import com.salesmanager.shop.model.catalog.manufacturer.ReadableManufacturer;
import com.salesmanager.shop.model.catalog.product.ProductSpecification;
import com.salesmanager.shop.model.catalog.product.ReadableImage;
import com.salesmanager.shop.model.catalog.product.ReadableProduct;
import com.salesmanager.shop.model.catalog.product.ReadableProductFull;
import com.salesmanager.shop.model.catalog.product.ReadableProductPrice;
import com.salesmanager.shop.model.catalog.product.RentalOwner;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductAttribute;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductAttributeValue;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductOption;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductProperty;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductPropertyValue;
import com.salesmanager.shop.model.catalog.product.attribute.api.ReadableProductOptionValueEntity;
import com.salesmanager.shop.model.catalog.product.type.ProductTypeDescription;
import com.salesmanager.shop.model.catalog.product.type.ReadableProductType;
import com.salesmanager.shop.utils.DateUtil;
import com.salesmanager.shop.utils.ImageFilePath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;



public class ReadableProductPopulator extends
		AbstractDataPopulator<Product, ReadableProduct> {

	private PricingService pricingService;

	private ImageFilePath imageUtils;

	public ImageFilePath getImageUtils() {
		return imageUtils;
	}

	// Renamed to follow Java naming conventions
	public void setImageUtils(ImageFilePath imageUtils) {
		this.imageUtils = imageUtils;
	}

	public PricingService getPricingService() {
		return pricingService;
	}

	public void setPricingService(PricingService pricingService) {
		this.pricingService = pricingService;
	}

	@Override
	public ReadableProduct populate(Product source,
			ReadableProduct target, MerchantStore store, Language language)
			throws ConversionException {
		Validate.notNull(pricingService, "Requires to set PricingService");
		Validate.notNull(imageUtils, "Requires to set imageUtils");


		try {

	        List<com.salesmanager.shop.model.catalog.product.ProductDescription> fulldescriptions = new ArrayList<com.salesmanager.shop.model.catalog.product.ProductDescription>();
	        if(language == null) {
	          target = new ReadableProductFull();
	        }

	        if(target==null) {
	        	target = new ReadableProduct();
	        }

	        ProductDescription description = source.getProductDescription();

	        if(source.getDescriptions()!=null && source.getDescriptions().size()>0) {
	          for(ProductDescription desc : source.getDescriptions()) {
                if(language != null && desc.getLanguage()!=null && desc.getLanguage().getId().intValue() == language.getId().intValue()) {
                    description = desc;
                    break;
                } else {
                  fulldescriptions.add(populateDescription(desc));
                }
              }
	        }

		     if(target instanceof ReadableProductFull) {
		          ((ReadableProductFull)target).setDescriptions(fulldescriptions);
		      }

		        if(language == null) {
			          language = store.getDefaultLanguage();
			    }

		   final Language lang = language;

			target.setId(source.getId());
			target.setAvailable(source.isAvailable());
			target.setProductShipeable(source.isProductShipeable());

			ProductSpecification specifications = new ProductSpecification();
			specifications.setHeight(source.getProductHeight());
			specifications.setLength(source.getProductLength());
			specifications.setWeight(source.getProductWeight());
			specifications.setWidth(source.getProductWidth());
			target.setProductSpecifications(specifications);

			target.setPreOrder(source.isPreOrder());
			target.setRefSku(source.getRefSku());
			target.setSortOrder(source.getSortOrder());

			if(source.getType() != null) {
				target.setType(this.type(source.getType(), language));
			}

			if(source.getOwner() != null) {
				RentalOwner owner = new RentalOwner();
				owner.setId(source.getOwner().getId());
				owner.setEmailAddress(source.getOwner().getEmailAddress());
				owner.setFirstName(source.getOwner().getBilling().getFirstName());
				owner.setLastName(source.getOwner().getBilling().getLastName());
				com.salesmanager.shop.model.customer.address.Address address = new com.salesmanager.shop.model.customer.address.Address();
				address.setAddress(source.getOwner().getBilling().getAddress());
				address.setBillingAddress(true);
				address.setCity(source.getOwner().getBilling().getCity());
				address.setCompany(source.getOwner().getBilling().getCompany());
				address.setCountry(source.getOwner().getBilling().getCountry().getIsoCode());
				address.setZone(source.getOwner().getBilling().getZone().getCode());
				address.setLatitude(source.getOwner().getBilling().getLatitude());
				address.setLongitude(source.getOwner().getBilling().getLongitude());
				address.setPhone(source.getOwner().getBilling().getTelephone());
				address.setPostalCode(source.getOwner().getBilling().getPostalCode());
				owner.setAddress(address);
				target.setOwner(owner);
			}


			if(source.getDateAvailable() != null) {
				target.setDateAvailable(DateUtil.formatDate(source.getDateAvailable()));
			}

			if(source.getAuditSection()!=null) {
			  target.setCreationDate(DateUtil.formatDate(source.getAuditSection().getDateCreated()));
			}

			target.setProductVirtual(source.getProductVirtual());
			if(description!=null) {
			    com.salesmanager.shop.model.catalog.product.ProductDescription tragetDescription = populateDescription(description);
				target.setDescription(tragetDescription);

			}

			if(source.getManufacturer()!=null) {
				ManufacturerDescription manufacturer = source.getManufacturer().getDescriptions().iterator().next();
				ReadableManufacturer manufacturerEntity = new ReadableManufacturer();
				com.salesmanager.shop.model.catalog.manufacturer.ManufacturerDescription d = new com.salesmanager.shop.model.catalog.manufacturer.ManufacturerDescription();
				d.setName(manufacturer.getName());
				manufacturerEntity.setDescription(d);
				manufacturerEntity.setId(source.getManufacturer().getId());
				manufacturerEntity.setOrder(source.getManufacturer().getOrder());
				manufacturerEntity.setCode(source.getManufacturer().getCode());
				target.setManufacturer(manufacturerEntity);
			}
			Set<ProductImage> images = source.getImages();
			if(images!=null && images.size()>0) {
				List<ReadableImage> imageList = new ArrayList<ReadableImage>();

				String contextPath = imageUtils.getContextPath();

				for(ProductImage img : images) {
					ReadableImage prdImage = new ReadableImage();
					prdImage.setImageName(img.getProductImage());
					prdImage.setDefaultImage(img.isDefaultImage());
					prdImage.setOrder(img.getSortOrder());

					if (img.getImageType() == 1 && img.getProductImageUrl()!=null) {
						prdImage.setImageUrl(img.getProductImageUrl());
					} else {
						prdImage.setImageUrl(contextPath + imageUtils.buildProductImageUtils(store, source.getSku(), img.getProductImage()));
					}
					prdImage.setId(img.getId());
					prdImage.setImageType(img.getImageType());
					if(img.getProductImageUrl()!=null){
						prdImage.setExternalUrl(img.getProductImageUrl());
					}
					if(img.getImageType()==1 && img.getProductImageUrl()!=null) {//video
						prdImage.setVideoUrl(img.getProductImageUrl());
					}

					if(prdImage.isDefaultImage()) {
						target.setImage(prdImage);
					}

					imageList.add(prdImage);
				}
				imageList = imageList.stream()
				.sorted(Comparator.comparingInt(ReadableImage::getOrder))
				.collect(Collectors.toList());
				
				target
				.setImages(imageList);
			}

			if(!CollectionUtils.isEmpty(source.getCategories())) {

				ReadableCategoryPopulator categoryPopulator = new ReadableCategoryPopulator();
				List<ReadableCategory> categoryList = new ArrayList<ReadableCategory>();

				for(Category category : source.getCategories()) {

					ReadableCategory readableCategory = new ReadableCategory();
					categoryPopulator.populate(category, readableCategory, store, language);
					categoryList.add(readableCategory);

				}

				target.setCategories(categoryList);

			}

			if(!CollectionUtils.isEmpty(source.getAttributes())) {

				Set<ProductAttribute> attributes = source.getAttributes();


				//split read only and options
				//Map<Long,ReadableProductAttribute> readOnlyAttributes = null;
				Map<Long,ReadableProductProperty> properties = null;
				Map<Long,ReadableProductOption> selectableOptions = null;

				if(!CollectionUtils.isEmpty(attributes)) {

					for(ProductAttribute attribute : attributes) {
							ReadableProductOption opt = null;
							ReadableProductAttribute attr = null;
							ReadableProductProperty property = null;
							ReadableProductPropertyValue propertyValue = null;
							ReadableProductOptionValueEntity optValue = new ReadableProductOptionValueEntity();
							ReadableProductAttributeValue attrValue = new ReadableProductAttributeValue();

							ProductOptionValue optionValue = attribute.getProductOptionValue();

							if(attribute.getAttributeDisplayOnly()) {//read only attribute = property


								//if(properties==null) {
								//	properties = new TreeMap<Long,ReadableProductProperty>();
								//}
								//property = properties.get(attribute.getProductOption().getId());
								//if(property==null) {
								property = createProperty(attribute, language);

								ReadableProductOption readableOption = new ReadableProductOption(); //that is the property
								ReadableProductPropertyValue readableOptionValue = new ReadableProductPropertyValue();

								readableOption.setCode(attribute.getProductOption().getCode());
								readableOption.setId(attribute.getProductOption().getId());

								Set<ProductOptionDescription> podescriptions = attribute.getProductOption().getDescriptions();
								if(podescriptions!=null && podescriptions.size()>0) {
									for(ProductOptionDescription optionDescription : podescriptions) {
										if(optionDescription.getLanguage().getCode().equals(language.getCode())) {
											readableOption.setName(optionDescription.getName());
										}
									}
								}

								property.setProperty(readableOption);

								Set<ProductOptionValueDescription> povdescriptions = attribute.getProductOptionValue().getDescriptions();
								readableOptionValue.setId(attribute.getProductOptionValue().getId());
								if(povdescriptions!=null && povdescriptions.size()>0) {
									for(ProductOptionValueDescription optionValueDescription : povdescriptions) {
										if(optionValueDescription.getLanguage().getCode().equals(language.getCode())) {
											readableOptionValue.setName(optionValueDescription.getName());
										}
									}
								}

								property.setPropertyValue(readableOptionValue);


								//} else{
								//	properties.put(attribute.getProductOption().getId(), property);
								//}

								target.getProperties().add(property);


							} else {//selectable option

								if(selectableOptions==null) {
									selectableOptions = new TreeMap<Long,ReadableProductOption>();
								}
								opt = selectableOptions.get(attribute.getProductOption().getId());
								if(opt==null) {
									opt = createOption(attribute, language);
								}
								if(opt!=null) {
									selectableOptions.put(attribute.getProductOption().getId(), opt);
								}

								optValue.setDefaultValue(attribute.getAttributeDefault());
								//optValue.setId(attribute.getProductOptionValue().getId());
								optValue.setId(attribute.getId());
								optValue.setCode(attribute.getProductOptionValue().getCode());
								com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription valueDescription = new com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription();
								valueDescription.setLanguage(language.getCode());
								//optValue.setLang(language.getCode());
								if(attribute.getProductAttributePrice()!=null && attribute.getProductAttributePrice().doubleValue()>0) {
									String formatedPrice = pricingService.getDisplayAmount(attribute.getProductAttributePrice(), store);
									optValue.setPrice(formatedPrice);
								}

								if(!StringUtils.isBlank(attribute.getProductOptionValue().getProductOptionValueImage())) {
									optValue.setImage(imageUtils.buildProductPropertyImageUtils(store, attribute.getProductOptionValue().getProductOptionValueImage()));
								}
								optValue.setSortOrder(0);
								if(attribute.getProductOptionSortOrder()!=null) {
									optValue.setSortOrder(attribute.getProductOptionSortOrder());
								}

								List<ProductOptionValueDescription> podescriptions = optionValue.getDescriptionsSettoList();
								ProductOptionValueDescription podescription = null;
								if(podescriptions!=null && podescriptions.size()>0) {
									podescription = podescriptions.get(0);
									if(podescriptions.size()>1) {
										for(ProductOptionValueDescription optionValueDescription : podescriptions) {
											if(optionValueDescription.getLanguage().getId().intValue()==language.getId().intValue()) {
												podescription = optionValueDescription;
												break;
											}
										}
									}
								}
								valueDescription.setName(podescription.getName());
								valueDescription.setDescription(podescription.getDescription());
								optValue.setDescription(valueDescription);

								if(opt!=null) {
									opt.getOptionValues().add(optValue);
								}
							}

						}

					}

				if(selectableOptions != null) {
					List<ReadableProductOption> options = new ArrayList<ReadableProductOption>(selectableOptions.values());
					target.setOptions(options);
				}


			}



			//remove products from invisible category -> set visible = false

			//target.setVisible(isVisible);

			//availability
			ProductAvailability availability = null;
			for(ProductAvailability a : source.getAvailabilities()) {
				//TODO validate region
				//if(availability.getRegion().equals(Constants.ALL_REGIONS)) {//TODO REL 2.1 accept a region
				availability = a;
				target.setQuantity(availability.getProductQuantity() == null ? 1 : availability.getProductQuantity());
				target.setQuantityOrderMaximum(availability.getProductQuantityOrderMax() == null ? 1 : availability.getProductQuantityOrderMax());
				target.setQuantityOrderMinimum(availability.getProductQuantityOrderMin() == null ? 1 : availability.getProductQuantityOrderMin());
				if (availability.getProductQuantity() > 0 && target.isAvailable()) {
					target.setCanBePurchased(true);
				}
				//}
			}


			target.setSku(source.getSku());

			FinalPrice price = pricingService.calculateProductPrice(source);

			if(price != null) {

				target.setFinalPrice(pricingService.getDisplayAmount(price.getFinalPrice(), store));
				target.setPrice(price.getFinalPrice());
				target.setOriginalPrice(pricingService.getDisplayAmount(price.getOriginalPrice(), store));

				if(price.isDiscounted()) {
					target.setDiscounted(true);
				}

				//price appender
				if(availability != null) {
					Set<ProductPrice> prices = availability.getPrices();
					if(!CollectionUtils.isEmpty(prices)) {
						ReadableProductPrice readableProductPrice = new ReadableProductPrice();
						readableProductPrice.setDiscounted(target.isDiscounted());
						readableProductPrice.setFinalPrice(target.getFinalPrice());
						readableProductPrice.setOriginalPrice(target.getOriginalPrice());

						Optional<ProductPrice> pr = prices.stream().filter(p -> p.getCode().equals(ProductPrice.DEFAULT_PRICE_CODE))
								.findFirst();

						target.setProductPrice(readableProductPrice);

						if(pr.isPresent()) {
							readableProductPrice.setId(pr.get().getId());
							Optional<ProductPriceDescription> d = pr.get().getDescriptions().stream().filter(desc -> desc.getLanguage().getCode().equals(lang.getCode())).findFirst();
							if(d.isPresent()) {
								com.salesmanager.shop.model.catalog.product.ProductPriceDescription priceDescription = new com.salesmanager.shop.model.catalog.product.ProductPriceDescription();
								priceDescription.setLanguage(language.getCode());
								priceDescription.setId(d.get().getId());
								priceDescription.setPriceAppender(d.get().getPriceAppender());
								readableProductPrice.setDescription(priceDescription);
							}
						}

					}
				}

			}
		     if(target instanceof ReadableProductFull) {
		          ((ReadableProductFull)target).setDescriptions(fulldescriptions);
		      }
			return target;
		} catch (Exception e) {
			throw new ConversionException(e);
		}
	}



	private ReadableProductOption createOption(ProductAttribute productAttribute, Language language) {


		ReadableProductOption option = new ReadableProductOption();
		option.setId(productAttribute.getProductOption().getId());//attribute of the option
		option.setType(productAttribute.getProductOption().getProductOptionType());
		option.setCode(productAttribute.getProductOption().getCode());
		List<ProductOptionDescription> descriptions = productAttribute.getProductOption().getDescriptionsSettoList();
		ProductOptionDescription description = null;
		if(descriptions!=null && descriptions.size()>0) {
			description = descriptions.get(0);
			if(descriptions.size()>1) {
				for(ProductOptionDescription optionDescription : descriptions) {
					if(optionDescription.getLanguage().getCode().equals(language.getCode())) {
						description = optionDescription;
						break;
					}
				}
			}
		}

		if(description==null) {
			return null;
		}

		option.setLang(language.getCode());
		option.setName(description.getName());
		option.setCode(productAttribute.getProductOption().getCode());


		return option;

	}

	private ReadableProductType type (ProductType type, Language language) {
		ReadableProductType readableType = new ReadableProductType();
		readableType.setCode(type.getCode());
		readableType.setId(type.getId());

		if(!CollectionUtils.isEmpty(type.getDescriptions())) {
			Optional<ProductTypeDescription> desc = type.getDescriptions().stream().filter(t -> t.getLanguage().getCode().equals(language.getCode()))
					.map(this::typeDescription).findFirst();
			if(desc.isPresent()) {
				readableType.setDescription(desc.get());
			}
		}

		return readableType;
	}

	private ProductTypeDescription typeDescription(com.salesmanager.core.model.catalog.product.type.ProductTypeDescription description) {
		ProductTypeDescription desc = new ProductTypeDescription();
		desc.setId(description.getId());
		desc.setName(description.getName());
		desc.setDescription(description.getDescription());
		desc.setLanguage(description.getLanguage().getCode());
		return desc;
	}

	private ReadableProductAttribute createAttribute(ProductAttribute productAttribute, Language language) {


		ReadableProductAttribute attr = new ReadableProductAttribute();
		attr.setId(productAttribute.getProductOption().getId());//attribute of the option
		attr.setType(productAttribute.getProductOption().getProductOptionType());
		List<ProductOptionDescription> descriptions = productAttribute.getProductOption().getDescriptionsSettoList();
		ProductOptionDescription description = null;
		if(descriptions!=null && descriptions.size()>0) {
			description = descriptions.get(0);
			if(descriptions.size()>1) {
				for(ProductOptionDescription optionDescription : descriptions) {
					if(optionDescription.getLanguage().getId().intValue()==language.getId().intValue()) {
						description = optionDescription;
						break;
					}
				}
			}
		}

		if(description==null) {
			return null;
		}

		attr.setLang(language.getCode());
		attr.setName(description.getName());
		attr.setCode(productAttribute.getProductOption().getCode());


		return attr;

	}

	private ReadableProductProperty createProperty(ProductAttribute productAttribute, Language language) {


		ReadableProductProperty attr = new ReadableProductProperty();
		attr.setId(productAttribute.getProductOption().getId());//attribute of the option
		attr.setType(productAttribute.getProductOption().getProductOptionType());




		List<ProductOptionDescription> descriptions = productAttribute.getProductOption().getDescriptionsSettoList();

		ReadableProductPropertyValue propertyValue = new ReadableProductPropertyValue();


		if(descriptions!=null && descriptions.size()>0) {
			for(ProductOptionDescription optionDescription : descriptions) {
				com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription productOptionValueDescription = new com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription();
				productOptionValueDescription.setId(optionDescription.getId());
				productOptionValueDescription.setLanguage(optionDescription.getLanguage().getCode());
				productOptionValueDescription.setName(optionDescription.getName());
				propertyValue.getValues().add(productOptionValueDescription);

			}
		}

		attr.setCode(productAttribute.getProductOption().getCode());
		return attr;

	}




	@Override
	protected ReadableProduct createTarget() {
		// TODO Auto-generated method stub
		return null;
	}

    com.salesmanager.shop.model.catalog.product.ProductDescription populateDescription(ProductDescription description) {
		if (description == null) {
			return null;
		}

		com.salesmanager.shop.model.catalog.product.ProductDescription targetDescription = new com.salesmanager.shop.model.catalog.product.ProductDescription();
		targetDescription.setFriendlyUrl(description.getSeUrl());
		targetDescription.setName(description.getName());
		targetDescription.setId(description.getId());
		if (!StringUtils.isBlank(description.getMetatagTitle())) {
			targetDescription.setTitle(description.getMetatagTitle());
		} else {
			targetDescription.setTitle(description.getName());
		}
		targetDescription.setMetaDescription(description.getMetatagDescription());
		targetDescription.setDescription(description.getDescription());
		targetDescription.setHighlights(description.getProductHighlight());
		targetDescription.setLanguage(description.getLanguage().getCode());
		targetDescription.setKeyWords(description.getMetatagKeywords());

		if (description.getLanguage() != null) {
			targetDescription.setLanguage(description.getLanguage().getCode());
		}
		return targetDescription;
	}

}
