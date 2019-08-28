package gov.nih.nlm.skr.outcomes.ml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

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
 * Train and test script for request classification.
 *
 * @author kilicogluh
 */
public class OutcomeTrain {
	private static final Log log = new Log(OutcomeTrain.class);
	
	private static Properties props = null;
	private final static OutcomePOSClassifier posClassifier = new OutcomePOSClassifier();
	private final static OutcomeNEGClassifier negClassifier = new OutcomeNEGClassifier();

	private static LinkedHashSet<Indicator> polarityDictionary = null;
	private static Map<Class<? extends SemanticItem>,List<String>> annTypes;
	private static XMLReader xmlReader;

	private static String DATA_DIR;
	private static String POS_FEATURE_FILE;
	private static String NEG_FEATURE_FILE;
	private static List<String> POS_FEATURE_NAMES;
	private static List<String> NEG_FEATURE_NAMES;

	public static void init(String[] argv) throws IOException {
		Config.init("config.properties", argv);
		props = FileUtils.loadPropertiesFromFile("config.properties");
		polarityDictionary = Utils.loadSentimentDictionary(props.getProperty("polarityDictionaryFile","resources/polarity.dic"));
		annTypes = Utils.getAnnotationTypes();
		xmlReader = Utils.getXMLReader();
	}
	
	private static List<String> getFeatureNames(String filename) throws IOException {
		return FileUtils.linesFromFile(filename, "UTF-8");
	}
	
