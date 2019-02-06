package com.aem.showcase.core.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

import com.aem.showcase.core.Constants;
import com.aem.showcase.core.api.LifeCycleModel;
import com.aem.showcase.core.api.LifeCycleNode;
import com.aem.showcase.core.api.LifeCycleSession;
import com.day.cq.i18n.I18n;

@Component(name = "com.aem.showcase.core.impl.GenerateLifeCycleModel", service = Servlet.class, property = {
		"service.description=Generate Life Cycle model", "sling.servlet.methods=GET", "sling.servlet.methods=POST",
		"sling.servlet.resourceTypes=" + GenerateLifeCycleModel.SERVLET_RT, "sling.servlet.extensions=" + "json",
		"sling.servlet.selectors=" + "generatelifecycle", "sling.servlet.selectors=" + "editlifecycle", })

public class GenerateLifeCycleModel extends SlingSafeMethodsServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = getLogger(GenerateLifeCycleModel.class);

	public static final String SERVLET_RT = "cq/lifecycle/components/pages/model";

	private static Map<String, String> STEP_MAPPINGS = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			put("cq/lifecycle/flow/components/lifecyclestep", "cq/lifecycle/flow/components/lifecyclestep");

		}
	};

	protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("utf-8");

		ResourceResolver resolver = req.getResourceResolver();
		Map<String, String> errors = new HashMap();

		I18n i18n = new I18n(req);

		Resource res = req.getResource();

		String msg = i18n.get("Model successfully generated.");

		ValueMap props = (ValueMap) res.adaptTo(ValueMap.class);

		String title = (String) props.get("jcr:title", "No Title");

		String desc = (String) props.get("jcr:description", "No Description");

		Node content = (Node) res.adaptTo(Node.class);
		try {
			String[] selectors = req.getRequestPathInfo().getSelectors();
			String modelId = null;
			LifeCycleSession lifeCycleSession = (LifeCycleSession) resolver.adaptTo(LifeCycleSession.class);
			if (content.getPath().startsWith("/conf/global/settings/lifecycle/models")) {
				modelId = content.getPath().replace("/conf/global/settings/lifecycle/models", "/var/lifecycle/models");
				modelId = modelId.replace("/jcr:content", "");
			}
			if (content.hasNode("normalflow") && selectors != null
					&& !Arrays.stream(selectors).anyMatch("editlifecycle"::equals)) {
				logger.debug("==== generating lifecycle for: {} ", content.getPath());
				logger.debug("==== generating lifecycle modelId is: {} ", modelId);
				logger.debug("==== generating lifeCycleSession is: {} ", lifeCycleSession);
				lifeCycleSession.createLifeCycleModel(modelId, title);
			} else {
				logger.debug("==== Edit for Life cycle model: {} ", modelId);
				LifeCycleModel model = lifeCycleSession.getModel(modelId);
				model.getNodes().clear();
				model.getTransitions().clear();

				ContentVisitor contentVisitor = new ContentVisitor(resolver, model, i18n);
				contentVisitor.visit(content.getNode("normalflow"));

				if (content.hasNode("suspendnode")) {
					contentVisitor.visit(content.getNode("suspendnode"));
				}
				lifeCycleSession.deployModel(contentVisitor.getModel());
			}
		} catch (RepositoryException e) {
			logger.error("Error: ", e);
		}

		JSONWriter w = new JSONWriter(resp.getWriter());
		try {
			w.object();
			w.key("msg").value(msg);
			if (errors.size() > 0) {
				w.key("errorList");
				w.array();
				for (Map.Entry<String, String> entry : errors.entrySet()) {
					w.object();
					w.key("errorPath").value(entry.getKey());
					w.key("errorMsg").value(entry.getValue());
					w.endObject();
				}
				w.endArray();
			}
			w.endObject();
		} catch (JSONException e) {
			log("Error", e);
		}

	}

	private class ContentVisitor extends TraversingItemVisitor.Default {
		private ResourceResolver resolver;
		private LifeCycleModel model;
		private Resource flow;
		I18n i18n;
		private Map<LifeCycleNode, ValueMap> rules;
		private Map<String, String> errors;
		private Stack<List<LifeCycleNode>> nested;
		private Stack<LifeCycleNode> suspendednested;
		private Map<String, LifeCycleNode> designtimePathMap;
		private int count = 0;
		private int suspendNodecount = 0;

		public ContentVisitor(final ResourceResolver res, final LifeCycleModel m, final I18n i18n) {
			this.rules = (Map<LifeCycleNode, ValueMap>) new HashMap<LifeCycleNode, ValueMap>();
			this.errors = (Map<String, String>) new HashMap<String, String>();
			this.nested = (Stack<List<LifeCycleNode>>) new Stack<List<LifeCycleNode>>();
			this.suspendednested = (Stack<LifeCycleNode>) new Stack<LifeCycleNode>();
			this.resolver = res;
			this.model = m;
			this.designtimePathMap = (Map<String, LifeCycleNode>) new HashMap<String, LifeCycleNode>();
			this.i18n = i18n;
		}

		public Map<String, String> getErrors() {
			return this.errors;
		}

		public LifeCycleModel getModel() {
			return this.model;
		}

		protected void entering(final Node node, final int level) throws RepositoryException {
			final Resource res = this.resolver.getResource(node.getPath());
			final ValueMap props = (ValueMap) res.adaptTo(ValueMap.class);
			GenerateLifeCycleModel.logger.debug("Entering: {} and level: {}", (Object) res.getPath(), level);

			GenerateLifeCycleModel.logger.debug("entering Count value is: {}", count);

			if (res != null) {
				if (this.flow == null && !res.getPath().contains("suspendnode")) {
					this.flow = res;
				} else if (res.getResourceType().equals(Constants.RT_LIFE_CYCLE_STEP)) {
					if (count == 1 && !res.getPath().contains("suspendnode")) {
						final List<LifeCycleNode> current = (List<LifeCycleNode>) new ArrayList<LifeCycleNode>();
						this.nested.push((List<LifeCycleNode>) current);
						String color = props.get("color", String.class);
						String name = props.get("name", String.class);
						String title = props.get("jcr:title", String.class);
						GenerateLifeCycleModel.logger.debug("Draft node color is {} for resource {}", color,
								res.getPath());

						final LifeCycleNode start = this.model.createNode(title, "DRAFT",
								"Initial state of a lifecycle", false, color, name);
						this.suspendednested.push(start);

						this.model.setRootNode(start);
						current.add((LifeCycleNode) start);
					} else {
						this.handleStep(res, props, this.i18n);
					}
				}
			}
			count++;
		}

		protected void leaving(final Node node, final int level) throws RepositoryException {
			final Resource res = this.resolver.getResource(node.getPath());
			GenerateLifeCycleModel.logger.debug("leaving Count value is: {}", count);

			if (res != null) {
				String path = res.getPath();
				if (this.flow != null && this.flow.getPath().equals((Object) res.getPath())
						&& !path.contains("suspendnode")) {
					this.flow = null;
				}

			}
		}

		private LifeCycleNode handleStep(final Resource res, final ValueMap props, final I18n i18n) {
			GenerateLifeCycleModel.logger.debug("******** handleStep resource type {}", res);

			final String title = (String) props.get("jcr:title", String.class);
			final String desc = (String) props.get("jcr:description", String.class);
			final String color = (String) props.get("color", String.class);
			final String name = (String) props.get("name", String.class);

			/*
			 * final String type = this.getStepType(res);
			 * 
			 * if (type == null) { return null; }
			 */

			LifeCycleNode node = null;

			if (res.getPath().contains("suspendnode")) {
				node = this.model.createNode(title, Constants.SUSPEND_RULE, desc, true, color, name);

				this.model.createTransition(suspendednested.peek(), node, Constants.SUSPEND_RULE);
				this.suspendednested.push(node);

			} else {
				node = this.model.createNode(title, Constants.NORMAL_RULE, desc, false, color, name);
				GenerateLifeCycleModel.this.addPathToNodeMapping(this.designtimePathMap, res, node);
				final List<LifeCycleNode> current = (List<LifeCycleNode>) this.nested.peek();

				LifeCycleNode last = (LifeCycleNode) current.get(current.size() - 1);
				if (this.nested.size() > 1) {
					last = (LifeCycleNode) current.get(current.size() - 2);
					current.add(current.size() - 1, (LifeCycleNode) node);
				} else {
					current.add((LifeCycleNode) node);
				}

				this.model.createTransition(last, node, Constants.NORMAL_RULE);
			}

			return node;
		}

		/*
		 * private String getStepType(final Resource res) { for (final String resType :
		 * GenerateLifeCycleModel.this.STEP_MAPPINGS.keySet()) { if
		 * (res.getResourceType().equals(resType)) { return (String)
		 * GenerateLifeCycleModel.this.STEP_MAPPINGS.get((Object) resType); } }
		 * GenerateLifeCycleModel.logger.error("Not a valid workflow step resource: {}",
		 * (Object) res); return null; }
		 */

		public Map<String, LifeCycleNode> getDesigntimePathMap() {
			return this.designtimePathMap;
		}
	}

	private void addPathToNodeMapping(Map<String, LifeCycleNode> mapDesigntimePathsToNodes, Resource res,
			LifeCycleNode node) {
		if ((res != null) && (res.getPath() != null) && (node != null)) {
			mapDesigntimePathsToNodes.put(stepPathRelativeToModelFlowPath(res.getPath()), node);
		}
	}

	public static String stepPathRelativeToModelFlowPath(String path) {
		int flowPathIndex = StringUtils.indexOf(path, "jcr:content/flow");
		if (flowPathIndex != -1) {
			return path.substring(flowPathIndex + "jcr:content/flow".length());
		}
		return null;
	}

}
