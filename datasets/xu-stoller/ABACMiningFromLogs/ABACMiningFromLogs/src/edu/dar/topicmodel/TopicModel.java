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

package edu.dar.topicmodel;

// to use this program, call TopicModel.createMATLABpgm(...) to create the
// MATLAB program, copy it to the folder in which you extracted
// topicmodeltoolbox.zip, and run the .m file in MATLAB.

// most of these imports are simply copied from progol/Tester.java.
// to do: eliminate unnecessary imports
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.lang.Math;

import edu.dar.util.Config;
import edu.dar.algo.ABACMiner;
import edu.dar.progol.AbacFilter;
import edu.dar.progol.Parser;
import edu.dar.util.AttrAttrConjunct;
import edu.dar.util.AttrValConjunct;
import edu.dar.util.Attribute;
import edu.dar.util.Operator;
import edu.dar.util.Pair;
import edu.dar.util.Rule;
import edu.dar.util.Triple;
import edu.dar.util.ValueType;

// Authors correspond to tuples $\\tuple{\\uae, \\rae, \\con}$, where
// $\\uae$ is a user attribute expression with at most $b_u$ conjuncts,
// $\\rae$ is a resource attribute expression with at most $b_r$
// conjuncts, and $\\con$ is a constraint with at most $b_c$ atomic
// constraints.  Furthermore, each conjunct in $\\uae$ or $\\rae$ may
// contain only one value, specifically, one atomic value or one set of
// size at most $b_s$, depending on whether the attribute is
// single-valued or multi-valued.  Disjunction (i.e., conjuncts with
// multiple values) is introduced later, by ``merging'' sets of rules,
// as described below.  In experiments, we take $b_u=2$, $b_r=2$,
// $b_c=2$, and $b_s=1$; these are the smallest values sufficient to
// express our case study policies.
class Author 
{
	public ArrayList<AttrValConjunct> uae;
	public ArrayList<AttrValConjunct> rae;
	public ArrayList<AttrAttrConjunct> con;
	int index;
	static int nextIndex = 1;

	public Author(ArrayList<AttrValConjunct> uae, ArrayList<AttrValConjunct> rae, ArrayList<AttrAttrConjunct> con, int idx) 
	{
		this.uae = uae;
		this.rae = rae;
		this.con = con;
		this.index = nextIndex;
		nextIndex++;
	}

	public String removeDelimiters(String s) {
		return s.substring(1, s.length() - 1);
	}

	public String toString() {
		return removeDelimiters(uae.toString()) + "; " 
		+ removeDelimiters(rae.toString()) + "; "
		+ removeDelimiters(con.toString());
	}
}

class Document 
{
	String user;
	String resource;
	// wordCount maps the index of a word to the count (number of
	// occurrences) of the word in this document
	//HashSet<String> words;
	HashMap<Integer,Integer> wordCount;
	HashSet<Author> authors;
	int index;
	static int nextIndex = 1;

	public Document(String user, String resource, HashMap<Integer,Integer> wc, HashSet<Author> authors) 
	{
		this.user = user;
		this.resource = resource;
		this.wordCount = wc;
		this.authors = authors;
		this.index = nextIndex;
		nextIndex++;
	}
}

public class TopicModel 
{
	
	static boolean noIDs=false;
	static boolean twoPlus=false;
	static boolean atomOnly=false;
	static enum AttributeClass {user, resource};

