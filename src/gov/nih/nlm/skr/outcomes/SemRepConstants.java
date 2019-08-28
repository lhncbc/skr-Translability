package gov.nih.nlm.skr.outcomes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Event;
import gov.nih.nlm.ling.sem.RelationArgument;
import gov.nih.nlm.ling.sem.RelationDefinition;
import gov.nih.nlm.ling.sem.SemanticItem;

public class SemRepConstants {
	
	public final static List<String> SEMREP_ENTITY_ABRRVS = Arrays.asList(
			"aapp","acab","aggp","alga","amph","anab","anim","anst","antb","arch",
			"bacs","bact","bdsu","bhvr","biof","bird","blor","bmod","bodm","bpoc","bsoj",
			"carb","celc","celf","cell","cgab","chem","chvs","clas","clnd","comd",
			"diap","drdd","dsyn",
			"eico","elii","emod","emst","enzy","euka",
			"famg","ffas","fish","fndg","fngs","food",
			"genf","gngm","grup",
			"hcro","hlca","hops","horm","humn",
			"imft","inbe","inch","inpo","invt","irda",
			"lbpr","lbtr","lipd",
			"mamm","mbrt","mcha","medd","menp","mobd","moft",
			"neop","nnon","npop","nsba","nusq",
			"ocdi","opco","orch","orga","orgf","orgm","orgt","ortf",
			"patf","phsf","phsu","plnt","podg","popg","prog","pros",
			"rcpt","rept","resd","rich","rnlw",
			"shro","socb","sosy","strd",
			"tisu","tmco","topp",
			"virs","vita","vtbt",
			"acty","amas","bdsy","chvf","clna","cnce","crbs","dora","edac","eehu","enty","evnt",
			"ftcn","geoa","gora","grpa","hcpp","idcn","mnob","mosq","ocac","phob","phpr","qlco",
			"qnco","resa","sbst","spco"
			);
	public final static List<String> SEMREP_ENTITY_TYPES = 
			Arrays.asList("AcquiredAbnormality","Activity","AgeGroup","Alga","AminoAcidSequence",
			"AminoAcidPeptideOrProtein","Amphibian","AnatomicalAbnormality","AnatomicalStructure","Animal",
			"Antibiotic","Archaeon","Bacterium","Behavior","BiologicFunction","BiologicallyActiveSubstance",
			"BiomedicalOccupationOrDiscipline","BiomedicalOrDentalMaterial","Bird","BodyLocationOrRegion",
			"BodyPartOrganOrOrganComponent","BodySpaceOrJunction","BodySubstance","BodySystem","Carbohydrate",
			"CarbohydrateSequence","Cell","CellComponent","CellFunction","CellOrMolecularDysfunction","Chemical",
			"ChemicalViewedFunctionally","ChemicalViewedStructurally","Classification","ClinicalAttribute",
			"ClinicalDrug","ConceptualEntity","CongenitalAbnormality","DailyOrRecreationalActivity","DiagnosticProcedure",
			"DiseaseOrSyndrome","DrugDeliveryDevice","EducationalActivity","Eicosanoid","ElementIonOrIsotope",
			"EmbryonicStructure","Entity","EnvironmentalEffectOfHumans","Enzyme","Event","ExperimentalModelOfDisease",
			"FamilyGroup","Finding","Fish","Food","FullyFormedAnatomicalStructure","FunctionalConcept","Fungus",
			"GeneOrGeneProduct","GeneOrGenome","GeneticFunction","GeographicArea","GovernmentalOrRegulatoryActivity",
			"Group","GroupAttribute","HazardousOrPoisonousSubstance","HealthCareActivity","HealthCareRelatedOrganization",
			"Hormone","Human","Human-causedPhenomenonOrProcess","IdeaOrConcept","ImmunologicFactor",
			"IndicatorReagentOrDiagnosticAid","IndividualBehavior","InjuryOrPoisoning","InorganicChemical",
			"IntellectualProduct","Invertebrate","LaboratoryProcedure","LaboratoryOrTestResult","Language","Lipid",
			"MachineActivity","Mammal","ManufacturedObject","MedicalDevice","MentalProcess","MentalOrBehavioralDysfunction",
			"MolecularBiologyResearchTechnique","MolecularFunction","MolecularSequence","NaturalPhenomenonOrProcess",
			"NeoplasticProcess","NeuroreactiveSubstanceOrBiogenicAmine","NucleicAcidNucleosideOrNucleotide",
			"NucleotideSequence","Object","OccupationOrDiscipline","OccupationalActivity","OrganOrTissueFunction",
			"OrganicChemical","Organism","OrganismAttribute","OrganismFunction","Organization","OrganophosphorusCompound",
			"PathologicFunction","PatientOrDisabledGroup","PharmacologicSubstance","PhenomenonOrProcess","PhysicalObject",
			"PhysiologicFunction","Plant","PopulationGroup","ProfessionalSociety","ProfessionalOrOccupationalGroup",
			"QualitativeConcept","QuantitativeConcept","Receptor","RegulationOrLaw","Reptile","ResearchActivity",
			"ResearchDevice","RickettsiaOrChlamydia","Self-helpOrReliefOrganization","SignOrSymptom","SocialBehavior",
			"SpatialConcept","Steroid","Substance","TemporalConcept","TherapeuticOrPreventiveProcedure","Tissue",
			"Vertebrate","Virus","Vitamin");
	public final static List<String> SEMREP_RELATION_TYPES = Arrays.asList(
						"ADMINISTERED_TO","AFFECTS","ASSOCIATED_WITH","AUGMENTS",
						"CAUSES","COEXISTS_WITH","COMPLICATES","CONVERTS_TO","DIAGNOSES","DISRUPTS",
						"INHIBITS","INTERACTS_WITH","LOCATION_OF","MANIFESTATION_OF","METHOD_OF","OCCURS_IN",
						"PART_OF","PREDISPOSES","PREVENTS","PROCESS_OF","PRODUCES","STIMULATES","TREATS","USES",
						"ISA","PRECEDES","compared_with","same_as","higher_than","lower_than");
	public final static List<String> SEMREP_NEG_RELATION_TYPES = Arrays.asList(
			"NEG_ADMINISTERED_TO","NEG_AFFECTS","NEG_ASSOCIATED_WITH","NEG_AUGMENTS",
			"NEG_CAUSES","NEG_COEXISTS_WITH","NEG_COMPLICATES","NEG_CONVERTS_TO","NEG_DIAGNOSES","NEG_DISRUPTS",
			"NEG_INHIBITS","NEG_INTERACTS_WITH","NEG_LOCATION_OF","NEG_MANIFESTATION_OF","NEG_METHOD_OF","NEG_OCCURS_IN",
			"NEG_PART_OF","NEG_PREDISPOSES","NEG_PREVENTS","NEG_PROCESS_OF","NEG_PRODUCES","NEG_STIMULATES","NEG_TREATS","NEG_USES",
			"NEG_ISA","NEG_PRECEDES","NEG_compared_with","NEG_same_as","NEG_higher_than","NEG_lower_than");
	
