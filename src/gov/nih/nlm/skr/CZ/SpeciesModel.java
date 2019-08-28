package gov.nih.nlm.skr.CZ;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import gov.nih.nlm.ling.util.FileUtils;

public class SpeciesModel {
    List<BratConcept> species;
    List<BratConcept> models;
    int SPECIES_MODEL_DISTANCE = 1;
    private static Map<String, String> NORMALIZED_SECTION_LABELS = null;
    Properties prop = new Properties();
    BufferedReader speciesin = null;
    BufferedReader modelin = null;
    HashMap<String, String> speciesCUIMap = new HashMap<>();
    HashSet<String> speciesConceptCUISet = new HashSet<>();
    HashMap<String, String> speciesWordMap = new HashMap<>();
    HashMap<String, String> speciesWordMap2 = new HashMap<>();
    HashSet<String> speciesConceptWordSet = new HashSet<>();
    HashMap<String, String> modelCUIMap = new HashMap<>();
    HashSet<String> modelConceptCUISet = new HashSet<>();
    HashMap<String, String> modelWordMap = new HashMap<>();
    HashMap<String, String> modelWordMap2 = new HashMap<>();
    HashSet<String> modelConceptWordSet = new HashSet<>();

    private static SpeciesModel sm = null;

    public static SpeciesModel getInstance() throws IOException {
	if (sm == null) {
	    System.out.println("Initializing a Brat instance...");
	    sm = new SpeciesModel();
	}
	return sm;
    }

