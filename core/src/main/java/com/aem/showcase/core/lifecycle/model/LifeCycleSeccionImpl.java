package com.aem.showcase.core.lifecycle.model;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import org.apache.sling.adapter.annotations.Adaptable;
import org.slf4j.Logger;

import com.aem.showcase.core.api.LifeCycleModel;
import com.aem.showcase.core.api.LifeCycleNode;
import com.aem.showcase.core.api.LifeCycleSession;
import com.aem.showcase.core.api.StateTransitions;

import com.aem.showcase.core.lifecycle.LifeCycleModelImpl;
import com.aem.showcase.core.lifecycle.LifeCycleModelManager;

import com.aem.showcase.core.lifecycle.LifeCycleNodeImpl;
import com.aem.showcase.core.lifecycle.transitions.StateTransitionImpl;

@Adaptable(adaptableClass = LifeCycleSession.class, adapters = {
		@org.apache.sling.adapter.annotations.Adapter({ Session.class, ResourceResolver.class }) })
public class LifeCycleSeccionImpl implements LifeCycleSession {

	private static final Logger log = getLogger(LifeCycleSeccionImpl.class);
	private static final String LIFE_CYCLE_LOCATION = "/var/lifecycle/models";

	Session userSession;
	ResourceResolverFactory resourceResolverFactory;
	private LifeCycleModelManager modelManager;

	public LifeCycleSeccionImpl(Session session, ResourceResolverFactory resourceResolverFactory) {
		this.userSession = session;
		this.resourceResolverFactory = resourceResolverFactory;

		this.modelManager = new LifeCycleModelManager(this, LIFE_CYCLE_LOCATION);
	}

	@Override
	public LifeCycleModel createLifeCycleModel(String id, String title) {

		LifeCycleModel model = new LifeCycleModelImpl(id, null);
		model.setDescription(title);
		model.setTitle(title);

		LifeCycleNode start = model.createNode("Draft", "DRAFT", "Initial state of a lifecycle", false, "", "");
		LifeCycleNode underReview = model.createNode("Under Review", "UNDER REVIEW", "In review lifecycle", false, "",
				"");
		LifeCycleNode end = model.createNode("Approved", "APPROVED", "End state of a lifecycle", false, "", "");

		model.setRootNode(start);
		model.setRootNode(underReview);
		model.setEndNode(end);

		model.createTransition(start, underReview, "");
		model.createTransition(underReview, end, "");

		deployModel(model);

		return model;
	}

	@Override
	public void deployModel(LifeCycleModel model) {
		log.debug("Deploying workflow model: " + model.getId());

		LifeCycleModelImpl cqModel = (LifeCycleModelImpl) model;
		this.modelManager.save(cqModel);

		log.debug("LifeCycle model deployed: " + model.getId());

	}

	@Override
	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {

		if (ResourceResolver.class == type) {
			if ((getSession() != null) && (this.resourceResolverFactory != null)) {
				try {
					Map<String, Object> authInfo = new HashMap<String, Object>();
					authInfo.put("user.jcr.session", getSession());
					log.debug("resourceResolverFactory **********", resourceResolverFactory);
					return (AdapterType) this.resourceResolverFactory.getResourceResolver(authInfo);
				} catch (org.apache.sling.api.resource.LoginException e) {
					log.error("failed to adapt LifeCycleSession to ResourceResolver", e);
				}
			}
		} else if (Session.class == type) {
			return (AdapterType) getSession();
		}
		return null;
	}

	public Session getSession() {
		return this.userSession;
	}

	@Override
	public LifeCycleModel[] getModels() {

		// Session jcrSession = (Session)this.userSession.adaptTo(Session.class);

		Session jcrSession = this.userSession;
		log.debug("******** this.userSession ********** : " + userSession.getUserID());

		List<LifeCycleModel> result = new ArrayList<LifeCycleModel>();
		Map<String, LifeCycleModel> collatedModels = new HashMap<String, LifeCycleModel>();
		try {
			if (jcrSession.nodeExists(LIFE_CYCLE_LOCATION)) {
				collatedModels = getModels(jcrSession.getNode(LIFE_CYCLE_LOCATION));
			}
		} catch (RepositoryException e) {
			log.error("failed to adapt Session to LifeCycleSession", e);
		}

		for (LifeCycleModel model : collatedModels.values()) {
			result.add(model);
		}

		LifeCycleModel[] lifeCycleModels = result.toArray(new LifeCycleModel[result.size()]);

		return lifeCycleModels;
	}

