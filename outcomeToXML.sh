CP=${CP}:lib/bioadi.jar
CP=${CP}:lib/stanford-corenlp-3.3.1.jar
CP=${CP}:lib/stanford-corenlp-3.3.1-models.jar
CP=${CP}:lib/automaton.jar
CP=${CP}:lib/chemspot.jar
CP=${CP}:lib/bioc.jar
CP=${CP}:lib/linnaeus-2.0.jar
CP=${CP}:lib/org.tartarus.snowball.jar
CP=${CP}:lib/pengyifan-pubtator-0.0.3-SNAPSHOT-jar-with-dependencies.jar
CP=${CP}:lib/commons-lang-2.4.jar
CP=${CP}:lib/j-seqalign.jar
CP=${CP}:lib/mallet.jar
CP=${CP}:lib/mallet-deps.jar
CP=${CP}:lib/mysql-connector-java-5.1.38.jar
CP=${CP}:lib/sptoolkit.jar
CP=${CP}:lib/ling.jar
# CP=${CP}:lib/semrep.jar
CP=${CP}:lib/semrepjava.jar
CP=${CP}:lib/translability.jar

java -Xmx64G -cp ${CP} gov.nih.nlm.skr.outcomes.OutcomesToXMLWriter   pubmed1950brat pubmed1950xml
