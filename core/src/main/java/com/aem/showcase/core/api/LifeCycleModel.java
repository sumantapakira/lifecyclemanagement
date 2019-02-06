package com.aem.showcase.core.api;

import java.util.List;

import com.aem.showcase.core.api.StateTransitions;

public interface LifeCycleModel {

	public LifeCycleNode createRootNode();

	public LifeCycleNode createSuspendedNode(String parentNode);

	public LifeCycleNode createNode(String param1, String param2, String param3, boolean isSuspendedNode, String color,
			String name);

	public StateTransitions createTransition(LifeCycleNode fromNode, LifeCycleNode toNode, String rule);

	public void setTitle(String title);

	public void setId(String id);

	public void setDescription(String description);

	public void setRootNode(LifeCycleNode lifeCycleNode);

	public void setEndNode(LifeCycleNode lifeCycleNode);

	public String getId();

	public String getTitle();

	public String getDescription();

	public List<LifeCycleNode> getNodes();

	public List<StateTransitions> getTransitions();

}
