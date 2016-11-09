import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;
import ru.avel.examples.ping.*;
import ru.avel.services.ASelectorService;
import ru.avel.services.MessageProvider;
import ru.avel.services.Service;
import ru.avel.services.ServiceException;
import ru.avel.services.helpers.Logger;

public class TCPPing {
	
	private Logger logger;
	private Properties props;
	private Service service;

	public TCPPing() {
		super();
		logger = new Logger(getClass().getName(), null);
		props = new Properties();
		defaultProperties();
		Runtime.getRuntime().addShutdownHook( new ShutdownHook());
	}
	
	public Logger getLogger() {
		return logger;
	}

	public Properties getProperties() {
		return props;
	}
	
	public int validOptions( String[] args ) {
		boolean valid = true;
		int i = 0, num;
		try {
			for(; valid && i < args.length; i++ ) {
				String arg = args[ i ];
				if ( arg.startsWith("-")) arg = arg.toLowerCase();
				
				if ( arg.equals("-c") ) {
					props.setProperty("mode", "catcher");  
				} else
				if ( arg.equals("-p") ) {
					props.setProperty("mode", "pitcher");  
				} else
				if ( arg.equals("-bind") && (valid = (++i < args.length)) ) {
					props.setProperty( "bind", args[ i ] );
				} else
				if ( arg.equals("-port") && (valid = (++i < args.length)) ) {
					num = Integer.valueOf(args[ i ]);
					valid = 0x0400 <= num && num <= 0xFFFF;
					props.setProperty( "port", Integer.toString(num) );
				} else
				if ( arg.equals("-mps") && (valid = (++i < args.length)) ) {
					num = Integer.valueOf(args[ i ]);
					valid = 0 < num;
					props.setProperty( "mps", Integer.toString(num) );
				} else
				if ( arg.equals("-size") && (valid = (++i < args.length)) ) {
					num = Integer.valueOf(args[ i ]);
					valid = 50 <= num && num <= 3000;
					props.setProperty( "size", Integer.toString(num) );
				} else 
				if ( i == args.length - 1 && !arg.startsWith("-") ) {
					props.setProperty( "hostname", arg );
				} else {
					valid = false;
				}
			}
		} catch( NumberFormatException e ) {
			valid = false;
		}
		return valid ? -1 : i;
	}
	
	void defaultProperties() {
		if ( !props.containsKey("port") ) props.setProperty("port", "9900");
		if ( !props.containsKey("mps") ) props.setProperty("mps", "1");
		if ( !props.containsKey("size") ) props.setProperty("size", "300");
	}
	
	
	Service createService() throws ServiceException {
		
		String mode = props.getProperty("mode");
		if ( mode == null ) mode = props.containsKey("hostname") ? "pitcher" : "catcher";
		
		ASelectorService svc = null;
		if ( mode.equals("pitcher") ) svc = new Pitcher( null, logger, props ); else 
		if ( mode.equals("catcher") ) svc = new Catcher( null, logger, props );
		if ( svc != null ) {
			svc.setProvider( new MessageProvider() {
				public PingMessage create() {
					return new PingMessage(); 
				};
			});
		}
		
		return svc;
	}
	
	public void run() throws Exception {
		getLogger().logInfo("Application running");
		getLogger().logInfo( printProps() );
	
		if (service == null) service = createService(); 
		if (service != null) service.start();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			while (!Thread.interrupted()) {
				try {
					Thread.sleep(500);
					if ( "exit".equalsIgnoreCase( br.readLine () ) ) break;
				} catch (InterruptedException e) {
					break;
				}
			}
		} finally {
			br.close();
		}
		
	}

	public void end() {
		if (service == null) return;
		try {
			service.stop();
		} catch (ServiceException e) {
			getLogger().logError("Service stopping failure", e);
		} finally {
			service = null;
		}
		getLogger().logInfo("Application finished");
	}

	public static void main(String[] args) {
		TCPPing app = new TCPPing();
		
		int i = app.validOptions(args);
		if ( i >= 0 ) {
			app.getLogger().logInfo( app.printUsage( i ) );
			return;
		}
		
		try {
			app.run();
			app.getLogger().logInfo("Application exiting normally.");
		} catch (Exception e) {
			app.getLogger().logError("Application Error: ", e);
		} finally {
			app.end();
		}

	}

	public String printUsage( int i ) {
		return ( i < 0 ? "" : "Invalid option: " + i + " \n") 
			 + "Usage: java TCPPing -c|-p [options...] [<hostname>] \n"
			 + "    -c     Catcher mode with options: \n"
			 + "               -bind <ip_address>    TCP socket bind address that will be to run listen \n"
			 + "               -port <port>          TCP socket port used for listening. Default 9900 \n"
			 + "    -p     Pitcher mode with options: \n"
			 + "               -port <port>          TCP socket port used for connecting. Default 9900 \n"
			 + "               -mps <rate>           The speed of message sending expressed as \"messages per second\". Default: 1 \n"
			 + "               -size <size>          Message length. Default 300, min 50, max 3000 \n"
			 + "    <hostname>    For Pitcher mode only. The name of the computer witch runs Catcher \n"
		;
	}
	
	public String printProps() {
		String s = "Properties:";
		for( String name : props.stringPropertyNames() ) s += "\n" + name + ": " + props.getProperty(name);
		return s;
	}

	class ShutdownHook extends Thread {
		@Override
		public void run() {
			if ( TCPPing.this.service == null ) return;
			TCPPing.this.getLogger().logInfo("Application shutting down, please wait...");
			TCPPing.this.end();
		}
	} 
}
