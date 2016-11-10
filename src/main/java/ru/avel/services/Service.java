package ru.avel.services;

public interface Service {
	enum Status { STOPPED, STARTING, STARTED, STOPPING }

	Status status();
	void status(Status status);
	
	Exception failure();
	void failure(Exception cause);
	
	void start() throws ServiceException;
	void stop() throws ServiceException;

}
