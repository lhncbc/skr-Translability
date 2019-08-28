package gov.nih.nlm.skr.CZ;

import java.util.Comparator;

public class PairComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		WordNumberPair w1 = (WordNumberPair) o1;
		WordNumberPair w2 = (WordNumberPair) o2;
		if(w1.number > w2.number)
			return 1;
		else if (w1.number == w2.number)
			return 0;
		else 
			return -1;
		
	}
}
