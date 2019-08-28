package gov.nih.nlm.skr.CZ;

public class BratSentence {
	String text;
	int sentNum;
	int startPos;
	int endPos;
	String syntacticUnit;
	
	public BratSentence(String t,  int snum, int spos, int epos) {
		text = t;
		sentNum = snum;
		startPos = spos;
		endPos = epos;
	}
	
	public BratSentence(String t,  int snum, int spos, int epos, String sunit) {
		text = t;
		sentNum = snum;
		startPos = spos;
		endPos = epos;
		syntacticUnit = sunit; 
	}
	
	public String posInfo() {
		return new String(startPos + "-" + endPos);
	}
}
