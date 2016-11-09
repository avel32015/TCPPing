package ru.avel.services;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import ru.avel.services.helpers.Logger;

/**
 * Абстрактная служба. 
 * 
 * @author Velikotsky.Andrey
 *
 */

public abstract class AbstractService implements Service {

	/**
	 * Аннотация для отметки полей, доступных для автоматической установки значений из коллекции свойств 
	 * 
	 * @author Velikotsky.Andrey
	 */
	@Target(FIELD)
	@Retention(RUNTIME)
	protected @interface Property { }
	
	private String id = "";
	private Status status;
	private Logger logger;
	private Properties props;
	private Thread thread;
	private ExecutorService executor;
	private Exception cause;
	
	public AbstractService() {
		this(null, null, null);
	}
	
	public AbstractService(String id, Logger logger, Properties props) {
		super();
		setId(id);
		setLogger(logger);
		setStatus(Status.STOPPED);
		setProperties(props);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id == null ? getClass().getSimpleName() : id;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public void setStatus(Status status) {
		if (this.status != status) getLogger().logDebug("Status: " + status);
		this.status = status;
	}
	
	@Override
	public Exception getFailure() {
		return cause;
	}

	@Override
	public void setFailure(Exception cause) {
		if ( (this.cause = cause) == null ) return;
		setStatus(Status.STOPPED);
		getLogger().logError("Service failure", cause);
	}
	

	public Logger getLogger() {
		if (logger == null) logger = new Logger();
		return logger;
	}

	public void setLogger(Logger logger) {
		if (logger != null) this.logger = logger.newLogger("SERVICE: " + getId());
		else this.logger = logger;
	}

	public Properties getProperties() {
		return props;
	}

	public void setProperties(Properties props) {
		this.props = props;
		applyProperties();
	}

	/**
	 * Находит поля своего класса, отмеченные @Property и устанавливает значения свойств с подобными именами  
	 */
	protected void applyProperties() {
		if (props == null) return; 
		String v;
		Constructor<?> c;
		
		for ( Field f : this.getClass().getDeclaredFields() ) {
			if ( f.isAnnotationPresent(Property.class) ) {
				// Получить конструктор класса для типа поля 
				try {
					c = f.getType().getConstructor( String.class );
				} catch (Exception e) {
					continue;
				}
				// Получить значение свойства с подобным именем
				if ( (v = selectProperty( f.getName() )) == null ) continue;
				
				try {
					f.setAccessible(true);
					f.set( this, c.newInstance( v ) );
				} catch (Exception e) {
					continue;
				} finally {
					f.setAccessible(false);
				}
				getLogger().logDebug("Property value: " + f.getName() + "=" + v); 
			}
		}
		
	}
	
	protected String selectProperty(String name) {
		if (props == null || name == null || name.isEmpty()) return null;
		Set<String> names = props.stringPropertyNames();
		String key = null;
		if ( !getId().isEmpty() && names.contains( key = getId() + "." + name ) ) ; 
		else if ( names.contains( key = this.getClass().getName() + "." + name ) ) ; 
		else if ( names.contains( key = name ) ) ; 
		else return null;
		return props.getProperty(key);
	}
	
	protected void checkStatus(boolean valid, Status... statuses) throws ServiceException {
		boolean b = false;
		Status s = getStatus();
		for( int i = 0; i < statuses.length && !b; i++ ) b = s == statuses[i];
		if ( valid ^ b ) throw new ServiceException("Service [" + getId() + "] has invalid status: " + s);
	}
	
	public void start() throws ServiceException {
		checkStatus(false, Status.STARTED);
		setFailure(null);
		setStatus(Status.STARTING);
		
		thread = new ServiceThread();
		
		setStatus(Status.STARTED);
	}

	public void stop() throws ServiceException {
		setStatus(Status.STOPPED);
		
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new ServiceException("Service thread not joined", e);
			}
			thread = null;
		}
	}

	protected abstract void process() throws ServiceException;
	

	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}
	
	
	
	
	
	private class ServiceThread extends Thread {
		
		AbstractService service = AbstractService.this; 
		
		ServiceThread() {
			super();
			setName("ServiceThread-" + service.getId());
			start();
		}

		@Override
		public void run() {
			try {
				while ( !isInterrupted() && service.getStatus() != Status.STOPPED ) {
					if ( service.getStatus() == Status.STARTED ) {
						try {
							service.process();
						} catch (Exception e) {
							setFailure(e);
							return;
						}
					} 
					yield(); 
				}
			} catch(Exception e) {
				getLogger().logError("ServiceThread exception: ", e);
			}
		}
		
	}


}
