package ru.avel.services;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

import ru.avel.services.helpers.Logger;

public class Context implements Runnable {
	
	private static int COUNTER = 0;
	private static int BUFFER_SIZE = 256;
	
	private ASelectorService service; 
	private Logger logger;
	private boolean running;
	private boolean reading;
	private boolean writing;
	private int selectOps;

	private SelectionKey key;
	private SocketChannel channel;
	private ServerSocketChannel server;
	
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	
	private Message readMessage;
	private Message writeMessage;
	

	public Context(ASelectorService service, SelectionKey key) {
		super();
		this.service = service;
		logger = service.getLogger().newLogger("Context-"+ ++COUNTER);
		selectionKey(key);
		logger.logDebug("Channel context created: " +  socketString());
	}
	
	public SelectionKey selectionKey() {
		return key;
	}
	
	public String socketString() {
		return channel != null ? channel.socket().toString() : server != null ? server.socket().toString() : "no socket";
	}
	
	
	public String socketAddress() {
		InetAddress addr = channel != null ? channel.socket().getInetAddress() : server != null ? server.socket().getInetAddress() : null;
		return addr != null ? addr.toString() : "no address";
	}
	
	private void selectionKey(SelectionKey k) {
		channel = null; 
		server = null;
		key = k;
		key.attach( this );
		
		if ( key.channel() instanceof SocketChannel ) channel = (SocketChannel) key.channel();  
		else server = (ServerSocketChannel) key.channel(); 
	}

	public boolean reading() {
		return reading;
	}
	public boolean writing() {
		return writing;
	}
	
	public void readMessage(Message message) {
		if ( (readMessage = message) == null ) {
			key.interestOps( key.interestOps() ^ SelectionKey.OP_READ );
			return;
		}
		key.interestOps( key.interestOps() | SelectionKey.OP_READ );
		service.nowSelect();
	}
	
	public void writeMessage(Message message) {
		if ( (writeMessage = message) == null ) {
			key.interestOps( key.interestOps() ^ SelectionKey.OP_WRITE );
			return;
		}
		key.interestOps( key.interestOps() | SelectionKey.OP_WRITE );
		service.nowSelect();
	}
	
	public void process(Executor executor) {
		if ( running ) return;
		running = true;
		if ( key.isValid() ) key.interestOps( key.interestOps() ^ key.readyOps() );
		if ( executor != null ) executor.execute( this ); else run();
	}
	
	@Override
	public void run() {
		running = true;
		try {
			if (logger.isDebug()) logger.logDebug( debugSelectionKey() );
			boolean ok = false;
			if ( key.isValid() ) {
				selectOps = 0;
				try {
					if ( key.isAcceptable() ) accept();
					if ( key.isConnectable() ) connect();
					if ( key.isReadable() ) read();
					if ( key.isWritable() ) write();
					if ( selectOps != 0 ) key.interestOps( key.interestOps() | selectOps ); 
					ok = true;
				} catch(Exception e) {
					logger.logError("Channel failed", e);
				}
			}
			if ( !ok ) close();
			else logger.logDebug("Context processed");
		} catch(Exception e) {
			logger.logError("Context run failed", e);
		} finally {
			running = false;
		}
	}
	
	private String debugSelectionKey() {
		StringBuilder str = new StringBuilder("Selection key (");
		if ( key == null ) str.append("null)");
		else if ( !key.isValid() ) str.append("no valid)"); 
		else {
			str.append("0x").append( Integer.toHexString( key.readyOps() ) ).append("): ");
			if ( key.isAcceptable() ) str.append("accept ");
			if ( key.isConnectable() ) str.append("connect ");
			if ( key.isReadable() ) str.append("read ");
			if ( key.isWritable() ) str.append("write ");
		}
		return str.toString();
	}
	
	private void accept() throws Exception {
		for (;;) {
			SocketChannel ch = null;
			try {
				ch = server.accept();
			} catch(IOException e) {
				logger.logError("Accept connection failed", e);
			}
			if ( ch == null) break; 
			service.onConnect( service.register( ch, 0 ) );
		}
		selectOps |= SelectionKey.OP_ACCEPT;
	}
	
	
	private void connect() throws IOException {
		if ( channel.finishConnect() ) {
			logger.logInfo("Channel opened: " + socketString());
			service.onConnect( this );
		}
	}
	
	private void read() throws IOException {
		if ( readMessage == null ) {
			logger.logWarn("Message for read is undefined", null);
			return;
		}
		
		for(;;) {
			readBuffer = checkBuffer( readBuffer, readMessage.length() );
			if ( !reading ) readBuffer.clear(); 
			reading = true;
			if ( channel.read( readBuffer ) == 0 ) {
				selectOps |= SelectionKey.OP_READ;
				return;
			}
			readBuffer.flip();
			try {
				if ( readMessage.read( readBuffer ) ) break;
			} catch (MessageException e) {
				throw new IOException("Message reading failed", e);
			}
			readBuffer.position( readBuffer.limit() );
		}
		
		readBuffer.clear();
		reading = false;
		Message m = readMessage;
		readMessage = null;
		service.onRead( this, m );
	}
	
	private void write() throws IOException {
		if ( writeMessage == null ) {
			logger.logWarn("Message for write is undefined", null);
			return;
		}
		
		if ( !writing ) {
			writeBuffer = checkBuffer( writeBuffer, writeMessage.length() );
			writeBuffer.clear();
			if ( !writeMessage.write( writeBuffer ) ) logger.logWarn("Buffer for write is overload", null);
			writeBuffer.flip();
			writing = true;
		}
		while ( writeBuffer.hasRemaining() ) 
			if ( channel.write( writeBuffer ) == 0 ) {
				selectOps |= SelectionKey.OP_WRITE;
				return;
			}
		
		writeBuffer.clear();
		writing = false;
		Message m = writeMessage;
		writeMessage = null;
		service.onWrite( this, m );
	}

	private ByteBuffer checkBuffer(ByteBuffer buf, int length) {
		if ( length > 0 ) {
			if ( buf == null ) buf = ByteBuffer.allocate( length );
			else if ( buf.capacity() < length ) buf = ByteBuffer.allocate( length ).put( (ByteBuffer) buf.rewind() );
			else if ( buf.limit() < length ) buf.limit( length );
		} else {
			if ( buf == null ) buf = ByteBuffer.allocate( BUFFER_SIZE );
			else if ( buf.position() == buf.capacity() ) buf = ByteBuffer.allocate( buf.capacity() + BUFFER_SIZE ).put( (ByteBuffer) buf.rewind() );
			else if ( buf.limit() < buf.capacity() ) buf.limit( buf.capacity() );
		}
		return buf;
	}
	
	public void close() {
		if ( key == null ) return;
		Channel ch = key.channel();
		key.cancel();
		key.attach(null);
		key = null;
		if ( ch == null ) return;
		
		logger.logInfo("Channel closed: " + socketString());
		try {
			ch.close();
		} catch (IOException e) {
			logger.logError("Channel closing failure", e);
		}
	}

}
