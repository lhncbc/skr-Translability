package gov.nih.nlm.skr.CZ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nih.nlm.ling.core.Span;

public class Utils {

    static List<String> categoryList = Arrays.asList("AUX", "CONJ", "DET", "HEAD", "MOD", "PREP", "PUNC", "SHAPES");
    static Pattern TOKEN_PATTERN = Pattern.compile("\\(\\[(.+?)\\]\\)");
    static Pattern MSU_PATTERN = Pattern.compile("([^\\s]+)");
    static Pattern WORD_PATTERN = Pattern.compile("\\w");

    /**
     * Convert Semrep syntactic unit into Java List Input:
     * [MOD([Tyrosine,kinase]),HEAD([inhibition])] [VERB([facilitates])]
     * [MOD([autophagic]),MOD([SNCA]),PUNC([/]),MOD([alpha,-,synuclein]),HEAD([
     * clearance]),PUNC([.])] Output: ["Tyrosine kinase inhibition", "facilitates",
     * "autophagic", "SNCA", "alpha-synuclein", "clearance"].
     * 
     * @param syntacticUnits
     * @return
     */
    /*
     * public static List ParseSyntacticUnit(String syntacticUnits, String[]
     * WordArray) { // Set Units = new HashSet(Arrays.asList(elements)); List
     * outputList = new ArrayList(); List<String> tempList = new
     * ArrayList<String>(); int locNextParenStart = 0; int locNextParenEnd = 0; int
     * wordIndex = 0; int charIndex = 0; int depth = 0; String inputStr = new
     * String(syntacticUnits); StringBuffer curBuf = new StringBuffer();
     */
    /**
     * First phase is to split syntactic units into the units surrounded by the
     * outermost bracket and save them into List Input:
     * [MOD([Tyrosine,kinase]),HEAD([inhibition])] [VERB([facilitates])]
     * [MOD([autophagic]),MOD([SNCA]),PUNC([/]),MOD([alpha,-,synuclein]),HEAD([
     * clearance]),PUNC([.])] Output: List(0) =
     * MOD([Tyrosine,kinase]),HEAD([inhibition]) List(1) = VERB([facilitates]),
     * List(2) =
     * MOD([autophagic]),MOD([SNCA]),PUNC([/]),MOD([alpha,-,synuclein]),HEAD([
     * clearance]),PUNC([.])
     **/
    /*
     * while(charIndex < inputStr.length()) { if(inputStr.charAt(charIndex) == '[')
     * { if(depth == 0) curBuf = new StringBuffer(); depth++; // increment the depth
     * of the syntactic unit } else if (inputStr.charAt(charIndex) == ']') {
     * depth--; if(depth == 0) { tempList.add(curBuf.toString()); // Add the outer
     * syntactic unit } } else if(inputStr.charAt(charIndex) == ',' &&
     * inputStr.charAt(charIndex+1) == '-' && inputStr.charAt(charIndex+2) == ','){
     * // do nothing for "," curBuf.append("-"); charIndex = charIndex+2; } else {
     * curBuf.append(inputStr.charAt(charIndex)); } charIndex++; }
     * 
     * int wordPos = 0;
     * 
     * for(String aUnit: tempList) { charIndex=0; int startWordIndex = wordPos; int
     * endWordIndex = wordPos; // System.out.println("\t\t->" + aUnit); StringBuffer
     * thisBuf = new StringBuffer(); while(charIndex < aUnit.length() && wordPos <
     * WordArray.length) { if(aUnit.charAt(charIndex) == '[' ||
     * aUnit.charAt(charIndex) == ']' || aUnit.charAt(charIndex) == '(' ||
     * aUnit.charAt(charIndex) == ')' || aUnit.charAt(charIndex) == ',' ||
     * aUnit.charAt(charIndex) == '/') charIndex++; else if(aUnit.charAt(charIndex)
     * == '-' ) { thisBuf.append("-"); charIndex++; } else
     * if(aUnit.substring(charIndex).startsWith("MOD") ||
     * aUnit.substring(charIndex).startsWith("DET") ||
     * aUnit.substring(charIndex).startsWith("AUX") ||
     * aUnit.substring(charIndex).startsWith("ADV")) charIndex = charIndex+3; else
     * if(aUnit.substring(charIndex).startsWith("HEAD") ||
     * aUnit.substring(charIndex).startsWith("VERB") ||
     * aUnit.substring(charIndex).startsWith("PUNC") ||
     * aUnit.substring(charIndex).startsWith("PREP") ||
     * aUnit.substring(charIndex).startsWith("CONJ") ||
     * aUnit.substring(charIndex).startsWith("PRON")) charIndex = charIndex+4; else
     * if(aUnit.substring(charIndex).startsWith("SHAPES")) charIndex = charIndex+6;
     * else { if(aUnit.substring(charIndex).startsWith(WordArray[wordPos])) {
     * thisBuf.append(" " + WordArray[wordPos]); charIndex = charIndex +
     * WordArray[wordPos].length(); wordPos++; } else
     * if(WordArray[wordPos].contains("-")) { // System.out.println(
     * "\t\\ttMeet Hypen Word : " + WordArray[wordPos]); String[] compo =
     * WordArray[wordPos].split("-"); if(compo != null && compo.length > 0) { // if
     * the work is not "-" while(charIndex < aUnit.length()) {
     * if(aUnit.substring(charIndex).startsWith(compo[compo.length-1])) { //
     * System.out.println("??????? Parse guess  : " + compo[compo.length-1]); //
     * System.out.println("Whole syntactic unit : " + WordArray[wordPos]);
     * thisBuf.append(" " + WordArray[wordPos]); wordPos++; break; } else
     * charIndex++; } // while } else wordPos++;
     * 
     * charIndex++; } else {
     * 
     * July 14 2017 When words in a sentecne and the part of its syntactic Unit does
     * not match, this algorithm copies the word in sentence to MSUList and move on
     * For instance, in the folloing case, MSUList : [HEAD([Curcumin])]
     * [MODAL([may])] [AUX([be])] [HEAD([effective])]
     * [PREP([in]),HEAD([preventing])] [CONJ([or])] [HEAD([slowing])]
     * [DET([the]),HEAD([progression])]
     * [PREP([of]),HEAD([Parkinson's,disease]),PUNC([.])] Sentence:
     * SE|26538809||ab|8|text|Curcumin may be effective in preventing or slowing the
     * progression of PD.
     * 
     * Parkinson's disease and PD does not match, so this algorithm copied PD to the
     * output MSUList
     * 
     * charIndex++; }
     * 
     * } } if(thisBuf.toString().trim().length() > 0) outputList.add(new
     * MSU(thisBuf.toString().trim(), startWordIndex, wordPos-1));
     * 
     * }
     * 
     * return outputList; }
     */
    public static List ParseSyntacticUnit(BratSentence sentence) {
	List<MSU> msus = new ArrayList<>();
	// [of Parkinson disease], 100-200
	String inputStr = sentence.syntacticUnit;
	String text = sentence.text;
	int offset = sentence.startPos;
	//		 System.out.println("SYN: " + inputStr);
	int sentIndex = 0;
	Matcher msuMatch = MSU_PATTERN.matcher(inputStr);
	while (msuMatch.find()) {
	    String s = msuMatch.group(1);
	    //			 System.out.println("MSU: " + s);
	    int msuStart = -1;
	    int msuEnd = -1;
	    Matcher tokenMatch = TOKEN_PATTERN.matcher(s);
	    while (tokenMatch.find()) {
		String token = tokenMatch.group(1);
		//				 System.out.println("SENT INDEX:" + sentIndex);
		//				 System.out.println("TOKEN: " + token);
		if (token.equals(",")) {
		    int startInd = text.indexOf(",", sentIndex);
		    if (startInd == -1)
			continue;
		    if (msuStart == -1) {
			msuStart = startInd;
			msuEnd = msuStart + token.length();
			msus.add(new MSU(text.substring(msuStart, msuEnd), msuStart + offset, msuEnd + offset));
		    }
		    sentIndex = msuEnd;

		} else {
		    String[] tokens = token.split(",");
		    for (int i = 0; i < tokens.length; i++) {
			int startInd = text.indexOf(tokens[i], sentIndex);
			if (startInd == -1) {
			    continue;
			    /*
			     * Matcher nonSpace = MSU_PATTERN.matcher(text.substring(sentIndex)); if
			     * (nonSpace.find()) startInd = nonSpace.start() + sentIndex;
			     */
			}
			if (msuStart == -1) {
			    msuStart = startInd;
			}
			msuEnd = startInd + tokens[i].length();
			if (msuEnd != -1)
			    sentIndex = msuEnd;
		    }
		}
	    }
	    if (msuStart != -1 && msuEnd != -1)
		msus.add(new MSU(text.substring(msuStart, msuEnd), msuStart + offset, msuEnd + offset));
	}
	/*
	 * for (MSU msu: msus) { System.out.println(msu.concept + "\t" + msu.startPos +
	 * "\t" + msu.endPos + "\t" + msu.locInSent); }
	 */
	return msus;
    }

