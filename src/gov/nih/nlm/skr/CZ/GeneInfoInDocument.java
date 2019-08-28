package gov.nih.nlm.skr.CZ;
import java.util.List;
import java.util.ArrayList;
public class GeneInfoInDocument {
	String PMID;
	List<SpeciesInfoInSentence> sentenceInfo;
	
	public GeneInfoInDocument(String p) {
		PMID = p;
	}
}
