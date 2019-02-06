package com.aem.showcase.core.lifecycle.transitions;

import com.aem.showcase.core.api.LifeCycleNode;
import com.aem.showcase.core.api.StateTransitions;


public class StateTransitionImpl implements StateTransitions {

	private LifeCycleNode toNode;
	private LifeCycleNode fromNode;
	private String rule;

	public StateTransitionImpl() {
	}

	public StateTransitionImpl(LifeCycleNode from, LifeCycleNode to, String rule) {
		this.fromNode = from;
		this.toNode = to;
		this.rule = rule;
	}

	@Override
	public LifeCycleNode getFrom() {
		return this.fromNode;
	}

	@Override
	public LifeCycleNode getTo() {
		return this.toNode;
	}

	@Override
	public void setFrom(LifeCycleNode lifeCycleNode) {
		this.fromNode = lifeCycleNode;
	}

	@Override
	public void setTo(LifeCycleNode lifeCycleNode) {
		this.toNode = lifeCycleNode;
	}

	@Override
	public String getRule() {
		return this.rule;
	}

	@Override
	public void setRule(String rule) {
		this.rule = rule;
	}

}
