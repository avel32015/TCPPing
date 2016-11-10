/**
 * 
 */
package ru.avel.examples.ping;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import ru.avel.services.Message;
import ru.avel.services.MessageException;

/**
 * @author avel
 *
 */
public class TextMessage extends Message {

	private static final long serialVersionUID = -920848289203671785L;

	public String text = "";
	
	public TextMessage() {
	}

	public TextMessage(String text) {
		this.text = text;
	}
	
	@Override
	public int length() {
		return text.length();
	}
	
	@Override
	public void length(int length) {
		if ( length < text.length() ) text = text.substring( 0, length );  
	}
	
	@Override
	public boolean read(ByteBuffer buffer) throws MessageException {
		StringBuilder str = new StringBuilder();
		
		boolean end = false;
		while ( buffer.hasRemaining() && !end ) {
			byte b = buffer.get();
			str.append( (char) b );
			if ( b == 0x0D || b == 0x0A ) {
				end = true;
				if ( buffer.hasRemaining() ) {
					b = buffer.get();
					if ( b == 0x0D || b == 0x0A ) str.append( (char) b ); 
					else buffer.position( buffer.position() - 1 );
				}
			}
		}
		if ( end ) text = str.toString();
		return end;
	}
	
	@Override
	public boolean write(ByteBuffer buffer) {
		try {
			buffer.put( text.getBytes() );
		} catch(BufferOverflowException e) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return text;
	}
	
}
