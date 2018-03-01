package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

public class MultithreadedServer implements Runnable {

	Socket csocket;
	public static int noOfConnections = 0;

	/* Logging object declaration */

	private final static Logger LOGGER = Logger.getLogger(Logger.class.getName());

	final static int socket = 5678;

	MultithreadedServer(Socket csocket) {
		this.csocket = csocket;
	}

	public static void main(String args[]) throws Exception {
		ServerSocket ssock = new ServerSocket(socket);
		/* System.out.println("Listening"); */
		LOGGER.info("Server Listening to port: 5678");
		while (true) {
			Socket sock = ssock.accept();
			// System.out.println("Connected");
			LOGGER.info("New tcp connection established");
			noOfConnections++;
			LOGGER.info("New tcp connection established - Connection Number: " + noOfConnections);
			new Thread(new MultithreadedServer(sock)).start();
		}

	}

	public void run() {
		try {
			// InputStream reader can read only characters
			// to read input line Buffered reader is needed
			String line;
			// for reading from input stream
			BufferedReader clientInputReader = new BufferedReader(new InputStreamReader(csocket.getInputStream()));

			// for writing to output stream
			PrintStream pstream = new PrintStream(csocket.getOutputStream());

			ArrayList<Long> fibo;
			while ((line = clientInputReader.readLine()) != null) { // receives the n value of the fibo series from
																	// client
				fibo = new ArrayList<>();
				// writing to the output stream of the socket to which client is connected
				for (long i = Long.parseLong(line); i >= 0; i--)
					fibo.add(fibonacciGenerator(i));
				pstream.println(fibo);
				pstream.flush();

			}
			csocket.close();
			LOGGER.info("Connection Closed!");
		} catch (IOException e) {
			/* System.out.println(e); */
			LOGGER.severe("IO interruption detected!");
		}
	}

	// returns the last fibonacci number in the series using recursion
	// if n = 5 returns (0 1 1 2 3 5)
	public long fibonacciGenerator(long n) {

		if (n == 0 || n == 1)
			return 1;

		return fibonacciGenerator(n - 1) + fibonacciGenerator(n - 2); // recursively generates the fibo
	}

}
