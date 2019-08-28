package gov.nih.nlm.skr.outcomes.ml;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.process.ComponentLoader;
import gov.nih.nlm.ling.process.IndicatorAnnotator;
import gov.nih.nlm.ling.process.TermAnnotator;
import gov.nih.nlm.ling.sem.Indicator;
import gov.nih.nlm.ling.sem.Modification;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ml.stats.ClassificationStats;
import gov.nih.nlm.skr.outcomes.DocumentInstance;
import gov.nih.nlm.skr.outcomes.DocumentInstance.OutcomePolarity;
import gov.nih.nlm.skr.outcomes.Utils;
import gov.nih.nlm.util.Config;
import gov.nih.nlm.util.Log;

/**
 * Test for overall outcome classification. Two classifiers are involved: A POS
 * Outcome classifier and a NEG outcome classifier. The results of these
 * classifiers are merged for a 4-way classification: POS, NEG, MIXED, and
 * OTHER.
 *
 * @author kilicogluh
 */
public class OutcomePrediction {
    private static final Log log = new Log(OutcomePrediction.class);

    private static Properties props = null;
    private final static OutcomePOSClassifier posClassifier = new OutcomePOSClassifier();
    private final static OutcomeNEGClassifier negClassifier = new OutcomeNEGClassifier();

    // private static Map<String,String> normalizedSectionLabels = null;
    private static LinkedHashSet<Indicator> polarityDictionary = null;
    private static LinkedHashSet<String> clinicalTermDictionary = null;
    private static Map<Class<? extends SemanticItem>, List<String>> annTypes;
    private static XMLReader xmlReader;

    public static String DATA_DIR;

    // public static Map<String,String> CLINICAL_TERM_LABELS = new HashMap<>();

    public static void init(String[] argv) throws IOException {
	Config.init("config.properties", argv);
	props = FileUtils.loadPropertiesFromFile("config.properties");
	// normalizedSectionLabels =
	// Utils.loadNormalizedSectionLabels(props.getProperty("structuredAbstractLabelFile",
	// "resources/Structured-Abstracts-Labels-110613.txt"));
	polarityDictionary = Utils
		.loadSentimentDictionary(props.getProperty("polarityDictionaryFile", "resources/polarity.dic"));
	clinicalTermDictionary = Utils.loadClinicalTermDictionary(
		props.getProperty("clinicalTermDictionaryFile", "resources/clinical_term_list.txt"));
	annTypes = Utils.getAnnotationTypes();
	xmlReader = Utils.getXMLReader();

	// loadClinicalTermLabels(props.getProperty("clinicalOutcomeTrainDirectory"));
    }

    private static Iterable<DocumentInstance> testRequests() throws Exception {
	return processDir(DATA_DIR);
    }

