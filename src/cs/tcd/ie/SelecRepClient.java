package cs.tcd.ie;

/**
 * @author Dylan Lewis
 * based off client class
 */
import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
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
public class SelecRepClient extends Node {
	static final int DEFAULT_SRC_PORT = 50000;
	static final int DEFAULT_DST_PORT = 50001;
	static final String DEFAULT_DST_NODE = "localhost";
	static final String URI_SCHEME = "file://";
	static final int NUM_OF_FRAMES = 16;
	// length in bytes
	static final int HEADER_LENGTH = 2;
	static final int DATA_LENGTH = 1250;
	private boolean done;
	// frame to send and place in window index
	private int frameToSend;
	// check if ack for window index has been received
	private boolean[] acks;
	private boolean started = false;
	private int ackFrame;
	private int start = 0;
	// windows have contain the frame values in order
	private int[] window;
	Terminal terminal;
	InetSocketAddress dstAddress;
	private byte[][] frames;
	private byte[] data;
	private byte[] totalData;
	private boolean nack;

	/**
	 * Constructor
	 * 
	 * Attempts to create socket at given port and create an InetSocketAddress
	 * for the destinations
	 */
	public SelecRepClient(Terminal terminal, String dstHost, int dstPort, int srcPort) {
		try {
			socket = new DatagramSocket(srcPort);
			socket.setSoTimeout(10000);
			this.terminal = terminal;
			dstAddress = new InetSocketAddress(dstHost, dstPort);

			listener.go();
			frameToSend = 0;
			done = false;
			frames = new byte[SelectiveRepeat.getWindowSize(NUM_OF_FRAMES)][];
			window = new int[SelectiveRepeat.getWindowSize(NUM_OF_FRAMES)];
			ackFrame = 0;
			acks = new boolean[SelectiveRepeat.getWindowSize(NUM_OF_FRAMES)];
			SelectiveRepeat.setToEmpty(window);
			started = true;
			nack = false;

		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		if (packet == null && !done && started) {

			reset();
			this.notify();
		} else if (done) {

			socket.close();
		} else {
			byte[] response = packet.getData();
			// address is the left most 4 bits in the first byte of the
			// header. Address for this sender is 0
			int address = response[0] >> 4;
			if (address == 0) {
				// next we check the code
				// the code should be 0b10XX
				if ((response[0] & 0b1000) == 0b1000) {
					int code = (response[0] & 0b11);
					ackFrame = (response[1] >> 4);
					if (ackFrame < 0)
						ackFrame = NUM_OF_FRAMES + ackFrame;
					// if last two digits of the code are 0b11 it means resend
					// the frame
					// if they are 0b00 it means send the next frame
					if (code == 0b11) {
						nack = true;
						byte [] printing = new byte[response.length - 2];
						System.arraycopy(response, 2, printing, 0, response.length - 2);
						terminal.println(new String(printing));
						

					} else if (code == 0b00) {
						// 4 leftmost bits of the 2nd byte gives the number
						// whereas 4th bit in the byte is the P/F bit which is
						// 0 if there is more info to come and 1 if it's the
						// final frame

						int pollFinal = (response[1] & 0b1000) >> 3;
						if (pollFinal == 0) {
							if (SelectiveRepeat.windowPos(window,
									((ackFrame + NUM_OF_FRAMES - 1) % NUM_OF_FRAMES)) != -1) {
								acks[SelectiveRepeat.windowPos(window,
										((ackFrame + NUM_OF_FRAMES - 1) % NUM_OF_FRAMES))] = true;
								byte[] string = new byte[response.length - 2];
								System.arraycopy(response, 2, string, 0, response.length - 2);
								terminal.println(new String(string));
								if (SelectiveRepeat.isFull(acks)) {
									start = getNextWindow(start, totalData);
									reset();
									this.notify();
								} else {

								}

							} else {

								System.out.println(ackFrame);
								terminal.println("Not in window error \n Resetting");
							}
						} else {
							done = true;
							terminal.println("Last Packet Received");
							this.notify();

						}
					} else {
						System.out.println("Invalid Instruction");
					}
				} else {
					System.out.println("invalid Code");
				}

			} else {
				System.out.println("Invalid address");
			}
		}

	}

	// sends a specified frame
	public void sendFrame(byte[] data) throws IOException {

		DatagramPacket packet = null;
		packet = new DatagramPacket(data, data.length, dstAddress);
		socket.send(packet);

	}

	// gets the next window for selective repeat
	public int getNextWindow(int start, byte[] totalData) {
		int index = 0;
		while (index < window.length) {
			if (start != -1) {
				window[index] = frameToSend;
				start = getNextdataPack(totalData, start);
				frames[index] = data;
			} else {
				window[index] = -1;
				frames[index] = null;
			}

			index++;

		}

		return start;

	}

	public void reset() {
		for (int i = 0; i < acks.length; i++) {
			acks[i] = false;
		}

	}

	public synchronized void resendFrames() {
		nack = true;
		if (SelectiveRepeat.windowPos(window, ackFrame) != -1) {
			terminal.println("NACK " + SelectiveRepeat.windowPos(window, ackFrame));
			try {
				while (frameToSend != ackFrame) {

					sendFrame(frames[SelectiveRepeat.windowPos(window, ackFrame)]);
					this.wait(2000);

				}
				nack = false;
				this.notify();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sender Method
	 * 
	 */
	public synchronized void start() {
		try {

			totalData = null;
			// get file name
			String contents = (terminal.readString("Enter file Name to send: "));
			// create a URI from file name
			URI theUri = new URI(URI_SCHEME + contents);
			// get a path from the URI
			Path path = Paths.get(theUri);
			// check it exists
			if (path == null) {
				terminal.println("Supplied Filename '" + contents + "' cannot be read");
				return;
			}
			// read the file in
			totalData = Files.readAllBytes(path);

			if (totalData != null) {
				// if it's all good - display the file contents and send to the
				// server
				String checkString = new String(totalData);
				terminal.println("Reading file " + contents + "\nfile contains: \n" + totalData.length
						+ "bytes \n sending  text :" + checkString);

				start = 0;
				start = getNextWindow(start, totalData);
				while (!done) {
					if (SelectiveRepeat.isFull(acks)) {
						start = getNextWindow(start, totalData);
						reset();
					}
					int index = 0;
					while (index < window.length) {

						if (nack) {
							while(frameToSend != ackFrame && nack){
								int windPos = SelectiveRepeat.windowPos(window, ackFrame);
								sendFrame(frames[windPos]);
								this.wait(2000);
							}
							nack = false;

						} else {
							sendFrame(frames[index]);
							System.out.println(window[index]);
							index++;

							this.wait(2000);
						}
					}

					this.wait();

				}
			}

		} catch (NullPointerException e) {
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @param packets
	 * @param start
	 * @return start This function gets the next datapack and indicates if the
	 *         datapack is the last datapack by returning -1. Otherwise it
	 *         returns the starting location of the next packet. It stores this
	 *         data in the byte array data
	 * 
	 */
	public int getNextdataPack(byte[] packets, int start) {

		if (packets.length - start < DATA_LENGTH && packets.length - start > 0) {
			byte[] newPackets = new byte[packets.length + HEADER_LENGTH];
			System.arraycopy(packets, start, newPackets, HEADER_LENGTH, (packets.length - start));
			newPackets[0] = (byte) 0b00011000;
			newPackets[1] = (byte) 0b1000;
			newPackets[1] += (byte) (frameToSend << 4);
			frameToSend = (frameToSend + 1) % NUM_OF_FRAMES;

			data = newPackets;
			start = -1;
		} else if (packets.length - start >= DATA_LENGTH) {
			byte[] newPackets = new byte[DATA_LENGTH + HEADER_LENGTH];
			System.arraycopy(packets, start, newPackets, HEADER_LENGTH, DATA_LENGTH);
			newPackets[0] = (byte) 0b00011000;
			newPackets[1] += (byte) (frameToSend << 4);
			frameToSend = (frameToSend + 1) % NUM_OF_FRAMES;
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
			(new SelecRepClient(terminal, DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	public void resend() {
		// TODO Auto-generated method stub

	}
}