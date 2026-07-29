		       ABAC POLICY MINING FROM LOGS

	 Zhongyuan Xu, Scott D. Stoller, and Christian Hesselbach
			  Stony Brook University
		    Contact: stoller@cs.stonybrook.edu

======================================================================
SYSTEM REQUIREMENTS

The program should compile and run on any system with a Java 6 or newer
compiler and runtime environment.

The shell scripts "compile" and "run", documented below, make it a little
easier to compute and run the program.  The shell scripts work on any
system with a bash shell: Windows/cygwin, linux, solaris, etc.  If your
system does not have a bash shell, you can run the program without the
shell scripts, by copying-and-pasting the commands in the shell scripts
into your command shell (for the run script, replace "$*" with the
appropriate arguments to the shell script, based on the documentation
below).

======================================================================
SYNTAX FOR ABAC POLICIES

an ABAC policy file has extension .abac. it contains rules and attributes
data.  specifically, it contains lines of the following five forms:

userAttrib(uid, attribute1=value1, attribute2=value2, ...)
resourceAttrib(rid, attribute1=value1, attribute2=value2, ...)
rule(ruleName; uae; pae; ops; con)
unremovableUserAttribs(attribute1, attribute2, ...)
unremovableResourceAttribs(attribute1, attribute2, ...)

the first argument of a userAttrib is automatically assigned to an
attribute named "uid".  the first argument of a resourceAttrib is
automatically assigned to an attribute named "rid".  value1, value2,
... are atomic values or sets.  an atomic value is a string that starts
with a character other than a left curly brace.  a set has the form
"{element1 element2 ...}".  note that elements of a set are separated by
spaces, not commas.

uae is a user attribute expression.  it is a conjunction, with the
conjuncts separated by commas.  each conjunct has the form "a in {value1
value2 ...}" where a is a single-valued user attribute, or "a supseteqIn
{set1 set2 ...}"  (meaning that a is a superset of one of the sets in {set1
set2 ...}) where a is a multi-valued user attribute, and set1, set2, ...,
are sets of the form {value1+value2+...}".  note that sets (such as set1,
set2, ...) that are nested inside another set are written with the elements
separated by "+" (instead of " ") to simplify parsing.

rae is a resource attribute expression.  the syntax is analogous to the
syntax for user attribute expressions.

ops is a set of operations.

con is a constraint.  it is a conjunction of atomic constraints, with the
conjuncts separated by commas.  an atomic constraint is a formula of the form
aum > arm, aum ] ars, or aus=ars$,where aus is a single-valued user aum is
a multi-valued user attribute, ars is a single-valued resource attribute,
and arm is a multi-valued resource attribute.  note that > denotes
$\supseteq$ and "]" denotes "\ni".

userAttrib and resourceAttrib statements must precede rule statements.

the sets of users, resources, operations, user attributes, permission
attributes, are not specified explicitly.  they are implicitly defined as
the sets containing the users, permissions, etc., that are mentioned in the
policy.

lines starting with "#" are comments.

the unremovableUserAttribs and unremovableResourceAttribs statements
specify user attributes and resource attributes that \eliminateconjuncts
should not eliminate from the UAE and PAE, respectively.

note: this syntax is the same as for our tool for mining ABAC policies from
ACLs, except that rules have one additional argument, the rule name,
inserted at the beginning.  the ruleName is used in .dist files, described
below.

======================================================================
SYNTAX FOR PROBABILITY DISTRIBUTION FILES

.dist files specify probability distributions used in generation of
operation logs.  the probability distributions are described in
Prelim-report/abac-from-logs/evaluation.tex.  .dist files contain entries
of the following forms:

ruleDist(rid,probability) // specifies an entry in P_rule
userDist(uid,probability) // specifies an entry in P_user
resDist(res,probability)  // specifies an entry in P_res
opDist(operation,probability)  // specificies an entry in P'_op

these four probability distributions are described in the CoRR paper.