	public static void annotatePolarity(Document doc, Properties properties) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		properties.setProperty("termAnnotators","gov.nih.nlm.ling.process.IndicatorAnnotator");
		List<TermAnnotator> termAnnotators = ComponentLoader.getTermAnnotators(properties);
		for (TermAnnotator annotator : termAnnotators) {
			if (annotator instanceof IndicatorAnnotator) {
				((IndicatorAnnotator)annotator).setIndicators(polarityDictionary);
				String ignore = properties.getProperty("ignorePOSforIndicators");
				boolean ignorePOS = Boolean.parseBoolean(ignore == null ? "false" : ignore);
				((IndicatorAnnotator)annotator).setIgnorePOS(ignorePOS);
			}
			((IndicatorAnnotator)annotator).annotateIndicators(doc, properties);
		}
	}

	public static DocumentInstance processFile(String filename,Properties props) throws Exception {
		Document doc = xmlReader.load(filename, true, SemanticItemFactory.class,annTypes, null);
		annotatePolarity(doc, props);
		LinkedHashSet<SemanticItem> outcomes = Document.getSemanticItemsByClassType(doc, Modification.class, Arrays.asList("OverallOutcome"));
		Modification outcome = (Modification) outcomes.iterator().next();
		String val = outcome.getValue();
		if (val.equals("UNDETERMINED") || val.equals("INEFFECTIVE") || val.equals("NEUTRAL")) val = "OTHER";
		DocumentInstance instance = new DocumentInstance(doc.getId(),doc,OutcomePolarity.valueOf(val));
		instance.setContext(Utils.getContext(instance));
		instance.setMetaData("goldOutcome", val);
		return instance;
	}

	public static Iterable<DocumentInstance> processDir(String dir) throws Exception {
		List<DocumentInstance> instances = new ArrayList<>();
		List<String> files = FileUtils.listFiles(dir, false, "xml");
		int fileNum = 0;
		for (String filename: files) {
			String filenameNoExt = filename.replace(".xml", "");
			filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator)+1);
			log.info("Processing " + filenameNoExt + ":" + ++fileNum);
			DocumentInstance instance = processFile(filename,props);
			instances.add(instance);
		}	
		return instances;
	}
	
	public static Iterable<DocumentInstance> trainRequests() throws Exception {
		return processDir(DATA_DIR);
	}

	public static void trainPOSClassifier(OutcomePOSClassifier classifier, int numFolds, List<DocumentInstance> instances) {
		if (numFolds > 0) {
			log.fine("sorting...");
			Collections.sort(instances, (a, b) -> a.getId().compareToIgnoreCase(b.getId()));
			log.fine("shuffling...");
			Collections.shuffle(instances, new Random(2));
			log.info("Number of training examples ... {0}.", ((Collection<?>)instances).size());
			log.fine("cross validating...");
			final ClassificationStats cvStats = classifier.crossValidate(instances, POS_FEATURE_NAMES, numFolds);
			printStats(cvStats);
		} else {
			classifier.train(instances, POS_FEATURE_NAMES);
		}
	}
	
	public static void trainNEGClassifier(OutcomeNEGClassifier classifier, int numFolds, List<DocumentInstance> instances) {
		if (numFolds > 0) {
			log.fine("sorting...");
			Collections.sort(instances, (a, b) -> a.getId().compareToIgnoreCase(b.getId()));
			log.fine("shuffling...");
			Collections.shuffle(instances, new Random(2));
			log.info("Number of training examples ... {0}.", ((Collection<?>)instances).size());
			log.fine("cross validating...");
			final ClassificationStats cvStats = classifier.crossValidate(instances, NEG_FEATURE_NAMES,numFolds);
			printStats(cvStats);
		} else {
			classifier.train(instances, NEG_FEATURE_NAMES);
		}
	}
	
	public static void printStats(ClassificationStats stats) {
		log.info("Classes: {0}", stats.classes());
		for (String c: stats.classes()) {
			log.info("{0}|Prec:{1}\tRec:{2}\tF1:{3}", c, stats.precision(c),stats.recall(c),stats.f1(c));
		}
		log.info("Accuracy: {0}", stats.accuracy());
		log.info("MicroF1: {0}", stats.microF1());
		log.info("MacroF1: {0}", stats.macroF1());
		log.info("{0}", stats.getConfusionMatrix());
	}
	
	// create instances for Positive Outcome and Negative Outcome classifiers
	public static List<DocumentInstance> relabel(List<DocumentInstance> instances, String label) {
		List<DocumentInstance> outInstances = new ArrayList<>();
		String negLabel = "N" + label;

		for (DocumentInstance ins: instances) {
			String val = ins.getPolarity().toString();
			DocumentInstance nins = null;
			String nval = label;
			if (val.equals(label) || val.equals("MIXED")) {
				nins = new DocumentInstance(ins.getId(),ins.getDocument(),ins.getPolarity());
			} else {
				nins = new DocumentInstance(ins.getId(),ins.getDocument(),OutcomePolarity.valueOf(negLabel));
				nval = negLabel;
			}
			nins.setMetaData("gold" + label + "Outcome", nval);
			nins.setContext(ins.getContext());
			outInstances.add(nins);
		}
		return outInstances;
	}
	
	/**
	 * Command line (intended entry).
	 */
	public static void main(String[] argv) throws Exception {
		init(argv);		
		int numFolds = 0;
		
		for (int i = 0; i < argv.length; i++) {
			if (argv[i].startsWith("--cv=")) {
				numFolds = Integer.valueOf(argv[i].replace("--cv=", ""));
			}
			else {
				log.warning("Unknown parameter: {0}", argv[i]);
			}
		}

		DATA_DIR = props.getProperty("outcomeTrainDirectory");
		POS_FEATURE_FILE = props.getProperty("outcomePOSFeatureFile");
		NEG_FEATURE_FILE = props.getProperty("outcomeNEGFeatureFile");
		
		POS_FEATURE_NAMES = getFeatureNames(POS_FEATURE_FILE);
		NEG_FEATURE_NAMES = getFeatureNames(NEG_FEATURE_FILE);
		
		List<DocumentInstance> trainRequests = (List<DocumentInstance>) trainRequests();
		List<DocumentInstance> trainPOSInstances = relabel(trainRequests,"POS");
		List<DocumentInstance> trainNEGInstances = relabel(trainRequests,"NEG");
		
		log.info("Training for POS outcomes...");
		trainPOSClassifier(posClassifier,numFolds,trainPOSInstances);
		log.info("Training for NEG outcomes...");
		trainNEGClassifier(negClassifier,numFolds,trainNEGInstances);
}
	

	/**
	 * Struct for keeping track of classifications.
	 */
/*	private static class CR {
		private final DocumentInstance request;
		private final MulticlassResult result;
		private final String guess;
		private final String gold;
		private CR(final DocumentInstance request, final MulticlassResult result,
				final String guess, final String gold) {
			this.request = request; this.result = result;
			this.guess = guess; this.gold = gold;
		}
	}*/

