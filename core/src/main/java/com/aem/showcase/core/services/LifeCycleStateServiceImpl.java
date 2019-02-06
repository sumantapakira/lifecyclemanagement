package com.aem.showcase.core.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aem.showcase.core.Constants;
import com.aem.showcase.core.Exceptons.LifeCycleException;
import com.aem.showcase.core.api.LifeCycleModel;
import com.aem.showcase.core.api.LifeCycleNode;
import com.aem.showcase.core.api.LifeCycleSession;
import com.aem.showcase.core.api.LifeCycleStateService;
import com.aem.showcase.core.api.StateTransitions;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.Page;

@Component(service = LifeCycleStateService.class, name = "com.aem.showcase.core.services.LifeCycleStateService", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true)
public class LifeCycleStateServiceImpl implements LifeCycleStateService {

	private static final Logger logger = LoggerFactory.getLogger(LifeCycleStateServiceImpl.class);

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Override
	public boolean canPromote(String modelId, String promoteTo, String promoteFrom, ResourceResolver resourceResolver,
			String resourcePath) {
		boolean canPromote = Boolean.FALSE;
		try {
			promoteTo = StringUtils.isNotEmpty(promoteTo) ? promoteTo.trim() : null;
			LifeCycleModel lsmodel = getLifeCycleModel(resourceResolver, modelId);
			logger.debug("PromoteTo is {}", promoteTo);
			logger.debug("promoteFrom is {}", promoteFrom);

			if (StringUtils.isEmpty(promoteFrom)) {
				logger.debug("No Life Cycle States is denfined for {} , assuming it is DRAFT", resourcePath);
				promoteFrom = getNameForFirstNode(lsmodel);
				logger.debug("First Node for model {} is {}", modelId, promoteFrom);
			}

			List<StateTransitions> transitions = lsmodel.getTransitions();
			MultiMap map = new MultiValueMap();

			for (StateTransitions trans : transitions) {
				if (promoteTo == null && trans.getFrom().getName().equals(promoteFrom.trim())) {
					promoteTo = trans.getTo().getName();
					logger.debug("promoteTo is {}", promoteTo);
				}
				map.put(trans.getFrom().getName(), trans.getTo().getName());
			}
			logger.debug("Transition mapping map : {}", map);

			canPromote = doesKeyExists(map, promoteFrom, promoteTo);
			logger.debug("Is promote allowed ? {}", canPromote);

		} catch (Exception e) {
			logger.error("Error : " + e);
		}
		return canPromote;
	}

	@Override
	public void doPromote(String modelId, String promoteTo, String promoteFrom, ResourceResolver resourceResolver,
			String resourcePath) {
		try {
			Resource resource = resourceResolver.getResource(resourcePath);
			LifeCycleModel model = getLifeCycleModel(resourceResolver, modelId);
			List<LifeCycleNode> nodeList = model.getNodes();

			PropertiesHolder properties = null;

			if (StringUtils.isEmpty(promoteTo)) {
				List<StateTransitions> transitions = model.getTransitions();
				for (StateTransitions trans : transitions) {
					if (!trans.getRule().equals(Constants.SUSPEND_RULE)
							&& trans.getFrom().getName().trim().equals(promoteFrom.trim())) {
						promoteTo = trans.getTo().getName();
						logger.debug("promoteTo {}", promoteTo);
					}
				}
			}
			StateProperty stateProperty = getLifeCycleStateProperites(nodeList, promoteTo);

			if (DamUtil.isAsset(resource)) {
				Resource metadataResource = getDAMMeataDataResource(resourceResolver, resource.getPath());
				if (metadataResource == null) {
					logger.error("Could not find the metadata node for Asset ");
					throw new RuntimeException("Could not find the metadata node for Asset ");
				}
				properties = getPropertiesObject(modelId, stateProperty.getTitle(), stateProperty.getColor(),
						promoteTo);
				updateProperties(metadataResource, properties);

			} else {
				logger.debug("Resource is not an asset ");
				Page page = resource.adaptTo(Page.class);
				if (page != null) {
					Resource pageResource = page.getContentResource();
					properties = getPropertiesObject(modelId, stateProperty.getTitle(), stateProperty.getColor(),
							promoteTo);
					updateProperties(pageResource, properties);
				} else {
					logger.debug("Resource is not a page ");
				}
			}

		} catch (Exception e) {
			logger.error("Error : " + e);
		}

	}

