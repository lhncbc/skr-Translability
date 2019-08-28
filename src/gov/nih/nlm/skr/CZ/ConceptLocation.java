package gov.nih.nlm.skr.CZ;

/* public class ConceptLocation {
	String concept;
	String name;
	int sentNum;
	int wordPos;
	public ConceptLocation(String c, String na, int n, int p) {
		concept = c;
		name = na;
		sentNum = n;
		wordPos = p;
	}
} */


public class ConceptLocation {
	String concept;
	int sentNum;
	int wordPos;
	int startPos;
	int endPos;
	
	public ConceptLocation(String c, int n, int p, int s, int e) {
		concept = c;
		sentNum = n;
		wordPos = p;
		startPos = s;
		endPos = e;
	}
	
	public String toString() {
		return concept +":" + sentNum + ":" + wordPos + ":" + startPos + ":" + endPos;
	}

}
