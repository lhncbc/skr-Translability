package gov.nih.nlm.skr.CZ;
import java.util.List;

public class BratDocument {
	String PMID;
	List<BratSentence> sentenceList;
	List<BratConcept> conceptList;
	List<BratPredication> predicationList;
	String text;
	public BratDocument(String id, List<BratSentence> sentList, List<BratConcept> concList, List<BratPredication> predList) {
		PMID = id;
		sentenceList = sentList;
		conceptList = concList;
		predicationList = predList;
	}
	
	public BratDocument(String id, List<BratSentence> sentList, List<BratConcept> concList, List<BratPredication> predList, String t) {
		PMID = id;
		sentenceList = sentList;
		conceptList = concList;
		predicationList = predList;
		text = t;
	}
	
}