	@Override
	public String[] getFirstandLastElementOfLifeCycle(String modelid, ResourceResolver resolver) {
		LifeCycleModel lsmodel = getLifeCycleModel(resolver, modelid);
		List<LifeCycleNode> list = lsmodel.getNodes();
		String firstElement = StringUtils.EMPTY;
		String lastElement = StringUtils.EMPTY;

		for (int index = 0; index < list.size(); index++) {
			if (index == 0) {
				firstElement = list.get(index).getName();
			}

			if (list.get(index).getType().equals(Constants.NORMAL_RULE)) {
				lastElement = list.get(index).getName();
			}
		}

		return new String[] { firstElement, lastElement };
	}

	@Override
	public LifeCycleNode[] getFirstandLastElementOfLifeCycle(LifeCycleModel lsmodel, ResourceResolver resolver) {
		List<LifeCycleNode> list = lsmodel.getNodes();
		LifeCycleNode firstElementLifeCycleNode = null;
		LifeCycleNode lastElementLifeCycleNode = null;

		for (int index = 0; index < list.size(); index++) {
			if (index == 0) {
				firstElementLifeCycleNode = list.get(index);
			}

			if (list.get(index).getType().equals(Constants.NORMAL_RULE)) {
				lastElementLifeCycleNode = list.get(index);
			}
		}

		return new LifeCycleNode[] { firstElementLifeCycleNode, lastElementLifeCycleNode };
	}

	@Override
	public String getCurrentLifeCycleState(ResourceResolver resourceResolver, String path) {
		String currentLcState = StringUtils.EMPTY;
		Resource resource = resourceResolver.getResource(path);
		if (DamUtil.isAsset(resource)) {
			Resource metadataResource = getDAMMeataDataResource(resourceResolver, path);
			ValueMap map = metadataResource.getValueMap();
			String lifeCycleState = metadataResource != null ? map.get(Constants.PN_LIFECYCLE, String.class)
					: StringUtils.EMPTY;
			return StringUtils.isNotEmpty(lifeCycleState) ? lifeCycleState : StringUtils.EMPTY;

			/*
			 * Asset asset = DamUtil.resolveToAsset(resourceResolver.getResource(path));
			 * return asset != null ? asset.getMetadataValue(Constants.PN_LIFECYCLE) :
			 * StringUtils.EMPTY;
			 */
		} else {
			Page page = resource.adaptTo(Page.class);
			if (page != null) {
				ValueMap value = page.getProperties();
				currentLcState = value.get(Constants.PN_LIFECYCLE, String.class);
			} else {
				logger.debug("Resource is an not page ");
			}
		}
		return currentLcState;
	}

	public static Map<String, Object> getAuthInfoMap(Class<?> serviceClass) {
		Map<String, Object> authInfo = new HashMap<>();
		authInfo.put(ResourceResolverFactory.SUBSERVICE, serviceClass.getName());
		return authInfo;
	}

