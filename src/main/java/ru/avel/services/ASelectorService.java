package ru.avel.services;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Properties;

import ru.avel.services.helpers.Logger;

public class ASelectorService extends AbstractService {

	private Selector selector;

	@Property
	private Long timeout = 1000L;
	
	private MessagePool messages = new MessagePool(); 

	private volatile boolean selecting;
	
	public ASelectorService(String id, Logger logger, Properties props) {
		super(id, logger, props);
	}
	
	@Override
	public void process() throws ServiceException {
		int s = 0;
		selecting = true;
		try {
			s = selector().select( timeout() );
		} catch (IOException e) {
			throw new ServiceException("Selecting failure ", e);
		} finally {
			selecting = false;
		}
		if ( s == 0 ) return;
		logger().logDebug("Selecting keys: " + s);
		
		Iterator<SelectionKey> it = selector.selectedKeys().iterator();
		while ( it.hasNext() ) {
			SelectionKey key = it.next();
			Context ctx = (Context) key.attachment();
			if ( ctx != null ) ctx.process( executor() ); 
			it.remove();
		}
	}
	
	@Override
	public void stop() throws ServiceException {
		status(Status.STOPPING);
		
		if ( selector != null ) {
			for ( SelectionKey key: selector.keys() ) {
				key.cancel();
				Context ctx = (Context) key.attachment();
				if (ctx != null ) ctx.process( executor() );
			}
		}
		
		super.stop();
		
		try {
			if ( selector != null ) selector.close();
		} catch (IOException e) { } 
		selector = null;
	}
	
	public Context register(SelectableChannel channel, int ops) throws IOException {
		channel.configureBlocking(false);
		SelectionKey key = channel.register( selector(), ops );
		Object obj = key.attachment();
		Context ctx = obj instanceof Context ? (Context) obj : new Context( this, key );   
		return ctx;
	}

	public boolean isSelecting() {
		return selecting;
	}

	public void nowSelect() {
		if ( selector != null && selecting ) selector.wakeup();
	}

	public Selector selector() throws IOException {
		if ( selector == null ) selector = Selector.open();
		return selector;
	}
	
	public long timeout() {
		return timeout;
	}
	
	public void timeout(long value) {
		timeout = value;
	}
	
	public MessagePool messages() {
		return messages;
	}
	
	protected void onConnect(Context ctx) {
		logger().logDebug("Connection established");
		ctx.readMessage( messages().acquire() );
	}
	
	protected void onClose(Context ctx) {
		logger().logDebug("Connection finished");
		
		messages().release( ctx.readMessage() );
		messages().release( ctx.writeMessage() );
		
	}
	
	protected void onRead(Context ctx, Message msg) {
		if ( logger().isDebug() ) logger().logDebug("Reading message is complete:\n" + msg.toString());
		ctx.readMessage( messages().acquire() );
	}
	
	protected void onWrite(Context ctx, Message msg) {
		if ( logger().isDebug() ) logger().logDebug("Writing message is complete:\n" + msg.toString());
		messages().release( msg );
	}
	
}
