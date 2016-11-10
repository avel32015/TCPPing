package ru.avel.examples.ping;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ru.avel.services.ASelectorService;
import ru.avel.services.Context;
import ru.avel.services.Message;
import ru.avel.services.ServiceException;
import ru.avel.services.helpers.Logger;

/**
 * –еализаци€ отправл€ющего сервиса.
 * 
 * —оедин€етс€ с принимающим сервисом. ÷иклически отправл€ет пакеты и принимает ответ.
 * 
 * @author Velikotsky.Andrey
 *
 */

public class Pitcher extends ASelectorService {
	
	@Property
	private String host;
	
	@Property
	private Integer port;

	@Property
	private Float mps;
	
	@Property
	private Integer size;

	@Property
	private Integer num;
	
	private SocketChannel channel;
	private Context context;
	private SenderTask sender = new SenderTask();
	
	private LinkedHashMap<Integer, PingMessage> history = new LinkedHashMap<Integer, PingMessage>();

	private int totalCount = 0;
	private long totalMaxTime = 0;
	private int lastCount;
	private long lastAvgTime;
	private long lastAvgSend;
	private long lastAvgRecv;
	
	
	public Pitcher(String id, Logger logger, Properties props) {
		super(id, logger, props);
	}

	public String host() {
		return host == null ? "" : host;
	}
	
	public void host(String host) {
		this.host = host;
	}
	
	public int port() {
		return port == null ? 0 : port;
	}

	public void port(int port) {
		this.port = port;
	}

	public float mps() {
		return mps == null ? 0 : mps;
	}

	public void mps(float mps) {
		this.mps = mps;
	}

	public int size() {
		return size == null ? 0 : size;
	}

	public void size(int size) {
		this.size = size;
	}
	
	public int num() {
		return num == null ? 0 : num;
	}
	
	public void num(int num) {
		this.num = num;
	}

	public void channel(SocketChannel channel) {
		try {
			if (this.channel != null) this.channel.close();
		} catch (IOException e) { } 
		this.channel = channel;
	}
	
	@Override
	public void start() throws ServiceException {
		check(false, Status.STARTED);
		status(Status.STARTING);

		InetSocketAddress addr = new InetSocketAddress( host(), port() );
		try {
			channel( SocketChannel.open() );
			context = register(channel, SelectionKey.OP_CONNECT);
			channel.connect( addr );
		} catch (IOException e) {
			ServiceException se = new ServiceException("Server socket not opened", e); 
			failure( se );
			channel(null);
			throw se;
		}
		super.start();
	}

	@Override
	public void stop() throws ServiceException {
		sender.stop();
		try {
			super.stop();
		} finally {
			channel(null);
		}
	}
	
	@Override
	protected void onConnect(Context ctx) {
		System.out.format( "%-12s | %10s | %9s | %12s | %12s | %12s | %12s |%n", 
						   "Time", "totalCount", "lastCount", "lastAvgTime", "totalMaxTime", "lastAvgSend", "lastAvgRecv" );
		super.onConnect(ctx);
		sender.start();
	}
	
	@Override
	protected void onClose(Context ctx) {
		super.onClose(ctx);
		status(Status.STOPPING);
	}
	
	@Override
	protected void onRead(Context ctx, Message msg) {
		super.onRead(ctx, msg);
		updateStatistics( msg );
		printStatistics();
	}
	
	@Override
	protected void onWrite(Context ctx, Message msg) {
		logger().logDebug("Writing message is complete");
		updateStatistics( msg );
	}

	private void printStatistics() {
		System.out.format( "%tT.%<tL | %10d | %9d | %8tS.%<tL | %8tS.%<tL | %8tS.%<tL | %8tS.%<tL |%n", 
							new Date(), totalCount, lastCount, lastAvgTime, totalMaxTime, lastAvgSend, lastAvgRecv );
	}
	
	private void updateStatistics(Message msg) {
		if ( msg == null || !msg.getClass().equals( PingMessage.class ) ) return; 
		
		PingMessage pmsg = (PingMessage) msg, p = history.put( pmsg.getId(), pmsg );
		if ( p == null ) totalCount++; else messages().release( p );
		
		long lastTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis( 1L );
		lastAvgTime = lastAvgSend = lastAvgRecv = lastCount = 0;
		Iterator<PingMessage> it = history.values().iterator();
		while ( it.hasNext() && (p = it.next()) != null ) {
			if ( p.timeSendForward() < lastTime ) {
				messages().release( p );
				it.remove();
			} else if ( p.timeRecvBackward() != 0 ) {
				lastCount++;
				long t = p.timeRecvBackward() - p.timeSendForward() - ( p.timeSendBackward() - p.timeRecvForward() );
				if ( totalMaxTime < t ) totalMaxTime = t;
				lastAvgTime += t;  
				lastAvgSend += p.timeRecvForward() - p.timeSendForward();
				lastAvgRecv += p.timeRecvBackward() - p.timeSendBackward();
			}
		}
		if ( lastCount != 0) {
			lastAvgTime /= lastCount;
			lastAvgSend /= lastCount;
			lastAvgRecv /= lastCount;
		}
	}
	
	private Message newMessage() {
		Message msg = messages().acquire();
		if ( msg != null ) msg.generate( size() );
		return msg;
	}
	
	private class SenderTask implements Runnable {
		
		Future<?> future;
		int count;
		
		void start() {
			stop();
			future = null;
			count = 0;
			long period = mps() == 0 ? 1000 : Math.round( 1000 / mps() );
			if ( executor() == null ) logger().logWarn("Executor is undefined", null );
			else future = executor().scheduleAtFixedRate( this, 0, period, TimeUnit.MILLISECONDS );
		}
		
		void stop() {
			if ( future != null ) future.cancel(true);
		}
		
		@Override
		public void run() {
			if ( status() == Status.STOPPED ) stop();
			else if ( status() == Status.STARTED ) {
				Message msg = null;
				if ( ( ++count <= num() || num() == 0 ) && (msg = newMessage()) != null ) context.writeMessage( msg );
				if ( 0 < num() && num() < count ) { status( Status.STOPPING ); stop(); }
			}
		}
	}
	

}
