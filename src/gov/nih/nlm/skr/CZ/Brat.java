package gov.nih.nlm.skr.CZ;

/*
 * Brat class that controls the conversion from SemRep, GNormPlus to Brat files and vice versa
 * Author: Dongwook Shin
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.process.MedlineSentenceSegmenter;
import gov.nih.nlm.ling.sem.Concept;
import gov.nih.nlm.ling.sem.Ontology;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ner.gnormplus.GNormPlusStringWrapper;

public class Brat {
    // private static GNormPlusWrapper gpw = null;
    private static GNormPlusStringWrapper gpw = null;
    // private static SemRepToBrat srb = null;
    private static Brat brat = null;
    private static Hashtable<String, String> semtypeHt = null; // Hashtable for "tisu" -> "Tissue"
    private static Hashtable<String, String> semtypeHtReverse = null; // Hashtable for "Tissue" -> "tisu"
    private static HashMap<String, String> OMIMMap = null;
    private static HashMap<String, String> OMIMHUMANGENEMap = null;
    private static Set<String> OMIMSet = null;
    // private static BufferedReader semtypein = null;
    private int GNORMPLUS_BATCH = 500;
    static Properties prop;
    static Connection conn;
    static Statement stmt;

    private static final String mysqlInsertSpecies = new String(
	    "INSERT INTO SPECIES_MODEL (SENTENCE_ID, TERM, TYPE, START_POS, END_POS) VALUES (");
    private static final String mysqlInsertSentence = new String(
	    "INSERT INTO SENTENCE (PMID, SENT_NUMBER, START_POS, END_POS, SENTENCE) VALUES (\"");
    private static final String mysqlSelectSentence = new String("SELECT SENTENCE_ID FROM SENTENCE WHERE PMID =\"");

    private static final String mysqlInsertGene = new String(
	    "INSERT INTO GENE (SENTENCE_ID, TERM, TYPE, TERM_ID, START_POS, END_POS) VALUES (");
    private static final String mysqlInsertIntervention = new String(
	    "INSERT INTO INTERVENTION (SENTENCE_ID, TERM, TYPE, ID, START_POS, END_POS) VALUES (");
    // Insert PMID to Publication table
    private static final String mysqlInsertPMID = new String("INSERT INTO PUBLICATION (PMID) VALUES (\"");
    private static final String mysqlSelectPMID = new String("SELECT PMID FROM PUBLICATION WHERE PMID =\"");

    /*
     * public static Brat getInstance(String typein) throws IOException { if (brat
     * == null) { // System.out.println("Initializing a Brat instance..." ); brat =
     * new Brat(); semtypeHt = new Hashtable<String, String>(); // Hashtable for
     * "tisu" -> "Tissue" semtypeHtReverse = new Hashtable<String, String>(); //
     * Hashtable for "Tissue" -> "tisu" semtypein = new BufferedReader(new
     * FileReader(typein)); String aLine = null; while((aLine =
     * semtypein.readLine()) != null) { String compo[] = aLine.split("\\|"); //
     * System.out.println(compo[1].trim() + ", " + compo[0].trim());
     * semtypeHt.put(compo[1].trim(), compo[0].trim());
     * semtypeHtReverse.put(compo[0].trim(), compo[1].trim()); }
     * 
     * } return brat; }
     */

    public static Brat getInstance() throws IOException {
	if (brat == null) {
	    // System.out.println("Initializing a Brat instance...");
	    brat = new Brat();
	    prop = new Properties();
	    prop = FileUtils.loadPropertiesFromFile("config.properties");
	    semtypeHt = new Hashtable<>(); // Hashtable for "tisu"
					   // -> "Tissue"
	    semtypeHtReverse = new Hashtable<>(); // Hashtable for
						  // "Tissue" ->
						  // "tisu"
	    String dbConnectionStr = prop.getProperty("connectionString");
	    String dbName = prop.getProperty("dbName");
	    String userName = prop.getProperty("dbUsername");
	    String password = prop.getProperty("dbPassword");
	    try {
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(dbConnectionStr + dbName + "?autoReconnect=true", userName,
			password);
		stmt = conn.createStatement();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    BufferedReader semtypein = new BufferedReader(new FileReader(prop.getProperty("SEMTYPE_FULL_NAME")));
	    String omimin = prop.getProperty("OMIM_FILE_NAME");
	    String omiminIdFile = prop.getProperty("OMIM_HUMANGENEID_FILE_NAME");
	    String aLine = null;
	    while ((aLine = semtypein.readLine()) != null) {
		String compo[] = aLine.split("\\|");
		// System.out.println(compo[1].trim() + ", " + compo[0].trim());
		semtypeHt.put(compo[1].trim(), compo[0].trim());
		semtypeHtReverse.put(compo[0].trim(), compo[1].trim());
	    }
	    OMIMMap = ReadConceptList.readOMIMGeneList(omimin);
	    OMIMHUMANGENEMap = ReadConceptList.readOMIMHumanGeneIdList(omiminIdFile);
	    OMIMSet = OMIMMap.keySet();

	}
	return brat;
    }

    public void semrepGNormPlusToBrat(String semfile, String textfile, String outDir, String setup) {
	try {
	    // gpw = GNormPlusWrapper.getInstance(setup);
	    gpw = GNormPlusStringWrapper.getInstance(setup);
	    Hashtable entityHt = new Hashtable(); // Hashtable for Concept ->
						  // Symbol (Ti)
						  // semtypeHt = new Hashtable<String, String>(); // Hashtable for
						  // "tisu" -> "Tissue"
	    boolean startNewDoc = false;
	    PrintWriter txtout = null;
	    PrintWriter annout = null;
	    PrintWriter prevTxtout = null;
	    PrintWriter prevAnnout = null;
	    boolean IsTitle = false;
	    boolean overall = false;
	    int rowNum = 0;
	    String prevPos = new String("-1");
	    boolean duplicateSentence = true;
	    // int signalNum = 3;
	    int entityNum = 3;
	    int eventNum = 1;
	    int attributeNum = 1;

	    String sentence = null;
	    String prevSentence = null;
	    int offset = 0;
	    String SymbolS = null;
	    String ObjectSymbolS = null;
	    String EventS = null;
	    String AttributeSymbol = null;
	    String PMID = null;
	    String prevPMID = null;
	    String pos = null;
	    BufferedReader semrepin = new BufferedReader(new FileReader(semfile));
	    BufferedReader textin = new BufferedReader(new FileReader(textfile));
	    // System.out.println("Reading semrep file " + PMID + ".semrep");
	    String aLine = null;

	    /**
	     * Converting semrep output into Brat
	     */

	    int citPos = 0;
	    // entityNum = 3;
	    int relNum = eventNum;
	    int refNum = 1;
	    int coreferenceNum = 1;
	    int searchLimit = 0;
	    int offsetDiff = 0;
	    int offsetMargin = 0;
	    int curTitleLen = 0;
	    int prevTitleLen = 0;
	    List<BratSentence> sentList = new ArrayList<>();
	    String syntacticUnit = null;
	    String curText = null;
	    String prevText = null;
	    String curTitle = null;
	    String prevTitle = null;
	    String curAbstract = null;
	    String prevAbstract = null;
	    String PMIDinText = null;
	    String NextPMIDinText = null;
	    String fLine = null;

	    /*
	     * Feb 15 2018 GNormPlusWrapper has too many files open problem, So for now, we
	     * need to re-initializa for every 500 citations
	     */
	    int numCitation = 0;

	    while ((aLine = semrepin.readLine()) != null) {
		// System.out.println(aLine);
		/*
		 * Dec 01 2017 Process the semrep ERROR message and skip corresponding text
		 */
		if (aLine.contains("ERROR *** ERROR ***")) {
		    boolean passPMID = false;
		    boolean passTitle = false;
		    while ((fLine = textin.readLine()) != null) { // read text
			if (fLine.startsWith("PMID")) {
			    passPMID = true;
			} else if (fLine.startsWith("TI")) {
			    passTitle = true;
			}
			if (passPMID == true && passTitle == true) // If pass PMID and TI, then stop at the text
			    break;
		    }
		}

		if (aLine.length() > 0) {
		    if (aLine.startsWith("["))
			syntacticUnit = new String(aLine);
		    String compo[] = aLine.split("\\|");
		    // System.out.println(compo[5]);
		    if (compo.length > 5) {
			if (PMID == null || (prevPMID != null && !prevPMID.equals(compo[1]) && compo[5].equals("text")))
			    startNewDoc = true;
			prevPMID = PMID;
			PMID = compo[1];
		    }

		    if (startNewDoc) {
			// prevPMID = PMID;
			System.out.println("PMID = " + PMID);
			prevTxtout = txtout;
			prevAnnout = annout;
			prevText = curText;
			prevTitleLen = curTitleLen;
			prevTitle = curTitle;
			prevAbstract = curAbstract;
			offsetMargin = 13 + PMID.length();
			if (NextPMIDinText == null) { // retriving PMID
			    while ((fLine = textin.readLine()) != null) { // read PMID from the current position of text file
				if (fLine.startsWith("PMID")) {
				    PMIDinText = fLine.substring(6).trim();
				    break;
				}
			    }
			} else
			    PMIDinText = NextPMIDinText;

			while ((fLine = textin.readLine()) != null) { // read
								      // text
			    if (fLine.startsWith("PMID")) {
				NextPMIDinText = fLine.substring(6).trim();

				// curText = null;
				break;
			    } else if (fLine.startsWith("TI")) {
				curTitle = fLine.substring(6).trim() + "\n";
				curAbstract = null;
				curText = curTitle;
				curTitleLen = curText.length();
			    } else if (fLine.length() > 0) {
				curText = curText + fLine + "\n";
				curAbstract = fLine + "\n";
			    }
			}
			// System.out.println("prevPMID = " + prevPMID + "\t
			// prevText = " + prevText);
			if (prevTxtout != null && prevText != null) {
			    prevTxtout.println(prevText);
			    prevTxtout.close();
			}

			/*
			 * Find genes from OMIM
			 */

			if (prevText != null) {
			    String lowercaseText = prevText.toLowerCase();
			    List<BratConcept> OMIMList = new ArrayList<>();
			    int textoffset = 0;
			    for (String omimStr : OMIMSet) {
				while (textoffset < lowercaseText.length()) { // do the following for the whole text of the citation
				    textoffset = lowercaseText.indexOf(omimStr.toLowerCase(), textoffset);
				    // System.out.println("textoffset = " + textoffset);
				    if (textoffset >= 0 && StringUtils.checkIfWordIsNotSubstring(textoffset,
					    omimStr.toLowerCase(), lowercaseText)) { // If found, word is not a substring of another word
					String preferredName = OMIMMap.get(omimStr.toLowerCase());
					String realString = prevText.substring(textoffset,
						textoffset + omimStr.length());
					BratConcept bc = new BratConcept(realString, textoffset,
						textoffset + omimStr.length());
					int endOffset = textoffset + omimStr.length();
					// System.out.println("text = " + lowercaseText);
					// System.out.println("\t\t  === OMIM string : " + omimStr + "\t real String : "
					//	+ realString + " offset = (" + textoffset + ", " + endOffset);

					bc.preferredName = preferredName;
					OMIMList = Utils.addUniqueOMIMConcepts(OMIMList, bc);
					textoffset++; // advance a letter to
						      // avoid the infinite
						      // loop
				    } else
					break;

				}
			    }

			    for (BratConcept omimconcept : OMIMList) {
				String SymbolT = new String("T" + entityNum);
				entityNum++;
				int start = omimconcept.startOffset;
				int end = omimconcept.endOffset;
				prevAnnout.println(
					SymbolT + "\tOMIM_Gene " + +start + " " + end + "\t" + omimconcept.name);
				String omimGeneId = null;
				omimGeneId = OMIMHUMANGENEMap.get(omimconcept.name.toLowerCase());
				if (omimGeneId == null)
				    omimGeneId = OMIMHUMANGENEMap.get(omimconcept.preferredName.toLowerCase());

				if (omimGeneId != null) { // if geneid is not null, add reference for the gene
				    String SymbolN = new String("N" + refNum);
				    annout.println(SymbolN + "\tReference " + SymbolT + " OMIMGene:" + omimGeneId);
				    refNum++;
				}
			    }
			}
			/*
			 * Call GNormPlusWrapper to extract GNormPlus output Process title and abstract
			 * separately since it gives offset relative to the start of each text For the
			 * abastract part, the offset needs to be added to the title length, since the
			 * Brat text consists of title + "\n" + abstract
			 */
			if (prevTitle != null) { // process title
			    Map<SpanList, LinkedHashSet<Ontology>> annotations = gpw.annotateText(prevTitle);
			    for (SpanList sp : annotations.keySet()) {
				LinkedHashSet<Ontology> concs = annotations.get(sp);
				for (Ontology c : concs) {
				    Concept con = (Concept) c;
				    String id = con.getId();
				    int startGeneIdIndex = id.indexOf("_");
				    String geneid = null;
				    if (startGeneIdIndex < id.length() - 1)
					geneid = id.substring(startGeneIdIndex + 1, id.length());

				    String SymbolT = new String("T" + entityNum);
				    entityNum++;
				    int startPos = sp.getBegin();
				    int endPos = sp.getEnd();
				    String type = con.getSemtypes().toString();
				    System.out.println("\tgene id  = " + geneid);
				    // con.getName());
				    // System.out.println( "\t\tGNormType = " +
				    // type);
				    String gnptype = new String("GNP_" + type.substring(1, type.length() - 1));
				    prevAnnout.println(SymbolT + "\t" + gnptype + " " + startPos + " " + endPos + "\t"
					    + con.getName());
				    if (geneid != null && !geneid.equals("0")) { // if geneid is not null, add reference for the gene
					String SymbolN = new String("N" + refNum);
					annout.println(SymbolN + "\tReference " + SymbolT + " Gene:" + geneid);
					refNum++;
				    }
				    if (geneid == null && !geneid.equals("0"))
					System.out.println(sp.toString() + "\t" + con.getName() + "\t" + gnptype);
				    else
					System.out.println(sp.toString() + "\t" + con.getName() + "\t" + gnptype
						+ "\t: " + geneid);
				}
			    }
			}

			if (prevAbstract != null) {
			    Map<SpanList, LinkedHashSet<Ontology>> annotations = gpw.annotateText(prevAbstract);
			    for (SpanList sp : annotations.keySet()) {
				LinkedHashSet<Ontology> concs = annotations.get(sp);
				for (Ontology c : concs) {
				    Concept con = (Concept) c;
				    String id = con.getId();
				    int startGeneIdIndex = id.indexOf("_");
				    String geneid = null;
				    if (startGeneIdIndex < id.length() - 1)
					geneid = id.substring(startGeneIdIndex + 1, id.length());

				    String SymbolT = new String("T" + entityNum);
				    entityNum++;
				    int startPos = sp.getBegin() + prevTitleLen;
				    int endPos = sp.getEnd() + prevTitleLen;
				    String type = con.getSemtypes().toString();
				    // System.out.println("\tname = " +
				    // con.getName());
				    // System.out.println( "\t\tGNormType = " +
				    // type);
				    String gnptype = new String("GNP_" + type.substring(1, type.length() - 1));
				    prevAnnout.println(SymbolT + "\t" + gnptype + " " + startPos + " " + endPos + "\t"
					    + con.getName());
				    if (geneid != null && !geneid.equals("0")) { // if geneid is not null, add reference for the gene
					String SymbolN = new String("N" + refNum);
					annout.println(SymbolN + "\tReference " + SymbolT + " Gene:" + geneid);
					refNum++;
				    }
				    if (geneid == null && !geneid.equals("0"))
					System.out.println(sp.toString() + "\t" + con.getName() + "\t" + gnptype);
				    else
					System.out.println(sp.toString() + "\t" + con.getName() + "\t" + gnptype
						+ "\t: " + geneid);
				}
			    }
			}
			if (prevText != null) {
			    for (BratSentence bsen : sentList) {
				prevAnnout.println("# SENT|" + bsen.sentNum + "|" + bsen.startPos + "|" + bsen.endPos
					+ "|" + bsen.syntacticUnit);
				// System.out.println("# SENT|" + bsen.sentNum +
				// "|" + bsen.startPos + "|" + bsen.endPos + "|"
				// + bsen.syntacticUnit);
			    }
			    prevAnnout.close();
			}
			sentList = new ArrayList<>();
			String separator = System.getProperty("file.separator");
			txtout = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outDir + separator + PMID + ".txt"))));
			annout = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outDir + separator + PMID + ".ann"))));
			numCitation++;
			System.out.println("=========== numCitation : " + numCitation);
			/*
			 * Feb 15 2018 For every number of citations that reached GNORMPLUS_BATCH,
			 * re-initialize the GnromPlusWrapper
			 */
			if (numCitation % GNORMPLUS_BATCH == 0) {
			    gpw = null;
			    gpw = GNormPlusStringWrapper.getInstance(setup);
			    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! GNormPlusWrapper reinitiated :");
			}
			startNewDoc = false;
			offset = 0;
			entityNum = 3;
			attributeNum = 1;
			eventNum = 1;
			EventS = null;
			SymbolS = null;
			// EventName = null;
			// firstTitle = true;
			refNum = 1;
			relNum = 1;
		    }
		    if (compo.length > 6 && compo[5].equals("text")) { // source
								       // sentence
								       // sentence = compo[6].trim().replaceAll("\\s+", " ");
			prevPMID = PMID;
			/*
			 * July 26 2019 MODIFICATION: In Semrep 1.8, sentence is located at 9th
			 * component of text, not 7th as in Semrep 1.7
			 * 
			 */
			//sentence = compo[6];
			sentence = compo[8];
			int sentNum = Integer.parseInt(compo[4]);
			// System.out.println(sentence);
			// citPos = citBuf.length();
			if (sentNum == 1)
			    citPos = 0;
			else
			    citPos = curText.indexOf(sentence, citPos);
			if (citPos < 0) { // If sentence does not match and citPos is returned as -1, retrieve the last sentence position and use it for the pos
			    BratSentence lastSent = sentList.get(sentList.size() - 1);
			    citPos = lastSent.endPos + 1; // add the end position of last sentence + 1 and use it for the start of the next sentence
			}
			BratSentence curSent = new BratSentence(sentence, sentNum, citPos, citPos + sentence.length(),
				syntacticUnit);
			citPos++;
			sentList.add(curSent);
			/*
			 * August 15 2017 For all semrep output, the positions is "ti.n", since abstract
			 * are concatenated to title with only new line. So, append "\n" only when the
			 * position info is "ti.1"
			 */
			// sentPos = 0;
			// firstEntity = true;
		    } else if (compo.length > 6) {
			String preferredName = compo[7];
			String sTypes[] = compo[8].split(",");
			String sType = sTypes[0];
			if (compo[5].equals("entity")) {
			    // if(!entityHt.contains(compo[7])) { // Finding new
			    // Concepts
			    // System.out.println(compo[8]);
			    String semtypeFullname = semtypeHt.get(sType);
			    String CUI = compo[6].trim();
			    if (CUI.length() <= 0)
				CUI = compo[9];
			    /*
			     * Feb 14 2018 Fix SemRep error when indicator string is null
			     * SE|22327139||ti|9|text|However only Tyrosine-kinase-non-receptor-type 13 and
			     * Netrin-G1 differed significantly between PDD and NDC cohorts.
			     * SE|22327139||ti|9|entity|C0205171|Singular|qnco|| |only||||766|1080|1084
			     * SE|22327139||ti|9|entity|C1518422|Negation|ftcn|| |||||766|1101|1101
			     */
			    if (CUI.length() > 0 && compo[11].length() > 0) {
				// if(CUI.length() > 0) {
				// System.out.println(semtypeFullname + ", " +
				// CUI);
				// System.out.println(CUI + ", " + compo[16] +
				// ", " + compo[17]);
				int semrepStartPos = Integer.parseInt(compo[16]);
				int semrepEndPos = Integer.parseInt(compo[17]);
				int startPos = semrepStartPos - offsetMargin;
				int endPos = semrepEndPos - offsetMargin;
				/*
				 * Feb 14 2018 Correct SemRep offset error such as "Tyrosine-"
				 * SE|22327139||ti|11|text|Apart from possible pathophysiological
				 * considerations, we propose that Tyrosine-kinase non-receptor-type 13 and
				 * Netrin G1 are biomarker candidates for the development of a Parkinson's
				 * disease dementia. SE|22327139||ti|11|entity|C0332149|Possible|
				 * qlco|||possible||||828|1307|1315 SE|22327139||ti|11|entity|C0518609|
				 * consideration|fndg|||considerations||||828| 1335|1349
				 * SE|22327139||ti|11|entity|C1518422|Negation|
				 * ftcn|||Tyrosine-||||817|1383|1376
				 * 
				 * if(endPos < startPos) { endPos = startPos + compo[11].length(); }
				 */
				// sentPos = endPos + 1;
				// TextEvidence te = new TextEvidence(CUI,
				// Integer.parseInt(compo[16]),
				// Integer.parseInt(compo[17]));
				TextEvidence te = new TextEvidence(CUI, startPos, endPos);
				// if(!entityHt.containsKey(te)) {
				String SymbolT = new String("T" + entityNum);
				String SymbolN = new String("N" + refNum);
				entityNum++; // Increase entity number;
				refNum++;
				// System.out.println(SymbolT + " -> (" + CUI +
				// ", " + startPos + ", " + endPos + ")");
				entityHt.put(te, SymbolT);
				// System.out.println(startPos + "," + endPos);
				// String citPart =
				// citSource.substring(startPos, endPos);
				annout.println(SymbolT + "\t" + semtypeFullname + " " + startPos + " " + endPos + "\t"
					+ compo[11]);
				if (compo[6].startsWith("C"))
				    annout.println(SymbolN + "\tReference " + SymbolT + " Metathesaurus:" + compo[6]);
				else
				    annout.println(SymbolN + "\tReference " + SymbolT + " SemRepGene:" + compo[9]);
				annout.flush();
			    }
			    // }
			} else if (compo[5].equals("coreference")) {
			    // if(!entityHt.contains(compo[7])) { // Finding new
			    // Concepts
			    // System.out.println(compo[8]);
			    String semtypeFullname = semtypeHt.get(sType);
			    String sortal = compo[11].trim();

			    if (sortal.length() > 0) {
				// System.out.println(semtypeFullname + ", " +
				// CUI);
				// System.out.println("Sortal Anaphor:" +
				// sortal);
				int semrepStartPos = Integer.parseInt(compo[16]);
				int semrepEndPos = Integer.parseInt(compo[17]);
				int startPos = semrepStartPos - offsetMargin;
				int endPos = semrepEndPos - offsetMargin;
				/*
				 * Feb 14 2018 Correct SemRep offset error such as "Tyrosine-"
				 * SE|22327139||ti|11|text|Apart from possible pathophysiological
				 * considerations, we propose that Tyrosine-kinase non-receptor-type 13 and
				 * Netrin G1 are biomarker candidates for the development of a Parkinson's
				 * disease dementia. SE|22327139||ti|11|entity|C0332149|Possible|
				 * qlco|||possible||||828|1307|1315 SE|22327139||ti|11|entity|C0518609|
				 * consideration|fndg|||considerations||||828| 1335|1349
				 * SE|22327139||ti|11|entity|C1518422|Negation|
				 * ftcn|||Tyrosine-||||817|1383|1376
				 * 
				 * if(endPos < startPos) { endPos = startPos + compo[11].length(); }
				 */
				// TextEvidence te = new TextEvidence(CUI,
				// Integer.parseInt(compo[16]),
				// Integer.parseInt(compo[17])) //
				// if(!entityHt.containsKey(te)) {
				String SymbolT = new String("T" + entityNum);
				// String SymbolN = new String("N" + refNum);
				entityNum++; // Increase entity number;
				String SymbolSPAN = new String("T" + entityNum);
				entityNum++; // Increase entity number;
				// System.out.println(SymbolT + " -> (" + sortal
				// + ", " + compo[16] + ", " + compo[17] + ")");
				// String citPart =
				// citSource.substring(startPos, endPos);
				annout.println(SymbolT + "\tSortalAnaphor " + startPos + " " + endPos + "\t" + sortal);
				annout.println(SymbolSPAN + "\tSPAN " + compo[29] + " " + compo[30] + "\t" + compo[24]);
				String SymbolCOREF = new String("R" + coreferenceNum);
				annout.println(SymbolCOREF + "\tCoreference Anaphora:" + SymbolT + " Antecedent:"
					+ SymbolSPAN);
				coreferenceNum++; // Increase entity number;
				annout.flush();
			    }
			    // }
			} else if (compo[5].equals("relation")) {
			    String SymbolT = new String("T" + entityNum);
			    entityNum++; // Increase entity number;
			    String SymbolE = new String("E" + relNum);
			    relNum++;
			    String RelationFullName[] = compo[22].split("\\(");
			    // String Relation = RelationFullName[0];
			    String Relation = compo[22];
			    // String RelationSuffix =
			    // System.out.println(Relation);
			    // System.out.println(offsetDiff);
			    int semrepStartPos = Integer.parseInt(compo[24]);
			    int semrepEndPos = Integer.parseInt(compo[25]);
			    int startPos = semrepStartPos - offsetMargin;
			    int endPos = semrepEndPos - offsetMargin;
			    // System.out.println(aLine);
			    // System.out.println("offset : " + startPos + " - "
			    // + endPos);
			    // System.out.println("citBuf = " +
			    // citBuf.toString());
			    // int startPos = sentence.indexOf(compo[11],
			    // sentPos);
			    // int endPos = startPos + compo[11].length();
			    // String citPart = citBuf.substring(startPos,
			    // endPos);
			    String citPart = curText.substring(startPos, endPos);
			    annout.println(SymbolT + "\t" + Relation + " " + startPos + " " + endPos + "\t" + citPart);
			    TextEvidence tesubj = null;
			    if (compo[8].trim().length() > 0)
				tesubj = new TextEvidence(compo[8].trim(), Integer.parseInt(compo[19]) - offsetMargin,
					Integer.parseInt(compo[20]) - offsetMargin);
			    else
				tesubj = new TextEvidence(compo[12].trim(), Integer.parseInt(compo[19]) - offsetMargin,
					Integer.parseInt(compo[20]) - offsetMargin);
			    TextEvidence teobj = null;
			    if (compo[28].trim().length() > 0)
				teobj = new TextEvidence(compo[28].trim(), Integer.parseInt(compo[39]) - offsetMargin,
					Integer.parseInt(compo[40]) - offsetMargin);
			    else
				teobj = new TextEvidence(compo[32].trim(), Integer.parseInt(compo[39]) - offsetMargin,
					Integer.parseInt(compo[40]) - offsetMargin);
			    String symbolSubj = (String) entityHt.get(tesubj);
			    String symbolObj = (String) entityHt.get(teobj);
			    // System.out.println(symbolSubj + " -> (" + tesubj
			    // + ", " + Integer.parseInt(compo[19]) + ", " +
			    // Integer.parseInt(compo[20]) + ")");
			    // System.out.println(symbolObj + " -> (" + teobj +
			    // ", " + Integer.parseInt(compo[39]) + ", " +
			    // Integer.parseInt(compo[40]) + ")");
			    annout.println(SymbolE + "\t" + Relation + ":" + SymbolT + " Subject:" + symbolSubj
				    + " Object:" + symbolObj);
			    annout.flush();
			}
		    }
		}
	    }
	    // txtout.println(citSource);
	    // Do this for final document
	    /*
	     * Find genes from OMIM
	     */

	    if (curText != null) {
		String lowercaseText = curText.toLowerCase();
		List<BratConcept> OMIMList = new ArrayList<>();
		int textoffset = 0;
		for (String omimStr : OMIMSet) {
		    while (textoffset < lowercaseText.length()) { // do the following for the whole text of the citation
			textoffset = lowercaseText.indexOf(omimStr, textoffset);
			if (StringUtils.checkIfWordIsNotSubstring(textoffset, omimStr, lowercaseText)) { // If found, word is not a substring of another word
			    String preferredName = OMIMMap.get(omimStr);
			    String realString = curText.substring(textoffset, textoffset + omimStr.length());
			    BratConcept bc = new BratConcept(realString, textoffset, textoffset + omimStr.length());
			    System.out.println("OMIM string : " + omimStr + "\t real String = " + realString);
			    bc.preferredName = preferredName;
			    OMIMList = Utils.addUniqueOMIMConcepts(OMIMList, bc);
			    textoffset++; // advance a letter to avoid the
					  // infinite loop
			} else
			    break;

		    }
		}

		for (BratConcept omimconcept : OMIMList) {
		    String SymbolT = new String("T" + entityNum);
		    entityNum++;
		    int start = omimconcept.startOffset;
		    int end = omimconcept.endOffset;
		    prevAnnout.println(SymbolT + "\tOMIM_Gene " + +start + " " + end + "\t" + omimconcept.name);
		}
	    }
	    txtout.println(curText);
	    if (curTitle != null) {
		Map<SpanList, LinkedHashSet<Ontology>> annotations = gpw.annotateText(curTitle);
		for (SpanList sp : annotations.keySet()) {
		    LinkedHashSet<Ontology> concs = annotations.get(sp);
		    for (Ontology c : concs) {
			Concept con = (Concept) c;
			String SymbolT = new String("T" + entityNum);
			entityNum++;
			int startPos = sp.getBegin();
			int endPos = sp.getEnd();
			String type = con.getSemtypes().toString();
			String gnptype = new String("GNP_" + type.substring(1, type.length() - 1));
			annout.println(SymbolT + "\t" + gnptype + " " + startPos + " " + endPos + "\t" + con.getName());
			System.out.println(sp.toString() + "\t" + con.getName() + "\t" + gnptype);
		    }
		}
	    }
	    if (curAbstract != null) {
		Map<SpanList, LinkedHashSet<Ontology>> annotations = gpw.annotateText(curAbstract);
		for (SpanList sp : annotations.keySet()) {
		    LinkedHashSet<Ontology> concs = annotations.get(sp);
		    for (Ontology c : concs) {
			Concept con = (Concept) c;
			String SymbolT = new String("T" + entityNum);
			entityNum++;
			int startPos = sp.getBegin() + curTitleLen;
			int endPos = sp.getEnd() + curTitleLen;
			String type = con.getSemtypes().toString();
			String gnptype = new String("GNP_" + type.substring(1, type.length() - 1));
			annout.println(SymbolT + "\t" + gnptype + " " + startPos + " " + endPos + "\t" + con.getName());
			System.out.println(sp.toString() + "\t" + con.getName() + "\t" + gnptype);
		    }
		}
	    }

	    for (BratSentence bsen : sentList) {
		annout.println(
			"# SENT|" + bsen.sentNum + "|" + bsen.startPos + "|" + bsen.endPos + "|" + bsen.syntacticUnit);
		// System.out.println("# SENT|" + bsen.sentNum + "|" +
		// bsen.startPos + "|" + bsen.endPos + "|" +
		// bsen.syntacticUnit);
	    }

	    txtout.close();
	    annout.close();
	    // errorOut.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /*
     * Convert only semrep output to Brat
     */
    public void semrepToBrat(String semfile, String textfile, String outDir) {
	try {
	    Hashtable entityHt = new Hashtable(); // Hashtable for Concept ->
						  // Symbol (Ti)
	    boolean startNewDoc = false;
	    PrintWriter txtout = null;
	    PrintWriter annout = null;
	    boolean IsTitle = false;
	    boolean overall = false;
	    int rowNum = 0;
	    StringBuffer titleBuf = new StringBuffer();
	    StringBuffer abstractBuf = new StringBuffer();
	    StringBuffer citBuf = new StringBuffer();
	    String prevPos = new String("-1");
	    boolean duplicateSentence = true;
	    // int signalNum = 3;
	    int entityNum = 3;
	    int eventNum = 1;
	    int attributeNum = 1;

	    String sentence = null;
	    String prevSentence = null;
	    int offset = 0;
	    String SymbolS = null;
	    String ObjectSymbolS = null;
	    String EventS = null;
	    String AttributeSymbol = null;
	    String PMID = null;
	    String prevPMID = null;
	    String pos = null;
	    /*
	     * BufferedReader semtypein = new BufferedReader(new FileReader(typein));
	     */
	    BufferedReader semrepin = new BufferedReader(new FileReader(semfile));
	    BufferedReader textin = new BufferedReader(new FileReader(textfile));
	    System.out.println("Reading text file " + textfile);
	    System.out.println("Reading semrep file " + PMID + ".semrep");
	    String aLine = null;

	    /**
	     * Converting semrep output into Brat
	     */
	    int citPos = 0;
	    int relNum = eventNum;
	    int refNum = 1;
	    int coreferenceNum = 1;
	    int offsetMargin = 0;
	    int titleLen = 0;
	    List<BratSentence> sentList = new ArrayList<>();
	    String syntacticUnit = null;
	    String curText = null;
	    String PMIDinText = null;
	    String NextPMIDinText = null;
	    String fLine = null;

	    while ((aLine = semrepin.readLine()) != null) {
		System.out.println(aLine);
		/*
		 * Dec 01 2017 Process the semrep ERROR message and skip corresponding text
		 */
		if (aLine.contains("ERROR *** ERROR ***")) {
		    boolean passPMID = false;
		    boolean passTitle = false;
		    while ((fLine = textin.readLine()) != null) { // read text
			if (fLine.startsWith("PMID")) {
			    passPMID = true;
			} else if (fLine.startsWith("TI")) {
			    passTitle = true;
			}
			if (passPMID == true && passTitle == true) // If pass PMID and TI, then stop at the text
			    break;
		    }
		}

		if (aLine.length() > 0) {
		    if (aLine.startsWith("["))
			syntacticUnit = new String(aLine);
		    String compo[] = aLine.split("\\|");
		    // System.out.println(compo[5]);
		    if (compo.length > 5) {
			if (PMID == null || (prevPMID != null && !prevPMID.equals(compo[1]) && compo[5].equals("text")))
			    startNewDoc = true;
			prevPMID = PMID;
			PMID = compo[1];
		    }

		    if (startNewDoc) {
			// prevPMID = PMID;
			System.out.println("PMID = " + PMID);
			offsetMargin = 13 + PMID.length();
			/*
			 * if(citBuf.length() > 0) { // txtout.println(citBuf);
			 * 
			 * txtout.close(); annout.close(); citBuf = new StringBuffer(); titleBuf = new
			 * StringBuffer(); abstractBuf = new StringBuffer(); titleLen = 0; }
			 */
			for (BratSentence bsen : sentList) {
			    annout.println("# SENT|" + bsen.sentNum + "|" + bsen.startPos + "|" + bsen.endPos + "|"
				    + bsen.syntacticUnit);
			    // System.out.println("# SENT|" + bsen.sentNum + "|"
			    // + bsen.startPos + "|" + bsen.endPos + "|" +
			    // bsen.syntacticUnit);
			}
			if (annout != null)
			    annout.close();
			sentList = new ArrayList<>();
			String separator = System.getProperty("file.separator");
			txtout = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outDir + separator + PMID + ".txt"))));
			annout = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outDir + separator + PMID + ".ann"))));
			startNewDoc = false;
			offset = 0;
			entityNum = 3;
			attributeNum = 1;
			eventNum = 1;
			EventS = null;
			SymbolS = null;
			// EventName = null;
			// firstTitle = true;
			refNum = 1;
			relNum = 1;
			citPos = 0;
			if (NextPMIDinText == null) { // retriving PMID
			    while ((fLine = textin.readLine()) != null) { // read PMID from the current position of text file
				if (fLine.startsWith("PMID")) {
				    PMIDinText = fLine.substring(6).trim();
				    break;
				}
			    }
			} else
			    PMIDinText = NextPMIDinText;

			while ((fLine = textin.readLine()) != null) { // read
								      // text
			    if (fLine.startsWith("PMID")) {
				NextPMIDinText = fLine.substring(6).trim();
				txtout.println(curText);
				txtout.close();
				// curText = null;
				break;
			    } else if (fLine.startsWith("TI")) {
				curText = fLine.substring(6).trim();
			    } else if (fLine.length() > 0)
				curText = curText + "\n" + fLine;
			}
			if (curText.endsWith("\n"))
			    curText = curText + "\n";
			if (curText != null && txtout != null) { // Do this for
								 // last PMID
			    txtout.println(curText);
			    txtout.close();
			}

		    }
		    if (compo.length > 6 && compo[5].equals("text")) { // source
								       // sentence
								       // sentence = compo[6].trim().replaceAll("\\s+", " ");
			prevPMID = PMID;
			sentence = compo[6];
			// System.out.println(sentence);
			citPos = citBuf.length();
			int sentNum = Integer.parseInt(compo[4]);
			BratSentence curSent = new BratSentence(sentence, sentNum, citPos, citPos + sentence.length(),
				syntacticUnit);
			sentList.add(curSent);
			/*
			 * August 15 2017 For all semrep output, the positions is "ti.n", since abstract
			 * are concatenated to title with only new line. So, append "\n" only when the
			 * position info is "ti.1"
			 */
			if (compo[3].equals("ti") && compo[4].equals("1")) {
			    citBuf.append(sentence + "\n");
			} else {
			    citBuf.append(sentence + " ");
			}
			// sentPos = 0;
			// firstEntity = true;
		    } else if (compo.length > 6) {
			String preferredName = compo[7];
			String sTypes[] = compo[8].split(",");
			String sType = sTypes[0];
			if (compo[5].equals("entity")) {
			    // if(!entityHt.contains(compo[7])) { // Finding new
			    // Concepts
			    // System.out.println(compo[8]);
			    String semtypeFullname = semtypeHt.get(sType);
			    String CUI = compo[6].trim();
			    if (CUI.length() <= 0)
				CUI = compo[9];
			    /*
			     * Feb 14 2018 Fix SemRep error when indicator string is null
			     * SE|22327139||ti|9|text|However only Tyrosine-kinase-non-receptor-type 13 and
			     * Netrin-G1 differed significantly between PDD and NDC cohorts.
			     * SE|22327139||ti|9|entity|C0205171|Singular|qnco|| |only||||766|1080|1084
			     * SE|22327139||ti|9|entity|C1518422|Negation|ftcn|| |||||766|1101|1101
			     */
			    if (CUI.length() > 0 && compo[11].length() > 0) {
				// System.out.println(semtypeFullname + ", " +
				// CUI);
				// System.out.println(CUI + ", " + compo[16] +
				// ", " + compo[17]);
				int semrepStartPos = Integer.parseInt(compo[16]);
				int semrepEndPos = Integer.parseInt(compo[17]);
				int startPos = semrepStartPos - offsetMargin;
				int endPos = semrepEndPos - offsetMargin;
				/*
				 * Feb 14 2018 Correct SemRep offset error such as "Tyrosine-"
				 * SE|22327139||ti|11|text|Apart from possible pathophysiological
				 * considerations, we propose that Tyrosine-kinase non-receptor-type 13 and
				 * Netrin G1 are biomarker candidates for the development of a Parkinson's
				 * disease dementia. SE|22327139||ti|11|entity|C0332149|Possible|
				 * qlco|||possible||||828|1307|1315 SE|22327139||ti|11|entity|C0518609|
				 * consideration|fndg|||considerations||||828| 1335|1349
				 * SE|22327139||ti|11|entity|C1518422|Negation|
				 * ftcn|||Tyrosine-||||817|1383|1376
				 */
				if (endPos < startPos) {
				    endPos = startPos + compo[11].length();
				}
				// sentPos = endPos + 1;
				// TextEvidence te = new TextEvidence(CUI,
				// Integer.parseInt(compo[16]),
				// Integer.parseInt(compo[17]));
				TextEvidence te = new TextEvidence(CUI, startPos, endPos);
				// if(!entityHt.containsKey(te)) {
				String SymbolT = new String("T" + entityNum);
				String SymbolN = new String("N" + refNum);
				entityNum++; // Increase entity number;
				refNum++;
				// System.out.println(SymbolT + " -> (" + CUI +
				// ", " + startPos + ", " + endPos + ")");
				entityHt.put(te, SymbolT);
				// System.out.println(startPos + "," + endPos);
				// String citPart =
				// citSource.substring(startPos, endPos);
				annout.println(SymbolT + "\t" + semtypeFullname + " " + startPos + " " + endPos + "\t"
					+ compo[11]);
				if (compo[6].startsWith("C"))
				    annout.println(SymbolN + "\tReference " + SymbolT + " Metathesaurus:" + compo[6]);
				else
				    annout.println(SymbolN + "\tReference " + SymbolT + " SemRepGene:" + compo[9]);
				annout.flush();
			    }
			    // }
			} else if (compo[5].equals("coreference")) {
			    // if(!entityHt.contains(compo[7])) { // Finding new
			    // Concepts
			    // System.out.println(compo[8]);
			    String semtypeFullname = semtypeHt.get(sType);
			    String sortal = compo[11].trim();

			    if (sortal.length() > 0) {
				// System.out.println(semtypeFullname + ", " +
				// CUI);
				// System.out.println("Sortal Anaphor:" +
				// sortal);
				int semrepStartPos = Integer.parseInt(compo[16]);
				int semrepEndPos = Integer.parseInt(compo[17]);
				int startPos = semrepStartPos - offsetMargin;
				int endPos = semrepEndPos - offsetMargin;
				// TextEvidence te = new TextEvidence(CUI,
				// Integer.parseInt(compo[16]),
				// Integer.parseInt(compo[17])) //
				// if(!entityHt.containsKey(te)) {
				String SymbolT = new String("T" + entityNum);
				// String SymbolN = new String("N" + refNum);
				entityNum++; // Increase entity number;
				String SymbolSPAN = new String("T" + entityNum);
				entityNum++; // Increase entity number;
				// System.out.println(SymbolT + " -> (" + sortal
				// + ", " + compo[16] + ", " + compo[17] + ")");
				// String citPart =
				// citSource.substring(startPos, endPos);
				annout.println(SymbolT + "\tSortalAnaphor " + startPos + " " + endPos + "\t" + sortal);
				annout.println(SymbolSPAN + "\tSPAN " + compo[29] + " " + compo[30] + "\t" + compo[24]);
				String SymbolCOREF = new String("R" + coreferenceNum);
				annout.println(SymbolCOREF + "\tCoreference Anaphora:" + SymbolT + " Antecedent:"
					+ SymbolSPAN);
				coreferenceNum++; // Increase entity number;
				annout.flush();
			    }
			    // }
			} else if (compo[5].equals("relation")) {
			    String SymbolT = new String("T" + entityNum);
			    entityNum++; // Increase entity number;
			    String SymbolE = new String("E" + relNum);
			    relNum++;
			    String RelationFullName[] = compo[22].split("\\(");
			    // String Relation = RelationFullName[0];
			    String Relation = compo[22];
			    // String RelationSuffix =
			    // System.out.println(Relation);
			    // System.out.println(offsetDiff);
			    int semrepStartPos = Integer.parseInt(compo[24]);
			    int semrepEndPos = Integer.parseInt(compo[25]);
			    int startPos = semrepStartPos - offsetMargin;
			    int endPos = semrepEndPos - offsetMargin;
			    // System.out.println(aLine);
			    // System.out.println("offset : " + startPos + " - "
			    // + endPos);
			    // System.out.println("citBuf = " +
			    // citBuf.toString());
			    // int startPos = sentence.indexOf(compo[11],
			    // sentPos);
			    // int endPos = startPos + compo[11].length();
			    // String citPart = citBuf.substring(startPos,
			    // endPos);
			    String citPart = curText.substring(startPos, endPos);
			    annout.println(SymbolT + "\t" + Relation + " " + startPos + " " + endPos + "\t" + citPart);
			    TextEvidence tesubj = null;
			    if (compo[8].trim().length() > 0)
				tesubj = new TextEvidence(compo[8].trim(), Integer.parseInt(compo[19]) - offsetMargin,
					Integer.parseInt(compo[20]) - offsetMargin);
			    else
				tesubj = new TextEvidence(compo[12].trim(), Integer.parseInt(compo[19]) - offsetMargin,
					Integer.parseInt(compo[20]) - offsetMargin);
			    TextEvidence teobj = null;
			    if (compo[28].trim().length() > 0)
				teobj = new TextEvidence(compo[28].trim(), Integer.parseInt(compo[39]) - offsetMargin,
					Integer.parseInt(compo[40]) - offsetMargin);
			    else
				teobj = new TextEvidence(compo[32].trim(), Integer.parseInt(compo[39]) - offsetMargin,
					Integer.parseInt(compo[40]) - offsetMargin);
			    String symbolSubj = (String) entityHt.get(tesubj);
			    String symbolObj = (String) entityHt.get(teobj);
			    // System.out.println(symbolSubj + " -> (" + tesubj
			    // + ", " + Integer.parseInt(compo[19]) + ", " +
			    // Integer.parseInt(compo[20]) + ")");
			    // System.out.println(symbolObj + " -> (" + teobj +
			    // ", " + Integer.parseInt(compo[39]) + ", " +
			    // Integer.parseInt(compo[40]) + ")");
			    annout.println(SymbolE + "\t" + Relation + ":" + SymbolT + " Subject:" + symbolSubj
				    + " Object:" + symbolObj);
			    annout.flush();
			}
		    }
		}
	    }

	    if (citBuf.length() > 0) {
		txtout.println(citBuf);
		for (BratSentence bsen : sentList) {
		    annout.println("# SENT|" + bsen.sentNum + "|" + bsen.startPos + "|" + bsen.endPos + "|"
			    + bsen.syntacticUnit);
		    System.out.println("# SENT|" + bsen.sentNum + "|" + bsen.startPos + "|" + bsen.endPos + "|"
			    + bsen.syntacticUnit);
		}

	    }

	    txtout.close();
	    annout.close();
	    // errorOut.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void semrepToBratFactuality(String typein, String semrepfile, String textfile, String outDir) {
	try {
	    Hashtable entityHt = new Hashtable(); // Hashtable for Concept ->
						  // Symbol (Ti)
	    Hashtable semtypeHt = new Hashtable(); // Hashtable for "tisu" ->
						   // "Tissue"
	    boolean startNewDoc = false;
	    PrintWriter txtout = null;
	    PrintWriter annout = null;
	    boolean IsTitle = false;
	    boolean overall = false;
	    int rowNum = 0;
	    StringBuffer titleBuf = new StringBuffer();
	    StringBuffer abstractBuf = new StringBuffer();
	    StringBuffer citBuf = new StringBuffer();
	    String prevPos = new String("-1");
	    boolean duplicateSentence = true;
	    // int signalNum = 3;
	    int entityNum = 3;
	    int eventNum = 1;
	    int attributeNum = 1;

	    String sentence = null;
	    String prevSentence = null;
	    int offset = 0;
	    String SymbolS = null;
	    String ObjectSymbolS = null;
	    String EventS = null;
	    String AttributeSymbol = null;
	    String PMID = null;
	    String prevPMID = null;
	    String pos = null;
	    BufferedReader semtypein = new BufferedReader(new FileReader(typein));
	    BufferedReader semrepin = new BufferedReader(new FileReader(semrepfile));
	    BufferedReader textin = new BufferedReader(new FileReader(textfile));
	    System.out.println("Reading semrep file " + PMID + ".semrep");
	    String aLine = null;
	    String fLine = null;
	    int numPredication = 0;
	    int numEvent = 0;
	    while ((aLine = semtypein.readLine()) != null) {
		String compo[] = aLine.split("\\|");
		// System.out.println(compo[1].trim() + ", " + compo[0].trim());
		semtypeHt.put(compo[1].trim(), compo[0].trim());
	    }

	    /**
	     * Converting semrep output into Brat
	     */
	    int citPos = 0;
	    int relNum = eventNum;
	    int refNum = 1;
	    int coreferenceNum = 1;
	    int offsetMargin = 0;
	    int titleLen = 0;
	    List<BratSentence> sentList = new ArrayList<>();
	    String syntacticUnit = null;
	    String curText = null;
	    String PMIDinText = null;
	    String NextPMIDinText = null;

	    while ((aLine = semrepin.readLine()) != null) {
		// System.out.println(aLine);
		if (aLine.length() > 0) {
		    if (aLine.startsWith("["))
			syntacticUnit = new String(aLine);
		    String compo[] = aLine.split("\\|");
		    // System.out.println(compo[5]);
		    if (compo.length > 5) {
			if (PMID == null || (prevPMID != null && !prevPMID.equals(compo[1]) && compo[5].equals("text")))
			    startNewDoc = true;
			prevPMID = PMID;
			PMID = compo[1];
		    }

		    if (startNewDoc) {
			// prevPMID = PMID;
			// System.out.println("PMID = " + PMID);
			offsetMargin = 13 + PMID.length();
			if (citBuf.length() > 0) {
			    // txtout.println(citBuf);
			    // For regular version, it does not print sentences
			    /*
			     * for(BratSentence bsen : sentList) { annout.println("# SENT|" + bsen.sentNum +
			     * "|" + bsen.startPos + "|" + bsen.endPos + "|" + bsen.syntacticUnit);
			     * System.out.println("# SENT|" + bsen.sentNum + "|" + bsen.startPos + "|" +
			     * bsen.endPos + "|" + bsen.syntacticUnit); }
			     */
			    // txtout.close();
			    // annout.close();
			    citBuf = new StringBuffer();
			    titleBuf = new StringBuffer();
			    abstractBuf = new StringBuffer();
			    titleLen = 0;
			}
			if (annout != null)
			    annout.close();
			if (numPredication != numEvent) {
			    System.out.println(
				    "\t### conversion error: " + prevPMID + "\t" + numPredication + "\t:" + numEvent);
			}
			String separator = System.getProperty("file.separator");
			txtout = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outDir + separator + PMID + ".txt"))));
			annout = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outDir + separator + PMID + ".ann"))));
			curText = null;
			startNewDoc = false;
			offset = 0;
			entityNum = 3;
			attributeNum = 1;
			eventNum = 1;
			EventS = null;
			SymbolS = null;
			// EventName = null;
			// firstTitle = true;
			refNum = 1;
			relNum = 1;
			numPredication = 0;
			numEvent = 0;

			if (NextPMIDinText == null) { // retriving PMID
			    while ((fLine = textin.readLine()) != null) { // read
				if (fLine.startsWith("PMID")) {
				    PMIDinText = fLine.substring(6).trim();
				    break;
				}
			    }
			} else
			    PMIDinText = NextPMIDinText;

			while ((fLine = textin.readLine()) != null) { // read
								      // text
			    if (fLine.startsWith("PMID")) {
				NextPMIDinText = fLine.substring(6).trim();
				txtout.println(curText);
				txtout.close();
				// curText = null;
				break;
			    } else if (fLine.startsWith("TI")) {
				curText = fLine.substring(6).trim();
			    } else if (fLine.length() > 0)
				curText = curText + "\n" + fLine;
			}
			if (curText != null && txtout != null) { // Do this for
								 // last PMID
			    txtout.println(curText);
			    txtout.close();
			}
		    }
		    if (compo.length >= 6 && compo[5].equals("text")) { // source
									// sentence
									// sentence = compo[6].trim().replaceAll("\\s+", " ");
			prevPMID = PMID;
			sentence = compo[6];
			// System.out.println(sentence);
			citPos = citBuf.length();
			int sentNum = Integer.parseInt(compo[4]);
			BratSentence curSent = new BratSentence(sentence, sentNum, citPos, citPos + sentence.length(),
				syntacticUnit);
			sentList.add(curSent);
			/*
			 * August 15 2017 For all semrep output, the positions is "ti.n", since abstract
			 * are concatenated to title with only new line. So, append "\n" only when the
			 * position info is "ti.1"
			 */
			/*
			 * if(compo[3].equals("ti") && compo[4].equals("1")) { citBuf.append(sentence +
			 * "\n"); } else { citBuf.append(sentence + " "); }
			 */
			// sentPos = 0;
			// firstEntity = true;
		    } else if (compo.length > 6) {
			String preferredName = compo[7];
			String sTypes[] = compo[8].split(",");
			String sType = sTypes[0];
			if (compo[5].equals("entity")) {
			    // if(!entityHt.contains(compo[7])) { // Finding new
			    // Concepts
			    // System.out.println(compo[8]);
			    String semtypeFullname = (String) semtypeHt.get(sType);
			    String CUI = compo[6].trim();
			    if (CUI.length() <= 0)
				CUI = compo[9];
			    if (CUI.length() > 0) {
				// System.out.println(semtypeFullname + ", " +
				// CUI);
				// System.out.println(CUI + ", " + compo[16] +
				// ", " + compo[17]);
				int semrepStartPos = Integer.parseInt(compo[16]);
				int semrepEndPos = Integer.parseInt(compo[17]);
				int startPos = semrepStartPos - offsetMargin;
				int endPos = semrepEndPos - offsetMargin;
				// sentPos = endPos + 1;
				// TextEvidence te = new TextEvidence(CUI,
				// Integer.parseInt(compo[16]),
				// Integer.parseInt(compo[17]));
				TextEvidence te = new TextEvidence(CUI, startPos, endPos);
				// if(!entityHt.containsKey(te)) {
				String SymbolT = new String("T" + entityNum);
				String SymbolN = new String("N" + refNum);
				entityNum++; // Increase entity number;
				refNum++;
				// System.out.println(SymbolT + " -> (" + CUI +
				// ", " + startPos + ", " + endPos + ")");
				entityHt.put(te, SymbolT);
				// System.out.println(startPos + "," + endPos);
				// String citPart =
				// citSource.substring(startPos, endPos);
				// annout.println(SymbolT + "\t" + sType + " " +
				// startPos + " " + endPos + "\t" + compo[11]);
				annout.println(SymbolT + "\t" + semtypeFullname + " " + startPos + " " + endPos + "\t"
					+ compo[11]);
				if (compo[6].startsWith("C"))
				    annout.println(SymbolN + "\tReference " + SymbolT + " Metathesaurus:" + compo[6]);
				else
				    annout.println(SymbolN + "\tReference " + SymbolT + " SemRepGene:" + compo[9]);
				annout.flush();
			    }
			    // }
			} else if (compo[5].equals("coreference")) {
			    // if(!entityHt.contains(compo[7])) { // Finding new
			    // Concepts
			    // System.out.println(compo[8]);
			    String semtypeFullname = (String) semtypeHt.get(sType);
			    String sortal = compo[11].trim();

			    if (sortal.length() > 0) {
				// System.out.println(semtypeFullname + ", " +
				// CUI);
				// System.out.println("Sortal Anaphor:" +
				// sortal);
				int semrepStartPos = Integer.parseInt(compo[16]);
				int semrepEndPos = Integer.parseInt(compo[17]);
				int startPos = semrepStartPos - offsetMargin;
				int endPos = semrepEndPos - offsetMargin;
				// TextEvidence te = new TextEvidence(CUI,
				// Integer.parseInt(compo[16]),
				// Integer.parseInt(compo[17])) //
				// if(!entityHt.containsKey(te)) {
				String SymbolT = new String("T" + entityNum);
				// String SymbolN = new String("N" + refNum);
				entityNum++; // Increase entity number;
				String SymbolSPAN = new String("T" + entityNum);
				entityNum++; // Increase entity number;
				// System.out.println(SymbolT + " -> (" + sortal
				// + ", " + compo[16] + ", " + compo[17] + ")");
				// String citPart =
				// citSource.substring(startPos, endPos);
				annout.println(SymbolT + "\tSortalAnaphor " + startPos + " " + endPos + "\t" + sortal);
				annout.println(SymbolSPAN + "\tSPAN " + compo[29] + " " + compo[30] + "\t" + compo[24]);
				String SymbolCOREF = new String("R" + coreferenceNum);
				annout.println(SymbolCOREF + "\tCoreference Anaphora:" + SymbolT + " Antecedent:"
					+ SymbolSPAN);
				coreferenceNum++; // Increase entity number;
				annout.flush();
			    }
			    // }
			} else if (compo[5].equals("relation")) {
			    numPredication++;
			    String SymbolT = new String("T" + entityNum);
			    entityNum++; // Increase entity number;
			    String SymbolE = new String("E" + relNum);
			    relNum++;
			    String RelationFullName[] = compo[22].split("\\(");
			    // String Relation = RelationFullName[0];
			    String orgRelation = compo[22];
			    String Relation = null;
			    String attrName = null;
			    if (orgRelation.contains("(")) {
				int index = orgRelation.indexOf("(");
				Relation = orgRelation.substring(0, index);
				// attrName = orgRelation.substring(index+1,
				// orgRelation.length()-1);
				attrName = compo[21];
				// System.out.println(attrName);
			    } else
				Relation = orgRelation;
			    // String RelationSuffix =
			    // System.out.println(Relation);
			    // System.out.println(offsetDiff);
			    int semrepStartPos = Integer.parseInt(compo[24]);
			    int semrepEndPos = Integer.parseInt(compo[25]);
			    int startPos = semrepStartPos - offsetMargin;
			    int endPos = semrepEndPos - offsetMargin;
			    // System.out.println(aLine);
			    // System.out.println("offset : " + startPos + " - "
			    // + endPos);
			    // System.out.println("citBuf = " +
			    // citBuf.toString());
			    // int startPos = sentence.indexOf(compo[11],
			    // sentPos);
			    // int endPos = startPos + compo[11].length();
			    String citPart = curText.substring(startPos, endPos);
			    annout.println(SymbolT + "\t" + Relation + " " + startPos + " " + endPos + "\t" + citPart);
			    numEvent++;
			    TextEvidence tesubj = null;
			    if (compo[8].trim().length() > 0)
				tesubj = new TextEvidence(compo[8].trim(), Integer.parseInt(compo[19]) - offsetMargin,
					Integer.parseInt(compo[20]) - offsetMargin);
			    else
				tesubj = new TextEvidence(compo[12].trim(), Integer.parseInt(compo[19]) - offsetMargin,
					Integer.parseInt(compo[20]) - offsetMargin);
			    TextEvidence teobj = null;
			    if (compo[28].trim().length() > 0)
				teobj = new TextEvidence(compo[28].trim(), Integer.parseInt(compo[39]) - offsetMargin,
					Integer.parseInt(compo[40]) - offsetMargin);
			    else
				teobj = new TextEvidence(compo[32].trim(), Integer.parseInt(compo[39]) - offsetMargin,
					Integer.parseInt(compo[40]) - offsetMargin);
			    String symbolSubj = (String) entityHt.get(tesubj);
			    String symbolObj = (String) entityHt.get(teobj);
			    // System.out.println(symbolSubj + " -> (" + tesubj
			    // + ", " + Integer.parseInt(compo[19]) + ", " +
			    // Integer.parseInt(compo[20]) + ")");
			    // System.out.println(symbolObj + " -> (" + teobj +
			    // ", " + Integer.parseInt(compo[39]) + ", " +
			    // Integer.parseInt(compo[40]) + ")");
			    annout.println(SymbolE + "\t" + Relation + ":" + SymbolT + " Subject:" + symbolSubj
				    + " Object:" + symbolObj);
			    if (attrName != null && attrName.equals("INFER")) {
				String attributeA = new String("A" + attributeNum);
				attributeNum++;
				annout.println(attributeA + "\tInference " + SymbolE + " true");
				// System.out.println(attributeA + "\tInference
				// " + SymbolE + " true");
			    }
			    if (compo[23].equals("negation")) {
				String attributeA = new String("A" + attributeNum);
				attributeNum++;
				annout.println(attributeA + "\tNegation " + SymbolE + " true");
				// System.out.println(attributeA + "\tNegation "
				// + SymbolE + " true");
			    }
			    annout.flush();
			}
		    }
		}
	    }

	    txtout.close();
	    annout.close();
	    // errorOut.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void readBratFiles(String bratDir) {
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
	    String[] children = dir.list();
	    for (int j = 0; j < children.length; j++) {
		String bratFile = new String(bratDir + separator + children[j]);
		String PMID = children[j].substring(0, children[j].indexOf("."));
		String txtFile = new String(bratDir + separator + PMID + ".txt");
		readABratFile(bratFile, txtFile);

	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public BratDocument readABratFile(String bratFile, String txtFile) {
	String syntacticUnit = null;
	List<BratConcept> conceptList = new ArrayList<>();
	List<BratPredication> predicationList = new ArrayList<>();
	List<BratSentence> sentenceList = new ArrayList<>();
	BratDocument doc = null;
	try {
	    System.out.println("In readABratFile()");
	    Hashtable entityHt = new Hashtable(); // Hashtable for Concept ->
						  // Symbol (Ti)
	    boolean startNewDoc = false;
	    PrintWriter txtout = null;
	    PrintWriter annout = null;
	    boolean IsTitle = false;
	    boolean overall = false;
	    int rowNum = 0;
	    StringBuffer titleBuf = new StringBuffer();
	    StringBuffer abstractBuf = new StringBuffer();
	    StringBuffer citBuf = new StringBuffer();
	    String prevPos = new String("-1");
	    boolean duplicateSentence = true;
	    // int signalNum = 3;
	    int entityNum = 3;
	    int eventNum = 1;
	    int attributeNum = 1;

	    String sentence = null;
	    String prevSentence = null;
	    int offset = 0;
	    String SymbolS = null;
	    String ObjectSymbolS = null;
	    String EventS = null;
	    String AttributeSymbol = null;
	    // String PMID = null;
	    // String prevPMID = null;
	    String pos = null;
	    String line = null;
	    BufferedReader bratin = new BufferedReader(new FileReader(bratFile));
	    BufferedReader txtin = new BufferedReader(new FileReader(txtFile));
	    MedlineSentenceSegmenter segmenter = new MedlineSentenceSegmenter();

	    int dotIndex = bratFile.lastIndexOf(".");
	    String separator = System.getProperty("file.separator");
	    int sepIndex = bratFile.lastIndexOf(separator);

	    String PMID = bratFile.substring(sepIndex + 1, dotIndex);
	    System.out.println("PMID = " + PMID);
	    int count = 0;
	    StringBuffer citTextBuf = new StringBuffer();
	    while ((line = txtin.readLine()) != null) {
		citTextBuf.append(line + "\n");
	    }
	    String citText = citTextBuf.toString();

	    // List sentenceList = new ArrayList();

	    /*
	     * segmenter.segment(citText.toString(), sentenceList); for(Sentence sent :
	     * sentenceList) { System.out.println(sent.getId() + " : " + sent.getText() +
	     * sent.); }
	     */
	    HashMap<String, BratConcept> conceptMap = new HashMap<>();
	    while ((line = bratin.readLine()) != null) {
		line = line.trim();
		// System.out.println(line);
		String[] component = line.split("\t");
		if (component.length >= 2) {
		    if (component[0].startsWith("T") && component.length > 2) { // Brat
										// entity
			String[] typeInfo = component[1].split(" ");
			String semtypeOrg = semtypeHtReverse.get(typeInfo[0].trim());
			String semtype = null;
			if (semtypeOrg == null) { // For relation like ISA,
						  // TREATS, there is no match
						  // System.out.println("relation : " +
						  // typeInfo[0].trim());
			    semtype = typeInfo[0].trim(); // If there is no
							  // match in semtype
							  // list, use the
							  // original name
			} else
			    semtype = semtypeOrg;
			int startOffset = Integer.parseInt(typeInfo[1]);
			int endOffset = Integer.parseInt(typeInfo[2]);
			/*
			 * There is no name for the term (PMID: 22327139). T68 FunctionalConcept 1080
			 * 1080 Because of semrep error
			 * SE|22327139||ti|9|entity|C1518422|Negation|ftcn|||||| |766|1101|1101
			 * 
			 */
			if (component.length == 3) { // If only there is a concept
			    String name = component[2].trim();
			    BratConcept concept = new BratConcept(name, semtype, startOffset, endOffset);
			    conceptMap.put(component[0].trim(), concept);
			    if (semtypeOrg != null || semtype.startsWith("GNP") || semtype.startsWith("OMIM")) // if the concept is not a predicate
				conceptList.add(concept);
			}
		    } else if (component[0].startsWith("N")) { // Brat reference
			String[] subc = component[1].split(" ");
			BratConcept conceptFound = conceptMap.get(subc[1]);
			// BratConcept conceltInList =
			// conceptList.get(conceptFound);
			// ConceptFound can be null if semrep output for entity
			// has error
			if (conceptFound != null) {
			    String[] types = subc[2].split(":");
			    String concepttype = types[0]; // cocnept type can be MethaThesusrus, EntrezGene, or Gene
			    if (concepttype.equals("Metathesaurus")) {
				String cui = types[1]; // retrieving CUI from reference
				conceptFound.CUI = cui;
			    } else if (concepttype.equals("SemRepGene")) {
				String geneId = types[1];
				conceptFound.geneId = types[1];
				conceptFound.geneType = new String("semrep");
			    } else if (concepttype.equals("GNP_Gene") || concepttype.equals("Gene")) {
				String geneId = types[1];
				conceptFound.geneId = types[1];
				conceptFound.geneType = new String("gnormplus");
			    } else if (concepttype.equals("OMIMGene")) {
				String geneId = types[1];
				conceptFound.geneId = types[1];
				conceptFound.geneType = new String("omim");
			    }
			}
		    } else if (component[0].startsWith("E")) { // Brat relation
			String[] subc = component[1].split(" ");
			String predSymbol = subc[0].split(":")[1];
			BratConcept predConcept = conceptMap.get(predSymbol);
			String subjSymbol = subc[1].split(":")[1];
			BratConcept subjConcept = conceptMap.get(subjSymbol);
			String objSymbol = subc[2].split(":")[1];
			BratConcept objConcept = conceptMap.get(objSymbol);
			BratPredication predication = new BratPredication(subjConcept, predConcept, objConcept);
			predicationList.add(predication);
		    }
		} else if (line.startsWith("# SYNTAX")) { // Brat comment used
							  // for Syntactic
							  // Unit
		    String compo[] = line.split("\\|");
		    syntacticUnit = compo[1];
		    // System.out.println(line);
		} else if (line.startsWith("# SENT")) { // Brat comment used for
							// sentecne position
		    String compo[] = line.split("\\|");
		    // System.out.println(line);
		    int sentNum = Integer.parseInt(compo[1]);
		    int sentStart = Integer.parseInt(compo[2]);
		    int sentEnd = Integer.parseInt(compo[3]);
		    syntacticUnit = compo[4];
		    String sentText = null;
		    if (sentEnd + 1 < citText.length())
			sentText = citText.substring(sentStart, sentEnd + 1);
		    else
			sentText = citText.substring(sentStart, sentEnd);
		    BratSentence bsent = new BratSentence(sentText, sentNum, sentStart, sentEnd, syntacticUnit);
		    sentenceList.add(bsent);
		}
	    }
	    /*
	     * for(BratSentence bs: sentenceList) { System.out.println(bs.text + " |" +
	     * bs.sentNum + "|" + bs.startPos + "|" + bs.endPos + "\n" ); }
	     */
	    // System.out.println("Concept List size = " + conceptList.size());
	    for (BratConcept bc : conceptList) {
		// System.out.println(bc.name + " | " + bc.semtype + " | " +
		// bc.startOffset + " | " + bc.endOffset);
		int startSent = 0;
		while (startSent < sentenceList.size()) {
		    BratSentence bs = sentenceList.get(startSent);
		    if (bc.startOffset >= bs.startPos && bc.endOffset <= bs.endPos) {
			bc.sentNum = bs.sentNum;
			// System.out.println(" \t--> " + bc.name + " | " +
			// bc.semtype + " | " + bc.sentNum + " | " +
			// bc.startOffset + " | " + bc.endOffset);
			break;
		    } else
			startSent++;
		}
	    }

	    Collections.sort(conceptList, new ConceptComparator()); // sort the
								    // concepts
								    // based on
								    // sent
								    // number
								    // and start
								    // offset
	    doc = new BratDocument(PMID, sentenceList, conceptList, predicationList, citText);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	return doc;

    }

    /*
     * Write BratDocument and species Information back to Brat files Input argument
     * doc preserves the same information obtained from brat file read previous step
     */
    public void writeSpeciesInfoToABratFile(BratDocument doc, SpeciesInfoInDocument SIDoc, String dirStr) {
	String separator = System.getProperty("file.separator");
	String txtFileName = dirStr + separator + doc.PMID + ".txt";
	String annFileName = dirStr + separator + doc.PMID + ".ann";
	Map<TextEvidence, String> ConceptMap = new HashMap<>();
	try {
	    PrintWriter txtout = new PrintWriter(new File(txtFileName));
	    PrintWriter annout = new PrintWriter(new File(annFileName));
	    txtout.println(doc.text);
	    txtout.close();

	    int entityNum = 1;
	    int attNum = 1;
	    int refNum = 1;
	    int relNum = 1;
	    /*
	     * for(BratConcept bc: doc.conceptList) { String semtypeFullname =
	     * semtypeHt.get(bc.semtype); String SymbolT = new String("T" + entityNum);
	     * entityNum++; // Increase entity number; annout.println(SymbolT + "\t" +
	     * semtypeFullname + " " + bc.startOffset + " " + bc.endOffset + "\t" +
	     * bc.name); TextEvidence te = new TextEvidence(bc.startOffset, bc.endOffset);
	     * String prevSymbol = ConceptMap.get(te); if(prevSymbol == null)
	     * ConceptMap.put(te, SymbolT); String SymbolN = null; if(bc.CUI != null) {
	     * SymbolN = new String("N" + refNum); refNum++; annout.println(SymbolN +
	     * "\tReference " + SymbolT + " Metathesaurus:" + bc.CUI); } annout.flush(); }
	     * 
	     * for(BratPredication bp: doc.predicationList) { String SymbolT = new
	     * String("T" + entityNum); entityNum++; // Increase entity number; String
	     * SymbolE = new String("E" + relNum); relNum++; TextEvidence tes = new
	     * TextEvidence(bp.subjConcept.startOffset, bp.subjConcept.endOffset); String
	     * subjectSymbol = ConceptMap.get(tes); String relation = bp.predConcept.name;
	     * TextEvidence teo = new TextEvidence(bp.objConcept.startOffset,
	     * bp.objConcept.endOffset); String objectSymbol = ConceptMap.get(teo);
	     * annout.println(SymbolE + "\t" + relation + ":" + SymbolT+ " Subject:" +
	     * subjectSymbol + " Object:" + objectSymbol); annout.flush(); }
	     */
	    int firstSpace = doc.text.indexOf(" ");
	    String firstWord = doc.text.substring(0, firstSpace);
	    String firstSymbol = new String("T" + entityNum);
	    annout.println(firstSymbol + "\tOverallResult 0 " + firstSpace + "\t" + firstWord);
	    entityNum++;
	    if (SIDoc.abstractLevelSpecies.size() > 0) { // write abstract level
							 // species
		for (String sp : SIDoc.abstractLevelSpecies) {
		    String attrName = new String("A" + refNum);
		    refNum++;
		    annout.println(attrName + "\tSpecies " + firstSymbol + " " + sp);
		}
	    }
	    if (SIDoc.abstractLevelModel.size() > 0) { // write abstract level
						       // species
		for (String sp : SIDoc.abstractLevelModel) {
		    String attrName = new String("A" + refNum);
		    refNum++;
		    annout.println(attrName + "\tModel " + firstSymbol + " " + sp);
		}
	    }
	    if (SIDoc.abstractLevelSpeciesModel.size() > 0) { // write abstract
							      // level species
		for (String sp : SIDoc.abstractLevelSpeciesModel) {
		    String attrName = new String("A" + refNum);
		    refNum++;
		    annout.println(attrName + "\tSpeciesModel " + firstSymbol + " " + sp);
		}
	    }

	    for (SpeciesInfoInSentence sis : SIDoc.sentenceInfo) {
		for (BratConcept bcs : sis.species) {
		    String SymbolT = new String("T" + entityNum);
		    entityNum++; // Increase entity number;
		    annout.println(SymbolT + "\tSpecies " + bcs.startOffset + " " + bcs.endOffset + "\t" + bcs.name);
		}
		for (BratConcept bcm : sis.models) {
		    String SymbolT = new String("T" + entityNum);
		    entityNum++; // Increase entity number;
		    annout.println(SymbolT + "\tModel " + bcm.startOffset + " " + bcm.endOffset + "\t" + bcm.name);
		}
	    }

	    // 6/19/2018 Temporarily blocking the sentence info writing
	    for (BratSentence bsen : doc.sentenceList) {
		// annout.println("# SENT|" + bsen.sentNum + "|" + bsen.startPos + "|" + bsen.endPos);
	    }
	    annout.close();

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    /*
     * Write sentence to database if the sentence is not in the SENTENCE table
     */
    public void writeSentenceToDB(BratDocument doc) {

	try {
	    String PMID = doc.PMID;

	    for (BratSentence bs : doc.sentenceList) {
		int sentNum = bs.sentNum;
		int startPos = bs.startPos;
		int endPos = bs.endPos;
		String sent = bs.text;
		sent = sent.replaceAll("[\"]", "\\\"\"");
		String SelectSentStmt = new String(mysqlSelectSentence + PMID + "\"" + " and START_POS = " + startPos);
		ResultSet rs = stmt.executeQuery(SelectSentStmt);
		if (!rs.next()) {
		    String InsertSentStmt = new String(mysqlInsertSentence + PMID + "\"," + sentNum + "," + startPos
			    + "," + endPos + ",\"" + sent + "\")");

		    System.out.println(InsertSentStmt);
		    try {
			stmt.executeUpdate(InsertSentStmt);
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
		rs.close();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /*
     * Write BratDocument and species Information back to Brat files Input argument
     * doc preserves the same information obtained from brat file read previous step
     */
    public void writeSpeciesInfoToDB(BratDocument doc, SpeciesInfoInDocument SIDoc, String dirStr) {
	String separator = System.getProperty("file.separator");
	Map<TextEvidence, String> ConceptMap = new HashMap<>();

	String sqlabstractString = new String("INSERT INTO ABSTRACT_ANNOTATION (PMID, FEATURE, VALUE) VALUE (\"");
	try {
	    String PMID = doc.PMID;
	    List speciesList = SIDoc.abstractLevelSpecies;
	    List modelList = SIDoc.abstractLevelModel;
	    List speciesModelList = SIDoc.abstractLevelSpeciesModel;
	    for (Object obj : speciesList) {
		String species = (String) obj;
		String sqlAbstractStmt = new String(sqlabstractString + PMID + "\",\"Species\",\"" + species + "\")");
		System.out.println(sqlAbstractStmt);
		try {
		    stmt.executeUpdate(sqlAbstractStmt);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }

	    for (Object obj : modelList) {
		String model = (String) obj;
		String sqlAbstractStmt = new String(sqlabstractString + PMID + "\",\"Model\",\"" + model + "\")");
		System.out.println(sqlAbstractStmt);
		try {
		    stmt.executeUpdate(sqlAbstractStmt);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }

	    for (Object obj : speciesModelList) {
		String speciesModel = (String) obj;
		String sqlAbstractStmt = new String(
			sqlabstractString + PMID + "\",\"SpeciesModel\",\"" + speciesModel + "\")");
		System.out.println(sqlAbstractStmt);
		try {
		    stmt.executeUpdate(sqlAbstractStmt);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }

	    for (SpeciesInfoInSentence sis : SIDoc.sentenceInfo) {
		int sentNum = sis.sentNumber;

		for (BratConcept bc : sis.species) {
		    String term = bc.name;
		    String type = bc.semtype;
		    int start = bc.startOffset;
		    int end = bc.endOffset;

		    String SelectSentStmt = new String(
			    mysqlSelectSentence + PMID + "\"" + " and SENT_NUMBER = " + sentNum);
		    ResultSet rs = stmt.executeQuery(SelectSentStmt);
		    int sentId = 0;
		    if (rs != null) {
			rs.next();
			sentId = rs.getInt(1);
		    }

		    String InsertSpeciesStmt = new String(mysqlInsertSpecies + sentId + ",\"" + term + "\",\"" + type
			    + "\", " + start + "," + end + ")");

		    System.out.println(InsertSpeciesStmt);
		    try {
			stmt.executeUpdate(InsertSpeciesStmt);
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    /*
     * Write gene information Information back to Brat files Input argument doc
     * preserves the same information obtained from brat file read previous step
     */
    public void writeGeneInfoToABratFile(BratDocument doc, List<BratConcept> conceptList, String dirStr) {
	String separator = System.getProperty("file.separator");
	String txtFileName = dirStr + separator + doc.PMID + ".txt";
	String annFileName = dirStr + separator + doc.PMID + ".ann";
	Map<TextEvidence, String> ConceptMap = new HashMap<>();
	int entityNum = 1;
	int refNum = 1;
	try {
	    PrintWriter txtout = new PrintWriter(new File(txtFileName));
	    PrintWriter annout = new PrintWriter(new File(annFileName));
	    txtout.println(doc.text);

	    for (BratConcept bcs : conceptList) {
		String SymbolT = new String("T" + entityNum);
		entityNum++; // Increase entity number;
		annout.println(SymbolT + "\tGene " + bcs.startOffset + " " + bcs.endOffset + "\t" + bcs.name);
		// July 2 2018
		// Add gene id to the Gene output
		// If ther is not multiple gene
		// simplified, what matters at this point is whether the ID is coming from
		if (bcs.geneType != null) {
		    String SymbolN = new String("N" + refNum);
		    if (bcs.geneType.equals("EntrezGene")) {
			annout.println(SymbolN + "\tReference " + SymbolT + " EntrezGene:" + bcs.geneId);
		    } else if (bcs.geneType.equals("Metathesaurus")) {
			annout.println(SymbolN + "\tReference " + SymbolT + " Metathesaurus:" + bcs.CUI);
		    }
		    refNum++;
		}
	    }
	    txtout.close();
	    annout.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    // not needed anymore -- Halil
    public int writeGeneReference(String geneId, String geneType, PrintWriter annout, String SymbolT, int refNum) {
	String SymbolN = new String("N" + refNum);
	if (geneType.equals("semrep")) {
	    String[] geneids = geneId.split(",");
	    for (int i = 0; i < geneids.length; i++) {
		annout.println(SymbolN + "\tReference " + SymbolT + " Semrep:" + geneids[i]);
		refNum++;
		SymbolN = new String("N" + refNum);
	    }
	} else if (geneType.equals("gnormplus"))
	    annout.println(SymbolN + "\tReference " + SymbolT + " Gene:" + geneId);
	else if (geneType.equals("omim"))
	    annout.println(SymbolN + "\tReference " + SymbolT + " OMIM:" + geneId);
	return refNum;
    }

    public void writeGeneInfoToDB(BratDocument doc, List<BratConcept> conceptList, String dirStr) {
	Map<TextEvidence, String> ConceptMap = new HashMap<>();
	int entityNum = 1;
	int refNum = 1;
	try {

	    for (BratConcept bcs : conceptList) {
		String PMID = doc.PMID;
		int sentNum = bcs.sentNum;
		String term = bcs.name;
		String type = bcs.semtype;
		int start = bcs.startOffset;
		int end = bcs.endOffset;

		String SelectSentStmt = new String(mysqlSelectSentence + PMID + "\"" + " and SENT_NUMBER = " + sentNum);
		ResultSet rs = stmt.executeQuery(SelectSentStmt);
		int sentId = 0;
		if (rs != null) {
		    rs.next();
		    sentId = rs.getInt(1);
		}

		String InsertGeneStmt = null;

		if (bcs.geneType != null) {
		    String SymbolN = new String("N" + refNum);
		    if (bcs.geneType.equals("EntrezGene")) {
			InsertGeneStmt = new String(mysqlInsertGene + sentId + ",\"" + term + "\",\"E" + "\",\""
				+ bcs.geneId + "\"," + start + "," + end + ")");
		    } else if (bcs.geneType.equals("Metathesaurus")) {
			InsertGeneStmt = new String(mysqlInsertGene + sentId + ",\"" + term + "\",\"M" + "\",\""
				+ bcs.CUI + "\"," + start + "," + end + ")");
		    }
		}

		System.out.println(InsertGeneStmt);
		try {
		    stmt.executeUpdate(InsertGeneStmt);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /*
     * Write gene information Information back to Brat files Input argument doc
     * preserves the same information obtained from brat file read previous step
     */
    public void writeInterventionInfoToABratFile(BratDocument doc, InterventionResult iresult, String dirStr) {
	String separator = System.getProperty("file.separator");
	String txtFileName = dirStr + separator + doc.PMID + ".txt";
	String annFileName = dirStr + separator + doc.PMID + ".ann";
	Map<TextEvidence, String> ConceptMap = new HashMap<>();
	List<BratConcept> chemList = iresult.chemList;
	List<BratConcept> semrepList = iresult.semrepList;
	List<BratConcept> GNormList = iresult.GNormList;
	List<BratConcept> mustConceptList = iresult.mustConceptList;
	List<String> MOAList = iresult.MOAList;
	int entityNum = 1;
	int refNum = 1;
	try {
	    // PrintWriter txtout
	    // = new PrintWriter(new File(txtFileName));
	    PrintWriter annout = new PrintWriter(new File(annFileName));
	    // txtout.println(doc.text);
	    int firstSpace = doc.text.indexOf(" ");
	    String firstWord = doc.text.substring(0, firstSpace);
	    String firstSymbol = new String("T" + entityNum);
	    entityNum++;
	    annout.println(firstSymbol + "\tAll 0 " + firstSpace + "\t" + firstWord);
	    for (String moa : MOAList) {
		String attrName = new String("A" + refNum);
		refNum++;
		annout.println(attrName + "\tInterventionClass " + firstSymbol + " " + moa);
	    }

	    List<String> seen = new ArrayList<>();

	    for (BratConcept ccs : chemList) {
		String SymbolT = new String("T" + entityNum);
		entityNum++; // Increase entity number;
		String SymbolN = new String("N" + entityNum);
		refNum++; // Increase entity number;
		annout.println(SymbolT + "\tIntervention " + ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name);
		// it is not true that 'C' concepts are from MT, they're MeSH supplementary concepts
		if (ccs.CUI != null && ccs.CUI.startsWith("D"))
		    annout.println(SymbolN + "\tReference " + SymbolT + " MESH:" + ccs.CUI);
		else if (ccs.CUI != null && ccs.CUI.startsWith("C") && ccs.CUI.length() == 8)
		    annout.println(SymbolN + "\tReference " + SymbolT + " Metathesaurus:" + ccs.CUI);
		else if (ccs.CUI != null && ccs.CUI.startsWith("C"))
		    annout.println(SymbolN + "\tReference " + SymbolT + " MESH:" + ccs.CUI);
		else if (ccs.CUI != null)
		    annout.println(SymbolN + "\tReference " + SymbolT + " Other:" + ccs.CUI);
		seen.add(ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name);
	    }
	    for (BratConcept ccs : semrepList) {
		if (seen.contains(ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name))
		    continue;
		String SymbolT = new String("T" + entityNum);
		entityNum++; // Increase entity number;
		String SymbolN = new String("N" + entityNum);
		refNum++; // Increase entity number;
		annout.println(SymbolT + "\tIntervention " + ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name);
		if (ccs.CUI != null && ccs.CUI.startsWith("D"))
		    annout.println(SymbolN + "\tReference " + SymbolT + " MESH:" + ccs.CUI);
		else if (ccs.CUI != null && ccs.CUI.startsWith("C") && ccs.CUI.length() == 8)
		    annout.println(SymbolN + "\tReference " + SymbolT + " Metathesaurus:" + ccs.CUI);
		else if (ccs.CUI != null && ccs.CUI.startsWith("C"))
		    annout.println(SymbolN + "\tReference " + SymbolT + " MESH:" + ccs.CUI);
		else if (ccs.CUI != null)
		    annout.println(SymbolN + "\tReference " + SymbolT + " Other:" + ccs.CUI);
		seen.add(ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name);
	    }

	    for (BratConcept ccs : GNormList) {
		if (seen.contains(ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name))
		    continue;
		String SymbolT = new String("T" + entityNum);
		entityNum++; // Increase entity number;
		String SymbolN = new String("N" + entityNum);
		refNum++; // Increase entity number;
		annout.println(SymbolT + "\tIntervention " + ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name);
		if (ccs.CUI != null && ccs.CUI.startsWith("D"))
		    annout.println(SymbolN + "\tReference " + SymbolT + " MESH:" + ccs.CUI);
		else if (ccs.CUI != null && ccs.CUI.startsWith("C") && ccs.CUI.length() == 8)
		    annout.println(SymbolN + "\tReference " + SymbolT + " Metathesaurus:" + ccs.CUI);
		else if (ccs.CUI != null && ccs.CUI.startsWith("C"))
		    annout.println(SymbolN + "\tReference " + SymbolT + " MESH:" + ccs.CUI);
		//		else if (ccs.CUI == null && ccs.geneId != null)
		else if (ccs.geneId != null)
		    annout.println(SymbolN + "\tReference " + SymbolT + " Gene:" + ccs.geneId);
		else if (ccs.CUI != null)
		    annout.println(SymbolN + "\tReference " + SymbolT + " Other:" + ccs.CUI);
		seen.add(ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name);
	    }

	    // GNormList is duplicated in chemList if there is some
	    /*
	     * for (BratConcept ccs : GNormList) { String SymbolT = new String("T" +
	     * entityNum); entityNum++; // Increase entity number; String SymbolN = new
	     * String("N" + entityNum); refNum++; // Increase entity number;
	     * annout.println(SymbolT + "\tIntervention " + ccs.startOffset + " " +
	     * ccs.endOffset + "\t" + ccs.name); if (ccs.CUI != null &&
	     * ccs.CUI.startsWith("D")) annout.println(SymbolN + "\tReference " + SymbolT +
	     * " MESH:" + ccs.CUI); else if (ccs.CUI != null && ccs.CUI.startsWith("C"))
	     * annout.println(SymbolN + "\tReference " + SymbolT + " Metathesaurus:" +
	     * ccs.CUI); }
	     */
	    for (BratConcept ccs : mustConceptList) {
		if (seen.contains(ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name))
		    continue;
		String SymbolT = new String("T" + entityNum);
		entityNum++; // Increase entity number;
		String SymbolN = new String("N" + entityNum);
		refNum++; // Increase entity number;
		annout.println(SymbolT + "\tIntervention " + ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name);
		if (ccs.CUI != null && ccs.CUI.startsWith("D"))
		    annout.println(SymbolN + "\tReference " + SymbolT + " MESH:" + ccs.CUI);
		else if (ccs.CUI != null && ccs.CUI.startsWith("C") && ccs.CUI.length() == 8)
		    annout.println(SymbolN + "\tReference " + SymbolT + " Metathesaurus:" + ccs.CUI);
		else if (ccs.CUI != null && ccs.CUI.startsWith("C"))
		    annout.println(SymbolN + "\tReference " + SymbolT + " MESH:" + ccs.CUI);
		else if (ccs.CUI != null && ccs.CUI.matches("^[0-9]+$"))
		    annout.println(SymbolN + "\tReference " + SymbolT + " Gene:" + ccs.CUI);
		else if (ccs.CUI != null) {
		    annout.println(SymbolN + "\tReference " + SymbolT + " Other:" + ccs.CUI);
		}
		seen.add(ccs.startOffset + " " + ccs.endOffset + "\t" + ccs.name);
	    }
	    // txtout.close();
	    annout.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void writeInterventionInfoToDB(BratDocument doc, InterventionResult iresult) {
	Map<TextEvidence, String> ConceptMap = new HashMap<>();
	int entityNum = 1;
	int refNum = 1;
	List<BratConcept> chemList = iresult.chemList;
	List<BratConcept> semrepList = iresult.semrepList;
	List<BratConcept> GNormList = iresult.GNormList;
	List<BratConcept> mustConceptList = iresult.mustConceptList;
	List<String> MOAList = iresult.MOAList;
	String InsertInterventionStmt = null;
	String sqlabstractString = new String("INSERT INTO ABSTRACT_ANNOTATION (PMID, FEATURE, VALUE) VALUE (\"");
	try {
	    // Add PMID to PUBLICATION table if it was not already added
	    String SelectPMIDStmt = new String(mysqlSelectPMID + doc.PMID + "\"");
	    ResultSet rsPMID = stmt.executeQuery(SelectPMIDStmt);
	    String PMID = null;
	    /*-  while (rsPMID.next()) {
	    PMID = rsPMID.getString(1);
	    }
	    if (PMID == null) {
	    String InsertPMIDStmt = new String(mysqlInsertPMID + doc.PMID + "\")");
	    try {
	        stmt.executeUpdate(InsertPMIDStmt);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    }
	     for (BratSentence bsent : doc.sentenceList) { // Add sentencs to SENTENCE table if it is not already in the database
	    String SelectSentStmt = new String(
	    	mysqlSelectSentence + doc.PMID + "\"" + " and SENT_NUMBER = " + bsent.sentNum);
	    ResultSet rs = stmt.executeQuery(SelectSentStmt);
	    int sentId = 0;
	    while (rs.next()) {
	        sentId = rs.getInt(1);
	    }
	    if (sentId <= 0) { // If there is no sentence in the database, insert it
	        String sent = bsent.text;
	        sent = sent.replaceAll("[\"]", "\\\"\"");
	        String InsertSentStmt = new String(mysqlInsertSentence + doc.PMID + "\"," + bsent.sentNum + ","
	    	    + bsent.startPos + "," + bsent.endPos + ",\"" + sent + "\")");
	        try {
	    	stmt.executeUpdate(InsertSentStmt);
	        } catch (Exception e) {
	    	e.printStackTrace();
	        }
	    
	    }
	    } */

	    for (String moa : MOAList) {
		String attrName = new String("A" + refNum);
		refNum++;
		InsertInterventionStmt = new String(
			sqlabstractString + doc.PMID + "\",\"Intervention Class\",\"" + moa + "\")");
		System.out.println(InsertInterventionStmt);
		try {
		    stmt.executeUpdate(InsertInterventionStmt);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	    List<String> seen = new ArrayList<>();

	    for (BratConcept bcs : chemList) {
		PMID = doc.PMID;
		int sentNum = bcs.sentNum;
		String term = bcs.name;
		String type = bcs.semtype;
		int start = bcs.startOffset;
		int end = bcs.endOffset;

		String SelectSentStmt = new String(mysqlSelectSentence + PMID + "\"" + " and SENT_NUMBER = " + sentNum);
		ResultSet rs = stmt.executeQuery(SelectSentStmt);
		int sentId = 0;
		while (rs.next()) {
		    sentId = rs.getInt(1);
		}

		String InsertGeneStmt = null;

		if (bcs.CUI != null && bcs.CUI.length() > 0) {
		    // String SymbolN = new String("N" + refNum);
		    InsertInterventionStmt = new String(mysqlInsertIntervention + sentId + ",\"" + term + "\",\"MESH"
			    + "\",\"" + bcs.CUI + "\"," + start + "," + end + ")");

		    seen.add(bcs.startOffset + " " + bcs.endOffset + "\t" + bcs.name);
		}

		System.out.println(InsertInterventionStmt);
		try {
		    stmt.executeUpdate(InsertInterventionStmt);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }

	    for (BratConcept bcs : semrepList) {
		PMID = doc.PMID;
		int sentNum = bcs.sentNum;
		String term = bcs.name;
		String type = bcs.semtype;
		int start = bcs.startOffset;
		int end = bcs.endOffset;

		String SelectSentStmt = new String(mysqlSelectSentence + PMID + "\"" + " and SENT_NUMBER = " + sentNum);
		ResultSet rs = stmt.executeQuery(SelectSentStmt);
		int sentId = 0;
		while (rs.next()) {
		    sentId = rs.getInt(1);
		}

		String InsertGeneStmt = null;

		if (seen.contains(bcs.startOffset + " " + bcs.endOffset + "\t" + bcs.name))
		    continue;
		if (bcs.CUI != null && bcs.CUI.startsWith("D")) {
		    InsertInterventionStmt = new String(mysqlInsertIntervention + sentId + ",\"" + term + "\",\"MESH"
			    + "\",\"" + bcs.CUI + "\"," + start + "," + end + ")");
		} else if (bcs.CUI != null && bcs.CUI.startsWith("C")) {
		    InsertInterventionStmt = new String(mysqlInsertIntervention + sentId + ",\"" + term + "\",\"META"
			    + "\",\"" + bcs.CUI + "\"," + start + "," + end + ")");
		}
		seen.add(bcs.startOffset + " " + bcs.endOffset + "\t" + bcs.name);

		System.out.println(InsertInterventionStmt);
		try {
		    stmt.executeUpdate(InsertInterventionStmt);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }

	    for (BratConcept bcs : GNormList) {
		PMID = doc.PMID;
		int sentNum = bcs.sentNum;
		String term = bcs.name;
		String type = bcs.semtype;
		int start = bcs.startOffset;
		int end = bcs.endOffset;

		String SelectSentStmt = new String(mysqlSelectSentence + PMID + "\"" + " and SENT_NUMBER = " + sentNum);
		ResultSet rs = stmt.executeQuery(SelectSentStmt);
		int sentId = 0;
		while (rs.next()) {
		    sentId = rs.getInt(1);
		}

		String InsertGeneStmt = null;

		if (seen.contains(bcs.startOffset + " " + bcs.endOffset + "\t" + bcs.name))
		    continue;
		if (bcs.CUI != null && bcs.CUI.startsWith("D")) {
		    InsertInterventionStmt = new String(mysqlInsertIntervention + sentId + ",\"" + term + "\",\"MESH"
			    + "\",\"" + bcs.CUI + "\"," + start + "," + end + ")");
		} else if (bcs.CUI != null && bcs.CUI.startsWith("C")) {
		    InsertInterventionStmt = new String(mysqlInsertIntervention + sentId + ",\"" + term + "\",\"META"
			    + "\",\"" + bcs.CUI + "\"," + start + "," + end + ")");
		} else { // There is no CUI found, add empty
		    InsertInterventionStmt = new String(mysqlInsertIntervention + sentId + ",\"" + term + "\",\""
			    + "\",\"\"," + start + "," + end + ")");
		}
		seen.add(bcs.startOffset + " " + bcs.endOffset + "\t" + bcs.name);

		System.out.println(InsertInterventionStmt);
		try {
		    stmt.executeUpdate(InsertInterventionStmt);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }

	    for (BratConcept bcs : mustConceptList) {
		PMID = doc.PMID;
		int sentNum = bcs.sentNum;
		String term = bcs.name;
		String type = bcs.semtype;
		int start = bcs.startOffset;
		int end = bcs.endOffset;

		String SelectSentStmt = new String(mysqlSelectSentence + PMID + "\"" + " and SENT_NUMBER = " + sentNum);
		ResultSet rs = stmt.executeQuery(SelectSentStmt);
		int sentId = 0;
		while (rs.next()) {
		    sentId = rs.getInt(1);
		}

		String InsertGeneStmt = null;

		if (seen.contains(bcs.startOffset + " " + bcs.endOffset + "\t" + bcs.name))
		    continue;
		if (bcs.CUI != null && bcs.CUI.startsWith("D")) {
		    InsertInterventionStmt = new String(mysqlInsertIntervention + sentId + ",\"" + term + "\",\"MESH"
			    + "\",\"" + bcs.CUI + "\"," + start + "," + end + ")");
		} else if (bcs.CUI != null && bcs.CUI.startsWith("C")) {
		    InsertInterventionStmt = new String(mysqlInsertIntervention + sentId + ",\"" + term + "\",\"META"
			    + "\",\"" + bcs.CUI + "\"," + start + "," + end + ")");
		} else { // There is no CUI found, add empty
		    InsertInterventionStmt = new String(mysqlInsertIntervention + sentId + ",\"" + term + "\",\""
			    + "\",\"\"," + start + "," + end + ")");
		}
		seen.add(bcs.startOffset + " " + bcs.endOffset + "\t" + bcs.name);

		System.out.println(InsertInterventionStmt);
		try {
		    stmt.executeUpdate(InsertInterventionStmt);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void gnormPlusTest() {
	/*
	 * String text =
	 * " An evaluation of neuropsychiatric symptoms in Parkinson's disease patients.\n"
	 * +
	 * "OBJECTIVE: We aimed to examine neuropsychiatric symptoms of patients with early and advanced stage Parkinson's disease (PD). MATERIALS AND METHODS: The study was performed at Kocatepe University Neurology Department in Turkey, comprised 46 PD patients and 46 controls."
	 * +
	 * " Hoehn-Yahr (HY) scale was used to evaluate the clinical stages of PD and Unified Parkinson's Disease Rating Scale (UPDRS) was used to evaluate the severity of clinical signs. Cognitive functions were evaluated by Mini-Mental State Examination (MMSE) and neuropsychiatric findings were evaluated by Beck Depression Inventory (BDI), Scale for the Assessment of Positive Symptoms (SAPS), and Scale for the Assessment of Negative Symptoms (SANS). RESULTS: Significant difference was determined between BDI values of patients (13.28 +/- 9.04) and control group (9.71 +/- 5.19) (P = 0.02). Significant difference was determined with SANS (23.84 +/- 15.42, 2.58 +/- 3.13, P < 0.001) but not with SAPS (1.36 +/- 4.16, 0.15 +/- 0.43, P = 0.07). The patients were evaluated according to the HY stages and there was no significant difference between mild and severe symptom groups in respect of BDI, SAPS, and SANS values (P = 0.91, P = 0.31, and P = 0.29). According to gender, no significant difference was found between groups in respect of BDI, SAPS, and SANS values (P = 0.60, P = 0.54, and P = 0.67). No correlation was found between BDI, SAPS, SANS values, and HY stages. CONCLUSION: Higher rates of depression and negative symptoms were observed in patients with PD compared with healthy individuals. Results did not differ with different stages of PD. Therefore, it should be kept in mind that neuropsychiatric symptoms can be seen from the early stages of the disease and should be treated earlier."
	 * ;
	 */
	String text = "Parkinson's disease: a complex disease revisited.\n";
	try {
	    gpw = GNormPlusStringWrapper.getInstance("setup.txt");
	    Map<SpanList, LinkedHashSet<Ontology>> annotations = gpw.annotateText(text);
	    for (SpanList sp : annotations.keySet()) {
		LinkedHashSet<Ontology> concs = annotations.get(sp);
		for (Ontology c : concs) {
		    Concept con = (Concept) c;

		    int startPos = sp.getBegin();
		    int endPos = sp.getEnd();
		    String type = con.getSemtypes().toString();
		    System.out.println("\tname = " + con.getName());
		    System.out.println("\t\tGNormType = " + type);
		    System.out.println("\t\tstart = " + startPos + "\tendPos = " + endPos);

		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static void main(String[] args) {

	try {
	    System.out.println("Program start...");

	    // sr.process("args[0],
	    // "Q:\\LHC_Projects\\Caroline\\testset\\normed100.semrep.txt",
	    // "Q:\\LHC_Projects\\Caroline\\testset\\brat2");
	    // System.out.println("Calling SemRepToBrat.process() with " +
	    // args[0]);
	    // System.out.println(args[1] + " " + args[2]);
	    Brat brat = Brat.getInstance();
	    brat.semrepGNormPlusToBrat(args[0], args[1], args[2], args[3]);
	    // brat.semrepToBrat(args[0], args[1], args[2]);

	    // brat.semrepToBrat("Q:\\LHC_Projects\\Caroline\\NEW_2017\\semTypeFullName.txt",
	    // "Q:\\LHC_Projects\\Caroline\\NEW_2017\\3_years_data\\semrep.0",
	    // "Q:\\LHC_Projects\\Caroline\\NEW_2017\\3_years_data\\cits.0",
	    // "Q:\\LHC_Projects\\Caroline\\NEW_2017\\3_years_data\\brat");
	    // Brat brat =
	    // Brat.getInstance("Q:\\LHC_Projects\\Caroline\\NEW_2017\\semTypeFullName.txt",
	    // "Q:\\LHC_Projects\\Caroline\\NEW_2017\\parkinson_omim_genes_expanded_06162017.txt");
	    // brat.semrepToBrat("Q:\\LHC_Projects\\Caroline\\NEW_2017\\3_years_data\\bug\\22327139.semrep",
	    // "Q:\\LHC_Projects\\Caroline\\NEW_2017\\3_years_data\\bug\\22327139.txt",
	    // "Q:\\LHC_Projects\\Caroline\\NEW_2017\\3_years_data\\bug\\brat");
	    // brat.semrepToBrat("Q:\\LHC_Projects\\Caroline\\NEW_2017\\semTypeFullName.txt",
	    // "Q:\\LHC_Projects\\Caroline\\GoldStandard\\SpeciesGS\\98test.semrepFRZ2015",
	    // "Q:\\LHC_Projects\\Caroline\\GoldStandard\\SpeciesGS\\98test.norm",
	    // "Q:\\LHC_Projects\\Caroline\\GoldStandard\\SpeciesGS\\98test_brat");
	    // brat.gnormPlusTest();
	    // brat.semrepToBrat(args[0], args[1], args[2]);
	    // brat.semrepToBratReg("Q:\\LHC_Projects\\Halil\\negation\\semTypeFullName.txt",
	    // "Q:\\LHC_Projects\\Halil\\negation\\neg_norm.semrep.txt",
	    // "Q:\\LHC_Projects\\Halil\\negation\\brat2");
	    // brat.semrepToBratReg("Q:\\LHC_Projects\\Halil\\negation\\semTypeFullName.txt",
	    // "Q:\\LHC_Projects\\Halil\\negation\\neg_norm2.semrep",
	    // "Q:\\LHC_Projects\\Halil\\negation\\brat4");
	    // brat.semrepToBratReg("Q:\\LHC_Projects\\Halil\\negation\\semTypeFullName.txt",
	    // "Q:\\LHC_Projects\\Halil\\negation\\19894388.semrep",
	    // "Q:\\LHC_Projects\\Halil\\negation\\brattest");
	    // brat.semrepToBratReg("Q:\\LHC_Projects\\Halil\\negation\\semTypeFullName.txt",
	    // "Q:\\LHC_Projects\\Halil\\negation\\20393963.semrep",
	    // "Q:\\LHC_Projects\\Halil\\negation\\20393963_norm.medline",
	    // "Q:\\LHC_Projects\\Halil\\negation\\brattest");
	    // brat.semrepToBratFactuality("Q:\\LHC_Projects\\Halil\\negation\\semTypeFullName.txt",
	    // "Q:\\LHC_Projects\\Halil\\negation\\neg_norm2.semrep",
	    // "Q:\\LHC_Projects\\Halil\\negation\\neg_norm2.medline",
	    // "Q:\\LHC_Projects\\Halil\\negation\\brat6");

	    /*
	     * brat.semrepToBratFactuality(
	     * "Q:\\LHC_Projects\\Halil\\negation\\semTypeFullName.txt",
	     * "Q:\\LHC_Projects\\Halil\\negation\\semrep1.8\\neg_norm2.semrep",
	     * "Q:\\LHC_Projects\\Halil\\negation\\semrep1.8\\neg_norm2.medline",
	     * "Q:\\LHC_Projects\\Halil\\negation\\semrep1.8\\brat-03162018"); /* Hashtable
	     * semtypeHt = new Hashtable(); BufferedReader semtypein = new
	     * BufferedReader(new FileReader(args[2])); String aLine = null; while((aLine =
	     * semtypein.readLine()) != null) { String compo[] = aLine.split("\\|"); //
	     * System.out.println(compo[1].trim() + ", " + compo[0].trim());
	     * semtypeHt.put(compo[0].trim(), compo[1].trim()); //
	     * System.out.println(compo[1].trim() + ", " + compo[0].trim()); } //
	     * BratDocument doc = brat.readABratFile(args[0], args[1], semtypeHt);
	     * BratDocument doc = brat.readABratFile(args[0], args[1], semtypeHt);
	     */
	    /*
	     * for(BratSentence bs: doc.sentenceList) { System.out.println(bs.text + " |" +
	     * bs.sentNum + "|" + bs.startPos + "|" + bs.endPos + "\n" ); }
	     */
	    // BratDocument doc =
	    // brat.readABratFile("Q:\\LHC_Projects\\Caroline\\NEW_2017\\3_years_data\\22327139.ann",
	    // "Q:\\LHC_Projects\\Caroline\\NEW_2017\\3_years_data\\22327139.txt");
	    /*
	     * SpeciesModel sm = SpeciesModel.getInstance(
	     * "Q:\\LHC_Projects\\Caroline\\data\\Field1ListSpecies_06222017",
	     * "Q:\\LHC_Projects\\Caroline\\data\\Field1ListSpecies_06222017");
	     * SpeciesInfoInDocument speciesDoc = sm.generateSM(doc);
	     */
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
