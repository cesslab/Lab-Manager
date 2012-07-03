package edu.nyu.cess.remote.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * The <code>NetworkContactInfo</code> stores the necessary network contact
 * information needed to open a socket with a remote network node. Specifically,
 * an IP Address (version 4), and Port number.
 * 
 * @author Anwar A. Ruff
 */
public class SocketInfo {
	private String IPAddress;
	private int portNumber;

	public SocketInfo() {
		IPAddress = null;
		portNumber = 0;
	}

	/**
	 * Initializes the port number
	 * 
	 * @param portNumber
	 *            port number
	 */
	public SocketInfo(int portNumber) {
		this.portNumber = portNumber;
	}

	/**
	 * Initializes both the port number and IP Address
	 * 
	 * @param IPAddress
	 *            IP Address
	 * @param portNumber
	 *            Port number
	 */
	public SocketInfo(String IPAddress, int portNumber) {
		this.IPAddress = IPAddress;
		this.portNumber = portNumber;
	}

	/**
	 * Sets the IP Address
	 * 
	 * @param IPAddress
	 *            IP Address
	 */
	public void setIPAddress(String IPAddress) {
		this.IPAddress = IPAddress;
	}

	/**
	 * Sets the Port Number
	 * 
	 * @param portNumber
	 */
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	/**
	 * Returns an IP Address (IP version 4)
	 * 
	 * @return IP Address
	 */
	public String getIPAddress() {
		return IPAddress;
	}

	/**
	 * Returns a Port Number
	 * 
	 * @return Port Number
	 */
	public int getPortNumber() {
		return portNumber;
	}

	/**
	 * Reads the remote network nodes IP Address and port number (respectively)
	 * using the {@link File} parameter, and sets them locally.
	 * 
	 * @param file
	 *            comma delimited file containing the IP Address and port number
	 * @return Network contact information retrieved from the {@link File}, or
	 *         null if the file reading process or data validation failed.
	 */
	public boolean readFromFile(File file) {
		String[] socketInfo = null, octets = null;
		Boolean validIPAddress = true, validPortNumber = true, exceptionError = false, result = false;
		final int IP_ADDRESS = 0, PORT_NUMBER = 1;
		int tempPortNumber = 0;

		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

			System.out.println("Reading network file information from: " + file.getAbsolutePath());

			socketInfo = (bufferedReader.readLine()).split(",");

			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.err.println("File not found." + ex.getMessage());
			exceptionError = true;
			System.exit(1);
		} catch (IOException ex) {
			exceptionError = true;
			System.err.println("IO Exception Occured.");
			System.exit(1);
		}

		// If socketInfo consists of an IP Address and a Port Number
		if (socketInfo.length == 2) {

			// 4 octets in an IP Address
			octets = socketInfo[IP_ADDRESS].split(".");
			if (octets.length == 4) {

				// check the range of each octet
				for (String octet : octets) {
					try {
						validIPAddress &= (Integer.parseInt(octet) >= 0 && Integer.parseInt(octet) <= 223);
					} catch (NumberFormatException ex) {
						exceptionError = true;
					}
				}
			}

			try {
				// check the port number range
				tempPortNumber = Integer.parseInt(socketInfo[PORT_NUMBER]);
				validPortNumber &= (tempPortNumber >= 1024 && tempPortNumber <= 49151);
			} catch (NumberFormatException ex) {
				exceptionError = true;
			}
		}

		if (validPortNumber && validIPAddress && !exceptionError) {
			System.out.println("Network info read is valid.");
			System.out.println("IP Address: " + socketInfo[IP_ADDRESS]);
			System.out.println("Port Number: " + tempPortNumber);

			this.IPAddress = socketInfo[IP_ADDRESS];
			this.portNumber = tempPortNumber;
			result = true;
		}

		return result;
	}
}
