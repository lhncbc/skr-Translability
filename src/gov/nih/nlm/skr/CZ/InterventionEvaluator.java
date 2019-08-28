package gov.nih.nlm.skr.CZ;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.util.FileUtils;
// for instance-level
//import gov.nih.nlm.ling.brat.StandoffAnnotationFileComparator;

/**
 * The class to compare/evaluate a directory of standoff annotation files
 * against another directory of such files.
 * <p>
 * In the case of evaluation, the second directory is expected to contain the
 * gold files.
 * 
 * 
 * @author kilicogluh
 *
 */
public class InterventionEvaluator {
    private static Logger log = Logger.getLogger(InterventionEvaluator.class.getName());

    public static final String FS = System.getProperty("file.separator");

    public static void main(String[] args) throws IOException {
	System.setProperty("java.util.logging.config.file", "logging.properties");
	if (args.length < 2) {
	    System.err.println("Usage: 'java StandoffAnnotationEvaluator annDirectory goldDirectory "
		    + "[approximateMatch] [useReference] [usedTermMatchOnly] [evaluateSPAN] [printErrors] [printCorrect]'");
	    return;
	}
	String annDirName = args[0];
	String goldDirName = args[1];
	if (annDirName.equals(goldDirName)) {
	    log.log(Level.SEVERE, "The annotation directory and the gold standard directory are the same: {0}.",
		    new Object[] { annDirName });
	    System.exit(1);
	}
	File annDir = new File(annDirName);
	File goldDir = new File(goldDirName);
	if (annDir.isDirectory() == false) {
	    log.log(Level.SEVERE, "annDirectory is not a directory: {0}.", new Object[] { annDirName });
	    System.exit(1);
	}
	if (goldDir.isDirectory() == false) {
	    log.log(Level.SEVERE, "goldDirectory is not a directory: {0}.", new Object[] { goldDirName });
	    System.exit(1);
	}

	boolean approximateMatch = false;
	boolean useReference = false;
	boolean usedTermMatchOnly = false;
	boolean evaluateSPAN = false;
	boolean printErrors = false;
	boolean printCorrect = false;

	if (args.length > 2)
	    approximateMatch = Boolean.parseBoolean(args[2]);
	if (args.length > 3)
	    useReference = Boolean.parseBoolean(args[3]);
	if (args.length > 4)
	    usedTermMatchOnly = Boolean.parseBoolean(args[4]);
	if (args.length > 5)
	    evaluateSPAN = Boolean.parseBoolean(args[5]);
	if (args.length > 6)
	    printErrors = Boolean.parseBoolean(args[6]);
	if (args.length > 7)
	    printCorrect = Boolean.parseBoolean(args[7]);

	Map<String, List<Annotation>> annoTP = new TreeMap<>();
	Map<String, List<Annotation>> annoFP = new TreeMap<>();
	Map<String, List<Annotation>> annoFN = new TreeMap<>();
	Map<Class, List<String>> map = new HashMap<>();

	map.put(TermAnnotation.class, Arrays.asList("Intervention", "Reference"));
	//		map.put(ModificationAnnotation.class, Arrays.asList("Species","Model","SpeciesModel"));
	List<String> parseTypes = new ArrayList<>();
	parseTypes.addAll(map.get(TermAnnotation.class));
	//		parseTypes.addAll(map.get(ModificationAnnotation.class));

	int fileNum = 0;
	List<String> files = FileUtils.listFiles(goldDirName, false, "ann");
	for (String filename : files) {
	    String id = filename.substring(filename.lastIndexOf(FS) + 1).replace(".ann", "");
	    //			if (id.equals("10201413") == false) continue;
	    log.log(Level.INFO, "Processing {0}: {1}", new Object[] { id, ++fileNum });
	    String annFilename = annDir.getAbsolutePath() + FS + id + ".ann";
	    String goldFilename = goldDir.getAbsolutePath() + FS + id + ".ann";
	    //instance level
	    //			StandoffAnnotationFileComparator.compare(id, annFilename, goldFilename, null, parseTypes, null,
	    //					approximateMatch, useReference, usedTermMatchOnly, evaluateSPAN, annoTP, annoFP, annoFN);
	    // document level
	    String txtFilename = goldDir.getAbsolutePath() + FS + id + ".txt";
	    StandoffAnnotationFileComparator.compare(id, txtFilename, annFilename, goldFilename, null, parseTypes, null,
		    approximateMatch, useReference, usedTermMatchOnly, evaluateSPAN, annoTP, annoFP, annoFN);
	}
	PrintWriter pw = new PrintWriter(System.out);
	SpeciesModelEvaluator.printResults(pw, map, annoTP, annoFP, annoFN);
	if (printErrors)
	    SpeciesModelEvaluator.printDiffs(pw, map, annoFP, annoFN);
	if (printCorrect)
	    SpeciesModelEvaluator.printCorrect(pw, map, annoTP);
	pw.flush();
	pw.close();
    }

}
