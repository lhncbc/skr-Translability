package gov.nih.nlm.skr.CZ;

import java.util.List;

public class InterventionResult {
    List<BratConcept> chemList;
    List<BratConcept> semrepList;
    List<BratConcept> GNormList;
    List<BratConcept> mustConceptList;
    List<String> MOAList;

    public InterventionResult(List<BratConcept> clist, List<BratConcept> slist, List<BratConcept> glist) {
	this.chemList = clist;
	this.semrepList = slist;
	this.GNormList = glist;
    }
}
