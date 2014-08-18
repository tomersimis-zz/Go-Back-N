import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;


public class Receiver {
	
	// Probability of ACK loss
	public static final double PROBABILITY = 0.1;

	public static void main(String[] args) throws Exception{
		
		DatagramSocket fromSender = new DatagramSocket(9876);
		
		// 83 is the base size (in bytes) of a serialized RDTPacket object 
		byte[] receivedData = new byte[Sender.MSS + 83];
		
		int waitingFor = 0;
		
		ArrayList<RDTPacket> received = new ArrayList<RDTPacket>();
		
		boolean end = false;
		
		while(!end){
			
			System.out.println("Waiting for packet");
			
			// Receive packet
			DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
			fromSender.receive(receivedPacket);
			
			// Unserialize to a RDTPacket object
			RDTPacket packet = (RDTPacket) Serializer.toObject(receivedPacket.getData());
			
			System.out.println("Packet with sequence number " + packet.getSeq() + " received (last: " + packet.isLast() + " )");
		
			if(packet.getSeq() == waitingFor && packet.isLast()){
				
				waitingFor++;
				received.add(packet);
				
				System.out.println("Last packet received");
				
				end = true;
				
			}else if(packet.getSeq() == waitingFor){
				waitingFor++;
				received.add(packet);
				System.out.println("Packed stored in buffer");
			}else{
				System.out.println("Packet discarded (not in order)");
			}
			
			// Create an RDTAck object
			RDTAck ackObject = new RDTAck(waitingFor);
			
			// Serialize
			byte[] ackBytes = Serializer.toBytes(ackObject);
			
			
			DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, receivedPacket.getAddress(), receivedPacket.getPort());
			
			// Send with some probability of loss
			if(Math.random() > PROBABILITY){
				fromSender.send(ackPacket);
			}else{
				System.out.println("[X] Lost ack with sequence number " + ackObject.getPacket());
			}
			
			System.out.println("Sending ACK to seq " + waitingFor + " with " + ackBytes.length  + " bytes");
			

		}
		
		// Print the data received
		System.out.println(" ------------ DATA ---------------- ");
		
		for(RDTPacket p : received){
			for(byte b: p.getData()){
				System.out.print((char) b);
			}
		}
		
	}
	
	
}
