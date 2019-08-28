package gov.nih.nlm.skr.CZ;
import java.util.List;
public class GeneInfoInSentence {
	int sentNumber;
	String text;
	List<BratConcept> species;
	List<BratConcept> models;
	
	public GeneInfoInSentence(int snum, String t) {
		sentNumber = snum;
		text = t;
	}
}
