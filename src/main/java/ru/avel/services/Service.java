package ru.avel.services;

public interface Service {
	enum Status { STOPPED, STARTING, STARTED, STOPPING }

	Status getStatus();
	void setStatus(Status status);
	
	Exception getFailure();
	void setFailure(Exception cause);
	
	void start() throws ServiceException;
	void stop() throws ServiceException;

}
