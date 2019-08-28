package gov.nih.nlm.skr.CZ;

/*
 * Intervention class that extracts Intervention from Brat files
 * Author: Dongwook Shin
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.util.FileUtils;

public class Intervention {
    List<BratConcept> genes;
    static Brat brat = null;
    /** Initialize classes for accesing MeSH info **/
    MeSH mesh = new MeSH();
    /** The remote host for Pubtator */
    static String host = null;
    /** The same port as used by the server */
    static String ivftablepath = null;
    static String ivfindexpath = null;
    static String ivfindexname = null;
    static int port = 0;
    static Socket socket = null;
    static List<String> intervention_filterout_term_set = new ArrayList<>();
    //	static List<String> intervention_term_list = new ArrayList<>();
    static Map<String, String> intervention_term_list = new HashMap<>();
    static List<String> genetic_intervention_term_list = new ArrayList<>();

    static Pattern structuredAbstractPattern = Pattern.compile("^[\\p{Lu}]+:");

    // DELETE LATER.
    static List<String> GENE_INTERVENTION_TERMS = Arrays.asList("inhibition", "inhibitor", "inhibitors", "stimulation",
	    "stimulator", "stimulators", "activation", "activator", "activators", "antagonism", "antagonist",
	    "antagonists", "agonism", "agonist", "agonists", "modulation", "modulator", "modulators", "ligand",
	    "ligands", "allosteric modulator");

    private static Comparator<String> STRING_LENGTH_COMP = new Comparator<String>() {
	@Override
	public int compare(String a, String b) {
	    if (a.length() > b.length())
		return -1;
	    else if (a.length() < b.length())
		return 1;
	    return a.compareTo(b);
	}
    };

    private static Map<String, String> NORMALIZED_SECTION_LABELS = null;

    private static Intervention inter = null;

    private static Gene gene = null;

    // DELETE LATER.
    private static void loadNormalizedSectionLabels(String filename) throws Exception {
	List<String> lines = FileUtils.linesFromFile(filename, "UTF-8");
	if (NORMALIZED_SECTION_LABELS == null) {
	    NORMALIZED_SECTION_LABELS = new HashMap<>();
	}
	for (String line : lines) {
	    String[] els = line.split("[|]");
	    NORMALIZED_SECTION_LABELS.put(els[0], els[1]);
	}
    }

    public static Intervention getInstance() throws Exception {
	if (inter == null) {
	    inter = new Intervention();
	    brat = Brat.getInstance();

	    List<String> filteroutTermList = FileUtils
		    .linesFromFile(Brat.prop.getProperty("INTERVENTION_FILTEROUT_TERM"), "UTF-8");
	    for (String line : filteroutTermList) {
		if (line.startsWith("#"))
		    continue;
		intervention_filterout_term_set.add(line.trim().toLowerCase());
	    }
	    Collections.sort(intervention_filterout_term_set, STRING_LENGTH_COMP);

	    List<String> interventionTermList = FileUtils
		    .linesFromFile(Brat.prop.getProperty("INTERVENTION_ESSENTIAL_TERM"), "UTF-8");
	    for (String line : interventionTermList) {
		if (line.startsWith("#"))
		    continue;
		String[] spl = line.split("[\t]");
		String name = spl[0].trim().toLowerCase();
		String id = null;
		if (spl.length > 1) {
		    id = spl[1].trim();
		}
		intervention_term_list.put(name.trim().toLowerCase(), id);
	    }
	    //			Collections.sort(intervention_term_list,STRING_LENGTH_COMP);

	    List<String> geneticInterventionTermList = FileUtils
		    .linesFromFile(Brat.prop.getProperty("GENETIC_INTERVENTION_TERM"), "UTF-8");
	    for (String line : geneticInterventionTermList) {
		genetic_intervention_term_list.add(line.trim().toLowerCase());
	    }
	    Collections.sort(genetic_intervention_term_list, STRING_LENGTH_COMP);

	    host = brat.prop.getProperty("CHEM_SERVER_NAME");
	    port = Integer.parseInt(Brat.prop.getProperty("CHEM_SERVER_PORT"));
	    System.out.println(host + " : " + port);
	    try {
		socket = ChemlistemClient.setupSocket(host, port);
	    } catch (Exception e) {
		System.out.println("ERROR: Chem server is not running ...");
	    }
	    ivftablepath = Brat.prop.getProperty("IVF_TABLE_PATH");
	    ivfindexpath = Brat.prop.getProperty("IVF_INDEX_PATH");
	    ivfindexname = Brat.prop.getProperty("IVF_INDEX_NAME");
	}
	if (gene == null) {
	    gene = Gene.getInstance();
	}
	if (NORMALIZED_SECTION_LABELS == null) {
	    try {
		//			  loadNormalizedSectionLabels("data/Structured-Abstracts-Labels-110613.txt");
		loadNormalizedSectionLabels(Brat.prop.getProperty("STRUCTURED_ABSTRACT_LABEL_FILE"));
	    } catch (Exception e) {
		e.printStackTrace();
		System.err.println("Cannot load section labels.");
		return null;
	    }
	}

	return inter;
    }

    // DELETE LATER.
    public static boolean inIntroduction(List<BratSentence> sentList, BratSentence sent, int titleLength) {
	if (sent.endPos <= titleLength)
	    return false;
	int sNum = sent.sentNum;
	int lastTitle = 0;
	for (BratSentence bs : sentList) {
	    int end = bs.endPos;
	    if (end <= titleLength) {
		lastTitle = bs.sentNum;
	    } else
		break;
	}
	boolean structured = false;
	if (lastTitle > 0) {
	    BratSentence first = sentList.get(lastTitle);
	    if (first.sentNum == sNum)
		return true;
	    String firstText = first.text;
	    if (firstText.contains(":")) {
		String stTitle = firstText.substring(0, firstText.indexOf(":")).toUpperCase();
		if (NORMALIZED_SECTION_LABELS.containsKey(stTitle)
			|| structuredAbstractPattern.matcher(firstText).find())
		    structured = true;
	    }
	    if (structured) {
		for (int i = lastTitle + 1; i < sentList.size(); i++) {
		    BratSentence p = sentList.get(i);
		    String pText = p.text;
		    if (pText.contains(":")) {
			String stTitle = pText.substring(0, pText.indexOf(":")).toUpperCase();
			if (NORMALIZED_SECTION_LABELS.containsKey(stTitle)
				|| structuredAbstractPattern.matcher(pText).find())
			    return false;
		    } else if (p.sentNum == sNum)
			return true;
		}
	    }
	    int numAbsSents = sentList.size() - lastTitle;
	    if (numAbsSents < 5)
		return false;
	    double ratio = (double) (sNum - lastTitle) / (double) numAbsSents;
	    if (ratio <= 0.2)
		return true;
	}
	return false;
    }

    public static BratSentence getSpanSentence(List<BratSentence> sentList, int begin, int end) {
	for (BratSentence bs : sentList) {
	    if (bs.startPos <= begin && end <= bs.endPos)
		return bs;
	}
	return null;
    }

    /**
     * The way it currently works is: - Check whether we can identify something in
     * the title with chem server. - Check GNormPlus entities in the title that do
     * not overlap with the chemserver annotations. - Check essential term list in
     * the title that do not overlap with the first two. - If nothing is found in
     * the title, use UMLS concepts with specific semtypes
     * 
     * This method can further be simplified, by removing the experimental stuff,
     * such as checking the Introduction section, or using regular expressions, etc.
     * Some of this is already commented out.
     * 
     * @param doc
     *            The document to process
     * @param text
     *            The documen text
     * @param titleLength
     *            Length of the title
     * @return a set of results
     */
    public InterventionResult generateIntervention(BratDocument doc, String text, int titleLength) {
	// String syntacticUnit = doc.syntacticUnit;

	boolean titleOnly = true;
	;
	boolean titleAndFirstOnly = false;

	Map<Span, BratConcept> seen = new HashMap<>();
	Set<String> seenStrings = new HashSet<>();

	List<BratConcept> bratConceptList = doc.conceptList;
	List<BratSentence> sentList = doc.sentenceList;
	List<BratConcept> semrepList = new ArrayList<>();
	List<BratConcept> GNormList = new ArrayList<>();
	List<BratConcept> mustConceptList = new ArrayList<>();
	InterventionResult iresult = null;

	//		List<BratConcept> geneConcepts = gene.generateGene(doc);

	try {
	    List<BratConcept> chemConceptList = new ArrayList<>();
	    // HashMap<String, String> OMIMMap =
	    // ReadConceptList.readOMIMGeneList("Q:\\LHC_Projects\\Caroline\\data\\parkinson_omim_genes_expanded_06162017.txt");

	    // process with ChemServer on the basis of the entire document
	    String chemResult = null;
	    try {
		ChemlistemClient.converse(socket, text);
	    } catch (Exception e) {
		chemResult = new String("");
	    }
	    int si = 0;
	    for (BratSentence bs : sentList) {
		si++;
		System.out.println("\tsentence " + si + " : " + bs.posInfo());
	    }
	    if (chemResult.length() > 1) {
		String[] chemArray = chemResult.split("\n");
		for (int i = 0; i < chemArray.length; i++) {
		    String[] chemInfo = chemArray[i].split("\\|");
		    if (chemInfo[1].equals(""))
			continue;
		    String[] chemlocInfo = chemInfo[2].split(";");
		    if (!intervention_filterout_term_set.contains(chemInfo[0].toLowerCase())) { // If the term does not belong to intervention_filterout_term list
			for (int locIndex = 0; locIndex < chemlocInfo.length; locIndex++) {
			    String[] chemlocBoundary = chemlocInfo[locIndex].split(",");
			    int start = Integer.parseInt(chemlocBoundary[0]);
			    int end = Integer.parseInt(chemlocBoundary[1]);
			    BratSentence sent = getSpanSentence(sentList, start, end);
			    if ((start >= titleLength) && (titleOnly
				    || (titleAndFirstOnly && !inIntroduction(sentList, sent, titleLength))))
				continue;
			    // find correct sentence number
			    int correctSentNum = 1;
			    for (BratSentence bs : sentList) {
				int st = bs.startPos;
				int ed = bs.endPos;
				if (start >= st && end <= ed) {
				    correctSentNum = bs.sentNum;
				    break;
				}
			    }
			    // Use Willie's Chem Server
			    BratConcept chemical = new BratConcept(chemInfo[0], chemInfo[1], correctSentNum, start,
				    end);
			    chemConceptList = StringUtils.subsumeCheckAndAdd(chemConceptList, chemical);
			    System.out.println("\tChemServer concept: " + chemical.toString());
			}
		    }
		}
		Collections.sort(chemConceptList, new BratConceptComparator<BratConcept>());
	    }
	    for (BratConcept bc : chemConceptList) {
		seen.put(new Span(bc.startOffset, bc.endOffset), bc);
		seenStrings.add(bc.name);
	    }

	    /*
	     * Do the following for each sentence
	     */
	    int chemListIndex = 0;
	    int bratListIndex = 0;
	    int sentIndex = 0;
	    for (BratSentence bs : sentList) {
		int sentStart = bs.startPos;
		int sentEnd = bs.endPos;
		if ((sentStart >= titleLength)
			&& (titleOnly || (titleAndFirstOnly && !inIntroduction(sentList, bs, titleLength))))
		    continue;
		boolean chemicalFoundInSentence = false;
		while (chemListIndex < chemConceptList.size()) {
		    BratConcept chemConcept = chemConceptList.get(chemListIndex);
		    int chemConceptStart = chemConcept.startOffset;
		    int chemConceptEnd = chemConcept.endOffset;
		    // If at least chemical found in the sentence, set
		    // chemicalFoundInSetnece to true and stop the loop
		    if (chemConceptStart >= sentStart && chemConceptEnd <= sentEnd) {
			chemicalFoundInSentence = true;
			/*
			 * * Feb 22 2018 Taking the offset info from Chem Server replace it with the
			 * exact sentence substring
			 */
			String realChemString = text.substring(chemConceptStart, chemConceptEnd);
			chemConcept.name = realChemString;
			/*
			 * if(!realChemString.equals(chemConcept.name)) {
			 * System.out.println("CHEM NAME CHANGE : " + chemConcept.name + " ---> " +
			 * realChemString); chemConcept.name = realChemString;
			 * 
			 * }
			 */

			chemListIndex++;
			// break;
			// If no checmical found in the sentence, stop it
		    } else if (chemConceptStart > sentEnd) {
			break;
		    } else if (chemConceptEnd < sentStart) // If the current chemical belong to the previous sentence, skip it
			chemListIndex++;
		} // end of while

		// For GNormPlus, we only look at the title, regardless
		boolean gNormInSentence = false;
		//				if (!chemicalFoundInSentence && !mustConceptInSentence && !semrepFoundInSentence && sentStart < titleLength) { 
		if (sentStart < titleLength) {
		    int bratGNPIndex = 0;
		    while (bratGNPIndex < bratConceptList.size()) {
			BratConcept bratConcept = bratConceptList.get(bratGNPIndex);
			int bratConceptStart = bratConcept.startOffset;
			int bratConceptEnd = bratConcept.endOffset;
			if (existsOverlapping(bratConcept, seen)) {
			    bratGNPIndex++;
			    continue;
			}
			if (bratConceptStart >= sentStart && bratConceptEnd <= sentEnd) {
			    if (bratConcept.semtype.equals("GNP_Gene") || bratConcept.semtype.equals("Gene")) {
				//	    	    			if (seen.containsKey(new Span(bratConceptStart,bratConceptEnd))) {bratListIndex++;continue;}
				if (intervention_filterout_term_set.contains(bratConcept.name.toLowerCase())) {
				    bratGNPIndex++;
				    continue;
				}
				// GNormList.add(bratConcept); // Add the concept into the final list
				GNormList = StringUtils.subsumeCheckAndAdd(GNormList, bratConcept);
				System.out.println("\t GNormPlus concept: " + bratConcept.toString());
				gNormInSentence = true;
				bratGNPIndex++;
			    } else
				bratGNPIndex++;
			} else
			    break;
		    }
		}
		for (BratConcept g : GNormList) {
		    seen.put(new Span(g.startOffset, g.endOffset), g);
		    seenStrings.add(g.name);
		}

		boolean mustConceptInSentence = false;
		List<String> interventionEssentialTerms = new ArrayList<>(intervention_term_list.keySet());
		Collections.sort(interventionEssentialTerms, STRING_LENGTH_COMP);
		for (String mustConceptStr : interventionEssentialTerms) {
		    List<Span> spans = Utils.findConceptPosFromSent(mustConceptStr, bs.text, true);
		    for (Span sp : spans) {
			if (existsOverlapping(sp, seen))
			    continue;
			String term = bs.text.substring(sp.getBegin(), sp.getEnd());
			// find correct sentence number
			int correctSentNum = 1;
			for (BratSentence bs1 : sentList) {
			    int st = bs1.startPos;
			    int ed = bs1.endPos;
			    if (sp.getBegin() >= st && sp.getEnd() <= ed) {
				correctSentNum = bs.sentNum;
				break;
			    }
			}
			BratConcept mustConcept = new BratConcept(term, new String(""), correctSentNum,
				bs.startPos + sp.getBegin(), bs.startPos + sp.getEnd());
			String id = intervention_term_list.get(mustConceptStr);
			if (id != null)
			    mustConcept.CUI = id;
			if (seen.containsKey(new Span(mustConcept.startOffset, mustConcept.endOffset)))
			    continue;
			System.out.println("\t Essential concept: " + mustConcept.toString());
			mustConceptList.add(mustConcept);
			seenStrings.add(mustConcept.name);
			seen.put(new Span(mustConcept.startOffset, mustConcept.endOffset), mustConcept);
			mustConceptInSentence = true;
		    }
		}

		boolean semrepFoundInSentence = false;
		// If nothing is found, consult UMLS
		if (!chemicalFoundInSentence && !mustConceptInSentence && !gNormInSentence) {
		    // BratSentence bsthis = sentList.get(sentIndex);
		    while (bratListIndex < bratConceptList.size()) {
			BratConcept bratConcept = bratConceptList.get(bratListIndex);
			int bratConceptStart = bratConcept.startOffset;
			int bratConceptEnd = bratConcept.endOffset;
			if (bratConceptStart >= sentStart && bratConceptEnd <= sentEnd) {
			    //	 if (hasProperSemRepType(bratConcept.semtype, bratConcept.name)) {
			    //	    			if (seen.containsKey(new Span(bratConceptStart,bratConceptEnd))) {bratListIndex++;continue;}
			    if (intervention_filterout_term_set.contains(bratConcept.name.toLowerCase())) {
				bratListIndex++;
				continue;
			    }
			    if (hasProperSemRepType(bratConcept.semtype, false)) {
				//							if ((!chemicalFoundInSentence && !mustConceptInSentence && hasProperSemRepType(bratConcept.semtype,false)) ||
				//								((chemicalFoundInSentence || mustConceptInSentence) && hasProperSemRepType(bratConcept.semtype))) {
				semrepFoundInSentence = true;
				System.out.println("\tSemrep concept: " + bratConcept.toString());
				semrepList = StringUtils.subsumeCheckAndAdd(semrepList, bratConcept);
				// semrepList.add(bratConcept); // Add the concept into the final list
				bratListIndex++;
			    } else
				bratListIndex++;
			} else if (bratConceptEnd < sentStart) {
			    // if the concept belongs to the previous sentence, add those
			    /*
			     * // if (hasProperSemRepType(bratConcept.semtype, bratConcept.name)) { // if
			     * (seen.containsKey(new Span(bratConceptStart,bratConceptEnd)))
			     * {bratListIndex++;continue;} if
			     * (intervention_filterout_term_set.contains(bratConcept.name.toLowerCase())) {
			     * bratListIndex++;continue;} if (hasProperSemRepType(bratConcept.semtype)) {
			     * semrepFoundInSentence = true; // semrepList.add(bratConcept); // Add the
			     * concept into the final list semrepList =
			     * StringUtils.subsumeCheckAndAdd(semrepList, bratConcept);
			     * System.out.println("\tSemrep concept: " + bratConcept.toString());
			     * bratListIndex++; } else
			     */
			    bratListIndex++;
			} else
			    break;// end of if
		    } // while
		} // end of outer if

		// Becomes useless after pushing SemRep results to the end
		/*
		 * for (BratConcept s: semrepList) { seen.put(new
		 * Span(s.startOffset,s.endOffset), s); seenStrings.add(s.name); }
		 */

		// regular expression based
		/*
		 * for (BratConcept conc: geneConcepts) { int num = conc.getSentNum();
		 * BratSentence sent = doc.sentenceList.get(num-1); if (sent.sentNum !=
		 * bs.sentNum) continue; int sentInd = conc.startOffset - sent.startPos;
		 * 
		 * for (String s : genetic_intervention_term_list) { Pattern fwdPattern =
		 * Pattern.compile(Pattern.quote(conc.name) + "\\s+" + Pattern.quote(s) +
		 * "\\b",Pattern.CASE_INSENSITIVE); Matcher m =
		 * fwdPattern.matcher(sent.text.toLowerCase()); Span ntSp = null; while
		 * (m.find()) { if (m.start() == sentInd) { ntSp = new Span(sentInd,m.end()); }
		 * } if (ntSp == null) { Pattern bckPattern = Pattern.compile(Pattern.quote(s) +
		 * "\\s+" + "of" + "\\s" + Pattern.quote(conc.name),Pattern.CASE_INSENSITIVE); m
		 * = bckPattern.matcher(sent.text.toLowerCase()); while (m.find()) { if
		 * (m.start() == sentInd) { ntSp = new Span(sentInd,m.end()); } } } if (ntSp ==
		 * null) continue; String term =
		 * sent.text.substring(ntSp.getBegin(),ntSp.getEnd());
		 * System.out.println("Term: " + term); BratConcept geneIntervention = new
		 * BratConcept(term,new
		 * String(""),ntSp.getBegin()+sent.startPos,ntSp.getEnd()+sent.startPos);
		 * mustConceptList.add(geneIntervention); } }
		 */

		sentIndex++; // repeat this for next sentence
	    } // end of outer for

	    // When doing document level annotation this would be redundant, so comment out.

	    //for some reason or another, one of the methods can miss a term instance, while finding others
	    // for example, Chem Server found 'N-0923', but missed '(N-0923' due to tokenization (I think).
	    // this method tries to address this, but keeping track of all identified term instances
	    // and completing the search for them.
	    /*
	     * for (BratSentence bs: sentList) { int sentStart = bs.startPos; if ((sentStart
	     * >= titleLength) && (titleOnly || (titleAndFirstOnly &&
	     * !inIntroduction(sentList,bs,titleLength)))) continue; for (String s
	     * :seenStrings) { List<Span> spans = Utils.findConceptPosFromSent(s, bs.text,
	     * true); for (Span sp: spans) { String term =
	     * bs.text.substring(sp.getBegin(),sp.getEnd()); BratConcept mustConcept = new
	     * BratConcept(term, new String(""), bs.startPos + sp.getBegin(), bs.startPos +
	     * sp.getEnd()); // if (seen.containsKey(new
	     * Span(mustConcept.startOffset,mustConcept.endOffset)))continue; if
	     * (existsOverlapping(mustConcept,seen)) continue;
	     * System.out.println("\t Ad-hoc concept for completion: "
	     * +mustConcept.toString()); mustConceptList.add(mustConcept); } } }
	     */

	    /**
	     * 1. Open the MeSh browser https://www.nlm.nih.gov/mesh/MBrowser.html 2. Paste
	     * the text term associated with CUI in to search box; select Find exact term
	     * e.g. C0085154|Nizatidine 3. Look for Pharm action field in returned page 4.
	     * If found, Enter term in Pharm action field of MeSH in Field 2. if there are
	     * several Pharm action fields, create a new line for each and enter them all 5.
	     * If Pharm action field not found, examine MeSH tree struture 6. Enter term
	     * directly above search term in Field 2 e.g. search term neurturin enter Glial
	     * Cell Line-Derived Neurotrophic Factor [D12.644.276.860.381.500]
	     * 
	     * UPDATED: Oct 24 2017 Abstract level intervention 1. In All row, list each
	     * unique intervention identified above on its own row
	     * 
	     * Abstract level Mechanism of action For each unique term in the "all" rows: 1.
	     * Open the MeSh browser https://www.nlm.nih.gov/mesh/MBrowser.html 2. Paste the
	     * text term associated with CUI in to search box; select Find exact term Note:
	     * if the intervention is gngm or aap, do not paste in MeSH - just enter a 0 in
	     * Field 2 3. Look for fields: Pharm action = yes = Enter in Field 2 = stop. 4.
	     * If found, Enter term in Field 2. if there are several Pharm action fields,
	     * create a new line for each and enter them all. If pharm action = no = go to
	     * Mesh tree structure 5. If Mesh tree structure = yes = Enter term directly
	     * above search term in Field 2, then stop. If Mesh tree structure = no, go
	     * to� Indexing information 6. Indexing information = yes = enter term in
	     * Field 2, then stop 7. If no indexing information is present, enter 0 8. In
	     * All row, list each unique MOA identified above on its own row
	     * 
	     */
	    boolean MOAfound = false;
	    List<BratConcept> allConcepts = new ArrayList<>(chemConceptList);
	    List<InterventionConcept> interventionList = new ArrayList<>();
	    iresult = new InterventionResult(chemConceptList, semrepList, GNormList);
	    //	    chemConceptList.addAll(semrepList); // Add semrepList to
	    //						// chemConceptList
	    //	    chemConceptList.addAll(GNormList); // Add GNormList to
	    // chemConceptList
	    allConcepts.addAll(semrepList);
	    allConcepts.addAll(GNormList);
	    //	    List<BratConcept> unsubsumedMustList = new ArrayList<>();
	    /*
	     * Check if must concept found in sentence are already found in the
	     * chemConceptList
	     */

	    List<BratConcept> finalMustConceptList = new ArrayList<>();
	    for (BratConcept mustConcept : mustConceptList) {
		//	    	int startOfMust = mustConcept.startOffset;
		//	    	int endOfMust = mustConcept.endOffset;
		boolean mustConceptFound = false;
		for (BratConcept chemConcept : chemConceptList) {
		    if (Utils.isSubsumed(mustConcept, chemConcept)) {
			mustConceptFound = true;
			break;
		    }
		}
		if (!mustConceptFound) // If mustConcept is not found in any of chemConceptList
		    finalMustConceptList.add(mustConcept);
	    }
	    iresult.mustConceptList = finalMustConceptList; // assign the final mustConceptList to InterventionList
	    allConcepts.addAll(finalMustConceptList);

	    Set<String> MOASet = new HashSet<>();
	    //	    for (BratConcept allConcept : chemConceptList) {
	    for (BratConcept allConcept : allConcepts) {
		String searchTerm = allConcept.name;
		MESHINFO meshinfo = mesh.SearchMESHDatabase(searchTerm);
		String PAStrings = meshinfo.PA;
		if (intervention_term_list.containsKey(searchTerm.toLowerCase())) {
		    allConcept.CUI = intervention_term_list.get(searchTerm.toLowerCase());
		    if (allConcept.geneId != null && allConcept.CUI.matches("^[0-9]+$"))
			allConcept.geneId = intervention_term_list.get(searchTerm.toLowerCase());
		}
		//				else if (allConcept.geneId != null && (allConcept.geneType.equals("gnormplus") || allConcept.geneType.equals("omim"))) allConcept.CUI = allConcept.geneId;
		else if (allConcept.geneId != null
			&& (allConcept.geneType.equals("gnormplus") || allConcept.geneType.equals("omim"))) {
		}
		//				else if (meshinfo.UI != null && meshinfo.UI.isEmpty() == false && (allConcept.CUI == null || allConcept.CUI.matches("C[0-9]{7}"))) allConcept.CUI = meshinfo.UI;
		else if (meshinfo.UI != null && meshinfo.UI.isEmpty() == false)
		    allConcept.CUI = meshinfo.UI;
		List<String> MOAList = new ArrayList<>();
		if (PAStrings != null && PAStrings.length() > 0) {
		    String PAList[] = PAStrings.split(";");
		    MOAfound = true;
		    for (int pindex = 0; pindex < PAList.length; pindex++) {
			MOAList.add(PAList[pindex]);
			MOASet.add(PAList[pindex]);
		    }
		} else if (meshinfo.HEADING == 1 && meshinfo.PRECONCEPT != null) { // Mesh Heading
		    /**
		     * 5. If Pharm action field not found, examine MeSH tree struture 6. Enter term
		     * directly above search term in Field 2
		     **/
		    MOAfound = true;
		    MOAList.add(meshinfo.PRECONCEPT);
		    MOASet.add(meshinfo.PRECONCEPT);
		    // System.out.println("next search term in outcome | " +
		    // meshinfo.PRECONCEPT);
		} else if (meshinfo.HEADING == 0 && meshinfo.II != null && meshinfo.II.length() > 0) {
		    // System.out.println("Indexing info found | " +
		    // meshinfo.II);
		    MOAfound = true;
		    MOAList.add(meshinfo.II);
		    MOASet.add(meshinfo.II);
		}
		InterventionConcept interConcept = new InterventionConcept(allConcept, MOAList);
		interventionList.add(interConcept);
		for (String moa : MOAList) {
		    System.out.println(
			    allConcept.startOffset + "-" + allConcept.endOffset + ": " + searchTerm + " -> " + moa);
		}

	    }
	    List<String> MOAFinalList = new ArrayList<>(MOASet);
	    iresult.MOAList = MOAFinalList;

	} catch (Exception e) {
	    e.printStackTrace();
	}
	return iresult;

    }

    public void writeTextToBratFile(String PMID, String inDir, String outDir, String text) {
	try {
	    String separator = System.getProperty("file.separator");
	    String inFile = new String(inDir + separator + PMID + ".txt");
	    String outFile = new String(outDir + separator + PMID + ".txt");
	    String line;
	    BufferedReader br = new BufferedReader(new FileReader(inFile));
	    PrintWriter out = new PrintWriter(new File(outFile));
	    while ((line = br.readLine()) != null) {
		out.println(line);
	    }
	    out.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static boolean existsOverlapping(BratConcept bc, Map<Span, BratConcept> existing) {
	Span bcs = new Span(bc.startOffset, bc.endOffset);
	return existsOverlapping(bcs, existing);
    }

    private static boolean existsOverlapping(Span termSpan, Map<Span, BratConcept> existing) {
	for (Span sp : existing.keySet()) {
	    if (Span.overlap(sp, termSpan))
		return true;
	}
	return false;
    }

    public void generateIntervention(String bratDir, String outDir) {
	String aLine = null;
	try {

	    String separator = System.getProperty("file.separator");
	    File dir = new File(bratDir);
	    FilenameFilter filter = null;
	    // It is also possible to filter the list of returned files.
	    // This example does not return any files that start with `.'.
	    filter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
		    return name.endsWith("ann");
		}
	    };
	    String[] children = dir.list(filter);
	    for (int j = 0; j < children.length; j++) {
		String bratFile = new String(bratDir + separator + children[j]);
		System.out.println(bratFile);
		String PMID = children[j].substring(0, children[j].indexOf("."));
		String txtFile = new String(bratDir + separator + PMID + ".txt");
		TextAndTitleLength ttl = readFile(txtFile);
		BratDocument doc = brat.readABratFile(bratFile, txtFile);
		InterventionResult iresult = inter.generateIntervention(doc, ttl.text, ttl.titleLength);
		System.out.println("output to the directory : " + outDir);
		writeTextToBratFile(doc.PMID, bratDir, outDir, ttl.text);
		brat.writeInterventionInfoToABratFile(doc, iresult, outDir);

	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public TextAndTitleLength readFile(String filename) {
	try {
	    StringBuffer messagebuf = new StringBuffer();
	    BufferedReader br = new BufferedReader(new FileReader(filename));
	    String line;
	    int titleLength = 0;
	    boolean isTitle = true;
	    while ((line = br.readLine()) != null) {
		if (isTitle) {
		    titleLength = line.length();
		    isTitle = false;
		}
		messagebuf.append(line).append(" ");
	    }
	    br.close();
	    TextAndTitleLength ttl = new TextAndTitleLength(messagebuf.toString(), titleLength);
	    return ttl;
	} catch (FileNotFoundException fnfe) {
	    throw new RuntimeException(fnfe);
	} catch (IOException ioe) {
	    throw new RuntimeException(ioe);
	}
    }

    /*
     * Check semrep type and name
     */

    public boolean hasProperSemRepType(String type, boolean strict) {
	//		return Arrays.asList("bacs","dora","horm","orch").contains(type);
	if (strict)
	    return Arrays.asList("antb", "bodm", "clnd", "dora", "drdd", "medd", "nsba", "phsu", "plnt", "topp")
		    .contains(type);
	return Arrays.asList("aapp", "antb", "bacs", "bodm", "chem", "clnd", "dora", "drdd", "food", "hops", "horm",
		"inch", "medd", "nsba", "orch", "phsu", "plnt", "sbst", "topp", "vita").contains(type);
    }

    // DELETE LATER.
    public boolean hasProperSemRepType(String type) {
	//		return Arrays.asList("bacs","dora","horm","orch").contains(type);
	return type.equals("phsu");
    }

    public static void main(String[] argv) {
	try {
	    Brat brat = Brat.getInstance();
	    Intervention inter = Intervention.getInstance();
	    // String textfile = new
	    // String("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\8726542.txt");
	    // String textfile = new
	    // String("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\10811399.txt");
	    // String textfile = new
	    // String("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\25449120.txt");
	    // String textfile = new
	    // String("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat3\\18474731.txt");
	    /*
	     * String textfile = new String(
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat3\\25680233.txt");
	     * TextAndTitleLength ttl = inter.readFile(textfile); // BratDocument doc =
	     * brat.readABratFile(
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\8726542.ann",
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\8726542.txt"); //
	     * BratDocument doc = brat.readABratFile(
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\10811399.ann",
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\10811399.txt"); //
	     * BratDocument doc = brat.readABratFile(
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\25449120.ann",
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\25449120.txt"); BratDocument
	     * doc = brat.readABratFile(
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat3\\25680233.ann",
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat3\\25680233.txt");
	     * InterventionResult finalList = inter.generateIntervention(doc, ttl.text,
	     * ttl.titleLength); // brat.writeInterventionInfoToABratFile(doc, finalList,
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\brat_intervention1");
	     * brat.writeInterventionInfoToABratFile(doc, finalList,
	     * "Q:\\LHC_Projects\\Caroline\\NEW_2017\\brat_intervention7");
	     */

	    // inter.generateIntervention("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2",
	    // "Q:\\LHC_Projects\\Caroline\\NEW_2017\\brat_gene2");
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
