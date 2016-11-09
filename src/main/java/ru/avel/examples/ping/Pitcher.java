package ru.avel.examples.ping;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Properties;

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
	
	private Thread sender;
	
	
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
		sender = new Sender();
	}

	@Override
	public void stop() throws ServiceException {
		if ( sender != null ) sender.interrupt();
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
	
	private class Sender extends Thread {
		
		public Sender() {
			super("SenderThread");
		}
		
		@Override
		public void run() {
			getLogger().logDebug("Sender started");
			
			long nextTime = System.currentTimeMillis();
			long deltaTime = mps == 0 ? 1000 : Math.round( 1000 / mps );

			for (int i = 0; num == 0 || i++ < num; ) {
				Message msg = null;
				if ( getStatus() == Status.STARTED && (msg = acquireMessage()) != null ) context.writeMessage( msg );
				if ( isInterrupted() || getStatus() == Status.STOPPED ) break;
				
				long currTime = System.currentTimeMillis();
				if ( (nextTime += deltaTime) < currTime ) nextTime = currTime; else
				try {
					sleep( nextTime - currTime );
				} catch (InterruptedException e) { break; }
			}
			
			getLogger().logDebug("Sender stopped");
		}
		
	}

}
