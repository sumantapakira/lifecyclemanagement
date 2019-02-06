package com.aem.showcase.core.services;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

import com.aem.showcase.core.Constants;
import com.day.cq.commons.jcr.JcrConstants;

@Component(service = SlingPostProcessor.class, immediate = true, name = "com.aem.showcase.core.services.UpdateLifeCycleStatePostProcessor")
public class UpdateLifeCycleStatePostProcessor implements SlingPostProcessor {
	private static final Logger logger = getLogger(UpdateLifeCycleStatePostProcessor.class);

	@Override
	public void process(SlingHttpServletRequest request, List<Modification> modifications) throws Exception {

		if (accepts(request)) {
			final Resource resource = request.getResourceResolver().getResource(request.getResource().getPath());
			ValueMap values = resource.getValueMap();
			String title = values.get(JcrConstants.JCR_TITLE, String.class);

			final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);

			title = title.trim().replaceAll("[^a-zA-Z0-9]", "-").toLowerCase(); // Remove all special characters
			properties.put("name", title);

			modifications.add(Modification.onModified(resource.getPath()));
		}
	}

	protected boolean accepts(SlingHttpServletRequest request) {
		logger.debug("Resource Type is: {}", request.getResource().getResourceType());
		return Constants.RT_LIFE_CYCLE_STEP.equals(request.getResource().getResourceType());
	}

}