    public SpeciesModel() {
	try {
	    // prop.load(SpeciesModel.class.getClassLoader().getResourceAsStream("config.properties"));
	    prop = FileUtils.loadPropertiesFromFile("config.properties");
	    SPECIES_MODEL_DISTANCE = Integer.parseInt(prop.getProperty("SPECIES_MODEL_DISTANCE"));
	    speciesin = new BufferedReader(new FileReader(prop.getProperty("FIELD1_SPECIES")));

	    String line;
	    String CUI;
	    String preferredName;
	    while ((line = speciesin.readLine()) != null) {
		// String[] compo = line.split("\\|");
		String[] compo = line.split("\t");
		if (line.length() >= 10 && Utils.isCUI(compo[0])) {
		    CUI = compo[0].trim();
		    // List4CUIHS.add(CUI);
		    String substitute = null;
		    if (compo.length == 4)
			substitute = compo[1].trim();
		    // System.out.println(line + ":CUI = " + CUI + ":substitute = " + substitute);
		    // List4CUItable.put(CUI, substitute);
		    speciesCUIMap.put(CUI, substitute);
		    // System.out.println("List1 CUI substitute: (" +CUI + "),(" + substitute + ")");
		    speciesConceptCUISet.add(CUI);
		    if (compo[1].trim() != null) {
			preferredName = compo[1].trim();
			preferredName = preferredName.replaceAll("^\\s+|\\s+$", "");
			speciesWordMap.put(preferredName.toLowerCase(), substitute);
			speciesWordMap2.put(preferredName.toLowerCase(), preferredName);
			speciesConceptWordSet.add(preferredName);
			// System.out.println(
			//	"=== List1 preferred name substitute: (" + preferredName + "),(" + substitute + ")");
		    }
		} else {
		    preferredName = compo[0].trim();
		    // System.out.println(preferredName);
		    String substitute = compo[1].trim();
		    // List4Wordstable.put(preferredName, substitute);
		    speciesWordMap.put(preferredName.toLowerCase(), substitute);
		    speciesWordMap2.put(preferredName.toLowerCase(), preferredName);
		    speciesConceptWordSet.add(preferredName);
		    // System.out.println("List1 preferred name substitute: (" + preferredName + "),(" + substitute + ")");
		    // }
		    // rs.close();
		}
	    }

	    /*
	     * April 5 2017 Caroline requirement adds the representative name for models in
	     * addition to species
	     */
	    modelin = new BufferedReader(new FileReader(prop.getProperty("FIELD4_MODEL")));

	    while ((line = modelin.readLine()) != null) {
		// String[] compo = line.split("\\|");
		String[] compo = line.split("\t");
		if (line.length() >= 10 && Utils.isCUI(compo[0])) {
		    CUI = compo[0].trim();
		    // System.out.println(CUI);
		    // List4CUIHS.add(CUI);
		    String substitute = null;
		    if (compo.length == 2)
			substitute = compo[1];
		    // System.out.println(line + ":CUI = " + CUI + ":substitute = " + substitute);
		    // List4CUItable.put(CUI, substitute);
		    modelCUIMap.put(CUI, substitute);
		    modelConceptCUISet.add(CUI);

		} else {
		    preferredName = compo[0].trim();
		    // System.out.println(preferredName);
		    String substitute = compo[1];
		    // List4Wordstable.put(preferredName, substitute);
		    modelWordMap.put(preferredName.toLowerCase(), substitute);
		    modelWordMap2.put(preferredName.toLowerCase(), preferredName);
		    modelConceptWordSet.add(preferredName);
		    // System.out.println("List4 substitute: (" + preferredName + "),(" + substitute + ")");
		    // }
		    // rs.close();
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public SpeciesInfoInDocument generateSM(BratDocument doc) {
	// String syntacticUnit = doc.syntacticUnit;
	List<BratSentence> sentList = doc.sentenceList;
	List<BratConcept> bratConceptList = doc.conceptList;
	SpeciesInfoInDocument speciesInfoInDocument = new SpeciesInfoInDocument(doc.PMID);
	try {
	    speciesInfoInDocument.sentenceInfo = new ArrayList<>();
	    int offset = 0;
	    for (BratSentence sentence : sentList) { // do the following for each sentence.
		//			        	String[] wordArray = sentence.syntacticUnit.split("\\s+");
		//						List MSUList = Utils.ParseSyntacticUnit(sentence.syntacticUnit, wordArray);
		List MSUList = Utils.ParseSyntacticUnit(sentence);
		Iterator conit = speciesConceptWordSet.iterator();
		String text = sentence.text;
		SpeciesInfoInSentence speciesInfoInSentence = new SpeciesInfoInSentence(sentence.sentNum,
			sentence.text);
		// System.out.println(sentence.text);
		List<BratConcept> conceptList = new ArrayList<>();
		List<BratConcept> modelList = new ArrayList<>();
		/**
		 * Extracting species from Field1 of Caroline spec
		 */
		while (conit.hasNext()) {
		    String nextConcept = (String) conit.next();
		    if (text.toLowerCase().contains(nextConcept.toLowerCase())) {
			int conceptPos = 0;
			while (conceptPos >= 0 && conceptPos < text.length()) {
			    conceptPos = Utils.findConceptPosFromSent(nextConcept.toLowerCase(), text.toLowerCase(),
				    conceptPos);
			    if (conceptPos >= 0) {
				String foundConcept = text.substring(conceptPos, conceptPos + nextConcept.length());
				String substituteConcept = speciesWordMap.get(foundConcept.toLowerCase()); // find the substitute concept
				BratConcept bc = new BratConcept(foundConcept, null, sentence.sentNum,
					offset + conceptPos, offset + conceptPos + nextConcept.length());
				bc.substitute = substituteConcept;
				if (subsumedByGene(sentence, bc) || potentialBackgroundSentence(doc, sentence)
					|| humanInConclusionOnly(doc, sentence, bc)) {
				    conceptPos = conceptPos + nextConcept.length();
				    continue;
				}
				System.out.println("Extracting species : " + bc.toString());

				// conceptList.add(CP);
				conceptList = Utils.normalizeAndAdd(conceptList, bc); // July 25 2017, check subsumption relation
				conceptPos = conceptPos + nextConcept.length();
			    }
			}
		    }
		}
		for (BratConcept concept : bratConceptList) { // For concepts in input BratConcetpList, extract the species for matching CUI
		    //					    	  System.out.println("Processing Brat Concept = " + concept.toString()); 
		    if (concept.sentNum == sentence.sentNum) { // If the concept comes from the sentence
			Iterator conCUIit = speciesConceptCUISet.iterator();
			while (conCUIit.hasNext()) {
			    String nextConcept = (String) conCUIit.next();
			    if (concept.CUI != null && concept.CUI.equals(nextConcept)) {
				String substituteConcept = speciesCUIMap.get(concept.CUI);
				concept.substitute = substituteConcept;
				if (subsumedByGene(sentence, concept) || potentialBackgroundSentence(doc, sentence)
					|| humanInConclusionOnly(doc, sentence, concept))
				    continue;
				conceptList = Utils.normalizeAndAdd(conceptList, concept); // check subsumption relation and add CUI to the concept list
			    }
			}
			if (concept.semtype.equals("GNP_Species")) { // species extracted from GNormPlus		
			    concept.substitute = speciesWordMap.get(concept.name.toLowerCase());
			    if (concept.substitute != null) // If the species is in the Caroline field1 list
				conceptList = Utils.normalizeAndAdd(conceptList, concept);
			}
		    }

		}
		/*
		 * Extracting models from Field1 of Caroline spec
		 */
		Iterator modelit = modelConceptWordSet.iterator();
		while (modelit.hasNext()) {
		    String nextConcept = (String) modelit.next();
		    if (text.toLowerCase().contains(nextConcept.toLowerCase())) {
			int conceptPos = 0;
			while (conceptPos >= 0 && conceptPos < text.length()) {
			    conceptPos = Utils.findConceptPosFromSent(nextConcept, text, conceptPos);
			    if (conceptPos >= 0) {
				String foundConcept = text.substring(conceptPos, conceptPos + nextConcept.length());
				String substituteConcept = modelWordMap.get(foundConcept.toLowerCase()); // find the substitute concept
				BratConcept bc = new BratConcept(foundConcept, null, sentence.sentNum,
					offset + conceptPos, offset + conceptPos + nextConcept.length());
				if (subsumedByGene(sentence, bc)) {
				    conceptPos = conceptPos + nextConcept.length();
				    continue;
				}
				System.out.println("Extracting model : " + bc.toString());
				bc.substitute = substituteConcept;
				// conceptList.add(CP);
				modelList = Utils.normalizeAndAdd(modelList, bc); // July 25 2017, check subsumption relation
				conceptPos = conceptPos + nextConcept.length();
			    }
			}
		    }
		}
		for (BratConcept concept : bratConceptList) { // For concepts in input BratConcetpList, extract the species for matching CUI
		    if (concept.sentNum == sentence.sentNum) { // If the concept comes from the sentence	 
			Iterator modelCUIit = modelConceptCUISet.iterator();
			while (modelCUIit.hasNext()) {
			    String nextConcept = (String) modelCUIit.next();
			    if (concept.CUI != null && concept.CUI.equals(nextConcept)) {
				String substituteConcept = modelCUIMap.get(concept.CUI);
				concept.substitute = substituteConcept;
				if (subsumedByGene(sentence, concept) == false)
				    modelList = Utils.normalizeAndAdd(modelList, concept); // check subsumption relation and add CUI to the concept list			    				
			    }
			}
		    }
		}
		speciesInfoInSentence.species = conceptList;
		speciesInfoInSentence.models = modelList;
		if (conceptList.size() > 0 || modelList.size() > 0)
		    speciesInfoInDocument.sentenceInfo.add(speciesInfoInSentence);
		/*
		 * Check the MSU of species and Model and if it is 1, add that into speciesModel
		 * list
		 */
		int modelIndex = 0;
		List<Integer> posList = new ArrayList<>();
		List<ConceptLocation> CList = new ArrayList<>();
		for (BratConcept bc : conceptList) {
		    if (!speciesInfoInDocument.abstractLevelSpecies.contains(bc.substitute)) {
			speciesInfoInDocument.abstractLevelSpecies.add(bc.substitute);
			// if (!speciesInfoInDocument.abstractLevelSpecies.contains(bc.name)) {
			// 	speciesInfoInDocument.abstractLevelSpecies.add(bc.name);
		    }
		    // int wordPos = Utils.findWordPos(bc.startOffset, sentList.get(bc.sentNum-1).text);
		    int wordPos = Utils.findWordPos(bc.startOffset, doc.text);
		    ConceptLocation cl = new ConceptLocation(bc.substitute, bc.sentNum, wordPos, bc.startOffset,
			    bc.endOffset);
		    CList.add(cl);
		    /*
		     * If start position of a concept is not in the posList, add the position info
		     */
		    if (!posList.contains(bc.startOffset))
			posList.add(bc.startOffset);
		}
		List<ConceptLocation> MList = new ArrayList<>();
		for (BratConcept bc : modelList) {
		    if (!speciesInfoInDocument.abstractLevelModel.contains(bc.substitute)) {
			speciesInfoInDocument.abstractLevelModel.add(bc.substitute);
		    }
		    //int wordPos = Utils.findWordPos(bc.startOffset, sentList.get(bc.sentNum-1).text);
		    int wordPos = Utils.findWordPos(bc.startOffset, doc.text);
		    ConceptLocation cl = new ConceptLocation(bc.substitute, bc.sentNum, wordPos, bc.startOffset,
			    bc.endOffset);
		    MList.add(cl);
		    /*
		     * If start position of a concept is not in the posList, add the position info
		     */
		    if (!posList.contains(bc.startOffset))
			posList.add(bc.startOffset);
		}
		if (posList.size() > 0)
		    Collections.sort(posList);

		for (ConceptLocation cl : CList) {
		    for (ConceptLocation ml : MList) {
			if (cl.sentNum == ml.sentNum) {
			    //									  wordArray = sentList.get(cl.sentNum-1).text.split("\\s+");
			    //									  List sunit = Utils.ParseSyntacticUnit(sentList.get(cl.sentNum-1).syntacticUnit, wordArray);
			    List sunit = Utils.ParseSyntacticUnit(sentList.get(cl.sentNum - 1));
			    // System.out.println("Species: " + cl.toString());
			    // System.out.println("Model:   " + ml.toString());
			    if (Utils.msuDistance(cl, ml, sunit) <= SPECIES_MODEL_DISTANCE) {
				String speciesModel = new String(cl.concept + "/" + ml.concept);
				if (!speciesInfoInDocument.abstractLevelSpeciesModel.contains(speciesModel))
				    speciesInfoInDocument.abstractLevelSpeciesModel.add(speciesModel);
			    }
			}

			// Utils.msuDistance(modelIndex, modelIndex, MSUList)
		    }
		}
		offset = offset + sentence.text.length();
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}
	return speciesInfoInDocument;
    }

    public void writeToBrat(SpeciesInfoInDocument sidoc) {
	List<SpeciesInfoInSentence> sentenceInfoList = sidoc.sentenceInfo;
	List ALSpecies = sidoc.abstractLevelSpecies;
	List ALModels = sidoc.abstractLevelModel;
	List ALSpeciesModels = sidoc.abstractLevelSpeciesModel;

	for (SpeciesInfoInSentence sis : sentenceInfoList) {

	}
    }

    // If the species/model string is subsumed by larger terms, they should be ignored.
    // This is not the ideal way of doing it. Normally, we should run species/model module
    // and gene/protein module in parallel, and annotations from one should inform the other.
    // Temporarily, I implemented this. 
    // Also, unwantedMappings is not final.
    private boolean subsumedByGene(BratSentence sentence, BratConcept bc) {
	Map<String, List<String>> unwantedMappings = new HashMap<>();
	unwantedMappings.put("cell line", Arrays.asList("glial cell line-derived neurotrophic factor",
		"glial cell line derived neurotrophic factor"));
	String text = sentence.text;
	if (unwantedMappings.containsKey(bc.name)) {
	    List<String> potentialSubsuming = unwantedMappings.get(bc.name);
	    for (String s : potentialSubsuming) {
		List<Integer> indices = getStartPositions(s, text);
		if (indices.size() == 0)
		    continue;
		for (int ind : indices) {
		    int end = ind + s.length();
		    if (subsumes(ind + sentence.startPos, end + sentence.startPos, bc.startOffset, bc.endOffset))
			return true;
		}
	    }
	}
	return false;

    }

    private List<Integer> getStartPositions(String s, String text) {
	List<Integer> poss = new ArrayList<>();
	int index = 0;
	int sind = text.toLowerCase().indexOf(s, index);
	while (sind >= 0) {
	    poss.add(sind);
	    index = sind + s.length();
	    sind = text.toLowerCase().indexOf(s, index);
	}
	return poss;
    }

    private boolean subsumes(int a, int b, int c, int d) {
	return (a <= c && a < b && c < d && d <= b);
    }

    public String getNormalizedSectionName(BratDocument document, BratSentence sentence) {
	if (NORMALIZED_SECTION_LABELS == null) {
	    try {
		//					  loadNormalizedSectionLabels("data/Structured-Abstracts-Labels-110613.txt");
		loadNormalizedSectionLabels(prop.getProperty("STRUCTURED_ABSTRACT_LABEL_FILE"));
	    } catch (Exception e) {
		e.printStackTrace();
		System.err.println("Cannot load section labels.");
		return null;
	    }
	}
	int ind = document.sentenceList.indexOf(sentence);
	boolean title = false;
	int newlineInd = document.text.indexOf("\n");
	if (sentence.endPos <= newlineInd || (sentence.endPos > newlineInd && sentence.startPos <= newlineInd))
	    title = true;
	if (title)
	    return "TITLE";
	String sect = "ABSTRACT";

	for (int i = ind; i >= 0; i--) {
	    BratSentence senti = document.sentenceList.get(i);
	    int colonInd = senti.text.indexOf(":");
	    if (colonInd == -1)
		continue;
	    String sub = senti.text.substring(0, colonInd).trim();
	    if (sub.matches("[A-Z ]+")) {
		String norm = NORMALIZED_SECTION_LABELS.get(sub);
		sect = (norm == null ? sub : norm);
		break;
	    }
	}
	return sect;
    }

    private static int getFirstAbstractSentenceIndex(BratDocument document) {
	int newlineInd = document.text.indexOf("\n");
	List<BratSentence> sentences = document.sentenceList;
	for (int i = 0; i < sentences.size(); i++) {
	    BratSentence s = sentences.get(i);
	    if (s.startPos > newlineInd)
		return i;
	}
	return -1;
    }

    public static void loadNormalizedSectionLabels(String filename) throws Exception {
	List<String> lines = FileUtils.linesFromFile(filename, "UTF-8");
	if (NORMALIZED_SECTION_LABELS == null) {
	    NORMALIZED_SECTION_LABELS = new HashMap<>();
	}
	for (String line : lines) {
	    String[] els = line.split("[|]");
	    NORMALIZED_SECTION_LABELS.put(els[0], els[1]);
	}
    }

    private boolean potentialBackgroundSentence(BratDocument document, BratSentence sentence) {
	String sectionName = getNormalizedSectionName(document, sentence);
	if (sectionName.equals("BACKGROUND"))
	    return true;
	List<BratSentence> sentences = document.sentenceList;
	int ssize = sentences.size();
	int ind = getFirstAbstractSentenceIndex(document);
	int sind = document.sentenceList.indexOf(sentence);
	if (ind == -1)
	    return false;
	//			int abstractSentSize = ssize - ind;
	if (sectionName.equals("ABSTRACT"))
	    return (ind == sind);
	//				return (ind == sind || (double) (sind-ind) < (double)(0.15 * abstractSentSize));
	return false;
    }

    private boolean potentialConclusionSentence(BratDocument document, BratSentence sentence) {
	String sectionName = getNormalizedSectionName(document, sentence);
	if (sectionName.equals("CONCLUSIONS"))
	    return true;
	List<BratSentence> sentences = document.sentenceList;
	int ssize = sentences.size();
	int ind = getFirstAbstractSentenceIndex(document);
	int sind = document.sentenceList.indexOf(sentence);
	if (ind == -1)
	    return false;
	if (sind == ssize - 1)
	    return true;
	int abstractSentSize = ssize - ind;
	if (sectionName.equals("ABSTRACT"))
	    return (sind == ssize - 1 || sind - ind > 0.85 * abstractSentSize);
	return false;
    }

    private boolean humanInConclusionOnly(BratDocument document, BratSentence sentence, BratConcept concept) {
	return (potentialConclusionSentence(document, sentence) && concept.substitute.equals("Human"));
    }

    static public void main(String[] argv) {
	String aLine = null;
	try {
	    // Properties prop = new Properties(); 
	    //  prop.load(Outcome.class.getClassLoader().getResourceAsStream("config.properties"));
	    Brat brat = Brat.getInstance();
	    // Brat brat = Brat.getInstance("Q:\\LHC_Projects\\Caroline\\testset\\semtypeFullName.txt");
	    // sm = SpeciesModel.getInstance("Q:\\LHC_Projects\\Caroline\\data\\Field1ListSpecies_06222017.txt", "Q:\\LHC_Projects\\Caroline\\data\\Field4ListModel_04052017.txt");
	    sm = SpeciesModel.getInstance();
	    // BratDocument doc = brat.readABratFile("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\26545632.ann", "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\20973483.txt");
	    // BratDocument doc = brat.readABratFile("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\26522958.ann", "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\26522958.txt");
	    // BratDocument doc = brat.readABratFile("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\16464239.ann", "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\16464239.txt");
	    // BratDocument doc = brat.readABratFile("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat3\\12525720.ann", "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat3\\12525720.txt");
	    BratDocument doc = brat.readABratFile("Z:TranslationResearch\\3yearsbrat2\\23028814.ann",
		    "Z:TranslationResearch\\3yearsbrat2\\23028814.txt");
	    // BratDocument doc = brat.readABratFile("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\11481696.ann", "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\11481696.txt");
	    // BratDocument doc = brat.readABratFile("C:\\TranslationalResearch\\3yearsbrat\\17353071.ann", "C:\\TranslationalResearch\\3yearsbrat\\17353071.txt");
	    // PrintWriter out = new PrintWriter(new File("Q:\\LHC_Projects\\Caroline\\NEW_2017\\MSUError.16532454.out"));
	    SpeciesInfoInDocument speciesDoc = sm.generateSM(doc);
	    brat.writeSpeciesInfoToABratFile(doc, speciesDoc, "C:\\TranslationalResearch\\3yearsSpecies_2");
	    // brat.writeABratFile(doc, speciesDoc, "Q:\\LHC_Projects\\Caroline\\NEW_2017\\brat_sp4");

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
