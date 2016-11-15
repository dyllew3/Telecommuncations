package cs.tcd.ie;
/**
 * 
 * @author Stefan Weber
 */
import java.net.DatagramPacket;

public interface PacketContent {
	public String toString();
	public DatagramPacket toDatagramPacket();
}