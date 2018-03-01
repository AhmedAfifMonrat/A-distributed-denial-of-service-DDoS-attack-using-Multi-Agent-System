package architect;

import java.util.ArrayList;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.AMSService;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;

public class CoordinatorAgent extends GuiAgent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private CoordinatorGUI coordinatorGUI;
	private String receiver = "";

	private String messageType = "";
	private String serverName = "";
	private String serverPort = "";
	private String tickerDuration = "";
	private String noOfAgents = "";
	private String statusMessagesText = "";

	private String payload;

	public ArrayList<AID> remoteBrokers;

	public ArrayList<String> agentList;
	public static int agentCounterInitial = 0;
	public static int agentCounterFinal = 0;

	protected void setup() {

		// Printout a welcome message
		System.out.println("Coordinator agent " + getAID().getName() + " is ready.");

		/*
		 * This part will query the AMS to get list of all active agents in all
		 * containers
		 */
		agentList = new ArrayList<String>();
		refreshActiveAgents();

		// Command and Control Setup
		remoteBrokers = new ArrayList<AID>();

		coordinatorGUI = new CoordinatorGUI(this);
		coordinatorGUI.displayGUI();

		// Register the book-selling service in the yellow pages

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("coordinator-agent");
		sd.setName(getLocalName() + "-Coordinator agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new ReceiveMessage());

	}

	// Agent clean-up
	protected void takeDown() {

		// Dispose the GUI if it is there
		if (coordinatorGUI != null) {
			coordinatorGUI.dispose();
		}

		// Printout a dismissal message
		System.out.println("Coordinator Agent " + getAID().getName() + " is terminating.");

		// De-register from the yellow pages
		try {
			DFService.deregister(this);
			System.out.println("Agent " + getAID().getName() + " has been signed off.");
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	// Sending message is an implementation of OneShotBehavior(Send once for one
	// time)
	public class SendMessage extends OneShotBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		// Send message from to someone
		public void action() {

			payload = messageType + " " + serverName + " " + serverPort + " " + tickerDuration + " " + noOfAgents;

			ACLMessage messageToSend = new ACLMessage(ACLMessage.REQUEST);

			// Add all message agents as receivers( For testing Locally)
			// for (String agent : agentList) {
			// if (agent.equals("Architect"))
			// continue;
			// messageToSend.addReceiver(new AID(agent, AID.ISLOCALNAME));
			// }

			// Set up receivers to prepare for broadcast
			for (AID receiver : remoteBrokers) {
				messageToSend.addReceiver(receiver);
			}

			// Broadcast message from coordinator
			messageToSend.setLanguage("English");
			messageToSend.setContent(payload);
			send(messageToSend);

			// saveToFile(getAID().getLocalName() +":"+ content);
			statusMessagesText += "\nCoordinator Message: " + messageToSend.getContent();
			coordinatorGUI.setMessageTextArea(statusMessagesText);
			System.out.println(getAID().getName() + " sent a message to " + receiver + "\n"
					+ "Content of the message is: " + messageToSend.getContent());

		}
	}

	public class ReceiveMessage extends CyclicBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// Variable for the contents of the received Message
		private int messagePerformative;
		private String messageContent;
		private String senderName;

		// Receive message and append it in the conversation textArea in the GUI
		@Override
		public void action() {
			// TODO Auto-generated method stub

			ACLMessage messageReceived = receive();
			if (messageReceived != null) {

				messagePerformative = messageReceived.getPerformative();
				if (messagePerformative == ACLMessage.REQUEST) {
					remoteBrokers.add(messageReceived.getSender());
					if (payload != null) {
						ACLMessage attack = new ACLMessage(ACLMessage.INFORM);
						attack.addReceiver(messageReceived.getSender());
						attack.setContent(payload);
						send(attack);
					}
				}
				if (messagePerformative == ACLMessage.FAILURE) {
					remoteBrokers.remove(messageReceived.getSender());
				}

				messageContent = messageReceived.getContent();
				senderName = messageReceived.getSender().getLocalName();

				// print the message details in console
				System.out.println(
						"**** " + getAID().getLocalName() + " received a message" + "\n" + "Sender name: " + senderName
								+ "\n" + "Content of the message: " + messageContent + "\n");
				System.out.println("**********************************");

				statusMessagesText += "\n" + senderName + ": " + messageContent;
				coordinatorGUI.setMessageTextArea(statusMessagesText);
			}
		}

	}

	// get all entered input from gui agent
	public void getFromGui(final String type, final String name, final String port, final String duration,
			final String number) {
		addBehaviour(new OneShotBehaviour() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void action() {
				messageType = type;
				serverName = name;
				serverPort = port;
				tickerDuration = duration;
				noOfAgents = number;
			}
		});
	}

	@Override
	protected void onGuiEvent(GuiEvent arg0) {
		// TODO Auto-generated method stub
		addBehaviour(new SendMessage());
	}

	// if new agents are created after instantiating this object
	// this method will keep the lists updated
	public void refreshActiveAgents() {

		AMSAgentDescription[] agents = null;

		try {
			SearchConstraints c = new SearchConstraints();
			c.setMaxResults(new Long(-1));
			agents = AMSService.search(this, new AMSAgentDescription(), c);
		} catch (Exception e) {
		}

		// Add all the active agents in the agent list to show in drop-down
		for (int i = 0; i < agents.length; i++) {
			AID agentID = agents[i].getName();
			if (agentID.getLocalName().equals("ams") || agentID.getLocalName().equals("rma")
					|| agentID.getLocalName().equals("df"))
				continue;
			agentList.add(agentID.getLocalName());
		}
		// System.out.println(agentList.toString());

	}

}
