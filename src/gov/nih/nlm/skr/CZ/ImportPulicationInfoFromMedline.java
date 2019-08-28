package gov.nih.nlm.skr.CZ;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
import java.lang.NumberFormatException;
import java.net.URL;
import java.security.Security;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Vector;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;


public class ImportPulicationInfoFromMedline {
	public static void main(String[] args) {
		try {
			  File dir = null;
			  FilenameFilter filter = null;
				System.out.println("Start importing");
			  Class.forName( "com.mysql.jdbc.Driver" ) ;
				Connection conn = DriverManager.getConnection( "jdbc:mysql://indsrv2.nlm.nih.gov/" + args[0] + "?autoReconnect=true", "root", "indsrv2@root" ) ;
				Statement stmtCitation = conn.createStatement() ;

			String SQLInsertHead = new String("insert into PUBLICATION (PMID, PUB_YEAR, TITLE) VALUES (");
			String SQLInsertEnd = new String(" on DUPLICATE KEY UPDATE PUB_YEAR = ");
			 dir = new File(args[1]);
		  		// It is also possible to filter the list of returned files.
		  		// This example does not return any files that start with `.'.
		    filter = new FilenameFilter() {
		  		public boolean accept(File dir, String name) {
		  			return  name.endsWith(".medline");
		  		}
		    };
		    String[] children = dir.list(filter);
			  System.out.println("Number of medline files: " + children.length);
			  int j = 0;
			  while(children != null && children.length > 0 && j < children.length) {
				  File currentFile =	new File(args[1] + "/" + children[j]);
				  BufferedReader pubin
				  = new BufferedReader(new FileReader(currentFile)); 
				  // MedlineBaseline medSrc = MedlineBaseline.getInstance();
			 // PubMedArticleParser medSrc = new PubMedArticleParser();
			  // int j = 0;
				  String aline = null;
				  String prevPMID = null;
				  String PMID = null;
				  PubmedArticle art = new PubmedArticle();
				  StringBuffer SQLInsert = null;
				  boolean isFound = false;
				  boolean titleFound = false;
				  while((aline = pubin.readLine()) != null) {
					  if(aline.startsWith("PMID-")) {
						  prevPMID = PMID;
						  PMID = aline.split(" ")[1].trim();
						  titleFound = false;
						  if(prevPMID != null) { // if there is a PMID processed						  
							  SQLInsert = new StringBuffer();
							  SQLInsert.append(SQLInsertHead);
							  SQLInsert.append("\"" + art.PMID + "\","); 
							  SQLInsert.append( art.PUB_YEAR + ",");
							  SQLInsert.append("\"" + art.TITLE + "\") ");
							  SQLInsert.append(SQLInsertEnd);
							  SQLInsert.append(art.PUB_YEAR + ", TITLE = \"" + art.TITLE + "\"");
							  isFound = false;
							  System.out.println(SQLInsert.toString());
							  try {
								stmtCitation.executeUpdate(SQLInsert.toString());
							  } catch (Exception e) {
								 // e.printStackTrace();
							  }
						  }
					  
						  art = new PubmedArticle();
						  art.setPMID(PMID);
					  }  else if(aline.startsWith("DP  -")) {
						  String DP = aline.substring(6).trim();
						  String year = findYear(DP);
						  // System.out.println(aline);
						  try {
							  int PYEAR = Integer.parseInt(year);
							  art.setPUB_YEAR(PYEAR);
						  } catch(Exception e) {
							  e.printStackTrace();
						  }
					  } else if(aline.startsWith("TI  -")) {
						  String title = aline.substring(6).trim();
						  title = title.replaceAll("[\"]", "\\\"\"");
						  titleFound = true;
						  // System.out.println(DP1);
						  art.setTITLE(title);
					  } else if(titleFound && aline.startsWith(" ")) {
						  aline = aline.replaceAll("[\"]", "\\\"\"");
						  art.TITLE = new String(art.TITLE + aline);
					  } else if(titleFound && !aline.startsWith(" ")) {
						  titleFound = false;
					  }
						
			  } // while
				  j++;
			  
			  SQLInsert = new StringBuffer();
			  SQLInsert.append(SQLInsertHead);
			  SQLInsert.append("\"" + art.PMID + "\","); 
			  SQLInsert.append( art.PUB_YEAR + ",");
			  SQLInsert.append("\"" + art.TITLE + "\")");
			  SQLInsert.append(SQLInsertEnd);
			  SQLInsert.append(art.PUB_YEAR + ", TITLE = \"" + art.TITLE + "\"");
			  isFound = false;
			  try {
				  System.out.println(SQLInsert.toString());	
				  stmtCitation.executeUpdate(SQLInsert.toString());					
				} catch (Exception e) {
					 e.printStackTrace();
				}
			  }


		 } catch (Exception e) {
			 e.printStackTrace();
		 }
	}
	
	static String findYear(String input) {
		int count = 0; 
	    StringBuffer sb = new StringBuffer();   
		for(int i = 0; i < input.length(); i++){
			 char c = input.charAt(i);
		     if(c > 47 && c < 58){
		            sb.append(c);
		            count++;
		            if(count == 4)
		       		 return sb.toString();
		     } else
		        	count=0;
		 }
		 return sb.toString();
	}
}
