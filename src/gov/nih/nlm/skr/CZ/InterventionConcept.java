package gov.nih.nlm.skr.CZ;
import java.util.List;
public class InterventionConcept  {
	BratConcept bc;
	List interventionList;
	public InterventionConcept(BratConcept bc, List interList) {
		this.bc = bc;
		this.interventionList = interList;
	}
}
