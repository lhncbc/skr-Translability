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
import java.util.Set;
import java.util.logging.Logger;

import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.EventAnnotation;
import gov.nih.nlm.ling.brat.StandoffAnnotationReader;
import gov.nih.nlm.ling.brat.StandoffAnnotationReader.AnnotationType;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.composition.EmbeddingCategorization;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLEventReader;
import gov.nih.nlm.ling.io.XMLModificationReader;
import gov.nih.nlm.ling.io.XMLPredicateReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Event;
import gov.nih.nlm.ling.sem.Modification;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.RelationArgument;
import gov.nih.nlm.ling.sem.RelationDefinition;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.util.FileUtils;
import nu.xom.Element;
import nu.xom.Serializer;

public class AddTermAnnotationsToXML {
	private static Logger log = Logger.getLogger(AddTermAnnotationsToXML.class.getName());

	private static Map<Class<? extends SemanticItem>, List<String>> annTypes;
	private static Map<Class<? extends SemanticItem>, List<String>> origAnnTypes;
	private static List<String> parseTypes;
	private static XMLReader xmlReader;
	private static Map<String, String> idMap = new HashMap<>();

	public static Element processSingleFile(Document doc, String annFilename) {
		int entMaxId = doc.getMaxId(Term.class);
		int evMaxId = doc.getMaxId(Event.class);
		int length = doc.getText().length();
		Map<AnnotationType, List<String>> lines = StandoffAnnotationReader
				.readAnnotationFiles(Arrays.asList(annFilename), null, parseTypes);
		Map<Class, List<Annotation>> annotations = StandoffAnnotationReader.parseAnnotations(doc.getId(), lines, null);
		// Map<Class,List<Annotation>> updated =
		// updateAnnotations(annotations,doc);
		SemanticItemFactory sif = doc.getSemanticItemFactory();
		Class[] processOrder = new Class[] { TermAnnotation.class, EventAnnotation.class };
		for (int i = 0; i < processOrder.length; i++) {
			Class c = processOrder[i];
			List<Annotation> anns = annotations.get(c);
			if (anns == null)
				continue;
			for (Annotation ann : anns) {
				if (ann.getSpan().getEnd() >= length)
					continue;
				String type = ann.getType();
				System.out.println(c.getName() + "|" + ann.toString());
				if (ann instanceof TermAnnotation) {
					String origid = ann.getId();
					ann.setId("T" + ++entMaxId);
					idMap.put(origid, ann.getId());
					if (SemRepConstants.SEMREP_ENTITY_TYPES.contains(type)) {
						Entity ent = sif.newEntity(doc, (TermAnnotation) ann);
					} else {
						Predicate pred = sif.newPredicate(doc, (TermAnnotation) ann);
					}
				} else if (ann instanceof EventAnnotation) {
					ann.setId("E" + ++evMaxId);
					Event ev = sif.newEvent(doc, (EventAnnotation) ann);
				}
			}
		}
		return doc.toXml();
	}

	/*
	 * private static Map<Class,List<Annotation>>
	 * updateAnnotations(Map<Class,List<Annotation>> annotations, Document doc)
	 * { Map<Class,List<Annotation>> updated = new HashMap<>(); int entMaxId =
	 * doc.getMaxId(Term.class); // int predMaxId =
	 * doc.getMaxId(Predicate.class); int evMaxId =
	 * doc.getMaxId(Predication.class); Class[] processOrder = new
	 * Class[]{TermAnnotation.class,EventAnnotation.class}; for (int i=0; i <
	 * processOrder.length; i++) { Class c = processOrder[i]; List<Annotation>
	 * anns = annotations.get(c); if (anns == null) continue; for (Annotation
	 * ann: anns) { System.out.println(c.getName() +"|" + ann.toString()); if
	 * (ann instanceof TermAnnotation) {
	 * 
	 * TermAnnotation ta = new TermAnnotation("T" +
	 * entMaxId,ann.getDocId(),ann.getType(),ann.getSpan(),((TermAnnotation)
	 * ann).getText(),((TermAnnotation) ann).getReferences()); List<Annotation>
	 * ex = updated.get(TermAnnotation.class); if (ex == null) ex = new
	 * ArrayList<>(); ex.add(ta); updated.put(TermAnnotation.class, ex); } else
	 * if (ann instanceof EventAnnotation) { EventAnnotation ea = new
	 * EventAnnotation("E" +
	 * ++evMaxId,ann.getDocId(),ann.getType(),((EventAnnotation)
	 * ann).getArguments(),((EventAnnotation) ann).getPredicate());
	 * List<Annotation> ex = updated.get(EventAnnotation.class); if (ex == null)
	 * ex = new ArrayList<>(); ex.add(ea); updated.put(EventAnnotation.class,
	 * ex); } } } return updated; }
	 */

