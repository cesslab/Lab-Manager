package edu.nyu.cess.remote.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.util.ArrayList;

public class LiteClientNetworkInterface implements ClientNetworkInterfaceObservable {

	ArrayList<ClientNetworkInterfaceObserver> observers = new ArrayList<ClientNetworkInterfaceObserver>();

	private Socket socket;

	private Thread inboundCommunicationThread;
	private Thread networkInterfaceMonitorThread;

	private String remoteIPAddress;

	private ObjectInputStream objectInputStream;
	private ObjectOutputStream objectOutputStream;

	public LiteClientNetworkInterface() {
	}

	public boolean setSocket(Socket sock) {
		boolean result = false;

		if (sock != null) {

			if (sock.isConnected()) {
				socket = sock;
				remoteIPAddress = socket.getInetAddress().getHostAddress();

				result = true;
			}
		}

		return result;
	}

	private boolean initializeObjectInputStream() {
		boolean result = false;

		if (socket != null) {
			if (socket.isConnected()) {
				try {
					objectInputStream = new ObjectInputStream(socket.getInputStream());
					result = true;
				} catch (IOException ex) {
					objectInputStream = null;
					result = false;
				}
			}
		}
		return result;
	}

	private boolean initializeObjectOutputStream() {
		boolean result = false;

		if (socket != null) {
			try {
				objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
				result = true;
			} catch (IOException ex) {
				System.out.println("Error occured retrieving ObjectOutputStream.");
			}
		}
		return result;
	}

	private DataPacket readDataPacket() {
		DataPacket dataPacket = null;
		boolean streamInitialized = true;

		if (socket.isConnected()) {

			if (objectInputStream == null) {
				streamInitialized = initializeObjectInputStream();
			}

			if (objectInputStream != null && streamInitialized) {
				try {
					Object object = objectInputStream.readObject();
					dataPacket = (DataPacket) object;
				} catch (ClassNotFoundException ex) {
					System.out.println("The Serialized Object Not Found");
					dataPacket = null;
				} catch (StreamCorruptedException ex) {
					dataPacket = null;
				} catch (IOException ex) {
					dataPacket = null;
				}
			}
		}

		return dataPacket;

	}

	public boolean writeDataPacket(DataPacket packet) {
		boolean streamInitialized = false;

		if (socket != null) {
			if (socket.isConnected()) {

				if (objectOutputStream == null) {
					streamInitialized = initializeObjectOutputStream();
				}

				if (objectOutputStream != null) {
					try {
						objectOutputStream.writeObject(packet);
						objectOutputStream.flush();
						streamInitialized = true;
					} catch (IOException ex) {
						System.out.println("IO Exception Occured Writing DataPacket");
					}
				}
			}
		}

		return streamInitialized;
	}

	public String getRemoteIPAddress() {
		return remoteIPAddress;
	}

	public boolean isConnected() {
		return (socket != null) ? socket.isConnected() : false;
	}

	public void close() {
		if (socket != null) {
			try {
				socket.close();
				socket = null;
			} catch (IOException e) {
				System.out.println("Socket Error: Failed to close.");
			}
		}

		if (objectOutputStream != null) {
			try {
				objectOutputStream.close();
				objectOutputStream = null;
			} catch (IOException ex) {
				System.out.println("IO Exception: Failed to close ObjectOutputStream.");
			}
		}

		if (objectInputStream != null) {
			try {
				objectInputStream.close();
				objectInputStream = null;
			} catch (IOException e) {
				System.out.println("IO Exception: Failed to close ObjectOutputStream.");
			}
		}

		if (inboundCommunicationThread != null) {
			if (inboundCommunicationThread.isAlive()) {
				inboundCommunicationThread.interrupt();
				try {
					inboundCommunicationThread.join();
				} catch (InterruptedException e) {
					System.out.println("LiteClientInterface Close(): Failed to join inbound communication thread");
				}
			}
		}

		if (networkInterfaceMonitorThread != null) {
			if (networkInterfaceMonitorThread.isAlive()) {
				networkInterfaceMonitorThread.interrupt();

				try {
					networkInterfaceMonitorThread.join();
				} catch (InterruptedException e) {
					System.out.println("LiteClientInterface Close(): Failed to join network monitor thread");
				}
			}
		}
	}

	public boolean addObserver(ClientNetworkInterfaceObserver networkObserver) {
		return observers.add(networkObserver);
	}

	public boolean deleteObserver(ClientNetworkInterfaceObserver networkObserver) {
		return observers.remove(networkObserver);
	}

	public void notifyNetworkStatusUpdate(String ipAddress, boolean isConnected) {
		for (ClientNetworkInterfaceObserver observer : observers) {
			observer.networkStatusUpdate(ipAddress, isConnected);
		}
	}

	public void notifyObservers(DataPacket dataPacket) {
		for (ClientNetworkInterfaceObserver observer : observers) {
			observer.networkPacketUpdate(dataPacket, remoteIPAddress);
		}
	}

	public void startThreadedInboundCommunicationMonitor() {

		inboundCommunicationThread = new Thread(new InboundCommunicationListener());
		inboundCommunicationThread.setName("Inbound communication thread");
		inboundCommunicationThread.start();

		startNetworkInterfaceMonitor();
	}

	public void startNetworkInterfaceMonitor() {
		networkInterfaceMonitorThread = new Thread(new NetworkStreamMonitor());
		networkInterfaceMonitorThread.setName("Network Interface Monitor");
		networkInterfaceMonitorThread.start();

	}

	private class NetworkStreamMonitor implements Runnable {

		public void run() {
			boolean interfaceState = true;
			DataPacket packet = new DataPacket(null);

			while (interfaceState) {
				try {
					interfaceState = writeDataPacket(packet);
					System.out.println(((interfaceState) ? "OPEN CONNECTION: " + remoteIPAddress
							: "CLOSED CONNECTION: " + remoteIPAddress));
					Thread.sleep(40000);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	private class InboundCommunicationListener implements Runnable {

		public void run() {
			DataPacket dataPacket = null;

			notifyNetworkStatusUpdate(remoteIPAddress, true);

			System.out.println("Waiting for message from Client " + remoteIPAddress);
			while ((dataPacket = readDataPacket()) != null) {
				System.out.println("Data Packet Received");
				notifyObservers(dataPacket);
			}

			notifyNetworkStatusUpdate(remoteIPAddress, false);

			System.out.println("Client " + remoteIPAddress + " connection closed...");
		}
	}

}
