package gov.nih.nlm.skr.outcomes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.EventAnnotation;
import gov.nih.nlm.ling.brat.ModificationAnnotation;
import gov.nih.nlm.ling.brat.StandoffAnnotationReader;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.process.ComponentLoader;
import gov.nih.nlm.ling.process.SentenceSegmenter;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Event;
import gov.nih.nlm.ling.sem.Modification;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.RelationArgument;
import gov.nih.nlm.ling.sem.RelationDefinition;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.wrappers.CoreNLPWrapper;
import nu.xom.Element;
import nu.xom.Serializer;

/**
 * 
 * @author kilicogluh
 *
 */
public class OutcomesToXMLWriter {
    private static Logger log = Logger.getLogger(OutcomesToXMLWriter.class.getName());

    public static final String FS = System.getProperty("file.separator");

    static Map<String, Element> sentPhrases = null;
    static List<String> parseTypes = new ArrayList<>();
    static SentenceSegmenter segmenter = null;

    static List<String> entityTypes = Arrays.asList("OverallOutcome", "OverallClinicalOutcome", "CLINICAL",
	    "NON-CLINICAL", "OutcomeObject", "OutcomeSignal");
    static List<String> relTypes = Arrays.asList("OUTCOME", "OutcomeSignal");
    static List<String> modTypes = Arrays.asList("OutcomeObjectPolarity", "ClinicalOutcome", "OverallOutcome",
	    "OverallClinicalOutcome", "SentenceOutcome");

    private static void analyze(Document doc, SentenceSegmenter segmenter) {
	List<Sentence> sentences = new ArrayList<>();
	segmenter.segment(doc.getText(), sentences);
	// Map<Span,Term> docTerms = new HashMap<Span,Term>();
	doc.setSentences(sentences);
	List<Sentence> modSentences = new ArrayList<>();
	for (int i = 0; i < doc.getSentences().size(); i++) {
	    Sentence sent = doc.getSentences().get(i);
	    sent.setDocument(doc);
	    // create word list, parse and dependencies
	    CoreNLPWrapper.coreNLP(sent);
	    List<SurfaceElement> surfs = new ArrayList<>();
	    surfs.addAll(sent.getWords());
	    sent.setSurfaceElements(surfs);
	    sent.setSurfaceElements(new ArrayList<SurfaceElement>(sent.getWords()));
	    sent.setEmbeddings(new ArrayList<>(sent.getDependencyList()));
	    modSentences.add(sent);
	}
	doc.setSentences(modSentences);
    }

    public static Element processSingleFile(String id, String txtFilename, String annFilename) throws Exception {
	Document doc = StandoffAnnotationReader.readTextFile(id, txtFilename);
	// osif = new OutcomeSemanticItemFactory(doc, new HashMap<Class<?
	// extends SemanticItem>,Integer>());
	// doc.setSemanticItemFactory(osif);
	SemanticItemFactory osif = doc.getSemanticItemFactory();
	analyze(doc, segmenter);
	Map<StandoffAnnotationReader.AnnotationType, List<String>> lines = StandoffAnnotationReader
		.readAnnotationFiles(Arrays.asList(annFilename), null, parseTypes);
	Map<Class, List<Annotation>> annotations = StandoffAnnotationReader.parseAnnotations(id, lines, null);
	Class[] processOrder = new Class[] { TermAnnotation.class, EventAnnotation.class,
		ModificationAnnotation.class };
	for (int i = 0; i < processOrder.length; i++) {
	    Class c = processOrder[i];
	    List<Annotation> anns = annotations.get(c);
	    if (anns == null)
		continue;
	    for (Annotation ann : anns) {
		String type = ann.getType();
		System.out.println(c.getName() + "|" + ann.toString());
		if (ann instanceof TermAnnotation) {
		    if (relTypes.contains(type)) {
			Predicate pr = osif.newPredicate(doc, (TermAnnotation) ann);
		    } else {
			Entity ent = osif.newEntity(doc, (TermAnnotation) ann);
		    }
		} else if (ann instanceof EventAnnotation && relTypes.contains(type)) {
		    // (SemRepConstants.SEMREP_RELATION_TYPES.contains(type) ||
		    // SemRepConstants.SEMREP_NEG_RELATION_TYPES.contains(type)))
		    // {
		    Event ev = osif.newEvent(doc, (EventAnnotation) ann);
		    /*
		     * String evid = ev.getId(); String line =
		     * relLines.get(Integer.parseInt(evid.substring(1))-1); String[] lels =
		     * line.split("[|]"); String relType = lels[22]; String specInfer = ""; if
		     * (relType.indexOf("(") > 0) { int ind = relType.indexOf("("); specInfer =
		     * relType.substring(ind+1,relType.indexOf(")",ind)); relType =
		     * relType.substring(0,ind); } String indType = lels[21]; boolean neg =
		     * lels[23].equals("negation"); ev.addFeature("indicatorType", indType); if
		     * (specInfer.equals("") == false) ev.addFeature("specInfer", specInfer); if
		     * (neg) { ev.addFeature("negation", "true"); }
		     */
		} else if (ann instanceof ModificationAnnotation && modTypes.contains(type)) {
		    // (SemRepConstants.SEMREP_MODIFICATION_TYPES.contains(type)))
		    // {
		    Modification mod = osif.newModification(doc, (ModificationAnnotation) ann);
		}
	    }
	}
	return doc.toXml();
    }

