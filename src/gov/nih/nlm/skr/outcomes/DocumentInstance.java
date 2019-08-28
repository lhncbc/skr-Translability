package gov.nih.nlm.skr.outcomes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.util.attr.Attributes;
import gov.nih.nlm.util.attr.HasAttributes;
import nu.xom.Attribute;
import nu.xom.Element;

public class DocumentInstance implements HasAttributes {
	
	public enum OutcomePolarity {
		POS, NEG, MIXED, OTHER, NNEG, NPOS
	}
	
	private String id;
	private Document document;
	private OutcomePolarity polarity;
	private Map<String,String> metaDataMap;
	private Attributes attrs;
	private List<Span> context;
	
	public DocumentInstance(String id, Document doc, OutcomePolarity sentiment) {
		this.id = id;
		this.document = doc;
		this.polarity = sentiment;
	}	
	
	public void setAttrs(Attributes attrs) {
		this.attrs = attrs;
	}

	public void setContext(List<Span> context) {
		this.context = context;
	}

	public Attributes getAttrs() {
		return attrs;
	}

	public List<Span> getContext() {
		return context;
	}
	
	public Element toXml() {
		Element el = new Element("DocumentInstance");
		el.addAttribute(new Attribute("xml:space", 
		          "http://www.w3.org/XML/1998/namespace", "preserve"));
		el.addAttribute(new Attribute("id",document.getId()));
		el.addAttribute(new Attribute("type","DocumentInstance"));
		if (document != null) 
			el.addAttribute(new Attribute("document",document.getId()));
		if (polarity != null) 
			el.addAttribute(new Attribute("polarity",polarity.toString()));
		
		return el;
	}
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Document getDocument() {
		return document;
	}
	
	public void setDocument(Document document) {
		this.document = document;
	}
	
	public OutcomePolarity getPolarity() {
		return polarity;
	}

	public void setPolarity(OutcomePolarity polarity) {
		this.polarity = polarity;
	}
	
	@Override
	public int hashCode() {
		return 
	    ((id == null ? 89 : id.hashCode()) ^
	     (document == null ? 139: document.hashCode()));
	}
	
	/**
	 * Equality on the basis of type and mention equality.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		DocumentInstance at = (DocumentInstance)obj;
		return (id.equals(at.getId()) &&
				document.equals(at.getDocument()));
	}
	
	public Map<String, String> getMetaDataMap() {
		return metaDataMap;
	}
	public void setMetaDataMap(Map<String, String> metadataMap) {
		this.metaDataMap = metadataMap;
	}
	
/*	public void addMetaData(String key, String value) {
		if (metaDataMap == null) metaDataMap = new HashMap<String,String>();
		metaDataMap.put(key, value);
	}*/
	
	public void setMetaData(String key, String value) {
		if (metaDataMap == null) metaDataMap = new HashMap<String,String>();
		metaDataMap.put(key, value);
	}
	
	public String getMetaData(String key) {
		return metaDataMap.get(key);
	}
	
	  public Attributes getAttributes() {
		    if (attrs == null) {
		      attrs = new Attributes();
		    }
		    return attrs;
		  }
	  
	  
	  public String toString() {
		  StringBuffer buf = new StringBuffer();
		  buf.append(getDocument().getId() + "_" + getId() + "_" + polarity.toString() + "\n");
		  if (context == null) return buf.toString();
		  for (Span s: context) {
			  buf.append("\tCONTEXT:" + getDocument().getStringInSpan(s) + "\n");
		  }
		  return buf.toString();
	  }

}