    /**
     * 
     * @return
     */

    static int msuDistance(int pos1, int pos2, List msuList) {
	int distance = Integer.MAX_VALUE;
	int index = 0;
	if (pos2 < pos1) {
	    int temp = pos2;
	    pos2 = pos1;
	    pos1 = temp;
	}
	boolean foundStartUnit = false;
	while (index < msuList.size()) {
	    MSU thisMSU = (MSU) msuList.get(index);
	    if (foundStartUnit == false && thisMSU.startPos <= pos1 && thisMSU.endPos >= pos1) {
		foundStartUnit = true;
		distance = 0;
	    } else if (foundStartUnit == true && thisMSU.startPos > pos1 && thisMSU.endPos > pos1
		    && /* thisMSU.startPos <= pos2 && */ thisMSU.endPos < pos2)
		distance++;
	    // else if (foundStartUnit = true && thisMSU.startPos >= pos1 && thisMSU.endPos > pos1 && thisMSU.startPos <= pos2 && thisMSU.endPos <= pos2) {
	    else if (foundStartUnit = true && thisMSU.startPos > pos2) {
		// distance++;
		break;
	    }
	    index++;
	}
	return distance;
    }

    static int msuDistance(ConceptLocation cl1, ConceptLocation cl2, List msuList) {
	int cl1s = cl1.startPos;
	int cl1e = cl1.endPos;
	int cl2s = cl2.startPos;
	int cl2e = cl2.endPos;
	int distance = Integer.MAX_VALUE;
	int index = 0;
	int pos1 = cl1e;
	int pos2 = cl2s;
	if (pos2 < pos1) {
	    pos2 = cl1s;
	    pos1 = cl2e;
	}
	boolean foundStartUnit = false;
	while (index < msuList.size()) {
	    MSU thisMSU = (MSU) msuList.get(index);
	    //			Matcher wp = WORD_PATTERN.matcher(thisMSU.concept);
	    //			if (wp.find() == false) { index++;continue;}
	    if (foundStartUnit == false && thisMSU.startPos <= pos1 && thisMSU.endPos >= pos1) {
		foundStartUnit = true;
		distance = 0;
	    } else if (foundStartUnit == true && thisMSU.startPos > pos1 && thisMSU.endPos > pos1
		    && pos2 > thisMSU.startPos/*
					       * thisMSU.startPos <= pos2 && thisMSU.endPos < pos2
					       */)
		distance++;
	    else if (foundStartUnit = true && thisMSU.startPos >= pos1 && thisMSU.endPos > pos1
		    && thisMSU.startPos > pos2) {
		//distance++;
		break;
	    }
	    index++;
	}
	return distance;
    }

