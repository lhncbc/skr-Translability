README for Caroline project

Date: July 20 2019
Author: Dongwook Shin

   
1. Build the system using ant
   a. Run the following command "ant main -f build_translability.xml"
   b. It will generate a jar file "lib/translability.jar" that includes the Java files you need
   c. Another jar files are already in the lib directory that are necessary to execute Translability packages

2. Update config.properties file properly.The system uses a database and the dbname/account needs to be set properly.
   One limitation of this setting is that it needs Chemical Server to generate Intervention. At the time of creating this document,
   an NLM Chem server is used, which is not accessible to outside user. 

3. Untar CRF package, CRF.tar.gz and save the result to the current directory. The CRF package is written in C++ and linked to the main package using Java Native Interface (JNI).

4. Creating SemRep file for the PubMed citations
   a. First create a file for the citations in MEDLINE format
   b. Run the Java program gov.nih.nlm.skr.CZ.Medline2NormedInput.java 
	input: Medline file
	output: normed format of the file which is composed of:
			PMID- 1234567
			TI  - title'\n'abstract
	The reason that you need to convert MEDLINE format to Mormed format is to let the 
	Scheduler generate the PMID in the full-fledged SemRep output. At the same time, the following steps can accurately 
	compute the offsets of terms using the offset provided in SemRep output.
   c. Submit the Normed file to the Scheculer using Batch Generic mode https://ii.nlm.nih.gov/Batch/UTS_Required/genericWithValidation.shtml
      Use the following command "semrep -FR -Z2015", which generates the Syntactic unit for each sentence as well as full-fledged semrep output
      
 4. Generating Brat files from SemRep output that is created from the previous step
    a. Run the script SemRepToBrat.sh. You probably need to modify four arguments:
       - The first argument is the Semrep output created in the previous step
       - The second argument is the normed format of PubMed citations
       - The third argument is the directory where the brat files are created. In this output directory,
         each citation has two files (one for annotation ( *.ann) and another for text file (*.txt))
    b. There is a sample directory ./pubmed1950test you can test by running SemRepToBrat.sh
         
  5. Run gov.nih.nlm.skr.CZ.GenerateOutput.java that runs species/model, Gene and Intervention, but except Outcome
     a. The first argument is the brat directory generated in step 4
     b. The second argument is the output directory if the third argument is given as "FILE"
     c. The third argument is the indicator if the output is saved as file or database. If the output is loaded into database, it is given as "DB", otherwise "FILE".
     There is a sample script GenerateSMGI.sh that runs this step. 
     * Please note that unless you have chem server run correctly, this step is not running correctly.

  6. Run gov.nih.nlm.skr.outcomes.OutcomesToXMLWriter. It generatesd XML files with which OutcomePrediction generates Outcome in Step 7. 
     Modify a sample shell script outcomeToXML.sh.
     a. The first argument is the Brat files generated in Step 4
     b. The second argument is the XML output directory, which is used in Step 7.
     A sample script outcomeToXML.sh runs this step.
     
  7. Run gov.nih.nlm.skr.outcomes.ml.OutcomePrediction. The output is generated into the database. A sample shell script outcomePredict.sh is provided.
     
