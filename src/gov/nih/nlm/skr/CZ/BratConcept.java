package gov.nih.nlm.skr.CZ;

import java.util.List;

public class BratConcept {
    String name;
    String preferredName;
    String CUI;
    String semtype;
    String geneId;
    String geneType;
    String substitute;
    int sentNum;
    int startOffset;
    int endOffset;

    // For multiple gene info
    boolean multipleGene = false;
    List<GeneInfo> geneList;

    public BratConcept(String n, int st, int en) {
	name = n;
	startOffset = st;
	endOffset = en;
    }

    public BratConcept(String n, String t, int st, int en) {
	name = n;
	semtype = t;
	startOffset = st;
	endOffset = en;
    }

    public BratConcept(String n, String c, String t, int st, int en) {
	name = n;
	CUI = c;
	semtype = t;
	startOffset = st;
	endOffset = en;
    }

    public BratConcept(String n, String c, int s, int st, int en) {
	name = n;
	CUI = c;
	sentNum = s;
	startOffset = st;
	endOffset = en;
    }

    public int getStartOffset() {
	return startOffset;
    }

    public int getEndOffset() {
	return endOffset;
    }

    public int getSentNum() {
	return sentNum;
    }

    @Override
    public boolean equals(Object o) {
	BratConcept bc = (BratConcept) o;
	return (this.startOffset == bc.startOffset && this.endOffset == bc.endOffset);
    }

    @Override
    public String toString() {
	return new String(name + ":" + CUI + ":" + semtype + ":" + substitute + ":" + startOffset + ":" + endOffset);
    }
}