    public static boolean isCUI(String aString) {
	if (!aString.startsWith("C") || aString.length() < 8)
	    return false;
	else {
	    String remaining = aString.substring(1, 8);
	    try {
		int num = Integer.parseInt(remaining);
	    } catch (Exception e) {
		return false;
	    }
	}
	return true;
    }

    /*
     * July 24 2017
     * 
     */
    public static int findConceptPosFromSent(String indicator, String sentence, int searchPos) {
	int beginSearch = searchPos;
	int index = 0;
	/*
	 * System.out.println(indicator); System.out.println(sentence);
	 * System.out.println(searchPos + "\n---\n");
	 */
	if (indicator.length() <= 0)
	    return 0;
	boolean caseIgnored = false;
	if (searchPos > 0)
	    caseIgnored = true;
	while (beginSearch < sentence.length()) {
	    index = sentence.indexOf(indicator, beginSearch);
	    if (index == 0) {
		/*
		 * June 20 2017 If the indicated word ends with non alphabetic letter, like
		 * "MHC-", the letter after that in the sentence does not need to be
		 * non-alphabetic. For instance, MHC- needs to match with "MHC-II+ (Citation
		 * 26300398 ab.5)
		 */
		// if(Character.isAlphabetic(indicator.length()-1))  {// If the last character of indicated word is an alphabet
		if (Character.isAlphabetic(indicator.charAt(indicator.length() - 1))) {// If the last character of indicated word is an alphabet
		    if (!Character.isAlphabetic(sentence.charAt(index + indicator.length()) + 1))
			return index;
		} else
		    return index;
	    } else if (index > 0) {

		char startC = sentence.charAt(index - 1);
		char endCC = sentence.charAt(index + indicator.length());
		boolean isChar = Character.isAlphabetic(indicator.charAt(indicator.length() - 1));
		if (isChar) { // If the last character of indicated word is an alphabet
		    if (!Character.isAlphabetic(sentence.charAt(index + indicator.length()))
			    && !Character.isAlphabetic(sentence.charAt(index - 1)))
			// if(!Character.isAlphabetic(sentence.charAt(index + indicatedString.length())) || (!Character.isAlphabetic(sentence.charAt(index -1)) && )
			return index;
		} else
		    return index;
	    } else if (index < 0) {
		if (caseIgnored == true) // If the comparison is done with case igonored, there is no way that the indicator is found in the sentence, so return -1
		    return -1;
		indicator = indicator.toLowerCase();
		sentence = sentence.toLowerCase();
		caseIgnored = true;
		// return -1;
	    }
	    beginSearch = index + 1;
	}
	return index;

    }

