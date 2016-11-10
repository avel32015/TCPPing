package ru.avel.examples.ping;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import ru.avel.services.Message;
import ru.avel.services.MessageException;

public class PingMessage extends Message {

	private static final long serialVersionUID = -7961996241585393781L;
	
	private static int nextId;
	
	{
		LABEL = (int) serialVersionUID;
	}
	
	private int id;
	
	private long timeSendForward;
	private long timeRecvForward;
	private long timeSendBackward;
	private long timeRecvBackward;
	
	public PingMessage() {
		super();
	}

	public void generate(int length) {
		super.generate(length);
		
		id = nextId();
	}
	
	private synchronized int nextId() { return ++nextId; }
	
	@Override
	public Message clear() {
		super.clear();
		timeSendForward = timeRecvForward = timeSendBackward = timeRecvBackward = id = 0; 
		
		return this;
	}
	
	@Override
	public boolean read(ByteBuffer buffer) throws MessageException {
		int p = buffer.position();
		if ( !super.read( buffer ) ) return false;

		try {
			id = buffer.getInt();
			timeSendForward = buffer.getLong();
			timeRecvForward = buffer.getLong();
			timeSendBackward = buffer.getLong();
			timeRecvBackward = buffer.getLong();

			if ( buffer.position() < p + length() ) buffer.position( p + length() );
			
		} catch(BufferUnderflowException e) {
			return false;
		}
		updateTime( false );
		return true;
	}

	@Override
	public boolean write(ByteBuffer buffer) {
		int p = buffer.position();
		if ( !super.write( buffer ) ) return false;
		
		updateTime( true );
		try {
			buffer.putInt( id );
			buffer.putLong( timeSendForward );
			buffer.putLong( timeRecvForward );
			buffer.putLong( timeSendBackward );
			buffer.putLong( timeRecvBackward );
			
			if ( buffer.position() < p + length() ) buffer.position( p + length() );
			
		} catch(BufferOverflowException | IllegalArgumentException e) {
			return false;
		}
		
		return true;
	}
	
	private void updateTime(boolean sending) {
		long t = System.currentTimeMillis();
		if ( sending )
			if ( timeRecvForward == 0 ) timeSendForward = t; else timeSendBackward = t;
		else
			if ( timeRecvForward == 0 ) timeRecvForward = t; else timeRecvBackward = t;
	}

	public int getId() { return id; }
	public long timeSendForward() { return timeSendForward; }
	public long timeRecvForward() { return timeRecvForward; } 
	public long timeSendBackward() { return timeSendBackward; }
	public long timeRecvBackward() { return timeRecvBackward; }

	@Override
	public String toString() {
		return new StringBuilder("PingMessage id=").append(id).append(" : ").append(timeSendForward).append(" ").append(timeRecvForward).append(" ").append(timeSendBackward).append(" ").append(timeRecvBackward).toString() ;
	}
	
	
}