	public final static List<String> SEMREP_MODIFICATION_TYPES = Arrays.asList("Negation","Speculation","Factuality","Correctness");
	
	public final static List<String> PUBMED_STOPWORDS = Arrays.asList(
			"a", "about", "again", "all", "almost", "also", "although", "always", "among", "an", "and", 
			"another", "any", "are", "as", "at", "be", "because", "been", "before", "being", "between", 
			"both", "but", "by", "can", "could", "did", "do", "does", "done", "due", "during", "each", 
			"either", "enough", "especially", "etc", "for", "found", "from", "further", "had", "has", 
			"have", "having", "here", "how", "however", "i", "if", "in", "into", "is", "it", "its", "itself",
			"just", "kg", "km", "made", "mainly", "make", "may", "mg", "might", "ml", "mm", "most", "mostly", 
			"must", "nearly", "neither", "no", "nor", "obtained", "of", "often", "on", "our", "overall", 
			"perhaps", "pmid", "quite", "rather", "really", "regarding", "seem", "seen", "several", "should", 
			"show", "showed", "shown", "shows", "significantly", "since", "so", "some", "such", "than", "that", 
			"the", "their", "theirs", "them", "then", "there", "therefore", "these", "they", "this", "those", 
			"through", "thus", "to", "upon", "use", "used", "using", "various", "very", "was", "we", "were", 
			"what", "when", "which", "while", "with", "within", "without", "would"
	); 
	
