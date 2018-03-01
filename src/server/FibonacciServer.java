package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class FibonacciServer extends Thread {

	public static class ClientHandler implements Runnable {

		private final static Logger LOGGER = Logger.getLogger(Logger.class.getName());
		/**
		 * The socket connected to the client.
		 */
		private final Socket csocket;

		/**
		 * Creates a new ClientHandler thread for the socket provided.
		 * 
		 * @param clientSocket
		 *            the socket to the client.
		 */
		public ClientHandler(final Socket csocket) {
			this.csocket = csocket;
		}

		/**
		 * The run method is invoked by the ExecutorService (thread pool).
		 */
		@Override
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

	public static void main(String[] args) {

		// Make sure both arguments are present
		if (args.length < 1) {
			FibonacciServer.printUsage();
			System.exit(1);
		}

		// Try to parse the port number
		int port = -1;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException nfe) {
			System.err.println("Invalid listen port value: \"" + args[1] + "\".");
			FibonacciServer.printUsage();
			System.exit(1);
		}

		// Make sure the port number is valid for TCP.
		if (port <= 0 || port > 65536) {
			System.err.println("Port value must be in (0, 65535].");
			System.exit(1);
		}

		final FibonacciServer server = new FibonacciServer(port);
		// Starts the server's independent thread
		server.start();

		try {
			// Wait for the server to shutdown
			server.join();
			System.out.println("Completed shutdown.");
		} catch (InterruptedException e) {
			// Exit with an error condition
			System.err.println("Interrupted before accept thread completed.");
			System.exit(1);
		}

	}

	/**
	 * Prints a simple usage string to standard error that describes the
	 * command-line arguments for this class.
	 */
	private static void printUsage() {
		System.err.println("Multithreaded Server requires argument: <Listen Port>");
	}

	/**
	 * Pool of worker threads of unbounded size. A new thread will be created for
	 * each concurrent connection, and old threads will be shut down if they remain
	 * unused for about 1 minute.
	 */
	private final ExecutorService workers = Executors.newCachedThreadPool();

	/**
	 * Server socket on which to accept incoming client connections.
	 */
	private ServerSocket listenSocket;

	/**
	 * Flag to keep this server running.
	 */
	private volatile boolean keepRunning = true;

	/**
	 * Creates a new threaded echo server on the specified TCP port. Calls
	 * {@code System.exit(1)} if it is unable to bind to the specified port.
	 * 
	 * @param port
	 *            the TCP port to accept incoming connections on.
	 */
	public FibonacciServer(final int port) {

		// Capture shutdown requests from the Virtual Machine.
		// This can occur when a user types Ctrl+C at the console
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				FibonacciServer.this.shutdown();
			}
		});

		try {
			this.listenSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("An exception occurred while creating the listen socket: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * This is executed when ThreadPoolEchoServer.start() is invoked by another
	 * thread. Will listen for incoming connections and hand them over to the
	 * ExecutorService (thread pool) for the actual handling of client I/O.
	 */
	@Override
	public void run() {
		// Set a timeout on the accept so we can catch shutdown requests
		try {
			this.listenSocket.setSoTimeout(1000);
		} catch (SocketException e1) {
			System.err.println("Unable to set acceptor timeout value.  The server may not shutdown gracefully.");
		}

		System.out.println("Accepting incoming connections on port " + this.listenSocket.getLocalPort());

		// Accept an incoming connection, handle it, then close and repeat.
		while (this.keepRunning) {
			try {
				// Accept the next incoming connection
				final Socket clientSocket = this.listenSocket.accept();
				System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());

				ClientHandler handler = new ClientHandler(clientSocket);
				this.workers.execute(handler);

			} catch (SocketTimeoutException te) {
				// Ignored, timeouts will happen every 1 second
			} catch (IOException ioe) {
				System.err.println("Exception occurred while handling client request: " + ioe.getMessage());
				// Yield to other threads if an exception occurs (prevent CPU
				// spin)
				Thread.yield();
			}
		}
		try {
			// Make sure to release the port, otherwise it may remain bound for several
			// minutes
			this.listenSocket.close();
		} catch (IOException ioe) {
			// Ignored
		}
		System.out.println("Stopped accepting incoming connections.");
	}

	/**
	 * Shuts down this server. Since the main server thread will time out every 1
	 * second, the shutdown process should complete in at most 1 second from the
	 * time this method is invoked.
	 */
	public void shutdown() {
		System.out.println("Shutting down the server.");
		this.keepRunning = false;
		this.workers.shutdownNow();
		try {
			this.join();
		} catch (InterruptedException e) {
			// Ignored, we're exiting anyway
		}
	}



}