    public static List<Span> findConceptPosFromSent(String indicator, String sentence, boolean ignoreCase) {
	List<Span> spans = new ArrayList<>();
	if (indicator.length() <= 0)
	    return spans;
	String lwcS = sentence;
	String lwcI = indicator;
	if (ignoreCase) {
	    lwcS = sentence.toLowerCase();
	    lwcI = indicator.toLowerCase();
	}
	int index = 0;
	while (index <= sentence.length()) {
	    index = lwcS.indexOf(lwcI, index);
	    if (index == -1)
		break;
	    if (!Character.isLetterOrDigit(sentence.charAt(index + indicator.length())))
		//   		if (Character.isAlphabetic(indicator.charAt(indicator.length() - 1))) {// If the last character of indicated word is an alphabet
		//   		    if (!Character.isAlphabetic(sentence.charAt(index + indicator.length()) + 1)) 
		spans.add(new Span(index, index + indicator.length()));
	    //  		}
	    index += indicator.length();
	}
	return spans;
    }

    /**
     * July 25 2017 Modify normalizeAndAdd in order to check subsumsion as well
     * 
     * @param conceptList
     * @param cst
     * @return
     */
    public static List<BratConcept> normalizeAndAdd(List<BratConcept> conceptList, BratConcept cst) {
	List<BratConcept> newList = new ArrayList<>();
	boolean found = false;
	if (cst.name == null || cst.name.equals(""))
	    return conceptList;
	for (int i = 0; i < conceptList.size(); i++) {
	    BratConcept curConcept = conceptList.get(i);
	    if (curConcept.name == null)
		continue;
	    else if (cst.name.toLowerCase().compareTo(curConcept.name.toLowerCase()) == 0
		    && cst.startOffset == curConcept.startOffset) {
		newList.add(cst);
		found = true;
	    } else if (cst.name.toLowerCase().contains(curConcept.name.toLowerCase())) {
		// int lenDiff = absDiff(cst.indicatedString.length(), curConcept.indicatedString.length());
		if ((cst.startOffset == curConcept.startOffset)) { // cst concept subsumes curCocnept and the string starts at the same position
		    found = true;
		    newList.add(cst);
		} else if (cst.startOffset < curConcept.startOffset
			&& (cst.startOffset + cst.name.length()) >= curConcept.startOffset + curConcept.name.length()) { // cst concept subsumes curCocnept
		    found = true;
		    newList.add(cst);
		} else
		    newList.add(curConcept);
	    } else if (curConcept.name.toLowerCase().contains(cst.name.toLowerCase())) {
		// int lenDiff = absDiff(cst.indicatedString.length(), curConcept.indicatedString.length());
		if ((cst.startOffset == curConcept.startOffset)) { // cst concept subsumes curCocnept and the string starts at the same position
		    found = true;
		    newList.add(curConcept);
		} else if (cst.startOffset > curConcept.startOffset
			&& (cst.startOffset + cst.name.length()) <= curConcept.startOffset + curConcept.name.length()) { // cst concept subsumes curCocnept
		    found = true;
		    newList.add(curConcept);
		} else
		    newList.add(curConcept);
	    } else
		newList.add(curConcept);
	} // end of for
	if (found == false)
	    newList.add(cst);
	return newList;
    }