	public final static List<String> ENGLISH_STOPWORDS = Arrays.asList(
			"a", "an", "and", 
			"are", "as", "at", "be",  "by", "for", "has", "he", "in", "is", "it", "its", "of", "on", "that", 
			"the","was", "were", "will", 
			"with"
	);
	
	public final static Map<String,List<String>> UMLS_SEMANTIC_GROUPS = new HashMap<>();
	

	
	static {
		UMLS_SEMANTIC_GROUPS.put("ACTIVITIES", 
				Arrays.asList("Behavior","DailyOrRecreationalActivity","Event","GovernmentalOrRegulatoryActivity",
						"IndividualBehavior","MachineActivity","OccupationalActivity","SocialBehavior"));
		UMLS_SEMANTIC_GROUPS.put("ANATOMY", Arrays.asList("BodyLocationOrRegion","AnatomicalStructure","BodyPartOrganOrOrganComponent",
				"BodySpaceOrJunction","BodySubstance","BodySystem","Cell","CellComponent","EmbryonicStructure",
				"FullyFormedAnatomicalStructure","Tissue"));
		UMLS_SEMANTIC_GROUPS.put("CHEMICALS", Arrays.asList("Antibiotic","BiologicallyActiveSubstance","BiomedicalOrDentalMaterial",
				"Carbohydrate","Chemical","ChemicalViewedFunctionally","ChemicalViewedStructurally","ClinicalDrug","Eicosanoid",
				"ElementIonOrIsotope","HazardousOrPoisonousSubstance","Hormone","ImmunologicFactor","IndicatorReagentOrDiagnosticAid",
				"InorganicChemical","Lipid","NeuroactiveSubstanceOrBiogenicAmine","OrganicChemical","Organophosphorus Compound",
				"PharmacologicSubstance","Receptor","Steroid","Vitamin",
				"AminoAcidProteinOrProtein","Enzyme","NucleicAcidNucleosideOrNucleotide"));
		UMLS_SEMANTIC_GROUPS.put("CONCEPTS", Arrays.asList("Classification","ConceptualEntity","FunctionalConcept",
				"GroupAttribute","IdeaOrCOncept","IntellectualProduct","Language","QualitativeConcept",
				"QuantitativeConcept","RegulationOrLaw","SpatialConcept","TemporalConcept"));
		UMLS_SEMANTIC_GROUPS.put("DEVICES", Arrays.asList("DrugDeliveryDevice","MedicalDevice","ResearchDevice"));
		UMLS_SEMANTIC_GROUPS.put("DISORDERS", Arrays.asList("AcquiredAbnormality","AnatomicalAbnormality",
				"CellOrMolecularDysfunction","CongenitalAbnormality","DiseaseOrSyndrome","ExperimentalModelOfDisease",
				"Finding","InjuryOrPoisoning","MentalOrBehavioralDysfunction","NeoplasticProcess",
						"PathologicFunction","SignOrSymptom"));
		UMLS_SEMANTIC_GROUPS.put("GENES", Arrays.asList("AminoAcidSequence","CarbohydrateSequence","GeneOrGenome",
				"MolecularSequence","NucleotideSequence"));
		UMLS_SEMANTIC_GROUPS.put("GEOGRAPHY", Arrays.asList("GeographicArea"));		
		UMLS_SEMANTIC_GROUPS.put("LIVING", Arrays.asList("AgeGroup","Amphibian","Animal","Archaeon","Bacterium","Bird",
				"Eukaryote","FamilyGroup","Fish","Fungus","Group","Human","Mammal","Organism","PatientOrDisabledGroup",
				"Plant","PopulationGroup","ProfessionalOrOccupationalGroup","Reptile","Vertebrate","Virus"));
		UMLS_SEMANTIC_GROUPS.put("OBJECTS", Arrays.asList("Entity","Food","ManufacturedObject","PhysicalObject","Substance"));
		UMLS_SEMANTIC_GROUPS.put("OCCUPATIONS", Arrays.asList("BiomedicalOccupationOrDiscipline","OccupationOrDiscipline"));
		UMLS_SEMANTIC_GROUPS.put("ORGANIZATIONS", Arrays.asList("HealthCareRelatedOrganization","Organization",
				"ProfessionalSociety","SelfHelpOrReliefOrganization"));
		UMLS_SEMANTIC_GROUPS.put("PHENOMENA", Arrays.asList("BiologicFunction","EnvironmentalEffectOfHumans",
				"HumanCausedPhenomenonOrProcess","LaboratoryOrTestResult","NaturalPhenomenonOrProcess","PhenomenonOrProcess"));
		UMLS_SEMANTIC_GROUPS.put("PHYSIOLOGY", Arrays.asList("CellFunction","ClinicalAttribute","GeneticFunction",
				"MentalProcess","MolecularFunction","OrganismAttribute","OrganismFunction","OrganOrTissueFunction",
				"PhysiologicFunction"));
		UMLS_SEMANTIC_GROUPS.put("PROCEDURES", Arrays.asList("DiagnosticProcedure","EducationalActivity","HealthCareActivity",
				"LaboratoryProcedure","MolecularBiologyResearchTechnique","ResearchActivity","TherapeuticOrPreventiveProcedure"));
	
		UMLS_SEMANTIC_GROUPS.put("ALTERNATE_DISORDERS", Arrays.asList("AcquiredAbnormality","AnatomicalAbnormality",
				"CellOrMolecularDysfunction","CongenitalAbnormality","DiseaseOrSyndrome","ExperimentalModelOfDisease",
				"InjuryOrPoisoning","MentalOrBehavioralDysfunction","NeoplasticProcess",
						"PathologicFunction","SignOrSymptom","Virus"));
	}
	