	@Override
	public void apply(LifeCycleModel model, ResourceResolver resourceResolver, String path) {
		try {
			logger.debug("Inside apply ");
			LifeCycleNode[] allowedStates = getFirstandLastElementOfLifeCycle(model, resourceResolver);
			PropertiesHolder properties = null;
			if (allowedStates != null) {
				properties = getPropertiesObject(model.getId(), allowedStates[0].getTitle(),
						allowedStates[0].getColor(), allowedStates[0].getName());

			}
			Resource resource = resourceResolver.getResource(path);
			if (DamUtil.isAsset(resource)) {
				Resource metadataResource = getDAMMeataDataResource(resourceResolver, resource.getPath());

				if (metadataResource == null) {
					logger.error("Could not find the metadata node for Asset ");
					throw new RuntimeException("Could not find the metadata node for Asset ");
				}

				updateProperties(metadataResource, properties);
				logger.debug("Life cycle applied for the id {} and path {} ", model.getId(),
						metadataResource.getPath());

			} else {
				logger.debug("Resource is not an asset ");
				Page page = resource.adaptTo(Page.class);
				if (page != null) {
					Resource pageResource = page.getContentResource();
					updateProperties(pageResource, properties);
					logger.debug("Life cycle applied for the id {} and path {} ", model.getId(),
							pageResource.getPath());
				} else {
					logger.debug("Resource is not a page ");
				}
			}

		} catch (Exception e) {
			logger.error("Error : ", e);
		}

	}

	@Override
	public String getCurrentLifeCycleId(ResourceResolver resourceResolver, String path) {
		Resource resource = resourceResolver.getResource(path);
		if (DamUtil.isAsset(resource)) {
			logger.debug("Resource is an asset ");
			Resource metadataResource = getDAMMeataDataResource(resourceResolver, path);
			ValueMap map = metadataResource.getValueMap();
			String lifeCycleId = metadataResource != null ? map.get(Constants.PN_LIFECYCLE_ID, String.class)
					: StringUtils.EMPTY;
			return StringUtils.isNotEmpty(lifeCycleId) ? lifeCycleId : StringUtils.EMPTY;

		} else {
			logger.debug("Resource is not an asset ");
			Page page = resource.adaptTo(Page.class);
			if (page != null) {
				ValueMap value = page.getProperties();
				String currentLcID = value.get(Constants.PN_LIFECYCLE_ID, String.class);
				return currentLcID != null ? currentLcID : StringUtils.EMPTY;
			}
		}
		return null;

	}

	private void updateProperties(Resource resource, PropertiesHolder properties)
			throws AccessDeniedException, PersistenceException {
		final ModifiableValueMap valueMap = resource.adaptTo(ModifiableValueMap.class);
		valueMap.remove(Constants.PN_LIFECYCLE);
		valueMap.remove(Constants.PN_LIFECYCLE_ID);
		valueMap.remove(Constants.PN_STATE_TITLE);
		valueMap.remove(Constants.PN_COLOR);

		valueMap.put(Constants.PN_LIFECYCLE_ID, properties.getModelId());
		valueMap.put(Constants.PN_STATE_TITLE, properties.getTitle());
		valueMap.put(Constants.PN_COLOR, properties.getColor());
		valueMap.put(Constants.PN_LIFECYCLE, properties.getName());
		resource.getResourceResolver().commit();
	}

	private PropertiesHolder getPropertiesObject(String modelid, String title, String color, String name) {
		PropertiesHolder properties = new PropertiesHolder();
		properties.setModelId(modelid);
		properties.setLifeCycleStatus(name);
		properties.setColor(color);
		properties.setName(name);
		properties.setTitle(title);

		return properties;
	}

	private String getNameForFirstNode(LifeCycleModel model) {
		String firstNodeName = StringUtils.EMPTY;
		Iterator<LifeCycleNode> itr = model.getNodes().iterator();
		while (itr.hasNext()) {
			firstNodeName = itr.next().getName();
			break;
		}
		return firstNodeName;
	}

	private class StateProperty {
		private String color;
		private String title;

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

	}

	private StateProperty getLifeCycleStateProperites(List<LifeCycleNode> nodeList, String stateName) {
		StateProperty stateProperty = new StateProperty();
		for (LifeCycleNode ln : nodeList) {
			if (ln.getName().trim().toLowerCase().equals(stateName.trim().toLowerCase())) {
				stateProperty.setColor(ln.getColor());
				stateProperty.setTitle(ln.getTitle());
			}
		}
		return stateProperty;
	}

