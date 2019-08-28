package gov.nih.nlm.skr.outcomes.ml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nih.nlm.ml.Classifier;
import gov.nih.nlm.ml.FeatureExtractor;
import gov.nih.nlm.ml.MulticlassResult;
import gov.nih.nlm.ml.SVMMulti;
import gov.nih.nlm.ml.TrainingListener;
import gov.nih.nlm.ml.feature.Feature;
import gov.nih.nlm.ml.feature.StringFeature;
import gov.nih.nlm.ml.stats.ClassificationStats;
import gov.nih.nlm.ml.svm_multi.LibLinearSVM;
import gov.nih.nlm.skr.outcomes.DocumentInstance;
import gov.nih.nlm.util.Config;
import gov.nih.nlm.util.Log;
import gov.nih.nlm.util.Pair;
import gov.nih.nlm.util.Place;
import gov.nih.nlm.util.Util;

/**
 * Abstract request classifier.
 *
 */
public class OutcomeNEGClassifier extends OutcomeOracle {
  private static final Log log = new Log(OutcomeNEGClassifier.class);

  protected final OutcomeOracle oracle;
  
  private SVMMulti<DocumentInstance> svm;
  private List<Feature<DocumentInstance,?>> features;
  
  /**
   * Creates a new <code>RequestClassifier</code> with the given
   * {@link OutcomeOracle}.
   */  
  public OutcomeNEGClassifier() {
	   oracle = OutcomeOracle.multiclassMetaData("goldNEGOutcome");
	}
  
  public boolean isKnown(final DocumentInstance request) {
	    return request.getMetaDataMap().containsKey("goldNEGOutcome");
	  }
  
  /**
   * Listener for classification results.
   */
  public static abstract class OutcomeNEGClassificationListener {
    public abstract void classification(DocumentInstance text,
                                        MulticlassResult result,
                                        String guess,
                                        String gold);
  }


  private final List<TrainingListener<DocumentInstance>> trainingListeners =
      new ArrayList<>();
  private final List<OutcomeNEGClassificationListener> classificationListeners =
      new ArrayList<>();
  private final StringFeature<DocumentInstance> goldFeature =
      new StringFeature<DocumentInstance>("result") {
    @Override
    public String compute(final DocumentInstance request) {
      return oracle.classify(request).replace(" ", "_");
    }
  };

  /**
   * Returns the gold classification feature.
   */
  protected StringFeature<DocumentInstance> getGoldFeature() {
    return goldFeature;
  }

  public String classify(final DocumentInstance request) {
    return classify2(request).getFirst();
//	  return getClassifier().classify(request);
  }

  /**
   * Same as {@link #classify}, but returns the confidence as well.
   */
  public Pair<String,Double> classify2(final DocumentInstance request) {
    final Pair<String,MulticlassResult> classification = classify3(request);
    final String guess = classification.getFirst();
    final MulticlassResult result = classification.getSecond();

    final boolean binary = guess.equals("true") || guess.equals("false");

    // Listeners
    if (binary) {
      assert classificationListeners.size() == 0 : "Currently no handling " +
          "for ClassificationListeners with the binary classifier";
      
      for (final OutcomeNEGClassificationListener listener : classificationListeners) {
          listener.classification(request, result, guess,
              oracle.isKnown(request) ? goldFeature.get(request) : null);
        }
      
    }
    else {
      for (final OutcomeNEGClassificationListener listener : classificationListeners) {
        listener.classification(request, result, guess,
            oracle.isKnown(request) ? goldFeature.get(request) : null);
      }
    }

    return Pair.of(guess, result.getScore(guess));
  }

  /**
   * Performs the actual classification.
   */
  private Pair<String,MulticlassResult> classify3(final DocumentInstance request) {
    final Classifier<DocumentInstance> classifier = getClassifier();
    final MulticlassResult result = classifier.classifyMulti(request);

    return Pair.of(result.getTopClass(), result);
  }

  public void train(final Iterable<DocumentInstance> requests, List<String> featuresToUse) {
    train(requests, featuresToUse, false);
  }


  public void train(final Iterable<DocumentInstance> requests, List<String> featuresToUse,
                    final boolean quiet) {
    final Classifier<DocumentInstance> classifier = getClassifier();
    classifier.setFeatureValueLog(Config.get(OutcomeNEGClassifier.class, "log").toPlace());
      
//    final FeatureExtractor<DocumentInstance> fe = classifier.extractFeatures();
    final FeatureExtractor<DocumentInstance> fe = classifier.extractSelectFeatures(featuresToUse);

    // Listeners
    if (trainingListeners.isEmpty() == false) {
      if (!quiet) {
        log.info("Informing Listeners...");
      }
      for (final TrainingListener<DocumentInstance> listener : trainingListeners) {
        listener.features(classifier.getFeatures());
        listener.featureExtractionStart();
      }
      for (final DocumentInstance request : requests) {
        for (final TrainingListener<DocumentInstance> listener : trainingListeners) {
          listener.preFeatureExtraction(request);
        }
      }
      for (final TrainingListener<DocumentInstance> listener : trainingListeners) {
        listener.featureExtractionFinish();
      }
    }

    // Feature Extraction
    if (!quiet) {
      log.info("Extracting Features...");
    }
    int numRequests = 0;
    int pos = 0;
    Map<String,Integer> classCounts = new HashMap<>();
    for (final DocumentInstance request : requests) {
      fe.sample(request);
      String type = request.getMetaData("goldNEGOutcome");
      int nc = 0;
      if (classCounts.containsKey(type)) {
    	  nc = classCounts.get(type) + 1;
      } else nc = 1;
      classCounts.put(type, nc);
      numRequests++;
    }
    fe.finish();

    // Training
    if (!quiet) {
      log.info("Training on {0} Requests...", numRequests);
      for (String t: classCounts.keySet()) 
    	  log.info("Training on {0} {1} Requests...", classCounts.get(t),t);
    }
    classifier.train();
   
  }
  
