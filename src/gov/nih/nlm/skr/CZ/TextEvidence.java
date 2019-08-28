package gov.nih.nlm.skr.CZ;
public class TextEvidence {
	String text;
	int start;
	int end;
	public TextEvidence(String text, int start, int end) {
		this.text = text;
		this.start = start;
		this.end = end;
	}
	
	public TextEvidence( int start, int end) {
		this.start = start;
		this.end = end;
	}

	public boolean equals(Object o) {
		TextEvidence t = (TextEvidence) o;
		if(/*( this.text.compareTo(t.text) == 0) && */ this.start == t.start && this.end == t.end)
			return true;
		else 
			return false;
	}

	public int hashCode() {
		int hash = 17;
		// hash = hash * 31 + text.hashCode();
		hash = hash + start * 31;
		hash = hash  + end * 19;
		return hash;
	}

}
