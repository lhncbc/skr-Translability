package gov.nih.nlm.skr.CZ;

public class PubmedArticle {
    //	private static Log log = LogFactory.getLog(PubmedArticle.class);

    private static final long serialVersionUID = 1L;
    public String PMID;
    public int PUB_YEAR;
    public String PUBLICATION_TYPE;
    public String TITLE;
    public String AB;
    int numAbstractText;

    public PubmedArticle() {
    }

    /**
     *
     */
    public PubmedArticle(String id, int pub_year, String title) {
	this.PMID = id;
	this.PUB_YEAR = pub_year;
	this.TITLE = title;
    }

    /**
     * @return Returns the pubDate.
     */
    public String getPMID() {
	return PMID;
    }

    public void setPMID(String id) {
	this.PMID = id;
    }

    public int getPUB_YEAR() {
	return PUB_YEAR;
    }

    public void setPUB_YEAR(int pub_year) {
	this.PUB_YEAR = pub_year;
    }

    public String getPUBLICATION_TYPE() {
	return PUBLICATION_TYPE;
    }

    public void setPUBLICATION_TYPE(String pubtype) {
	this.PUBLICATION_TYPE = pubtype;
    }

    public String getTITLE() {
	return TITLE;
    }

    public void setTITLE(String title) {
	this.TITLE = title;
    }

    public String getABSTRACT() {
	return AB;
    }

    public void setABSTRACT(String ab) {
	this.AB = ab;
    }

}
