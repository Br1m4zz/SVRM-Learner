
package nl.cypherpunk.SVCSLearner.SVCS;

import java.util.Arrays;

import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import nl.cypherpunk.SVCSLearner.StateLearnerSUL;


public class SVCSSUL implements StateLearnerSUL<String, String>{
    Alphabet<String> alphabet;
    SVCSHarness svcs;
    public SVCSSUL(SVCSConfig config) throws Exception{
        alphabet = Alphabets.fromList(Arrays.asList(config.alphabet.split(" ")));

        svcs = new SVCSHarness();
        svcs.setHost(config.host);
        svcs.setPort(config.port);
        svcs.setCommand(config.cmd);
        svcs.enable_sdebug(config.enable_socketdebug);
        svcs.enable_pdebug(config.enable_processdebug);
        svcs.setcleanupcmd(config.cleanup_script);
        svcs.setRequireRestart(config.restart);
		svcs.setReceiveMessagesTimeout(config.timeout);
		svcs.setConsoleOutput(config.console_output);
        svcs.setInputfiles(config.InputPath);
        svcs.setShareMemory("/dev/shm/", "SVshm");
        svcs.setreceivefirst(config.receivefist);
        svcs.start();
    }

    public Alphabet<String> getalAlphabet(){
        return alphabet;
    }

    public boolean canFork() {
		return false;
	}

	@Override
	public String step(String symbol) {
		String result = null;
		try {
			result = svcs.processSymbol(symbol);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public void pre() {
		try {
			svcs.reset();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}	
	}

    @Override
	public void post() 
    {

	}
    
}
