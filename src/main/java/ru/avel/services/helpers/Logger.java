package ru.avel.services.helpers;

public class Logger {

	private org.slf4j.Logger logger = null;
	
	private String subject = "SUBJ";

	public Logger() {
		this("", null);
	}
	
	public Logger(org.slf4j.Logger logger, String subj) {
		this.logger = logger != null ? logger : loggerFactory(null);
		this.subject = subj;
	}

	public Logger(String name, String subj) {
		this(loggerFactory(name), subj);
	}
	
	private static org.slf4j.Logger loggerFactory(String name) {
		if (name == null || name.isEmpty()) name = org.slf4j.Logger.ROOT_LOGGER_NAME;
		return org.slf4j.LoggerFactory.getLogger(name);
	}
	
	public Logger newLogger(String subj) {
		return new Logger(logger, subj);
	}
	
	public org.slf4j.Logger getLogger() {
		return this.logger;
	}

	public void logInfo(String message) {
		logInfo(message, null);
	}
	
	public void logInfo(String message, Exception e) {
		if (e == null) getLogger().info(formatMessage(message, subject));
		else getLogger().info(formatMessage(message, subject), e);
	}

	public void logWarn(String message, Exception e) {
		if (e == null) getLogger().warn(formatMessage(message, subject));
		else getLogger().warn(formatMessage(message, subject), e);
	}
	
	public void logDebug(String message) {
		logDebug(message, null);
	}

	public void logDebug(String message, Exception e) {
		if (e == null) getLogger().debug(formatMessage(message, subject));
		else getLogger().debug(formatMessage(message, subject), e);
	}

	public void logError(String message, Exception e) {
		if (e == null) getLogger().error(formatMessage(message, subject));
		else getLogger().error(formatMessage(message, subject), e);
	}

	private String formatMessage(String message, String subj) {
		return subj == null ? message : subj + ": " + message;
	}
	
	public boolean isDebug() {
		return getLogger().isDebugEnabled();
	}
	
}
