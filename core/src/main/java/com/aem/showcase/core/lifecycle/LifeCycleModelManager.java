package com.aem.showcase.core.lifecycle;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.aem.showcase.core.api.LifeCycleModel;
import com.aem.showcase.core.api.LifeCycleNode;
import com.aem.showcase.core.api.LifeCycleSession;
import com.aem.showcase.core.api.StateTransitions;

public class LifeCycleModelManager {

	private static final Logger log = getLogger(LifeCycleModelManager.class);

	private String modelLocation;
	private LifeCycleSession session;

	public LifeCycleModelManager(LifeCycleSession session, String modelLocation) {
		this.modelLocation = modelLocation;
		this.session = session;
	}

	public void save(LifeCycleModel model) {
		try {
			Session jcrSession = (Session) this.session.adaptTo(Session.class);
			Node modelStorageLocation = (Node) jcrSession.getItem(this.modelLocation);
			if (StringUtils.isBlank(model.getId())) {
				String id = createName(model.getTitle(), modelStorageLocation);
				((LifeCycleModelImpl) model).setId(this.modelLocation + "/" + id);
			} else {
				((LifeCycleModelImpl) model).setId(model.getId());
			}
			String relativeModelPath = model.getId().replace(modelStorageLocation.getPath(), "");

			if (relativeModelPath.startsWith("/")) {
				relativeModelPath = relativeModelPath.substring(1);
			}
			String[] path = relativeModelPath.split("/");
			for (int i = 0; i < path.length - 1; i++) {

				if (!modelStorageLocation.hasNode(path[i])) {
					modelStorageLocation = modelStorageLocation.addNode(path[i]);
				} else {
					modelStorageLocation = modelStorageLocation.getNode(path[i]);
				}
			}
			String label = path[(path.length - 1)];
			Node modelNode;
			if (!modelStorageLocation.hasNode(label)) {

				modelNode = modelStorageLocation.addNode(label, "nt:unstructured"); // ns:LifeCycleModel
			} else {

				modelNode = modelStorageLocation.getNode(label);
				NodeIterator nitr = modelNode.getNodes();
				while (nitr.hasNext()) {
					nitr.nextNode().remove();
				}
			}
			modelNode.setProperty("title", model.getTitle());
			modelNode.setProperty("description", model.getDescription());

			Node nodes = modelNode.addNode("nodes", "nt:unstructured");

			for (LifeCycleNode lsNode : model.getNodes()) {

				Node aNode = nodes.addNode(lsNode.getId(), "nt:unstructured");
				aNode.setProperty("title", lsNode.getTitle());
				aNode.setProperty("description", lsNode.getDescription());
				aNode.setProperty("type", lsNode.getType());
				aNode.setProperty("color", lsNode.getColor());
				aNode.setProperty("name", lsNode.getName());

			}
			Node transitions = modelNode.addNode("transitions", "nt:unstructured");
			Iterator<StateTransitions> transIt = model.getTransitions().iterator();
			while (transIt.hasNext()) {
				StateTransitions lcTransition = (StateTransitions) transIt.next();
				Node aNode = transitions.addNode(createTransitionName(lcTransition), "nt:unstructured");
				aNode.setProperty("rule", lcTransition.getRule());
				aNode.setProperty("from", lcTransition.getFrom().getName());
				aNode.setProperty("to", lcTransition.getTo().getName());

			}
			jcrSession.save();

		} catch (RepositoryException re) {
			throw new RuntimeException("Unable to save LifeCycle model", re);
		}
	}

	private String createTransitionName(StateTransitions transition) {
		return transition.getFrom().getId() + "#" + transition.getTo().getId();
	}

	private String createName(String title, Node rootNode) throws RepositoryException {
		String id = title.replaceAll(" ", "-");

		int count = 0;
		String newID = id;
		while (rootNode.hasNode(newID)) {
			count++;
			newID = id + "_" + count;
		}
		return newID;
	}

}
