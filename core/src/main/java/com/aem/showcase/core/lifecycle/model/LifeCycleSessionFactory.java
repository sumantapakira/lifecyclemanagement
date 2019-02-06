package com.aem.showcase.core.lifecycle.model;

import javax.jcr.Session;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aem.showcase.core.api.LifeCycleSession;


@Component(service = AdapterFactory.class, name = "com.aem.showcase.core.lifecycle.model.LifeCycleSessionFactory", immediate = true,
property= {"adapters=com.aem.showcase.core.lifecycle.model.LifeCycleSession", "adaptables=javax.jcr.Session",
		"adaptables=org.apache.sling.api.resource.ResourceResolver"})
public class LifeCycleSessionFactory implements AdapterFactory{
	private static Logger log = LoggerFactory.getLogger(LifeCycleSessionFactory.class);
	
	@Reference(policy=ReferencePolicy.STATIC)
	private ResourceResolverFactory resolverFactory;
	
	@SuppressWarnings("unchecked")
	@Override
	public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
		Session userSession = null;
	    if (LifeCycleSession.class == type)
	    {
	      if ((adaptable instanceof Session))
	      {
	        log.debug("Adapting Session to LifeCycleSession");
	        userSession = (Session)adaptable;
	      }
	      else if ((adaptable instanceof ResourceResolver))
	      {
	        log.debug("Adapting ResourceResolver to LifeCycleSession");
	        userSession = (Session)((ResourceResolver)adaptable).adaptTo(Session.class);
	      }
	      else
	      {
	        log.info("Can't adapt {} to LifeCycleSession", adaptable == null ? "null" : adaptable.getClass().getName());
	      }
	      if (userSession != null)
	      {
	        if (log.isDebugEnabled()) {
	          log.debug("Creating LifeCycleSession session for user: " + userSession.getUserID());
	        }
	        return (AdapterType) getLifeCycleSession(userSession);
	      }
	    }
	    return null;
	}
	
	private LifeCycleSession getLifeCycleSession(Session session) {
		LifeCycleSeccionImpl lifeCycleSeccion = new LifeCycleSeccionImpl(session,resolverFactory);
		
		return  lifeCycleSeccion;
	}
	
	


}
