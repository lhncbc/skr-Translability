package gov.nih.nlm.skr.CZ;

public class MSU {
	String concept;
	String morph;
	int locInSent;
	/*
	 * April 14 2017 Need the end position to prevent some cases where more than one word unit 
	 */
	int endPos;
	
	/*
	 * July 08 2017 Need the start position 
	 */
	int startPos;
	
	public MSU(String concept, String morph, int locInSent) {
		this.concept = concept;
		this.morph = morph;
		this.locInSent = locInSent;
	}
	
	public MSU(String concept, String morph, int locInSent, int endPos) {
		this.concept = concept;
		this.morph = morph;
		this.locInSent = locInSent;
		this.endPos = endPos;
	}
	
	/*
	 * July 08 2017 Need the start position 
	 */ 
	public MSU(String concept,  int startPos, int endPos) {
		this.concept = concept;
		this.startPos = startPos;
		this.endPos = endPos;
	}
}
