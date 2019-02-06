package com.aem.showcase.core.commands;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;

import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HtmlResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import com.aem.showcase.core.Constants;
import com.aem.showcase.core.api.LifeCycleStateService;
import com.day.cq.commons.servlets.HtmlStatusResponseHelper;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Revision;
import com.day.cq.wcm.api.commands.WCMCommand;
import com.day.cq.wcm.api.commands.WCMCommandContext;

@Component(service = WCMCommand.class, immediate = true, name = "com.aem.showcase.core.commands.CustomCreateVersionCommand")
public class CustomCreateVersionCommand implements WCMCommand {

	private static final Logger logger = getLogger(CustomCreateVersionCommand.class);

	@Reference
	LifeCycleStateService lifeCycleStateService;

	@Override
	public String getCommandName() {
		return "customCreateVersion";
	}

	@Override
	public HtmlResponse performCommand(WCMCommandContext wcmommandContext,
			SlingHttpServletRequest slingHttpServletRequest, SlingHttpServletResponse slingHttpServletResponse,
			PageManager pageManager) {

		HtmlResponse resp = null;
		try {
			String[] pagePath = slingHttpServletRequest.getParameterValues("path");
			String[] msgs = new String[pagePath.length];
			boolean isAllowedToCreateVersion = Boolean.FALSE;
			I18n i18n = new I18n(slingHttpServletRequest);

			ResourceResolver resolver = slingHttpServletRequest.getResourceResolver();
			Session session = (Session) resolver.adaptTo(Session.class);
			String userID = session.getUserID();

			for (int i = 0; i < pagePath.length; i++) {

				Page page = pageManager.getPage(pagePath[i]);
				if (page != null) {
					ValueMap map = page.getProperties();
					String modelId = map.get(Constants.PN_LIFECYCLE_ID, String.class);
					if (StringUtils.isNotEmpty(modelId)) {
						String[] validStates = getValidLifeCycleState(modelId, resolver);
						if (validStates != null) {
							for (int j = 0; j < validStates.length; j++) {
								logger.debug(" validStates : " + validStates[j]);
							}

							String currentLifeCycleState = lifeCycleStateService.getCurrentLifeCycleState(resolver,
									pagePath[i]);
							logger.debug(" currentLifeCycleState : " + currentLifeCycleState);
							isAllowedToCreateVersion = Arrays.stream(validStates)
									.anyMatch(currentLifeCycleState::equals);
						}
					} else {
						isAllowedToCreateVersion = Boolean.TRUE;
					}

				}
				if (isAllowedToCreateVersion) {
					if ((page.isLocked()) && (!userID.equals(page.getLockOwner()))) {
						return HtmlStatusResponseHelper.createStatusResponse(423, i18n.get(
								"The page ({0}) is locked by another user", null, new Object[] { page.getName() }));
					}
					Revision revision = pageManager.createRevision(page,
							getRequestParameter(slingHttpServletRequest, "label"),
							getRequestParameter(slingHttpServletRequest, "comment"));
					msgs[i] = i18n.get("Revision {0} created for: {1}", null,
							new Object[] { revision.getLabel(), page.getTitle() });

					resp = HtmlStatusResponseHelper.createStatusResponse(true, i18n.get("Version created"),
							page.getPath());
				} else {
					resp = HtmlStatusResponseHelper.createStatusResponse(203,
							i18n.get("Version creation not allowed in this state."), page.getPath());
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

	private String[] getValidLifeCycleState(String modelId, ResourceResolver resolver) {
		return lifeCycleStateService.getFirstandLastElementOfLifeCycle(modelId, resolver);
	}

	

}