  public ClassificationStats test(final Iterable<DocumentInstance> requests) {
	    final ClassificationStats stats = new ClassificationStats();
	    test(requests, stats);
	    return stats;
	  }
	  /**
	   * Tests the given <var>requests</var>.
	   */
	  public void test(final Iterable<DocumentInstance> requests,
	                   final ClassificationStats stats) {
	    int numRequests = 0;
	    for (final DocumentInstance request : requests) {
	      final String gold = oracle.classify(request).replace(" ", "_");

	      final String guess = classify(request);
	      stats.addInstance(gold, guess);
	      numRequests++;
	    }
	  }

  public ClassificationStats crossValidate(final Iterable<DocumentInstance> requests, List<String> featuresToUse,
                                           final int n) {
    return crossValidate(requests, featuresToUse, n, false);
  }

  public ClassificationStats crossValidate(final Iterable<DocumentInstance> requests, List<String> featuresToUse, 
                                           final int n,
                                           final boolean quiet) {
    final List<DocumentInstance> requestList = Util.toList(requests);

   final ClassificationStats stats = new ClassificationStats();
   double avgTotal = 0.0;
   int correct = 0;
   int total = 0;
    final List<List<DocumentInstance>> folds = Util.splitFolds(requestList, n);
    for (int fold = 0; fold < n; fold++) {
      final List<DocumentInstance> trainSet = new ArrayList<DocumentInstance>();
      final List<DocumentInstance> testSet = new ArrayList<DocumentInstance>();
      for (int i = 0; i < folds.size(); i++) {
        if (i == fold) {
          testSet.addAll(folds.get(i));
        }
        else {
          trainSet.addAll(folds.get(i));
        }
      }

      resetClassifier();
      train(trainSet, featuresToUse, quiet);
      test(testSet, stats);
  	log.DBG("CV " + fold + " seen classes: {0}", stats.classes());
  	for (final String type : stats.classes()) {
  	  log.info("CV Type: {0}", type);
  	  log.info("  Precision: {0}", stats.precision(type));
  	  log.info("  Recall: {0}", stats.recall(type));
  	  log.info("  F1: {0}", stats.f1(type));
  	  log.info("  F5: {0}", stats.f(5.0, type));
  	}
  	log.info("Accuracy: {0}", stats.accuracy());
  	double foldAccuracy = (double)(stats.correct() - correct)/(stats.total()-total);
  	avgTotal += foldAccuracy;
  	correct = stats.correct();
  	total = stats.total();
//  	log.info("Skipped: {0}", stats.getSkipped());
  	log.info("Fold accuracy: {0}", foldAccuracy);
  	log.info("Confusion Matrix:\n{0}", stats.getConfusionMatrix());
    }
  	log.info("Avg. accuracy: {0}", avgTotal/n);

    return stats;
  }


  /**
   * Adds a {@link TrainingListener} to this
   * <code>MachineRequestClassifier</code>.
   */
  public void addTrainingListener(final TrainingListener<DocumentInstance> listener) {
    trainingListeners.add(listener);
  }

  /**
   * Adds a {@link OutcomeClassifiationListener} to this
   * <code>MachineRequestClassifier</code>.
   */
  public void addClassificationListener(final OutcomeNEGClassificationListener listener) {
    classificationListeners.add(listener);
  }
  

	  protected Classifier<DocumentInstance> getClassifier() {
	    if (svm == null) {
	      svm = new LibLinearSVM();
	      svm.setFeatures(getFeatures());
	      svm.setTargetFeature(getGoldFeature());

	      final Place modelFile =
	          Config.get(OutcomeNEGClassifier.class, "model").toPlace();
	      svm.setModelFile(modelFile);
	      svm.setModelPath(modelFile);
	    }
	    return svm;
	  }

	  protected void resetClassifier() {
	    LibLinearSVM.resetRandom();
	    svm = null;
	  }

	  protected void setFeatures(final List<Feature<DocumentInstance,?>> features) {
	    this.features = features;
	    resetClassifier();
	  }

	  public List<Feature<DocumentInstance,?>> getFeatures() {
		 return  OutcomePOSClassifier.NiuFeatures();
	  }
	  
	  protected Double getThreshold() {
	    return 0.0;
	  }
}
