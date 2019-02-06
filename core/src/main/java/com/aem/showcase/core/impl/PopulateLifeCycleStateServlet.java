package com.aem.showcase.core.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.service.component.annotations.Component;

@Component(name = "com.aem.showcase.core.impl.PopulateLifeCycleStateServlet", service = Servlet.class, property = {
		"service.description=Generate Life Cycle States", "sling.servlet.methods=GET",
"sling.servlet.paths=/bin/lifeCycleStates" })

public class PopulateLifeCycleStateServlet extends SlingSafeMethodsServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("utf-8");
		try {
			ResourceResolver resolver = request.getResourceResolver();
			Session sess = (Session) resolver.adaptTo(Session.class);
			String lifeCycleModel = request.getParameter("selectedModel");
			if (StringUtils.isBlank(lifeCycleModel)) {
				return;
			}
			Node modelNode = sess.getNode(lifeCycleModel + "/nodes");

			NodeIterator modelsItr = modelNode.getNodes();
			Map<String, String> map = new HashMap<String, String>();

			while (modelsItr.hasNext()) {
				Node lifeCycleStatesNodes = modelsItr.nextNode();
				String title = lifeCycleStatesNodes.hasProperty("title")
						? lifeCycleStatesNodes.getProperty("title").getString()
								: "";
						String name = lifeCycleStatesNodes.hasProperty("name")
								? lifeCycleStatesNodes.getProperty("name").getString()
										: "";
								map.put(name, title);
			}
			Map<String, String> result = map.entrySet().stream().sorted(Map.Entry.comparingByValue())
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
							LinkedHashMap::new));

			JSONWriter jsonWriter = new JSONWriter(resp.getWriter());
			jsonWriter.object();
			for (Map.Entry<String, String> entry : result.entrySet()) {
				jsonWriter.key(entry.getKey()).value(entry.getValue());
			}

			jsonWriter.endObject();

		} catch (JSONException | RepositoryException e) {
			log("Error : " + e);
		}

	}

}
