package agents;

import java.io.*;
import java.net.*;
import java.nio.CharBuffer;

public class ClientSocket {
	//define a constant used as size of buffer 
	static final int BUFSIZE=10000;
	
	String serverAddress;
	int portNumber;
	Socket clientSocket = null;
    PrintWriter out = null;
    BufferedReader in = null;
    /*InputStream in = null;*/
    byte[] buff = new byte[BUFSIZE];
	int bytesread = 0;
	
	
	public ClientSocket(String serverAddress, int portNumber){
		
		this.serverAddress	=	serverAddress;
		this.portNumber 	=	portNumber;
		
	}
	
	public void connect() throws IOException{
		
	    try {

	    	clientSocket = new Socket(serverAddress, portNumber);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             
            
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + serverAddress);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for "+ "the connection to: " + serverAddress+ " at the port: "+portNumber);
            System.exit(1);
        }

		
	}
	
	public String send(String command) throws IOException{
		
		out.println(command);
		String serverResponse = in.readLine();
		
		
		
		return serverResponse;
	}
	
	
	public void terminate() throws IOException{
		
		out.close();
		in.close();
		clientSocket.close();
		
	}
	
}