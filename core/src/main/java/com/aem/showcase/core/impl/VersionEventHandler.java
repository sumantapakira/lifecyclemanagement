package com.aem.showcase.core.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;

import com.aem.showcase.core.Constants;
import com.aem.showcase.core.api.LifeCycleStateService;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.PageEvent;
import com.day.cq.wcm.api.PageModification;

@Component(service = { EventHandler.class,
		JobConsumer.class }, immediate = true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property = {
				"event.topics=" + PageEvent.EVENT_TOPIC,
				JobConsumer.PROPERTY_TOPICS + "=" + VersionEventHandler.JOB_TOPICS })
public class VersionEventHandler implements EventHandler, JobConsumer {

	private static final Logger logger = getLogger(VersionEventHandler.class);

	private static final String PAGE_EVENT = "pageEvent";

	public static final String JOB_TOPICS = "aem/custom/version";

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private JobManager jobManager;

	@Reference
	LifeCycleStateService lifeCycleStateService;

	@Override
	public void handleEvent(Event event) {
		PageEvent pageEvent = PageEvent.fromEvent(event);
		Map<String, Object> properties = new HashMap<>();
		properties.put(PAGE_EVENT, pageEvent);
		jobManager.addJob(JOB_TOPICS, properties);

	}

	@Override
	public JobResult process(Job job) {
		PageEvent pageEvent = (PageEvent) job.getProperty(PAGE_EVENT);
		ResourceResolver resolver = getResolver();
		try {
			if (pageEvent != null && pageEvent.isLocal()) {
				Iterator<PageModification> modificationsIterator = pageEvent.getModifications();
				while (modificationsIterator.hasNext()) {
					PageModification modification = modificationsIterator.next();
					if (PageModification.ModificationType.VERSION_CREATED.equals(modification.getType())) {

						String path = modification.getPath();
						Node node = resolver.getResource(path + "/" + JcrConstants.JCR_CONTENT).adaptTo(Node.class);
						String modelid = lifeCycleStateService.getCurrentLifeCycleId(resolver, path);
						if (StringUtils.isNotEmpty(modelid)) {
							String[] initialAndEndState = lifeCycleStateService
									.getFirstandLastElementOfLifeCycle(modelid, resolver);
							if (initialAndEndState != null) {
								node.setProperty(Constants.PN_LIFECYCLE, initialAndEndState[0]);
								node.setProperty(Constants.PN_LIFECYCLE_ID, StringUtils.EMPTY);
								node.setProperty(Constants.PN_STATE_TITLE, StringUtils.EMPTY);
								node.setProperty(Constants.PN_COLOR, StringUtils.EMPTY);
								node.getSession().save();
							}
						}

					}
				}

			}
		} catch (Exception e) {
			logger.error("Error : ", e);
		} finally {
			if (resolver.isLive()) {
				resolver.close();
			}
		}
		return JobResult.OK;
	}

	/**
	 * Get a resource resolver.
	 *
	 * @return resolver or null
	 */
	private ResourceResolver getResolver() {
		ResourceResolver resolver = null;
		try {
			resolver = resourceResolverFactory.getServiceResourceResolver(getAuthInfoMap(getClass()));
		} catch (LoginException ex) {
			logger.debug("Cannot get resourceresolver: {}.", ex.getMessage());
		}
		return resolver;
	}

	public static Map<String, Object> getAuthInfoMap(Class<?> serviceClass) {
		Map<String, Object> authInfo = new HashMap<>();
		authInfo.put(ResourceResolverFactory.SUBSERVICE, serviceClass.getName());
		return authInfo;
	}

}
