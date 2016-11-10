package ru.avel.services;

import java.io.Serializable;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Message implements Serializable {

	private static final long serialVersionUID = -4169011766837078311L;
	
	protected static int LABEL = (int) serialVersionUID;
	protected final int SIZE;  
	protected final int MAX = 0xFFFF;  
	
	private int length = 0;
	

	public Message() {
		SIZE = ( Integer.SIZE + (LABEL == 0 ? 0 : Integer.SIZE) ) / Byte.SIZE; 
	}
	
	public Message(int length) {
		this();
		length( length );
	}
	
	public int length() {
		return length;
	}
	
	public void length(int length) {
		this.length = length;
	}
	
	public Message clear() {
		length(0);
		return this;
	}

	public void generate(int length) {
		clear();
		length(length);
	}
	
	/**
	 * Метод <b>read(buffer)</b> читаете значения атрибутов объекта из буфера.
	 * @param buffer Позиция буфера определяет начало чтения значений и после завершения чтения должна определять конец значений.
	 * @return Сообщает, все ли атрибуты были прочитаны.
	 * @throws MessageException возникает в случае неверного формата данных.
	 */
	public boolean read(ByteBuffer buffer) throws MessageException {
		clear();
		int i;
		try {
			if ( LABEL != 0 && ( i = buffer.getInt() ) != LABEL ) throw new MessageException("Label of message incorrect: 0x" + Integer.toHexString(i) );
			length( buffer.getInt() );
		} catch(BufferUnderflowException e) {
			return false;
		}
		if ( length < 0 || MAX < length ) throw new MessageException("Length of message incorrect: " + length);
		if ( length == 0) return true;
		return length - SIZE <= buffer.remaining();
	}
	
	/**
	 * Метод <b>write(buffer)</b> сохраняет значения атрибутов объекта в буфере.
	 * @param buffer Позиция буфера определяет начало записи значений и после завершения записи должна определять конец значений.
	 * @return Сообщает, все ли атрибуты были сохранены.
	 */
	public boolean write(ByteBuffer buffer) {
		try {
			if ( LABEL != 0 ) buffer.putInt( LABEL );
			buffer.putInt( length() );
		} catch(BufferOverflowException e) {
			return false;
		}
		return true;
	}
	
	
}
