package za.co.wethinkcode;

import java.io.IOException;
import java.net.UnknownHostException;

import za.co.wethinkcode.market.Market;

public class App {
    public static void main( String[] args ) throws UnknownHostException, IOException, Exception {
    	Market m = new Market();
    	
    	while (true) {
    		String message = m.getFixMessage();
    		m.sendResponse(message);
    	}
    }
}
