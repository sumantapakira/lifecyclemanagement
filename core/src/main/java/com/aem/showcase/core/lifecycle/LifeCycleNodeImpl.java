package com.aem.showcase.core.lifecycle;

import java.util.ArrayList;
import java.util.List;

import com.aem.showcase.core.api.LifeCycleNode;
import com.aem.showcase.core.api.StateTransitions;



public class LifeCycleNodeImpl implements LifeCycleNode{
	
	  private String id;
	  private String type;
	  private String title;
	  private String description;
	  private String color;
	  private String name;
	  private List<StateTransitions> transitions = new ArrayList<StateTransitions>();
	  private List<StateTransitions> incoming = new ArrayList<StateTransitions>();
	  
	  public LifeCycleNodeImpl() {}
	
	public LifeCycleNodeImpl(String id, String title, String type, String description,String color,String name)
	  {
	    this.id = id;
	    this.title = title;
	    this.type = type;
	    this.description = description;
	    this.color = color;
	    this.name = name;
	  }
	

	@Override
	public String getId() {
		return this.id ;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String paramString) {
		this.type = paramString;
		
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public void setDescription(String paramString) {
		this.description = paramString;
		
	}

	@Override
	public List<StateTransitions> getTransitions() {
		return this.transitions;
	}

	@Override
	public List<StateTransitions> getIncomingTransitions() {
		return this.incoming;
	}

	@Override
	public String getTitle() {
		return this.title;
	}

	@Override
	public void setTitle(String paramString) {
		this.title = paramString;
	}

	@Override
	public void setId(String id) {
		this.id = id;
		
	}

	@Override
	public void setColor(String color) {
		this.color = color;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getColor() {
		return this.color;
	}

	@Override
	public String getName() {
		return this.name;
	}

}
