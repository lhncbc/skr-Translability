package gov.nih.nlm.skr.CZ;

/*
 * Metadata class that retrieve meta information using SAX parser and insert it into database table PUBLICATION
 * Author: Dongwook Shin
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import gov.nih.nlm.ling.util.FileUtils;

public class Metadata {
    static MedlineBaseline mb = MedlineBaseline.getInstance();
    static MedlineSource ms = MedlineSource.getInstance();
    static Properties properties;
    static Metadata metadata = null;
    String connectionString = null;
    String dbName = null;
    Connection conn;
    Statement stmt;
    static String insertSql = "insert into PUBLICATION (PMID, PUB_YEAR, TITLE, PUBLICATION_TYPE) values (\"";

    public static Metadata getInstance() throws IOException {
	if (metadata == null) {
	    metadata = new Metadata();
	    properties = new Properties();
	    properties = FileUtils.loadPropertiesFromFile("config.properties");
	    metadata.initDB();
	}
	return metadata;
    }

    public Metadata() {

    }

    public void initDB() {
	connectionString = properties.getProperty("connectionString");
	dbName = properties.getProperty("dbName");
	String dbusername = properties.getProperty("dbUsername");
	String dbpassword = properties.getProperty("dbPassword");
	try {
	    Class.forName("com.mysql.jdbc.Driver");
	    conn = DriverManager.getConnection(connectionString + dbName + "?autoReconnect=true", dbusername,
		    dbpassword);
	    stmt = conn.createStatement();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void insertMetadata2DB(String ASCIIXMLDir) {
	try {
	    // MedlineBaseline mb = MedlineBaseline.getInstance();
	    List<String> listInXMLFiles = FileUtils.listFiles(ASCIIXMLDir, false, "xml");

	    for (String inFileName : listInXMLFiles) {
		int pos1 = inFileName.lastIndexOf(File.separator) + 1;
		String fileName = inFileName.substring(pos1).replace(".xml", "");
		String XMLFileName = new String(ASCIIXMLDir + File.separator + fileName + ".xml");
		System.out.println(XMLFileName);
		File XMLFile = new File(XMLFileName);
		List<PubmedArticle> articles = mb.extractCitationInfo(XMLFile);
		insertMetaData2DB(articles);
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    public void insertMetadata2DBFromFile(String PMIDfile, int batchSize) {
	try {
	    BufferedReader br = new BufferedReader(new FileReader(PMIDfile));
	    List<String> PMIDList = FileUtils.linesFromFile(PMIDfile, "UTF-8");
	    int i = 0;
	    List<Integer> batchList = new ArrayList<>();
	    for (String PMID : PMIDList) {
		if ((i > 0) && ((i % batchSize) == 0)) {
		    List<PubmedArticle> articles = ms.fetch(batchList);
		    insertMetaData2DB(articles);
		    batchList = new ArrayList<>();
		} else {
		    batchList.add(Integer.parseInt(PMID));
		}
		i++;
	    }
	    if (batchList.size() > 0) {
		List<PubmedArticle> articles = ms.fetch(batchList);
		insertMetaData2DB(articles);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void insertMetadata2DBFromPMID(int PMID) {
	try {
	    int i = 0;
	    List<PubmedArticle> articles = ms.fetch(PMID);
	    insertMetaData2DB(articles);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void insertMetaData2DB(List<PubmedArticle> articles) {
	for (PubmedArticle article : articles) {
	    try {
		article.TITLE = article.TITLE.replaceAll("\\\\", "\\\\\\\\");
		article.TITLE = article.TITLE.replaceAll("\"", "\\\\\"");
		String curSql = new String(insertSql + article.PMID + "\"," + article.PUB_YEAR + ",\"" + article.TITLE
			+ "\",\"" + article.PUBLICATION_TYPE + "\")");
		stmt.executeUpdate(curSql);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    static public void main(String[] args) {
	// insertMetadata2DB(args[0]);
	// insertMetadata2DBFromFile("C:\\SemMedDatabase\\version 4.0\\PMIDNotFoundInCitationsTable_2.txt", 500);
	// insertMetadata2DBFromFile(args[0], Integer.parseInt(args[1]));
	try {
	    Metadata md = Metadata.getInstance();
	    // md.insertMetadata2DBFromPMID(6915564);
	    md.insertMetadata2DBFromFile("Q:\\LHC_Projects\\Caroline\\NEW_2017\\pubmed1950\\pmid.txt", 500);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