    public static Map<String, List<String>> readRelationLines(String filename) throws IOException {
	Map<String, List<String>> out = new HashMap<>();
	List<String> lines = FileUtils.linesFromFile(filename, "UTF-8");
	String id = "";
	int cnt = 0;
	for (String l : lines) {
	    // System.out.println("LINE: " + l);
	    if (l.trim().equals(""))
		continue;
	    String[] lels = l.split("[|]");

	    String type = lels[5];
	    if (type.equals("text")) {
		if (lels[1].equals(id) == false)
		    id = lels[1];
	    } else if (type.equals("relation")) {
		List<String> outRels = out.get(id);
		if (outRels == null)
		    outRels = new ArrayList<>();
		outRels.add(l);
		out.put(id, outRels);
	    }
	    cnt++;
	}
	System.out.println("READ " + cnt + " lines.");
	return out;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
	// TODO Auto-generated method stub
	try {
	    // PropertyConfigurator.configure("log4j.properties");
	    if (args.length < 2) {
		return;
	    }
	    String annDirName = args[0];
	    // String semrep = args[1];
	    String xmlDirName = args[1];
	    if (annDirName.equals(xmlDirName)) {
		System.err.println("The annotation directory and the xml directory are the same.");
		System.exit(1);
	    }
	    // if (new File(semrep).exists() == false) System.exit(1);
	    File annDir = new File(annDirName);
	    File xmlDir = new File(xmlDirName);
	    if (annDir.isDirectory() == false) {
		System.err.println("annDirectory is not a directory.");
		System.exit(1);
	    }
	    if (xmlDir.isDirectory() == false) {
		xmlDir.mkdir();
	    }
	    int fileNum = 0;
	    List<String> files = FileUtils.listFiles(annDirName, false, "ann");
	    Map<Class, List<String>> map = new HashMap<>();
	    Map<Class<? extends SemanticItem>, Set<String>> entityMap = new HashMap<>();
	    Map<Class<? extends SemanticItem>, Set<String>> eventMap = new HashMap<>();

	    List<String> entityAnns = new ArrayList<>();
	    entityAnns.addAll(SemRepConstants.SEMREP_ENTITY_TYPES);
	    entityAnns.addAll(entityTypes);
	    map.put(TermAnnotation.class, entityAnns);
	    entityMap.put(Entity.class, new HashSet<>(entityTypes));
	    // map.put(TermAnnotation.class,
	    // SemRepConstants.SEMREP_ENTITY_TYPES);
	    map.put(EventAnnotation.class, relTypes);
	    eventMap.put(Event.class, new HashSet<>(relTypes));
	    // map.put(EventAnnotation.class,
	    // SemRepConstants.SEMREP_RELATION_TYPES);
	    map.put(ModificationAnnotation.class, modTypes);
	    // map.put(EventModificationAnnotation.class,
	    // SemRepConstants.SEMREP_MODIFICATION_TYPES);
	    parseTypes.addAll(map.get(TermAnnotation.class));
	    parseTypes.addAll(map.get(EventAnnotation.class));
	    parseTypes.addAll(map.get(ModificationAnnotation.class));
	    parseTypes.add("Reference");
	    // Properties props =
	    // FileUtils.loadPropertiesFromFile("ling.properties");
	    // DomainProperties.init(props);

	    Set<RelationDefinition> relDefs = SemRepConstants.loadSemRepDefinitions();
	    relDefs.add(new RelationDefinition("OUTCOME", "OUTCOME", Event.class,
		    Arrays.asList(new RelationArgument("Object", entityMap, false)), null));
	    relDefs.add(new RelationDefinition("OutcomeSignal", "OutcomeSignal", Event.class,
		    Arrays.asList(new RelationArgument("Object", entityMap, false)), null));
	    Event.setDefinitions(relDefs);

	    Properties props = new Properties();
	    props.put("annotators", "tokenize,ssplit,pos,lemma,parse");
	    props.put("tokenize.options", "invertible=true");
	    props.put("ssplit.isOneSentence", "true");
	    props.put("sentenceSegmenter", "gov.nih.nlm.ling.process.MedlineSentenceSegmenter");
	    CoreNLPWrapper.getInstance(props);
	    segmenter = ComponentLoader.getSentenceSegmenter(props);
	    // Map<String,List<String>> relLines = readRelationLines(semrep);
	    for (String filename : files) {
		String id = filename.substring(filename.lastIndexOf(FS) + 1).replace(".ann", "");
		log.info("Processing " + id + ":" + ++fileNum);
		// if (id.equals("26491600") == false) continue;
		String txtFilename = annDir.getAbsolutePath() + FS + id + ".txt";
		String annFilename = annDir.getAbsolutePath() + FS + id + ".ann";
		String xmlFilename = xmlDir.getAbsolutePath() + FS + id + ".xml";
		if (new File(xmlFilename).exists()) {
		    System.out.println("Skip: " + xmlFilename);
		    continue;
		}
		// Element docEl = processSingleFile(id, txtFilename,
		// annFilename,relLines.get(id),props);
		try {
		    Element docEl = processSingleFile(id, txtFilename, annFilename);
		    nu.xom.Document xmlDoc = new nu.xom.Document(docEl);
		    Serializer serializer = new Serializer(new FileOutputStream(xmlFilename));
		    serializer.setIndent(4);
		    serializer.write(xmlDoc);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

}
