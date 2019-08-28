package gov.nih.nlm.skr.CZ;

public class PositionMSUPair {
	int position;
	int MSU;
	public PositionMSUPair(int p, int M) {
		this.position = p;
		this.MSU = M;
	}
	
	public String toString() {
		return new String("(" + position + "," + MSU + ")");
	}
}
