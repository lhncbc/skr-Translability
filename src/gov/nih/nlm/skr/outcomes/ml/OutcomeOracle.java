package gov.nih.nlm.skr.outcomes.ml;

import java.util.Map;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.skr.outcomes.DocumentInstance;
import gov.nih.nlm.util.Log;

/**
 * Oracle class for stating the correct class of a given <var>request</var>.
 *
 */
public abstract class OutcomeOracle {
  private static final Log log = new Log(OutcomeOracle.class);

  /**
   * Returns a {@link OutcomeOracle} that derives its binary value from
   * {@link Document}-level meta-data.  If the {@link Document}'s meta-data for
   * the given <var>key</var> matches the given <var>value</var>, then the
   * result will be <code>true</code>.
   */
  public static OutcomeOracle binaryMetaData(final String key,
                                              final String value) {
    return new BinaryMetaDataRequestOracle(key, value);
  }

  /**
   * Returns a {@link OutcomeOracle} that derives its multi-class value from
   * {@link Document}-level meta-data.  Returns the {@link Document}'s meta-data
   * value for the given <var>key</var>.
   */
  public static OutcomeOracle multiclassMetaData(final String key) {
    return new MulticlassMetaDataRequestOracle(key);
  }

  public abstract String classify(DocumentInstance request);
  public abstract boolean isKnown(DocumentInstance request);

}

/**
 * A {@link OutcomeOracle} that derives its binary value from {@link Document}
 * level meta-data.
 *
 * @author Kirk Roberts - kirk.roberts@nih.gov
 */
class BinaryMetaDataRequestOracle extends OutcomeOracle {
  private static final Log log = new Log(BinaryMetaDataRequestOracle.class);

  private final String key;
  private final String value;

  /**
   * Creates a new <code>BinaryMetaDataRequestOracle</code> with the given
   * <var>key</var> and <var>value</var>.  If the {@link Document}'s meta-data
   * for the given <var>key</var> matches the given <var>value</var>, then the
   * result will be <code>true</code>.
   */
  public BinaryMetaDataRequestOracle(final String key, final String value) {
    this.key = key;
    this.value = value;
  }

  /**
   * If the {@link Document}'s meta-data for the provided <var>key</var> matches
   * the provided <var>value</var>, then the result will be <code>true</code>.
   * @throws IllegalStateException If the {@link Document}'s meta-data does not
   *     contain the provided <var>key</var>.
   */
  @Override
  public String classify(final DocumentInstance request) {
//    final Document document = request.getDocument();

    final Map<String,String> metaData = request.getMetaDataMap();
    final String value = metaData.get(key);
    if (value == null) {
      log.severe("Available Meta-Data: {0}", metaData);
    }
    assert value != null : "No request dir in meta-data";

    if (value.equals(this.value)) {
      return "true";
    }
    else {
      return "false";
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isKnown(final DocumentInstance request) {
    return request.getMetaDataMap().containsKey(key);
  }

}

/**
 * A {@link OutcomeOracle} that derives its multi-class value from
 * {@link Document}-level meta-data.
 *
 * @author Kirk Roberts - kirk.roberts@nih.gov
 */
class MulticlassMetaDataRequestOracle extends OutcomeOracle {

  private final String key;

  /**
   * Creates a new <code>MulticlassMetaDataRequestOracle</code> with the given
   * <var>key</var>.  The {@link #classify} method will return the value
   * associated with the given <var>key</var> in the {@link Document}'s
   * meta-data.
   */
  public MulticlassMetaDataRequestOracle(final String key) {
    this.key = key;
  }

  /**
   * Returns the {@link Document}'s meta-data for the provided <var>key</var>.
   * @throws IllegalStateException If the {@link Document}'s meta-data does not
   *     contain the provided <var>key</var>.
   */
  @Override
  public String classify(final DocumentInstance request) {
//    final Document document = request.getDocument();

    final Map<String,String> metaData = request.getMetaDataMap();
    final String value = metaData.get(key);
    assert value != null : "No request dir in meta-data";

    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isKnown(final DocumentInstance request) {
    return request.getMetaDataMap().containsKey(key);
  }

}