======================================================================
SYNTAX FOR LOG FILES

the "log files" are actually log summaries, as described in the Log
Generation section of the paper.  each line contains a user-permission
tuple t and the frequency freq(t) of that user-permission tuple, separated
by a space.  the user-permission tuple contains a unique ID as the first
component and hence has the form <tupleID, userID, resourceID, operation>.

======================================================================
FILES FOR CASE STUDIES AND SYNTHETIC POLICIES

case-studies/ contains .abac and .dist files for case studies with manually
written attribute data.  the .dist files are also manually written.
<jaccard-similarity-of-UP0-and-UP-induced-by-output-policy_avg><jaccard-similarity-of-UP0-and-UP-induced-by-output-policy_stddev>

case-studies-synthetic/ contains two kinds of files:

  <caseStudy>_<Ndept>_<sequenceNum>.abac
    case studies with synthetic attribute data

  <caseStudy>_<Ndept>_<sequenceNum>_<ratio>.dist
    where ratio is ratio of most and least frequent items; this ratio is used
    in the algorithm that generates the .dist file.

logs/case-studies and logs/case-studies-synthetic contains UP frequency
files generated for case studies with manually written attribute data and
synthetic attribute data, respectively.  filenames have the form
<caseStudy>_<completeness>_<sequenceNum>.log, where <completeness> is the
fractional log completeness times 10 (equivalently, the percentage of log
completeness divided by 10).  e.g., healthcare_10_0.log has completeness
100%.  each line in a UP frequency file specifies a user-permission tuple
and its frequency.

for case studies with synthetic attribute data, for each .abac file (i.e.,
each synthetic dataset of a given size), exactly one .dist file is
generated, and exactly one log file with 100% completeness is generated
from that .abac file and the associated .dist file, and then four more log
files with lower completeness (60%, 70%, 80%, 90%) are generated from that
log file with 100% completeness, and all seven of these files have the same
sequenceNum in their name.  in principle, <ratio> should be included in the
name of the log file, but we are currently using only one value of the
ratio (namely, 25), so omitting it is OK for now.  thus, each .abac file
should be used together with 5 log files; specifically, 
case-studies-synthetic/<caseStudy>_<Ndept>_<sequenceNum>.abac
should be used with
logs/case-studies-synthetic/<caseStudy>_<completeness>_<sequenceNum>.log
for <completeness> in {6,7,8,9,10}.

output/case-studies and output/case-studies-synthetic contain output
for case studies with manually written attribute data and synthetic
attribute data, respectively.  in each file, each line contains
<completeness> <syntactic_similarity_avg> <syntactic_similarity_stddev> <over-assignmentRatio_avg> <over-assignmentRatio_stddev> <under-assignmentRatio_avg><under-assignmentRatio_stddev>
<jaccard-similarity-of-UP0-and-induced-up-of-outpput-policy_avg><jaccard-similarity-of-UP0-and-induced-UP-of-output-policy_stddev>

synthetic/ contains .abac and .dist files for synthetic policies.
synthetic_<i>_<Nrule>.abac is the i'th synthetic policy with Nrule rules.
synthetic_<i>_<Nrule>_<ratio>.disc is a .dist file for the i'th synthetic
policy with the specified ratio of frequencies of the most frequent and
least frequent items (see SYNTHETIC LOG GENERATION FOR SYNTHETIC POLICIES
section in notes.txt).

logs/synthetic contains .log files for synthetic policies.
synthetic_<Nrule>_<completeness>_<i>.log is the log with the specified
completeness for the i'th synthetic policy with Nrule rules.  note that,
for each synthetic policy and log completeness, there is exactly one log
for that synthetic policy with that log completeness.  the value of <ratio>
is not recorded in the filename of the log (even though it would be better
if it were there) because currently we are using only one value of the
ratio.  also, it is a little confusing that the parameters appear in
different orders in the filenames (e.g., <i> is at the beginning of the
.abac and .dist filenames and at the end of the .log filenames).

