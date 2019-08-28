package gov.nih.nlm.skr.outcomes.ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import gov.nih.nlm.bioscores.process.PorterStemmer;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ml.feature.Feature;
import gov.nih.nlm.ml.feature.FeatureSet;
import gov.nih.nlm.ml.feature.StringFeature;
import gov.nih.nlm.ml.feature.StringSetFeature;
import gov.nih.nlm.skr.outcomes.DocumentInstance;
import gov.nih.nlm.skr.outcomes.SemRepConstants;
import gov.nih.nlm.util.Log;
import gov.nih.nlm.util.Strings;

public class OverallOutcomeFeatures<T extends DocumentInstance> extends FeatureSet<T> {
	private static final Log log = new Log(OverallOutcomeFeatures.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Feature<T,?>> getFeatures() {
		final Set<Feature<T,?>> features = newSet();
		for (final Type type : Type.values()) {
			for (int i=1; i<= 5; i++) {
				features.addAll(getFeatures(type,i));
			}
		}
		return features;
	}

	/**
	 *
	 */
	public Set<Feature<T,?>> getFeatures(final Type type, final int window) {
		final Set<Feature<T,?>> features = newSet();
		return features;
	}

	public enum Type {
		NORMAL {
			public String convert(final SurfaceElement token) {
				return token.getText();
			}
		},

		UNCASED {
			@Override
			public String convert(final SurfaceElement token) {
				return token.getText().toLowerCase();
			}
		},

		STEMMED {
			@Override
			public String convert(final SurfaceElement token) {
				return token.getLemma();
			}
		},

		PORTER {
			@Override
			public String convert(final SurfaceElement token) {
				PorterStemmer stemmer = new PorterStemmer();
				Set<String> stems = new HashSet<>();
				for (Word w: token.toWordList()) {
					for (int i=0; i <w.getText().length(); i++ ) {
						char c = w.getText().toLowerCase().charAt(i);
						stemmer.add(c);
					}
					stemmer.stem();
					stems.add(stemmer.toString());
				}
				return Strings.join(stems, " ");
			}
		},

		UNCASED_WORDS {
			@Override
			public String convert(final SurfaceElement token) {
				if (token instanceof Word) {
					final String rawString = token.getText().toLowerCase();
					if (Strings.containsLetter(rawString)) return rawString;
					return null;
				} else {
					return token.getText().toLowerCase();
				}
			}
		},

		STEMMED_WORDS {
			@Override
			public String convert(final SurfaceElement token) {
				final String rawString = token.getLemma().toLowerCase();
				if (Strings.containsLetter(rawString)) return rawString;
				return null;
			}
		},

		POS {
			@Override
			public String convert(final SurfaceElement token) {
				return token.getPos();
			}
		},

		CAT {
			@Override
			public String convert(final SurfaceElement token) {
				return token.getCategory();
			}
		},

		SEMTYPE {
			@Override
			public String convert(final SurfaceElement token) {
				LinkedHashSet<SemanticItem> sems = token.getSemantics();
				if (sems == null) return null;
				return sems.iterator().next().getType();
			}
		},

		SEMTYPE_STEMMED_NOSTOP {
			@Override
			public String convert(final SurfaceElement token) {
				if (SemRepConstants.PUBMED_STOPWORDS.contains(token.getLemma())) return null;
				if (Strings.isAllPunctuation(token.getText())) return null;
				LinkedHashSet<SemanticItem> sems = token.getSemantics();
				if (sems == null || sems.size() == 0) 
					return STEMMED.convert(token);	
				String type = sems.iterator().next().getType();
				if (SemRepConstants.SEMREP_ENTITY_TYPES.contains(type) == false) 
					return STEMMED.convert(token);
				String semgroup = SemRepConstants.getSemGroup(type);
				if (semgroup.equals("ALTERNATE_DISORDERS")) return "DISORDER";
				return STEMMED.convert(token);
			}
		},

		SEMTYPE_PORTER_NOSTOP {
			@Override
			public String convert(final SurfaceElement token) {
				if (SemRepConstants.PUBMED_STOPWORDS.contains(token.getLemma())) return null;
				if (Strings.isAllPunctuation(token.getText())) return null;
				LinkedHashSet<SemanticItem> sems = token.getSemantics();
				if (sems == null || sems.size() == 0) 
					return PORTER.convert(token);	
				String type = sems.iterator().next().getType();
				if (SemRepConstants.SEMREP_ENTITY_TYPES.contains(type) == false) 
					return PORTER.convert(token);
				String semgroup = SemRepConstants.getSemGroup(type);
				if (semgroup != null)
					if (semgroup.equals("ALTERNATE_DISORDERS")) return "DISORDER";
				return PORTER.convert(token);
			}
		},

		ORTHO {
			@Override
			public String convert(final SurfaceElement token) {
				String t = token.getText();
				if (Strings.isInitCap(t)) return "INITCAP";
				else if (Strings.isAllUpperCase(t)) return "ALLCAPS";
				else if (Strings.isAllLowerCase(t)) return "LOWERCASE";
				else return "MIXED";
			}
		};
		//GENERIC {
		//  @Override
		//  public String convert(final Token token) {
		//    return token.asRawString().replaceAll("\\d", "0");
		//  }
		//},

		//GENERIC_UNCASED {
		//  @Override
		//  public String convert(final Token token) {
		//    return token.asRawString().replaceAll("\\d", "0").toLowerCase();
		//  }
		//},

		//GENERIC_STEMMED {
		//  @Override
		//  public String convert(final Token token) {
		//    assert token.getStem() != null : "No stem: " + token;
		//    return token.getStem().replace(" ", "_").replaceAll("\\d", "0");
		//  }
		//},

		//HEAD {
		//  @Override
		//  public String convert(final Token token) {
		//    throw new UnsupportedOperationException();
		//  }
		//};

		/**
		 * Converts the given {@link SurfaceElement} to a <code>String</code>.  A
		 * <code>null</code> value indicates the <var>token</var> should not be
		 * included.
		 */
		public abstract String convert(SurfaceElement token);
	}

	private class ContextNGramFeature extends StringSetFeature<T> {
		private final int n;
		private final Type type;
		protected ContextNGramFeature(final String name,
				final int n,
				final Type type) {
			super(name);
			this.n = n;
			this.type = type;
		}
		@Override
		public Set<String> compute(final T span) {
			final List<Span> context = span.getContext();
			List<SurfaceElement> surfs = new ArrayList<>();
			for (Span sp: context) {
				surfs.addAll(span.getDocument().getSurfaceElementsInSpan(sp));
			}
			final Set<String> grams = new TreeSet<String>();
			//			      ngrams(sentence, n, type, grams);
			surfaceElementNgrams(surfs, span,n, type, grams);
			log.fine("{0} returning {1}", getName(), grams);
			return grams;
		}
	}
	
	public static void surfaceElementNgrams(final List<SurfaceElement> surfaceElements, final DocumentInstance cm,
			final int n,
			final Type type,
			final Collection<String> grams) {
		for (int i = 0; i <= surfaceElements.size() - n; i++) {
			final List<String> words = new ArrayList<String>(n);
			for (int j = i; j < i + n; j++) {
				final SurfaceElement token = surfaceElements.get(j);
				final String word = type.convert(token);
				if (word == null) {
					break;
				}
				else {
					words.add(word);
				}
			}
			if (words.size() == n) {
				grams.add(Strings.join(words, "__"));
			}
		}
	}
	
	public class ContextUnigramFeature extends ContextNGramFeature {
		public ContextUnigramFeature(final Type type) {
			super("ContextUnigram(" + type.toString().toLowerCase() + ")", 1, type);
		}
	}

	public class ContextBigramFeature extends ContextNGramFeature {
		public ContextBigramFeature(final Type type) {
			super("ContextBigram(" + type.toString().toLowerCase() + ")", 2, type);
		}
	}

	public class ContextTrigramFeature extends ContextNGramFeature {
		public ContextTrigramFeature(final Type type) {
			super("ContextTrigram(" + type.toString().toLowerCase() + ")", 3, type);
		}
	}
	
	private class ChangePhraseNGramFeature extends StringSetFeature<T> {
		private final int n;
		private final Type type;
		protected ChangePhraseNGramFeature(final String name, final int n, final Type type) {
			super(name);
			this.n = n;
			this.type = type;
		}
		@Override
		public Set<String> compute(final T span) {
			final Set<String> grams = new TreeSet<String>();
			changePhraseNgrams(span, n, type, grams);
			log.fine("{0} returning {1}", getName(), grams);	
			return grams;
		}
	}
	
	private static void changePhraseNgrams(final DocumentInstance text, final int n, final Type type, final Collection<String> grams) {
		List<Span> context = text.getContext();
		Document doc = text.getDocument();
		List<String> contextgrams = new ArrayList<>();
		for (Span sp: context) {
			List<SurfaceElement> surfs = doc.getSurfaceElementsInSpan(sp);
			List<String> wordsApp = new ArrayList<>();
			boolean more = false;
			boolean less = false;
			for (SurfaceElement surf: surfs) {	
				LinkedHashSet<SemanticItem> lessSems = Document.getSemanticItemsByClassTypeSpan(doc, Predicate.class, Arrays.asList("LESS"), surf.getSpan(), false);
				LinkedHashSet<SemanticItem> moreSems = Document.getSemanticItemsByClassTypeSpan(doc, Predicate.class, Arrays.asList("MORE"), surf.getSpan(), false);
				String stemmed = type.convert(surf);
				if ((lessSems == null || lessSems.size() == 0) && (moreSems == null || moreSems.size() == 0))  {
					if (Strings.isAllPunctuation(surf.getText())) {
						more = false;  less = false;
					} else {
						if (stemmed != null) {
							if (more && !less) {
								wordsApp.add(stemmed + "_MORE");
							} else if (less && !more) {
								wordsApp.add(stemmed + "_LESS");
							} else 
								wordsApp.add(stemmed);
						}
					}
				}
				else {
					if (lessSems != null && lessSems.size() > 0) {
						less = true;
						more = false;
						wordsApp.add(stemmed + "_LESS");
					} else if (moreSems != null && moreSems.size() > 0) {
						more = true;
						less= false;
						wordsApp.add(stemmed + "_MORE");
					}
				}
			}
			stringNgrams(wordsApp,n,contextgrams);
			grams.addAll(contextgrams);
		}
	}
	
	public static void stringNgrams(final List<String> words, 
			final int n,
			final Collection<String> grams) {
		for (int i = 0; i <= words.size() - n; i++) {
			final List<String> ngrams = new ArrayList<String>(n);
			for (int j = i; j < i + n; j++) {
				final String word = words.get(j);
				if (word == null) {
					break;
				}
				else {
					ngrams.add(word);
				}
			}
			if (ngrams.size() == n) {
				grams.add(Strings.join(ngrams, "__"));
			}
		}
	}

	public class ChangePhraseUnigramFeature extends ChangePhraseNGramFeature {
		public ChangePhraseUnigramFeature(final Type type) {
			super("ChangePhraseUnigram(" + type.toString().toLowerCase() + ")", 1, type);
		}
	}

	public class ChangePhraseBigramFeature extends ChangePhraseNGramFeature {
		public ChangePhraseBigramFeature(final Type type) {
			super("ChangePhraseBigram(" + type.toString().toLowerCase() + ")", 2, type);
		}
	}

	private  class ChangePhraseFeature extends StringFeature<T> {
		private final String moreOrLess;
		private final String posOrNeg;
		private final int n;
		protected ChangePhraseFeature(final String name, final String moreOrLess, final String posOrNeg, final int n) {
			super(name);
			this.n = n;
			this.moreOrLess = moreOrLess;
			this.posOrNeg = posOrNeg;
		}
		@Override
		public String compute(final T span) {
			List<Span> context = span.getContext();
			Document doc = span.getDocument();
			for (Span sp: context) {
				LinkedHashSet<SemanticItem> sems = Document.getSemanticItemsByClassTypeSpan(doc, Predicate.class, Arrays.asList(moreOrLess), new SpanList(sp), false);
				for (SemanticItem sem: sems) {
					SurfaceElement se = ((Predicate)sem).getSurfaceElement();
					if (elementInWindow(se,n,Arrays.asList(posOrNeg))) return "TRUE";
				}
			}
			return "FALSE";
		}
	}
	
	private boolean elementInWindow(SurfaceElement surf, int windowSize, List<String> types) {
		List<SurfaceElement> windowEls = windowSurfaceElements(surf,windowSize);
		for (SurfaceElement el: windowEls) {
			LinkedHashSet<SemanticItem> sems = el.getSemantics();
			if (sems == null || sems.size() == 0) continue;
			for (SemanticItem sem: sems) {
				String sg = SemRepConstants.getSemGroup(sem.getType());
				if (types.contains(sem.getType()) ||
						(types.contains("NEG") && sg != null && sg.equals("ALTERNATE_DISORDERS"))) 
					return true;
			}
		}
		return false;
	}

	private List<SurfaceElement> windowSurfaceElements(SurfaceElement surf, int windowSize) {
		Sentence sent = surf.getSentence();
		List<SurfaceElement> all = sent.getSurfaceElements();
		int ind = all.indexOf(surf);
		List<SurfaceElement> els = new ArrayList<>();
		for (int i=Math.max(0, ind-windowSize); i<ind; i++) {
			SurfaceElement sel = all.get(i);
			els.add(sel);
		}
		for (int i=ind+1; i<Math.min(all.size(), ind+windowSize); i++) {
			SurfaceElement sel = all.get(i);
			els.add(sel);
		}
		return els;
	}
	
	public class MorePOSFeature extends ChangePhraseFeature {
		public MorePOSFeature(int n ) {
			super("MorePOS(" + n + ")","MORE","POS",n);
		}
	}

	public class MoreNEGFeature extends ChangePhraseFeature {
		public MoreNEGFeature(int n ) {
			super("MoreNEG(" + n + ")","MORE","NEG",n);
		}
	}
	public class LessPOSFeature extends ChangePhraseFeature {
		public LessPOSFeature(int n ) {
			super("LessPOS(" + n + ")","LESS","POS",n);
		}
	}

	public class LessNEGFeature extends ChangePhraseFeature {
		public LessNEGFeature(int n ) {
			super("LessNEG(" + n + ")","LESS","NEG",n);
		}
	}
	
	public class SemTypePresentFeature extends StringFeature<T> {
		private final String type;
		protected SemTypePresentFeature(final String name, final String type) {
			super(name);
			this.type = type;
		}
		@Override
		public String compute(final T span) {
			List<Span> context = span.getContext();
			Document doc = span.getDocument();
			for (Span sp: context) {
				LinkedHashSet<SemanticItem> sems = Document.getSemanticItemsByClassTypeSpan(doc, Entity.class, Arrays.asList(type), new SpanList(sp), false);
				if (sems != null && sems.size() > 0) return "TRUE";
			}
			return "FALSE";
		}
	}

	public class NegationPhraseFeature extends StringSetFeature<T> {
		Type type;
		public NegationPhraseFeature(final Type type) {
			super("NegationPhrase");
			this.type = type;
		}

		@Override
		public Set<String> compute(final T cm) {
			Set<SurfaceElement> negs =  getNegationClues(cm);
			Set<String> els = new HashSet<>();
			for (SurfaceElement n: negs) {
				List<String> phraseEls = getPhraseElements(n,type);
				els.addAll(phraseEls);
			}
			return els;
		}
	}
	
	private Set<SurfaceElement> getNegationClues(DocumentInstance cm) {
		return getClues(cm,Predicate.class,"NEGATE");
	}
	
	private Set<SurfaceElement> getPositiveClues(DocumentInstance d) {
		return getClues(d,Predicate.class,"POS");
	}

	private Set<SurfaceElement> getNegativeClues(DocumentInstance d) {
		return getClues(d,Predicate.class,"NEG");
	}

	
	private static List<String> getPhraseElements(SurfaceElement surf, Type type) {
		Sentence sent = surf.getSentence();

		List<String> els = new ArrayList<>();
		List<SynDependency> syns = SynDependency.inDependenciesWithTypes(surf, sent.getDependencyList(), Arrays.asList("neg","det"), true);
		int sind = sent.getWords().indexOf(surf);
		int maxInd = sind;
		if (sent.getWords().size() > sind + 1) {
			for (SynDependency sd: syns) {
				SurfaceElement head = sd.getGovernor();
				int ind = sent.getWords().indexOf(head.getHead());
				if (ind > maxInd) maxInd = ind;
			}
			for (int i=sind+1;i <= maxInd; i++) {
				els.add(type.convert(sent.getWords().get(i)) + "_NEG");
			}
		}
		return els;
	}
	
	private Set<SurfaceElement> getClues(DocumentInstance d, Class<? extends SemanticItem> clazz, String type) {
		List<Span> context = d.getContext();

		LinkedHashSet<SemanticItem> allClues = Document.getSemanticItemsByClassTypeSpan(d.getDocument(), clazz, Arrays.asList(type),new SpanList(context), false);
		Set<SurfaceElement> clues = new HashSet<>();
		for (SemanticItem tr: allClues) {
			Predicate trp = (Predicate)tr;
			clues.add(trp.getSurfaceElement());
		}
		return clues;
	}
	
}
