package com.aem.showcase.core.impl;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONWriter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aem.showcase.core.Constants;
import com.aem.showcase.core.api.LifeCycleStateService;
import com.day.cq.i18n.I18n;

@Component(name = "com.aem.showcase.core.impl.QuickUpdateActionLifeCycleServlet", service = Servlet.class, property = {
		"service.description=Generate Life Cycle States", "sling.servlet.methods=GET",
		"sling.servlet.paths=/bin/cq/updatestate" })

public class QuickUpdateActionLifeCycleServlet extends SlingSafeMethodsServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(QuickUpdateActionLifeCycleServlet.class);

	@Reference
	LifeCycleStateService lifeCycleStateService;

	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("utf-8");
		try {
			String[] pagePath = request.getParameterValues("path");
			String action = request.getParameter("action");
			int responseCode = 0;
			String responseMessage = StringUtils.EMPTY;
			I18n i18n = new I18n(request);

			if (StringUtils.isBlank(action) && pagePath == null) {
				return;
			}

			Actions theAction = Actions.fromString(action);

			for (int i = 0; i < pagePath.length; i++) {
				String currentLifeCycleid = lifeCycleStateService.getCurrentLifeCycleId(request.getResourceResolver(),
						pagePath[i]);
				logger.debug("CurrentLifeCycle id {}", currentLifeCycleid);

				if (StringUtils.isNotEmpty(currentLifeCycleid)) {

					switch (theAction) {
					case PROMOTE:
						String currentLifeCycleStatePromoteFrom = lifeCycleStateService
								.getCurrentLifeCycleState(request.getResourceResolver(), pagePath[i]);
						if (StringUtils.isNotEmpty(currentLifeCycleStatePromoteFrom)) {
							boolean canPromote = lifeCycleStateService.canPromote(currentLifeCycleid, StringUtils.EMPTY,
									currentLifeCycleStatePromoteFrom, request.getResourceResolver(), pagePath[i]);
							if (canPromote) {
								lifeCycleStateService.doPromote(currentLifeCycleid, StringUtils.EMPTY,
										currentLifeCycleStatePromoteFrom, request.getResourceResolver(), pagePath[i]);
								responseCode = 201;
								responseMessage = i18n.get(Constants.RESPONCE_CODE_PROMOTE_201, String.valueOf(i++));
							} else {
								responseCode = 203;
								responseMessage = i18n.get(Constants.RESPONCE_CODE_PROMOTE_203);
							}
						} else {
							responseCode = 204;
							responseMessage = i18n.get(Constants.RESPONCE_CODE_PROMOTE_204);
						}
						break;
					case DEMOTE:
						String currentLifeCycleStateDemoteFrom = lifeCycleStateService
								.getCurrentLifeCycleState(request.getResourceResolver(), pagePath[i]);
						if (StringUtils.isNotEmpty(currentLifeCycleStateDemoteFrom)) {
							boolean canDemote = lifeCycleStateService.canDemote(currentLifeCycleid, StringUtils.EMPTY,
									currentLifeCycleStateDemoteFrom, request.getResourceResolver(), pagePath[i]);
							if (canDemote) {
								lifeCycleStateService.doDemote(currentLifeCycleid, StringUtils.EMPTY,
										currentLifeCycleStateDemoteFrom, request.getResourceResolver(), pagePath[i]);
								responseCode = 201;
								responseMessage = i18n.get(Constants.RESPONCE_CODE_DEMOTE_201, String.valueOf(i++));
							} else {
								responseCode = 203;
								responseMessage = i18n.get(Constants.RESPONCE_CODE_DEMOTE_203, String.valueOf(i++));
							}

						} else {
							responseCode = 204;
							responseMessage = i18n.get(Constants.RESPONCE_CODE_DEMOTE_204);
						}
						break;
					case SUSPEND:
						String currentLifeCycleStateSuspendFrom = lifeCycleStateService
								.getCurrentLifeCycleState(request.getResourceResolver(), pagePath[i]);
						if (StringUtils.isNotEmpty(currentLifeCycleStateSuspendFrom)) {
							boolean canSuspend = lifeCycleStateService.canSuspend(currentLifeCycleid, StringUtils.EMPTY,
									currentLifeCycleStateSuspendFrom, request.getResourceResolver(), pagePath[i]);
							if (canSuspend) {
								lifeCycleStateService.doSuspend(currentLifeCycleid, StringUtils.EMPTY,
										currentLifeCycleStateSuspendFrom, request.getResourceResolver(), pagePath[i]);
								responseCode = 201;
								responseMessage = i18n.get(Constants.RESPONCE_CODE_SUSPEND_201, String.valueOf(i++));
							} else {
								responseCode = 203;
								responseMessage = i18n.get(Constants.RESPONCE_CODE_SUSPEND_203, String.valueOf(i++));
							}

						} else {
							responseCode = 204;
							responseMessage = i18n.get(Constants.RESPONCE_CODE_SUSPEND_204);
						}

						break;
					case RESUME:
						String currentLifeCycleStateResumeFrom = lifeCycleStateService
								.getCurrentLifeCycleState(request.getResourceResolver(), pagePath[i]);
						if (StringUtils.isNotEmpty(currentLifeCycleStateResumeFrom)) {
							boolean canSuspend = lifeCycleStateService.canResume(currentLifeCycleid, StringUtils.EMPTY,
									currentLifeCycleStateResumeFrom, request.getResourceResolver(), pagePath[i]);
							if (canSuspend) {
								lifeCycleStateService.doResume(currentLifeCycleid, StringUtils.EMPTY,
										currentLifeCycleStateResumeFrom, request.getResourceResolver(), pagePath[i]);
								responseCode = 201;
								responseMessage = i18n.get(Constants.RESPONCE_CODE_RESUME_201, String.valueOf(i++));
							} else {
								responseCode = 203;
								responseMessage = i18n.get(Constants.RESPONCE_CODE_RESUME_203, String.valueOf(i++));
							}

						} else {
							responseCode = 204;
							responseMessage = i18n.get(Constants.RESPONCE_CODE_RESUME_204);
						}
						break;
					default:
						break;
					}
				} else {
					responseCode = 202;
					responseMessage = i18n.get(Constants.RESPONCE_CODE_202);
				}

			}

			JSONWriter jsonWriter = new JSONWriter(resp.getWriter());
			jsonWriter.object();
			jsonWriter.key("statuscode").value(responseCode);
			jsonWriter.key("statusmessage").value(responseMessage);

			jsonWriter.endObject();

		} catch (Exception e) {
			log("Error : " + e);
		}

	}

	protected enum Actions {
		PROMOTE("promote"), DEMOTE("demote"), SUSPEND("suspend"), RESUME("resume"), EMPTY(StringUtils.EMPTY);

		private String value;

		Actions(String value) {
			this.value = value;
		}

		public static Actions fromString(String value) {
			for (Actions action : values()) {
				if (StringUtils.equals(value, action.value)) {
					return action;
				}
			}
			return null;
		}
	}

}
