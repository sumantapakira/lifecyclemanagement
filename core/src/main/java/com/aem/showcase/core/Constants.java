package com.aem.showcase.core;

public class Constants {
	
	
	public static final String PN_LIFECYCLE ="status";
	public static final String PN_LIFECYCLE_ID ="lifeCycleModelId";
	public static final String PN_COLOR ="color";
	public static final String PN_STATE_TITLE ="lifecyclestatetitle";
	
	public static final String RT_LIFE_CYCLE_STEP= "cq/lifecycle/flow/components/lifecyclestep";
	public static final String SUSPEND_RULE ="suspend";
	public static final String NORMAL_RULE ="normal";
	
	public static final String RESPONCE_CODE_PROMOTE_201 = "The pages have been promoted.";
	public static final String RESPONCE_CODE_202 = "The action cannot be performed because no life cycle is attached.";
	public static final String RESPONCE_CODE_PROMOTE_203 = "The page cannot be promoted because it is End state.\\n Create new version and then apply Life cycle.";
	public static final String RESPONCE_CODE_PROMOTE_204 = "The page cannot be promoted because the state is blank.";
	
	public static final String RESPONCE_CODE_DEMOTE_201 = "The pages have been demoted.";
	public static final String RESPONCE_CODE_DEMOTE_203 = "The page cannot be demoted because it is First state of the life cycle attached to it.";
	public static final String RESPONCE_CODE_DEMOTE_204 = "The page cannot be demoted because the state is blank.";
	
	public static final String RESPONCE_CODE_SUSPEND_204 = "The page cannot be suspended because the state is blank.";
	public static final String RESPONCE_CODE_SUSPEND_201 = "The pages have been suspended.";
	public static final String RESPONCE_CODE_SUSPEND_203 = "The page cannot be suspended because it is Not the First state of the life cycle attached to it.";
	
	public static final String RESPONCE_CODE_RESUME_204 = "The page cannot be resumed because the state is blank.";
	public static final String RESPONCE_CODE_RESUME_201 = "The pages have been resumed.";
	public static final String RESPONCE_CODE_RESUME_203 = "The page cannot be resumed because it is Not the First state of the life cycle attached to it.";

}
