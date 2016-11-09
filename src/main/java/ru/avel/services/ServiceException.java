package ru.avel.services;

public class ServiceException extends Exception {

	/**
	 * 
	 */
	
	private static final long serialVersionUID = 333083733620352712L;

	public ServiceException(String message) {
		super(message);
	}

	public ServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}