	// construct all attrib exprs containing two conjuncts using the given
	// attribute set, where each conjunct contains 1 atomic value or 1
	// singleton set.
	//
	// attrClass = attribute class, identifies whether the UAE or RAE is being constructed
	@SuppressWarnings("unchecked")
	public static ArrayList<ArrayList<AttrValConjunct>> constructAttrExprs(HashMap<String, Attribute> attrSet, AttributeClass attrClass) 
	{
		// construct all conjuncts containing 1 atomic value or 1 singleton set
		ArrayList<AttrValConjunct> conjuncts = new ArrayList<AttrValConjunct>();
		for (Map.Entry<String, Attribute> entry : attrSet.entrySet()) 
		{
			//String name = entry.getKey();
			Attribute attr = entry.getValue();
			// assert(name == attr.getName());
			switch (attr.getvType()) 
			{
				case Single:
					for (String s : attr.getDomain()) 
					{
						HashSet<String> sSet = new HashSet<String>();
						sSet.add(s);
						AttrValConjunct conjunct = new AttrValConjunct(attr.getName(), sSet, null, "=");
						conjuncts.add(conjunct);
					}
					break;
				case Set:
					// compute the set of atomic values that occur in values of this attribute.
					HashSet<String> atomicVals = new HashSet<String>();
					for (HashSet<String> s : attr.getSetDomain()) {
						atomicVals.addAll(s);
					}
					for (String s : atomicVals) 
					{
						HashSet<String> sSet = new HashSet<String>();
						sSet.add(s);
						HashSet<HashSet<String>> sSetSet = new HashSet<HashSet<String>>();
						sSetSet.add(sSet);
						AttrValConjunct conjunct = new AttrValConjunct(attr.getName(), null, sSetSet, "=");
						conjuncts.add(conjunct);
					}
					break;
			}
		}
		
		
		int counter=0;
		// construct all attrib exprs containing two distinct conjuncts from uaeConjuncts
		ArrayList<ArrayList<AttrValConjunct>> attrExprs = new ArrayList<ArrayList<AttrValConjunct>>();
		for (AttrValConjunct c1 : conjuncts) 
		{
			for (AttrValConjunct c2 : conjuncts)
			{
				if (c1.equals(c2) || c1.getLHS().equals(c2.getLHS())) continue;
				if (noIDs==true && (c1.getLHS().equals("rid") || c1.getLHS().equals("uid") || c2.getLHS().equals("rid") || c2.getLHS().equals("uid") ))
					continue;
				
				ArrayList<AttrValConjunct> ae = new ArrayList<AttrValConjunct>();
				ae.add(c1);
				ae.add(c2);
				
				ArrayList<AttrValConjunct> revved=(ArrayList<AttrValConjunct>) ae.clone();
				Collections.reverse(revved);
				
				if (!attrExprs.contains(ae) && !attrExprs.contains(revved))
				{
					if (twoPlus==false)
					{
						attrExprs.add(ae);
						counter++;
					}
					else
					{
						switch(attrClass)
						{
							case user:
								if (areTwoUsersSatisfied(ae, Parser.config))
								{
									attrExprs.add(ae);
									counter++;
								}
								break;
							case resource:
								if (areTwoResrSatisfied(ae, Parser.config))
								{
									attrExprs.add(ae);
									counter++;
								}
								break;
						}
					}
				}
			}
		}
		prtr("-AEs: "+counter);
		stats.add(counter);
		counter=0;
		
		// construct atomic attrib exprs
		for (AttrValConjunct c : conjuncts)
		{
			if (noIDs==true && (c.getLHS().equals("rid") || c.getLHS().equals("uid")  ))
				continue;
			
			ArrayList<AttrValConjunct> attr = new ArrayList<AttrValConjunct>();
			attr.add(c);
			
			if (twoPlus==false)
			{
				attrExprs.add(attr);
				counter++;
			}
			else
			{
				switch(attrClass)
				{
					case user:
						if (areTwoUsersSatisfied(attr, Parser.config))
						{
							attrExprs.add(attr);
							counter++;
						}
						break;
					case resource:
						if (areTwoResrSatisfied(attr, Parser.config))
						{
							attrExprs.add(attr);
							counter++;
						}
						break;
				}
			}
		}
		prtr("- conjuncts: "+counter);
		stats.add(counter);
		
		// blank conjunct
		attrExprs.add(new ArrayList<AttrValConjunct>());
		
		return attrExprs;
	}

	// construct all constraints containing two atomic constraints using the
	// given attribute sets
	@SuppressWarnings("unchecked")
	public static ArrayList<ArrayList<AttrAttrConjunct>> constructConstraints(HashMap<String, Attribute> userAttrSet, HashMap<String, Attribute> resAttrSet) 
	{
		// construct all atomic constraints 
		ArrayList<AttrAttrConjunct> atomicConstraints = new ArrayList<AttrAttrConjunct>();
		// iterate over user attributes
		for (Map.Entry<String, Attribute> userEntry : userAttrSet.entrySet())
		{
			Attribute ua = userEntry.getValue();
			// iterate over resource attributes
			for (Map.Entry<String, Attribute> resEntry : resAttrSet.entrySet()) 
			{
				Attribute ra = resEntry.getValue();
				// create an atomic constraint with user attribute ua, resource
				// attribute ra, and the operator (if any) that is consistent the
				// multiplicities of those attributes.
				switch (ua.getvType()) 
				{
					case Single:
						switch (ra.getvType()) 
						{
							case Single:
								atomicConstraints.add(new AttrAttrConjunct(ua.getName(), ra.getName(), Operator.EQUALS));
								break;
							case Set:
								// no constraint can be constructed in this case
								break;
						}
						break;
					case Set:
						switch (ra.getvType()) 
						{
							case Single:
								atomicConstraints.add(new AttrAttrConjunct(ua.getName(), ra.getName(), Operator.IN));
								break;
							case Set:
								atomicConstraints.add(new AttrAttrConjunct(ua.getName(), ra.getName(), Operator.SUPSETEQ));
								break;
						}
						break;
				}
			}
		}

		// construct all constraints containing two distinct atomic constraints
		// from atomicConstraints
		int counter=0;
		ArrayList<ArrayList<AttrAttrConjunct>> constraints = new ArrayList<ArrayList<AttrAttrConjunct>>();
		if (atomOnly==false)
		{
			for (AttrAttrConjunct ac1 : atomicConstraints) 
			{
				for (AttrAttrConjunct ac2 : atomicConstraints) 
				{
					if (ac1.equals(ac2) || ac1.getRHS().equals(ac2.getRHS()) || ac1.getLHS().equals(ac2.getLHS())) continue;
					if (noIDs==true && (ac1.getLHS().equals("rid") || ac1.getLHS().equals("uid") || 
							ac1.getRHS().equals("rid") || ac1.getRHS().equals("uid") || 
							ac2.getLHS().equals("rid") || ac2.getLHS().equals("uid") ||
							ac2.getRHS().equals("rid") || ac2.getRHS().equals("uid") ))
						continue;
					
					ArrayList<AttrAttrConjunct> c = new ArrayList<AttrAttrConjunct>();
					c.add(ac1);
					c.add(ac2);
					
					if (!constraints.contains(c))
					{
						if (twoPlus==false)
						{
							constraints.add(c);
							counter++;
						}
						else
						{
							if (areTwoConsSatisfied(c, Parser.config))
							{
								constraints.add(c);
								counter++;
							}
						}
					}
				}
			}
		}
		prtr("Constraints: "+counter);
		stats.add(counter);
		counter=0;
		
		// atomic constraints
		for (AttrAttrConjunct aac: atomicConstraints)
		{
			if (noIDs==true && (aac.getLHS().equals("rid") || aac.getLHS().equals("uid") ))
				continue;
			
			ArrayList<AttrAttrConjunct> c=new ArrayList<AttrAttrConjunct>();
			c.add(aac);
			
			if (twoPlus==false)
			{
				constraints.add(c);
				counter++;
			}
			else
			{
				if (areTwoConsSatisfied(c, Parser.config))
				{
					constraints.add(c);
					counter++;
				}
			}
		}
		prtr("Atomic constraints: "+counter);
		stats.add(counter);
		
		// blank/no consntraint
		constraints.add(new ArrayList<AttrAttrConjunct>());
		
		return constraints;
	}