/**
 * Determines the false negative and false positive distributions for the
 * class with the given <var>className</var>.
 */
/*private static void reportFPandFN(final List<CR> classifications,
		final String className) {
	final Map<String,Integer> fpCounts = new TreeMap<>();
	final Map<String,Integer> fnCounts = new TreeMap<>();

	for (final CR classification : classifications) {
		log.info(classification.request + "\t" + classification.result + "\t" + classification.guess + "\t" + classification.gold);
		if (classification.guess.equals(className) &&
				classification.gold.equals(className)) {
			// correct
		}
		else if (classification.guess.equals(className)) {
			Maps.increment(fpCounts, classification.gold);
		}
		else if (classification.gold.equals(className)) {
			Maps.increment(fnCounts, classification.guess);
		}
	}

	final List<Map.Entry<String,Double>> fpEntries = new ArrayList<>(
			Maps.normalizeInt(fpCounts).entrySet());
	final List<Map.Entry<String,Double>> fnEntries = new ArrayList<>(
			Maps.normalizeInt(fnCounts).entrySet());

	Collections.sort(fpEntries, Comparators.doubleWeightedMapEntry().reverse());
	Collections.sort(fnEntries, Comparators.doubleWeightedMapEntry().reverse());

	final StringBuilder builder1 = new StringBuilder("Most common ");
	builder1.append(className);
	builder1.append(" FP:");
	for (int i = 0; i < 5 && i < fpEntries.size(); i++) {
		builder1.append("  ");
		builder1.append(fpEntries.get(i).getKey());
		builder1.append("=");
		builder1.append(Strings.threeDecimal(fpEntries.get(i).getValue()));
	}
	log.info(builder1.toString());

	final StringBuilder builder2 = new StringBuilder("Most common ");
	builder2.append(className);
	builder2.append(" FN:");
	for (int i = 0; i < 10 && i < fnEntries.size(); i++) {
		builder2.append("  ");
		builder2.append(fnEntries.get(i).getKey());
		builder2.append("=");
		builder2.append(Strings.threeDecimal(fnEntries.get(i).getValue()));
	}
	log.info(builder2.toString());
}*/

/**
 * Writes up to <var>max</var> of the <var>errors</var> to the given
 * <var>file</var>.
 */
/*private static void writeErrors(final Place file,
		final String className,
		final List<Pair<DocumentInstance,Double>> errors,
		final Map<DocumentInstance,MulticlassResult> resultMap,
		final Map<DocumentInstance,String> goldMap,
		final Map<DocumentInstance,String> guessMap,
		final int max) throws IOException {
	//   final int num = Math.min(errors.size(), max);
	final int num = errors.size();

	final Element root = new Element("Errors");
	root.setAttribute("class", className);
	root.setAttribute("totalErrors", Integer.toString(errors.size()));
	root.setAttribute("reportedErrors", Integer.toString(num));

	for (int i = 0; i < num; i++) {
		final DocumentInstance request = errors.get(i).getFirst();
		final double score = errors.get(i).getSecond();
		final MulticlassResult result = resultMap.get(request);

		final Element error = new Element("Error");
		error.setAttribute("docID", request.getId());
		error.setAttribute("score", Double.toString(score));
		error.setAttribute("gold", goldMap.get(request));
		error.setAttribute("guess", guessMap.get(request));

		final Element scores = new Element("Result");
		final Iterator<Pair<String,Double>> scoresIter = result.pairIterator();
		while (scoresIter.hasNext()) {
			final Pair<String,Double> pair = scoresIter.next();
			final String key = pair.getFirst().replace("/", "-slash-")
					.replace("&amp;", "-amp-");
			final String value = Strings.fiveDecimal(pair.getSecond());
			scores.setAttribute(key, value);
		}
		error.addContent(scores);

		final Element text = new Element("Request");
		//     text.setAttribute("from", request.getDocument().getMetaData("from"));
		//     text.setAttribute("origin", request.getDocument().getMetaData("origin"));
		//     text.setText(request.asRawString());
		error.addContent(text);

		root.addContent(error);
	}

	XMLUtil.writeFile(root, file);
}*/

}