    /*
     * Sep 19 2017 Add OMIM concept to the concept list only if it is not previously
     * added
     */
    public static List<BratConcept> addUniqueOMIMConcepts(List<BratConcept> conceptList, BratConcept bc) {
	boolean found = false;
	for (int i = 0; i < conceptList.size(); i++) {
	    BratConcept curConcept = conceptList.get(i);
	    if (curConcept.equals(bc)) {
		found = true;
		break;
	    }
	} // end of for
	if (found == false)
	    conceptList.add(bc);
	return conceptList;
    }

    public static int findWordPos(int position, String sentence) {
	int count = 0;
	if (position == 0)
	    return 0;
	else {
	    int index = 0;
	    while (index < position) {
		count++;
		index = sentence.indexOf(" ", index);
		if (index < 0)
		    return count;
		index++;
	    }
	    return count;
	}
    }

    public static List<BratConcept> removeHopsFromList(List<BratConcept> hopsList, List<BratConcept> conceptList) {
	List<BratConcept> newList = new ArrayList<>();
	for (BratConcept bc : conceptList) {
	    if (!hopsList.contains(bc))
		newList.add(bc);
	}
	return newList;
    }

    public static List calculateSU(String syntacticUnit, String sentence, List<Integer> posList) {
	String[] SUCompo = syntacticUnit.split("\\s+");
	List SUList = new ArrayList();
	int SUCompoPtr = 0;
	int SUPtr = 0;
	int sentPtr = 0;
	int MSUUnit = 0;
	while (SUCompoPtr < SUCompo.length) {
	    List wordList = convertSyntacticSymbol(SUCompo[SUCompoPtr], MSUUnit);
	    if (SUCompo[SUCompoPtr].contains("HEAD"))
		MSUUnit++;
	    ;
	}
	return SUList;

    }

    /*
     * public static List<PositionMSUPair> calculateSU(String syntacticUnit, String
     * sentence, List<ConceptLocation> posList, int sentOffset, int sentNum) {
     * String[] SUCompo = syntacticUnit.split("\\s+"); List<WordNumberPair> SUList =
     * new ArrayList<WordNumberPair>(); List<PositionMSUPair> PosMSUList = new
     * ArrayList<PositionMSUPair>(); int SUCompoPtr = 0;
     * 
     * int MSUUnit = 0;
     * 
     * while(SUCompoPtr <SUCompo.length ) { if(SUCompo[SUCompoPtr].contains("HEAD"))
     * MSUUnit++; SUList.addAll(convertSyntacticSymbol(SUCompo[SUCompoPtr], MSUUnit,
     * sentNum)); SUCompoPtr++; } int SUPtr = 0; int sentPtr = 0; try {
     * for(ConceptLocation cl: posList) { int position = cl.wordPos; while(sentPtr
     * <= position && SUPtr < SUList.size()) { WordNumberPair curPair =
     * SUList.get(SUPtr);
     * 
     * String curWord = curPair.word; int curMSU = curPair.number;
     * if(sentence.substring(sentPtr).contains(curWord)) { //
     * if(sentence.substring(sentPtr).startsWith(curWord)) { // current word from
     * MSU match with the sentence part sentPtr = sentPtr + curWord.length();
     * SUPtr++; if(sentPtr + sentOffset >= position) { PositionMSUPair posMSUPair =
     * new PositionMSUPair(position, curMSU); PosMSUList.add(posMSUPair); break; } }
     * else
     * 
     * sentPtr++; // if it does not match, advance sentence position by 1 since
     * there may be a space in the sentence } else
     * 
     * SUPtr++; }
     * 
     * } return PosMSUList; } catch(java.lang.StringIndexOutOfBoundsException ex) {
     * return new ArrayList(); // When there is an error, return empty list }
     * 
     * 
     * }
     */