======================================================================
COMMANDS FOR EXPERIMENTS WITH OUR ALGORITHM

To compile the program: ./compile

To run the program, the general format is: ./run [option] arg1 arg2 ...
The options and the corresponding arguments are described below.

For running modes that do not include the "outputFile" option, the output
is written to standard output (i.e., the terminal).

By default, the rule quality metric Qrul is used, and the weights w_o and
w'_o are set as described in the "Over-Assignment Weight" paragraph in
Section 7 of our paper, i.e., as a function of log completeness.

--------------------
Command: 
./run -m inputABACFile.abac inputDistFile.dist ruleQualityMetric

Meaning: run the ABAC policy mining algorithm on case-studies with manually
written attribute data NUMBER_OF_TEST_CASES times, with 5 different log
completeness {0.6, 0.7, 0.8, 0.9, 1.0 }.

Note: NUMBER_OF_TEST_CASES is a constant defined in src/edu/dar/algo/Experiment.java.
It is set to 1.  To run the experiments multiple times, user can change the 
variable NUMBER_OF_TEST_CASES to a larger value. 

Input: inputABACFile.abac, which should contain an ABAC policy, in the
syntax described above, and inputDistFile.dist file which should contain
the distribution information of various components of the input, in the
syntax described above. ruleQualityMetric should be an integer in the range
[0, 10]. Rule quality metrics are defined in src/edu/dar/util/RuleQualityMetricType.java 

Output: the generated log files are
logs/case-studies/inputABACFile_j_n.log, where j ranges from 6 to 10
indicating the completeness, and n ranges from 0 to NUMBER_OF_TEST_CASES -
1.  the output file is output/case-studies/inputABACFile.output

Example: ./run -m case-studies/university.abac case-studies/university.dist 0

This runs the ABAC policy mining algorithm on the university case study
with manually written attribute data using the first rule quality metric.

--------------------
Command:
./run -r caseStudyType N ruleQualityMetric

Meaning: generate NUMBER_OF_TEST_CASES case study policies (i.e., .abac
files) with synthetic attribute data of size N, and generate a .dist file
for each, and run the ABAC mining algorithm on the generated policies and
.dist files with 5 different log completeness {0.6, 0.7, 0.8, 0.9, 1.0 }.
caseStudyType may be university, healthcare, or projectmanagement.  N is
Ndept or Nward, depending on caseStudyType. ruleQualityMetric is the same
as in -m mode.

Output: the generated abac file is case-studies-synthetic/caseStudyType_N_n.abac, 
the generated dist file is case-studies-synthetic/caseStudyType_N_n.dist,
the generated log file is logs/case-studies-synthetic/caseStudyType_N_j_n.log,
the output file is output/case-studies-synthetic/caseStudyType_N.output,
where j ranges from 6 to 10 indicating the completeness, 
and n ranges from 0 to NUMBER_OF_TEST_CASES - 1.

Example: ./run -r university 10 0

This generates synthetic attribute data of size 10 (i.e., 10 departments)
for the university case study, and runs the ABAC policy mining algorithm on
the resulting policies, using the first rule quality metric.



--------------------
Command:
./run -d inputABACFile

Meaning: generate the distribution .dist file in the same folder and name as ABAC file with extension .dist

Output: the generated .dist file in the same folder as inputABACFile

Example: ./run -d case-studies/university.abac

This generates university.dist file in the folder /case-studies/

--------------------
Command:
./run -s N ruleQualityMetric

Meaning: generate NUMBER_OF_TEST_CASES synthetic policies (i.e., .abac
files) with N rules, and generate a .dist file for each, and run the ABAC
mining algorithm on the generated policies and .dist files, with 5
different log completeness {0.6, 0.7, 0.8, 0.9, 1.0 }.

