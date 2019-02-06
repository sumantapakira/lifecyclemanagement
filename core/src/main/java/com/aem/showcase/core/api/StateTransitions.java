package com.aem.showcase.core.api;



public interface StateTransitions {
	
	public LifeCycleNode getFrom();
	public LifeCycleNode getTo();
	public void setFrom(LifeCycleNode lifeCycleNode);
	public void setTo(LifeCycleNode lifeCycleNode);
	public String getRule();
	public void setRule(String rule);
	

}