    public static List<WordNumberPair> convertSyntacticSymbol(String syntacticUnit, int MSUUnit) {
	if (syntacticUnit.length() == 0)
	    return new ArrayList<>();
	else if ((syntacticUnit.length() == 1)) {
	    ArrayList<WordNumberPair> singletonList = new ArrayList<>();
	    singletonList.add(new WordNumberPair(syntacticUnit, MSUUnit));
	    return singletonList;
	}

	List<WordNumberPair> finalList = new ArrayList<>();
	int index = 0;
	// Stack<Character> curStack = new Stack<Character>();

	/*
	 * if the first character is '[' or ']', skip the letter in order to avoid the
	 * increse depth
	 */
	int depth = 0;
	if (syntacticUnit.charAt(0) == '[' || syntacticUnit.charAt(index) == '(')
	    index++;
	StringBuffer curBuf = new StringBuffer();
	boolean markPunctuation = false;
	List<WordNumberPair> curList = new ArrayList<>();
	while (index < syntacticUnit.length()) { // repeat it excluding "[" at the start and "]" at the end
	    if (syntacticUnit.substring(index).startsWith("PUNC([")) {
		curBuf.append(syntacticUnit.charAt(index + 6));
		index = index + 8;
		// markPunctuation = true;
	    } else if (index > 0 && syntacticUnit.charAt(index) == ',') {
		List<WordNumberPair> recList = convertSyntacticSymbol(curBuf.toString(), MSUUnit);
		curList.addAll(recList);
		curBuf = new StringBuffer();
		/*
		 * When input is like "[MOD([K,(,i,),=]),SHAPES([17,.,0]),HEAD([nM,)])]" "(" or
		 * ")" need to be part of real output. Otherwise the output is K i = 17 . 0 nM
		 */
		if (syntacticUnit.charAt(index + 1) == '(' || syntacticUnit.charAt(index + 1) == ')'
			|| syntacticUnit.charAt(index + 1) == '[' || syntacticUnit.charAt(index + 1) == ']') {
		    curBuf.append(syntacticUnit.charAt(index + 1));
		    index++;
		}
	    } else if (syntacticUnit.charAt(index) == '[' || syntacticUnit.charAt(index) == '(') {
		// curStack.push(syntacticUnit.charAt(index));
		depth++;
		// curBuf.append(syntacticUnit.charAt(index) );
	    } else if (syntacticUnit.charAt(index) == ']' || syntacticUnit.charAt(index) == ')') {
		depth--;
		/*
		 * if(curStack.isEmpty()) depth--; else { Character prevParen = curStack.pop();
		 * if(matchParenthesis(prevParen, syntacticUnit.charAt(index))) depth--; else {
		 * curBuf.append(prevParen); depth= depth-1; } }
		 */
		// curBuf.append(syntacticUnit.charAt(index) );
	    } else {
		if (syntacticUnit.substring(index).startsWith("AUX"))
		    index = index + 2;
		else if (syntacticUnit.substring(index).startsWith("CONJ"))
		    index = index + 3;
		else if (syntacticUnit.substring(index).startsWith("DET"))
		    index = index + 2;
		else if (syntacticUnit.substring(index).startsWith("HEAD"))
		    index = index + 3;
		else if (syntacticUnit.substring(index).startsWith("MOD"))
		    index = index + 2;
		else if (syntacticUnit.substring(index).startsWith("PREP"))
		    index = index + 3;
		else if (syntacticUnit.substring(index).startsWith("PRON"))
		    index = index + 3;
		else if (syntacticUnit.substring(index).startsWith("PUNC([")) {
		    index = index + 8;
		    curBuf.append(syntacticUnit.charAt(index + 6));
		    // markPunctuation = true;
		} else if (syntacticUnit.substring(index).startsWith("SHAPES"))
		    index = index + 5;
		else if (syntacticUnit.substring(index).startsWith("VERB"))
		    index = index + 3;
		else
		    curBuf.append(syntacticUnit.charAt(index));
	    }
	    index++;
	}
	if (curBuf.length() > 0) {
	    curList.add(new WordNumberPair(curBuf.toString(), MSUUnit));
	    curBuf = new StringBuffer();
	}
	finalList.addAll(curList);
	return finalList;
    }