	private class PropertiesHolder {
		private String modelId;
		private String lifeCycleStatus;
		private String color;
		private String name;
		private String title;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getModelId() {
			return modelId;
		}

		public void setModelId(String modelId) {
			this.modelId = modelId;
		}

		public String getLifeCycleStatus() {
			return lifeCycleStatus;
		}

		public void setLifeCycleStatus(String lifeCycleStatus) {
			this.lifeCycleStatus = lifeCycleStatus;
		}

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@Override
	public boolean canDemote(String modelId, String demoteTo, String demoteFrom, ResourceResolver resourceResolver,
			String resourcePath) {
		boolean canDemote = Boolean.FALSE;
		try {
			demoteTo = StringUtils.isNotEmpty(demoteTo) ? demoteTo.trim() : null;
			LifeCycleModel lsmodel = getLifeCycleModel(resourceResolver, modelId);
			logger.debug("demoteTo is {}", demoteTo);
			logger.debug("demoteFrom is {}", demoteFrom);

			if (StringUtils.isEmpty(demoteFrom)) {
				return false;
			}
			List<StateTransitions> transitions = lsmodel.getTransitions();
			MultiMap map = new MultiValueMap();
			for (StateTransitions trans : transitions) {
				if (demoteTo == null && trans.getTo().getName().equals(demoteFrom)) {
					demoteTo = trans.getFrom().getName();
				}
				map.put(trans.getTo().getName(), trans.getFrom().getName());
			}
			logger.debug("demoteTo is: {}", demoteTo);
			logger.debug("Transition mapping map : {}", map);
			canDemote = doesKeyExists(map, demoteFrom, demoteTo);
			logger.debug("Is Demote allowed ? {}", canDemote);

		} catch (Exception e) {
			logger.error("Error : " + e);
		}
		return canDemote;
	}

	@Override
	public void doDemote(String modelId, String demoteTo, String demoteFrom, ResourceResolver resourceResolver,
			String resourcePath) throws LifeCycleException {
		try {
			logger.debug("Inside demoteTo {} and demoteFrom {}", demoteTo, demoteFrom);
			Resource resource = resourceResolver.getResource(resourcePath);
			LifeCycleModel model = getLifeCycleModel(resourceResolver, modelId);
			List<LifeCycleNode> nodeList = model.getNodes();
			PropertiesHolder properties = null;

			if (StringUtils.isEmpty(demoteTo)) {
				List<StateTransitions> transitions = model.getTransitions();
				for (StateTransitions trans : transitions) {
					if (!trans.getRule().equals(Constants.SUSPEND_RULE)
							&& trans.getTo().getName().trim().equals(demoteFrom.trim())) {
						demoteTo = trans.getFrom().getName();
						logger.debug("demoteTo {}", demoteTo);
					}
				}
			}
			if (StringUtils.isEmpty(demoteTo)) {
				throw new LifeCycleException("Demote to cannot be blank.");
			}
			StateProperty stateProperty = getLifeCycleStateProperites(nodeList, demoteTo);

			if (DamUtil.isAsset(resource)) {
				Resource metadataResource = getDAMMeataDataResource(resourceResolver, resource.getPath());
				if (metadataResource == null) {
					logger.error("Could not find the metadata node for Asset ");
					throw new RuntimeException("Could not find the metadata node for Asset ");
				}
				properties = getPropertiesObject(modelId, stateProperty.getTitle(), stateProperty.getColor(), demoteTo);
				updateProperties(metadataResource, properties);
			} else {
				logger.debug("Resource is not an asset ");
				Page page = resource.adaptTo(Page.class);
				if (page != null) {
					Resource pageResource = page.getContentResource();
					properties = getPropertiesObject(modelId, stateProperty.getTitle(), stateProperty.getColor(),
							demoteTo);
					updateProperties(pageResource, properties);

				} else {
					logger.debug("Resource is not a page ");
				}
			}

		} catch (Exception e) {
			logger.error("Error : " + e);
			throw new LifeCycleException(e);
		}

	}

