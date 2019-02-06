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

import com.aem.showcase.core.api.LifeCycleModel;
import com.aem.showcase.core.api.LifeCycleStateService;


@Component(name = "com.aem.showcase.core.impl.GetLifeCycleTitle", service = Servlet.class, property = {
		"service.description=Get life cycle title", "sling.servlet.methods=GET",
		"sling.servlet.paths=/bin/getlifecycletitle" })

public class GetLifeCycleTitle extends SlingSafeMethodsServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(GetLifeCycleTitle.class);

	@Reference
	LifeCycleStateService lifeCycleStateService;

	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("utf-8");
		try {
			String url = request.getParameter("url");
			String title = StringUtils.EMPTY;

			if (StringUtils.isBlank(url)) {
				return;
			}
			String resourcePath = StringUtils.substringAfter(url, ".html");
			String currentLifeCycleId = lifeCycleStateService.getCurrentLifeCycleId(request.getResourceResolver(),
					resourcePath);
			if (StringUtils.isNotBlank(currentLifeCycleId)) {
				LifeCycleModel model = lifeCycleStateService.getLifeCycleModel(request.getResourceResolver(),
						currentLifeCycleId);
				if (model != null) {
					title = model.getTitle();
				}
			}

			JSONWriter jsonWriter = new JSONWriter(resp.getWriter());
			jsonWriter.object();
			jsonWriter.key("title").value(title);

			jsonWriter.endObject();

		} catch (Exception e) {
			log("Error : " + e);
		}

	}

}
