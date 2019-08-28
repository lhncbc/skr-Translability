#!/usr/bin/perl

# Insert SemRep predications to the Semantic Medline db.
# Input file should have full-fielded output format (-F).
# dbname is currently semmed2006.

use DBI;

$dbname = shift @ARGV;
chomp $dbname;
$input_file = shift @ARGV;
chomp $input_file;
$account = shift @ARGV;
chomp $account;
$password = shift @ARGV;
chomp $password;
$section_header_file = shift @ARGV;

chomp $section_header_file;

$type = "semrep";

$dbh = DBI->connect("dbi:mysql:database=" . $dbname . ";host=indsrv2:3306;user=" . $account  .  ";password="  . $password) or die "Couldn't connect to database: " . DBI->errstr;
$insert_sentence_sth = $dbh->prepare_cached('INSERT INTO SENTENCE (PMID,TYPE,NUMBER,SECTION_HEADER,NORMALIZED_SECTION_HEADER,SENTENCE) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE SENTENCE=?') or die "Couldn't prepare statement: " . $dbh->errstr;
$insert_entity_sth = $dbh->prepare_cached('INSERT INTO ENTITY (SENTENCE_ID, CUI, NAME, SEMTYPE, GENE_ID, GENE_NAME, TEXT, SCORE, START_INDEX, END_INDEX) VALUES(?,?,?,?,?,?,?,?,?,?)') or die "Couldn't prepare statement: " . $dbh->errstr;

$insert_predication_sth = $dbh->prepare('INSERT INTO PREDICATION (SENTENCE_ID, PMID, PREDICATE, SUBJECT_CUI, SUBJECT_NAME, SUBJECT_SEMTYPE, OBJECT_CUI,OBJECT_NAME, OBJECT_SEMTYPE) VALUES (?,?,?,?,?,?,?,?,?)');
$insert_predication_aux_sth = $dbh->prepare_cached('INSERT INTO PREDICATION_AUX (PREDICATION_ID, SUBJECT_DIST, SUBJECT_MAXDIST, SUBJECT_START_INDEX, SUBJECT_END_INDEX, SUBJECT_TEXT, SUBJECT_SCORE, INDICATOR_TYPE, PREDICATE_START_INDEX, PREDICATE_END_INDEX, OBJECT_DIST, OBJECT_MAXDIST, OBJECT_START_INDEX, OBJECT_END_INDEX, OBJECT_TEXT, OBJECT_SCORE) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)') or die  "Couldn't prepare statement: " . $dbh->errstr;
$select_sentence_sth = $dbh->prepare_cached('SELECT SENTENCE_ID FROM SENTENCE WHERE PMID=? AND TYPE=? AND NUMBER=?') or die "Couldn't prepare statement: " . $dbh->errstr;
$delete_sentence_sth = $dbh->prepare_cached('DELETE P FROM PREDICATION P, PREDICATION_AUX PA, SENTENCE S WHERE S.PMID=?  AND P.PREDICATION_ID=PA.PREDICATION_ID AND S.SENTENCE_ID=P.SENTENCE_ID') or die "Couldn't prepare statement: " . $dbh->errstr;

$update_sentence_sth = $dbh->prepare_cached('UPDATE SENTENCE S SET SECTION_HEADER=?, NORMALIZED_SECTION_HEADER=? WHERE S.SENTENCE_ID=?') or die "Couldn't prepare statement: " . $dbh->errstr;

$delete_predication_sth = $dbh->prepare_cached('DELETE P FROM PREDICATION P WHERE P.SENTENCE_ID=?') or die "Couldn't prepare statement: " . $dbh->errstr;

$delete_entity_sth = $dbh->prepare_cached('DELETE E FROM ENTITY E WHERE E.SENTENCE_ID=?') or die "Couldn't prepare statement: " . $dbh->errstr;

# $select_pmid_sentence_sth = $dbh->prepare_cached('SELECT COUNT(*) FROM SENTENCE WHERE PMID = ?') or die "Couldn't prepare statement: " . $dbh->errstr;


open(SHF,"<:utf8",$section_header_file) or die("Can not open the $section_header_file!");

# needed for unicode characters
$dbh->do('SET NAMES utf8');
my %shhash;

while(<SHF>) {
    my($line) = $_;

    chomp($line);

    @line_elements = split(/\|/, $line);
    
    $shhash{$line_elements[0]} = $line_elements[1];

}

close(SHF);

open(F,"<:utf8",$input_file) or die("Can not open the $input_file file!");

$num_sentetce_pmid = 0;
while (<F>) {
    my($line) = $_;
    chomp($line);
    @line_elements = split(/\|/, $line);
    if ($line_elements[0] eq "SE") {
      $pmid = $line_elements[1];
      $senttype = $line_elements[3];
      $number = $line_elements[4];
      # ensure that if predications already exist in the database for the pmid, they are removed, as we are probably trying to update the predications
      # unless ($prev_pmid eq $pmid) {
	# $delete_predication_sth->execute($pmid,$type) or die "Couldn't execute statement: " + $delete_predication_sth->errstr;
      # }      	# get the sentence_id in the db for the given sentence.
      	
	$select_sentence_sth->execute($pmid,$senttype,$number) or die "Couldn't execute statement: " + $select_sentence_sth->errstr;
	
        $sentence_id = $select_sentence_sth->fetchrow_array();
        
        # printf "$sentence_id\n";


      # we are looking at the text line. Insert or update the sentence record.
      if ($line_elements[5] eq "text") {
	$sentence = $line_elements[6];
	$section_header = $line_elements[2];
	$norm_section_header = $shhash{$line_elements[2]};
	if(!defined($norm_section_header)) {
	       $norm_section_header = "";
	       # printf "norm_section_header is null\n";
        } 
        
        if($sentence_id) {
        	
        	# $delete_predication_sth->execute($sentence_id)  or die "Couldn't execute statement: " . $sth->errstr;
        	
        	# $delete_entity_sth->execute($sentence_id)  or die "Couldn't execute statement: " . $sth->errstr;
        	
        	# $norm_section_header = $shhash{$line_elements[2]};

        	
        	$update_sentence_sth->execute($section_header,$norm_section_header,$sentence_id) or die "Couldn't execute statement: " . $update_sentence_sth->errstr;
        
        } else {

		# $insert_sentence_sth->execute($pmid,$senttype,$number,$section_header,$sentence,$sentence) or die "Couldn't execute statement: " . $sth->errstr;	 	# $norm_section_header = $shhash{$section_header};
  
	 	$insert_sentence_sth->execute($pmid,$senttype,$number,$section_header,$norm_section_header, $sentence,$sentence) or die "Couldn't execute statement: " . $sth->errstr;
	 }

      }   }

    

}