    static boolean matchParenthesis(Character prev, Character cur) {
	if ((prev == '[' && cur == ']') || (prev == '(' && cur == ')'))
	    return true;
	else
	    return false;

    }

    static boolean isSubsumed(BratConcept ba, BratConcept bb) {
	return (ba.startOffset >= bb.startOffset && ba.endOffset <= bb.startOffset);
    }

    public static void main(String[] argv) {
	String sy = new String(
		"[MOD([Compound]),SHAPES([1])]      [AUX([is])]      [DET([a]),MOD([potent]),HEAD([A])]      [PUNC([(]),HEAD([2A])]      [PUNC([)])]      [PUNC([/]),HEAD([A,(,1])]      [PUNC([)])]      [MOD([receptor]),HEAD([antagonist])]      [HEAD([in,vitro])]      [PUNC([(]),HEAD([A])]      [PUNC([(]),HEAD([2A])]      [PUNC([)])]      [MOD([K,(,i,),=]),SHAPES([4,.,1]),HEAD([nM])]      [PUNC([;]),HEAD([A,(,1])]      [PUNC([)])]      [MOD([K,(,i,),=]),SHAPES([17,.,0]),HEAD([nM,)])]      [PRON([that])]      [AUX([has])]      [MOD([excellent]),HEAD([activity]),PUNC([,])]      [CONJ([after])]      [HEAD([oral,administration]),PUNC([,])]      [PREP([across]),DET([a]),HEAD([number])]      [PREP([of]),HEAD([animal,models])]      [PREP([of]),HEAD([Parkinson's,disease])]      [VERB([including])]      [HEAD([mouse])]      [CONJ([and])]      [HEAD([rat])]      [VERB([models])]      [PREP([of]),MOD([haloperidol]),PUNC([-]),MOD([induced]),HEAD([catalepsy]),PUNC([,])]      [MOD([mouse]),HEAD([model])]      [PREP([of]),MOD([reserpine]),PUNC([-]),MOD([induced]),HEAD([akinesia]),PUNC([,])]      [MOD([rat]),SHAPES([6]),PUNC([-]),HEAD([hydroxydopamine])]      [PUNC([(]),SHAPES([6]),PUNC([-]),HEAD([OHDA])]      [PUNC([)])]      [MOD([lesion]),HEAD([model])]      [PREP([of]),MOD([drug,-,induced]),HEAD([rotation]),PUNC([,])]      [CONJ([and])]      [MOD([MPTP]),PUNC([-]),MOD([treated]),MOD([non,-,human,primate]),HEAD([model]),PUNC([.])]  ");
	String sentence = "Compound 1 is a potent A(2A)/A(1) receptor antagonist in vitro (A(2A) K(i) = 4.1 nM; A(1) K(i) = 17.0 nM) that has excellent activity, after oral administration, across a number of animal models of Parkinson's disease including mouse and rat models of haloperidol-induced catalepsy, mouse model of reserpine-induced akinesia, rat 6-hydroxydopamine (6-OHDA) lesion model of drug-induced rotation,";
	String[] SUCompo = sy.split("\\s+");
	int MSUUnit = 0;
	for (int i = 0; i < SUCompo.length; i++) {
	    System.out.println(SUCompo[i]);
	    if (SUCompo[i].contains("HEAD"))
		MSUUnit++;
	    List<WordNumberPair> finalL = convertSyntacticSymbol(SUCompo[i], MSUUnit);

	    for (int j = 0; j < finalL.size(); j++)
		System.out.println("\t" + finalL.get(j) + " ");
	}
	System.out.println();
	//  List<String> finalL = convertSyntacticSymbol("[MOD([receptor]),HEAD([antagonist])]");
	// List<String> finalL = convertSyntacticSymbol("[MOD([r]),HEAD([a])]");
	/*
	 * List<String> finalL =
	 * convertSyntacticSymbol("[PREP([of]),HEAD([Parkinson's,disease])]");
	 */
	/*
	 * List<String> finalL = convertSyntacticSymbol("[PUNC([(]),HEAD([A])]");
	 * for(int i=0; i < finalL.size(); i++) System.out.println(finalL.get(i));
	 */
    }
}
