package gov.nih.nlm.skr.CZ;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Dongwook Shin
 *
 */
public class MedlineBaseline {

    private static final int MAX_RETURN = 1000000;

    private static MedlineBaseline myInstance;

    private MedlineBaseline() {
    }

    public static MedlineBaseline getInstance() {
	if (myInstance == null)
	    synchronized (MedlineBaseline.class) {
		if (myInstance == null)
		    myInstance = new MedlineBaseline();
	    }
	return myInstance;
    }

    public List<PubmedArticle> extractMetaInfo(File file)
	    throws SAXException, ParserConfigurationException, IOException {
	SAXParserFactory factory = SAXParserFactory.newInstance();
	SAXParser saxParser = factory.newSAXParser();
	MedlineBaselineParser handler = new MedlineBaselineParser();
	saxParser.parse(file, handler);
	return handler.getArticles();
    }

    public List<PubmedArticle> extractCitationInfo(File file)
	    throws SAXException, ParserConfigurationException, IOException {
	SAXParserFactory factory = SAXParserFactory.newInstance();
	SAXParser saxParser = factory.newSAXParser();

	PubMedArticleParser handler = new PubMedArticleParser();
	Reader isr = new InputStreamReader(new FileInputStream(file), "UTF-8");
	InputSource is = new InputSource();
	is.setCharacterStream(isr);
	saxParser.parse(is, handler);

	return handler.getArticles();
    }

}
