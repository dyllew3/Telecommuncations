package cs.tcd.ie;

/**
 * @author Dylan Lewis
 * @author Stefan Weber
 * 
 * Sends data to
 */

import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import tcdIO.*;

/**
 *
 * Client class
 * 
 * An instance accepts user input
 *
 */
public class Client extends Node {
	static final int DEFAULT_SRC_PORT = 50000;
	static final int DEFAULT_DST_PORT = 50001;
	static final String DEFAULT_DST_NODE = "localhost";
	static final String URI_SCHEME="file://";
	
	static final int NUM_OF_FRAMES = 2;
	// length in bytes
	static final int HEADER_LENGTH = 2;
	static final int DATA_LENGTH = 1250;
	private int frameToSend;
	private int ackFrame;
	
	Terminal terminal;
	InetSocketAddress dstAddress;
	private byte[] data;

	/**
	 * Constructor
	 * 
	 * Attempts to create socket at given port and create an InetSocketAddress
	 * for the destinations
	 */
	Client(Terminal terminal, String dstHost, int dstPort, int srcPort) {
		try {
			this.terminal = terminal;
			dstAddress = new InetSocketAddress(dstHost, dstPort);
			socket = new DatagramSocket(srcPort);
			socket.setSoTimeout(10000);
			listener.go();
			frameToSend = 0;
			ackFrame = 0;
		
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		if (packet == null && data != null) {
			terminal.println("Socket timed Out");
			terminal.println("Re-attempting connection");
			try {
				sendPacket(data);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (data == null || (packet.getData()[1] & 0x1000) == 0x1000) {
			socket.close();
		} else {
			byte[] ack = packet.getData();
			if (ack[0] == 0) {

				ackFrame = ack[1] >> 4;

				System.arraycopy(ack, 2, ack, 0, ack.length - 2);
				String content = new String(ack);
				String toPrint = (content);
				terminal.println(toPrint);
				this.notify();
			} else {
				terminal.println("Packet not from valid address");
			}
		}

	}

	public void sendPacket(byte[] data) throws IOException {

		DatagramPacket packet = null;
		packet = new DatagramPacket(data, data.length, dstAddress);
		socket.send(packet);

	}

	/**
	 * Sender Method
	 * 
	 */
	public synchronized void start() throws Exception {
		try {
			

			byte[] totalData = null;
//get file name			
			String contents = (terminal.readString("Enter file Name to send: "));					
//create a URI from file name			
			URI theUri=new URI(URI_SCHEME+contents);
//get a path from the URI		 
			Path path=Paths.get(theUri);
//check it exists			
			if(path==null){
				terminal.println("Supplied Filename '"+contents+"' cannot be read");
				return;
			}
//read the file in			
			totalData = Files.readAllBytes(path);

			if (totalData != null) {
//if it's all good - display the file contents and send to the server				
				String checkString=new String(totalData);
				terminal.println("Reading file "+contents+"\nfile contains: \n"+totalData.length+"bytes \n sending  text :"+checkString);
				int start = 0;
				//does this until the all packets sent
				while (start != -1) {

					if (frameToSend == ackFrame) {
						terminal.println("Sending packet...");
						start = getNextdataPack(totalData, start);
						sendPacket(data);
						terminal.println("Packet sent");
						frameToSend = (frameToSend + 1)% NUM_OF_FRAMES;

						this.wait();
					} else {

					}
				}
				data = null;

			}
		} catch (NullPointerException e) {
//something bad happened...			
			terminal.println("Exception "+e.getLocalizedMessage());
		}

	}
	
/**
 * 
 * @param packets
 * @param start
 * @return -1 or start += DATA_LENGTH
 * returns -1 if this is the final data packet to be sent
 */
	public int getNextdataPack(byte[] packets, int start) {
		if (packets.length - start < DATA_LENGTH && packets.length - start > 0) {
			byte[] newPackets = new byte[packets.length + HEADER_LENGTH];
			System.arraycopy(packets, start, newPackets, HEADER_LENGTH, (packets.length -start));
			int next = ackFrame + 8;
			newPackets[0] = 1;	
			newPackets[1] = (byte) ((frameToSend << 4) | next );
			data = newPackets;
			start = packets.length;
			start = -1;
		} else if (packets.length - start >= DATA_LENGTH) {
			byte[] newPackets = new byte[DATA_LENGTH + HEADER_LENGTH];
			System.arraycopy(packets, start, newPackets, HEADER_LENGTH, DATA_LENGTH);
			newPackets[0] = 1;
			newPackets[1] = (byte) ((frameToSend << 4) | ackFrame);
			data = newPackets;
			start += DATA_LENGTH;
		} else {
			
			start = -1;
		}

		return start;
	}

	/**
	 * Test method
	 * 
	 * Sends a packet to a given address
	 */
	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Client");
			(new Client(terminal, DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	public void resend() {
		// TODO Auto-generated method stub

	}
}