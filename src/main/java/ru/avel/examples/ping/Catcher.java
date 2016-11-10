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

	public String bind() {
		return bind;
	}

	public void bind(String bind) {
		this.bind = bind;
	}

	public int port() {
		return port;
	}

	public void port(int port) {
		this.port = port;
	}
	
	@Override
	public void start() throws ServiceException {
		check(false, Status.STARTED);
		status(Status.STARTING);

		InetSocketAddress addr = null;
		if (bind() == null) addr = new InetSocketAddress( port() );
		else addr = new InetSocketAddress( bind(), port() );
		
		ServerSocketChannel ch = null;
		try {
			ch = ServerSocketChannel.open().bind( addr );
			register( ch, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			ServiceException se = new ServiceException("Server socket not opened", e); 
			failure( se );
			try {
				if ( ch != null ) ch.close();
			} catch (IOException e1) { }
			throw se;
		}
		
		super.start();
	}

	@Override
	protected void onRead(Context ctx, Message msg) {
		super.onRead(ctx, msg);
		ctx.writeMessage( msg );
	}
}
