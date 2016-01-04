package edu.nyu.cess.remote.client;

import edu.nyu.cess.remote.client.config.HostConfigInterface;
import edu.nyu.cess.remote.common.app.*;
import org.apache.log4j.Logger;

import javax.swing.*;

/**
 * The Client essentially manages local application execution requests
 * on behalf of the Server. The client receives status updates from both
 * the Application and the ServerProxy, which manages communication with the
 * Server.
 *
 * @author Anwar A. Ruff
 */
public class Client implements ApplicationObserver, ServerProxyObserver
{
	final static Logger log = Logger.getLogger(Client.class);

	private App app;

	private ServerMessageDispatcher serverMessageDispatcher;

	/**
	 * Initialize the client
	 * @param hostConfig host config file
     */
	public void initServerConnection(HostConfigInterface hostConfig) {
		serverMessageDispatcher = new ServerMessageDispatcher(hostConfig);
		serverMessageDispatcher.addDispatchObserver(this);
		serverMessageDispatcher.createPersistentServerConnection();
	}

	/**
	 * Updates the application state
	 * @param applicationState update the application state
     */
	public void applicationUpdate(State applicationState) {

		serverMessageDispatcher.sendServerApplicationState(applicationState);

		if (applicationState instanceof StartedState) {
			log.info("Sending Started State");
		}
		else if (applicationState instanceof StopedState) {
			log.info("Sending Stopped State");
		}
	}

	/**
	 * Update the server with the execution request received.
	 * @param exeReq the application execution request
     */
	public void updateServerExecutionRequestReceived(ExeRequestMessage exeReq) {

		State requestedApplicationState = exeReq.getApplicationState();
		log.info("Application execution request received from the server.");

		// If a request to start an application has been made...
		if (requestedApplicationState instanceof StartedState) {

			if (app == null) {
				app = new App(exeReq.getName(), exeReq.getPath(), exeReq.getArgs());
				app.addObserver(this);
				app.changeState(requestedApplicationState);
			}
			else if (app.isStopped()) {
				app = new App(exeReq.getName(), exeReq.getPath(), exeReq.getArgs());
				app.addObserver(this);
				app.changeState(requestedApplicationState);
			}
			else if (app.isStarted()) {
				applicationUpdate(requestedApplicationState);
			}
		}
		else if (requestedApplicationState instanceof StopedState) {

			if (app == null) {
				applicationUpdate(requestedApplicationState);
			}
			else if (app.isStarted()) {
				app.changeState(requestedApplicationState);
			}
			else if (app.isStopped()) {
				applicationUpdate(requestedApplicationState);
			}

		}
	}

	/**
	 * Update the observers with the clients connection status.
	 * @param isConnected the connection status
     */
	public void updateNetworkStateChanged(boolean isConnected) {
		if (isConnected && app != null) {
			if (app.isStarted()) {
				log.info("Sending started state message server.");
				applicationUpdate(new StartedState());
			}
		}
	}

	/**
	 * Passes and invokes the MessageRunnable with the message string.
	 * @param message message status string
     */
	public void updateServerMessageReceived(String message) {
		SwingUtilities.invokeLater(new MessageRunnable(message));
	}

	/**
	 * The message runnable class used to display messages sent from the server.
	 */
	private class MessageRunnable implements Runnable {
		String message;

		public MessageRunnable(String message) {
			this.message = message;
		}

		public void run() {
			JFrame frame = new JFrame();
			JOptionPane.showMessageDialog(frame, message, "Experimenter Notification", JOptionPane.WARNING_MESSAGE);
		}

	}
}
