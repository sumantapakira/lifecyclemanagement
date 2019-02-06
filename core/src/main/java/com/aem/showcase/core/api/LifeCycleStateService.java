package com.aem.showcase.core.api;

import org.apache.sling.api.resource.ResourceResolver;

import com.aem.showcase.core.Exceptons.LifeCycleException;



public interface LifeCycleStateService {

	public boolean canPromote(String model, String promoteTo, String promoteFrom, ResourceResolver resolver,
			String resourcePath);

	public void doPromote(String model, String promoteTo, String promoteFrom, ResourceResolver resourceResolver,
			String resourcePath) throws LifeCycleException;

	public boolean canDemote(String model, String demoteTo, String demoteFrom, ResourceResolver resolver,
			String resourcePath);

	public void doDemote(String model, String demoteTo, String demoteFrom, ResourceResolver resourceResolver,
			String resourcePath) throws LifeCycleException;

	public boolean canSuspend(String model, String suspendTo, String suspendFrom, ResourceResolver resolver,
			String resourcePath);

	public void doSuspend(String model, String suspendTo, String suspendFrom, ResourceResolver resourceResolver,
			String resourcePath) throws LifeCycleException;

	public boolean canResume(String model, String resumeTo, String resumeFrom, ResourceResolver resolver,
			String resourcePath);

	public void doResume(String model, String resumeTo, String resumeFrom, ResourceResolver resourceResolver,
			String resourcePath) throws LifeCycleException;

	public String[] getFirstandLastElementOfLifeCycle(String modelid, ResourceResolver resolver);

	public LifeCycleNode[] getFirstandLastElementOfLifeCycle(LifeCycleModel modelid, ResourceResolver resolver);

	public String getCurrentLifeCycleState(ResourceResolver resourceResolver, String path);

	public void apply(LifeCycleModel model, ResourceResolver resolver, String resourcePath) throws LifeCycleException;

	public String getCurrentLifeCycleId(ResourceResolver resolver, String resourcePath);
	
	public LifeCycleModel getLifeCycleModel(ResourceResolver resourceResolver, String modelId);

}
