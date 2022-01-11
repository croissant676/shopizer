package com.salesmanager.shop.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("deprecation")
public class CorsFilter extends HandlerInterceptorAdapter {

		public CorsFilter() {
		}

		/**
		 * Allows public web services to work from remote hosts
		 */
	   @SuppressWarnings("NullableProblems")
	   public boolean preHandle(
	            HttpServletRequest request,
	            HttpServletResponse response,
	            Object handler) {
		   String origin = "*";
		   if (!StringUtils.isBlank(request.getHeader("origin"))) {
			   origin = request.getHeader("origin");
		   }
		   response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS, DELETE, PATCH");
		   response.setHeader("Access-Control-Allow-Headers", "X-Auth-Token, Content-Type, Authorization, Cache-Control, X-Requested-With");
		   response.setHeader("Access-Control-Allow-Origin", origin);
		   return true;
	   }
}
