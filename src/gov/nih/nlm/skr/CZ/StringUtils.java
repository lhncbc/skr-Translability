package gov.nih.nlm.skr.CZ;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    /**
     * 
     * @param concept
     * @param targetWord
     * @return June 23 2017 Find concepts from a given word like find MPTP from the
     *         word MPTP-induced
     */
    public static String retrieveConcept(String concept, String targetWord) {
	String concepttemp = concept.toLowerCase();
	String targetWordtemp = targetWord.toLowerCase();
	if (targetWordtemp.contains(concepttemp)) {
	    int start = targetWordtemp.indexOf(concepttemp);
	    int end = start + concepttemp.length();
	    if (start == 0) { // compare MPTP with MPTP-induced
		if (targetWord.length() == end || !Character.isAlphabetic(targetWord.charAt(end))) {
		    return targetWord.substring(start, end); // the case MPTP and MPTP-induced
		    // return concept; // the case MPTP and MPTP-induced

		} else
		    return null;
	    } else {
		if (!Character.isAlphabetic(targetWord.charAt(start - 1))
			&& (targetWord.length() == end || !Character.isAlphabetic(targetWord.charAt(end)))) {
		    return targetWord.substring(start, end);
		    // return concept;
		} else
		    return null;
	    }
	} else
	    return null;
    }

    public static boolean checkIfWordIsNotSubstring(int offset, String word, String text) {
	word = word.toLowerCase();
	text = text.toLowerCase();
	int endOffset = offset + word.length();
	if (offset == 0) { // string is the first word of the text
	    if (endOffset < text.length()) { // if the string is not the end word of the text
		if (!Character.isAlphabetic(text.charAt(endOffset)))
		    return true; // if the next character of the last character of the string is not ascii, return true
		else
		    return false;
	    } else // the word is the whole text, so return true
		return true;
	} else if (offset > 0) { // string is not the first word of the text
	    if (endOffset < text.length()) { // if the string is not the end word of the text
		if (!Character.isAlphabetic(text.charAt(offset - 1))
			&& !Character.isAlphabetic(text.charAt(endOffset))) { // if both character of end is not ascii
		    return true;
		} else
		    return false;
	    } else { // if the string is the end word of the text, but not the first word
		if (!Character.isAlphabetic(text.charAt(offset - 1)))
		    return true;
		else
		    return false;
	    }
	} else
	    return false;

    }

    /**
     * July 25 2017 Add a concept to the list if the concept is subsuming
     * (encompassing) other concepts and remove the subsumed concept For instance,
     * alpha-syn subsumes syn if they happen to locate in the same place.
     * 
     * @param conceptList
     * @param cst
     * @return
     */
    public static List<BratConcept> subsumeCheckAndAdd(List<BratConcept> geneList, BratConcept gene) {
	List<BratConcept> newList = new ArrayList<>();
	boolean found = false;
	if (gene.name == null || gene.name.equals(""))
	    return geneList;
	for (int i = 0; i < geneList.size(); i++) {
	    BratConcept curGene = geneList.get(i);
	    if (curGene.name == null)
		continue;
	    else if (gene.name.toLowerCase().compareTo(curGene.name.toLowerCase()) == 0
		    && gene.startOffset == curGene.startOffset) {
		newList.add(gene);
		found = true;
	    } else if (gene.name.toLowerCase().contains(curGene.name.toLowerCase())) {
		// int lenDiff = absDiff(gene.indicatedString.length(), curConcept.indicatedString.length());
		if ((gene.startOffset == curGene.startOffset)) { // cst concept subsumes curCocnept and the string starts at the same position
		    found = true;
		    newList.add(gene);
		} else if (gene.startOffset < curGene.startOffset && gene.endOffset >= curGene.endOffset) { // cst concept subsumes curCocnept
		    found = true;
		    newList.add(gene);
		} else
		    newList.add(curGene);
	    } else if (curGene.name.toLowerCase().contains(gene.name.toLowerCase())) {
		// int lenDiff = absDiff(gene.indicatedString.length(), curConcept.indicatedString.length());
		if ((gene.startOffset == curGene.startOffset)) { // cst concept subsumes curCocnept and the string starts at the same position
		    found = true;
		    newList.add(curGene);
		} else if (gene.startOffset > curGene.startOffset && gene.endOffset <= curGene.endOffset) { // cst concept subsumes curCocnept
		    found = true;
		    newList.add(curGene);
		} else
		    newList.add(curGene);
	    } else
		newList.add(curGene);
	} // end of for
	if (found == false)
	    newList.add(gene);
	return newList;
    }

    public static void main(String[] argv) {
	// String text = new String("Treatment of animals and dopaminergic SH-SY5Y cells with DMF resulted in increased nuclear levels of active Nrf2, with subsequent upregulation of antioxidant target genes");
	// String word = new String("Treatment");
	// boolean IsString = checkIfWordIsNotSubstring(0, word, text);
	// String word = new String("cells");
	// boolean IsString = checkIfWordIsNotSubstring(46, word, text);
	// String word = new String("SH-S");
	// boolean IsString = checkIfWordIsNotSubstring(38, word, text);
	String word = new String("VPS35");

	String text = new String(
		"Parkinson's disease-associated mutant VPS35 causes mitochondrial dysfunction by recycling DLP1 complexes. Mitochondrial dysfunction represents a critical step during the pathogenesis of Parkinson's disease (PD), and increasing evidence suggests abnormal mitochondrial dynamics and quality control as important underlying mechanisms. The VPS35 gene, which encodes a key component of the membrane protein-recycling retromer complex, is the third autosomal-dominant gene associated with PD. However, how VPS35 mutations lead to neurodegeneration remains unclear. Here we demonstrate that PD-associated VPS35 mutations caused mitochondrial fragmentation and cell death in cultured neurons in vitro, in mouse substantia nigra neurons in vivo and in human fibroblasts from an individual with PD who has the VPS35(D620N) mutation. VPS35-induced mitochondrial deficits and neuronal dysfunction could be prevented by inhibition of mitochondrial fission. VPS35 mutants showed increased interaction with dynamin-like protein (DLP) 1, which enhanced turnover of the mitochondrial DLP1 complexes via the mitochondria-derived vesicle-dependent trafficking of the complexes to lysosomes for degradation. Notably, oxidative stress increased the VPS35-DLP1 interaction, which we also found to be increased in the brains of sporadic PD cases. These results revealed a novel cellular mechanism for the involvement of VPS35 in mitochondrial fission, dysregulation of which is probably involved in the pathogenesis of familial, and possibly sporadic, PD.");
	boolean IsString = checkIfWordIsNotSubstring(337, word, text);
	System.out.println("Answer is : " + IsString);
    }
}
