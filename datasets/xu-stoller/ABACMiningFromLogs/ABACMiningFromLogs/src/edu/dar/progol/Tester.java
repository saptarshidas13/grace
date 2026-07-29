/**
 *  Mining Attribute-Based Access Control Policies From Logs
 * Copyright (C) 2014 Zhongyuan Xu
 * Copyright (C) 2014 Scott D. Stoller
 * Copyright (c) 2014 Stony Brook University
 * Copyright (c) 2014 Research Foundation of SUNY
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package edu.dar.progol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import edu.dar.algo.ABACMiner;
import edu.dar.algo.LogGenerator;
import edu.dar.algo.SyntheticDistGenerator;
import edu.dar.util.LogEntry;
import edu.dar.progol.ProgolTranslator;
import edu.dar.util.CaseStudyGenerator;
import edu.dar.util.Config;
import edu.dar.progol.Parser;
import edu.dar.util.Rule;
import edu.dar.util.RuleQualityMetricType;
import edu.dar.util.SyntheticPolicyCaseStudyGenerator;
import edu.dar.util.Triple;

public class Tester 
{
	public static int NUMBER_OF_TEST_CASES = 10;
	public static int NUMBER_RULE_METRICS = 11;
	public static int NUMBER_OF_STEPS = 5;
	public static final String VERBOSE_OPTION = "-verbose";

	public static double intArrayAverage(int[] a) {
		int sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i];
		}
		return (double) sum / a.length;
	}

	public static double doubleArrayAverage(double[] a) {
		double sum = 0.0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i];
		}
		return sum / a.length;
	}

	public static double doubleArraySTDV(double[] a) {
		double mean = doubleArrayAverage(a);
		double sum = 0.0;
		for (int i = 0; i < a.length; i++) {
			sum += (a[i] - mean) * (a[i] - mean);
		}
		return Math.sqrt(sum / a.length);
	}

	public static void outputLogsToFile(ArrayList<LogEntry> logs,
			Config config, String outputFile) {
		// output to file
		try {
			File file = new File(outputFile);
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (LogEntry log : logs) {
				bw.write(log.toString()
						+ " "
						+ config.getLogToProbability().get(
								new Triple<String, String, String>(log.user,
										log.op, log.resource)));
				bw.write("\n");
			}
			bw.write("\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static <T> double jaccardSimilarity(HashSet<T> a, HashSet<T> b) {
		HashSet<T> s1 = new HashSet<T>(a);
		s1.retainAll(b);
		HashSet<T> s2 = new HashSet<T>(a);
		s2.addAll(b);
		if (s2.isEmpty()) {
			return 1.0;
		}
		return (double) s1.size() / s2.size();
	}

	public static void main(String[] args) 
	{
		if (args.length == 0) {
			System.err.println("Too few arguments!");
			System.exit(1);
		} else {
			if (args[0].length() != 2 || args[0].charAt(0) != '-') {
				System.err
						.println("The first argument specifies the excution mode, which should begin with '-' ");
				System.exit(1);
			}
			//convert logs to progol at varied percents
			if (args[0].charAt(1) =='l')
			{
				if (args.length < 3) {
					System.err.println("Three or more arguments needed for -l mode. 1. ABAC file (String) " +
							"2. Main output folder (String)\n[3.] sort order or shuffle (-small or -big or -shuffle) [3.] attribvar (Boolean)\n" +
							"[4] -all/-shuf/-shuff/-low/-high [5.] -verbose");
					System.exit(1);
				}
				
				try 
				{
					
					
					String flnameBase=args[1].substring(args[1].lastIndexOf('/')+1, args[1].lastIndexOf('.'));
					for (int i=6; i<=10; i++)
					{
						for (int j=0; j<10; j++)
						{
							
							//read in log file
							File fl = new File("./logs/case-studies/"+flnameBase+'_'+i+'_'+j+".log");
							
							// if file doesn't exists, exit with error
							if (!fl.exists()) 
							{
								prtre("Error: file not found");
								System.exit(1);
							}

							FileReader fr =new FileReader(fl);
							BufferedReader buff = new BufferedReader(fr);
							String tmp="";
							LogEntry ltmp=new LogEntry();
							ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
							
							tmp=buff.readLine();
							while (!tmp.equals(""))
							{
								ltmp.fromStringWithFreq(tmp);
								logs.add(ltmp);
								
								tmp=buff.readLine();
								ltmp=new LogEntry();
							}
							
							
							
							/*if (args.length==5)
							{
								if (args[4].equals("-shuffle"))
								{
									Collections.shuffle(logs);
								}
								else if (args[4].equals("-small") || args[4].equals("-big"))
								{
									Collections.sort(logs);
									if (args[4].equals("-big"))
										Collections.reverse(logs);
								}
							}*/
							
							Parser.config=new Config();
							Parser.parseInputABACFile("./" + args[1]);
							boolean attribVar = false;
							if (args.length >= 5) {
								if (args[4].equals("-attribVar")) {
									attribVar = true;
								}
							}
							
							prtr("Now generating "+args[2]+flnameBase+"/"+flnameBase+'_'+i+'_'+j+"-prog.pl");
							ProgolTranslator.translateLogToProgol(logs, args[2]+"/"+flnameBase +"/"+ flnameBase +'_'+i+'_'+j+"-prog.pl", Parser.config, attribVar);
							Collections.shuffle(logs, new Random(System.currentTimeMillis()));
							prtr("Now generating "+args[2]+flnameBase+"/"+flnameBase+'_'+i+'_'+j+"-prog-shuf1.pl");
							ProgolTranslator.translateLogToProgol(logs, args[2]+"/"+flnameBase +"/"+ flnameBase +'_'+i+'_'+j+"-prog-shuf1.pl", Parser.config, attribVar);
							Collections.shuffle(logs, new Random(System.currentTimeMillis()));
							prtr("Now generating "+args[2]+flnameBase+"/"+flnameBase+'_'+i+'_'+j+"-prog-shuf2.pl");
							ProgolTranslator.translateLogToProgol(logs, args[2]+"/"+flnameBase +"/"+ flnameBase +'_'+i+'_'+j+"-prog-shuf2.pl", Parser.config, attribVar);
							Collections.sort(logs);
							prtr("Now generating "+args[2]+flnameBase+"/"+flnameBase+'_'+i+'_'+j+"-prog-low.pl");
							ProgolTranslator.translateLogToProgol(logs, args[2]+"/"+flnameBase +"/"+ flnameBase +'_'+i+'_'+j+"-prog-low.pl", Parser.config, attribVar);
							Collections.reverse(logs);
							prtr("Now generating "+args[2]+flnameBase+"/"+flnameBase+'_'+i+'_'+j+"-prog-high.pl");
							ProgolTranslator.translateLogToProgol(logs, args[2]+"/"+flnameBase +"/"+ flnameBase +'_'+i+'_'+j+"-prog-high.pl", Parser.config, attribVar);
							
						}
					}
					
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//quality measurements
			else if (args[0].charAt(1)=='q')
			{
				if (args.length < 4)
				{
					System.err.println("Four or more arguments needed for -q mode. 1. ABAC file (String) " +
							"2. New ABAC files' folder 3. Output name (String) [4]. Order type (String) [5]. -o , Output coveredUPs");
					System.exit(1);
					
					//ex: -q ./case-studies/project-management.abac ./progol/project-management/progbac/ ./progol/project-management/quality/all.output
				}
				
				String nameBase=args[1].substring(args[1].lastIndexOf('/')+1, args[1].lastIndexOf('.'));
				String order="";
				if (args.length>4)
				{
					if (!args[4].equals("-o"))
					{
						order=args[4];
					}
				}
				
				ArrayList<String> qualities=new ArrayList<String>();
				
				for (int completeness=6; completeness<=10; completeness++)
				{
					for (int sequence=0; sequence<10; sequence++)
					{
						
						String progbac=args[2]+nameBase+"_"+completeness+"_"+sequence+"-prog"+order+".abac";
						//String outfile=args[3]+nameBase+"_"+completeness+"_"+sequence+"-prog-high.output";
						
						prtr("Now processing "+progbac);
						
						double[][][] numUsers = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
						double[][][] numResources = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
						double[][][] numOps = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
						double[][][] numCoveredUP = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
		
						double[][][] syntacticSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
						double[][][] overAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
						double[][][] underAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
						double[][][] jaccardSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
						
						
						
						Parser.config=new Config();
						Parser.parseInputProgFile(progbac);
						ArrayList<Rule> progRs=(ArrayList<Rule>) Parser.config.getRuleList().clone();
						
						
						Parser.config = new Config();
						Parser.parseInputABACFile(args[1]);
						
						
						
						
						double similarity = ABACMiner.symmetricSyntacticSimilarityOfPolicies(Parser.config.getRuleList(), progRs, Parser.config);
						
						
						HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
						for (Rule r : progRs) 
						{
							coveredUP.addAll(r.getCoveredUPTriple());
						}
						
						HashSet<Triple<String, String, String>> overAssignment = new HashSet<Triple<String, String, String>>(coveredUP);
						overAssignment.removeAll(Parser.config.getCoveredUP());
						
						HashSet<Triple<String, String, String>> underAssignment = new HashSet<Triple<String, String, String>>(Parser.config.getCoveredUP());
						underAssignment.removeAll(coveredUP);
						
						double overAssignmentRatio = (double) overAssignment.size() / Parser.config.getCoveredUP().size();
						double underAssignmentRatio = (double) underAssignment.size() / Parser.config.getCoveredUP().size();
						
						
						if (args.length==6 || (args.length==5 && args[4].equals("-o")))
						{
							recordCoveredUP(coveredUP, Parser.config.getCoveredUP(), overAssignment, underAssignment, nameBase, 
									completeness, sequence, overAssignmentRatio, underAssignmentRatio);
						}
						
						
						for (int i = 0; i < NUMBER_OF_TEST_CASES; i++)
						{
							for (int j = 6; j <= 10; j += 1)
							{
								
								for (int k = 0; k < NUMBER_RULE_METRICS; k++) 
								{
									syntacticSimilarities[j - 6][k][i] = similarity;
									overAssignmentRatios[j - 6][k][i] = overAssignmentRatio;
									underAssignmentRatios[j - 6][k][i] = underAssignmentRatio;
		
									numUsers[j - 6][k][i] = Parser.config.getNumUsers();
									numResources[j - 6][k][i] = Parser.config.getResources().size();
									numOps[j - 6][k][i] = Parser.config.getOps().size();
									numCoveredUP[j-6][k][i] = coveredUP.size();
									jaccardSimilarities[j-6][k][i] = jaccardSimilarity(coveredUP, Parser.config.getCoveredUP());
									
									
									
									
								}
							}
						}
						/*try
						{*/
							//String outputFile = outfile;
							//File file = new File(outputFile);
							// if file doesnt exists, then create it
							/*if (!file.exists()) {
								file.createNewFile();
							}*/
			
							//FileWriter fw = new FileWriter(file.getAbsoluteFile());
							//BufferedWriter bw = new BufferedWriter(fw);
							for (int r = 0; r < 1/*NUMBER_RULE_METRICS*/; r++) 
							{
								//bw.write((RuleQualityMetricType.values()[r]).toString() + "\n");
								for (int s = 0; s < 1/*NUMBER_OF_STEPS*/; s++) 
								{
									Double fraction = s * 0.1 + 0.6;
									
									/*bw.write("\nIteration: "+fraction+"\n"+
										"Avg users\tSTDV users\tAvg resources\tSTDV resources\tAvg ops\tSTDV ops\tAvg covered\tSTDV covered\tAvg syntactic sim\tSTDV syntactic sim\t" +
										"Avg overassign\tSTDV overassign\tAvg underassign\tSTDV underassign\tAvg jaccard\tSTDV jaccard\n"
										+*/qualities.add(doubleArrayAverage(numUsers[s][r])
										+ "\t"
										+ doubleArraySTDV(numUsers[s][r])
										+ "\t\t"
										+ doubleArrayAverage(numResources[s][r])
										+ "\t"
										+ doubleArraySTDV(numResources[s][r])
										+ "\t\t"
										+ doubleArrayAverage(numOps[s][r])
										+ "\t"
										+ doubleArraySTDV(numOps[s][r])
										+ "\t\t"
										+ doubleArrayAverage(numCoveredUP[s][r])
										+ "\t"
										+ doubleArraySTDV(numCoveredUP[s][r])
										+ "\t\t"
										+ doubleArrayAverage(syntacticSimilarities[s][r])
										+ "\t"
										+ doubleArraySTDV(syntacticSimilarities[s][r])
										+ "\t\t"
										+ doubleArrayAverage(overAssignmentRatios[s][r])
										+ "\t"
										+ doubleArraySTDV(overAssignmentRatios[s][r])
										+ "\t\t"
										+ doubleArrayAverage(underAssignmentRatios[s][r])
										+ "\t"
										+ doubleArraySTDV(underAssignmentRatios[s][r])
										+ "\t\t"
										+ doubleArrayAverage(jaccardSimilarities[s][r])
										+ "\t"
										+ doubleArraySTDV(jaccardSimilarities[s][r]));
								}
								//bw.write("\n\n");
								qualities.add("\n");
							}
							//bw.close();
						/*}
						catch (IOException e)
						{
							
						}*/
					}
				}
				
				
				try
				{
					File f=new File(args[3]);
					if (!f.exists()) {
						f.createNewFile();
					}
					
					FileWriter fw = new FileWriter(f.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);
					
					for (int i=0; i<qualities.size(); i++)
					{
						bw.write(qualities.get(i));
					}
					bw.close();
					fw.close();
				}
				catch (IOException e)
				{
					
				}
				
				
				
				
			}
			//parse proglogs time taken
			else if (args[0].charAt(1)=='t')
			{
				String baseF=args[1];
				String order="";
				if (args.length==4)
				{
					order=args[3];
				}
				
				ArrayList<String> times=new ArrayList<String>();
				
				try
				{
					for (int i=6; i<=10; i++)
					{
						for (int j=0; j<10; j++)
						{
							File f=new File(baseF+"_"+i+"_"+j+"-prog"+order+".proglog");
							if (!f.exists()) {
								prtre("File not found");
							}
			
							BufferedReader br = new BufferedReader(new FileReader(f));
							String line = null;
							while ((line = br.readLine()) != null) 
							{
								if (!line.trim().isEmpty()) 
								{
									if (line.startsWith("[Time") )
									{
										times.add(line);
									}
								}
							}
							br.close();
						}
					}
					
					File out = new File(args[2]);
					if (!out.exists()) {
						out.createNewFile();
					}
					
					FileWriter fw = new FileWriter(out.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);
					
					for (String s : times)
					{
						s=s.substring(s.lastIndexOf(' ')+1,s.indexOf(']'));
						s=s.replace('s', '\n');
						bw.write(s);
					}
					bw.close();
					fw.close();
					
				}
				catch (IOException e)
				{
					
				}
				
			}
			//take proglog rules and put them into abac files
			else if (args[0].charAt(1)=='a')
			{
				if (args.length<4)
				{
					prtre("Three or more argurments required for -a mode: 1. Original abac file (String) 2. Folder containing proglog files (String)"
							+ "3. Output locations (String) [4]. Order type (String)");
					System.exit(1);
					
					//ex: -a ./case-studies/online-video.abac ./progol/online-video/proglog/ ./progol/online-video/tmp/ -high
				}
				
				String order="";
				if (args.length==5)
				{
					order=args[4];
				}
				
				
				
				try
				{
					File f=new File(args[1]);
					if (!f.exists())
					{
						prtre("Error file "+args[1]+" could not be found");
						System.exit(1);
					}
					FileReader fr =new FileReader(f);
					BufferedReader buff = new BufferedReader(fr);
					
					ArrayList<String> abacFile=new ArrayList<String>();
					
					//read in abac file up to the rules section and store it
					String tmp=buff.readLine();
					while (tmp!=null)
					{
						if (!tmp.equals(""))
						{
							//comments and blank lines excluded
							if (tmp.charAt(0)!='#')
							{
								if (!tmp.startsWith("rule("))
								{
									abacFile.add(tmp);
								}
							}
						}
						tmp=buff.readLine();
					}
					buff.close();
					fr.close();
					
					ArrayList<String> tmpRules=new ArrayList<String>();
					String nameBase=args[1].substring(args[1].lastIndexOf('/')+1, args[1].lastIndexOf('.'));
					for (int i=6; i<=10; i++)
					{
						for (int j=0; j<10; j++)
						{
							f=new File(args[2]+nameBase+"_"+i+"_"+j+"-prog"+order+".proglog");
							fr=new FileReader(f);
							buff=new BufferedReader(fr);
							tmpRules=new ArrayList<String>();
							
							//read in the proglog up to the very last up-tuple
							tmp=buff.readLine();
							boolean ended=false;
							prtr("Now processing: "+f.toString());
							while(tmp!=null && ended==false)
							{
								if (!tmp.equals(""))
								{
									
									if (tmp.toLowerCase().contains("error"))
									{
										prtre("Error detected in file: "+tmp);
										System.exit(1);
									}
									
									//exclude unnecessary lines
									if (!tmp.startsWith("[C:"))
									{
										if (!tmp.startsWith("[:- "))
										{											
											if (tmp.contains("[Total number of clauses"))
											{
												ended=true;
											}
											else
											{
												if (tmp.charAt(0)=='\t')
												{
													
													tmp=tmpRules.get(tmpRules.size()-1)+tmp.substring(1);
													tmpRules.remove(tmpRules.size()-1);
												}
												tmpRules.add(tmp);
											}
										}
									}
								}
								tmp=buff.readLine();
							}
							buff.close();
							fr.close();
							
							//work backwards to find the index where the results are
							int index=0;
							for (int k=tmpRules.size()-1; k>0; k--)
							{
								if (tmpRules.get(k).charAt(0)=='[' && tmpRules.get(k).charAt(tmpRules.get(k).length()-1)==']')
								{
									index=k+1;
									k=0;
								}
							}
							//remove everything except the tuples
							for (int k=0; k<index; k++)
							{
								tmpRules.remove(0);
							}
							
							
							//write out a new abac file with abac attr/reso and progol rules
							f=new File(args[3]+nameBase+"_"+i+"_"+j+"-prog"+order+".abac");
							if (!f.exists())
							{
								f.createNewFile();
							}
							
							FileWriter fw=new FileWriter(f);
							BufferedWriter bw=new BufferedWriter(fw);
							
							for (String s : abacFile)
							{
								bw.write(s+"\n");
							}
							for (String s: tmpRules)
							{
								bw.write(s+"\n");
							}
							
							bw.close();
							fw.close();
						}
					}
				}
				catch (IOException e)
				{
					
				}
			}
			//single quality evaluation
			else if (args[0].charAt(1)=='s')
			{
				//-s ./case-studies/online-video.abac ./progol/online-video/progbac/perfect.abac ./progol/online-video/quality/perfect.output
				
				
				String nameBase=args[2].substring(args[2].lastIndexOf('/')+1);
				ArrayList<String> qualities=new ArrayList<String>();
				
				prtr("Now processing "+args[2]);
				
				double[][][] numUsers = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numResources = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numOps = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numCoveredUP = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				double[][][] syntacticSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] overAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] underAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] jaccardSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				
				
				Parser.config=new Config();
				Parser.parseInputProgFile(args[2]);
				ArrayList<Rule> progRs=(ArrayList<Rule>) Parser.config.getRuleList().clone();
				
				Parser.config = new Config();
				Parser.parseInputABACFile(args[1]);
				
				double similarity = ABACMiner.symmetricSyntacticSimilarityOfPolicies(Parser.config.getRuleList(), progRs, Parser.config);
				
				
				HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
				for (Rule r : progRs) 
				{
					coveredUP.addAll(r.getCoveredUPTriple());
				}
				
				HashSet<Triple<String, String, String>> overAssignment = new HashSet<Triple<String, String, String>>(coveredUP);
				overAssignment.removeAll(Parser.config.getCoveredUP());
				
				HashSet<Triple<String, String, String>> underAssignment = new HashSet<Triple<String, String, String>>(Parser.config.getCoveredUP());
				underAssignment.removeAll(coveredUP);
				
				double overAssignmentRatio = (double) overAssignment.size() / Parser.config.getCoveredUP().size();
				double underAssignmentRatio = (double) underAssignment.size() / Parser.config.getCoveredUP().size();
				
				
				if (args.length==5)
				{
					recordCoveredUP(coveredUP, Parser.config.getCoveredUP(), overAssignment, underAssignment, nameBase, 
							11, 11, overAssignmentRatio, underAssignmentRatio);
				}
				
				
				for (int i = 0; i < NUMBER_OF_TEST_CASES; i++)
				{
					for (int j = 6; j <= 10; j += 1)
					{
						
						for (int k = 0; k < NUMBER_RULE_METRICS; k++) 
						{
							syntacticSimilarities[j - 6][k][i] = similarity;
							overAssignmentRatios[j - 6][k][i] = overAssignmentRatio;
							underAssignmentRatios[j - 6][k][i] = underAssignmentRatio;

							numUsers[j - 6][k][i] = Parser.config.getNumUsers();
							numResources[j - 6][k][i] = Parser.config.getResources().size();
							numOps[j - 6][k][i] = Parser.config.getOps().size();
							numCoveredUP[j-6][k][i] = coveredUP.size();
							jaccardSimilarities[j-6][k][i] = jaccardSimilarity(coveredUP, Parser.config.getCoveredUP());
						}
					}
				}
				for (int r = 0; r < 1; r++) 
				{
					for (int s = 0; s < 1; s++) 
					{
						qualities.add(doubleArrayAverage(numUsers[s][r])
							+ "\t"
							+ doubleArraySTDV(numUsers[s][r])
							+ "\t\t"
							+ doubleArrayAverage(numResources[s][r])
							+ "\t"
							+ doubleArraySTDV(numResources[s][r])
							+ "\t\t"
							+ doubleArrayAverage(numOps[s][r])
							+ "\t"
							+ doubleArraySTDV(numOps[s][r])
							+ "\t\t"
							+ doubleArrayAverage(numCoveredUP[s][r])
							+ "\t"
							+ doubleArraySTDV(numCoveredUP[s][r])
							+ "\t\t"
							+ doubleArrayAverage(syntacticSimilarities[s][r])
							+ "\t"
							+ doubleArraySTDV(syntacticSimilarities[s][r])
							+ "\t\t"
							+ doubleArrayAverage(overAssignmentRatios[s][r])
							+ "\t"
							+ doubleArraySTDV(overAssignmentRatios[s][r])
							+ "\t\t"
							+ doubleArrayAverage(underAssignmentRatios[s][r])
							+ "\t"
							+ doubleArraySTDV(underAssignmentRatios[s][r])
							+ "\t\t"
							+ doubleArrayAverage(jaccardSimilarities[s][r])
							+ "\t"
							+ doubleArraySTDV(jaccardSimilarities[s][r]));
					}
					//bw.write("\n\n");
					qualities.add("\n");
				}
				

				try
				{
					File f=new File(args[3]);
					if (!f.exists()) {
						f.createNewFile();
					}
					
					FileWriter fw = new FileWriter(f.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);
					
					for (int i=0; i<qualities.size(); i++)
					{
						bw.write(qualities.get(i));
					}
					bw.close();
					fw.close();
				}
				catch (IOException e)
				{
					
				}
			}	
		}
	}
	
	public static void recordCoveredUP(HashSet<Triple<String, String, String>> progUP, HashSet<Triple<String, String, String>> parserUP, 
			HashSet<Triple<String, String, String>> overAssignment, HashSet<Triple<String, String, String>> underAssignment, 
			String nameBase, int completeness, int sequence, double oRatio, double uRatio)
	{
		try
		{
			File f=new File("./progol/"+nameBase+"/tmp/"+nameBase+"_"+completeness+"_"+sequence+".coup");
			if (!f.exists())
			{
				f.createNewFile();
			}
			FileWriter fw=new FileWriter(f);
			BufferedWriter buff=new BufferedWriter(fw);
			
			buff.write("UP Tuples for "+nameBase+"_"+completeness+"_"+sequence+"\n");
			buff.write("\nProgol's tuples\n");
			for (Triple up: progUP)
			{
				buff.write(up.toString()+"\n");
			}
			buff.write("\nSize = "+progUP.size());
			buff.write("\n\nABAC's tuples\n");
			for (Triple up: parserUP)
			{
				buff.write(up.toString()+"\n");
			}
			buff.write("\nSize = "+parserUP.size());
			buff.write("\n\nOverassignments\n");
			for (Triple up: overAssignment)
			{
				buff.write(up.toString()+"\n");
			}
			buff.write("\nSize = "+overAssignment.size());
			buff.write("\n\nUnderassignments\n");
			for (Triple up: underAssignment)
			{
				buff.write(up.toString()+"\n");
			}
			buff.write("\nSize = "+underAssignment.size());
			buff.write("\n\n-----------------------");
			buff.write("\n\nOverassign = "+oRatio);
			buff.write("\nUnderassign = "+uRatio+"\n\n\n");
			
			buff.close();
			fw.close();
		}
		catch (IOException e)
		{
			
		}
	}
	
	public static void prtr(String txt)
	{
		System.out.println(txt);
	}
	public static void prtre(String txt)
	{
		System.err.println(txt);		
	}
}
