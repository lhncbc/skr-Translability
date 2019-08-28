package gov.nih.nlm.skr.CZ;

import java.util.Comparator;

public class BratConceptComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		BratConcept bc1 = (BratConcept) o1;
		BratConcept bc2 = (BratConcept) o2;
		if(bc1.startOffset == bc2.startOffset)
			return 0;
		else if(bc1.startOffset < bc2.startOffset)
			return -1;
		else 
			return 1;
	}
} 