Output: the generated abac file is synthetic/synthetic_n_N.abac,
the generated dist file is synthetic/synthetic_n_N.dist,
the generated log file is logs/synthetic/synthetic_N_j_n.log,
the output file is output/synthetic/synthetic_N.output,
where j ranges from 6 to 10 indicating the completeness, 
and n ranges from 0  to NUMBER_OF_TEST_CASES - 1. ruleQualityMetric 
is the same as in -m mode.

Example: ./run -s 10 0

This generates synthetic policies with 10 rules and runs the ABAC mining
algorithm on them, using the first rule quality metric.

------------------
Command:
./run -o inputABACFile.abac inputDistFile.dist

Meaning: run the ABAC policy mining algorithm on case-studies with manually
written attribute data NUMBER_OF_OVERASSIGNMENT_TEST_CASES times, with 5 different log
completeness {0.6, 0.7, 0.8, 0.9, 1.0 }.

Note: NUMBER_OF_OVERASSIGNMENT_TEST_CASES is a constant defined in src/edu/dar/algo/Experiment.java.
It is set to 2.  To run the experiments multiple times, user can change the 
variable NUMBER_OF_OVERASSIGNMENT_TEST_CASES to a larger value. 

Input: inputABACFile.abac, which should contain an ABAC policy, in the
syntax described above, and inputDistFile.dist file which should contain
the distribution information of various components of the input, in the
syntax described above. 

