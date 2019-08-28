package gov.nih.nlm.skr.CZ;
import java.util.List;
import java.util.ArrayList;
public class SpeciesInfoInDocument {
	String PMID;
	List<SpeciesInfoInSentence> sentenceInfo;
	List<String> abstractLevelSpecies;
	List<String> abstractLevelModel;
	List<String> abstractLevelSpeciesModel;
	
	public SpeciesInfoInDocument(String p) {
		PMID = p;
		abstractLevelSpecies = new ArrayList();
		abstractLevelModel = new ArrayList();
		abstractLevelSpeciesModel = new ArrayList();
	}
}
