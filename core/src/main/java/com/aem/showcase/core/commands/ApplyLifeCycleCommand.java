package com.aem.showcase.core.commands;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import com.aem.showcase.core.api.LifeCycleModel;
import com.aem.showcase.core.api.LifeCycleSession;
import com.aem.showcase.core.api.LifeCycleStateService;

import com.day.cq.commons.servlets.HtmlStatusResponseHelper;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.commands.WCMCommand;
import com.day.cq.wcm.api.commands.WCMCommandContext;

@Component(service = WCMCommand.class, immediate = true, name = "com.aem.showcase.core.commands.ApplyLifeCycleCommand")
public class ApplyLifeCycleCommand implements WCMCommand {

	private static final Logger logger = getLogger(ApplyLifeCycleCommand.class);

	@Reference
	LifeCycleStateService lifeCycleStateService;

	@Override
	public String getCommandName() {
		return "applylifecycle";
	}

	@Override
	public HtmlResponse performCommand(WCMCommandContext wcmommandContext,
			SlingHttpServletRequest slingHttpServletRequest, SlingHttpServletResponse slingHttpServletResponse,
			PageManager pageManager) {

		HtmlResponse resp = null;
		try {
			String[] pagePath = slingHttpServletRequest.getParameterValues("path");
			String lifecycleModel = slingHttpServletRequest.getParameter("lifecyclemodel");
			String lifecycleModelVersion = slingHttpServletRequest.getParameter("lifecycleversion");

			String[] msgs = new String[pagePath.length];
			I18n i18n = new I18n(slingHttpServletRequest);
			ResourceResolver resourceResolver = slingHttpServletRequest.getResourceResolver();

			LifeCycleSession lsSession = resourceResolver.adaptTo(LifeCycleSession.class);
			LifeCycleModel lsmodel = lsSession.getModel(lifecycleModel);

			for (int i = 0; i < pagePath.length; i++) {
				Page page = pageManager.getPage(pagePath[i]);
				Resource resource = resourceResolver.getResource(pagePath[i]);

				if (page != null && lsmodel != null) {
					String currentLifeCycleid = lifeCycleStateService.getCurrentLifeCycleId(resourceResolver,
							page.getPath());
					if (StringUtils.isEmpty(currentLifeCycleid)) {
						lifeCycleStateService.apply(lsmodel, resourceResolver, pagePath[i]);
						resp = HtmlStatusResponseHelper.createStatusResponse(true, "201", page.getPath());
					} else {
						resp = HtmlStatusResponseHelper.createStatusResponse(true, "203", page.getPath());
					}
				} else if (DamUtil.isAsset(resource)) {
					String currentLifeCycleid = lifeCycleStateService.getCurrentLifeCycleId(resourceResolver,
							resource.getPath());
					if (StringUtils.isEmpty(currentLifeCycleid)) {
						lifeCycleStateService.apply(lsmodel, resourceResolver, pagePath[i]);
						resp = HtmlStatusResponseHelper.createStatusResponse(true, "201", resource.getPath());
					} else {
						resp = HtmlStatusResponseHelper.createStatusResponse(true, "203", resource.getPath());
					}

				} else {
					resp = HtmlStatusResponseHelper.createStatusResponse(true,
							i18n.get("Either page or model is null."), page.getPath());
					throw new RuntimeException("Either page or model is null.");
				}

			}

		} catch (Exception e) {
			resp = HtmlStatusResponseHelper.createStatusResponse(false, e.getMessage());
			logger.error("Error: ", e);
		}

		return resp;
	}

	protected String getRequestParameter(SlingHttpServletRequest req, String name) {
		String value = req.getParameter(name);
		if ((value != null) && (value.trim().length() == 0)) {
			value = null;
		}
		return value;
	}

}
