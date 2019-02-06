package com.aem.showcase.core.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aem.showcase.core.api.LifeCycleModel;
import com.aem.showcase.core.api.LifeCycleNode;
import com.aem.showcase.core.api.StateTransitions;
import com.aem.showcase.core.lifecycle.transitions.StateTransitionImpl;

public class LifeCycleModelImpl implements LifeCycleModel {

	private static Logger log = LoggerFactory.getLogger(LifeCycleModelImpl.class);

	private String title;
	private String version;
	private String description;
	private String id;
	private List<StateTransitions> transitions = new ArrayList<StateTransitions>();

	private List<LifeCycleNode> nodes = new ArrayList<LifeCycleNode>();
	private LifeCycleNode rootNode;
	private LifeCycleNode endNode;

	public LifeCycleModelImpl() {
	}

	public LifeCycleModelImpl(String id, String name) {
		this.id = id;
		this.title = name;
	}

	public List<LifeCycleNode> getNodes() {
		return this.nodes;
	}

	@Override
	public LifeCycleNode createRootNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LifeCycleNode createSuspendedNode(String parentNode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LifeCycleNode createNode(String title, String type, String description, boolean isSuspendedNode,
			String color, String name) {

		long nodeId = -1L;
		long susPendnodeId = -1L;
		LifeCycleNode lsnode = null;

		for (LifeCycleNode node : this.nodes) {
			if (node.getId().startsWith("node")) {
				try {
					long tmpId = Long.valueOf(node.getId().substring("node".length())).longValue();
					if (tmpId > nodeId) {
						nodeId = tmpId;
					}
				} catch (NumberFormatException localNumberFormatException) {
				}
			}
			if (node.getId().startsWith("suspendnode")) {
				try {
					long tmpId = Long.valueOf(node.getId().substring("suspendnode".length())).longValue();
					if (tmpId > susPendnodeId) {
						susPendnodeId = tmpId;
					}
				} catch (NumberFormatException localNumberFormatException) {
				}
			}
		}

		if (!isSuspendedNode) {
			lsnode = new LifeCycleNodeImpl("node" + ++nodeId, title, type, description, color, name);
			this.nodes.add(lsnode);
		} else {
			lsnode = new LifeCycleNodeImpl("suspendnode" + ++susPendnodeId, title, type, description, color, name);
			this.nodes.add(lsnode);
		}

		return lsnode;
	}

	@Override
	public StateTransitions createTransition(LifeCycleNode fromNode, LifeCycleNode toNode, String rule) {
		StateTransitions statetransition = new StateTransitionImpl(fromNode, toNode, rule);
		fromNode.getTransitions().add(statetransition);

		this.transitions.add(statetransition);

		return statetransition;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setRootNode(LifeCycleNode lifeCycleNode) {
		this.rootNode = lifeCycleNode;

	}

	@Override
	public void setEndNode(LifeCycleNode lifeCycleNode) {
		this.endNode = lifeCycleNode;

	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getTitle() {
		return this.title;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public List<StateTransitions> getTransitions() {
		return this.transitions;
	}

}
