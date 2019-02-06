package com.aem.showcase.core.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.aem.showcase.core.Constants;
import com.aem.showcase.core.api.LifeCycleModel;
import com.aem.showcase.core.api.LifeCycleStateService;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageInfoProvider;

@Component(service = PageInfoProvider.class, name = "com.aem.showcase.core.impl.ResourceInfoProvider", immediate = true, property = {
		"pageInfoProviderType=sites.listView.info.provider.lifecycleTitle" })

public class ResourceInfoProvider implements PageInfoProvider {

	public static final String PROVIDER_TYPE = "lifecycleTitle";

	@Reference
	LifeCycleStateService lifeCycleStateService;

	@Override
	public void updatePageInfo(SlingHttpServletRequest request, JSONObject info, Resource resource)
			throws JSONException {
		Page page = resource.adaptTo(Page.class);
		JSONObject lifecycleInfo = new JSONObject();
		LifeCycleModel lifeCycleModel = null;
		if (page != null) {
			ValueMap vm = page.getContentResource().adaptTo(ValueMap.class);
			String lifeCyleId = vm.get(Constants.PN_LIFECYCLE_ID, String.class);
			if (StringUtils.isNotBlank(lifeCyleId))
				lifeCycleModel = lifeCycleStateService.getLifeCycleModel(request.getResourceResolver(), lifeCyleId);

			lifecycleInfo.put("lifecycleTitle", lifeCycleModel.getTitle());
		}

		info.put(PROVIDER_TYPE, lifecycleInfo);

	}

}
