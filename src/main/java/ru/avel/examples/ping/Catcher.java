package ru.avel.examples.ping;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.Properties;
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
	private String bind;

	public Catcher(String id, Logger logger, Properties props) {
		super(id, logger, props);
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
	public void start() throws ServiceException {
		checkStatus(false, Status.STARTED);
		setStatus(Status.STARTING);

		InetSocketAddress addr = null;
		if (getBind() == null) addr = new InetSocketAddress( getPort() );
		else addr = new InetSocketAddress( getBind(), getPort() );
		
		ServerSocketChannel ch = null;
		try {
			ch = ServerSocketChannel.open().bind( addr );
			register( ch, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			ServiceException se = new ServiceException("Server socket not opened", e); 
			setFailure( se );
			try {
				if ( ch != null ) ch.close();
			} catch (IOException e1) { }
			throw se;
		}
		
		super.start();
	}

	@Override
	protected void onConnect(Context ctx) {
		super.onConnect(ctx);
		ctx.readMessage( new PingMessage() );
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