	@Override
	public boolean canSuspend(String model, String suspendTo, String suspendFrom, ResourceResolver resolver,
			String resourcePath) {

		boolean canSuspend = Boolean.FALSE;
		try {
			suspendTo = StringUtils.isNotEmpty(suspendTo) ? suspendTo.trim() : StringUtils.EMPTY;
			LifeCycleModel lsmodel = getLifeCycleModel(resolver, model);
			logger.debug("Suspend To is {}", suspendTo);
			logger.debug("Suspend From is {}", suspendFrom);

			List<StateTransitions> transitions = lsmodel.getTransitions();
			MultiMap map = new MultiValueMap();

			for (StateTransitions trans : transitions) {
				if (trans.getRule().equals(Constants.SUSPEND_RULE) && trans.getFrom().getName().equals(suspendFrom)) {
					suspendTo = trans.getTo().getName();
				}
				map.put(trans.getFrom().getName(), trans.getTo().getName());
			}
			logger.debug("Transition mapping map : {}", map);

			canSuspend = doesKeyExists(map, suspendFrom, suspendTo);
			logger.debug("Is Suspend allowed ? {}", canSuspend);

		} catch (Exception e) {
			logger.error("Error : " + e);
		}
		return canSuspend;

	}

	private boolean doesKeyExists(MultiMap map, String key, String value) {
		boolean isKeyFound = Boolean.FALSE;
		key = key.trim();
		if (map.get(key) != null) {
			String[] arr = map.get(key).toString().split(",");
			for (int i = 0; i < arr.length; i++) {
				arr[i] = arr[i].replace("[", "");
				arr[i] = arr[i].replace("]", "");
				arr[i] = arr[i].replace(" ", "");
			}
			List<String> result = Arrays.asList(arr);
			if (result.contains(value)) {
				isKeyFound = Boolean.TRUE;
			}
		} else {
			logger.debug("Key not found {} ", key);
		}
		return isKeyFound;
	}

	@Override
	public void doSuspend(String modelId, String suspendTo, String suspendFrom, ResourceResolver resourceResolver,
			String resourcePath) throws LifeCycleException {

		try {
			logger.debug("Inside suspendTo {} and suspendFrom {}", suspendTo, suspendFrom);
			Resource resource = resourceResolver.getResource(resourcePath);
			LifeCycleModel model = getLifeCycleModel(resourceResolver, modelId);
			List<LifeCycleNode> nodeList = model.getNodes();
			String color = "";
			String title = "";
			PropertiesHolder properties = null;

			if (StringUtils.isEmpty(suspendTo)) {
				List<StateTransitions> transitions = model.getTransitions();
				for (StateTransitions trans : transitions) {
					if (trans.getRule().equals(Constants.SUSPEND_RULE)
							&& trans.getFrom().getName().trim().equals(suspendFrom.trim())) {
						suspendTo = trans.getTo().getName();
						logger.debug("suspendTo {}", suspendTo);
					}
				}
			}

			for (LifeCycleNode ln : nodeList) {
				if (ln.getName().trim().toLowerCase().equals(suspendTo.trim().toLowerCase())) {
					color = ln.getColor();
					title = ln.getTitle();
					logger.debug("Match found suspendTo LifeCycleNode node name is {} and color {}", ln.getName(),
							ln.getColor());
				}
			}

			if (DamUtil.isAsset(resource)) {
				Resource metadataResource = getDAMMeataDataResource(resourceResolver, resource.getPath());
				if (metadataResource == null) {
					logger.error("Could not find the metadata node for Asset ");
					throw new RuntimeException("Could not find the metadata node for Asset ");
				}
				properties = getPropertiesObject(modelId, title, color, suspendTo);
				updateProperties(metadataResource, properties);

			} else {
				logger.debug("Resource is not an asset ");
				Page page = resource.adaptTo(Page.class);
				if (page != null) {
					Resource pageResource = page.getContentResource();
					properties = getPropertiesObject(modelId, title, color, suspendTo);
					updateProperties(pageResource, properties);

				} else {
					logger.debug("Resource is not a page ");
				}
			}

		} catch (Exception e) {
			logger.error("Error : " + e);
			throw new LifeCycleException(e);
		}

	}

