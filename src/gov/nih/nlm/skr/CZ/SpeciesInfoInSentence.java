package gov.nih.nlm.skr.CZ;
import java.util.List;
public class SpeciesInfoInSentence {
	int sentNumber;
	String text;
	List<BratConcept> species;
	List<BratConcept> models;
	
	public SpeciesInfoInSentence(int snum, String t) {
		sentNumber = snum;
		text = t;
	}
}