	// this is a copy of Parser.satisfyingRuleConstraints with two minor changes:
	// replace argument "Rule r" with "ArrayList<AttrAttrConjunct> con";
	// replace occurrences of "r.getCon()" with "con".
	public static boolean satisfyingConstraints(String user, String perm,
			ArrayList<AttrAttrConjunct> con, Config config) {
		if (con.isEmpty()) {
			return true;
		}
		for (AttrAttrConjunct c : con) {
			if (c instanceof AttrAttrConjunct) {
				if (!config.getUserAttrInfo().get(user).containsKey(c.getLHS())) {
					return false;
				}
				if (!config.getResourceAttrInfo().get(perm)
						.containsKey(c.getRHS())) {
					return false;
				}

				String uAttr = c.getLHS();
				String pAttr = c.getRHS();

				HashSet<String> userAttrVals = config.getUserAttrInfo()
						.get(user).get(c.getLHS());
				HashSet<String> permAttrVals = config.getResourceAttrInfo()
						.get(perm).get(c.getRHS());
				switch (c.getOperator()) {
				case EQUALS:
					if (!(config.getUserAttrSet().get(uAttr).getvType() == ValueType.Single)
							|| !(config.getResourceAttrSet().get(pAttr)
									.getvType() == ValueType.Single)
							|| !userAttrVals.equals(permAttrVals)) {
						return false;
					}
					break;
				case SUBSETEQ:
					if (!permAttrVals.containsAll(userAttrVals)) {
						return false;
					}
					break;
				case SUPSETEQ:
					if (!(config.getUserAttrSet().get(uAttr).getvType() == ValueType.Set)
							|| !(config.getResourceAttrSet().get(pAttr)
									.getvType() == ValueType.Set)
							|| !userAttrVals.containsAll(permAttrVals)) {
						return false;
					}
					break;
				case IN:
					if (!(config.getUserAttrSet().get(uAttr).getvType() == ValueType.Set)
							|| !(config.getResourceAttrSet().get(pAttr)
									.getvType() == ValueType.Single)
							|| !userAttrVals.containsAll(permAttrVals)) {
						return false;
					}
					break;
				default:
					return false;
				}
			}
		}
		return true;
	}
	
	
	
