import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;


public class Sender {

	// Maximum Segment Size - Quantity of data from the application layer in the segment
	public static final int MSS = 4;

	// Probability of loss during packet sending
	public static final double PROBABILITY = 0.1;

	// Window size - Number of packets sent without acking
	public static final int WINDOW_SIZE = 2;
	
	// Time (ms) before REsending all the non-acked packets
	public static final int TIMER = 30;


	public static void main(String[] args) throws Exception{

		// Sequence number of the last packet sent (rcvbase)
		int lastSent = 0;
		
		// Sequence number of the last acked packet
		int waitingForAck = 0;

		// Data to be sent (you can, and should, use your own Data-> byte[] function here)
		byte[] fileBytes = "ABCDEFGHIJKLMNOPQRSTUVXZ".getBytes();

		System.out.println("Data size: " + fileBytes.length + " bytes");

		// Last packet sequence number
		int lastSeq = (int) Math.ceil( (double) fileBytes.length / MSS);

		System.out.println("Number of packets to send: " + lastSeq);

		DatagramSocket toReceiver = new DatagramSocket();

		// Receiver address
		InetAddress receiverAddress = InetAddress.getByName("localhost");
		
		// List of all the packets sent
		ArrayList<RDTPacket> sent = new ArrayList<RDTPacket>();

		while(true){

			// Sending loop
			while(lastSent - waitingForAck < WINDOW_SIZE && lastSent < lastSeq){

				// Array to store part of the bytes to send
				byte[] filePacketBytes = new byte[MSS];

				// Copy segment of data bytes to array
				filePacketBytes = Arrays.copyOfRange(fileBytes, lastSent*MSS, lastSent*MSS + MSS);

				// Create RDTPacket object
				RDTPacket rdtPacketObject = new RDTPacket(lastSent, filePacketBytes, (lastSent == lastSeq-1) ? true : false);

				// Serialize the RDTPacket object
				byte[] sendData = Serializer.toBytes(rdtPacketObject);

				// Create the packet
				DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, 9876 );

				System.out.println("Sending packet with sequence number " + lastSent +  " and size " + sendData.length + " bytes");

				// Add packet to the sent list
				sent.add(rdtPacketObject);
				
				// Send with some probability of loss
				if(Math.random() > PROBABILITY){
					toReceiver.send(packet);
				}else{
					System.out.println("[X] Lost packet with sequence number " + lastSent);
				}

				// Increase the last sent
				lastSent++;

			} // End of sending while
			
			// Byte array for the ACK sent by the receiver
			byte[] ackBytes = new byte[40];
			
			// Creating packet for the ACK
			DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);
			
			try{
				// If an ACK was not received in the time specified (continues on the catch clausule)
				toReceiver.setSoTimeout(TIMER);
				
				// Receive the packet
				toReceiver.receive(ack);
				
				// Unserialize the RDTAck object
				RDTAck ackObject = (RDTAck) Serializer.toObject(ack.getData());
				
				System.out.println("Received ACK for " + ackObject.getPacket());
				
				// If this ack is for the last packet, stop the sender (Note: gbn has a cumulative acking)
				if(ackObject.getPacket() == lastSeq){
					break;
				}
				
				waitingForAck = Math.max(waitingForAck, ackObject.getPacket());
				
			}catch(SocketTimeoutException e){
				// then send all the sent but non-acked packets
				
				for(int i = waitingForAck; i < lastSent; i++){
					
					// Serialize the RDTPacket object
					byte[] sendData = Serializer.toBytes(sent.get(i));

					// Create the packet
					DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, 9876 );
					
					// Send with some probability
					if(Math.random() > PROBABILITY){
						toReceiver.send(packet);
					}else{
						System.out.println("[X] Lost packet with sequence number " + sent.get(i).getSeq());
					}

					System.out.println("REsending packet with sequence number " + sent.get(i).getSeq() +  " and size " + sendData.length + " bytes");
				}
			}
			
		
		}
		
		System.out.println("Finished transmission");

	}

}