	public static XMLReader getXMLReader() {
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Entity.class, new XMLEntityReader());
		reader.addAnnotationReader(Predicate.class, new XMLPredicateReader());
		reader.addAnnotationReader(Event.class, new XMLEventReader());
		reader.addAnnotationReader(Modification.class, new XMLModificationReader());
		return reader;
	}

	public static Map<Class<? extends SemanticItem>, List<String>> getAnnotationTypes() {
		Map<Class<? extends SemanticItem>, List<String>> annTypes = new HashMap<Class<? extends SemanticItem>, List<String>>();
		List<String> entityTypes = new ArrayList<>(SemRepConstants.SEMREP_ENTITY_TYPES);
		entityTypes.addAll(Arrays.asList("GNP_Gene", "GNP_Species", "OMIM_Gene"));
		annTypes.put(Entity.class, entityTypes);
		List<String> evTypes = new ArrayList<>(SemRepConstants.SEMREP_RELATION_TYPES);
		evTypes.addAll(SemRepConstants.SEMREP_NEG_RELATION_TYPES);
		annTypes.put(Event.class, evTypes);
		return annTypes;
	}

	public static Map<Class<? extends SemanticItem>, List<String>> getOutcomeAnnotationTypes() {
		Map<Class<? extends SemanticItem>, List<String>> annTypes = new HashMap<Class<? extends SemanticItem>, List<String>>();
		List<String> entityTypes = Arrays.asList("OverallOutcome", "OverallClinicalOutcome", "CLINICAL", "NON-CLINICAL",
				"OutcomeObject"/* ,"OutcomeSignal" */);
		annTypes.put(Entity.class, entityTypes);
		List<String> relTypes = Arrays.asList(/* "OUTCOME", */"OutcomeSignal");
		annTypes.put(Predicate.class, relTypes);
		annTypes.put(Event.class, relTypes);
		List<String> modTypes = Arrays.asList("OutcomeObjectPolarity", "ClinicalOutcome", "OverallOutcome",
				"OverallClinicalOutcome", "SentenceOutcome");
		annTypes.put(Modification.class, modTypes);
		return annTypes;

	}

	/*
	 * private static List<String> readSyntax(String filename) throws Exception
	 * { List<String> lines = FileUtils.linesFromFile(filename, "UTF-8");
	 * List<String> out = new ArrayList<>(); for (String line: lines) { if
	 * (line.startsWith("#") == false) continue;
	 * 
	 * } }
	 */

	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.err.print("Usage: inputXMLDirectory inputAnnotationDirectory outputXMLDirectory");
		}
		String xmlIn = args[0];
		String annIn = args[1];
		String out = args[2];
		File inDir = new File(xmlIn);
		File annDir = new File(annIn);
		if (inDir.isDirectory() == false) {
			System.err.println("inputXMLDirectory is not a directory.");
			System.exit(1);
		}
		if (annDir.isDirectory() == false) {
			System.err.println("inputXMLDirectory is not a directory.");
			System.exit(1);
		}
		File outDir = new File(out);
		if (outDir.isDirectory() == false) {
			System.err.println("The directory " + out + " doesn't exist. Creating a new directory..");
			outDir.mkdir();
		}

		origAnnTypes = getOutcomeAnnotationTypes();
		annTypes = getAnnotationTypes();
		xmlReader = getXMLReader();

		parseTypes = new ArrayList<>();
		parseTypes.addAll(annTypes.get(Entity.class));
		parseTypes.addAll(annTypes.get(Event.class));
		parseTypes.add("Reference");

		EmbeddingCategorization embeddingGraph = EmbeddingCategorization.getInstance();
		Map<Class<? extends SemanticItem>, Set<String>> entityMap = new HashMap<Class<? extends SemanticItem>, Set<String>>();
		entityMap.put(Entity.class, new HashSet<String>(origAnnTypes.get(Entity.class)));

		Set<RelationDefinition> relDefs = SemRepConstants.loadSemRepDefinitions();
		relDefs.add(new RelationDefinition("OutcomeSignal", "OutcomeSignal", Event.class,
				Arrays.asList(new RelationArgument("Object", entityMap, false)), null));
		Event.setDefinitions(relDefs);

		List<String> files = FileUtils.listFiles(xmlIn, false, "xml");
		int fileNum = 0;
		for (String filename : files) {
			String filenameNoExt = filename.replace(".xml", "");
			filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator) + 1);
			log.info("Processing " + filenameNoExt + ":" + ++fileNum);
			String annFilename = annDir.getAbsolutePath() + File.separator + filenameNoExt + ".ann";
			String outFilename = outDir.getAbsolutePath() + File.separator + filenameNoExt + ".xml";
			Document doc = xmlReader.load(filename, true, SemanticItemFactory.class, origAnnTypes, null);
			Element docEl = processSingleFile(doc, annFilename);
			nu.xom.Document xmlDoc = new nu.xom.Document(docEl);
			Serializer serializer = new Serializer(new FileOutputStream(outFilename));
			serializer.setIndent(4);
			serializer.write(xmlDoc);
		}
	}

}