	/*
	 * args:
	 * 	-m: 	generate a single .m file
	 * 	-ml:	generate a batch of 50 .m for a given case study
	 * 
	 * param samples:
	 * case-studies/online-video.abac logs/case-studies/online-video_6_0.log atm/online_video_6_0 6 1 0.01 10 3 0
	 * case-studies/healthcare.abac logs/case-studies/healthcare_6_0.log atm/healthcare_6_0 9 1 0.01 10 3 0
	 * case-studies/university.abac logs/case-studies/university_6_0.log atm/university_6_0 10 1 0.01 10 3 0
	 * case-studies/project-management.abac logs/case-studies/project-management_6_0.log atm/project_management_6_0 11 1 0.01 10 3 0
	 * 
	 * case-studies/modified/nurseHR_healthcare.abac logs/case-studies/new/healthcare_10_0_new.log atm/nurseHR_healthcare_10_0_new 9 1 0.01 10 3 0 -id -tp -atoms
	 * 
	 * changes from ExampleAT1:
	 * 		K = number of operations
	 * 		E = 1.1
	 * 		M = 1, easier to read the output
	 *
	 * problems:
	 * 		336: Parser.satisfyingRAE --> causes null pointer exception
	 *  		
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) 
	{
		if (args[0].charAt(1)=='m')
		{
			String abacFile = args[1];
			String logFile = args[2];
			String outFile = args[3];
			int numTopics = Integer.parseInt(args[4])+1;
			double alpha = Double.parseDouble(args[5]);
			double beta = Double.parseDouble(args[6]);
			int numIter = Integer.parseInt(args[7]);
			int seed = Integer.parseInt(args[8]);
			int verbosity = Integer.parseInt(args[9]);
			
			stats=new ArrayList();
			outFile=outFile.replace('-', '_');
			
			if (args.length>=11)
			{
				if (args.length==13)
				{
					noIDs=true;
					twoPlus=true;
					atomOnly=true;
				}
				else
				{
					for (int i=10; i<args.length; i++)
					{
						if (args[i].equals("-tp"))
							twoPlus=true;
						else if (args[i].equals("-id"))
							noIDs=true;
						else if (args[i].equals("-atoms"))
							atomOnly=true;
					}
				}
				
			}
			
			if (args[0].equals("-m"))
			{
				final long startTime=System.currentTimeMillis();
				createMATLABpgm(abacFile, logFile, outFile, numTopics, alpha, beta, numIter, seed, verbosity);
				final long endTime=System.currentTimeMillis();
				prtr("Total time: "+(endTime-startTime));
				stats.add("Total time:\t"+(endTime-startTime));
				writeStats(logFile);
			}
			else if (args[0].equals("-ml"))
			{
				String baseName=abacFile.substring(abacFile.lastIndexOf('/')+1, abacFile.lastIndexOf('.'));
				for (int i=6; i<=10; i++)
				{
					for (int j=0; j<10; j++)
					{
						logFile="logs/case-studies/new/"+baseName+"_"+i+"_"+j+"_new.log";
						outFile="atm/"+baseName.replace('-', '_')+"_"+i+"_"+j;
						
						prtr(logFile+"\n----------------\n");
						
						final long startTime=System.currentTimeMillis();
						createMATLABpgm(abacFile, logFile, outFile, numTopics, alpha, beta, numIter, seed, verbosity);
						final long endTime=System.currentTimeMillis();
						prtr("Total time: "+(endTime-startTime));
						stats.add("Total time:\t"+(endTime-startTime));
						writeStats(logFile);
						
						Author.nextIndex=1;
						Document.nextIndex=1;
						cleanUp();
					}
				}
			}
		}
		else if (args[0].charAt(1)=='t')
		{
			if (args[0].equals("-to"))
			{
				// -to inputFolder outputname basename
				// -to ./atm/input_1atom/ ./atm/input_1atom_summary.output
				
				AbacFilter filt = new AbacFilter();
				filt.setExt(".output");
				
				if (args.length==4)
					filt.setBase(args[3]);
				else
					filt.setBase("*");
				
				File dir = new File(args[1]);
				
				File[] outputs=dir.listFiles(filt);
				
				File out = new File(args[2]);
				
				try
				{
					if (!out.exists())
					{
						out.createNewFile();
					}
					
					FileWriter fw=new FileWriter(out);
					BufferedWriter bw=new BufferedWriter(fw);
					
					FileReader fr=null;
					BufferedReader buff=null;
					
					for (File f : outputs)
					{
						fr=new FileReader(f);
						buff=new BufferedReader(fr);
						
						String tmp=buff.readLine();
						while (tmp!=null && !tmp.equals(""))
						{
							String[] pair=tmp.split("\t");
							
							if (pair[0].contains("allAuthors"))
							{
								bw.write(pair[1]+"\t\t");
							}
							else if (pair[0].contains("time"))
							{
								bw.write(pair[1]+"\t\t");
							}
							tmp=buff.readLine();
						}
						bw.write('\n');
					}
					
					bw.close();
					fw.close();
					buff.close();
					fr.close();
				}
				catch (IOException e)
				{
					////
				}
			}
			else if (args[0].equals("-tr"))
			{
				// -tr inputfile outputfile
				// -tr ./atm/atOut/1atom_MAT_times.txt ./atm/atOut/simplified.txt
				
				File input=new File(args[1]);
				File output=new File(args[2]);
				try
				{
					FileReader fr=new FileReader(input);
					BufferedReader br=new BufferedReader(fr);
					
					if (!output.exists())
						output.createNewFile();
					
					FileWriter fw=new FileWriter(output);
					BufferedWriter bw=new BufferedWriter(fw);
					
					
					String tmp=br.readLine();
					while (tmp!=null)
					{
						String[] parts=tmp.split(" ");
						
						double time=Double.parseDouble(parts[3]);
						time*=1000;
						
						bw.write(time+"\n");
						
						tmp=br.readLine();
					}
					
					bw.close();
					fw.close();
					br.close();
					fr.close();
					
				}
				catch (IOException e)
				{
					
				}
				
			}
		}
		else if (args[0].charAt(1)=='q')
		{
			// -q case-studies/healthcare.abac atm/matABAC/healthcare.abac atm/matABAC/
			
			prtr("Now processing: "+args[1]+" against "+args[2]);
			
			ArrayList<String> qualities=new ArrayList<String>();
			
			
			Parser.config=new Config();
			Parser.parseInputABACFile(args[2]);
			ArrayList<Rule> matRs=(ArrayList<Rule>) Parser.config.getRuleList().clone();
			int opSize=Parser.config.getOps().size();
			
			Parser.config=new Config();
			Parser.parseInputABACFile(args[1]);
			
			double similarity = ABACMiner.symmetricSyntacticSimilarityOfPolicies(Parser.config.getRuleList(), matRs, Parser.config);
			double jaccard=-1;
			
			HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
			for (Rule r : matRs) 
			{
				coveredUP.addAll(r.getCoveredUPTriple());
			}
			
			HashSet<Triple<String, String, String>> overAssignment = new HashSet<Triple<String, String, String>>(coveredUP);
			overAssignment.removeAll(Parser.config.getCoveredUP());
			
			HashSet<Triple<String, String, String>> underAssignment = new HashSet<Triple<String, String, String>>(Parser.config.getCoveredUP());
			underAssignment.removeAll(coveredUP);
			
			double overAssignmentRatio = (double) overAssignment.size() / Parser.config.getCoveredUP().size();
			double underAssignmentRatio = (double) underAssignment.size() / Parser.config.getCoveredUP().size();
			
			jaccard = jaccardSimilarity(coveredUP, Parser.config.getCoveredUP());
			
			///////
			qualities.add(opSize
				+ "\t"
				+ coveredUP.size()
				+ "\t"
				+ similarity
				+ "\t"
				+ overAssignmentRatio
				+ "\t"
				+ underAssignmentRatio
				+ "\t"
				+ jaccard);
			qualities.add("\n");
			///////
			
			try
			{
				File f=new File(args[3]);
				if (!f.exists()) {
					f.createNewFile();
				}
				
				FileWriter fw = new FileWriter(f.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				
				bw.write("Operations\tCoveredUP\tSyntactic Similarity\tOverassignment Ratio\tUnderassignment Ratio\tJaccard Similarity\n");
				
				for (int i=0; i<qualities.size(); i++)
				{
					bw.write(qualities.get(i));
				}
				bw.close();
				fw.close();
			}
			catch (IOException e)
			{
				////
			}
			prtr("Complete");
		}
		else if (args[0].charAt(1)=='a')
		{
			//-a origAbac matOutput newAbac
			
			File f=new File(args[1]);
			ArrayList<String> newABAC=new ArrayList<String>();
			String unremoves="";
			
			try
			{
				FileReader fr=new FileReader(f);
				BufferedReader buff=new BufferedReader(fr);
				
				String tmp=buff.readLine();
				while (tmp!=null)				//read in user/resource data
				{
					if (!tmp.equals("") && tmp.charAt(0)!='#' && !tmp.startsWith("rule"))
					{
						if (tmp.startsWith("unremovableResourceAttribs"))
							unremoves=tmp;
						else
							newABAC.add(tmp);
					}
					tmp=buff.readLine();
				}
				buff.close();
				fr.close();
				
				
				f=new File(args[2]);
				fr=new FileReader(f);
				buff=new BufferedReader(fr);
				
				int fileSwitch=-1;
				
				tmp=buff.readLine();
				while (tmp!=null)				//read in and create new rules
				{
					String[] parts=(tmp.trim()).split(" ");
					
					if (fileSwitch==-1 && parts[0].equals("TOPIC"))
					{
						buff.readLine();
						fileSwitch=1;
					}
					else if (fileSwitch==1)
					{
						parts=parts[0].split("\t");
						
						newABAC.add(parts[0]);
						fileSwitch=0;
					}
					else if (fileSwitch==0)
					{
						if (tmp.equals(""))
							fileSwitch=2;
					}
					else if (fileSwitch==2)
					{
						parts=(tmp.trim()).split("\t");
						String[] ruleComps = new String[3];
						
						ruleComps=parts[0].split(";");
						
						if (ruleComps[2].trim().equals(""))
						{
							ruleComps[2]="";
						}
						
						String finishedRule="rule(MAT; "+ruleComps[0]+";"+ruleComps[1]+"; {"+newABAC.get(newABAC.size()-1)+"};"+ruleComps[2]+")";
						
						newABAC.set(newABAC.size()-1, finishedRule);
						
						prtr(finishedRule);
						fileSwitch=-1;
					}
					
					
					tmp=buff.readLine();
				}
				buff.close();
				fr.close();
				
				//write new abac file
				f=new File(args[3]);
				if (!f.exists())
					f.createNewFile();
				
				FileWriter fw=new FileWriter(f);
				BufferedWriter bw=new BufferedWriter(fw);
				for (String s : newABAC)
				{
					bw.write(s);
					bw.write("\n");
				}
				bw.write(unremoves);
				
				bw.flush();
				bw.close();
				fw.flush();
				fw.close();
			}
			catch(IOException e)
			{
				//
			}
			
		}
	}
	
	// output a MATLAB program that will create an author-topic model.
	// abacFile = name of .abac file to read
	// logFile = name of log file to read
	// outFile = base name of output files
	// numTopics = number of topics
	// alpha = a parameter of the ATM algorithm
	// beta = a parameter of the ATM algorithm
	// numIter = a parameter of the ATM algorithm
	// seed = random seed (a parameter of the ATM algorithm)
	// verbosity = a parameter of the ATM algorithm
	//    0=no output; 1=iterations; 2=all output
	@SuppressWarnings("unchecked")
	public static void createMATLABpgm(String abacFile, String logFile, String outFile, int numTopics, double alpha, double beta, int numIter, int seed, int verbosity) 
	{
		Parser.config=new Config();
		Parser.parseInputABACFile("./" + abacFile);
		
		Parser.parseInputLogFile("./" + logFile);

		Config config = Parser.config;
		// construct UAEs, RAEs, and constraints to use as components of authors
		ArrayList<ArrayList<AttrValConjunct>> UAEs = constructAttrExprs(config.getUserAttrSet(), AttributeClass.user);		
		ArrayList<ArrayList<AttrValConjunct>> RAEs = constructAttrExprs(config.getResourceAttrSet(), AttributeClass.resource);
		ArrayList<ArrayList<AttrAttrConjunct>> constraints = constructConstraints(config.getUserAttrSet(), config.getResourceAttrSet());
		
		prtr("UAE.size="+UAEs.size()+"\nRAE.size="+RAEs.size()+"\nconstraints.size="+constraints.size());
		stats.add(UAEs.size());
		stats.add(RAEs.size());
		stats.add(constraints.size());
		
		/*
		writeConjuncts(UAEs, "./atm/online-video_6_0-uaes.txt", true);
		writeConjuncts(RAEs, "./atm/online-video_6_0-raes.txt", true);
		writeConjuncts(constraints, "./atm/online-video_6_0-cons.txt", false);
		*/
		// construct authors using the UAEs, RAEs, and constraints constructed above
		HashSet<Author> allAuthors = new HashSet<Author>();
		for (ArrayList<AttrValConjunct> uae : UAEs) 
		{
			for (ArrayList<AttrValConjunct> rae : RAEs)
			{
				for (ArrayList<AttrAttrConjunct> con : constraints) 
				{
					if (twoPlus==true)
					{
						if (areTwoOfAllSatisfied(uae, rae, con, config))
						{
							allAuthors.add(new Author(uae, rae, con, 0));
						}
					}
					else
						allAuthors.add(new Author(uae, rae, con, 0));					
				}
			}
		}
		prtr("allAuthors.size="+allAuthors.size());
		/*UAEs=(ArrayList<ArrayList<AttrValConjunct>>) cleanUp();
		RAEs=(ArrayList<ArrayList<AttrValConjunct>>) cleanUp();
		constraints=(ArrayList<ArrayList<AttrAttrConjunct>>) cleanUp();*/
		stats.add(allAuthors.size());

