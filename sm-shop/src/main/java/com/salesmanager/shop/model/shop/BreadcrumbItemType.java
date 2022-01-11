package com.salesmanager.shop.model.shop;

public enum BreadcrumbItemType {
	CATEGORY,
	PRODUCT,
	HOME,
	PAGE;

	@Override
	public String toString() {
		return "BreadCrumbItemType[" + name() + "]";
	}
}
