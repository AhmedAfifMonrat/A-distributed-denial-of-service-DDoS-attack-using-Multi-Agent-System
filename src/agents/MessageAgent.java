package agents;

import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class MessageAgent extends Agent {
	// private MessageAgentGui myGui;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String coordinatorName = "Architect@172.31.24.191:1099/JADE";
	private String coordinatorAddress = "http://54.212.247.209:7778";
	private String coordinatorMessage;

	private String localName;
	private String messageType = "";
	private String serverIP = "";
	private String serverPort = "";
	private String timeForTickerBehaviour = "";
	private int noOfAgents = 0; // no of agents to be added
	public ArrayList<AgentController> agentList;
	public final static String AgentclassPath = "agents.Smith";
	public int agentCount = 0;

	protected void setup() {
		// Printout a welcome message and save Local Name
		localName = getLocalName();
		System.out.println("Messenger agent " + getAID().getName() + " is ready.");

		agentList = new ArrayList<AgentController>();

	
		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("messenger-agent");
		sd.setName(getLocalName() + "-Messenger agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new HelloMessage());
		addBehaviour(new ReceiveMessage());
	}

	/**
	 * Agent clean-up
	 **/
	protected void takeDown() {

		// Printout a dismissal message
		System.out.println("Message-agent " + getAID().getName() + "terminating.");

		addBehaviour(new GoodbyeMessage());

		/**
		 * This piece of code, to deregister with the DF, is explained in the book in
		 * section 4.4.2.1, page 73
		 **/
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	public class CreateAgent extends OneShotBehaviour {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void action() {

			Runtime rt = Runtime.instance();
			ProfileImpl p = new ProfileImpl(false);
			p.setParameter("serverIP", serverIP);
			p.setParameter("serverPort", serverPort);
			p.setParameter("timeForTicker", timeForTickerBehaviour);
			jade.wrapper.AgentContainer container = rt.createMainContainer(p);

			for (int counter = 0; counter < noOfAgents; counter++) {

				// AgentController Agent = null;
				try {
					agentList.add(((ContainerController) container).createNewAgent("Agent" + agentCount, "agents.Smith",
							null));
					agentList.get(agentList.size() - 1).start(); // gives the recently added object
					agentCount++;
				} catch (StaleProxyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	public class RemoveAgent extends OneShotBehaviour {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void action() {

			for (AgentController agent : agentList) {
				try {
					agent.kill();
				} catch (StaleProxyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			agentList.clear();
			agentCount = 0;

		}
	}

	// Sending Hello message is an implementation of OneShotBehavior(Send once for
	// one
	// time)
	public class HelloMessage extends OneShotBehaviour {
		// Send message to Coordinator

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void action() {

			AID coordinator = new AID();
			coordinator.setName(coordinatorName);
			coordinator.addAddresses(coordinatorAddress);

			// Send a Presence Message
			ACLMessage hello = new ACLMessage(ACLMessage.REQUEST);
			coordinatorMessage = "Agent Broker spawned and ready for control messages";
			hello.addReceiver(coordinator);
			hello.setContent(coordinatorMessage);
			send(hello);
		}
	}

	// Sending Goodbye message is an implementation of OneShotBehavior(Send once for
	// one
	// time)
	public class GoodbyeMessage extends OneShotBehaviour {

		// Send message to Coordinator

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void action() {

			AID coordinator = new AID();
			coordinator.setName(coordinatorName);
			coordinator.addAddresses(coordinatorAddress);

			// Send a Presence Message
			ACLMessage goodbye = new ACLMessage(ACLMessage.FAILURE);
			coordinatorMessage = "Agent Broker terminated";
			goodbye.addReceiver(coordinator);
			goodbye.setContent(coordinatorMessage);
			send(goodbye);
		}
	}

	public class ReceiveMessage extends CyclicBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// Variable for the contents of the received Message
		private String messagePerformative;
		private String[] messageParameters;
		private String SenderName;
		private String response;

		// Receive message and append it in the conversation textArea in the GUI
		public void action() {

			ACLMessage messageReceived = receive();

			if (messageReceived != null) {
				messagePerformative = ACLMessage.getPerformative(messageReceived.getPerformative());
				SenderName = messageReceived.getSender().getLocalName();

				// Split content into its components
				messageParameters = messageReceived.getContent().split(" ");

				if (messageParameters.length != 5) {
					System.out.println(
							"Insufficient parameters. Expected: messageType ServerIP ServerPort TimeforTickerBehaviourInMilliSeconds NoOfAgents ");
					System.exit(0);
				}

				messageType = messageParameters[0];
				serverIP = messageParameters[1];
				serverPort = messageParameters[2];
				timeForTickerBehaviour = messageParameters[3];
				noOfAgents = Integer.parseInt(messageParameters[4]);

				// print the message details in console
				System.out.println("****Message Agent " + getAID().getLocalName() + " received a message" + "\n"
						+ "Sender name: " + SenderName + "\n" + "Content of the message: "
						+ messageReceived.getContent() + "\n" + "Message type: " + messagePerformative + "\n");
				System.out.println("**********************************");

				// Create and Kill Agents based on request
				if (messageType.equals("start")) {
					addBehaviour(new CreateAgent());
					// Send a Response
					ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
					response = localName + ": Created " + noOfAgents + " agents successfully";
					reply.addReceiver(messageReceived.getSender());
					reply.setContent(response);
					send(reply);
				} else {
					addBehaviour(new RemoveAgent());
					// Send a Response
					ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
					response = localName + ": Killed " + noOfAgents + " agents successfully";
					reply.addReceiver(messageReceived.getSender());
					reply.setContent(response);
					send(reply);
				}
			}

		}
	}

}