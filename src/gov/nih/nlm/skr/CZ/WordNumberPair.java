package gov.nih.nlm.skr.CZ;

/* public class WordNumberPair {
	String word;
	int number;
	int sentNum;
	public WordNumberPair(String w, int n, int sn) {
		this.word = w;
		this.number = n;
		this.sentNum = sn;
	}
	
	public boolean equals(Object o) {
		WordNumberPair wnp = (WordNumberPair) o;
		return this.number == wnp.number;
	}
	
	public String toString() {
		return new String("(" + word + "," + number + ")");
	}
} */

public class WordNumberPair {
	String word;
	int number;
	public WordNumberPair(String w, int n) {
		this.word = w;
		this.number = n;
	}
	
	public String toString() {
		return new String("(" + word + "," + number + ")");
	}
}