		// words correspond to operations.  we need to index the words.
		// hashset iterators do not guarantee consistent iteration order,
		// so put the words in an arraylist.
		ArrayList<String> wordList = new ArrayList<String>(config.getOps());

		// Documents correspond to user-resource pairs; the document
		// corresponding to a user-resource pair $\tuple{u,r}$ is the sequence
		// of operations performed (according to the log) by user $u$ on
		// resource $r$.  we summarize this sequence by storing the number of
		// times that u performed each operation on r.  the absolute values of
		// those numbers are not significant; the ratios are.  since the
		// numbers need to be integers, we construct them by multiplying the
		// relative frequencies by a scaling factor and then rounding.  the
		// scaling factor is the reciprocal of the smallest
		// frequency.

		// compute scale factor = reciprocal of smallest frequency.
		double minFreq = 1;
		for (Triple<String, String, String> up : config.getLogInducedUP()) 
		{
			double freq = config.getLogToProbability().get(up);
			if (freq < minFreq) minFreq = freq;
		}
		double scaleFactor = 1.0 / minFreq;

		// construct documents.  maps a pair of a user and a resource to the
		// associated document.
		HashMap<Pair<String,String>,Document> documentMap = new HashMap<Pair<String,String>,Document>();
		for (Triple<String, String, String> up : config.getLogInducedUP())
		{
			
			String u = up.getFirst();
			String op = up.getSecond();
			String r = up.getThird();
			Pair<String,String> ur = new Pair<String,String>(u,r);
			Document doc = documentMap.get(ur);
			if (doc == null) 
			{
				// create a new document for this user-resource pair.
				HashMap<Integer,Integer> wordCount = new HashMap<Integer,Integer>();
				// For the document corresponding to $tuple{u,r}$, the set of authors
				// is the set of $ tuple{uae, rae, con}$ tuples such that
				// $u models uae land r models rae land u,rmodels con$.
				HashSet<Author> authors = new HashSet<Author>();
				for (Author author : allAuthors) 
				{
					try
					{
						if (Parser.satisfyingUAE(u, author.uae, config) && Parser.satisfyingRAE(r, author.rae, config)
							&& TopicModel.satisfyingConstraints(u, r, author.con, config)) 
						{
							authors.add(author);
						}
					} catch (NullPointerException e) {}
				}
				doc = new Document(u, r, wordCount, authors);
				
				if (authors.size()>=100)
					prtre("Document contains: "+authors.size());
				else if (authors.size()<=0)
					prtre("Document does not contain authors.");
				
				// add an entry for this word to the document for this user-resource
				// pair store the index of the word, rather than the word itself, to
				// avoid looking up the index multiple times later.  add 1 to convert
				// from JAva's 0-based indexing to the required 1-based indexing.
				int wordIdx = wordList.indexOf(op)+1;
				doc.wordCount.put(new Integer(wordIdx), new Integer((int) Math.round(scaleFactor*config.getLogToProbability().get(up))));
				
				documentMap.put(ur,doc);
			}
		}
		config=(Config) cleanUp();
		Parser.config=(Config) cleanUp();
		
