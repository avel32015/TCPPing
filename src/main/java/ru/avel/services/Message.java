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
	 * ����� <b>read(buffer)</b> ������� �������� ��������� ������� �� ������.
	 * @param buffer ������� ������ ���������� ������ ������ �������� � ����� ���������� ������ ������ ���������� ����� ��������.
	 * @return ��������, ��� �� �������� ���� ���������.
	 * @throws MessageException ��������� � ������ ��������� ������� ������.
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
	 * ����� <b>write(buffer)</b> ��������� �������� ��������� ������� � ������.
	 * @param buffer ������� ������ ���������� ������ ������ �������� � ����� ���������� ������ ������ ���������� ����� ��������.
	 * @return ��������, ��� �� �������� ���� ���������.
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
