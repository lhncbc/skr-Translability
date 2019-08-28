package gov.nih.nlm.skr.CZ;

public class MESHINFO {
    String TERM;
    String PA;
    String MN;
    String II;
    String UI;
    int HEADING;
    String PRECONCEPT;

    public MESHINFO() {
    }

    public MESHINFO(String TERM, String PA, String MN, String II, int HEADING, String PRECONCEPT, String UI) {
	this.TERM = TERM;
	this.PA = PA;
	this.MN = MN;
	this.II = II;
	this.HEADING = HEADING;
	this.PRECONCEPT = PRECONCEPT;
	this.UI = UI;
    }
}
