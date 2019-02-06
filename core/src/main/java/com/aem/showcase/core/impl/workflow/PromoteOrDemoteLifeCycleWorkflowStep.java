package com.aem.showcase.core.impl.workflow;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.taskmanagement.Task;
import com.adobe.granite.taskmanagement.TaskManager;
import com.aem.showcase.core.api.LifeCycleStateService;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

@Component(name = "Promote or Demote state of an Asset or Page", service = WorkflowProcess.class, immediate = true, property = {
		"service.description=Promote or Demote state of an Asset or Page.",
		"process.label=Promote or Demote state of an Asset or Page." })
public class PromoteOrDemoteLifeCycleWorkflowStep implements WorkflowProcess {

	private static final Logger logger = LoggerFactory.getLogger(PromoteOrDemoteLifeCycleWorkflowStep.class);
	private static final String TYPE_JCR_PATH = "JCR_PATH";

	@Reference
	private LifeCycleStateService lifeCycleStateService;

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Override
	public void execute(WorkItem workItem, WorkflowSession wfSession, MetaDataMap metaDataMap)
			throws WorkflowException {
		ResourceResolver resourceResolver = null;
		try {
			String lifeCycleModelId = metaDataMap.get("lifeCycleModel", String.class);
			String lifeCycleModelStatePromoteTo = metaDataMap.get("lifeCycleStates", String.class);
			String wfPayload = (String) workItem.getWorkflowData().getPayload();

			resourceResolver = this.getResourceResolver(wfSession.getSession());

			if (StringUtils.isNoneBlank(wfPayload)) {
				String currentLifeCycleStatePromoteFrom = lifeCycleStateService
						.getCurrentLifeCycleState(resourceResolver, wfPayload);
				logger.debug("Current Life Cycle Status for the {} is {}", wfPayload, currentLifeCycleStatePromoteFrom);
				if (lifeCycleStateService.canPromote(lifeCycleModelId, lifeCycleModelStatePromoteTo,
						currentLifeCycleStatePromoteFrom, resourceResolver, wfPayload)) {
					logger.debug("Life cycle promotion allowed for {}, going to perform promote...", wfPayload);
					lifeCycleStateService.doPromote(lifeCycleModelId, lifeCycleModelStatePromoteTo, StringUtils.EMPTY,
							resourceResolver, wfPayload);
				} else {
					logger.debug("Life cycle promotion not allowed for {}", wfPayload);
					String initiator = workItem.getWorkflow().getInitiator();
					logger.debug("Workflow Initiator is {}", initiator);

					TaskManager taskManager = resourceResolver.adaptTo(TaskManager.class);
					Task newTask = taskManager.getTaskManagerFactory().newTask("Notification");

					newTask.setName("Life cycle promotion not allowed");
					newTask.setContentPath(wfPayload);
					newTask.setDescription("Life cycle promotion not allowed due to the rule defined.");
					newTask.setCurrentAssignee(initiator);

					taskManager.createTask(newTask);
				}

			}
		} catch (Exception e) {
			logger.error("Error: " + e);
		} finally {
			if (resourceResolver != null) {
				resourceResolver.close();
			}
		}

	}

	private ResourceResolver getResourceResolver(Session session) throws LoginException {
		final Map<String, Object> authInfo = new HashMap<String, Object>();
		authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session);
		return resourceResolverFactory.getResourceResolver(authInfo);
	}

}