The input log files are taken from logs/inputABACFile_{Log Completeness No(6,7,8,9,or 10}_{No of Over-Assignment Test Case}.log one after another
to generate the reports.

Output: 

the generated reports are 
overassignment_analysis/reports/case-studies/inputABACFile_j_n.txt where j ranges from 6 to 10
indicating the completeness, and n ranges from 0 to NUMBER_OF_OVERASSIGNMENT_TEST_CASES for all rule quality metrics.

The summary of output is generated in case-studies/output/inputABACFile (taken from 2nd argument) folder with *.output extension.
  
Example: ./run -o ../case-studies/university.abac ../case-studies/university.dist

This runs the ABAC policy mining algorithm on the university case study
with manually written attribute data and generates over assignment reports.



============================================================
FILES FOR EXPERIMENTS WITH PROGOL

The progol input files and their related output files are organized 
as follows:

progol input files:
progol/<case_study>/<basename>.pl

progol output:
progol/<case_study>/proglog/<order>/<basename>.proglog

ABAC rules in our language, obtained by parsing progol's output and
translating the generated rules into our ABAC policy language:
progol/<case_study>/progbac/<basename>.abac

output from the Java program that computes various quality metrics (e.g.,
Jaccard similarity to meaning of original ABAC policy) for the rules that
Progol generated:
progol/<case_study>/quality/<order>.output

running time of progol:
progol/<case_study>/quality/time-<order>.output

<basename> has the form
<casestudy>_<completeness>_<sequenceNumber>-prog-<order>

where <completeness> is the log completeness, and <sequenceNum> is the
sequence number (same sequenceNum as the .abac and .dist files that the log
was generated from), "prog" is short for "Progol", and <order> (if
present) indicates the order in which the up facts are presented to Progol
in the generated .pl file (i.e., Progol program).

when <order> is omitted or equals "orig", the facts are presented in the
order that they appear in the log file.  <order> of "shuf1" or "shuf2"
indicates the order is randomized.  <order> of "low" indicates the facts
are presented in asending order of frequency in the log file.  <order> of
"high" indicates the facts are presented in descending order of frequency
in the log file; this gives the beset results and is used for the graphs in
the paper.

quality-<order>.output contains data in tab-separated columns with the
following meanings (from left to right): 

first two columns: average and std dev of number of users mentioned in the
log

next two columns: average and std dev of number of resources mentioned in
the log

next two columns: average and std dev of number of operations mentioned in
the log

next two columns: average and std dev of number of user-permission tuples
in UP_0 covered by the generated rules

next two columns: average and std dev of syntactic similarity of the
generated and original policies

next two columns: average and std dev of fraction of over-assignments, as
defined in the paper

next two columns: average and std dev of fraction of under-assignments, as
defined in the paper

next two columns: average and std dev of Jaccard similarity of the meanings
of the generated and original policies

The results are reported in ascending order by completeness, and within
each level of completeness, by ascending sequenceNum, so the running time
on the first line is for completeness=6 (i.e., 60%) and sequenceNum=0, the
second line is for completeness=6 (i.e., 60%) and sequenceNum=0, and the
last line is for completeness=10 and sequenceNum=9.

quality/time-<order>.output contains all of the running times for the
Progol runs whose outputs' quality is reported in quality/<order>.output.
The times are reported in the same order as the results in
quality/<order>.output.

======================================================================
COMMANDS FOR EXPERIMENTS WITH PROGOL

To compile the program: ./compile

Download Progol 4.4 from http://www.doc.ic.ac.uk/~shm/Software/progol4.4

Note: If you use Progol 5.0, you need to modify the program to omit
"set(posonly)?", because it causes Progol 5.0 to segmentation fault.

Compile progol.  put the progol executable at ./progol/source/progol
this location is assumed by the scripts described below; it can easily be
changed if desired.

--------------------
Command: 
./run-progol -l inputAbac outputFolder
./run-progol-synth -l inputAbacBase inputLogBase outputFolder

Meaning: convert log summary files to progol input files, using the
original ABAC policy (for the attribute data).  Assumes that the logs are
located at ./logs/case-studies/ and are named in the following format:
<caseStudy>_<completeness>_<sequenceNum>.log where <caseStudy> is the name
of the case study (assumed to be the same as the name of the inputAbac
file), and <completeness> and <sequenceNum> are as described above.  Output
is stored in a subfolder with the same name as inputAbac (but without the
.abac extension) within the supplied outputFolder.

Note: When translating an ABAC policy into progol, in addition to the
transformations described in our paper, two transformations are performed
on literals, to avoid confusing progol's parser: (1) Dashes are replaced
with underscores (e.g., gradebook0-1 is translated as gradebook0_1); and
(2) the letter 'n' is prepended to literals whose first character is a
number (e.g., 0gradebook1 is translated to n0gradebook1).

run-progol should be used for case studies with manual attribute data.
run-progol-synth should be used for case studies with synthetic attribute
data and for synthetic policies.  the difference between these two programs
is the assumptions they make about how the input files (.abac, .dist, and
.log) are organized, consistent with the organization described above in
the FILES FOR CASE STUDIES AND SYNTHETIC POLICIES section.

each run-progol command iterates to process all of the .abac and
.log files for the specified case study.  It assumes there are ten sequence
numbers per completeness level, for completeness levels from 60% to 100% in
steps of 10%.  Thus, each command processes 50 log files.

Similarly, run-progol-synth command iterates to process all of the .abac
and .log files for each sequence and completeness, resulting in 50 
progol input files. The inputAbacBase parameter specifies the 
path and base name or identifier of the .abac files to be processed 
and the inputLogBase parameter specifies the path and base name for 
the .log files.

Example: ./run-progol -l ../case-studies/healthcare.abac ../progol/

This creates progol input files in the folder ./progol/healthcare/

Example: ./run-progol-synth -l ../case-studies-synthetic/university_6 ../logs/case-studies-synthetic/university ../progol/case-studies-synthetic/university/

This creates progol input files in the folder
./progol/case-studies-synthetic/university/ for .abac files such as
university_6_7.abac (a case study with synthetic attribute data).

Example: ./run-progol-synth -l ../synthetic/_20 ../logs/synthetic/synthetic_20 ../progol/synthetic/

This creates progol input files in the folder ./progol/synthetic/ for .abac
files such as synthetic_3_20.abac (a synthetic policy).

--------------------
Command: 
./progol/script-<caseStudy>.sh or  ./progol/script-synthetic.sh

These scripts run progol and generate proglog files, whose format is
described above.  These scripts assume the progol executable is located at
./source/progol, and that the progol input files are in the folders
described in the FILES FOR EXPERIMENTS WITH PROGOL section.  each proglog
file has the same name as the corresponding input file, exept the
extension is .proglog.

--------------------
Command:
./run-progol -a origAbac progolOutputFolder outputFolder [-sortOrder]
./run-progol-synth -a origAbac progolOutputFolder outputFolder [-sortOrder]

Meaning: parse the output files generated by progol to obtain the generated
rules, and create new ABAC files containing the original attribute data and
the progol-generated rules. The sortOrder parameter is optional if there is
no order in the file name. The sortOrder will be placed in the file name
and determines the subfolder for that order in outputFolder.

Example: ./run-progol -a ../case-studies/online-video.abac ../progol/online-video/proglog/ ../progol/online-video/progbac/ -high

This writes the new .abac files to ./progol/online-video/progbac/high/ in
the form of <case-study>_<completeness>_<sequence>-prog-<sortOrder>.abac

Example: ./run-progol-synth -a ./synthetic/synthetic_0_20.abac ../progol/synthetic/proglog/0/synthetic_ ../progol/synthetic/progbac/0/synthetic_0 -high

This writes five new abac files to ./progol/synthetic/progbac/0/ in the
form of <case-study>_<sequence>_<completeness>-prog-<sortOrder>.abac

--------------------
Commmand:
./run-progol -t input outputFile [-sortOrder]
./run-progol-synth -t input outputFile [-sortOrder]

Meaning: Extracts progol's running time from the proglogs.  "input" here
is the relative pathname together with the basename for the .proglog files
to be processed.

Example: ./run-progol -t ../progol/healthcare/proglog/high/healthcare ../progol/healthcare/quality/healthcare-time-high.output -high

Example: ./run-progol-synth -t ../progol/synthetic/proglog/0/synthetic_0 ../progol/synthetic/quality/synthetic_0-time-high.output -high

--------------------
Command:
./run-progol -q origAbac progbacFolder outputFile [-sortOrder] [-o]
./run-progol-synth -q origAbac progbacFolder outputFile [-sortOrder] [-o]

Meaning: Process the generated and original ABAC files and compute quality
metrics including syntactic similarity, fraction of over-assignments,
fraction of under-assignments, and jaccard similarity.  The -o parameter
provides the option to output the coveredUP from the new and original abac
policies to a text file located at
./progol/<case-study>/tmp/<case-study>_<completeness>_<sequenceNum>.coup

Example: ./run-progol -q ../case-studies/project-management.abac ../progol/project-management/progbac/ ../progol/project-management/quality/prog-high.output -high

Example: ./run-progol-synth -q ../synthetic/synthetic_0_20.abac ../progol/synthetic/progbac/0/ ../progol/synthetic/quality/synthetic_0.output -high



============================================================
FILES FOR EXPERIMENTS USING THE AUTHOR-TOPIC MODEL

download topictoolbox from
http://psiexp.ss.uci.edu/research/programs_data/toolbox.htm

Files related exclusively to the author-topic model can be found 
in ./atm, which contains the following:
	atOut
	matABAC
	topictoolbox
	runner.m

runner.m is a simple MATLAB script that runs all 50 input files for 
each case-study for a total of 200 executions.

atOut contains the output returned from running .m files gererated 
by the TopicModel class. The topictoolbox contains the MATLAB scripts 
and associated files necessary to run the generated .m files.

TopicModel will output .m and .output files directly into the atm 
folder. It is recommended to organize them into their own folder upon 
creation as seen in the input_2atom folder. Like TopicModel, the output 
files from the generated .m's will also dump their results into atOut to 
be organized later.

.output files created by TopicModel contain information on their 
associated .m file, such as the number of UAEs, RAEs, constraints, etc. 
These are all labeled within the file itself.

There are additional files residing in atOut that are there only due to 
their content.

	Files containing the suffix "MAT_times.txt" are the copy-pasted 
	output from MATLAB's command-line. They are unlabled, but their 
	order matches that found in runner.m, which happens to be 
	alphabetical in this instance.
	
	Files with "MAT_simplified.txt" contain the extracted times from 
	their "MAT_times" file. This is generated by TopicModel and once 
	again is unlabled but matches the original order.
	
	"summary.output" files contain extracted information from the
	generated .output files. This is also generated by TopicModel.  The
	information is unlabled and designed to be easily pasted into
	Excel. Currently the first column contains the number of authors in
	the .m file and the second contains the time taken to create it. As
	with the previous two, the order is alphabetical.

matABAC contains ABAC policies manually created based on MATLAB's output.
the .output files in this folder contain the quality information obtained
using "TopicModel -q" to compare these ABAC policies with the original ABAC
policies. The quality .output files are labled and contain the following 
information on a tab-separated line:
Operations, CoveredUP, Syntactic Similarity, Overassignment Ratio, 
Underassignment Ratio, and Jaccard Similarity.
	
============================================================
COMMANDS FOR EXPERIMENTS USING THE AUTHOR-TOPIC MODEL

the "java TopicModel" commands below should not be executed directly.
they should be executed using the ./run-atm script.
Example: for the first command: ./run-atm -m[l] abacfile ...

--------------------
Command: 
java TopicModel -m[l] abacfile logfile outputname numTopics alpha beta numIter seed verbosity [-id -tp -atoms]

Generates a .m and .output file based on the abac and log files.

Using the -ml argument will loop over 50 log files for the specified case
study and generate everything as a batch. The parameters stay the same.

Optional flags:
	-id: Blocks uid and rid attributes from entering the UAE and RAE
	
	-tp: "Two plus", forces all uae, rae, and constraints to be checked 
		for whether there are two users/resources that are satisfied 
		by them
	
	-atoms: Blocks constraints made up of two atomic constraints

Example: java TopicModel -ml ../case-studies/online-video.abac ../logs/case-studies/new/online-video_6_0_new.log ../atm/online_video_6_0 6 1 0.01 10 3 0 -id -tp -atoms

This writes 50 .m and .output files (in the current directory) for the
online-video case study and apply all three constraints. Changing the
argument to "-m" will create a single .m and .output for the
online-video_6_0 log.

--------------------
Commands for Extracting Time Info: 
	
java TopicModel -to inputFolder outputname [basename]

Writes an output file containing the extracted information from 
all generated .output files in inputFolder. A basename can be provided 
to place only the information for all the outputs matching that name 
into the file otherwise all .output files will be processed.

Example: -to ../atm/input_1atom/ ../atm/input_1atom_summary.output


java TopicModel -tr inputfile outputfile

Takes a text file containing the copy-pasted time information from 
MATLAB and extracts just the times into a single file. MATLAB displays 
time in seconds, this method converts the time to milliseconds.

Example: -tr ../atm/atOut/1atom_MAT_times.txt ../atm/atOut/simplified.txt
	
--------------------
Command:

java TopicModel -q abacfile1 abacfile2 outputfile
	
Writes out quality information comparing the policies in the specified abac
files.  We use this to compare the original ABAC policy to a manually
discretized policy based on the output from MATLAB.

Example: -q ../case-studies/healthcare.abac ../atm/matABAC/healthcare_10_0.abac ../atm/matABAC/healthcare_10_0.output

--------------------
Command:	

java TopicModel -a origAbac matOutput newAbac

Creates a new .abac file based on MATLAB's output files.  this is based on
a trivial discretization that uses the most likely author and the most
likely word for each topic.  note that the MATLAB output file is produced
when you run the generated .m files in MATLAB.  recall that you can do this
for all .m files at once using runner.m.

./genABACs.sh will run this command for all input files.  since it is easy
to re-generate them, we did not commit these files to the repository.

Example: -a ../case-studies/healthcare.abac ../atm/atOut/2atom/healthcare_10_9_atomic-Out.txt ../atm/matABAC/healthcare_10_9.abac

