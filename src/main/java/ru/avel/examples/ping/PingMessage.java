package ru.avel.examples.ping;

import ru.avel.services.Message;

public class PingMessage extends Message {

	private static final long serialVersionUID = -7961996241585393781L;

	{
		LABEL = (int) serialVersionUID;
	}
	
	public PingMessage() {
		super();
	}

	public PingMessage(int length) {
		super(length);
	}

}
