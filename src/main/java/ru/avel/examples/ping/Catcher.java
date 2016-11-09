package ru.avel.examples.ping;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ru.avel.services.ASelectorService;
import ru.avel.services.Context;
import ru.avel.services.Message;
import ru.avel.services.ServiceException;
import ru.avel.services.helpers.Logger;

/**
 * Реализация принимающего сервиса.
 * 
 * Открывает порт слушателя. Все принятые пакеты отправляет обратно.
 * 
 * @author Velikotsky.Andrey
 *
 */

public class Catcher extends ASelectorService {
	
	@Property
	private Integer port;
	
	@Property
	protected String bind;

	@Property
	protected Boolean async = true;

	final static int MIN_POOL = 5;
	final static int MAX_POOL = 100;
	final static long KEEP_ALIVE = 60;
	
	public Catcher(String id, Logger logger, Properties props) {
		super(id, logger, props);
		//applyProperties();
	}

	public String getBind() {
		return bind;
	}

	public void setBind(String bind) {
		this.bind = bind;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	@Override
	@SuppressWarnings("unused")
	public void start() throws ServiceException {
		checkStatus(false, Status.STARTED);
		setStatus(Status.STARTING);

		InetSocketAddress addr = null;
		if (getBind() == null) addr = new InetSocketAddress( getPort() );
		else addr = new InetSocketAddress( getBind(), getPort() );
		
		ServerSocketChannel ch = null;
		Context ctx = null;
		try {
			ch = ServerSocketChannel.open().bind( addr );
			ctx = register( ch, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			ServiceException se = new ServiceException("Server socket not opened", e); 
			setFailure( se );
			
			if ( ctx != null ) ctx.close();
			try {
				if ( ch != null ) ch.close();
			} catch (IOException e1) { }
			
			throw se;
		}
		
		if (async) setExecutor( new ThreadPoolExecutor( MIN_POOL, MAX_POOL, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()) );
		
		super.start();
	}

	@Override
	protected void onConnect(Context ctx) {
		super.onConnect(ctx);
		
	}
	
	@Override
	protected void onRead(Context ctx, Message msg) {
		getLogger().logDebug("Reading message is complete");
		ctx.writeMessage( msg );
	}
	
	@Override
	protected void onWrite(Context ctx, Message msg) {
		getLogger().logDebug("Writing message is complete");
		
		msg.clear();
		ctx.readMessage( msg );
		
	}

	
	
}