	public static Set<RelationDefinition> loadSemRepDefinitions() {
		Set<RelationDefinition> relDefs = new HashSet<RelationDefinition>();
		Map<Class<? extends SemanticItem>,Set<String>> entityMap= new HashMap<Class<? extends SemanticItem>,Set<String>>();
		Map<Class<? extends SemanticItem>,Set<String>> eventMap = new HashMap<Class<? extends SemanticItem>,Set<String>>();		
		entityMap.put(Entity.class, new HashSet<String>(SEMREP_ENTITY_ABRRVS));
		eventMap.put(Event.class, new HashSet<String>(SEMREP_RELATION_TYPES));
		// This clearly does not take into account actual semantic type restrictions
		for (String relType: SEMREP_RELATION_TYPES) {
			relDefs.add(new RelationDefinition(relType,relType,Event.class,
					Arrays.asList(new RelationArgument("Subject",entityMap,false),new RelationArgument("Object",entityMap,false)),
					null));
		}
		for (String relType: SEMREP_NEG_RELATION_TYPES) {
			relDefs.add(new RelationDefinition(relType,relType,Event.class,
					Arrays.asList(new RelationArgument("Subject",entityMap,false),new RelationArgument("Object",entityMap,false)),
					null));
		}
/*		for (String modType: SEMREP_MODIFICATION_TYPES) {
			relDefs.add(new RelationDefinition(modType,modType,EventModification.class,
					Arrays.asList(new RelationArgument("Relation",eventMap,false)),
					null));
		}*/
		return relDefs;
	}
	
	public static String getSemGroup(String semtype) {
		for (String group: UMLS_SEMANTIC_GROUPS.keySet()) {
			if (UMLS_SEMANTIC_GROUPS.get(group).contains(semtype)) return group;
		}
		return null;
	}
}