package com.aem.showcase.core.api;

import org.apache.sling.api.adapter.Adaptable;



public interface LifeCycleSession extends Adaptable{
	
	public LifeCycleModel createLifeCycleModel(String modelId, String title);
	public LifeCycleModel[] getModels();
	public LifeCycleModel getModel(String modelId);
	public void deployModel(LifeCycleModel model);

}
