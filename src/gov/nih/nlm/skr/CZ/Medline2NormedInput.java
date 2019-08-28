package gov.nih.nlm.skr.CZ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

public class Medline2NormedInput {

    static public void main(String args[]) {
	try {
	    BufferedReader medin = new BufferedReader(new FileReader(args[0]));
	    PrintWriter out = new PrintWriter(new File(args[1]));
	    /*
	     * Input format: Medline Output Format: PMID- 1234567 TI - title ended by new
	     * line abstract without new line
	     */
	    String aline = null;
	    boolean inTitle = false;
	    boolean inAbstract = false;
	    boolean newlineInsertedAfterTitle = false;
	    boolean newlineInsertedAfterAbstract = false;
	    int numPMID = 0;
	    while ((aline = medin.readLine()) != null) {
		if (aline.startsWith("PMID- ")) {
		    if (numPMID > 0)
			out.println();
		    out.println(aline); // write "PMID- as it is
		    // System.out.println(aline);
		    newlineInsertedAfterTitle = false;
		    numPMID++;
		} else if (aline.startsWith("TI  - ")) {
		    String normedText = aline.substring(6).replace("\\s+", " ").trim();
		    inTitle = true;
		    out.print(aline.substring(0, 6) + normedText); // write "PMID- as it is
		} else if (aline.startsWith("PG  - ") || aline.startsWith("LID - ") || aline.startsWith("BTI - ")) {
		    inTitle = false;
		    /*
		     * if(newlineInsertedAfterTitle == false) { out.println();
		     * newlineInsertedAfterTitle = true; }
		     */
		} else if (aline.startsWith("AB  - ")) {
		    out.println();
		    String normedText = aline.substring(6).replace("\\s+", " ").trim();
		    out.print(normedText);
		    newlineInsertedAfterAbstract = false;
		    inTitle = false;
		    inAbstract = true;
		} else if (aline.startsWith("AD  - ") || aline.startsWith("AU  - ") || aline.startsWith("CI  - ")
			|| aline.startsWith("CN  - ") || aline.startsWith("DEP - ") || aline.startsWith("DP  - ")
			|| aline.startsWith("FAU - ") || aline.startsWith("GR  - ") || aline.startsWith("JT  - ")
			|| aline.startsWith("LA  - ") || aline.startsWith("MH  - ") || aline.startsWith("NH  - ")
			|| aline.startsWith("OT  - ") || aline.startsWith("PL  - ") || aline.startsWith("PT  - ")
			|| aline.startsWith("RN  - ") || aline.startsWith("SB  - ") || aline.startsWith("SO  - ")
			|| aline.startsWith("TA  - ")) {
		    inAbstract = false;
		    /*
		     * if(newlineInsertedAfterAbstract == false) { out.println();
		     * newlineInsertedAfterAbstract = true; }
		     */
		} else if (inTitle == true) { // second title line
		    aline = aline.replace("\\s+", " ").trim();
		    out.print(" " + aline);
		} else if (inAbstract == true) {
		    aline = aline.replace("\\s+", " ").trim();
		    out.print(" " + aline);
		    //  System.out.println(aline);
		} else if (aline.length() == 0) {
		    out.println();
		}
	    }
	    out.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
