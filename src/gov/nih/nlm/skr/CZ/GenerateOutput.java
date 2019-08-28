package gov.nih.nlm.skr.CZ;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class GenerateOutput {
    static Brat brat = null;
    static SpeciesModel sm = null;
    static Gene gene = null;
    static Intervention inter = null;
    private static GenerateOutput go = null;
    static int SAVE_TO_FILE = 0;
    static int SAVE_TO_DB = 1;

    public static GenerateOutput getInstance() throws IOException {
	if (go == null) {
	    go = new GenerateOutput();
	    brat = Brat.getInstance();
	}
	return go;
    }

    public void generateSpecies(String bratDir, String outDir, int fileOrDB) {
	String aLine = null;
	try {
	    if (sm == null)
		sm = SpeciesModel.getInstance();

	    String separator = System.getProperty("file.separator");
	    // PrintWriter out = new PrintWriter(new File(errorFile));
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
		System.out.println("Processing " + bratFile);
		String PMID = children[j].substring(0, children[j].indexOf("."));
		String txtFile = new String(bratDir + separator + PMID + ".txt");
		BratDocument doc = brat.readABratFile(bratFile, txtFile);
		SpeciesInfoInDocument speciesDoc = sm.generateSM(doc);
		System.out.println("output to the directory : " + outDir);
		if (fileOrDB == SAVE_TO_FILE)
		    brat.writeSpeciesInfoToABratFile(doc, speciesDoc, outDir);
		else
		    brat.writeSpeciesInfoToDB(doc, speciesDoc, outDir);

	    }
	    // out.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void generateGene(String bratDir, String outDir, int fileOrDB) {
	String aLine = null;
	try {
	    if (gene == null)
		gene = Gene.getInstance();
	    String separator = System.getProperty("file.separator");
	    // PrintWriter out = new PrintWriter(new File(errorFile));
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
		System.out.println("Processing " + bratFile);
		String PMID = children[j].substring(0, children[j].indexOf("."));
		String txtFile = new String(bratDir + separator + PMID + ".txt");
		BratDocument doc = brat.readABratFile(bratFile, txtFile);
		List<BratConcept> geneList = gene.generateGene(doc);
		System.out.println("output to the directory : " + outDir);
		if (fileOrDB == SAVE_TO_FILE)
		    brat.writeGeneInfoToABratFile(doc, geneList, outDir);
		else
		    brat.writeGeneInfoToDB(doc, geneList, outDir);

	    }
	    // out.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void generateIntervention(String bratDir, String outDir, int fileOrDB, String errorFile) {
	try {
	    if (inter == null)
		inter = Intervention.getInstance();

	    String separator = System.getProperty("file.separator");
	    PrintWriter out = new PrintWriter(new File(errorFile));
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
		BratDocument doc = brat.readABratFile(bratFile, txtFile);
		TextAndTitleLength ttl = inter.readFile(txtFile);
		// BratDocument doc =
		// brat.readABratFile("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\25449120.ann",
		// "Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2\\25449120.txt");
		InterventionResult finalList = inter.generateIntervention(doc, ttl.text, ttl.titleLength);
		if (fileOrDB == SAVE_TO_FILE) {
		    inter.writeTextToBratFile(doc.PMID, bratDir, outDir, ttl.text);
		    brat.writeInterventionInfoToABratFile(doc, finalList, outDir);
		} else {
		    brat.writeInterventionInfoToDB(doc, finalList);
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void generateAll(String bratDir, String outDir, int fileOrDB) {
	String aLine = null;
	try {
	    if (sm == null)
		sm = SpeciesModel.getInstance();
	    if (gene == null)
		gene = Gene.getInstance();
	    if (inter == null)
		inter = Intervention.getInstance();
	    String separator = System.getProperty("file.separator");
	    // PrintWriter out = new PrintWriter(new File(errorFile));
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
		System.out.println("Processing " + bratFile);
		String PMID = children[j].substring(0, children[j].indexOf("."));
		String txtFile = new String(bratDir + separator + PMID + ".txt");
		BratDocument doc = brat.readABratFile(bratFile, txtFile);
		/*
		 * Generate species
		 */
		SpeciesInfoInDocument speciesDoc = sm.generateSM(doc);
		System.out.println("output to the directory : " + outDir);
		if (fileOrDB == SAVE_TO_FILE)
		    brat.writeSpeciesInfoToABratFile(doc, speciesDoc, outDir);
		else {
		    brat.writeSentenceToDB(doc);
		    brat.writeSpeciesInfoToDB(doc, speciesDoc, outDir);
		}
		/*
		 * Generate gene
		 */
		List<BratConcept> geneList = gene.generateGene(doc);
		if (fileOrDB == SAVE_TO_FILE)
		    brat.writeGeneInfoToABratFile(doc, geneList, outDir);
		else
		    brat.writeGeneInfoToDB(doc, geneList, outDir);
		/*
		 * Generate intervention
		 */
		TextAndTitleLength ttl = inter.readFile(txtFile);

		InterventionResult finalList = inter.generateIntervention(doc, ttl.text, ttl.titleLength);
		if (fileOrDB == SAVE_TO_FILE) {
		    inter.writeTextToBratFile(doc.PMID, bratDir, outDir, ttl.text);
		    brat.writeInterventionInfoToABratFile(doc, finalList, outDir);
		} else {
		    brat.writeInterventionInfoToDB(doc, finalList);
		}

	    }
	    // out.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static void main(String[] args) {

	try {
	    System.out.println("Program start...");
	    // brat =
	    // Brat.getInstance("Q:\\LHC_Projects\\Caroline\\testset\\semtypeFullName.txt");
	    // sm =
	    // SpeciesModel.getInstance("Q:\\LHC_Projects\\Caroline\\data\\Field1ListSpecies_06222017.txt",
	    // "Q:\\LHC_Projects\\Caroline\\data\\Field4ListModel_04052017.txt");
	    // sr.process("args[0],
	    // "Q:\\LHC_Projects\\Caroline\\testset\\normed100.semrep.txt",
	    // "Q:\\LHC_Projects\\Caroline\\testset\\brat2");
	    // System.out.println("Calling SemRepToBrat.process() with " +
	    // args[0]);
	    // System.out.println(args[1] + " " + args[2]);
	    // brat.semrepGNormPlusToBrat(args[0], args[1], args[2],
	    // "setup.txt");
	    // brat.semrepToBrat(args[0], args[1], args[2]);
	    // String aLine = null;
	    // BratDocument doc = brat.readABratFile(args[0], args[1],
	    // semtypeHt);
	    /*
	     * BratDocument doc = brat.readABratFile(
	     * "Q:\\LHC_Projects\\Caroline\\testset\\brat4\\28792609.ann",
	     * "Q:\\LHC_Projects\\Caroline\\testset\\brat4\\28792609.txt"); for(BratSentence
	     * bs: doc.sentenceList) { System.out.println(bs.text + " |" + bs.sentNum + "|"
	     * + bs.startPos + "|" + bs.endPos + "\n" ); }
	     * 
	     * SpeciesInfoInDocument speciesDoc = sm.generateSM(doc);
	     * brat.writeABratFile(doc, speciesDoc,
	     * "Q:\\LHC_Projects\\Caroline\\testset\\brat-sp1");
	     */
	    GenerateOutput go = GenerateOutput.getInstance();
	    // First argument is the directory for input semrep brat files
	    // Second  argument is the output directory
	    // Third argument is the error file
	    //  go.generateIntervention(args[0], args[1], args[2]);
	    // go.generateSpecies("Q:\\LHC_Projects\\Caroline\\testset\\brat_06182018",
	    //	    "Q:\\LHC_Projects\\Caroline\\testset\\sm_06182018", SAVE_TO_FILE, null, null, null);
	    // go.generateSpecies("C:\\TranslationalResearch\\3yearsbrat_08062018",
	    //    "C:\\TranslationalResearch\\3yearsGene_08082018", SAVE_TO_FILE, null, null, null);
	    // go.generateSpecies("X:\\TranslationResearch\\czbrat2",
	    // "X:\\Zeiss\\DATA\\speciesModel_OUT_012518");
	    /*-  go.generateSpecies("Z:\\Zeiss\\DATA\\pubmed1950brat", "", SAVE_TO_DB,
	        "jdbc:mysql://indsrv2.nlm.nih.gov/TranslationResearch70", "root", "indsrv2@root"); */
	    // go.generateGene("Z:\\Zeiss\\DATA\\pubmed1950brat", "", SAVE_TO_DB);
	    go.generateAll("Z:\\Zeiss\\DATA\\pubmed1950brat", "", SAVE_TO_DB);
	    // go.generateSpecies("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat3",
	    // "Q:\\LHC_Projects\\Caroline\\NEW_2017\\brat_sp4");
	    // go.generateSpecies("Z:\\TranslationResearch\\3yearsbrat2",
	    // "C:\\TranslationalResearch\\3yearsSpeciesModelbrat", SAVE_TO_DB,
	    // "jdbc:mysql://indsrv2.nlm.nih.gov/TranslationResearch", "root",
	    // "indsrv2@root");
	    /*- go.generateIntervention("Z:\\Zeiss\\Data\\SR_GNP_OMIM_Test_080318",
	        "Q:\\LHC_Projects\\Caroline\\NEW_2017\\intervention_halil_output",
	        "Q:\\LHC_Projects\\Caroline\\NEW_2017\\intervention_error_10012018"); */
	    /*- go.generateIntervention("C:\\TranslationalResearch\\3yearsbrat_09132018", "", SAVE_TO_DB,
	        "C:\\TranslationalResearch\\errorOutput\\interventionError_02262019",
	        "jdbc:mysql://indsrv2.nlm.nih.gov/TranslationResearch70", "root", "indsrv2@root"); */
	    /*- go.generateIntervention("Z:\\Zeiss\\DATA\\pubmed1950brat", "", SAVE_TO_DB,
	        "C:\\TranslationalResearch\\errorOutput\\interventionError_08022019"); */
	    // go.generateIntervention("Q:\\LHC_Projects\\Caroline\\NEW_2017\\intervention_debug",
	    //    "Q:\\LHC_Projects\\Caroline\\NEW_2017\\intervention_debug_output",
	    //	    "Q:\\LHC_Projects\\Caroline\\NEW_2017\\intervention_error_02272018");
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