	private Map<String, LifeCycleModel> getModels(Node modelsHome) {
		Map<String, LifeCycleModel> included = new HashMap<String, LifeCycleModel>();
		try {
			NodeIterator models = modelsHome.getNodes();
			while (models.hasNext()) {
				Node node = models.nextNode();
				if ("nt:unstructured".equals(node.getPrimaryNodeType().getName())) {
					String path = null;
					try {
						long st = System.currentTimeMillis();
						path = node.getPath();
						LifeCycleModel model = loadLifeCycleModel(node);
						if (log.isDebugEnabled()) {
							log.debug("createWfModel for " + modelsHome.getSession().getUserID() + " took "
									+ (System.currentTimeMillis() - st) + "ms");
						}
						included.put(model.getId(), model);
					} catch (Exception we) {
						log.warn("Cannot load model: " + path);
					}
				} else {
					included.putAll(getModels(node));
				}
			}
		} catch (Exception e) {
			log.error("Error occured : ", e);
		}
		return included;
	}

	private LifeCycleModel loadLifeCycleModel(Node node) {
		String mid = null;
		try {
			mid = node.getPath();
			LifeCycleModelImpl model = new LifeCycleModelImpl();
			model.setId(mid);
			model.setTitle(node.getProperty("title").getString());

			if (node.hasProperty("description")) {
				model.setDescription(node.getProperty("description").getString());
			}
			NodeIterator itr = node.getNode("nodes").getNodes();
			while (itr.hasNext()) {

				Node nNode = itr.nextNode();
				LifeCycleNode lsNode = createLifeCycleNode(nNode);
				model.getNodes().add(lsNode);
			}
			itr = node.getNode("transitions").getNodes();
			while (itr.hasNext()) {
				Node tNode = itr.nextNode();
				StateTransitions lsTransition = createWorkflowTransition(tNode, model);
				model.getTransitions().add(lsTransition);
			}
			// populateMetaDataMap(node, model, resourceResolver);

			return model;
		} catch (RepositoryException re) {
			throw new RuntimeException("Cannot create LifeCycle model from node: " + re);
		}
	}

	private static LifeCycleNodeImpl createLifeCycleNode(Node node) {
		try {
			LifeCycleNodeImpl lsNode = new LifeCycleNodeImpl();
			lsNode.setId(node.getName());
			lsNode.setType(node.getProperty("type").getString());

			if (node.hasProperty("title")) {
				lsNode.setTitle(node.getProperty("title").getString());
			}
			if (node.hasProperty("type")) {
				lsNode.setType(node.getProperty("type").getString());
			}

			if (node.hasProperty("description")) {
				lsNode.setDescription(node.getProperty("description").getString());
			}
			if (node.hasProperty("color")) {
				lsNode.setColor(node.getProperty("color").getString());
			}
			if (node.hasProperty("name")) {
				lsNode.setName(node.getProperty("name").getString());
			}
			return lsNode;
		} catch (RepositoryException re) {
			throw new RuntimeException("Cannot create LifeCycle node from node" + re);
		}
	}

	@Override
	public LifeCycleModel getModel(String modelId) {
		LifeCycleModel lifeCycleModel = null;
		Session jcrSession = this.userSession;
		try {
			Node modelNode = jcrSession.getNode(modelId);
			lifeCycleModel = loadLifeCycleModel(modelNode);
		} catch (PathNotFoundException e) {
			log.error("Errror " + e);
		} catch (RepositoryException e) {
			log.error("Errror " + e);
		}

		return lifeCycleModel;
	}

	private StateTransitions createWorkflowTransition(Node node, LifeCycleModel model) {
		StateTransitionImpl wfTransition = new StateTransitionImpl();
		try {
			String fromId = node.getProperty("from").getString();
			String toId = node.getProperty("to").getString();

			for (LifeCycleNode wfNode : model.getNodes()) {
				if (wfNode.getName().equals(fromId)) {
					wfNode.getTransitions().add(wfTransition);
					wfTransition.setFrom(wfNode);
				} else if (wfNode.getName().equals(toId)) {
					wfNode.getIncomingTransitions().add(wfTransition);
					wfTransition.setTo(wfNode);
				}
			}
			if (node.hasProperty("rule")) {
				wfTransition.setRule(node.getProperty("rule").getString());
			}
		} catch (Exception e) {
			log.error("error: " + e);
		}
		return wfTransition;
	}

}
