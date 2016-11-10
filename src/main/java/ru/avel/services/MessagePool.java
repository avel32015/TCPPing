package ru.avel.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class MessagePool {
	
	private Supplier<Message> supplier;
	
	private ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<Message>();
	
	private ConcurrentHashMap<Message, Boolean> map = new ConcurrentHashMap<Message, Boolean>(); 
	
	public MessagePool() {
	}
	
	public void supplier(Supplier<Message> supplier) {
		this.supplier = supplier;
	}
	
	protected Message create() {
		try {
			if ( supplier != null ) return supplier.get();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Message acquire() {
		Message msg = queue.poll();
		if ( msg == null ) msg = create(); 
		else map.put( msg, false );
		return msg; 
	}
	
	public void release(Message msg) {
		if ( msg == null || Boolean.TRUE.equals( map.replace( msg, true ) ) ) return;
		msg.clear();
		queue.offer( msg );		
	}

}
