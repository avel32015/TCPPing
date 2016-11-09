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
	private MessageProvider provider; 
	private long timeout = 1000;
	
	private volatile boolean selecting;
	
	public ASelectorService(String id, Logger logger, Properties props) {
		super(id, logger, props);
	}
	
	public Context register(SelectableChannel channel, int ops) throws IOException {
		channel.configureBlocking(false);
		SelectionKey key = channel.register( getSelector(), ops );
		Object obj = key.attachment();
		Context ctx = obj instanceof Context ? (Context) obj : new Context( this, key );   
		return ctx;
	}
	
	@Override
	public void process() throws ServiceException {
		int s = 0;
		selecting = true;
		try {
			s = getSelector().select( getTimeout() );
		} catch (IOException e) {
			throw new ServiceException("Selecting failure ", e);
		} finally {
			selecting = false;
		}
		getLogger().logDebug("Selecting keys: " + s);
		if ( s == 0 ) return;
		
		Iterator<SelectionKey> it = selector.selectedKeys().iterator();
		while ( it.hasNext() ) {
			SelectionKey key = it.next();
			Context ctx = (Context) key.attachment();
			if ( ctx != null ) ctx.process( getExecutor() ); 
			it.remove();
		}
	}
	
	public boolean isSelecting() {
		return selecting;
	}

	public void nowSelect() {
		if ( selector != null && selecting ) selector.wakeup();
	}

	protected void onConnect(Context ctx) {
		getLogger().logDebug("Connection established");
		//if ( getProvider() != null ) ctx.readMessage( getProvider().create() );
	}
	
	protected void onRead(Context ctx, Message msg) {
		getLogger().logDebug("Reading message is complete");
		msg.clear();
		ctx.readMessage( msg );
	}
	
	protected void onWrite(Context ctx, Message msg) {
		getLogger().logDebug("Writing message is complete");
	}

	public MessageProvider getProvider() {
		return provider;
	}
	
	public void setProvider(MessageProvider provider) {
		this.provider = provider;
	}
	
	public Selector getSelector() throws IOException {
		if ( selector == null ) selector = Selector.open();
		return selector;
	}

	@Override
	public void stop() throws ServiceException {
		setStatus(Status.STOPPING);
		
		if ( selector != null ) {
			for ( SelectionKey key: selector.keys() ) {
				key.cancel();
				Context ctx = (Context) key.attachment();
				if (ctx != null ) ctx.process( getExecutor() );
			}
		}
		
		/*
		if ( executor != null ) {
			executor.shutdown();
			boolean term = false;
			try {
				term = executor.awaitTermination( 60L, TimeUnit.SECONDS );
			} catch (InterruptedException e) {
			}
			if ( !term ) executor.shutdownNow(); 
		}
		*/
			
		super.stop();
		
		if ( selector != null ) {
			try {
				selector.close();
			} catch (IOException e) { } 
			selector = null;
		}
	}
	

	public long getTimeout() {
		return timeout;
	}
	
	public void setTimeout(long value) {
		timeout = value;
	}
	
	
}