		Collection<Document> documents = documentMap.values();
		documentMap=(HashMap<Pair<String, String>, Document>) cleanUp();
		prtr("documents.size="+documents.size());
		

		// the desired information has been constructed.  now output it in a
		// suitable format, in the form of the matrices described in
		// topictoolbox/dataformat.m.

		// <strike>accumulate the MATLAB program in an ArrayList of strings.
		// the content is based on exampleAT1.m.
		// ArrayList<String> pgm = new ArrayList<String>();</strike>
		//
		// write out the .m as the values are created
		try
		{
			File file = new File(outFile+".m");
			if (!file.exists()) 
			{
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);

		// matrices are represented in the sparse matrix format described at
		// http://www.mathworks.com/help/matlab/ref/spconvert.html. in
		// summary, for a 2-D matrix, the format is a text file with 3
		// columms, separated by tabs (or spaces?).  a line "i j v" in the
		// file means that entry [i,j] of the matrix equals v.  other entries
		// are zero.

		// construct MATLAB statements defining WS and DS.  WS is a 1 x |N|
		// vector where WS(k) contains the vocabulary index of the kth word
		// token, and N is the number of word tokens.  DS is a 1 x |N| vector
		// where DS(k) contains the document index of the kth word token.
		// indexing of words (vocabulary) is 1-based, not 0-based.
			StringBuffer WS = new StringBuffer("WS = [ ");
			StringBuffer DS = new StringBuffer("DS = [ ");
			for (Document d : documents) 
			{
				for (Map.Entry<Integer, Integer> entry : d.wordCount.entrySet()) 
				{
					int wordIdx = entry.getKey().intValue();
					int count = entry.getValue().intValue();
					// the next <count> tokens are occurrences of <word> in <doc>
					for (int i = 1; i<=count; i++) 
					{
						WS.append(wordIdx + " ");
						DS.append(d.index + " ");
					}
				}
				WS.append(" ... \n");
				DS.append(" ... \n");
			}
			WS.append("];\n");
			DS.append("];\n");
			
			bw.write(WS.toString());
			bw.write(DS.toString());
			
			WS=(StringBuffer) cleanUp();
			DS=(StringBuffer) cleanUp();
	
			// construct MATLAB statements defining AD.  AD is a |A| x |D| sparse
			// matrix, where |A| is the number of distinct authors and |D| is the
			// number of documents. AD(a,d) = 1 when author a is present on
			// document d and zero otherwise.
	
			bw.write("ADsparse = [");
			// put no separator before the first row.  put a semicolon as a
			// separator before subsequent rows.
			String rowSeparator = "";
			for (Document dcm : documents) 
			{
				for (Author author : dcm.authors) 
				{
					bw.write(rowSeparator + author.index + " " + dcm.index + " " + "1");
					rowSeparator = ";\n";
				}
			}
			bw.write("];\n");
			
			bw.write("AD = spconvert(ADsparse);\n");
			
			documents=(Collection<Document>) cleanUp();
	
			// construct MATLAB statement defining WO.  WO is a 1 x |W| cell array
			// of strings where |WO{k}| contains the kth vocabulary item and |W| is
			// the number of distinct vocabulary items.
			bw.write("WO = {");
			for (String w : wordList) {
				bw.write("'" + w + "' ... \n");
			}
			bw.write("};\n");
			
			// construct MATLAB statement defining AN.  AN is a 1 x |A| cell array
			// of strings where AN{k} contains the kth author name, and |A| is the
			// number of distinct authors.
			bw.write("AN = {");
			for (Author author : allAuthors) {
				bw.write("'" + author.toString() + "';\n");
			}
			bw.write("};\n");
			
			allAuthors=(HashSet<Author>) cleanUp();
			cleanUp();
	
			// construct MATLAB statement defining filename to contain textual
			// description of the generated author-topic model.
			bw.write("filename = '../atOut/" + outFile.substring(outFile.lastIndexOf('/')+1) + "-Out.txt';\n");
	
			// Set the parameters of the author-topic algorithm			
			bw.write("T = " + numTopics + ";\n");
			bw.write("ALPHA = " + alpha + ";\n");
			bw.write("BETA = " + beta + ";\n");
			bw.write("N = " + numIter + ";\n");
			bw.write("SEED = " + seed + ";\n");
			bw.write("OUTPUT = " + verbosity + ";\n");
			// "tic" and "toc" cause MATLAB to display the elapsed time
			bw.write("tic\n");
			
			// call the main author-topic model algorithm.  here is the
			// interpretation of its outputs, copied from dataformat.m:
			// WP is a sparse matrix of size |W| x |T|, where |W| is the number of
			// words in the vocabulary and |T| is the number of topics. |WP(i,j)|
			// contains the number of times word |i| has been assigned to topic |j|.   
			// AT is a sparse |A| x |T| matrix, where |A| is the number of
			// |authors.  AT(a,j)| contains the number of times a word token
			// |associated with author |a| has // been assigned to topic |j|.
			// Z is a 1 x |N| vector containing the topic assignments where |N|
			// is the number of word tokens. |Z(k)| contains the topic assignment
			// for token |k|.
			// X is a 1 x |N| vector containing the author assignments where |N|
			// is the number of word tokens. |X(k)| contains the author assignment
			// for token |k|.
			
			bw.write("[ WP, AT , Z , X ] = GibbsSamplerAT( WS , DS , AD , T , N , ALPHA , BETA , SEED , OUTPUT );\n");
			bw.write("toc\n");
			
			// save detailed results to a file in binary MATLAB-readable format, 
			// in case we want to examine them later.
			bw.write("save '../atOut/" + outFile.substring(outFile.lastIndexOf('/')+1) + "-Out' WP AT Z X ALPHA BETA SEED N;\n");
			
			// the following statements are copied from exampleAT1.m.  I didn't
			// examine them in detail.

			bw.write("WPM{1} = WP; WPM{2} = AT;\n");
			bw.write("BETAM(1)=BETA; BETAM(2) = ALPHA;\n");
			bw.write("WOM{1}=WO; WOM{2}=AN;\n");
			
			// Write the word topic and author topic distributions to a text file,
			// converting word indices to words, and author indices to authors.
			
			bw.write("[ SM ] = WriteTopicsMult( WPM , BETAM , WOM , "+wordList.size()+" , 1.1 , 1 , filename );\n");
			wordList=(ArrayList<String>) cleanUp();
			
			///////////
			bw.flush();
			bw.close();
			fw.flush();
			fw.close();
		
		} catch (IOException e) {
			//
		}		
	}	// end createMATLABpgm
	
	public static Object cleanUp()
	{
		System.runFinalization();
		System.gc();
		
		//prtr("Free: "+Runtime.getRuntime().freeMemory()+" - Max: " + Runtime.getRuntime().maxMemory() + " - Total: "+Runtime.getRuntime().totalMemory());
		
		return null;
	}
	
	public static void prtr(String s)
	{
		System.out.println(s);
	}
	public static void prtre(String s)
	{
		System.err.println(s);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void writeConjuncts(ArrayList conj, String out, boolean version)
	{
		File f = new File(out);
		try
		{
			if (!f.exists())
			{
				f.createNewFile();
			}
			
			FileWriter fw=new FileWriter(f);
			BufferedWriter buff=new BufferedWriter(fw);
			
			if (version)
			{
				for (ArrayList<AttrValConjunct> ls : (ArrayList<ArrayList<AttrValConjunct>>) conj)
				{
					for (AttrValConjunct avc : ls)
					{
						buff.write(avc.toString()+"\n");
					}
					buff.write("\n-----------------------------\n\n");
				}
			}
			else
			{
				for (ArrayList<AttrAttrConjunct> ls : (ArrayList<ArrayList<AttrAttrConjunct>>) conj)
				{
					for (AttrAttrConjunct aac: ls)
					{
						buff.write(aac.toString()+"\n");
					}
					buff.write("\n-----------------------------\n\n");
				}
			}
			
			buff.flush();
			buff.close();
			fw.flush();
			fw.close();
		}
		catch (IOException e)
		{
			
		}
	}
	
	@SuppressWarnings("rawtypes")
	static ArrayList stats;
	@SuppressWarnings("rawtypes")
	public static void writeStats(String fname)
	{
		fname=fname.substring(fname.lastIndexOf('/')+1, fname.lastIndexOf('.'));
		fname="./atm/"+fname+".output";
		
		try
		{
			File f = new File(fname);
			if (!f.exists())
				f.createNewFile();
			
			FileWriter fw=new FileWriter(f);
			BufferedWriter buff=new BufferedWriter(fw);
			
			//buff.write("Order is: UAEs, UAE conjuncts, RAEs, RAE conjuncts...\n\n");
			
			buff.write("UAE conjuncts:\t"+stats.get(1));
			buff.write("\nUAEs:\t"+stats.get(0));
			buff.write("\nRAE conjuncts:\t"+stats.get(3));
			buff.write("\nRAEs:\t"+stats.get(2));
			buff.write("\nAtomic constraints:\t"+stats.get(5));
			buff.write("\nConstraints:\t"+stats.get(4));
			buff.write("\nUAEs.size:\t"+stats.get(6));
			buff.write("\nRAEs.size:\t"+stats.get(7));
			buff.write("\nconstraints.size:\t"+stats.get(8));
			buff.write("\nallAuthors.size:\t"+stats.get(9));
			buff.write("\n"+stats.get(10));
			
			buff.flush();
			buff.close();
			fw.flush();
			fw.close();
		}
		catch (IOException e)
		{
			
		}
		
		stats=new ArrayList();
		cleanUp();
	}
	
	/*@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean satisfyChecker(ArrayList conj, boolean ver, Config conf)
	{
		boolean result=false;
		int counter=0;
		
		for (Triple<String, String, String> up : conf.getLogInducedUP())
		{
			
			String u = up.getFirst();
			String r = up.getThird();
			
			if (ver == true)
			{
				try
				{
					if (Parser.satisfyingUAE(u, conj, conf))
						counter++;
					
					if (Parser.satisfyingRAE(r, conj, conf))
						counter++;
				}
				catch(NullPointerException e) {}
			}
			else
			{
				
				
				//if (TopicModel.satisfyingConstraints(u, r, conj, conf))
					
	
			}
			if (counter == 2)
			{
				result=true;
				break;
			}
		}
		
		return result;
	}*/
	
	public static boolean areTwoUsersSatisfied(ArrayList<AttrValConjunct> conj, Config conf)
	{
		int ctr=0;
		
		for (String uid : conf.getUserAttrSet().get("uid").getDomain()) 
		{
			if (ctr>=2)
			{
				return true;
			}
			else if (Parser.satisfyingUAE(uid, conj, conf))
			{
				ctr++;
			}
		}
		
		if (ctr>=2)
			return true;
		else
			return false;
	}
	public static boolean areTwoResrSatisfied(ArrayList<AttrValConjunct> conj, Config conf)
	{
		int ctr=0;
		
		for (String rid : conf.getResourceAttrSet().get("rid").getDomain()) 
		{
			if (ctr>=2)
			{
				return true;
			}
			else if (Parser.satisfyingRAE(rid, conj, conf))
			{
				ctr++;
			}
		}
		
		if (ctr>=2)
			return true;
		else
			return false;
	}
	public static boolean areTwoConsSatisfied(ArrayList<AttrAttrConjunct> conj, Config conf)
	{
		int ctr=0;
		
		for (String uid: conf.getUserAttrSet().get("uid").getDomain())
		{
			for (String rid: conf.getResourceAttrSet().get("rid").getDomain())
			{
				
				if (ctr>=2)
				{
					return true;
				}
				else if (TopicModel.satisfyingConstraints(uid, rid, conj, conf))
				{
					ctr++;
				}
				
			}
		}
		
		if (ctr>=2)
			return true;
		else
			return false;
	}
	public static boolean areTwoOfAllSatisfied(ArrayList<AttrValConjunct> uae, ArrayList<AttrValConjunct> rae, ArrayList<AttrAttrConjunct> con, Config conf)
	{
		int ctr=0;
		
		for (String uid: conf.getUserAttrSet().get("uid").getDomain())
		{
			if (Parser.satisfyingUAE(uid, uae, conf))
			{
				for (String rid: conf.getResourceAttrSet().get("rid").getDomain())
				{
					if (Parser.satisfyingRAE(rid, rae, conf))
					{
						if (ctr>=2)
							return true;
						else if (TopicModel.satisfyingConstraints(uid, rid, con, conf))
						{
							ctr++;
							break;
						}
					}
				}
			}
		}
		
		if (ctr>=2)
			return true;
		else
			return false;
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
	
} // end class TopicModel
