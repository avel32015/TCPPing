//package ru.avel.examples.ping;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Date;

public class MessageOld {
	
	private static int NUMBERS = 1;

	private ByteBuffer buffer = ByteBuffer.allocate( 256 );
	private ByteBuffer outbound = null;
	private boolean catcher = false;
	
	private static int LABEL = 0xCAFE;
	private int length = 0;
	private int number = 0;
	
	private long sourceSend = 0;
	private long targetRecv = 0;
	private long targetSend = 0;
	private long sourceRecv = 0;
	
	private long clientOut = 0;
	private long catcherIn = 0;
	private long catcherOut = 0;
	private long pitcherIn = 0;
	
	public static final int HEADER_SIZE = ( 3 * Integer.SIZE + 4 * Long.SIZE ) / Byte.SIZE; 
	
	public MessageOld(boolean catcher) {
		super();
		this.catcher = catcher;
	}
	
	public MessageOld(int length) {
		super();
		generate(length);
	}
	
	private void stage(boolean receipt) {
		long time = System.currentTimeMillis();
		if ( catcher ) {
			if ( receipt ) catcherIn = time; else catcherOut = time;
		} else {
			if ( receipt ) pitcherIn = time; else clientOut = time;
		}
	}
	
	public boolean read(ByteChannel channel) throws IOException {
		if ( complete() ) return false;
		int read = 0;
		while ( (read = channel.read( buffer )) > 0 && parse() == false );
		if ( complete() ) {
			stage( true );
			return true;
		}
		if ( read < 0 ) throw new IOException("Channel has reached end-of-stream");
		return false;
	}
	
	public boolean parse() throws IOException {
		if ( length == 0 ) {
			if ( buffer.position() < Integer.SIZE / Byte.SIZE ) return false;
			ByteBuffer b = buffer.duplicate();
			b.flip();
			if ( b.getInt() != LABEL ) throw new IOException("Message structure is broken");
			if ( buffer.position() < HEADER_SIZE ) return false;
			int l = b.getInt();
			if ( l < 0 ) throw new IOException("Message length is invalid");
			length( l );
			number = b.getInt();
			clientOut = b.getLong();
			catcherIn = b.getLong();
			catcherOut = b.getLong();
			pitcherIn = b.getLong();
		}
		return complete();
	}

	public boolean complete() {
		return 0 < length && length <= buffer.position();
	}
	
	public boolean write(ByteChannel channel) throws IOException {
		if ( !complete() ) return false;
		if ( outbound == null ) {
			stage( false );
			outbound = store();
			outbound.rewind();
		}
		while ( channel.write( outbound ) > 0 );
		if ( outbound.hasRemaining() ) return false;
		outbound = null;
		return true;
	}

	public void length(int length) {
		if ( buffer.capacity() < length ) buffer = ByteBuffer.allocate( length ).put( (ByteBuffer) buffer.flip() );
		buffer.limit( length );
		this.length = length;
	}
	
	public int length() {
		return length;
	}
	
	public void reset() {
		buffer.flip().position( length );
		buffer.compact();
		length = 0;
		outbound = null;
	}

	private ByteBuffer store() {
		ByteBuffer buf = buffer.duplicate();
		buf.rewind(); 
		
		buf.putInt(LABEL);
		buf.putInt(length);
		buf.putInt(number);
		buf.putLong(clientOut);
		buf.putLong(catcherIn);
		buf.putLong(catcherOut);
		buf.putLong(pitcherIn);
		
		return buf;
	}
	
	private void generate(int length) {
		outbound = null;
		buffer.clear();
		if ( length < HEADER_SIZE ) length = HEADER_SIZE;
		length(length);
		number = nextNumber();
		
		@SuppressWarnings("unused")
		ByteBuffer buf = store();
		/*
		 Заполнить buf нужным набором данных до limit 
		 */
		
	}

	private synchronized int nextNumber() {
		return NUMBERS++;
	}

	public int getNumber() {
		return number;
	}
	
	public Date getPitcherOut() {
		return new Date(clientOut);
	}

	public Date getCatcherIn() {
		return new Date(catcherIn);
	}

	public Date getCatcherOut() {
		return new Date(catcherOut);
	}

	public Date getPitcherIn() {
		return new Date(pitcherIn);
	}



	
}
