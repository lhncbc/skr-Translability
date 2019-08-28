package gov.nih.nlm.skr.CZ;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ReadConceptList {
    HashSet readInterventionList(String filename) {

	HashSet interventionSet = new HashSet();
	try {
	    BufferedReader list3in = new BufferedReader(new FileReader(filename));
	    String line = null;
	    while ((line = list3in.readLine()) != null) {
		interventionSet.add(line.trim());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return interventionSet;

    }

    HashSet readGeneSignalList(String filename) {

	HashSet geneSet = new HashSet();
	try {
	    BufferedReader genelistin = new BufferedReader(new FileReader(filename));
	    String line = null;
	    while ((line = genelistin.readLine()) != null) {
		if (line.length() > 0)
		    geneSet.add(line.trim());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return geneSet;

    }

    HashSet readOutcomeExceptionSet(String filename) {
	HashSet resultSet = new HashSet();
	try {
	    BufferedReader in = new BufferedReader(new FileReader(filename));
	    String line = null;
	    while ((line = in.readLine()) != null) {
		String compo[] = line.split("\\|");
		resultSet.add(compo[0].trim());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return resultSet;
    }

    List readClinicalOutcomeObjectList(String filename) {
	List resultList = new ArrayList();
	try {
	    BufferedReader in = new BufferedReader(new FileReader(filename));
	    String line = null;
	    while ((line = in.readLine()) != null) {
		resultList.add(line.trim());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return resultList;
    }

    static public HashMap<String, String> readOMIMGeneList(String filename) {

	HashMap<String, String> OMIMRevtable = new HashMap<>();
	try {
	    BufferedReader in = new BufferedReader(new FileReader(filename));
	    String line = null;
	    // PrintWriter OMIMOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter
	    //		(new FileOutputStream("Q:\\LHC_Projects\\Caroline\\data\\OMIMtable.txt"))));
	    while ((line = in.readLine()) != null) {
		String[] compo = line.split("\\t");
		if (compo.length == 2) {
		    OMIMRevtable.put(compo[1].trim().toLowerCase(), compo[0].trim());
		    // OMIMOut.println(compo[1].trim().toLowerCase() + " -> " + compo[0].trim());	 
		    /*
		     * HashSet matchSet = (HashSet) OMIMtable.get(compo[0].trim()); if(matchSet ==
		     * null) { matchSet = new HashSet();
		     * matchSet.add(compo[1].trim().toLowerCase()); OMIMtable.put(compo[0].trim(),
		     * matchSet); } else { matchSet.add(compo[1].trim().toLowerCase()); }
		     */
		}
	    }
	    // OMIMOut.close();

	} catch (Exception e) {
	    e.printStackTrace();
	}
	return OMIMRevtable;
    }

    static public HashMap<String, String> readOMIMHumanGeneIdList(String filename) {
	HashMap<String, String> OMIMGenetable = new HashMap<>();
	try {
	    BufferedReader in = new BufferedReader(new FileReader(filename));
	    String line = null;
	    while ((line = in.readLine()) != null) {
		String[] compo = line.split("\\t");
		if (compo.length == 2) {
		    OMIMGenetable.put(compo[0].trim().toLowerCase(), compo[1].trim());
		    // OMIMOut.println(compo[1].trim().toLowerCase() + " -> " + compo[0].trim());	 
		    /*
		     * HashSet matchSet = (HashSet) OMIMtable.get(compo[0].trim()); if(matchSet ==
		     * null) { matchSet = new HashSet();
		     * matchSet.add(compo[1].trim().toLowerCase()); OMIMtable.put(compo[0].trim(),
		     * matchSet); } else { matchSet.add(compo[1].trim().toLowerCase()); }
		     */
		}
	    }
	    // OMIMOut.close();

	} catch (Exception e) {
	    e.printStackTrace();
	}
	return OMIMGenetable;
    }

}
