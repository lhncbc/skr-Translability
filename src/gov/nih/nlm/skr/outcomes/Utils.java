package gov.nih.nlm.skr.outcomes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.ContiguousLexeme;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.MultiWordLexeme;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.WordLexeme;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLEventReader;
import gov.nih.nlm.ling.io.XMLModificationReader;
import gov.nih.nlm.ling.io.XMLPredicateReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Event;
import gov.nih.nlm.ling.sem.Indicator;
import gov.nih.nlm.ling.sem.Modification;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Sense;
import gov.nih.nlm.ling.util.FileUtils;


/** 
 * A collection of utility methods for dealing with file IO.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Utils {
	private static Logger log = Logger.getLogger(Utils.class.getName());
	
	public static final List<String> ENTITY_TYPES = Arrays.asList("OverallOutcome");
	public static final List<String> MOD_TYPES = Arrays.asList("OverallOutcome");
	
	public static Map<String,String> loadNormalizedSectionLabels(String filename) throws IOException {
			Map<String,String> normalizedSectionLabels = new HashMap<String,String>();
			List<String> lines = FileUtils.linesFromFile(filename, "UTF-8");
			for (String line: lines) {
				String[] els = line.split("[|]");
				normalizedSectionLabels.put(els[0], els[1]);
			}
			return normalizedSectionLabels;
		}
	
	public static  LinkedHashSet<Indicator> loadSentimentDictionary(String filename) throws IOException {
			 LinkedHashSet<Indicator>dictionary = new LinkedHashSet<>();
		final List<String> lines = FileUtils.linesFromFile(filename, "UTF8");
		for (String l: lines) {
//			System.out.println("LINE:" + l);
			if (l.startsWith("#")) continue;
			String[] els = l.split("\t");
			String text = els[0];
			String pos = els[1];
			String cat = els[2];
			String[] textsubs = text.split("[ ]+");
			String[] possubs = pos.split("[ ]+");
			List<WordLexeme> lexs = new ArrayList<>();
			for (int i=0; i < textsubs.length; i++) {
				lexs.add(new WordLexeme(textsubs[i],possubs[i]));
			}
			List<ContiguousLexeme> indlexs = new ArrayList<>();
			indlexs.add(new MultiWordLexeme(lexs));
			
			Indicator ind = new Indicator(text,indlexs,true,Arrays.asList(new Sense(cat)));
			dictionary.add(ind);
		}
		return dictionary;
	}
	
	public static LinkedHashSet<String> loadClinicalTermDictionary(String filename) throws IOException {
		LinkedHashSet<String> clinicalTerms = new LinkedHashSet<>();
		final List<String> lines = FileUtils.linesFromFile(filename, "UTF8");
		for (String l: lines) {
			if (l.startsWith("#")) continue;
			clinicalTerms.add(l.trim());
		}
		return clinicalTerms;
	}
	
	public static Map<Class<? extends SemanticItem>,List<String>> getAnnotationTypes() {
		Map<Class<? extends SemanticItem>,List<String>> annTypes = new HashMap<>();
		List<String> entityTypes = new ArrayList<>(ENTITY_TYPES);
		entityTypes.addAll(SemRepConstants.SEMREP_ENTITY_TYPES);
		annTypes.put(Entity.class,entityTypes);
		annTypes.put(Modification.class, MOD_TYPES);
		return annTypes;
	}
	
	public static XMLReader getXMLReader() {
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Entity.class, new XMLEntityReader());
		reader.addAnnotationReader(Predicate.class, new XMLPredicateReader());
		reader.addAnnotationReader(Event.class, new XMLEventReader());
		reader.addAnnotationReader(Modification.class,new XMLModificationReader());
		return reader;
	}
	
	  public static List<Span> getContext(DocumentInstance m) {
		  List<Span> context = new ArrayList<>();
		  Document doc = m.getDocument();
		  Span sp = getTitleSpan(doc);
		  List<Sentence> sents = doc.getSentences();
		  for (Sentence sent : sents) {
			  if (Span.subsume(sp,sent.getSpan())) {
				  context.add(sent.getSpan());
				  break;
			  }
			  context.add(sent.getSpan());
		  }
		  int cnt = 0;
		  for (int i=sents.size()-1; i > context.size(); i--) {
			  Sentence sent = sents.get(i);
			  if (cnt == 2) break;
			  if (sent.getText().toLowerCase().startsWith("funding:") || sent.getText().toLowerCase().startsWith("trial registration:")) continue;
			  context.add(sents.get(i).getSpan());
			  cnt++;
			  if (sent.getText().toLowerCase().startsWith("discussion:") || sent.getText().toLowerCase().startsWith("interpretation:") 
					  || sent.getText().toLowerCase().startsWith("conclusion:")  || sent.getText().toLowerCase().startsWith("conclusions:")) break;
		  }
		  return context; 
	  }
	  
	  public static Span getTitleSpan(Document doc) {
		  int newlineInd = doc.getText().indexOf("\n");
		  return new Span(0,newlineInd);
	  }

}
