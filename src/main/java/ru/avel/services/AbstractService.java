package ru.avel.services;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
	
	@Property
	private Integer TASK_DELAY = 10;
	
	private String id = "";
	private Status status;
	private Logger logger;
	private Properties props;
	private Exception cause;
	private ScheduledExecutorService executor;

	private ServiceTask task = new ServiceTask(); 
	
	public AbstractService() {
		this(null, null, null);
	}
	
	public AbstractService(String id, Logger logger, Properties props) {
		super();
		id(id);
		logger(logger);
		status(Status.STOPPED);
		properties(props);
	}

	public String id() {
		return id;
	}

	public void id(String id) {
		this.id = id == null ? getClass().getSimpleName() : id;
	}

	@Override
	public Status status() {
		return status;
	}

	@Override
	public void status(Status status) {
		if (this.status != status) logger().logDebug("Status: " + status);
		this.status = status;
	}
	
	@Override
	public Exception failure() {
		return cause;
	}

	@Override
	public void failure(Exception cause) {
		if ( (this.cause = cause) == null ) return;
		status(Status.STOPPED);
		logger().logError("Service failure", cause);
	}
	

	public Logger logger() {
		if (logger == null) logger = new Logger();
		return logger;
	}

	public void logger(Logger logger) {
		if (logger != null) this.logger = logger.newLogger("SERVICE: " + id());
		else this.logger = logger;
	}

	public Properties properties() {
		return props;
	}

	public void properties(Properties props) {
		this.props = props;
		applyProperties();
	}

	/**
	 * Находит поля своего класса, отмеченные @Property и устанавливает значения свойств с подобными именами  
	 */
	protected void applyProperties() {
		if (props == null) return; 
		String v;
		Method m;
		
		for ( Field f : this.getClass().getDeclaredFields() ) {
			if ( f.isAnnotationPresent(Property.class) ) {
				Class<?> t = f.getType();
				// Получить конструктор класса для типа поля 
				try {
					m = t.getMethod("valueOf", t.equals( String.class ) ? Object.class : String.class );
				} catch (Exception e) {
					continue;
				}
				// Получить значение свойства с подобным именем
				if ( (v = selectProperty( f.getName() )) == null ) continue;
				
				try {
					f.setAccessible(true);
					f.set( this, m.invoke( t, v ) );
				} catch (Exception e) {
					continue;
				} finally {
					f.setAccessible(false);
				}
				logger().logDebug("Property value: " + f.getName() + "=" + v); 
			}
		}
		
	}
	
	protected String selectProperty(String name) {
		if (props == null || name == null || name.isEmpty()) return null;
		Set<String> names = props.stringPropertyNames();
		String key = null;
		if ( !id().isEmpty() && names.contains( key = id() + "." + name ) ) ; 
		else if ( names.contains( key = this.getClass().getName() + "." + name ) ) ; 
		else if ( names.contains( key = name ) ) ; 
		else return null;
		return props.getProperty(key);
	}
	
	protected void check(boolean valid, Status... statuses) throws ServiceException {
		boolean b = false;
		Status s = status();
		for( int i = 0; i < statuses.length && !b; i++ ) b = s == statuses[i];
		if ( valid ^ b ) throw new ServiceException("Service [" + id() + "] has invalid status: " + s);
	}
	
	@Override
	public void start() throws ServiceException {
		check(false, Status.STARTED);
		failure(null);
		status(Status.STARTING);
		task.start();
		status(Status.STARTED);
	}

	@Override
	public void stop() throws ServiceException {
		status(Status.STOPPED);
		task.stop();
	}

	public ScheduledExecutorService executor() {
		return executor;
	}

	public void executor(ScheduledExecutorService executor) {
		this.executor = executor;
	}

	protected abstract void process() throws ServiceException;
	
	private class ServiceTask implements Runnable {
		
		Future<?> future; 
		
		void start() {
			stop();
			future = null;
			if ( executor() == null ) logger.logWarn("Executor is undefined", null);
			else future = executor().scheduleWithFixedDelay( this, TASK_DELAY, TASK_DELAY, TimeUnit.MILLISECONDS);
		}
		
		void stop() {
			if ( future != null ) future.cancel(true);
		}
		
		@Override
		public void run() {
			if ( status() == Status.STOPPED ) future.cancel(false);
			else if ( status() == Status.STARTED ) {
				try {
					process();
				} catch (Exception e) {
					failure(e);
				}
			}
		}
	}

}