    public static Iterable<DocumentInstance> processDir(String dir) throws Exception {
	List<DocumentInstance> instances = new ArrayList<>();
	List<String> files = FileUtils.listFiles(dir, false, "xml");
	int fileNum = 0;
	for (String filename : files) {
	    String filenameNoExt = filename.replace(".xml", "");
	    filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator) + 1);
	    // if (Integer.parseInt(filenameNoExt) != 2765955) continue;
	    log.info("Processing " + filenameNoExt + ":" + ++fileNum);
	    DocumentInstance instance = processFile(filename);
	    instances.add(instance);
	}
	return instances;
    }

    public static DocumentInstance processFile(String filename) throws Exception {
	Document doc = xmlReader.load(filename, true, SemanticItemFactory.class, annTypes, null);
	annotatePolarity(doc, props);
	LinkedHashSet<SemanticItem> outcomes = Document.getSemanticItemsByClassType(doc, Modification.class,
		Arrays.asList("OverallOutcome"));
	DocumentInstance instance = null;
	// If there is a gold annotation for the document
	if (outcomes.size() > 0) {
	    Modification outcome = (Modification) outcomes.iterator().next();
	    String val = outcome.getValue();
	    if (val.equals("UNDETERMINED") || val.equals("INEFFECTIVE") || val.equals("NEUTRAL"))
		val = "OTHER";
	    instance = new DocumentInstance(doc.getId(), doc, OutcomePolarity.valueOf(val));

	    instance.setMetaData("goldOutcome", val);
	} else {
	    instance = new DocumentInstance(doc.getId(), doc, OutcomePolarity.OTHER);
	    // Add this 08/22/2019
	    instance.setMetaData("goldOutcome", "OTHER");

	}
	instance.setContext(Utils.getContext(instance));
	return instance;
    }

    public static void annotatePolarity(Document doc, Properties properties)
	    throws InstantiationException, IllegalAccessException, ClassNotFoundException {
	properties.setProperty("termAnnotators", "gov.nih.nlm.ling.process.IndicatorAnnotator");
	List<TermAnnotator> termAnnotators = ComponentLoader.getTermAnnotators(properties);
	for (TermAnnotator annotator : termAnnotators) {
	    if (annotator instanceof IndicatorAnnotator) {
		((IndicatorAnnotator) annotator).setIndicators(polarityDictionary);
		String ignore = properties.getProperty("ignorePOSforIndicators");
		boolean ignorePOS = Boolean.parseBoolean(ignore == null ? "false" : ignore);
		((IndicatorAnnotator) annotator).setIgnorePOS(ignorePOS);
	    }
	    ((IndicatorAnnotator) annotator).annotateIndicators(doc, properties);
	}
    }

    public static void testPOSClassifier(List<DocumentInstance> instances) {
	ClassificationStats stats = posClassifier.test(instances);
	log.info("Features: {0}", posClassifier.getFeatures());
	OutcomeTrain.printStats(stats);
    }

    public static void testNEGClassifier(List<DocumentInstance> instances) {
	ClassificationStats stats = negClassifier.test(instances);
	log.info("Features: {0}", negClassifier.getFeatures());
	OutcomeTrain.printStats(stats);
    }

    public static void combine(Statement stmt, List<DocumentInstance> instances) {
	final ClassificationStats stats = new ClassificationStats();
	for (int i = 0; i < instances.size(); i++) {
	    DocumentInstance ins = instances.get(i);
	    String gold = ins.getMetaData("goldOutcome");
	    String guess = predict(ins);
	    boolean clinical = hasClinicalTerm(ins);
	    // System.out.println(ins.getId() + "|" + guess + "|" + gold + "|" +
	    // CLINICAL_TERM_LABELS.get(ins.getId()) + "|" + (clinical ?
	    // "Functional(" +
	    // String.join(",",getClinicalTerms(ins.getDocument())) +")" :
	    // "NoFunctional"));
	    // System.out.println(ins.getId() + "|" + guess + "|" + gold + "|" + (clinical
	    //		? "Functional(" + String.join(",", getClinicalTerms(ins.getDocument())) + ")" : "NoFunctional"));
	    System.out.println(ins.getId() + "|" + guess + "|" + gold + "|"
		    + (clinical ? String.join(";", getClinicalTerms(ins.getDocument())) : "NO"));
	    insertOutcomeToDB(stmt, ins.getId(), guess, clinical,
		    String.join(";", getClinicalTerms(ins.getDocument())));
	    stats.addInstance(gold, guess);
	}
	OutcomeTrain.printStats(stats);
    }

    public static boolean hasClinicalTerm(String filename) {
	Document doc = xmlReader.load(filename, true, SemanticItemFactory.class, annTypes, null);
	return hasClinicalTerm(doc);
    }

    public static Statement getStmtFromDB(String connectionString, String DBname, String username, String passwd)
	    throws Exception {
	Class.forName("com.mysql.jdbc.Driver");
	Connection conn = DriverManager.getConnection(connectionString + "/" + DBname + "?autoReconnect=true", username,
		passwd);
	Statement stmt = conn.createStatement();
	return stmt;
    }

    /*- public static void insertOverallOutcomeToDB(Statement stmt, String PMID, String clinicalValue, String triggerTerm) {
    // String insert = new String("insert into ABSTRACT_ANNOTATION (PMID,
    // FEATURE, VALUE) VALUES (\"");
    try {
        StringBuilder insertStmt = new StringBuilder(
    	    "insert into ABSTRACT_ANNOTATION (PMID, FEATURE, VALUE, TRIGGERTERM) VALUES (\"");
        insertStmt.append(PMID + "\",\"OverallOutcome\",\"" + value + "\")");
        System.out.println(insertStmt.toString());
        stmt.execute(insertStmt.toString());
    } catch (Exception e) {
        e.printStackTrace();
    }
    } */

    public static void insertOutcomeToDB(Statement stmt, String PMID, String guess, boolean boolValue,
	    String triggerTerm) {
	// String insert = new String("insert into ABSTRACT_ANNOTATION (PMID,
	// FEATURE, VALUE) VALUES (\"");
	try {
	    StringBuilder insertClinicalStmt = new StringBuilder(
		    "insert into ABSTRACT_ANNOTATION (PMID, FEATURE, VALUE, TRIGGERTERM) VALUES (\"");
	    String insertOverallStmt = new String(
		    "insert into ABSTRACT_ANNOTATION (PMID, FEATURE, VALUE, TRIGGERTERM) VALUES (\"" + PMID
			    + "\",\"OverallOutcome\",\"" + guess + "\",\"\")");
	    String clinicalValue = null;
	    if (boolValue == true)
		clinicalValue = new String("YES");
	    else
		clinicalValue = new String("NO");
	    insertClinicalStmt
		    .append(PMID + "\",\"ClinicalOutcome\",\"" + clinicalValue + "\",\"" + triggerTerm + "\")");
	    System.out.println(insertOverallStmt);
	    System.out.println(insertClinicalStmt.toString());
	    stmt.execute(insertOverallStmt);
	    stmt.execute(insertClinicalStmt.toString());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static String predict(DocumentInstance ins) {
	final String posGuess = posClassifier.classify(ins);
	final String negGuess = negClassifier.classify(ins);
	String guess = "";
	if (posGuess.equals("POS") && negGuess.equals("NNEG"))
	    guess = "POS";
	else if (posGuess.equals("POS") && negGuess.equals("NEG"))
	    guess = "MIXED";
	else if (posGuess.equals("NPOS") && negGuess.equals("NEG"))
	    guess = "NEG";
	else if (posGuess.equals("NPOS") && negGuess.equals("NNEG"))
	    guess = "OTHER";
	return guess;
    }

    public static String predict(Document doc, boolean annotated)
	    throws InstantiationException, IllegalAccessException, ClassNotFoundException {
	if (!annotated)
	    annotatePolarity(doc, props);
	DocumentInstance instance = new DocumentInstance(doc.getId(), doc, null);
	return predict(instance);
    }

    public static String predict(String filename)
	    throws InstantiationException, IllegalAccessException, ClassNotFoundException {
	Document doc = xmlReader.load(filename, true, SemanticItemFactory.class, annTypes, null);
	annotatePolarity(doc, props);
	DocumentInstance instance = new DocumentInstance(doc.getId(), doc, null);
	return predict(instance);
    }

    public static boolean hasClinicalTerm(DocumentInstance ins) {
	return hasClinicalTerm(ins.getDocument());
    }

    public static boolean hasClinicalTerm(Document doc) {
	return (getClinicalTerms(doc).size() > 0);
    }

    /*
     * public static List<String> getClinicalTerms(Document doc) { List<Span>
     * context = Utils.getContext(doc); String contText = Utils.getContextText(doc,
     * context).toLowerCase(); List<String> seen = new ArrayList<>(); List<String>
     * terms = new ArrayList<>(); for (String t: clinicalTermDictionary) { Pattern
     * wordPattern = Pattern.compile("\\b" + Pattern.quote(t) + "\\b"); Matcher m =
     * wordPattern.matcher(contText); if (m.find()) { boolean subsumed = false; for
     * (String s: seen) { if (s.contains(t)) { subsumed = true; break; } } if
     * (!subsumed) { terms.add(t); seen.add(t); } } } return terms; }
     */

    public static List<String> getClinicalTerms(Document doc) {
	String docText = doc.getText().toLowerCase();
	List<String> seen = new ArrayList<>();
	List<String> terms = new ArrayList<>();
	for (String t : clinicalTermDictionary) {
	    Pattern wordPattern = Pattern.compile("\\b" + Pattern.quote(t) + "\\b");
	    Matcher m = wordPattern.matcher(docText);
	    if (m.find()) {
		boolean subsumed = false;
		for (String s : seen) {
		    if (s.contains(t)) {
			subsumed = true;
			break;
		    }
		}
		if (!subsumed) {
		    terms.add(t);
		    seen.add(t);
		}
	    }
	}
	return terms;
    }

    /*
     * public static void loadClinicalTermLabels(String filename) throws IOException
     * { List<String> lines = FileUtils.linesFromFile(filename, "UTF-8"); for
     * (String line: lines) { String[] els = line.split("\t"); if
     * (line.matches("^$")) continue; System.out.println("LINE:" + line +"|" +
     * els[0] + "|" + els[1].trim()); CLINICAL_TERM_LABELS.put(els[0],
     * els[1].trim()); } }
     */

    /**
     * Command line (intended entry).
     */
    public static void main(String[] argv) throws Exception {
	init(argv);

	DATA_DIR = props.getProperty("outcomeTestDirectory");
	String connectionString = props.getProperty("connectionString");
	String dbName = props.getProperty("dbName");
	String username = props.getProperty("dbUsername");
	String password = props.getProperty("dbPassword");
	Statement stmt = getStmtFromDB(connectionString, dbName, username, password);

	List<DocumentInstance> testRequests = (List<DocumentInstance>) testRequests();

	List<DocumentInstance> testPOSInstances = OutcomeTrain.relabel(testRequests, "POS");
	List<DocumentInstance> testNEGInstances = OutcomeTrain.relabel(testRequests, "NEG");
	log.info("Testing for POS outcomes...");
	testPOSClassifier(testPOSInstances);
	log.info("Testing for NEG outcomes...");
	testNEGClassifier(testNEGInstances);
	log.info("Testing for all outcomes...");
	combine(stmt, testRequests);
    }
}
