package gov.nih.nlm.skr.CZ;

public class BratPredication {
	BratConcept subjConcept;
	BratConcept predConcept;
	BratConcept objConcept;
	
	public BratPredication(BratConcept sconcept, BratConcept pconcept, BratConcept oconcept) {
		subjConcept = sconcept;
		predConcept = pconcept;
		objConcept = oconcept;
	}
}
