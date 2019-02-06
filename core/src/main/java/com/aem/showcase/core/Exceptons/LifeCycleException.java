package com.aem.showcase.core.Exceptons;

public class LifeCycleException extends RuntimeException{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LifeCycleException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
	
	public LifeCycleException(String errorMessage) {
        super(errorMessage);
    }
	
	public LifeCycleException(Throwable err) {
        super(err);
    }

}
