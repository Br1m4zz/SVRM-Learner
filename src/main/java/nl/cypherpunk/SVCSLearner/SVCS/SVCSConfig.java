
package nl.cypherpunk.SVCSLearner.SVCS;

import java.io.IOException;

import nl.cypherpunk.SVCSLearner.LearningConfig;

public class SVCSConfig extends LearningConfig{
	String alphabet;
	String cmd;
	
	String cleanup_script;

	String host;

	String InputPath;

	String shmname;

	int port;
	
	boolean restart;
	boolean console_output;
	boolean receivefist;
	boolean enable_socketdebug;
	boolean enable_processdebug;
	int timeout;    

    public SVCSConfig(String filename) throws IOException {
    super(filename);
	}


	public SVCSConfig(LearningConfig config) {
		super(config);
	}	

	@Override
	public void loadProperties() {
		super.loadProperties();

		if(properties.getProperty("alphabet") != null)
			alphabet = properties.getProperty("alphabet");
		
		
		if(properties.getProperty("cmd") != null)
			cmd = properties.getProperty("cmd");
		
		
		if(properties.getProperty("host") != null)
			host = properties.getProperty("host");

		if(properties.getProperty("input_dir")!= null)
		InputPath = properties.getProperty("input_dir");
		
		if(properties.getProperty("shmname")!= null)
			shmname = properties.getProperty("shmname");
		
		if(properties.getProperty("port") != null)
			port = Integer.parseInt(properties.getProperty("port"));

		if(properties.getProperty("console_output") != null)
			console_output = Boolean.parseBoolean(properties.getProperty("console_output"));
		else
			console_output = false;
		
		if(properties.getProperty("receive_first") != null)
			receivefist = Boolean.parseBoolean(properties.getProperty("receive_first"));
		else
			receivefist = false;

		if(properties.getProperty("restart") != null)
			restart = Boolean.parseBoolean(properties.getProperty("restart"));
		else
			restart = false;
		
		if(properties.getProperty("timeout") != null)
			timeout = Integer.parseInt(properties.getProperty("timeout"));

		if(properties.getProperty("cleanup_script") != null)
			cleanup_script = properties.getProperty("cleanup_script");

		if(properties.getProperty("debug_socket") != null)
			enable_socketdebug = Boolean.parseBoolean(properties.getProperty("debug_socket"));

		if(properties.getProperty("debug_process") != null)
			enable_processdebug = Boolean.parseBoolean(properties.getProperty("debug_process"));
	}

}
