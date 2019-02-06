package com.aem.showcase.core.commands;

import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import com.aem.showcase.core.api.LifeCycleStateService;
import com.day.cq.commons.servlets.HtmlStatusResponseHelper;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.commands.WCMCommand;
import com.day.cq.wcm.api.commands.WCMCommandContext;

@Component(service = WCMCommand.class, immediate = true, name = "com.aem.showcase.core.commands.DeleteLifeCycleModel")
public class DeleteLifeCycleModel implements WCMCommand {

	private static final Logger logger = getLogger(DeleteLifeCycleModel.class);

	@Reference
	LifeCycleStateService lifeCycleStateService;

	@Override
	public String getCommandName() {
		return "deleteLifecycle";
	}

	@Override
	public HtmlResponse performCommand(WCMCommandContext wcmommandContext,
			SlingHttpServletRequest slingHttpServletRequest, SlingHttpServletResponse slingHttpServletResponse,
			PageManager pageManager) {

		HtmlResponse resp = null;
		try {
			String[] pagePath = slingHttpServletRequest.getParameterValues("path");
			I18n i18n = new I18n(slingHttpServletRequest);

			ResourceResolver resourceResolver = slingHttpServletRequest.getResourceResolver();
			for (int i = 0; i < pagePath.length; i++) {
				Resource resource = resourceResolver.getResource(pagePath[i]);
				Node node = resource.adaptTo(Node.class);
				node.remove();
				resourceResolver.commit();
				resp = HtmlStatusResponseHelper.createStatusResponse(true,
						i18n.get("Life cycle model is deleted successfully"), pagePath[i]);

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
