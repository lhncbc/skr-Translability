package gov.nih.nlm.skr.CZ;
import java.util.Comparator;
public class ConceptComparator implements Comparator {
	public int compare(Object o1, Object o2) {
		BratConcept bc1 = (BratConcept) o1;
		BratConcept bc2 = (BratConcept) o2;
		if(bc1.sentNum< bc2.sentNum || (bc1.sentNum == bc2.sentNum && bc1.startOffset < bc2.startOffset))
			return -1;
		else if(bc1.sentNum == bc2.sentNum && bc1.startOffset == bc2.startOffset)
			return 0;
		else return 1;
	}
}
