import java.io.Serializable;


public class RDTAck implements Serializable{
	
	private int packet;

	public RDTAck(int packet) {
		super();
		this.packet = packet;
	}

	public int getPacket() {
		return packet;
	}

	public void setPacket(int packet) {
		this.packet = packet;
	}
	
	

}
