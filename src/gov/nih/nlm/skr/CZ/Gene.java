package gov.nih.nlm.skr.CZ;

/*
 * Gene class that extracts Genes from Brat files
 * Author: Dongwook Shin
 */
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gene {
    List<BratConcept> genes;
    static Brat brat = null;
    static int SAVE_TO_FILE = 0;
    static int SAVE_TO_DB = 1;
    Statement stmt = null;

    private static Gene gene = null;

    // this can potentially be replaced by the suppressed gene list that Graciela created, but it looks like we would still need to have a separate list "lcig" etc. is not in that list.
    // this is mainly based on the training data
    private static List<String> SUPPRESS_MAPPINGS_BAD_GENE = Arrays.asList("all", "gov", "impact", "impacts",
	    "difference -3.", "bid", "at 1", "at 1 and 2", "at 1 and 3", "yrs", "k(i) = 4.", "lcig", "lcigs", "lid",
	    "lids", "his", "and1", "c)) in", "fate", "max)", "mri", "q1 and q4", "revival", "six 1");

    // perhaps would be better with CUIs instead
    // or since these are interventions basically, running the modules jointly can help alleviate this problem
    private static List<String> SUPPRESS_MAPPINGS_LEVODOPA = Arrays.asList("l-dopa", "levodopa", "l-dopas", "l-dopa's",
	    "dopa", "dihydroxyphenylalanine", "18)f-dopa", "alpha-methyl dopa");
    //same as above
    private static List<String> SUPPRESS_INTERVENTIONS = Arrays.asList("2 mg rasagiline -0.19", "liraglutide",
	    "lixisenatide");

    // perhaps would be better with CUIs. Or If we are fairly accurate with NCBI Gene IDs, we can ignore everything that comes only with CUI, and remove them.
    // Currently, this won't work well, because even though we used 'enzy' semantic type, these are not mapped to Gene IDs.
    private static List<String> SUPPRESS_MAPPINGS_GENERIC = Arrays.asList("protein", "proteins", "gene", "genes",
	    "receptor", "receptors", "cytokine", "cytokines", "peptide", "peptides", "kinase", "kinases", "genome",
	    "genomes", "signaling protein", "signaling proteins", "wild-type", "wild type", "wildtype", "wt",
	    "transcription factor", "transcription factors", "growth factor", "growth factors", "growth-factor",
	    "growth-factors", "locus", "loci", "neurotrophic factor", "neurotrophic factors", "neurotrophic-factor",
	    "neurotrophic-factors", "antibody", "antibodies", "neuropeptide", "neuropeptides", "enzyme", "enzymes",
	    "amino acid", "amino acids", "chemokine", "chemokines", "target gene", "target genes", "enzymatic",
	    "homolog", "homologue", "homologous", "isoform", "autoreceptor", "allele", "alleles", "chaperone", "cell",
	    "cells", "abnormal protein", "abnormal proteins", "candidate gene", "candidate genes", "candidate pd genes",
	    "candidate pd gene", "candidate therapeutic gene", "candidate therapeutic genes", "developmental gene",
	    "developmental gene", "genome", "genome-", "genomic", "cytoskeletal protein", "cytoskeletal proteins",
	    "deacetylase", "deacteylases", "g protein", "g proteins", "homeoprotein", "homeoproteins",
	    "immediate-early gene", "immediate-early genes", "kinase inhibitor", "ampa receptors", "ampa receptor",
	    "lysosomal enzyme", "mature neurons specific gene", "mitochondrial protein", "mitochondrial proteins",
	    "monoclonal antibody", "monoclonal antibodies", "mutant protein", "mutant proteins",
	    "neurotrophic/neuroprotective factors", "neurotrophic/neuroprotective factor", "neurotrophic factor",
	    "neurotrophic factors", "neuroprotective factors", "neuroprotective factor",
	    "neutralizing antiliatermin antibody", "neutralizing antiliatermin antibodies", "peroxiredoxine",
	    "peroxiredoxines", "protease", "proteases", "proteasomal", "protein family", "protein families",
	    "receptor gene", "receptor genes", "secreted growth factor", "secreted protein", "secreted proteins",
	    "serine protease", "serine", "synaptic striatal dopamine receptor", "synaptic striatal dopamine receptors",
	    "transgene", "transgenes", "transporter", "transporters", "-tyrosine", "ub-bound protein",
	    "ub-bound proteins", "ubiquitinated protein", "ubiquitinated proteins", "enzyme complex",
	    "enzyme complexes", "extracellular protein", "extracellular proteins", "specific antibody", "dominant gene",
	    "dominant genes", "membrane protein", "membrane proteins");

    // perhaps would be better with CUIs
    // Same as above, if Gene IDs work well, we may not have to use these.
    private static List<String> SUPPRESS_MAPPINGS_UNANNOTATED = Arrays.asList("bis", "creatine", "cr", "glutathione",
	    "glutamate", "exenatide", "glu", "stn", "gpe", "gpi", "fractalkine", "sp", "d3r", "aav", "exendin-4",
	    "pole", "rg1", "delta", "gaba", "gad67", "rhpdgf-bb", "14-3-3theta", "5-htp", "iia", "med", "pdp", "sta",
	    "activin a", "bvpla2", "histone", "mif", "msx-3", "s14", "st1535", "tyrosine", /* "ubiquitin", */
	    "4-hne", "5-ht1a", "acetyl-l-cysteine", "acetylcysteine", "acting full dopamine d", "adl", "aes",
	    "alpha-methyltyrosine", "angiotensin", "aspartate", "ast", "atp", "ca1", "calpeptin", "carnitine",
	    "carnosine", "cin", "cms", "cno", "cre", "csa", "csf", "dbs", "ddqtc", "dep1", "difopein", "dipeptide",
	    "g15", "gamma-aminobutyric acid", "gelatin", "glycinamide", "hf-lf", "histone deacetylase inhibitor", "huc",
	    "iba-1", "inhibitor 1", "l-3", "l-amd", "lb", "lce", "lox", "lps", "lr3", "lsp1", "l-tryptophan", "mater",
	    "mg132", "mitochondrial complex ii", "msc", "mug/2", "mul", "nachr",
	    "nerve growth and differentiation factor", "net", "neurotrophin", "nps", "ntn", "oxy", "oxyntomodulin",
	    "pan", "pars", "pbs", "phe", "phosphoprotein", "plg", "polypeptide", "ppn", "ppx",
	    "protein comprising co-chaperone function", "r1141", "rapa", "rbd", "rd-1", "reduced glutathione",
	    "respiratory complex i", "ros", "s184", "s58", "san", "saps", "saps-pd", "scs", "scs)-", "ser2", "sharp",
	    "sk-n", "small gtp-binding protein", "snpc at 1", "spatial", "spice", "str", "substance p",
	    "tetrahydrobiopterin", "th1", "thr", "thr69", "threonine", "tryptophan", "tsg", "ttr", "vg", "vlo", "vplo",
	    "3'-utr");

    // perhaps would be better with CUIs
    // These could be handled with model names. If some entity refers to a model name, exclude it. 
    // The list here is based on what I see in the training data
    private static List<String> SUPPRESS_MPTP = Arrays.asList("mptp", "mptp(+)", "mptp+", "mptp-", "mpp", "mpp(+)",
	    "mpp+", "mpp-", "mptpp");

    // Common errorrs (confidence intervals, 6th, 2nd, etc.)
    private Pattern CHAR_PATTERN = Pattern.compile("[a-zA-z]+");
    private Pattern CI_PATTERN = Pattern.compile("^ci ([0-9\\.\\-]+)$");
    private Pattern ORDINAL_PATTERN = Pattern.compile("(?<=[0-9])(?:st|nd|rd|th)");

    public static Gene getInstance() throws IOException {
	if (gene == null) {
	    System.out.println("Initializing a Brat instance...");
	    gene = new Gene();
	    brat = Brat.getInstance();
	}
	return gene;
    }

    public boolean isConfidenceInterval(String s) {
	return (CI_PATTERN.matcher(s).find());
    }

    public boolean isOrdinalNumber(String s) {
	return (ORDINAL_PATTERN.matcher(s).find());
    }

    public List<BratConcept> generateGene(BratDocument doc) {
	Map<String, String> annotated = new HashMap<>();
	// String syntacticUnit = doc.syntacticUnit;
	List<BratConcept> bratConceptList = doc.conceptList;
	List<BratConcept> conceptList = new ArrayList<>();
	List<BratConcept> finalList = new ArrayList<>();

	List<BratConcept> gnpList = new ArrayList<>();
	List<BratConcept> semrepList = new ArrayList<>();
	List<BratConcept> omimList = new ArrayList<>();

	for (BratConcept bconcept : bratConceptList) {
	    Matcher m = CHAR_PATTERN.matcher(bconcept.name);
	    if (!m.find())
		continue;
	    if (isConfidenceInterval(bconcept.name))
		continue;
	    if (isOrdinalNumber(bconcept.name))
		continue;
	    if (bconcept.semtype.contains("GNP_Gene"))
		gnpList.add(bconcept);
	    if (bconcept.semtype.contains("OMIM_Gene"))
		omimList.add(bconcept);
	    else if (bconcept.semtype.equals("gngm") || bconcept.semtype.equals("aapp")
		    || bconcept.semtype.equals("enzy"))
		semrepList.add(bconcept);
	}

	//give priority to GNPlus 
	for (BratConcept g : gnpList) {
	    g.geneType = new String("EntrezGene");
	    conceptList = StringUtils.subsumeCheckAndAdd(conceptList, g);
	    annotated.put(g.name.toLowerCase(), g.semtype);
	}
	for (BratConcept o : omimList) {
	    o.geneType = new String("EntrezGene");
	    conceptList = StringUtils.subsumeCheckAndAdd(conceptList, o);
	    annotated.put(o.name.toLowerCase(), o.semtype);
	}
	// SemRep is given least priority, because it comes with lots of noise
	for (BratConcept s : semrepList) {
	    if (s.geneId != null) {
		String[] ids = s.geneId.split(",");
		s.geneId = ids[0];
		s.geneType = new String("EntrezGene");
	    }
	    // I think we can just ignore entities with no gene IDs, but this needs more work, because we are accepting enzy, for which SemRep does not map to Gene IDs.
	    else if (s.CUI != null) {
		s.geneType = new String("Metathesaurus");
	    }
	    conceptList = StringUtils.subsumeCheckAndAdd(conceptList, s);
	    annotated.put(s.name.toLowerCase(), s.semtype);
	}
	conceptList.sort(new ConceptComparator());

	for (BratConcept concept : conceptList) {
	    String lw = concept.name.toLowerCase();
	    if (SUPPRESS_MAPPINGS_BAD_GENE.contains(lw) == false && SUPPRESS_MAPPINGS_LEVODOPA.contains(lw) == false
		    && SUPPRESS_MAPPINGS_GENERIC.contains(lw) == false
		    && SUPPRESS_MAPPINGS_UNANNOTATED.contains(lw) == false && SUPPRESS_MPTP.contains(lw) == false
		    && SUPPRESS_INTERVENTIONS.contains(lw) == false && isConfidenceInterval(lw) == false)
		finalList.add(concept);
	}

	return finalList;

    }

    /*
     * private List<BratConcept> getUnannotatedInstances(BratDocument doc,
     * Map<String,String> anns, List<BratConcept> conceptList) { String lowercase =
     * doc.text.toLowerCase(); List<BratConcept> additional = new ArrayList<>(); for
     * (String ann: anns.keySet()) { Pattern annPattern = Pattern.compile("\\b" +
     * Pattern.quote(ann) + "\\b"); Matcher m = annPattern.matcher(lowercase); while
     * (m.find()) { int start = m.start(); int end = m.end(); if
     * (!hasBeenAddedAlready(conceptList,start,end)) { BratConcept bc = new
     * BratConcept(ann, anns.get(ann), start, end); additional.add(bc); } } } return
     * additional; }
     */

    /*
     * private boolean hasBeenAddedAlready(List<BratConcept> list, int start, int
     * end) { for (BratConcept bc: list) { int s = bc.startOffset; int e=
     * bc.endOffset; if (s== start && e == end) return true; } return false; }
     */

    public void generateGene(String bratDir, String outDir) {
	if (new File(outDir).exists() == false)
	    new File(outDir).mkdir();
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
		//  			if (PMID.equals("25146322") == false) 	    				continue;
		String txtFile = new String(bratDir + separator + PMID + ".txt");
		BratDocument doc = brat.readABratFile(bratFile, txtFile);
		List<BratConcept> geneList = gene.generateGene(doc);
		System.out.println("output to the directory : " + outDir);
		brat.writeGeneInfoToABratFile(doc, geneList, outDir);

	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void generateGene(String bratDir, String outDir, int fileOrDB) {
	String aLine = null;
	try {
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
		List<BratConcept> geneList = generateGene(doc);
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

    /*- public static void main(String[] argv) {
    try {
        Gene gene = Gene.getInstance();
        //			 gene.generateGene("Q:\\LHC_Projects\\Caroline\\NEW_2017\\czbrat2", "Q:\\LHC_Projects\\Caroline\\NEW_2017\\brat_gene2");
        //			 gene.generateGene("X:\\TranslationResearch\\czbrat2", "X:\\Zeiss\\DATA\\geneProtein_OUT_train_031918");
        //			 gene.generateGene("X:\\Zeiss\\DATA\\SR_GNP_OMIM_Train", "X:\\Zeiss\\DATA\\geneProtein_OUT_test_031918");		
        // gene.generateGene("X:\\Zeiss\\DATA\\SR_GNP_OMIM_test_080118",
        //    "X:\\Zeiss\\DATA\\geneProtein_OUT_test_080118");
        gene.generateGene("Z:\\Zeiss\\DATA\\pubmed1950brat", "", SAVE_TO_DB);
    
    
         }catch(Exception e)
        {
        e.printStackTrace();
    } */
}
