/**
 *
 */
package edu.nyu.cess.remote.server;

import edu.nyu.cess.remote.common.app.ExecutionRequest;
import edu.nyu.cess.remote.common.app.State;
import edu.nyu.cess.remote.common.net.*;
import org.apache.log4j.Logger;

import java.net.Socket;
import java.util.HashMap;

/**
 * The Class ClientProxy.
 */
public class ClientProxy implements ClientNetworkInterfaceObserver
{
    final static Logger logger = Logger.getLogger(ClientProxy.class);

    private Server server;

	/** The client network interfaces used to communicate with clients. */
	HashMap<String, LiteClientNetworkInterface> clientNetworkInterfaces = new HashMap<String, LiteClientNetworkInterface>();

	public ClientProxy(Server server)
    {
        this.server = server;
	}

	public void connectionRequestHandler()
    {
        int PORT_NUMBER = 2600;
        ServerNetworkInterface serverNetworkInterface = new ServerNetworkInterface(PORT_NUMBER);
		serverNetworkInterface.initializeServerSocketConnection();

		while (true) {
			// Blocks until a socket connection request is received
			Socket clientSocket = serverNetworkInterface.waitForIncomingConnection();
			
			String IPAddress = clientSocket.getInetAddress().getHostAddress();
			if (IPAddress != null && !IPAddress.isEmpty() && clientNetworkInterfaces.get(IPAddress) == null) {
                // Create a LiteClientNetworkInterface to manage the socket connection.
				LiteClientNetworkInterface clientNetworkInterface = new LiteClientNetworkInterface();
				clientNetworkInterface.setSocket(clientSocket);
				clientNetworkInterface.addClientNetworkInterfaceObserver(this);
                logger.debug("Client Connected: " + clientNetworkInterface.getRemoteIPAddress());

				clientNetworkInterfaces.put(clientNetworkInterface.getRemoteIPAddress(), clientNetworkInterface);

                server.addClientProxy(clientNetworkInterface.getRemoteIPAddress());

				clientNetworkInterface.startThreadedInboundCommunicationMonitor();
			}
		}
	}

	public void updateNetworkPacketReceived(DataPacket dataPacket, String ipAddress)
    {
        logger.debug("Packet received from client " + ipAddress);

		switch(dataPacket.getPacketType()) {
		case APPLICATION_EXECUTION_REQUEST:
			// Not supported by the server
			break;
		case APPLICATION_STATE_CHAGE:
			State appState = (State) dataPacket.getPayload();
			if (appState != null && appState instanceof State) {
                server.updateApplicationStateChanged(ipAddress, appState);
			}
			break;
		case HOST_INFO:
			HostInfo hostInfo = (HostInfo) dataPacket.getPayload();
			String hostName = hostInfo.getHostName();
			
			if(hostName != null && !hostName.isEmpty()) {
                server.updateClientHostNameUpdate(hostName, ipAddress);
			}
			break;
		case MESSAGE:
			// Not supported by the server
			break;
		case SOCKET_TEST:
			// No processing is done when a socket test is received
			break;
		default:
			// Do nothing
			break;
		}
	}

	public void updateNetworkConnectionStateChanged(String ipAddress, boolean isConnected)
    {
        logger.debug("Client " + ipAddress + " has " + ((isConnected) ? " connected to the server" : " disconnected"));

		if (!isConnected) {
			clientNetworkInterfaces.get(ipAddress).close();
			clientNetworkInterfaces.remove(ipAddress);
		}

        server.updateClientConnectionStateChanged(ipAddress, isConnected);
	}

	public void startApplicationOnClient(ExecutionRequest applicationExecutionRequest, String ipAddress) {
		DataPacket dataPacket = new DataPacket(PacketType.APPLICATION_EXECUTION_REQUEST, applicationExecutionRequest);
		clientNetworkInterfaces.get(ipAddress).writeDataPacket(dataPacket);
	}

	public void stopApplicationOnClient(ExecutionRequest stopExecutionRequest, String ipAddress) {
		DataPacket dataPacket = new DataPacket(PacketType.APPLICATION_EXECUTION_REQUEST, stopExecutionRequest);
		clientNetworkInterfaces.get(ipAddress).writeDataPacket(dataPacket);
	}

	public void sendMessageToClient(String message, String ipAddress) {
		DataPacket dataPacket = new DataPacket(PacketType.MESSAGE, message);
		clientNetworkInterfaces.get(ipAddress).writeDataPacket(dataPacket);
	}
}
