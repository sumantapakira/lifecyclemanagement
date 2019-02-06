package com.aem.showcase.core.api;

import java.util.List;

import com.aem.showcase.core.api.StateTransitions;

public interface LifeCycleNode {

	public static final String TYPE_DRAFT = "DRAFT";
	public static final String TYPE_END = "END";

	public abstract String getId();

	public abstract String getType();

	public abstract void setType(String paramString);

	public abstract String getDescription();

	public abstract void setDescription(String paramString);

	public abstract List<StateTransitions> getTransitions();

	public abstract List<StateTransitions> getIncomingTransitions();

	public abstract String getTitle();

	public abstract void setTitle(String paramString);

	public void setId(String id);

	public abstract void setColor(String color);

	public abstract void setName(String name);

	public abstract String getColor();

	public abstract String getName();

}
