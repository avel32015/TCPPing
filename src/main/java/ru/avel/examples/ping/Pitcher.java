package ru.avel.examples.ping;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ru.avel.services.ASelectorService;
import ru.avel.services.Context;
import ru.avel.services.Message;
import ru.avel.services.ServiceException;
import ru.avel.services.helpers.Logger;

/**
 * Реализация отправляющего сервиса.
 * 
 * Соединяется с принимающим сервисом. Циклически отправляет пакеты и принимает ответ.
 * 
 * @author Velikotsky.Andrey
 *
 */

public class Pitcher extends ASelectorService {
	
	@Property
	private String hostname = null;
	
	@Property
	private int port = 0;

	@Property
	private float mps = 0;
	
	@Property
	private int size = 0;

	@Property
	private int num = 0;
	
	private SocketChannel channel;
	private Context context;
	
	private SenderTask sender = new SenderTask();
	
	
	public Pitcher(String id, Logger logger, Properties props) {
		super(id, logger, props);
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public float getMps() {
		return mps;
	}

	public void setMps(float mps) {
		this.mps = mps;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setChannel(SocketChannel channel) {
		try {
			if (this.channel != null) this.channel.close();
		} catch (IOException e) { } 
		this.channel = channel;
	}
	
	@Override
	public void start() throws ServiceException {
		checkStatus(false, Status.STARTED);
		setStatus(Status.STARTING);

		InetSocketAddress addr = new InetSocketAddress( getHostname(), getPort() );
		try {
			setChannel( SocketChannel.open() );
			context = register(channel, SelectionKey.OP_CONNECT);
			channel.connect( addr );
		} catch (IOException e) {
			ServiceException se = new ServiceException("Server socket not opened", e); 
			setFailure( se );
			setChannel(null);
			throw se;
		}
		super.start();
	}

	@Override
	public void stop() throws ServiceException {
		sender.stop();
		try {
			super.stop();
		} finally {
			setChannel(null);
		}
	}
	
	@Override
	protected void onConnect(Context ctx) {
		super.onConnect(ctx);
		
		// Установлено подключение, начинаем отправлять 
		sender.start();
	}
	
	@Override
	protected void onRead(Context ctx, Message msg) {
		// Сообщение вернулось
		
		
		super.onRead(ctx, msg);
	}
	
	@Override
	protected void onWrite(Context ctx, Message msg) {
		getLogger().logDebug("Writing message is complete");
		
		
	}

	private Message acquireMessage() {
		//return getProvider() == null ? null : getProvider().create();
		return new PingMessage( size );
	}
	
	private class SenderTask implements Runnable {
		
		Future<?> future;
		int count;
		
		void start() {
			stop();
			future = null;
			count = 0;
			long period = mps == 0 ? 1000 : Math.round( 1000 / mps );
			if ( getExecutor() == null ) getLogger().logWarn("Executor is undefined", null);
			else future = getExecutor().scheduleAtFixedRate( this, 0, period, TimeUnit.MILLISECONDS);
		}
		
		void stop() {
			if ( future != null ) future.cancel(true);
		}
		
		@Override
		public void run() {
			Message msg = null;
			if ( getStatus() == Status.STOPPED ) future.cancel(false);
			else if ( getStatus() == Status.STARTED ) {
				if ( ( count++ < num || num == 0 ) && (msg = acquireMessage()) != null ) context.writeMessage( msg );
				if ( 0 < num && num <= count ) {
					future.cancel(false);
				}
			}
		}
	}
	

}
