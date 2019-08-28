package gov.nih.nlm.skr.CZ;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;

import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.sem.Concept;
import gov.nih.nlm.ling.sem.Ontology;
import gov.nih.nlm.ner.gnormplus.GNormPlusStringWrapper;

public class SemRepToBrat {
    private static GNormPlusStringWrapper gpw = null;
    private static SemRepToBrat srb = null;

    public static SemRepToBrat getInstance() throws IOException {
	if (srb == null) {
	    System.out.println("Initializing a SemRepToBrat instance...");
	    srb = new SemRepToBrat();
	}
	return srb;
    }

    public SemRepToBrat() {
	try {
	    gpw = GNormPlusStringWrapper.getInstance("setup.txt");
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void process(String typein, String in, String outDir) {
	try {
	    Hashtable entityHt = new Hashtable(); // Hashtable for Concept -> Symbol (Ti)
	    Hashtable semtypeHt = new Hashtable(); // Hashtable for "tisu" -> "Tissue"
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
	    BufferedReader semrepin = new BufferedReader(new FileReader(in));
	    System.out.println("Reading semrep file " + PMID + ".semrep");
	    String aLine = null;
	    while ((aLine = semtypein.readLine()) != null) {
		String compo[] = aLine.split("\\|");
		// System.out.println(compo[1].trim() + ", " + compo[0].trim());
		semtypeHt.put(compo[1].trim(), compo[0].trim());
	    }

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
	    int titleLen = 0;

	    while ((aLine = semrepin.readLine()) != null) {
		// System.out.println(aLine);
		if (aLine.length() > 0) {
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
			if (citBuf.length() > 0) {
			    txtout.println(citBuf);
			    txtout.close();
			    System.out.println(abstractBuf);
			    abstractBuf.append("\n");
			    /*
			     * Call GNormPlusWrapper to extract GNormPlus output for abstract
			     * 
			     */
			    Map<SpanList, LinkedHashSet<Ontology>> annotations = gpw
				    .annotateText(abstractBuf.toString());
			    for (SpanList sp : annotations.keySet()) {
				LinkedHashSet<Ontology> concs = annotations.get(sp);
				for (Ontology c : concs) {
				    Concept con = (Concept) c;
				    String SymbolT = new String("T" + entityNum);
				    entityNum++;
				    int startPos = sp.getBegin() + titleLen;
				    int endPos = sp.getEnd() + titleLen;
				    String type = con.getSemtypes().toString();
				    type = type.substring(1, type.length() - 1);
				    annout.println(SymbolT + "\t" + type + " " + startPos + " " + endPos + "\t"
					    + con.getName());
				    System.out.println(sp.toString() + "\t" + con.getName() + "\t" + type);
				}
			    }

			    annout.close();
			    citBuf = new StringBuffer();
			    titleBuf = new StringBuffer();
			    abstractBuf = new StringBuffer();
			    titleLen = 0;
			}
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

		    }
		    if (compo.length > 6 && compo[5].equals("text")) { // source sentence
			// sentence = compo[6].trim().replaceAll("\\s+", " ");
			sentence = compo[6];
			// System.out.println(sentence);
			citPos = citBuf.length();
			/*
			 * August 15 2017 For all semrep output, the positions is "ti.n", since abstract
			 * are concatenated to title with only new line. So, append "\n" only when the
			 * position info is "ti.1"
			 */
			if (compo[3].equals("ti") && compo[4].equals("1")) {
			    citBuf.append(sentence + "\n");
			    titleBuf.append(sentence + "\n");
			    titleLen = titleBuf.length();
			    System.out.println(titleBuf);
			    Map<SpanList, LinkedHashSet<Ontology>> annotations = gpw.annotateText(titleBuf.toString());
			    for (SpanList sp : annotations.keySet()) {
				LinkedHashSet<Ontology> concs = annotations.get(sp);
				for (Ontology c : concs) {
				    Concept con = (Concept) c;
				    String SymbolT = new String("T" + entityNum);
				    entityNum++;
				    int startPos = sp.getBegin();
				    int endPos = sp.getEnd();
				    String type = con.getSemtypes().toString();
				    type = type.substring(1, type.length() - 1);
				    annout.println(SymbolT + "\t" + type + " " + startPos + " " + endPos + "\t"
					    + con.getName());
				    System.out.println(sp.toString() + "\t" + con.getName() + "\t" + type);
				}
			    }
			} else {
			    citBuf.append(sentence + " ");
			    abstractBuf.append(sentence + " ");
			}
			// sentPos = 0;
			// firstEntity = true;
		    } else if (compo.length > 6) {
			String preferredName = compo[7];
			String sTypes[] = compo[8].split(",");
			String sType = sTypes[0];
			if (compo[5].equals("entity")) {
			    // if(!entityHt.contains(compo[7])) { // Finding new Concepts
			    // System.out.println(compo[8]);
			    String semtypeFullname = (String) semtypeHt.get(sType);
			    String CUI = compo[6].trim();
			    if (CUI.length() <= 0)
				CUI = compo[9];
			    if (CUI.length() > 0) {
				// System.out.println(semtypeFullname + ", " + CUI);
				// System.out.println(CUI + ", " + compo[16] + ", " + compo[17]);
				int semrepStartPos = Integer.parseInt(compo[16]);
				int semrepEndPos = Integer.parseInt(compo[17]);
				int startPos = semrepStartPos - offsetMargin;
				int endPos = semrepEndPos - offsetMargin;
				// sentPos = endPos + 1;
				// TextEvidence te = new TextEvidence(CUI, Integer.parseInt(compo[16]), Integer.parseInt(compo[17]));
				TextEvidence te = new TextEvidence(CUI, startPos, endPos);
				// if(!entityHt.containsKey(te)) {
				String SymbolT = new String("T" + entityNum);
				String SymbolN = new String("N" + refNum);
				entityNum++; // Increase entity number;
				refNum++;
				// System.out.println(SymbolT + " -> (" + CUI + ", " + startPos + ", " + endPos + ")");
				entityHt.put(te, SymbolT);
				// System.out.println(startPos + "," + endPos);
				// String citPart = citSource.substring(startPos, endPos);
				annout.println(SymbolT + "\t" + semtypeFullname + " " + startPos + " " + endPos + "\t"
					+ compo[11]);
				if (compo[6].startsWith("C"))
				    annout.println(SymbolN + "\tReference " + SymbolT + " Metathesaurus:" + compo[6]);
				else
				    annout.println(SymbolN + "\tReference " + SymbolT + " EntrezGene:" + compo[9]);
				annout.flush();
			    }
			    // }
			} else if (compo[5].equals("coreference")) {
			    // if(!entityHt.contains(compo[7])) { // Finding new Concepts
			    // System.out.println(compo[8]);
			    String semtypeFullname = (String) semtypeHt.get(sType);
			    String sortal = compo[11].trim();

			    if (sortal.length() > 0) {
				// System.out.println(semtypeFullname + ", " + CUI);
				// System.out.println("Sortal Anaphor:" + sortal);
				int semrepStartPos = Integer.parseInt(compo[16]);
				int semrepEndPos = Integer.parseInt(compo[17]);
				int startPos = semrepStartPos - offsetMargin;
				int endPos = semrepEndPos - offsetMargin;
				// TextEvidence te = new TextEvidence(CUI, Integer.parseInt(compo[16]), Integer.parseInt(compo[17]))        						// if(!entityHt.containsKey(te)) {
				String SymbolT = new String("T" + entityNum);
				// String SymbolN = new String("N" + refNum);
				entityNum++; // Increase entity number;
				String SymbolSPAN = new String("T" + entityNum);
				entityNum++; // Increase entity number;
				// System.out.println(SymbolT + " -> (" + sortal + ", " + compo[16] + ", " + compo[17] + ")");
				// String citPart = citSource.substring(startPos, endPos);
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
			    // System.out.println("offset : " + startPos + " - " + endPos);
			    // System.out.println("citBuf = " + citBuf.toString());
			    // int startPos = sentence.indexOf(compo[11], sentPos);
			    // int endPos = startPos + compo[11].length();
			    String citPart = citBuf.substring(startPos, endPos);
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
			    // System.out.println(symbolSubj + " -> (" + tesubj + ", " + Integer.parseInt(compo[19])  + ", " + Integer.parseInt(compo[20]) + ")");
			    // System.out.println(symbolObj + " -> (" + teobj + ", " + Integer.parseInt(compo[39]) + ", " + Integer.parseInt(compo[40]) + ")");
			    annout.println(SymbolE + "\t" + Relation + ":" + SymbolT + " Subject:" + symbolSubj
				    + " Object:" + symbolObj);
			    annout.flush();
			}
		    }
		}
	    }
	    // txtout.println(citSource);

	    if (citBuf.length() > 0) {
		txtout.println(citBuf);
		txtout.close();
		System.out.println(citBuf);
		/*
		 * Call GNormPlusWrapper to extract GNormPlus output
		 * 
		 */
		Map<SpanList, LinkedHashSet<Ontology>> annotations = gpw.annotateText(citBuf.toString());
		for (SpanList sp : annotations.keySet()) {
		    LinkedHashSet<Ontology> concs = annotations.get(sp);
		    for (Ontology c : concs) {
			Concept con = (Concept) c;
			String SymbolT = new String("T" + entityNum);
			entityNum++;
			int startPos = sp.getBegin();
			int endPos = sp.getEnd();
			String type = con.getSemtypes().toString();
			type = type.substring(1, type.length() - 1);
			annout.println(SymbolT + "\t" + type + " " + startPos + " " + endPos + "\t" + con.getName());
			System.out.println(sp.toString() + "\t" + con.getName() + "\t" + type);
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

    public static void main(String[] args) {

	try {
	    System.out.println("Program start...");
	    SemRepToBrat sr = SemRepToBrat.getInstance();
	    // sr.process("args[0], "Q:\\LHC_Projects\\Caroline\\testset\\normed100.semrep.txt", "Q:\\LHC_Projects\\Caroline\\testset\\brat2");
	    // System.out.println("Calling SemRepToBrat.process() with " + args[0]);
	    // System.out.println(args[1] + " " + args[2]);
	    sr.process(args[0], args[1], args[2]);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