	@Override
	public boolean canResume(String modelId, String resumeTo, String resumeFrom, ResourceResolver resolver,
			String resourcePath) {
		return canDemote(modelId, resumeTo, resumeFrom, resolver, resourcePath);
	}

	@Override
	public void doResume(String modelId, String resumeTo, String resumeFrom, ResourceResolver resourceResolver,
			String resourcePath) throws LifeCycleException {

		try {
			logger.debug("Inside resumeTo {} and resumeFrom {}", resumeTo, resumeFrom);
			Resource resource = resourceResolver.getResource(resourcePath);
			LifeCycleModel model = getLifeCycleModel(resourceResolver, modelId);
			List<LifeCycleNode> nodeList = model.getNodes();
			PropertiesHolder properties = null;

			if (StringUtils.isEmpty(resumeTo)) {
				List<StateTransitions> transitions = model.getTransitions();
				for (StateTransitions trans : transitions) {
					if (trans.getRule().equals(Constants.SUSPEND_RULE)
							&& trans.getTo().getName().trim().equals(resumeFrom.trim())) {
						resumeTo = trans.getFrom().getName();
						logger.debug("resumeTo {}", resumeTo);
					}
				}
			}
			if (StringUtils.isEmpty(resumeTo)) {
				throw new LifeCycleException("Resume To cannot be blank.");
			}

			StateProperty stateProperty = getLifeCycleStateProperites(nodeList, resumeTo);

			if (DamUtil.isAsset(resource)) {
				Resource metadataResource = getDAMMeataDataResource(resourceResolver, resource.getPath());
				if (metadataResource == null) {
					logger.error("Could not find the metadata node for Asset ");
					throw new RuntimeException("Could not find the metadata node for Asset ");
				}
				properties = getPropertiesObject(modelId, stateProperty.getTitle(), stateProperty.getColor(), resumeTo);
				updateProperties(metadataResource, properties);

			} else {
				logger.debug("Resource is not an asset ");
				Page page = resource.adaptTo(Page.class);
				if (page != null) {
					Resource pageResource = page.getContentResource();
					properties = getPropertiesObject(modelId, stateProperty.getTitle(), stateProperty.getColor(),
							resumeTo);
					updateProperties(pageResource, properties);

				} else {
					logger.debug("Resource is not a page ");
				}
			}

		} catch (Exception e) {
			logger.error("Error : " + e);
			throw new LifeCycleException(e);
		}

	}

	private Resource getDAMMeataDataResource(ResourceResolver resourceResolver, String resourcePath) {
		Asset asset = DamUtil.resolveToAsset(resourceResolver.getResource(resourcePath));
		String metadataPath = String.format("%s/%s/%s", asset.getPath(), JcrConstants.JCR_CONTENT,
				DamConstants.METADATA_FOLDER);
		return resourceResolver.getResource(metadataPath);
	}

	@Override
	public LifeCycleModel getLifeCycleModel(ResourceResolver resourceResolver, String modelId) {
		LifeCycleSession lifeCycleSession = (LifeCycleSession) resourceResolver.adaptTo(LifeCycleSession.class);
		LifeCycleModel model = lifeCycleSession.getModel(modelId);
		return model;
	}
}
