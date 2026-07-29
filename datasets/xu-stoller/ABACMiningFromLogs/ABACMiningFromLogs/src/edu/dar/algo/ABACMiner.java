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

package edu.dar.algo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Random;

import edu.dar.util.AttrAttrConjunct;
import edu.dar.util.AttrValConjunct;
import edu.dar.util.Attribute;
import edu.dar.util.AttributeType;
import edu.dar.util.CaseStudyGenerator;
import edu.dar.util.Config;
import edu.dar.util.LogEntry;
import edu.dar.util.Operator;
import edu.dar.util.Pair;
import edu.dar.util.Parser;
import edu.dar.util.PolicyQualityValue;
import edu.dar.util.Rule;
import edu.dar.util.RulePairComparator;
import edu.dar.util.RuleQualityMetricType;
import edu.dar.util.RuleQualityValue;
import edu.dar.util.SyntheticPolicyCaseStudyGenerator;
import edu.dar.util.Triple;
import edu.dar.util.ValueType;

public class ABACMiner {
	public static int numElimConjuncts = 0;
	public static int numElimValues = 0;
	public static int numElimElements = 0;
	public static int numElimConstraints = 0;
	public static int numGeneralizations = 0;
	public static int numMerges = 0;
	public static int numMergeRulesCalls = 1;
	public static int numSubsumptionCheck = 0;
	public static int numUnsuccessfulMerges = 0;
	public static int numRemovedPairs = 0;
	public static int numExtraCompares = 0;

	public static long startTime;
	public static long endTime;
	public static long totalTime = 0;
	public static long duration;

	public static boolean debug = false;
	public static boolean newElimConjunct = true;

	public static double underAssignFrac = 0.0;
	public static int overAssignThres = 0;
	public static double W0_rule = 1.2;
	public static double W0_policy = 10.0;
	public static double W1_policy = 1.0;
	public static ArrayList<Rule> resultRules;

	public static final HashSet<String> topSet;

	public static HashSet<Triple<String, String, String>> overAssignments;

	public static final double[] noiseRatios = new double[] { /* 0.01, 0.02, */
	0.03, 0.06, 0.09, 0.12, 0.15, 0.18, 0.21 };

	public static final double[] TAU = new double[] { 1, 2, 3, 4, 5, 6, 7, 8,
			9, 10, 11, 12, 13, 14, 15 };
	public static final double[] ALPHA = new double[] { 0.01, 0.02, 0.03, 0.04,
			0.05, 0.06, 0.07, 0.08 /*
									 * , 0.09, 0.10, 0.15, 0.18, 0.21
									 */};
	public static int NUMBER_OF_TEST_CASES = 10;
	public static int NUMBER_RULE_METRICS = 3;
	public static int NUMBER_OF_NOISES = 1;
	public static int NUMBER_OF_STEPS = 5;
	public static final String VERBOSE_OPTION = "-verbose";
	public static final int[] weights = { 15, 20, 25, 30, 35 };

	private static final String SEPARATOR_STRING = "=================================";
	// top set is a special set that contains "TOP"
	static {
		topSet = new HashSet<String>();
		topSet.add("TOP");
	}

	/**
	 * add noisePercentage * |up0| numbers of overassignment noise
	 * 
	 * @param config
	 * @param noisePercentage
	 */
	public static void addOverassignmentNoise(Config config,
			double noisePercentage) {
		int numNoise = (int) (config.getCoveredUP().size() * noisePercentage);

		while (config.getOverassignmentUP().size() < numNoise) {
			String user = randomElement(config.getUsers());
			String op = randomElement(config.getOps());
			String res = randomElement(config.getResources());
			Triple<String, String, String> userPerm = new Triple<String, String, String>(
					user, op, res);
			if (!config.getCoveredUP().contains(userPerm)) {
				config.getOverassignmentUP().add(userPerm);
				config.getCoveredUP().add(userPerm);
				config.getUserPerms().get(user)
						.add(new Pair<String, String>(op, res));
				if (!config.getPermUsers().containsKey(
						new Pair<String, String>(op, res))) {
					config.getPermUsers().put(
							new Pair<String, String>(op, res),
							new HashSet<String>());
				}
				config.getPermUsers().get(new Pair<String, String>(op, res))
						.add(user);
			}
		}
	}

	/**
	 * add noisePercentage * |up0| numbers of underassignment noise
	 * 
	 * @param config
	 * @param noisePercentage
	 */
	public static void addUnderassignmentNoise(Config config,
			double noisePercentage) {
		int numNoise = (int) (config.getCoveredUP().size() * noisePercentage);

		while (config.getUnderassignmentUP().size() < numNoise
				&& !config.getCoveredUP().isEmpty()) {
			Triple<String, String, String> element = randomElement(config
					.getCoveredUP());
			config.getUnderassignmentUP().add(element);
			config.getCoveredUP().remove(element);
			config.getUserPerms()
					.get(element.getFirst())
					.remove(new Pair<String, String>(element.getSecond(),
							element.getThird()));
			config.getPermUsers()
					.get(new Pair<String, String>(element.getSecond(), element
							.getThird())).remove(element.getFirst());
		}
	}

	public static <T> T randomElement(HashSet<T> S) {
		if (S == null) {
			return null;
		}
		int size = S.size();
		int item = new Random(System.currentTimeMillis()).nextInt(size);
		int i = 0;
		T result = null;
		for (T obj : S) {
			if (i == item) {
				result = obj;
				break;
			}
			i = i + 1;
		}
		return result;
	}

	public static void mineABACPolicy(Config config, boolean dbg,
			Integer qualityMetricType) {
		debug = dbg;
		// Line 1 Rules is the set of candidate rules
		LinkedList<Rule> Rules = new LinkedList<Rule>();
		// Line 2 and 3
		// uncovUP containts user-permission tuples in UP(L) that are not
		// covered by Rules, initially it is equal to UP(L)
		HashSet<Triple<String, String, String>> uncovUP = new HashSet<Triple<String, String, String>>(
				config.getLogInducedUP());
		if (debug) {
			// print the size of the log induced
			System.out.println(SEPARATOR_STRING);
			System.out.println("Log induced UP size: " + uncovUP.size());
		}

		// hashmap that maps permission to users
		HashMap<Pair<String, String>, HashSet<String>> permUsers = new HashMap<Pair<String, String>, HashSet<String>>();
		for (Triple<String, String, String> up : config.getLogInducedUP()) {
			Pair<String, String> perm = new Pair<String, String>(
					up.getSecond(), up.getThird());
			if (!permUsers.keySet().contains(perm)) {
				permUsers.put(perm, new HashSet<String>());
			}
			permUsers.get(perm).add(up.getFirst());
		}

		// hashmap that maps user to permissions
		HashMap<String, HashSet<Pair<String, String>>> userPerms = new HashMap<String, HashSet<Pair<String, String>>>();
		for (Triple<String, String, String> up : config.getLogInducedUP()) {
			String user = up.getFirst();
			if (!userPerms.keySet().contains(user)) {
				userPerms.put(user, new HashSet<Pair<String, String>>());
			}
			userPerms.get(user).add(
					new Pair<String, String>(up.getSecond(), up.getThird()));
		}

		RuleQualityMetricType qType;
		switch (qualityMetricType) {
		case 0:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_COVERED;
			break;
		case 1:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_MEANING;
			break;
		case 2:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_UP0;
			break;
		case 3:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_PERMISSIONS;
			break;
		case 4:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_SIZE_AND_PERMISSIONS;
			break;
		case 5:
			qType = RuleQualityMetricType.Q_PROGOL;
			break;
		case 6:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_COVERED_FREQ;
			break;
		case 7:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_MEANING_FREQ;
			break;
		case 8:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_UP0_FREQ;
			break;
		case 9:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_PERMISSIONS_FREQ;
			break;
		case 10:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_SIZE_AND_PERMISSIONS_FREQ;
			break;
		default:
			qType = RuleQualityMetricType.Q_RELATIVE_TO_COVERED;
			break;
		}

		// Main loop to generate candidate rules
		while (!uncovUP.isEmpty()) {
			// select an uncovered user-permission tuple
			ArrayList<Triple<String, String, String>> coveredUPList = new ArrayList<Triple<String, String, String>>(
					uncovUP);

			// select a user-perm seed
			String userSeed = coveredUPList.get(0).getFirst();
			Pair<String, String> permSeed = new Pair<String, String>(
					coveredUPList.get(0).getSecond(), coveredUPList.get(0)
							.getThird());

			// expand user to a user set
			HashSet<String> users = new HashSet<String>(permUsers.get(permSeed));
			ArrayList<String> usersList = new ArrayList<String>(users);

			if (debug) {
				System.out.println(SEPARATOR_STRING);
				System.out
						.println("Select an uncovered user-permission tuple:");
				System.out.println("<u,p>=" + "<" + userSeed + "," + permSeed
						+ ">");
			}

			// compute candidate constraints
			ArrayList<AttrAttrConjunct> cc = candidateConstraint(userSeed,
					permSeed.getSecond(), config);
			Collections.sort(cc);

			if (debug) {
				System.out.println(SEPARATOR_STRING);
				System.out.println("Candidate constraint:");
				System.out.println(cc);
			}

			// Line 7-8 s_u contains users for which permSeed is uncovered and
			// that have the same candidate constraint as userSeed
			HashSet<String> s_u = new HashSet<String>();
			s_u.add(userSeed);

			for (int i = 1; i < usersList.size(); i++) {
				ArrayList<AttrAttrConjunct> constraints = candidateConstraint(
						usersList.get(i), permSeed.getSecond(), config);
				Collections.sort(constraints);
				if (constraints.equals(cc)) {
					s_u.add(usersList.get(i));
				}
			}

			if (debug) {
				System.out.println(SEPARATOR_STRING);
				System.out
						.println("The set of users which "
								+ permSeed
								+ " is uncovered and that have the same candidate constraint as "
								+ userSeed);
				System.out.println(s_u);
			}

			// Line 9 compute UAE
			ArrayList<AttrValConjunct> uae = computeUAE(s_u, config);

			// Line 10 compute RAE
			HashSet<String> rSet = new HashSet<String>();
			rSet.add(permSeed.getSecond());
			ArrayList<AttrValConjunct> pae = computeRAE(rSet, config);

			// Line 10 set operation set
			Rule r = new Rule();
			r.setUAE(uae);
			r.setRAE(pae);
			HashSet<String> ops = new HashSet<String>();
			ops.add(permSeed.getFirst());
			r.setOps(ops);

			if (debug) {
				System.out.println(SEPARATOR_STRING);
				System.out.println("Result of computeUAE:");
				System.out.println(uae);
				System.out.println(SEPARATOR_STRING);
				System.out.println("Result of computePAE:");
				System.out.println(pae);
			}

			Rule gr = null;

			// line 12 first call of generalize rule
			gr = generalizeRule(r, cc, config, uncovUP, qType, permUsers);
			gr.setUaeChanged(true);
			gr.setRaeChanged(true);
			numGeneralizations += gr.getCon().size();

			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Generalized Rule: \n" + gr);
			}

			// line 13
			Rules.add(gr);
			// update covered UP of gr and uncovUP
			gr.setCoveredUPTriple(computeCoveredUPTriple(gr, config));
			HashSet<Triple<String, String, String>> coveredLogTriples = new HashSet<Triple<String, String, String>>(
					gr.getCoveredUPTriple());
			coveredLogTriples.retainAll(config.getLogInducedUP());
			gr.setCoveredLogTriple(coveredLogTriples);
			uncovUP.removeAll(gr.getCoveredUPTriple());

			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Remove UP Set: " + gr.getCoveredUPTriple());
				if (debug) {
					System.out
							.println("==============================================");
					System.out.println("Remaining Uncovered UP Size: "
							+ uncovUP.size());
				}
			}

			HashSet<String> opSet = new HashSet<String>();
			opSet.add(permSeed.getFirst());

			HashSet<Pair<String, String>> perms = new HashSet<Pair<String, String>>(
					userPerms.get(userSeed));
			ArrayList<Pair<String, String>> permList = new ArrayList<Pair<String, String>>(
					perms);
			// line 14
			for (int i = 0; i < permList.size(); i++) {
				if (permList.get(i).getSecond().equals(permSeed.getSecond())) {
					opSet.add(permList.get(i).getFirst());
				}
			}

			if (debug) {
				System.out
						.println("==============================================");
				System.out
						.println("The set of perms which "
								+ userSeed
								+ " is uncovered and that have the same candidate constraint as "
								+ permSeed);
				System.out.println(opSet);
			}

			// Line 15-17
			r = new Rule();
			HashSet<String> uSet = new HashSet<String>();
			uSet.add(userSeed);
			uae = computeUAE(uSet, config);
			r.setUAE(uae);
			r.setRAE(pae);
			r.setOps(opSet);
			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Result of computeUAE:");
				System.out.println(uae);
				System.out
						.println("==============================================");
				System.out.println("Result of computePAE:");
				System.out.println(pae);
			}
			// Line 18 second call to generalize rule
			gr = generalizeRule(r, cc, config, uncovUP, qType, permUsers);
			gr.setUaeChanged(true);
			gr.setRaeChanged(true);
			numGeneralizations += gr.getCon().size();
			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Generalized Rule: \n" + gr);
			}

			// line 19
			Rules.add(gr);

			gr.setCoveredUPTriple(computeCoveredUPTriple(gr, config));
			uncovUP.removeAll(gr.getCoveredUPTriple());
			coveredLogTriples = new HashSet<Triple<String, String, String>>(
					gr.getCoveredUPTriple());
			coveredLogTriples.retainAll(config.getLogInducedUP());
			gr.setCoveredLogTriple(coveredLogTriples);

			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Remaining Uncovered UP Size: "
						+ uncovUP.size());
			}
		}

		computeDuration();
		if (debug) {
			System.out
					.println("==============================================");
			System.out.println("Time for the main while loop : " + duration
					+ " millisec");
		}
		// line 15
		for (Rule r : Rules) {
			Collections.sort(r.getUAE());
			Collections.sort(r.getRAE());
			Collections.sort(r.getCon());
		}

		class RuleQualityComparator implements Comparator<Rule> {
			@Override
			public int compare(Rule r1, Rule r2) {
				if (r1 == r2) {
					return 0;
				}
				return r1.getQuality().compareTo(r2.getQuality());
			}
		}

		if (debug) {
			int nRules = 1;
			System.out
					.println("==============================================");
			System.out.println("BEFORE MERGING RULES\n");
			for (Rule r : Rules) {
				System.out.println(nRules++ + ".\n" + r);
			}
			nRules = 1;
		}

		mergeRules(Rules, config, permUsers, qType);

		if (debug) {
			int nRules = 1;
			System.out
					.println("==============================================");
			System.out.println("Round " + numMergeRulesCalls);
			for (Rule r : Rules) {
				System.out.println(nRules++ + ".\n" + r);
			}
			nRules = 1;
		}
		computeDuration();

		if (debug) {
			System.out
					.println("==============================================");
			System.out.println("Time for the mergeRules: " + duration
					+ " millisec");
		}

		while (simplifyRules(Rules, config, config.getLogInducedUP(), qType,
				permUsers)) {
			computeDuration();
			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Time for simplifyRules Round: "
						+ numMergeRulesCalls + " " + duration + " millisec");
			}
			mergeRules(Rules, config, permUsers, qType);

			if (debug) {
				int nRules = 1;
				System.out
						.println("==============================================");
				System.out.println("Round " + ++numMergeRulesCalls);
				for (Rule r : Rules) {
					System.out.println(nRules++ + ".\n" + r);
				}
				nRules = 1;
			}
			computeDuration();
			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Time for mergeRules Round: "
						+ numMergeRulesCalls + " " + duration + " millisec");
			}
		}

		resultRules = new ArrayList<Rule>();

		uncovUP = new HashSet<Triple<String, String, String>>(
				config.getLogInducedUP());
		for (Rule r : Rules) {
			r.setQuality(computeRuleQuality(r, uncovUP, config, qType,
					permUsers));
		}
		overAssignments = new HashSet<Triple<String, String, String>>();
		while (!uncovUP.isEmpty()) {
			Rule r = Collections.max(Rules, new RuleQualityComparator());
			HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>(
					r.getCoveredUPTriple());
			coveredUP.retainAll(uncovUP);
			Rules.remove(r);
			if (coveredUP.size() == 0) {
				continue;
			}
			resultRules.add(r);

			if (Double.compare(overAssignThres, 0) > 0) {
				HashSet<Triple<String, String, String>> overUP = new HashSet<Triple<String, String, String>>(
						r.getCoveredUPTriple());
				overUP.retainAll(config.getOverassignmentUP());
				if (!overUP.isEmpty()) {
					System.out
							.println("==============================================");
					System.out
							.println("The rule contains overassignment: " + r);
					System.out.println("Overassignments: " + overUP + " size:"
							+ overUP.size());
					System.out.println("r's UP size: "
							+ r.getCoveredUPTriple().size());
					System.out.println("r's rule quality "
							+ r.getQuality().firstComponent);
					System.out.println("r is the " + resultRules.size()
							+ "th rule");
				}

				else {
					System.out
							.println("==============================================");
					System.out
							.println("The rule does not contain overassignment: "
									+ r);
					System.out.println("r's UP size: "
							+ r.getCoveredUPTriple().size());
					System.out.println("r's rule quality "
							+ r.getQuality().firstComponent);
					System.out.println("r is the " + resultRules.size()
							+ "th rule");
				}
			}

			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Adding Rule: \n" + r);
			}

			// uncovUP.removeAll(computeCoveredUPTriple(r, config));
			uncovUP.removeAll(coveredUP);

			if (coveredUP.size() <= overAssignThres) {
				overAssignments.addAll(coveredUP);
			}
			// TODO:modify rule quality
			for (Rule r1 : Rules) {
				r1.setQuality(computeRuleQuality(r1, uncovUP, config, qType,
						permUsers));
			}
		}
		computeDuration();
		if (debug) {
			System.out
					.println("==============================================");
			System.out.println("Time for selection step: " + " " + duration
					+ " millisec");
		}

		if (debug) {
			outputResult(config, resultRules);
		}
		// int numRules = 1;
		// int totalWSC = 0;
		// double wsc = 0;
		//
		// int[] maxSimilarityArray = new int[resultRules.size()];
		//
		// for (int i = 0; i < resultRules.size(); i++) {
		// Rule r1 = resultRules.get(i);
		// double maxSimilarity = -1;
		// double currSimilarity = 0;
		//
		// for (int j = 0; j < config.getRuleList().size(); j++) {
		// Rule r2 = config.getRuleList().get(j);
		// currSimilarity = ruleSimilarity(r1, r2, config);
		// if (Double.compare(currSimilarity, maxSimilarity) > 0) {
		// maxSimilarityArray[i] = j;
		// maxSimilarity = currSimilarity;
		// }
		// }
		// }
		//
		// System.out.println("==============================================");
		// System.out.println("OUTPUT RULES\n");
		// for (Rule r : resultRules) {
		// System.out.println(numRules++ + ".\n" + r);
		// wsc = r.getSize();
		// totalWSC += wsc;
		// System.out.println("WSC=" + wsc);
		// }
		// System.out.println("\ntotal WSC=" + totalWSC);
		//
		// System.out.println("==============================================");
		// System.out.println("INPUT-OUTPUT CORRESPONDENCE");
		// for (int i = 0; i < config.getRuleList().size(); i++) {
		// System.out.println((i + 1) + ". " + config.getRuleList().get(i));
		// System.out.println("SIMILAR OUTPUT RULES:");
		// for (int j = 0; j < maxSimilarityArray.length; j++) {
		// if (maxSimilarityArray[j] == i) {
		// System.out.print(resultRules.get(j));
		// System.out.format(
		// ", similarity: %.2f",
		// ruleSimilarity(config.getRuleList().get(i),
		// resultRules.get(j), config));
		// System.out.println();
		// }
		// }
		// System.out.println();
		// }

		if (debug) {

			System.out
					.println("==============================================");
			System.out
					.println("Rule Quality Metric Type: " + qualityMetricType);
			System.out.println("Consistency Check Result:");
			System.out.println(consistencyCheck(resultRules, config));

			System.out
					.println("==============================================");
			System.out.println("Statistics:");
			System.out.println("Number of calls to mergeRules: "
					+ numMergeRulesCalls);
			System.out.println("Number of generalizations: "
					+ numGeneralizations);
			System.out.println("Number of merges: " + numMerges);
			System.out.println("Number of eliminated conjuncts: "
					+ numElimConjuncts);
			System.out.println("Number of eliminated values: " + numElimValues);
			System.out
					.println("Number of eliminated elements for multi-value: "
							+ numElimElements);
			System.out.println("Number of eliminated constraints: "
					+ numElimConstraints);
			System.out.println("Number of subsumption checks: "
					+ numSubsumptionCheck);
			System.out.println("Number of unsuccessful merges: "
					+ numUnsuccessfulMerges);
			System.out.println("Number of removed pairs: " + numRemovedPairs);
			System.out.println("Number of extra compares: " + numExtraCompares);

			computeDuration();
			System.out
					.println("==============================================");
			System.out.println("Remaining time: " + duration + " millisec");

		}

		if (Double.compare(underAssignFrac, 0) > 0) {
			HashSet<Triple<String, String, String>> underAssignments = new HashSet<Triple<String, String, String>>();
			for (Rule r : resultRules) {
				underAssignments
						.addAll(new HashSet<Triple<String, String, String>>(r
								.getCoveredUPTriple()));
			}
			underAssignments.removeAll(config.getCoveredUP());
			System.out
					.println("==============================================");
			System.out.println("Under Assignment Noise: "
					+ config.getUnderassignmentUP());
			System.out.println("Under Assignments in Output Policy: "
					+ underAssignments);
			System.out
					.println("Jaccard Similary of Under Assignment Noises and Under Assignments in Output Policy is: "
							+ jaccardSimilarity(config.getUnderassignmentUP(),
									underAssignments));

		}

		if (Double.compare(overAssignThres, 0) > 0) {
			System.out
					.println("==============================================");
			System.out.println("Over Assignment Noise: "
					+ config.getOverassignmentUP());
			System.out.println("Over Assignments in Output Policy: "
					+ overAssignments);
			System.out
					.println("Jaccard Similary of Over Assignment Noises and Over Assignments in Output Policy is: "
							+ jaccardSimilarity(config.getOverassignmentUP(),
									overAssignments));

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

	public static boolean simplifyRules(LinkedList<Rule> rules, Config config,
			HashSet<Triple<String, String, String>> uncovUP,
			RuleQualityMetricType type,
			HashMap<Pair<String, String>, HashSet<String>> permUsers) {
		boolean changed = false;
		for (int i = 0; i < rules.size(); i++) {
			Rule r = rules.get(i);
			elimRedundantSets(r.getUAE(), config, AttributeType.UserAttr);
			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Eliminating Conjuncts for:\n" + r);
			}
			if (elimConjuncts(r, rules, config, uncovUP, i, type, permUsers)) {
				changed = true;
				r.setChanged(true);
				r.setUaeChanged(true);
				r.setRaeChanged(true);
				if (debug) {
					System.out
							.println("==============================================");
					System.out
							.println("Result rule after eliminating conjuncts:\n"
									+ rules.get(i));
				}
			}
		}

		for (int i = 0; i < rules.size(); i++) {
			Rule r = rules.get(i);
			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Eliminating Overlap Elements for:\n" + r);
			}
			if (elimOverlapBtwnRules(r, rules, config)) {
				changed = true;
				r.setChanged(true);
				r.setUaeChanged(true);
				r.setRaeChanged(true);
				if (rules.size() > i && !r.equals(rules.get(i))) {
					i--;
				}
			}
		}

		for (int i = 0; i < rules.size(); i++) {
			Rule r = rules.get(i);
			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Eliminating Overlap Ops for:\n" + r);
			}
			if (elimOverlapOpsBtwnRules(r, rules, config)) {
				changed = true;
				r.setChanged(true);
			}
		}

		for (int i = 0; i < rules.size(); i++) {
			Rule r = rules.get(i);
			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Eliminating Constraints for:\n" + r);
			}
			Rule bestRule = elimConstraints(r, r.getCon(), config, uncovUP,
					type, permUsers);
			if (!bestRule.equals(r)) {
				changed = true;
				rules.set(i, bestRule);
				bestRule.setChanged(true);
			}
		}

		return changed;
	}

	public static Rule elimConstraints(Rule r,
			List<AttrAttrConjunct> constraints, Config config,
			HashSet<Triple<String, String, String>> uncovUP,
			RuleQualityMetricType type,
			HashMap<Pair<String, String>, HashSet<String>> permUsers) {
		Rule bestRule = r;
		RuleQualityValue bestQuality = computeRuleQuality(bestRule, uncovUP,
				config, type, permUsers);
		ArrayList<AttrAttrConjunct> validConstraints = new ArrayList<AttrAttrConjunct>(
				constraints);
		for (int i = 0; i < validConstraints.size(); i++) {
			Rule temp = new Rule(r);
			temp.getCon().remove(validConstraints.get(i));
			temp.setChanged(true);
			boolean isValid = false;
			isValid = true;
			if (!isValid) {
				validConstraints.remove(i);
				i--;
			}
		}

		for (int i = 0; i < validConstraints.size(); i++) {
			Rule r1 = new Rule(r);
			r1.getCon().remove(validConstraints.get(i));
			r1.setChanged(true);
			boolean isValid = false;
			isValid = true;
			if (!isValid) {
				continue;
			}
			Rule tempR = elimConstraints(r1,
					validConstraints.subList(i + 1, validConstraints.size()),
					config, uncovUP, type, permUsers);
			if (tempR != null) {
				RuleQualityValue quality = computeRuleQuality(tempR, uncovUP,
						config, type, permUsers);
				if (quality.compareTo(bestQuality) > 0) {
					bestRule = tempR;
					bestQuality = quality;
				}
			}
		}
		return bestRule;
	}

	public static boolean elimElements(Rule r, LinkedList<Rule> rules,
			Config config, RuleQualityMetricType type,
			HashMap<Pair<String, String>, HashSet<String>> permUsers) {
		boolean changed = false;
		RuleQualityValue currentValue = computeRuleQuality(r,
				config.getLogInducedUP(), config, type, permUsers);
		HashSet<AttrValConjunct> removeConjunctSet = new HashSet<AttrValConjunct>();
		for (String a : config.getUserAttrSet().keySet()) {
			if (config.getUserAttrSet().get(a).getvType() == ValueType.Single) {
				continue;
			}
			for (AttrValConjunct c : r.getUAE()) {
				if (c.getLHS().equals(a)) {
					for (HashSet<String> s : c.getRHSet()) {
						HashSet<String> temp = new HashSet<String>(s);
						for (String v : temp) {
							s.remove(v);
							r.setUaeChanged(true);
							boolean uaeChanged = r.isUaeChanged();
							boolean paeChanged = r.isRaeChanged();
							boolean rchanged = r.isChanged();
							boolean isValid = false;
							RuleQualityValue tempValue = computeRuleQuality(r,
									config.getLogInducedUP(), config, type,
									permUsers);
							isValid = true;
							if (tempValue.compareTo(currentValue) > 0) {
								s.add(v);
								r.setUaeChanged(uaeChanged);
								r.setRaeChanged(paeChanged);
								r.setChanged(rchanged);
							} else {
								changed = true;
								numElimElements++;
								if (debug) {
									System.out
											.println("==============================================");
									System.out.println("Removed Element " + v
											+ " from Rule " + r + " Conjunct "
											+ c);
								}
								if (s.isEmpty()) {
									removeConjunctSet.add(c);
									break;
								}
							}
						}
					}
				}
			}
			r.getUAE().removeAll(removeConjunctSet);
		}
		return changed;
	}

	public static boolean elimOverlapOpsBtwnRules(Rule r,
			LinkedList<Rule> rules, Config config) {
		boolean changed = false;
		for (Rule r1 : rules) {
			if (r == r1) {
				continue;
			}
			if (!(new HashSet<AttrAttrConjunct>(r.getCon()))
					.containsAll((new HashSet<AttrAttrConjunct>(r1.getCon())))) {
				continue;
			}
			if (!(r.getUserAttrs().containsAll(r1.getUserAttrs()))) {
				continue;
			}
			if (!(r.getPermAttrs().containsAll(r1.getPermAttrs()))) {
				continue;
			}
			HashSet<Triple<String, String, String>> s1 = new HashSet<Triple<String, String, String>>(
					r1.getCoveredUPTriple());
			s1.removeAll(config.getLogInducedUP());
			if (s1.size() > 0) {
				continue;
			}

			boolean isSubset = true;
			for (AttrValConjunct c1 : r1.getUAE()) {
				String uattr = c1.getLHS();
				for (AttrValConjunct c2 : r.getUAE()) {
					if (c2.getLHS().equals(uattr)) {
						if (config.getUserAttrSet().get(uattr).getvType() == ValueType.Single) {
							if (!c1.getRHS().containsAll(c2.getRHS())) {
								isSubset = false;
								break;
							}
						} else {
							if (!c1.getRHSet().containsAll(c2.getRHSet())) {
								isSubset = false;
								break;
							}
						}
					}
				}
				if (isSubset == false) {
					break;
				}
			}
			if (isSubset == false) {
				continue;
			}
			for (AttrValConjunct c1 : r1.getRAE()) {
				String pattr = c1.getLHS();
				for (AttrValConjunct c2 : r.getRAE()) {
					if (c2.getLHS().equals(pattr)) {
						if (config.getResourceAttrSet().get(pattr).getvType() == ValueType.Single) {
							if (!c1.getRHS().containsAll(c2.getRHS())) {
								isSubset = false;
								break;
							}
						} else {
							if (!c1.getRHSet().containsAll(c2.getRHSet())) {
								isSubset = false;
								break;
							}
						}
					}
				}
				if (isSubset == false) {
					break;
				}
			}
			if (isSubset == false) {
				continue;
			}
			HashSet<String> removeVals = new HashSet<String>();
			for (String op : r.getOps()) {
				if (!r1.getOps().contains(op)) {
					continue;
				} else {
					removeVals.add(op);
				}
			}
			r.getOps().removeAll(removeVals);
			numElimElements += removeVals.size();
			if (!removeVals.isEmpty()) {
				changed = true;
			}
			if (r.getOps().isEmpty()) {
				rules.remove(r);
				return true;
			}
		}
		return changed;
	}

	public static boolean elimOverlapBtwnRules(Rule r, LinkedList<Rule> rules,
			Config config) {
		boolean changed = false;
		for (AttrValConjunct c : r.getUAE()) {
			String attr = c.getLHS();
			if (config.getUserAttrSet().get(attr).getvType() == ValueType.Single) {
				HashSet<String> vals = c.getRHS();
				HashSet<String> removeVals = new HashSet<String>();
				for (String val : vals) {
					for (Rule r1 : rules) {
						if (r1.equals(r)) {
							continue;
						}
						if (!r.getCon().containsAll(r1.getCon())) {
							continue;
						}
						if (!(r.getUserAttrs().containsAll(r1.getUserAttrs()))) {
							continue;
						}
						if (!(r.getPermAttrs().containsAll(r1.getPermAttrs()))) {
							continue;
						}
						if (!r1.getUserAttrs().contains(attr)) {
							continue;
						}
						if (!r1.getOps().containsAll(r.getOps())) {
							continue;
						}
						HashSet<Triple<String, String, String>> s1 = new HashSet<Triple<String, String, String>>(
								r1.getCoveredUPTriple());
						s1.removeAll(config.getLogInducedUP());
						if (s1.size() > 0) {
							continue;
						}
						boolean isSubset = true;
						for (AttrValConjunct c1 : r1.getUAE()) {
							String uattr = c1.getLHS();
							if (uattr.equals(attr)) {
								if (!c1.getRHS().contains(val)) {
									isSubset = false;
									break;
								} else {
									continue;
								}
							}
							for (AttrValConjunct c2 : r.getUAE()) {
								if (c2.getLHS().equals(uattr)) {
									if (config.getUserAttrSet().get(uattr)
											.getvType() == ValueType.Single) {
										if (!c1.getRHS().containsAll(
												c2.getRHS())) {
											isSubset = false;
											break;
										}
									} else {
										if (!c1.getRHSet().containsAll(
												c2.getRHSet())) {
											isSubset = false;
											break;
										}
									}
								}
							}
							if (isSubset == false) {
								break;
							}
						}
						if (isSubset == false) {
							continue;
						}
						for (AttrValConjunct c1 : r1.getRAE()) {
							String pattr = c1.getLHS();
							for (AttrValConjunct c2 : r.getRAE()) {
								if (c2.getLHS().equals(pattr)) {
									if (config.getResourceAttrSet().get(pattr)
											.getvType() == ValueType.Single) {
										if (!c1.getRHS().containsAll(
												c2.getRHS())) {
											isSubset = false;
											break;
										}
									} else {
										if (!c1.getRHSet().containsAll(
												c2.getRHSet())) {
											isSubset = false;
											break;
										}
									}
								}
							}
							if (isSubset == false) {
								break;
							}
						}
						if (isSubset == false) {
							continue;
						}
						removeVals.add(val);
						changed = true;
						r.setUaeChanged(true);
						if (debug) {
							System.out
									.println("==============================================");
							System.out.println("Removing " + val
									+ " from UAE of \n" + r + "because of \n"
									+ r1);
						}
					}
				}
				c.getRHS().removeAll(removeVals);
				numElimElements += removeVals.size();
				if (c.getRHS().isEmpty()) {
					rules.remove(r);
					if (debug) {
						System.out
								.println("==============================================");
						System.out.println("Removing Rule " + r);
					}
					return true;
				}
			} else if (config.getUserAttrSet().get(attr).getvType() == ValueType.Set) {
				HashSet<HashSet<String>> vals = c.getRHSet();
				HashSet<HashSet<String>> removeVals = new HashSet<HashSet<String>>();
				for (HashSet<String> val : vals) {
					for (Rule r1 : rules) {
						if (r1.equals(r)) {
							continue;
						}
						if (!r.getCon().containsAll(r1.getCon())) {
							continue;
						}
						if (!(r.getUserAttrs().containsAll(r1.getUserAttrs()))) {
							continue;
						}
						if (!(r.getPermAttrs().containsAll(r1.getPermAttrs()))) {
							continue;
						}
						if (!r1.getUserAttrs().contains(attr)) {
							continue;
						}
						if (!r1.getOps().containsAll(r.getOps())) {
							continue;
						}
						boolean isSubset = true;
						for (AttrValConjunct c1 : r1.getUAE()) {
							String uattr = c1.getLHS();
							if (uattr.equals(attr)) {
								if (!c1.getRHSet().contains(val)) {
									isSubset = false;
									break;
								} else {
									continue;
								}
							}
							for (AttrValConjunct c2 : r.getUAE()) {
								if (c2.getLHS().equals(uattr)) {
									if (config.getUserAttrSet().get(uattr)
											.getvType() == ValueType.Single) {
										if (!c1.getRHS().containsAll(
												c2.getRHS())) {
											isSubset = false;
											break;
										}
									} else {
										if (!c1.getRHSet().containsAll(
												c2.getRHSet())) {
											isSubset = false;
											break;
										}
									}
								}
							}
							if (isSubset == false) {
								break;
							}
						}
						if (isSubset == false) {
							continue;
						}
						for (AttrValConjunct c1 : r1.getRAE()) {
							String pattr = c1.getLHS();
							for (AttrValConjunct c2 : r.getRAE()) {
								if (c2.getLHS().equals(pattr)) {
									if (config.getResourceAttrSet().get(pattr)
											.getvType() == ValueType.Single) {
										if (!c1.getRHS().containsAll(
												c2.getRHS())) {
											isSubset = false;
											break;
										}
									} else {
										if (!c1.getRHSet().containsAll(
												c2.getRHSet())) {
											isSubset = false;
											break;
										}
									}
								}
							}
							if (isSubset == false) {
								break;
							}
						}
						if (isSubset == false) {
							continue;
						}
						removeVals.add(val);
						changed = true;
						r.setUaeChanged(true);
						if (debug) {
							System.out
									.println("==============================================");
							System.out.println("Removing " + val
									+ " from UAE of \n" + r + "because of \n"
									+ r1);
						}
					}
				}
				c.getRHSet().removeAll(removeVals);
				numElimElements += removeVals.size();
				if (c.getRHSet().isEmpty()) {
					rules.remove(r);
					if (debug) {
						System.out
								.println("==============================================");
						System.out.println("Removing Rule " + r);
					}
					return true;
				}
			}
		}

		for (AttrValConjunct c : r.getRAE()) {
			String attr = c.getLHS();
			if (config.getResourceAttrSet().get(attr).getvType() == ValueType.Single) {

				HashSet<String> vals = c.getRHS();
				HashSet<String> removeVals = new HashSet<String>();
				for (String val : vals) {
					for (Rule r1 : rules) {
						if (r1.equals(r)) {
							continue;
						}
						if (!(new HashSet<AttrAttrConjunct>(r.getCon()))
								.containsAll((new HashSet<AttrAttrConjunct>(r1
										.getCon())))) {
							continue;
						}
						if (!(r.getUserAttrs().containsAll(r1.getUserAttrs()))) {
							continue;
						}
						if (!(r.getPermAttrs().containsAll(r1.getPermAttrs()))) {
							continue;
						}
						if (!r1.getPermAttrs().contains(attr)) {
							continue;
						}
						if (!r1.getOps().containsAll(r.getOps())) {
							continue;
						}
						boolean isSubset = true;
						for (AttrValConjunct c1 : r1.getRAE()) {
							String pattr = c1.getLHS();
							if (pattr.equals(attr)) {
								if (!c1.getRHS().contains(val)) {
									isSubset = false;
									break;
								} else {
									continue;
								}
							}
							for (AttrValConjunct c2 : r.getRAE()) {
								if (c2.getLHS().equals(pattr)) {
									if (config.getResourceAttrSet().get(pattr)
											.getvType() == ValueType.Single) {
										if (!c1.getRHS().containsAll(
												c2.getRHS())) {
											isSubset = false;
											break;
										}
									} else {
										if (!c1.getRHSet().containsAll(
												c2.getRHSet())) {
											isSubset = false;
											break;
										}
									}
								}
							}
							if (isSubset == false) {
								break;
							}
						}
						if (isSubset == false) {
							continue;
						}
						for (AttrValConjunct c1 : r1.getUAE()) {
							String uattr = c1.getLHS();
							for (AttrValConjunct c2 : r.getUAE()) {
								if (c2.getLHS().equals(uattr)) {
									if (config.getUserAttrSet().get(uattr)
											.getvType() == ValueType.Single) {
										if (!c1.getRHS().containsAll(
												c2.getRHS())) {
											isSubset = false;
											break;
										}
									} else {
										if (!c1.getRHSet().containsAll(
												c2.getRHSet())) {
											isSubset = false;
											break;
										}
									}
								}
							}
							if (isSubset == false) {
								break;
							}
						}
						if (isSubset == false) {
							continue;
						}
						removeVals.add(val);
						changed = true;
						r.setRaeChanged(true);
						if (debug) {
							System.out
									.println("==============================================");
							System.out.println("Removing " + val
									+ " from PAE of \n" + r + "because of \n"
									+ r1);
						}
					}
				}
				c.getRHS().removeAll(removeVals);
				numElimElements += removeVals.size();
				if (c.getRHS().isEmpty()) {
					rules.remove(r);
					if (debug) {
						System.out
								.println("==============================================");
						System.out.println("Removing Rule " + r);
					}
					return true;
				}
			} else if (config.getResourceAttrSet().get(attr).getvType() == ValueType.Set) {

				HashSet<HashSet<String>> vals = c.getRHSet();
				HashSet<HashSet<String>> removeVals = new HashSet<HashSet<String>>();
				for (HashSet<String> val : vals) {
					for (Rule r1 : rules) {
						if (r1.equals(r)) {
							continue;
						}
						if (!(new HashSet<AttrAttrConjunct>(r.getCon()))
								.containsAll((new HashSet<AttrAttrConjunct>(r1
										.getCon())))) {
							continue;
						}
						if (!(r.getUserAttrs().containsAll(r1.getUserAttrs()))) {
							continue;
						}
						if (!(r.getPermAttrs().containsAll(r1.getPermAttrs()))) {
							continue;
						}
						if (!r1.getPermAttrs().contains(attr)) {
							continue;
						}
						if (!r1.getOps().containsAll(r.getOps())) {
							continue;
						}
						boolean isSubset = true;
						for (AttrValConjunct c1 : r1.getRAE()) {
							String pattr = c1.getLHS();
							if (pattr.equals(attr)) {
								if (!c1.getRHSet().contains(val)) {
									isSubset = false;
									break;
								} else {
									continue;
								}
							}
							for (AttrValConjunct c2 : r.getRAE()) {
								if (c2.getLHS().equals(pattr)) {
									if (config.getResourceAttrSet().get(pattr)
											.getvType() == ValueType.Single) {
										if (!c1.getRHS().containsAll(
												c2.getRHS())) {
											isSubset = false;
											break;
										}
									} else {
										if (!c1.getRHSet().containsAll(
												c2.getRHSet())) {
											isSubset = false;
											break;
										}
									}
								}
							}
							if (isSubset == false) {
								break;
							}
						}
						if (isSubset == false) {
							continue;
						}
						for (AttrValConjunct c1 : r1.getUAE()) {
							String uattr = c1.getLHS();
							for (AttrValConjunct c2 : r.getUAE()) {
								if (c2.getLHS().equals(uattr)) {
									if (config.getUserAttrSet().get(uattr)
											.getvType() == ValueType.Single) {
										if (!c1.getRHS().containsAll(
												c2.getRHS())) {
											isSubset = false;
											break;
										}
									} else {
										if (!c1.getRHSet().containsAll(
												c2.getRHSet())) {
											isSubset = false;
											break;
										}
									}
								}
							}
							if (isSubset == false) {
								break;
							}
						}
						if (isSubset == false) {
							continue;
						}
						removeVals.add(val);
						changed = true;
						r.setRaeChanged(true);
						if (debug) {
							System.out
									.println("==============================================");
							System.out.println("Removing " + val
									+ "from PAE of \n" + r + "because of \n"
									+ r1);
						}
					}
				}
				c.getRHSet().removeAll(removeVals);
				numElimElements += removeVals.size();
				if (c.getRHSet().isEmpty()) {
					rules.remove(r);
					return true;
				}
			}
		}
		return changed;
	}

	public static boolean consistencyCheck(ArrayList<Rule> rules, Config config) {
		HashMap<Pair<String, String>, HashSet<String>> permUsers = new HashMap<Pair<String, String>, HashSet<String>>();
		for (Rule r : rules) {
			HashMap<Pair<String, String>, HashSet<String>> permUser = computeCoveredUP(
					r, config);
			for (Pair<String, String> perm : permUser.keySet()) {
				if (!permUsers.containsKey(perm)) {
					permUsers.put(perm, new HashSet<String>());
				}
				permUsers.get(perm).addAll(permUser.get(perm));
			}
		}
		if (!config.getPermUsers().keySet().equals(permUsers.keySet())) {
			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Inconsistency");
				HashSet<Pair<String, String>> permUserKeySet = new HashSet<Pair<String, String>>(
						config.getPermUsers().keySet());
				permUserKeySet.removeAll(permUsers.keySet());
				System.out.println("difference:" + permUserKeySet);
			}
			return false;
		}
		for (Pair<String, String> perm : config.getPermUsers().keySet()) {
			if (!config.getPermUsers().get(perm).equals(permUsers.get(perm))) {
				if (debug) {
					System.out
							.println("==============================================");
					System.out.println("Inconsistency in Permission " + perm);
					System.out.println("input:"
							+ config.getPermUsers().get(perm));
					System.out.println("output:" + permUsers.get(perm));
				}
				return false;
			}
		}
		return true;
	}

	public static <T> double setSimilarity(HashSet<T> s1, HashSet<T> s2) {
		HashSet<T> cloneSet1 = new HashSet<T>(s1);
		cloneSet1.retainAll(s2);
		HashSet<T> cloneSet2 = new HashSet<T>(s1);
		cloneSet2.addAll(s2);
		return (double) cloneSet1.size() / cloneSet2.size();
	}

	public static double constraintSimilarity(HashSet<AttrAttrConjunct> s1,
			HashSet<AttrAttrConjunct> s2) {
		HashSet<AttrAttrConjunct> cloneSet1 = new HashSet<AttrAttrConjunct>(s1);
		cloneSet1.retainAll(s2);
		HashSet<AttrAttrConjunct> cloneSet2 = new HashSet<AttrAttrConjunct>(s1);
		cloneSet2.addAll(s2);
		if (cloneSet2.size() == 0) {
			return 1.0;
		} else {
			return (double) cloneSet1.size() / cloneSet2.size();
		}
	}

	public static double ruleSimilarity(Rule r1, Rule r2, Config config) {
		double uaeSimilarity = 0.0;
		double paeSimilarity = 0.0;
		double conSimilarity = 0.0;
		double opsSimilarity = 0.0;

		int denominator = config.getUserAttrSet().keySet().size();
		for (String uattr : config.getUserAttrSet().keySet()) {
			boolean found1 = false;

			for (AttrValConjunct c1 : r1.getUAE()) {
				if (c1.getLHS().equals(uattr)) {
					boolean found2 = false;
					for (AttrValConjunct c2 : r2.getUAE()) {
						if (c2.getLHS().equals(uattr)) {
							if (config.getUserAttrSet().get(c1.getLHS())
									.getvType() == ValueType.Single) {
								uaeSimilarity += setSimilarity(c1.getRHS(),
										c2.getRHS());
							} else {
								uaeSimilarity += setSimilarity(c1.getRHSet(),
										c2.getRHSet());
							}
							found2 = true;
							break;
						}
					}
					if (found2 == false) {
						if (config.getUserAttrSet().get(c1.getLHS()).getvType() == ValueType.Single) {
							uaeSimilarity += (double) c1.getRHS().size()
									/ config.getUserAttrSet().get(uattr)
											.getDomain().size();
						} else {
							uaeSimilarity += (double) c1.getRHSet().size()
									/ config.getUserAttrSet().get(uattr)
											.getSetDomain().size();
						}
					}
					found1 = true;
					break;
				}
			}
			if (found1 == false) {
				boolean found2 = false;
				for (AttrValConjunct c2 : r2.getUAE()) {
					if (c2.getLHS().equals(uattr)) {
						if (config.getUserAttrSet().get(c2.getLHS()).getvType() == ValueType.Single) {
							uaeSimilarity += (double) c2.getRHS().size()
									/ config.getUserAttrSet().get(uattr)
											.getDomain().size();
						} else {
							uaeSimilarity += (double) c2.getRHSet().size()
									/ config.getUserAttrSet().get(uattr)
											.getSetDomain().size();
						}
						found2 = true;
						break;
					}
				}
				if (found2 == false) {
					// uaeSimilarity += 1.0;
					denominator -= 1;
				}
			}
		}
		if (denominator > 0) {
			uaeSimilarity = uaeSimilarity / denominator;
		} else {
			uaeSimilarity = 1.0;
		}

		denominator = config.getResourceAttrSet().keySet().size();

		for (String pattr : config.getResourceAttrSet().keySet()) {
			boolean found1 = false;
			for (AttrValConjunct c1 : r1.getRAE()) {
				if (c1.getLHS().equals(pattr)) {
					boolean found2 = false;
					for (AttrValConjunct c2 : r2.getRAE()) {
						if (c2.getLHS().equals(pattr)) {
							if (config.getResourceAttrSet().get(pattr)
									.getvType() == ValueType.Single) {
								paeSimilarity += setSimilarity(c1.getRHS(),
										c2.getRHS());
							} else {
								paeSimilarity += setSimilarity(c1.getRHSet(),
										c2.getRHSet());
							}
							found2 = true;
							break;
						}
					}
					if (found2 == false) {
						if (config.getResourceAttrSet().get(pattr).getvType() == ValueType.Single) {
							paeSimilarity += (double) c1.getRHS().size()
									/ config.getResourceAttrSet().get(pattr)
											.getDomain().size();
						} else {
							paeSimilarity += (double) c1.getRHSet().size()
									/ config.getResourceAttrSet().get(pattr)
											.getSetDomain().size();
						}
					}
					found1 = true;
					break;
				}
			}
			if (found1 == false) {
				boolean found2 = false;
				for (AttrValConjunct c2 : r2.getRAE()) {
					if (c2.getLHS().equals(pattr)) {
						if (config.getResourceAttrSet().get(pattr).getvType() == ValueType.Single) {
							paeSimilarity += (double) c2.getRHS().size()
									/ config.getResourceAttrSet().get(pattr)
											.getDomain().size();
						} else {
							paeSimilarity += (double) c2.getRHSet().size()
									/ config.getResourceAttrSet().get(pattr)
											.getSetDomain().size();
						}
						found2 = true;
						break;
					}
				}
				if (found2 == false) {
					// paeSimilarity += 1.0;
					denominator -= 1;
				}
			}
		}
		if (denominator > 0) {
			paeSimilarity = paeSimilarity / denominator;
		} else {
			paeSimilarity = 1.0;
		}

		conSimilarity = constraintSimilarity(
				new HashSet<AttrAttrConjunct>(r1.getCon()),
				new HashSet<AttrAttrConjunct>(r2.getCon()));

		opsSimilarity = setSimilarity(r1.getOps(), r2.getOps());

		return (uaeSimilarity + paeSimilarity + conSimilarity + opsSimilarity) / 4;
	}

	public static boolean ruleContainsOverassignments(Rule r, Config config) {
		return r.getCoveredLogTriple().size() == r.getCoveredUPTriple().size();
	}

	public static boolean mergeRules(LinkedList<Rule> Rules, Config config,
			HashMap<Pair<String, String>, HashSet<String>> permUsers,
			RuleQualityMetricType type) {
		boolean merged = false;
		PriorityQueue<Pair<Rule, Rule>> workSet = new PriorityQueue<Pair<Rule, Rule>>(
				100, new RulePairComparator());
		// remove redundant rules
		for (int i = 0; i < Rules.size(); i++) {
			Rule r1 = Rules.get(i);
			if (r1.getCoveredUPTriple() == null || r1.isRaeChanged()
					|| r1.isUaeChanged()) {
				r1.setCoveredUPTriple(computeCoveredUPTriple(r1, config));
			}
			HashSet<Triple<String, String, String>> s1 = new HashSet<Triple<String, String, String>>(
					r1.getCoveredUPTriple());
			s1.retainAll(config.getLogInducedUP());
			r1.setCoveredLogTriple(s1);
			r1.setChanged(false);

			for (int j = i + 1; j < Rules.size(); j++) {
				Rule r2 = Rules.get(j);
				if (r1.equals(r2)) {
					Rules.remove(j);
					if (debug) {
						System.out
								.println("==============================================");
						System.out.println("Removing Rule " + r2
								+ "\nbecause of \n" + r1);
					}
					j--;
					continue;
				}
				if (r2.getCoveredUPTriple() == null || r2.isRaeChanged()
						|| r2.isUaeChanged()) {
					r2.setCoveredUPTriple(computeCoveredUPTriple(r2, config));
					r2.setChanged(false);
				}
				HashSet<Triple<String, String, String>> s2 = new HashSet<Triple<String, String, String>>(
						r2.getCoveredUPTriple());
				s2.retainAll(config.getLogInducedUP());
				r2.setCoveredLogTriple(s2);

				if (r1.getCoveredLogTriple().containsAll(
						r2.getCoveredLogTriple())) {
					Rules.remove(j);
					if (debug) {
						System.out
								.println("==============================================");
						System.out.println("Removing Rule " + r2
								+ "\nbecause of \n" + r1);
					}
					j--;
				} else if (r2.getCoveredLogTriple().containsAll(
						r1.getCoveredLogTriple())) {
					Rules.remove(i);
					if (debug) {
						System.out
								.println("==============================================");
						System.out.println("Removing Rule " + r1
								+ "\nbecause of \n" + r2);
					}
					i--;
					break;
				}
			}
		}

		// set up workSet
		for (int i = 0; i < Rules.size(); i++) {
			Rule r1 = Rules.get(i);
			for (int j = i + 1; j < Rules.size(); j++) {
				Rule r2 = Rules.get(j);
				if ((r1.getCon().equals(r2.getCon()))) {
					if (r1.getQuality() == null) {
						r1.setQuality(computeRuleQuality(r1,
								config.getLogInducedUP(), config, type,
								permUsers));
					}
					if (r2.getQuality() == null) {
						r2.setQuality(computeRuleQuality(r2,
								config.getLogInducedUP(), config, type,
								permUsers));
					}

					if (r1.getQuality().compareTo(r2.getQuality()) >= 0) {
						workSet.add(new Pair<Rule, Rule>(r1, r2));
					} else {
						workSet.add(new Pair<Rule, Rule>(r2, r1));
					}
				}
			}
		}

		HashSet<Rule> removedRules = new HashSet<Rule>();
		int workSetSize = workSet.size();
		// compute current policy quality
		PolicyQualityValue currentPolicyQuality = new PolicyQualityValue();
		int currentWSC = 0;
		HashSet<Triple<String, String, String>> currentPolicyMeaning = new HashSet<Triple<String, String, String>>();

		for (Rule r : Rules) {
			currentWSC += r.getSize();
		}

		HashMap<Triple<String, String, String>, Integer> currentUPTripleToFreq = new HashMap<Triple<String, String, String>, Integer>();

		for (Rule r : Rules) {
			if (r.getCoveredUPTriple() == null || r.isChanged()
					|| r.isRaeChanged() || r.isUaeChanged()) {
				r.setCoveredUPTriple(computeCoveredUPTriple(r, config));
				r.setChanged(false);
			}
			currentPolicyMeaning.addAll(r.getCoveredUPTriple());
			for (Triple<String, String, String> tuple : r.getCoveredUPTriple()) {
				if (!currentUPTripleToFreq.containsKey(tuple)) {
					currentUPTripleToFreq.put(tuple, 1);
				} else {
					currentUPTripleToFreq.put(tuple,
							currentUPTripleToFreq.get(tuple) + 1);
				}
			}
		}

		HashSet<Triple<String, String, String>> currentOverAssignments = new HashSet<Triple<String, String, String>>(
				currentPolicyMeaning);
		currentOverAssignments.removeAll(config.getLogInducedUP());

		double item2 = (W0_policy * currentOverAssignments.size())
				/ config.getUsers().size();

		HashSet<Triple<String, String, String>> currentUnderAssignments = new HashSet<Triple<String, String, String>>(
				config.getLogInducedUP());
		currentUnderAssignments.removeAll(currentPolicyMeaning);

		double item3 = 0.0;
		for (Triple<String, String, String> tuple : currentUnderAssignments) {
			item3 += ((double) config.logToProbability.get(tuple));
		}
		item3 /= config.getLogs().size();
		item3 *= W1_policy;

		currentPolicyQuality.firstComponent = currentWSC + item2 + item3;

		while (!workSet.isEmpty()) {
			Rule r1 = workSet.peek().getFirst();
			Rule r2 = workSet.peek().getSecond();
			workSet.poll();
			if (removedRules.contains(r1) || removedRules.contains(r2)) {
				continue;
			}
			Rule temp2 = r2;
			Rule temp = new Rule(r1);
			if (debug) {
				System.out
						.println("==============================================");
				System.out.println("Trying to merge two rules:");
				System.out.println(r1);
				System.out.println(r2);
			}
			for (int n = 0; n < temp.getUAE().size(); n++) {
				AttrValConjunct c1 = temp.getUAE().get(n);
				boolean found = false;
				for (int m = 0; m < temp2.getUAE().size(); m++) {
					AttrValConjunct c2 = temp2.getUAE().get(m);
					if (c2.getLHS().equals(c1.getLHS())) {
						if (config.getUserAttrSet().get(c1.getLHS()).getvType() == ValueType.Single) {
							c1.getRHS().addAll(c2.getRHS());
						} else {
							c1.getRHSet().addAll(c2.getRHSet());
						}
						found = true;
						break;
					}
				}
				if (found == false) {
					temp.getUAE().remove(n);
					n--;
				}
			}
			for (int n = 0; n < temp.getRAE().size(); n++) {
				AttrValConjunct c1 = temp.getRAE().get(n);
				boolean found = false;
				for (int m = 0; m < temp2.getRAE().size(); m++) {
					AttrValConjunct c2 = temp2.getRAE().get(m);
					if (c2.getLHS().equals(c1.getLHS())) {
						if (config.getResourceAttrSet().get(c1.getLHS())
								.getvType() == ValueType.Single) {
							c1.getRHS().addAll(c2.getRHS());
						} else {
							c1.getRHSet().addAll(c2.getRHSet());
						}
						found = true;
						break;
					}
				}
				if (found == false) {
					temp.getRAE().remove(n);
					n--;
				}
			}
			temp.getOps().addAll(temp2.getOps());
			if (debug) {
				System.out.println("Merge Rule is:");
				System.out.println(temp);
			}
			temp.setUaeChanged(true);
			temp.setRaeChanged(true);
			//temp.setCoveredUPTriple(computeCoveredUPTriple(temp, config));
			boolean isValid = false;
			isValid = isValidMergedRule(temp, config);
			
			
			if (isValid) {
				temp.setChanged(false);
				if (temp.getQuality() == null) {
					temp.setQuality(computeRuleQuality(temp,
							config.getLogInducedUP(), config, type, permUsers));
				}

				// PolicyQualityValue currentValue = computePolicyQualityValue(
				// Rules, config, W0_policy, W1_policy);
				LinkedList<Rule> rulesToRemove = new LinkedList<Rule>();
				rulesToRemove.add(r1);
				rulesToRemove.add(r2);
				for (Rule rule : Rules) {
					if (!rule.equals(r1) && !rule.equals(r2)) {
						if (temp.getCoveredUPTriple().containsAll(
								rule.getCoveredUPTriple())
								&& !(ruleContainsOverassignments(temp, config) && !ruleContainsOverassignments(
										rule, config))) {
							rulesToRemove.add(rule);
						}
					}
				}

				// compute new policy quality
				PolicyQualityValue tempPolicyQuality = new PolicyQualityValue();
				int tempWSC = 0;
				HashSet<Triple<String, String, String>> tempPolicyMeaning = new HashSet<Triple<String, String, String>>(
						currentPolicyMeaning);

				tempWSC = currentWSC;
				for (Rule r : rulesToRemove) {
					tempWSC -= r.getSize();
				}
				tempWSC += temp.getSize();

				for (Rule r : rulesToRemove) {
					for (Triple<String, String, String> tuple : r
							.getCoveredUPTriple()) {
						if (currentUPTripleToFreq.get(tuple) == 1) {
							tempPolicyMeaning.remove(tuple);
						}
					}
				}
				tempPolicyMeaning.addAll(temp.getCoveredUPTriple());

				HashSet<Triple<String, String, String>> tempOverAssignments = new HashSet<Triple<String, String, String>>(
						tempPolicyMeaning);
				tempOverAssignments.removeAll(config.getLogInducedUP());

				double temp_item2 = (W0_policy * tempOverAssignments.size())
						/ config.getUsers().size();

				HashSet<Triple<String, String, String>> tempUnderAssignments = new HashSet<Triple<String, String, String>>(
						config.getLogInducedUP());
				tempUnderAssignments.removeAll(tempPolicyMeaning);

				double temp_item3 = 0.0;
				for (Triple<String, String, String> tuple : tempUnderAssignments) {
					temp_item3 += ((double) config.logToProbability.get(tuple));
				}
				temp_item3 /= config.getLogs().size();
				temp_item3 *= W1_policy;

				tempPolicyQuality.firstComponent = tempWSC + temp_item2
						+ temp_item3;
				
				
				if ((tempPolicyQuality.compareTo(currentPolicyQuality) <= 0) ) {
				
					currentPolicyMeaning.clear();
					currentOverAssignments.clear();
					currentUnderAssignments.clear();

					currentPolicyQuality = tempPolicyQuality;
					currentPolicyMeaning = tempPolicyMeaning;
					currentWSC = tempWSC;
					currentOverAssignments = tempOverAssignments;
					currentUnderAssignments = tempOverAssignments;

					for (Rule r : rulesToRemove) {
						for (Triple<String, String, String> tuple : r
								.getCoveredUPTriple()) {
							if (currentUPTripleToFreq.get(tuple) == 1) {
								currentUPTripleToFreq.remove(tuple);
							} else {
								currentUPTripleToFreq.put(tuple,
										currentUPTripleToFreq.get(tuple) - 1);
							}
						}
					}

					for (Triple<String, String, String> tuple : temp
							.getCoveredUPTriple()) {
						if (!currentUPTripleToFreq.containsKey(tuple)) {
							currentUPTripleToFreq.put(tuple, 1);
						} else {
							currentUPTripleToFreq.put(tuple,
									currentUPTripleToFreq.get(tuple) + 1);
						}
					}

					merged = true;
					numMerges++;
					removedRules.add(r1);
					removedRules.add(r2);
					removedRules.addAll(rulesToRemove);
					if (debug) {
						System.out.println("Merging two rules:");
						System.out.println("1." + r1);
						System.out.println("2." + r2);
					}
					ListIterator<Rule> itr = Rules.listIterator();

					while (itr.hasNext()) {
						Rule r = itr.next();
						if (rulesToRemove.contains(r)) {
							itr.remove();
							r.getCoveredLogTriple().clear();
							r.getCoveredUPTriple().clear();
							continue;
						}

						if (r.getCoveredUPTriple() == null || r.isChanged()
								|| r.isRaeChanged() || r.isUaeChanged()) {
							r.setCoveredUPTriple(computeCoveredUPTriple(r,
									config));
							r.setChanged(false);
						}

						if (temp.getCon().equals(r.getCon())) {
							temp.setQuality(computeRuleQuality(temp,
									config.getLogInducedUP(), config, type,
									permUsers));

							workSet.add(new Pair<Rule, Rule>(temp, r));
						}
					}
					if (debug) {
						System.out
								.println("==============================================");
						System.out.println("Result Rule " + temp);
					}
					Rules.add(temp);
				}

			} else {
				numUnsuccessfulMerges++;
			}
		}
		return merged;
	}

	/**
	 * compute all possible constraints between a user and a
	 * permission(resource)
	 * 
	 * @param user
	 *            : user identity
	 * @param perm
	 *            : permission identity
	 * @param config
	 *            : configuration
	 * @return a list of constraints
	 */
	public static ArrayList<AttrAttrConjunct> candidateConstraint(String user,
			String perm, Config config) {
		ArrayList<AttrAttrConjunct> constraints = new ArrayList<AttrAttrConjunct>();
		for (String uattr : config.getUserAttrInfo().get(user).keySet()) {
			HashSet<String> uvals = config.getUserAttrInfo().get(user)
					.get(uattr);
			if (uvals == null) {
				continue;
			}
			for (String pattr : config.getResourceAttrInfo().get(perm).keySet()) {
				HashSet<String> pvals = config.getResourceAttrInfo().get(perm)
						.get(pattr);
				if (pvals== null) {
					continue;
				}
				if (config.getUserAttrSet().get(uattr).getvType() == ValueType.Single
						&& config.getResourceAttrSet().get(pattr).getvType() == ValueType.Single
						&& uvals.equals(pvals)) {
					// add equal relation
					constraints.add(new AttrAttrConjunct(uattr, pattr,
							Operator.EQUALS));
				} else if (config.getUserAttrSet().get(uattr).getvType() == ValueType.Set
						&& config.getResourceAttrSet().get(pattr).getvType() == ValueType.Set
						&& uvals.containsAll(pvals)) {
					// add supseteq relation
					constraints.add(new AttrAttrConjunct(uattr, pattr,
							Operator.SUPSETEQ));
				} else if (config.getUserAttrSet().get(uattr).getvType() == ValueType.Set
						&& config.getResourceAttrSet().get(pattr).getvType() == ValueType.Set
						&& pvals.containsAll(uvals)) {
					// add sunseteq relation
					constraints.add(new AttrAttrConjunct(uattr, pattr,
							Operator.SUBSETEQ));
				} else if (config.getUserAttrSet().get(uattr).getvType() == ValueType.Set
						&& config.getResourceAttrSet().get(pattr).getvType() == ValueType.Single
						&& uvals.containsAll(pvals)) {
					// add contains relation
					constraints.add(new AttrAttrConjunct(uattr, pattr,
							Operator.IN));
				}
			}
		}
		return constraints;
	}

	public static ArrayList<AttrValConjunct> computeRAE(HashSet<String> s,
			Config config) {
		// construct a set of conjuncts
		ArrayList<AttrValConjunct> rae = new ArrayList<AttrValConjunct>();
		int count = 0;
		for (String perm : s) {
			count++;
			for (String attr : config.getResourceAttrInfo().get(perm).keySet()) {
				if (attr.equals("rid")) {
					continue;
				}
				boolean flag = false;
				for (AttrValConjunct c : rae) {
					if (c.getLHS().equals(attr)) {
						if (config.getResourceAttrSet().get(attr).getvType() == ValueType.Single) {
							if (!c.getRHS().contains("TOP")) {
								c.getRHS().addAll(
										config.getResourceAttrInfo().get(perm)
												.get(attr));
							}

						} else {
							if (!c.getRHSet().contains(topSet)) {
								c.getRHSet().add(
										new HashSet<String>(config
												.getResourceAttrInfo()
												.get(perm).get(attr)));
							}
						}
						flag = true;
						break;
					}
				}
				if (!flag) {
					// This is the first user
					if (count == 1) {
						if (config.getResourceAttrSet().get(attr).getvType() == ValueType.Single) {
							rae.add(new AttrValConjunct(attr,
									new HashSet<String>(config
											.getResourceAttrInfo().get(perm)
											.get(attr)), null, "="));
						} else {
							HashSet<HashSet<String>> valSet = new HashSet<HashSet<String>>();
							
							valSet.add(new HashSet<String>(config
									.getResourceAttrInfo().get(perm).get(attr)));
							rae.add(new AttrValConjunct(attr, null, valSet, "="));
						}
					} else { // This is not the first user, then the conjunct
								// should be top
						if (config.getResourceAttrSet().get(attr).getvType() == ValueType.Single) {
							HashSet<String> top = new HashSet<String>();
							top.add("TOP");
							rae.add(new AttrValConjunct(attr, top, null, "="));
						} else {
							HashSet<HashSet<String>> top = new HashSet<HashSet<String>>();
							top.add(topSet);
							rae.add(new AttrValConjunct(attr, null, top, "="));
						}
					}
				}
			}

			for (AttrValConjunct c : rae) {
				String attr = c.getLHS();
				if (!config.getResourceAttrInfo().get(perm).keySet()
						.contains(attr)) {
					if (config.getResourceAttrSet().get(attr).getvType() == ValueType.Single) {
						c.getRHS().removeAll(c.getRHS());
						c.getRHS().add("TOP");
					} else {
						c.getRHSet().removeAll(c.getRHSet());
						c.getRHSet().add(topSet);
					}
				}
			}
		}

		// test if there are any outsiders
		boolean noOutsider = true;
		for (int i = 0; i < rae.size(); i++) {
			AttrValConjunct c = rae.get(i);
			if (c.getRHS() != null && c.getRHS().contains("TOP")) {
				rae.remove(i);
				i--;
			}
			if (c.getRHSet() != null && c.getRHSet().contains(topSet)) {
				rae.remove(i);
				i--;
			}
		}

		for (String resource : config.getResources()) {
			if (s.contains(resource)) {
				continue;
			}
			try {
				if (Parser.satisfyingRAE(resource, rae, config)) {
					noOutsider = false;
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// add rid expression
		if (noOutsider == false) {
			AttrValConjunct c = new AttrValConjunct("rid", s, null, "=");
			rae.add(c);
		}

		// elimRedundantSets(rae, config, AttributeType.ResourceAttr);
		return rae;
	}

	/**
	 * compute UAE for a set of users s
	 * 
	 * @param s
	 *            : a set of users
	 * @param config
	 *            : configuration
	 * @return UAE
	 */
	public static ArrayList<AttrValConjunct> computeUAE(HashSet<String> s,
			Config config) {
		// construct a set of conjuncts
		ArrayList<AttrValConjunct> uae = new ArrayList<AttrValConjunct>();
		int count = 0;
		for (String user : s) {
			count++;
			for (String attr : config.getUserAttrInfo().get(user).keySet()) {
				if (attr.equals("uid")) {
					continue;
				}
				boolean flag = false;
				for (AttrValConjunct c : uae) {
					if (c.getLHS().equals(attr)) {
						if (config.getUserAttrSet().get(attr).getvType() == ValueType.Single) {
							if (!c.getRHS().contains("TOP")) {
								c.getRHS().addAll(
										config.getUserAttrInfo().get(user)
												.get(attr));
							}

						} else {
							if (!c.getRHSet().contains(topSet)) {
								c.getRHSet().add(
										new HashSet<String>(config
												.getUserAttrInfo().get(user)
												.get(attr)));
							}
						}
						flag = true;
						break;
					}
				}
				if (!flag) {
					if (count == 1) {
						if (config.getUserAttrSet().get(attr).getvType() == ValueType.Single) {
							uae.add(new AttrValConjunct(attr,
									new HashSet<String>(config
											.getUserAttrInfo().get(user)
											.get(attr)), null, "="));
						} else {
							HashSet<HashSet<String>> valSet = new HashSet<HashSet<String>>();
							valSet.add(new HashSet<String>(config
									.getUserAttrInfo().get(user).get(attr)));
							uae.add(new AttrValConjunct(attr, null, valSet, "="));
						}
					} else {
						if (config.getUserAttrSet().get(attr).getvType() == ValueType.Single) {
							HashSet<String> top = new HashSet<String>();
							top.add("TOP");
							uae.add(new AttrValConjunct(attr, top, null, "="));
						} else {
							HashSet<HashSet<String>> top = new HashSet<HashSet<String>>();
							top.add(topSet);
							uae.add(new AttrValConjunct(attr, null, top, "="));
						}
					}
				}
			}

			for (AttrValConjunct c : uae) {
				String attr = c.getLHS();
				if (!config.getUserAttrInfo().get(user).keySet().contains(attr)) {
					if (config.getUserAttrSet().get(attr).getvType() == ValueType.Single) {
						c.getRHS().removeAll(c.getRHS());
						c.getRHS().add("TOP");
					} else {
						c.getRHSet().removeAll(c.getRHSet());
						c.getRHSet().add(topSet);
					}
				}
			}
		}

		// test if there are any outsiders
		boolean noOutsider = true;
		for (int i = 0; i < uae.size(); i++) {
			AttrValConjunct c = uae.get(i);
			if (c.getRHS() != null && c.getRHS().contains("TOP")) {
				uae.remove(i);
				i--;
			}
			if (c.getRHSet() != null && c.getRHSet().contains(topSet)) {
				uae.remove(i);
				i--;
			}
		}

		for (String user : config.getUsers()) {
			if (s.contains(user)) {
				continue;
			}
			try {
				if (Parser.satisfyingUAE(user, uae, config)) {
					noOutsider = false;
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// add uid expression
		if (noOutsider == false) {
			AttrValConjunct c = new AttrValConjunct("uid", s, null, "=");
			uae.add(c);
		}
		elimRedundantSets(uae, config, AttributeType.UserAttr);

		return uae;
	}

	public static boolean elimRedundantSets(ArrayList<AttrValConjunct> ae,
			Config config, AttributeType type) {
		HashSet<AttrValConjunct> removeConjunctSet = new HashSet<AttrValConjunct>();
		switch (type) {
		case UserAttr:
			for (AttrValConjunct c : ae) {
				if (config.getUserAttrSet().get(c.getLHS()).getvType() == ValueType.Set) {
					HashSet<HashSet<String>> removeSet = new HashSet<HashSet<String>>();
					boolean foundEmptySet = false;
					for (HashSet<String> s1 : c.getRHSet()) {
						if (s1.isEmpty()) {
							foundEmptySet = true;
							break;
						}
						boolean found = false;
						for (HashSet<String> s2 : c.getRHSet()) {
							if (s1 != s2 && s1.containsAll(s2)) {
								found = true;
								break;
							}
						}
						if (found) {
							removeSet.add(s1);
						}
					}
					if (foundEmptySet == true) {
						removeConjunctSet.add(c);
						continue;
					}
					if (debug && !removeSet.isEmpty()) {
						System.out
								.println("==============================================");
						System.out.println("Removed Sets for UAE " + c);
						System.out.println(removeSet);
					}
					c.getRHSet().removeAll(removeSet);
				}
			}
			ae.removeAll(removeConjunctSet);
			break;
		case ResourceAttr:
			for (AttrValConjunct c : ae) {
				if (config.getResourceAttrSet().get(c.getLHS()).getvType() == ValueType.Set) {
					HashSet<HashSet<String>> removeSet = new HashSet<HashSet<String>>();
					boolean foundEmptySet = false;
					for (HashSet<String> s1 : c.getRHSet()) {
						if (s1.isEmpty()) {
							foundEmptySet = true;
							break;
						}
						boolean found = false;
						for (HashSet<String> s2 : c.getRHSet()) {
							if (s1 != s2 && s1.containsAll(s2)) {
								found = true;
								break;
							}
						}
						if (found) {
							removeSet.add(s1);
						}
					}
					if (foundEmptySet == true) {
						removeConjunctSet.add(c);
						continue;
					}
					if (debug && !removeSet.isEmpty()) {
						System.out
								.println("==============================================");
						System.out.println("Removed Sets for UAE " + c);
						System.out.println(removeSet);
					}
					c.getRHSet().removeAll(removeSet);
				}
			}
			ae.removeAll(removeConjunctSet);
			break;
		default:
			break;
		}
		return false;
	}

	public static Rule generalizeRule(Rule r,
			List<AttrAttrConjunct> constraints, Config config,
			HashSet<Triple<String, String, String>> uncovUP,
			RuleQualityMetricType type,
			HashMap<Pair<String, String>, HashSet<String>> permUsers) {
		// bestRule is the generalization of r with the best quality
		Rule bestRule = r;
		RuleQualityValue bestQuality = computeRuleQuality(bestRule, uncovUP,
				config, type, permUsers);

		ArrayList<AttrAttrConjunct> cc = new ArrayList<AttrAttrConjunct>();
		// gen[i] is a generalization of r using formula cc[i]
		ArrayList<ArrayList<Rule>> gen = new ArrayList<ArrayList<Rule>>();

		// if (debug) {
		// System.out
		// .println("==============================================");
		// System.out.println("Constraints List:");
		// System.out.println(constraints);
		// }

		// add the first case
		for (int i = 0; i < constraints.size(); i++) {
			// add the first rule to the set, where both conjuncts are removed
			Rule r1 = new Rule(r);
			String uattr = constraints.get(i).getLHS();
			String pattr = constraints.get(i).getRHS();
			// try to generalize r by adding constraints[i] and eliminating both
			// relevant conjuncts
			int uattrIndex = -1;
			int rattrIndex = -1;
			for (int j = 0; j < r1.getUAE().size(); j++) {
				if (uattr.equals(r1.getUAE().get(j).getLHS())) {
					r1.getUAE().remove(j);
					r1.setUaeChanged(true);
					uattrIndex = j;
					break;
				}
			}
			for (int j = 0; j < r1.getRAE().size(); j++) {
				if (pattr.equals(r1.getRAE().get(j).getLHS())) {
					r1.getRAE().remove(j);
					r1.setRaeChanged(true);
					rattrIndex = j;
					break;
				}
			}
			r1.setChanged(true);
			r1.getCon().add(constraints.get(i));
			cc.add(constraints.get(i));
			gen.add(new ArrayList<Rule>());
			gen.get(i).add(r1);
			// if (debug) {
			// System.out
			// .println("==============================================");
			// System.out.println("Add a constraint:");
			// System.out.println(constraints.get(i));
			// System.out.println("Add a rule by the first case:");
			// System.out.println(r1);
			// }
			// add the second rule to the set, where user attribute conjunct is
			// removed
			if (uattrIndex != -1) {
				Rule r2 = new Rule(r);
				r2.getUAE().remove(uattrIndex);
				r2.setUaeChanged(true);
				r2.setChanged(true);
				r2.getCon().add(constraints.get(i));
				gen.get(i).add(r2);
				// if (debug) {
				// System.out
				// .println("==============================================");
				// System.out.println("Add a constraint:");
				// System.out.println(constraints.get(i));
				// System.out.println("Add a rule by the second case:");
				// System.out.println(r2);
				// }
			}

			// add the third rule to the set, where resource attribute conjunct
			// is removed
			if (rattrIndex != -1) {
				Rule r3 = new Rule(r);
				r3.getRAE().remove(rattrIndex);
				r3.setRaeChanged(true);
				r3.setChanged(true);
				r3.getCon().add(constraints.get(i));
				gen.get(i).add(r3);
				// if (debug) {
				// System.out
				// .println("==============================================");
				// System.out.println("Add a constraint:");
				// System.out.println(constraints.get(i));
				// System.out.println("Add a rule by the third case:");
				// System.out.println(r3);
				// }
			}
		}

		for (int i = 0; i < cc.size(); i++) {
			for (int j = 0; j < gen.get(i).size(); j++) {
				// try to further generalize gen[i][j]
				Rule tempR = generalizeRule(gen.get(i).get(j),
						cc.subList(i + 1, cc.size()), config, uncovUP, type,
						permUsers);
				if (tempR != null) {
					RuleQualityValue quality = computeRuleQuality(tempR,
							uncovUP, config, type, permUsers);
					if (quality.compareTo(bestQuality) > 0) {
						bestRule = tempR;
						bestQuality = quality;
					}
				}
			}
		}
		return bestRule;
	}

	public static boolean elimConjuncts(Rule r, LinkedList<Rule> rules,
			Config config, HashSet<Triple<String, String, String>> uncovUP,
			int index, RuleQualityMetricType type,
			HashMap<Pair<String, String>, HashSet<String>> permUsers) {
		boolean changed = false;
		ArrayList<Pair<AttributeType, String>> A = new ArrayList<Pair<AttributeType, String>>();
		ArrayList<Pair<AttributeType, String>> B = new ArrayList<Pair<AttributeType, String>>();
		for (String uattr : r.getUserAttrs()) {
			A.add(new Pair<AttributeType, String>(AttributeType.UserAttr, uattr));
		}
		for (String rattr : r.getPermAttrs()) {
			B.add(new Pair<AttributeType, String>(AttributeType.ResourceAttr,
					rattr));
		}

		Rule minRule = new Rule(r);

		if (newElimConjunct) {
			if (r.getMaxUAESize() >= r.getMaxRAESize()) {
				minRule = elimConjunctsHelper(minRule, A, config, uncovUP,
						type, permUsers);
				minRule = elimConjunctsHelper(minRule, B, config, uncovUP,
						type, permUsers);
			} else {
				minRule = elimConjunctsHelper(minRule, B, config, uncovUP,
						type, permUsers);
				minRule = elimConjunctsHelper(minRule, A, config, uncovUP,
						type, permUsers);
			}
		} else {
			minRule = elimConjunctsHelper(minRule, A, config, uncovUP, type,
					permUsers);
			minRule = elimConjunctsHelper(minRule, B, config, uncovUP, type,
					permUsers);
		}

		if (!r.equals(minRule)) {
			rules.set(index, minRule);
			numElimConjuncts += r.getUAE().size() + r.getRAE().size()
					- minRule.getUAE().size() - minRule.getRAE().size();

			changed = true;
		}

		return changed;
	}

	public static Rule elimConjunctsHelper(Rule r,
			List<Pair<AttributeType, String>> A, Config config,
			HashSet<Triple<String, String, String>> uncovUP,
			RuleQualityMetricType qType,
			HashMap<Pair<String, String>, HashSet<String>> permUsers) {
		if (A.isEmpty()) {
			return r;
		}
		Rule bestRule = r;
		RuleQualityValue bestQuality = computeRuleQuality(bestRule, uncovUP,
				config, qType, permUsers);

		ArrayList<Pair<AttributeType, String>> validPairs = new ArrayList<Pair<AttributeType, String>>(
				A);

		ArrayList<Rule> potentialRules = new ArrayList<Rule>();
		for (int i = 0; i < validPairs.size(); i++) {
			Rule temp = new Rule(r);
			AttributeType type = validPairs.get(i).getFirst();
			String attr = validPairs.get(i).getSecond();
			boolean isValid = false;
			switch (type) {
			case UserAttr:
				for (int j = 0; j < temp.getUAE().size(); j++) {
					if (attr.equals(temp.getUAE().get(j).getLHS())) {
						temp.getUAE().remove(j);
						temp.setUaeChanged(true);
						break;
					}
				}
				// isValid = true;
				isValid = isValidRule(temp, config);

				if (!isValid) {
					validPairs.remove(i);
					i--;
				} else if (config.getUserAttrSet().get(attr).isUnremovable()) {
					validPairs.remove(i);
					i--;
				} else {
					potentialRules.add(temp);
				}
				break;
			case ResourceAttr:
				for (int j = 0; j < temp.getRAE().size(); j++) {
					if (attr.equals(temp.getRAE().get(j).getLHS())) {
						temp.getRAE().remove(j);
						temp.setRaeChanged(true);
						break;
					}
				}
				// isValid = true;
				isValid = isValidRule(temp, config);
				if (!isValid) {
					validPairs.remove(i);
					i--;
				} else if (config.getResourceAttrSet().get(attr)
						.isUnremovable()) {
					validPairs.remove(i);
					i--;
				} else {
					potentialRules.add(temp);
				}
				break;
			}
		}

		for (int i = 0; i < validPairs.size(); i++) {
			Rule tempR = new Rule(r);
			AttributeType type = validPairs.get(i).getFirst();
			String attr = validPairs.get(i).getSecond();
			switch (type) {
			case UserAttr:
				for (int j = 0; j < tempR.getUAE().size(); j++) {
					if (attr.equals(tempR.getUAE().get(j).getLHS())) {
						tempR.getUAE().remove(j);
						tempR.setUaeChanged(true);
						break;
					}
				}
				break;
			case ResourceAttr:
				for (int j = 0; j < tempR.getRAE().size(); j++) {
					if (attr.equals(tempR.getRAE().get(j).getLHS())) {
						tempR.getRAE().remove(j);
						tempR.setRaeChanged(true);
						break;
					}
				}
				break;
			}
			Rule minTemp = elimConjunctsHelper(tempR,
					validPairs.subList(i + 1, validPairs.size()), config,
					uncovUP, qType, permUsers);
			if (minTemp != null) {
				RuleQualityValue quality = computeRuleQuality(minTemp, uncovUP,
						config, qType, permUsers);
				if (quality.compareTo(bestQuality) > 0) {
					// System.out.println(minTemp + ":\n " +
					// quality.firstComponent);;
					bestRule = minTemp;
					bestQuality = quality;
				}
			}
		}
		return bestRule;
	}

	public static PolicyQualityValue computePolicyQualityValue(
			LinkedList<Rule> ruleList, Config config, double w0, double w1) {
		PolicyQualityValue result = new PolicyQualityValue();

		int WSC = 0;
		for (Rule r : ruleList) {
			WSC += r.getSize();
		}

		HashSet<Triple<String, String, String>> policyMeaning = new HashSet<Triple<String, String, String>>();
		for (Rule r : ruleList) {
			if (r.getCoveredUPTriple() == null || r.isChanged()
					|| r.isRaeChanged() || r.isUaeChanged()) {
				r.setCoveredUPTriple(computeCoveredUPTriple(r, config));
				r.setChanged(false);
			}
			policyMeaning.addAll(r.getCoveredUPTriple());
		}

		HashSet<Triple<String, String, String>> overAssignments = new HashSet<Triple<String, String, String>>(
				policyMeaning);
		overAssignments.removeAll(config.getLogInducedUP());

		double item2 = (w0 * overAssignments.size()) / config.getUsers().size();

		HashSet<Triple<String, String, String>> underAssignments = new HashSet<Triple<String, String, String>>(
				config.getLogInducedUP());
		underAssignments.removeAll(policyMeaning);

		double item3 = 0.0;
		for (Triple<String, String, String> tuple : underAssignments) {
			item3 += ((double) config.logToProbability.get(tuple));
		}
		item3 /= config.getLogs().size();
		item3 *= w1;

		result.firstComponent = WSC + item2 + item3;
		return result;
	}

	public static RuleQualityValue computeRuleQuality(Rule r,
			HashSet<Triple<String, String, String>> uncovUP, Config config,
			RuleQualityMetricType type,
			HashMap<Pair<String, String>, HashSet<String>> permUsers) {
		RuleQualityValue result = new RuleQualityValue();

		if (r.getCoveredUPTriple() == null || r.isChanged() || r.isRaeChanged()
				|| r.isUaeChanged()) {
			r.setCoveredUPTriple(computeCoveredUPTriple(r, config));
			r.setChanged(false);
		}
		HashSet<Triple<String, String, String>> covered = new HashSet<Triple<String, String, String>>(
				r.getCoveredUPTriple());

		covered.retainAll(uncovUP);

		HashSet<Triple<String, String, String>> overAssignment = new HashSet<Triple<String, String, String>>(
				r.getCoveredUPTriple());
		overAssignment.removeAll(config.getLogInducedUP());

		int up = 0;
		for (String op : r.getOps()) {
			for (String resource : r.getSatResources()) {
				if (permUsers
						.containsKey(new Pair<String, String>(op, resource))) {
					up += permUsers.get(new Pair<String, String>(op, resource))
							.size();
				}
			}
		}

		double coveredFreq = 0.0;
		for (Triple<String, String, String> tuple : covered) {
			if (config.logToProbability.keySet().contains(tuple)) {
				coveredFreq += ((double) config.logToProbability.get(tuple));
			}
		}
		// coveredFreq /= config.getLogs().size();

		switch (type) {
		case Q_RELATIVE_TO_COVERED:
			result.firstComponent = ((double) covered.size() / r.getSize())
					* (1 - (double) W0_rule * overAssignment.size()
							/ covered.size());
			break;
		case Q_RELATIVE_TO_MEANING:
			result.firstComponent = ((double) covered.size() / r.getSize())
					* (1 - (double) W0_rule * overAssignment.size()
							/ r.getCoveredUPTriple().size());
			break;
		case Q_RELATIVE_TO_UP0:
			result.firstComponent = (double) ((double) covered.size() / r
					.getSize())
					* (1 - (double) W0_rule * overAssignment.size()
							/ config.getLogInducedUP().size());
			break;
		case Q_RELATIVE_TO_PERMISSIONS:

			result.firstComponent = (double) ((double) covered.size() / r
					.getSize())
					* (1 - (double) W0_rule * overAssignment.size() / up);
			break;
		case Q_RELATIVE_TO_SIZE_AND_PERMISSIONS:
			result.firstComponent = (double) ((double) covered.size() - overAssignment
					.size()) / ((double) W0_rule * r.getSize() * up);
			break;
		case Q_PROGOL:
			int m = config.getLogInducedUP().size();
			int p = covered.size();
			double C_i = r.getSize();
			double g_diff = (p + overAssignment.size())
					/ config.getUsers().size() * config.getResources().size()
					* config.getOps().size();
			result.firstComponent = Math.pow(2, -((double) m * C_i) / p)
					* Math.pow((1 - m * g_diff / p), m);
			break;
		case Q_RELATIVE_TO_COVERED_FREQ:
			result.firstComponent = ((double) coveredFreq / r.getSize())
					* (1 - (double) W0_rule * overAssignment.size()
							/ coveredFreq);
			break;
		case Q_RELATIVE_TO_MEANING_FREQ:
			result.firstComponent = ((double) coveredFreq / r.getSize())
					* (1 - (double) W0_rule * overAssignment.size()
							/ r.getCoveredUPTriple().size());
			break;
		case Q_RELATIVE_TO_UP0_FREQ:
			result.firstComponent = (double) ((double) coveredFreq / r
					.getSize())
					* (1 - (double) W0_rule * overAssignment.size()
							/ config.getLogInducedUP().size());
			break;
		case Q_RELATIVE_TO_PERMISSIONS_FREQ:
			result.firstComponent = (double) ((double) coveredFreq / r
					.getSize())
					* (1 - (double) W0_rule * overAssignment.size() / up);
			break;
		case Q_RELATIVE_TO_SIZE_AND_PERMISSIONS_FREQ:
			result.firstComponent = (double) ((double) coveredFreq - overAssignment
					.size()) / ((double) W0_rule * r.getSize() * up);
			break;
		default:
			break;
		}

		return result;
	}

	public static HashMap<Pair<String, String>, HashSet<String>> computeCoveredUP(
			Rule r, Config config) {
		HashMap<Pair<String, String>, HashSet<String>> coveredUP = new HashMap<Pair<String, String>, HashSet<String>>();
		HashSet<String> satUsers = new HashSet<String>();
		HashSet<String> satPerms = new HashSet<String>();
		try {
			for (String user : config.getUsers()) {
				if (Parser.satisfyingRule(user, r, config, true)) {
					satUsers.add(user);
				}
			}
			for (String permission : config.getResources()) {
				if (Parser.satisfyingRule(permission, r, config, false)) {
					satPerms.add(permission);
				}
			}
			for (String user : satUsers) {
				for (String permission : satPerms) {
					if (r.getCon().isEmpty()
							|| Parser.satisfyingRuleConstraints(user,
									permission, r, config)) {

						for (String op : r.getOps()) {
							Pair<String, String> perm = new Pair<String, String>(
									op, permission);
							if (!coveredUP.containsKey(perm)) {
								coveredUP.put(perm, new HashSet<String>());
							}
							coveredUP.get(perm).add(user);
						}

					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return coveredUP;
	}
	
	/**
	 * compute covered UP set for a rule
	 * 
	 * @param r
	 *            : rule
	 * @param config
	 *            : config
	 * @return true if noOA in Rule r and sets the r.setCoveredUPTriple to the coveredUP
	 */
	public static boolean isValidMergedRule(
			Rule r, Config config) {
		HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
		HashSet<String> satUsers = new HashSet<String>();
		HashSet<String> satResources = new HashSet<String>();

		if (r.isUaeChanged() == false && r.getSatUsers() != null
				&& !r.getSatUsers().isEmpty()) {
			satUsers = r.getSatUsers();
		} else {
			for (String user : config.getUsers()) {
				if (Parser.satisfyingRule(user, r, config, true)) {
					satUsers.add(user);
				}
			}
			r.setSatUsers(satUsers);
			r.setUaeChanged(false);
		}
		if (r.isRaeChanged() == false && r.getSatResources() != null
				&& !r.getSatResources().isEmpty()) {
			satResources = r.getSatResources();
		} else {
			for (String permission : config.getResources()) {
				if (Parser.satisfyingRule(permission, r, config, false)) {
					satResources.add(permission);
				}
			}
			r.setSatResources(satResources);
			r.setRaeChanged(false);
		}
		for (String user : satUsers) {
			for (String permission : satResources) {
				boolean satisfied = false;
				if (r.getCon().isEmpty()) {
					satisfied = true;
				} else {
					satisfied = Parser.satisfyingRuleConstraints(user,
							permission, r, config);
				}
				if (satisfied) {
					for (String op : r.getOps()) {
						Triple<String, String, String> up = new Triple<String, String, String>(
								user, op, permission);
						if (!config.getLogInducedUP().contains(up)) {
							return false;
						}
						coveredUP.add(up);
					}
				}
			}
		}

		r.setCoveredUPTriple(coveredUP);
		return true;
	}

	/**
	 * compute covered UP set for a rule
	 * 
	 * @param r
	 *            : rule
	 * @param config
	 *            : config
	 * @return covered UP set
	 */
	public static HashSet<Triple<String, String, String>> computeCoveredUPTriple(
			Rule r, Config config) {
		HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
		HashSet<String> satUsers = new HashSet<String>();
		HashSet<String> satResources = new HashSet<String>();

		if (r.isUaeChanged() == false && r.getSatUsers() != null
				&& !r.getSatUsers().isEmpty()) {
			satUsers = r.getSatUsers();
		} else {
			for (String user : config.getUsers()) {
				if (Parser.satisfyingRule(user, r, config, true)) {
					satUsers.add(user);
				}
			}
			r.setSatUsers(satUsers);
			r.setUaeChanged(false);
		}
		if (r.isRaeChanged() == false && r.getSatResources() != null
				&& !r.getSatResources().isEmpty()) {
			satResources = r.getSatResources();
		} else {
			for (String permission : config.getResources()) {
				if (Parser.satisfyingRule(permission, r, config, false)) {
					satResources.add(permission);
				}
			}
			r.setSatResources(satResources);
			r.setRaeChanged(false);
		}
		for (String user : satUsers) {
			for (String permission : satResources) {
				boolean satisfied = false;
				if (r.getCon().isEmpty()) {
					satisfied = true;
				} else {
					satisfied = Parser.satisfyingRuleConstraints(user,
							permission, r, config);
				}
				if (satisfied) {
					for (String op : r.getOps()) {
						Triple<String, String, String> up = new Triple<String, String, String>(
								user, op, permission);
						coveredUP.add(up);
					}
				}
			}
		}

		return coveredUP;
	}

	public static boolean isValidRule(Rule r, Config config) {
		if (r.getCoveredUPTriple().isEmpty() || r.isChanged()
				|| r.isUaeChanged() || r.isRaeChanged()) {
			r.setCoveredUPTriple(computeCoveredUPTriple(r, config));
			r.setChanged(false);
		}
		boolean isValid = config.getCoveredUP().containsAll(
				r.getCoveredUPTriple());

		if (isValid) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isValidRuleWithUnderAssignment(Rule r, Config config,
			double underAssignFrac) {
		if (r.getCoveredUPTriple().isEmpty() || r.isChanged()
				|| r.isUaeChanged() || r.isRaeChanged()) {
			r.setCoveredUPTriple(computeCoveredUPTriple(r, config));
			r.setChanged(false);
		}

		if (Double.compare(underAssignFrac, 0) == 0) {
			boolean isValid = config.getCoveredUP().containsAll(
					r.getCoveredUPTriple());

			if (isValid) {
				return true;
			} else {
				return false;
			}
		} else {
			int numUnderAssign = (int) (r.getCoveredUPTriple().size() * underAssignFrac);
			int underAssign = 0;
			for (Triple<String, String, String> element : r
					.getCoveredUPTriple()) {
				if (!config.getCoveredUP().contains(element)) {
					underAssign++;
					if (underAssign > numUnderAssign) {
						return false;
					}
				}
			}
			return true;
		}
	}

	public static boolean isValidMergedRule(Rule r, Rule r1, Rule r2,
			Config config) {
		HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
		coveredUP.addAll(new HashSet<Triple<String, String, String>>(r1
				.getCoveredUPTriple()));
		coveredUP.addAll(new HashSet<Triple<String, String, String>>(r2
				.getCoveredUPTriple()));

		for (String op : r2.getOps()) {
			if (!r1.getOps().contains(op)) {
				for (Triple<String, String, String> up : r1
						.getCoveredUPTriple()) {
					Triple<String, String, String> up1 = new Triple<String, String, String>(
							up.getFirst(), op, up.getThird());
					if (!config.getCoveredUP().contains(up1)) {
						return false;
					} else {
						coveredUP.add(up1);
					}
				}
			}
		}

		for (String op : r1.getOps()) {
			if (!r2.getOps().contains(op)) {
				for (Triple<String, String, String> up : r2
						.getCoveredUPTriple()) {
					Triple<String, String, String> up1 = new Triple<String, String, String>(
							up.getFirst(), op, up.getThird());
					if (!config.getCoveredUP().contains(up1)) {
						return false;
					} else {
						coveredUP.add(up1);
					}
				}
			}
		}

		for (String user : r1.getSatUsers()) {
			for (String permission : r2.getSatResources()) {
				boolean satisfied = false;
				if (r.getCon().isEmpty())
					satisfied = true;
				else
					satisfied = Parser.satisfyingRuleConstraints(user,
							permission, r, config);
				if (satisfied) {
					for (String op : r.getOps()) {
						Triple<String, String, String> up = new Triple<String, String, String>(
								user, op, permission);
						if (!config.getCoveredUP().contains(up)) {
							return false;
						} else {
							coveredUP.add(up);
						}
					}
				}
			}
		}

		for (String user : r2.getSatUsers()) {
			for (String permission : r1.getSatResources()) {
				boolean satisfied = false;
				if (r.getCon().isEmpty())
					satisfied = true;
				else
					satisfied = Parser.satisfyingRuleConstraints(user,
							permission, r, config);
				if (satisfied) {
					for (String op : r.getOps()) {
						Triple<String, String, String> up = new Triple<String, String, String>(
								user, op, permission);
						if (!config.getCoveredUP().contains(up)) {
							return false;
						} else {
							coveredUP.add(up);
						}
					}
				}
			}
		}

		HashSet<String> satUsers = new HashSet<String>();
		satUsers.addAll(new HashSet<String>(r1.getSatUsers()));
		satUsers.addAll(new HashSet<String>(r2.getSatUsers()));
		HashSet<String> satResources = new HashSet<String>();
		satResources.addAll(new HashSet<String>(r1.getSatResources()));
		satResources.addAll(new HashSet<String>(r2.getSatResources()));

		if (r.isUaeChanged() == false && r.getSatUsers() != null
				&& !r.getSatUsers().isEmpty()) {
			satUsers = r.getSatUsers();
		} else {
			for (String user : config.getUsers()) {
				if (!satUsers.contains(user)
						&& Parser.satisfyingRule(user, r, config, true)) {
					satUsers.add(user);
				}
			}
			r.setSatUsers(satUsers);
			r.setUaeChanged(false);
		}
		if (r.isRaeChanged() == false && r.getSatResources() != null
				&& !r.getSatResources().isEmpty()) {
			satResources = r.getSatResources();
		} else {
			for (String permission : config.getResources()) {
				if (!satResources.contains(permission)
						&& Parser.satisfyingRule(permission, r, config, false)) {
					satResources.add(permission);
				}
				r.setSatResources(satResources);
				r.setRaeChanged(false);
			}
		}
		for (String user : satUsers) {
			for (String permission : satResources) {
				boolean satisfied = false;
				if (r.getCon().isEmpty())
					satisfied = true;
				else
					satisfied = Parser.satisfyingRuleConstraints(user,
							permission, r, config);
				if (satisfied) {
					for (String op : r.getOps()) {
						Triple<String, String, String> up = new Triple<String, String, String>(
								user, op, permission);
						if (!config.getCoveredUP().contains(up)) {
							return false;
						}
						coveredUP.add(up);
					}
				}
			}
		}

		r.setCoveredUPTriple(coveredUP);
		return true;
	}

	public static void computeDuration() {
		endTime = System.currentTimeMillis();
		duration = endTime - startTime;
		startTime = endTime;
		totalTime += duration;
	}

	public static void computeAttributeRelevance(Config config) {
		double[] maxRelativeMutualInfo = new double[config.getUserAttrSet()
				.keySet().size()];
		ArrayList<String> userAttrs = new ArrayList<String>(config
				.getUserAttrSet().keySet());
		HashMap<Pair<String, String>, HashSet<String>> userAttributeToUsers = new HashMap<Pair<String, String>, HashSet<String>>();
		for (String userAttr : userAttrs) {
			Attribute uAttr = config.getUserAttrSet().get(userAttr);
			if (uAttr.getvType() == ValueType.Single) {
				for (String uAttrValue : uAttr.getDomain()) {
					Pair<String, String> userAttribute = new Pair<String, String>(
							userAttr, uAttrValue);
					userAttributeToUsers.put(userAttribute,
							new HashSet<String>());
					for (String user : config.getUsers()) {
						if (config.getUserAttrInfo().get(user)
								.containsKey(userAttr)) {
							if (config.getUserAttrInfo().get(user)
									.get(userAttr).contains(uAttrValue)) {
								userAttributeToUsers.get(userAttribute).add(
										user);
							}
						}
					}
				}
			}
		}

		for (Pair<String, String> perm : config.getPermUsers().keySet()) {
			HashSet<String> userSet = config.getPermUsers().get(perm);
			double p = (double) userSet.size() / config.getUsers().size();
			double binaryEntropy = -(p * (Math.log(p) / Math.log(2)) + (1 - p)
					* (Math.log(1 - p) / Math.log(2)));
			for (int i = 0; i < userAttrs.size(); i++) {
				String userAttr = userAttrs.get(i);
				Attribute uAttr = config.getUserAttrSet().get(userAttr);
				double conditionalEntropy = 0.0;
				if (uAttr.getvType() == ValueType.Single) {
					for (String uAttrValue : uAttr.getDomain()) {
						Pair<String, String> userAttribute = new Pair<String, String>(
								userAttr, uAttrValue);
						HashSet<String> userWithPerm = new HashSet<String>(
								config.getPermUsers().get(perm));

						userWithPerm.retainAll(userAttributeToUsers
								.get(userAttribute));
						if (!userWithPerm.isEmpty()) {
							double cp = (double) userWithPerm.size()
									/ userAttributeToUsers.get(userAttribute)
											.size();
							if (Double.compare(cp, 0.0) != 0
									&& Double.compare(cp, 1.0) != 0) {
								conditionalEntropy += -((double) userAttributeToUsers
										.get(userAttribute).size() / config
										.getUsers().size())
										* (cp * (Math.log(cp) / Math.log(2)) + (1 - cp)
												* (Math.log(1 - cp) / Math
														.log(2)));
							}
						}
					}
					if (Double.compare(conditionalEntropy, 0.0) != 0) {
						double mutualInfo = 1 - (conditionalEntropy / binaryEntropy);
						if (mutualInfo > maxRelativeMutualInfo[i]) {
							maxRelativeMutualInfo[i] = mutualInfo;
						}
					}
				}
			}
		}
		System.out.println(userAttrs);
		System.out.println(Arrays.toString(maxRelativeMutualInfo));
	}

	/**
	 * 
	 * @param rules1
	 *            : the first policy
	 * @param rules2
	 *            : the second policy
	 * @param config
	 * @return the syntactic similarity of two policies
	 */
	public static double nonsymmetricSyntacticSimilarityPolicies(
			ArrayList<Rule> rules1, ArrayList<Rule> rules2, Config config) {

		double similarity = 0.0;

		Rule maxRule = null;

		for (Rule r2 : rules2) {
			double maxSimilarity = 0.0;
			for (Rule r1 : rules1) {
				double currentSimilarity = syntacticSimilarityOfRules(r2, r1,
						config);
				if (Double.compare(currentSimilarity, maxSimilarity) > 0) {
					maxSimilarity = currentSimilarity;
					maxRule = r1;
				}
			}
			similarity += maxSimilarity;

			// System.out.println(r2);
			// System.out.println(maxRule);
			// System.out.println(maxSimilarity);
		}
		similarity /= rules2.size();

		return similarity;
	}

	/**
	 * 
	 * @param rules1
	 *            : the first policy
	 * @param rules2
	 *            : the second policy
	 * @param config
	 * @return the syntactic similarity of two policies
	 */
	public static double symmetricSyntacticSimilarityOfPolicies(
			ArrayList<Rule> rules1, ArrayList<Rule> rules2, Config config) {
		double similarity1 = 0.0;
		double similarity2 = 0.0;

		for (Rule r1 : rules1) {
			double maxSimilarity = 0.0;
			for (Rule r2 : rules2) {
				double currentSimilarity = syntacticSimilarityOfRules(r1, r2,
						config);
				if (Double.compare(currentSimilarity, maxSimilarity) > 0) {
					maxSimilarity = currentSimilarity;
				}
			}
			similarity1 += maxSimilarity;
		}
		similarity1 /= rules1.size();

		for (Rule r2 : rules2) {
			double maxSimilarity = 0.0;
			for (Rule r1 : rules1) {
				double currentSimilarity = syntacticSimilarityOfRules(r2, r1,
						config);
				if (Double.compare(currentSimilarity, maxSimilarity) > 0) {
					maxSimilarity = currentSimilarity;
				}
			}
			similarity2 += maxSimilarity;
		}
		similarity2 /= rules2.size();

		return Math.max(similarity1, similarity2);
	}

	public static double syntacticSimilarityOfRules(Rule r1, Rule r2,
			Config config) {
		double uaeSimilarity = 0.0;
		double paeSimilarity = 0.0;
		double conSimilarity = 0.0;
		double opsSimilarity = 0.0;

		// System.out.println(r1);
		// System.out.println(r2);
		// compute syntactic similarity of UAEs
		int denominator = config.getUserAttrSet().keySet().size();
		for (String uattr : config.getUserAttrSet().keySet()) {
			// System.out.println("User attribute:" + uattr);
			boolean found1 = false;
			// look for uattr in c1
			for (AttrValConjunct c1 : r1.getUAE()) {
				if (c1.getLHS().equals(uattr)) {
					boolean found2 = false;
					for (AttrValConjunct c2 : r2.getUAE()) {
						if (c2.getLHS().equals(uattr)) {
							// uattr appears in both c1 and c2
							if (config.getUserAttrSet().get(c1.getLHS())
									.getvType() == ValueType.Single) {
								uaeSimilarity += setSimilarity(c1.getRHS(),
										c2.getRHS());
							} else {
								uaeSimilarity += setSimilarity(c1.getRHSet(),
										c2.getRHSet());
							}
							// System.out.println("Case 1:");
							// System.out.println(c1);
							// System.out.println(c2);
							// System.out.println(uaeSimilarity);
							found2 = true;
							break;
						}
					}
					if (found2 == false) {
						// System.out.println("Case 2:");
						if (config.getUserAttrSet().get(c1.getLHS()).getvType() == ValueType.Single) {
							uaeSimilarity += (double) c1.getRHS().size()
									/ config.getUserAttrSet().get(uattr)
											.getDomain().size();
							// System.out.println(c1);
							// System.out.println(config.getUserAttrSet().get(uattr).getDomain());
							// System.out.println(uaeSimilarity);
						} else {
							uaeSimilarity += (double) c1.getRHSet().size()
									/ config.getUserAttrSet().get(uattr)
											.getSetDomain().size();
							// System.out.println(c1);
							// System.out.println(config.getUserAttrSet().get(uattr).getSetDomain());
							// System.out.println(uaeSimilarity);
						}
					}
					found1 = true;
					break;
				}
			}
			if (found1 == false) {
				boolean found2 = false;
				for (AttrValConjunct c2 : r2.getUAE()) {
					if (c2.getLHS().equals(uattr)) {
						// System.out.println("Case 3:");
						if (config.getUserAttrSet().get(c2.getLHS()).getvType() == ValueType.Single) {
							uaeSimilarity += (double) c2.getRHS().size()
									/ config.getUserAttrSet().get(uattr)
											.getDomain().size();
							// System.out.println(c2);
							// System.out.println(config.getUserAttrSet().get(uattr).getDomain());
							// System.out.println(uaeSimilarity);
						} else {
							uaeSimilarity += (double) c2.getRHSet().size()
									/ config.getUserAttrSet().get(uattr)
											.getSetDomain().size();
						}
						found2 = true;
						break;
					}
				}
				if (found2 == false) {
					// uaeSimilarity += 1.0;
					denominator -= 1;
					// System.out.println("Case 4:");
					// System.out.println(uaeSimilarity);
				}
			}
		}
		if (denominator > 0) {
			uaeSimilarity = uaeSimilarity / denominator;
		} else {
			uaeSimilarity = 1.0;
		}

		denominator = config.getResourceAttrSet().keySet().size();

		for (String pattr : config.getResourceAttrSet().keySet()) {
			boolean found1 = false;
			for (AttrValConjunct c1 : r1.getRAE()) {
				if (c1.getLHS().equals(pattr)) {
					boolean found2 = false;
					for (AttrValConjunct c2 : r2.getRAE()) {
						if (c2.getLHS().equals(pattr)) {
							if (config.getResourceAttrSet().get(pattr)
									.getvType() == ValueType.Single) {
								paeSimilarity += setSimilarity(c1.getRHS(),
										c2.getRHS());
							} else {
								paeSimilarity += setSimilarity(c1.getRHSet(),
										c2.getRHSet());
							}
							found2 = true;
							break;
						}
					}
					if (found2 == false) {
						if (config.getResourceAttrSet().get(pattr).getvType() == ValueType.Single) {
							paeSimilarity += (double) c1.getRHS().size()
									/ config.getResourceAttrSet().get(pattr)
											.getDomain().size();
						} else {
							paeSimilarity += (double) c1.getRHSet().size()
									/ config.getResourceAttrSet().get(pattr)
											.getSetDomain().size();
						}
					}
					found1 = true;
					break;
				}
			}
			if (found1 == false) {
				boolean found2 = false;
				for (AttrValConjunct c2 : r2.getRAE()) {
					if (c2.getLHS().equals(pattr)) {
						if (config.getResourceAttrSet().get(pattr).getvType() == ValueType.Single) {
							paeSimilarity += (double) c2.getRHS().size()
									/ config.getResourceAttrSet().get(pattr)
											.getDomain().size();
						} else {
							paeSimilarity += (double) c2.getRHSet().size()
									/ config.getResourceAttrSet().get(pattr)
											.getSetDomain().size();
						}
						found2 = true;
						break;
					}
				}
				if (found2 == false) {
					denominator -= 1;
				}
			}
		}
		if (denominator > 0) {
			paeSimilarity = paeSimilarity / denominator;
		} else {
			paeSimilarity = 1.0;
		}

		conSimilarity = constraintSimilarity(
				new HashSet<AttrAttrConjunct>(r1.getCon()),
				new HashSet<AttrAttrConjunct>(r2.getCon()));

		opsSimilarity = setSimilarity(r1.getOps(), r2.getOps());
		return (uaeSimilarity + paeSimilarity + conSimilarity + opsSimilarity) / 4;
	}

	public static HashSet<Triple<String, String, String>> computeRuleOverlap(
			Rule r1, Rule r2, Config config) {

		if (r1.getCoveredUPTriple() == null || r1.isChanged()) {
			r1.setCoveredUPTriple(new HashSet<Triple<String, String, String>>(
					computeCoveredUPTriple(r1, config)));
		}
		HashSet<Triple<String, String, String>> overlap = new HashSet<Triple<String, String, String>>(
				r1.getCoveredUPTriple());
		if (r2.getCoveredUPTriple() == null || r2.isChanged()) {
			r2.setCoveredUPTriple(new HashSet<Triple<String, String, String>>(
					computeCoveredUPTriple(r2, config)));
		}
		overlap.retainAll(r2.getCoveredUPTriple());
		return overlap;
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

	public static void outputResult(Config config, ArrayList<Rule> ruleList) {
		int numRules = 1;
		int totalWSC = 0;
		double wsc = 0;

		int[] maxSimilarityArray = new int[ruleList.size()];

		for (int i = 0; i < ruleList.size(); i++) {
			Rule r1 = ruleList.get(i);
			double maxSimilarity = -1;
			double currSimilarity = 0;

			for (int j = 0; j < config.getRuleList().size(); j++) {
				Rule r2 = config.getRuleList().get(j);
				currSimilarity = ruleSimilarity(r1, r2, config);
				if (Double.compare(currSimilarity, maxSimilarity) > 0) {
					maxSimilarityArray[i] = j;
					maxSimilarity = currSimilarity;
				}
			}
		}

		System.out.println("==============================================");
		System.out.println("OUTPUT RULES\n");
		for (Rule r : ruleList) {
			System.out.println(numRules++ + ".\n" + r);
			wsc = r.getSize();
			totalWSC += wsc;
			System.out.println("WSC=" + wsc);
		}
		System.out.println("\ntotal WSC=" + totalWSC);

		System.out.println("==============================================");
		System.out.println("INPUT-OUTPUT CORRESPONDENCE");
		for (int i = 0; i < config.getRuleList().size(); i++) {
			System.out.println((i + 1) + ". " + config.getRuleList().get(i));
			System.out.println("SIMILAR OUTPUT RULES:");
			for (int j = 0; j < maxSimilarityArray.length; j++) {
				if (maxSimilarityArray[j] == i) {
					System.out.print(ruleList.get(j));
					System.out.format(
							", similarity: %.2f",
							ruleSimilarity(config.getRuleList().get(i),
									resultRules.get(j), config));
					System.out.println();
				}
			}
			System.out.println();
		}
	}

	public static int computeAffectedUPTriples(Config config,
			HashSet<Triple<String, String, String>> originalUP) {
		HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
		for (Rule r : config.getRuleList()) {
			HashSet<String> satUsers = new HashSet<String>();
			HashSet<String> satPerms = new HashSet<String>();
			for (String u : config.getUsers()) {
				if (Parser.satisfyingRule(u, r, config, true)) {
					satUsers.add(u);
				}
			}
			for (String p : config.getResources()) {
				if (Parser.satisfyingRule(p, r, config, false)) {
					satPerms.add(p);
				}
			}
			for (String u : satUsers) {
				for (String p : satPerms) {
					if (Parser.satisfyingRuleConstraints(u, p, r, config)) {
						for (String op : r.getOps()) {
							config.getUserPerms().get(u)
									.add(new Pair<String, String>(op, p));
							Pair<String, String> perm = new Pair<String, String>(
									op, p);
							if (!config.getPermUsers().containsKey(perm)) {
								config.getPermUsers().put(perm,
										new HashSet<String>());
							}
							config.getPermUsers().get(perm).add(u);
							coveredUP.add(new Triple<String, String, String>(u,
									op, p));
						}
					}
				}
			}
		}

		HashSet<Triple<String, String, String>> differenceSet1 = new HashSet<Triple<String, String, String>>(
				originalUP);
		differenceSet1.removeAll(coveredUP);
		HashSet<Triple<String, String, String>> differenceSet2 = new HashSet<Triple<String, String, String>>(
				coveredUP);
		differenceSet2.removeAll(originalUP);
		return differenceSet1.size() + differenceSet2.size();
	}

	public static void addAttributeNoise(Config config, double anoiseRatio) {
		HashSet<Triple<String, String, String>> originalUP = new HashSet<Triple<String, String, String>>(
				config.getCoveredUP());
		int numANoise = (int) (originalUP.size() * anoiseRatio);
		int affectedUPTuples = 0;
		HashSet<Pair<String, String>> changedUserAttrPairs = new HashSet<Pair<String, String>>();
		HashSet<Pair<String, String>> changedResourceAttrPairs = new HashSet<Pair<String, String>>();
		Random rand = new Random(System.currentTimeMillis());
		while (true) {
			if (rand.nextFloat() <= 0.5) {
				// change user attribute
				String user = Experiment.randomElement(config.getUsers());
				if (rand.nextFloat() <= 0.5) {
					// replace a non-bottom value with the value of the
					// same attribute from a randomly selected user
					if (config.getUserAttrInfo().get(user).keySet().size() == 1) {
						continue;
					} else {
						String uAttr = Experiment
								.randomElement(new HashSet<String>(config
										.getUserAttrInfo().get(user).keySet()));
						while (uAttr.equals("uid")) {
							uAttr = Experiment
									.randomElement(new HashSet<String>(config
											.getUserAttrInfo().get(user)
											.keySet()));
						}
						Pair<String, String> userAttrPair = new Pair<String, String>(
								user, uAttr);
						if (changedUserAttrPairs.contains(userAttrPair)) {
							continue;
						} else {
							changedUserAttrPairs.add(userAttrPair);
						}
						HashSet<String> uAttrValueSet = new HashSet<String>();
						if (config.getUserAttrSet().get(uAttr).getvType() == ValueType.Single) {
							if (config.getUserAttrSet().get(uAttr).getDomain()
									.size() == 1) {
								continue;
							}
							uAttrValueSet.add(Experiment.randomElement(config
									.getUserAttrSet().get(uAttr).getDomain()));
						} else {
							if (config.getUserAttrSet().get(uAttr)
									.getSetDomain().size() == 1) {
								continue;
							}
							uAttrValueSet = new HashSet<String>(
									Experiment.randomElement(config
											.getUserAttrSet().get(uAttr)
											.getSetDomain()));
						}
						if (uAttrValueSet.equals(config.getUserAttrInfo()
								.get(user).get(uAttr))) {
							continue;
						}
						HashSet<String> uAttrValueSetOld = config
								.getUserAttrInfo().get(user).get(uAttr);
						config.getUserAttrInfo().get(user)
								.put(uAttr, uAttrValueSet);
						affectedUPTuples = computeAffectedUPTriples(config,
								originalUP);
						if (affectedUPTuples > numANoise) {
							config.getUserAttrInfo().get(user)
									.put(uAttr, uAttrValueSetOld);
							changedUserAttrPairs.remove(userAttrPair);
							break;
						}
					}
				} else {
//					if (true) {
//						continue;
//					}
					// replace a non-bottom value with bottom
					if (config.getUserAttrInfo().get(user).keySet().size() == 1) {
						continue;
					}
					String uAttr = Experiment
							.randomElement(new HashSet<String>(config
									.getUserAttrInfo().get(user).keySet()));
					while (uAttr.equals("uid")) {
						uAttr = Experiment.randomElement(new HashSet<String>(
								config.getUserAttrInfo().get(user).keySet()));
					}
					Pair<String, String> userAttrPair = new Pair<String, String>(
							user, uAttr);
					if (changedUserAttrPairs.contains(userAttrPair)) {
						continue;
					} else {
						changedUserAttrPairs.add(userAttrPair);
					}
					HashSet<String> uAttrValueSetOld = config.getUserAttrInfo()
							.get(user).get(uAttr);
					config.getUserAttrInfo().get(user).remove(uAttr);
					//config.getUserAttrInfo().get(user).put(uAttr, new HashSet<String>());
					affectedUPTuples = computeAffectedUPTriples(config,
							originalUP);
					if (affectedUPTuples > numANoise) {
						config.getUserAttrInfo().get(user)
								.put(uAttr, uAttrValueSetOld);
						changedUserAttrPairs.remove(userAttrPair);
						break;
					}
				}
			} else {
				// change resource attribute
				String resource = Experiment.randomElement(config
						.getResources());
				if (rand.nextFloat() <= 0.5) {
					// replace a non-bottom value with the value of the
					// same attribute from a randomly selected user
					if (config.getResourceAttrInfo().get(resource).keySet()
							.size() == 1) {
						continue;
					} else {
						String rAttr = Experiment
								.randomElement(new HashSet<String>((config
										.getResourceAttrSet().keySet())));
						while (rAttr.equals("rid")) {
							rAttr = Experiment
									.randomElement(new HashSet<String>(config
											.getResourceAttrSet().keySet()));
						}
						Pair<String, String> resourceAttrPair = new Pair<String, String>(
								resource, rAttr);
						if (changedResourceAttrPairs.contains(resourceAttrPair)) {
							continue;
						} else {
							changedResourceAttrPairs.add(resourceAttrPair);
						}
						HashSet<String> rAttrValueSet = new HashSet<String>();
						if (config.getResourceAttrSet().get(rAttr).getvType() == ValueType.Single) {
							if (config.getResourceAttrSet().get(rAttr)
									.getDomain().size() == 1) {
								continue;
							}
							rAttrValueSet.add(Experiment.randomElement(config
									.getResourceAttrSet().get(rAttr)
									.getDomain()));
						} else {
							if (config.getResourceAttrSet().get(rAttr)
									.getSetDomain().size() == 1) {
								continue;
							}
							rAttrValueSet = new HashSet<String>(
									Experiment.randomElement(config
											.getResourceAttrSet().get(rAttr)
											.getSetDomain()));
						}
						if (rAttrValueSet.equals(config.getResourceAttrInfo()
								.get(resource).get(rAttr))) {
							continue;
						}
						HashSet<String> rAttrValueSetOld = config
								.getResourceAttrInfo().get(resource).get(rAttr);
						config.getResourceAttrInfo().get(resource)
								.put(rAttr, rAttrValueSet);
						affectedUPTuples = computeAffectedUPTriples(config,
								originalUP);
						if (affectedUPTuples > numANoise) {
							config.getResourceAttrInfo().get(resource)
									.put(rAttr, rAttrValueSetOld);
							changedResourceAttrPairs.remove(resourceAttrPair);
							break;
						}
					}
				} else {
//					if (true) {
//						continue;
//					}
					if (config.getResourceAttrInfo().get(resource).keySet()
							.size() == 1) {
						continue;
					}
					String rAttr = Experiment
							.randomElement(new HashSet<String>(config
									.getResourceAttrInfo().get(resource)
									.keySet()));
					while (rAttr.equals("rid")) {
						rAttr = Experiment.randomElement(new HashSet<String>(
								config.getResourceAttrInfo().get(resource)
										.keySet()));
					}
					Pair<String, String> resourceAttrPair = new Pair<String, String>(
							resource, rAttr);
					if (changedResourceAttrPairs.contains(resourceAttrPair)) {
						continue;
					} else {
						changedResourceAttrPairs.add(resourceAttrPair);
					}
					HashSet<String> rAttrValueSetOld = config
							.getResourceAttrInfo().get(resource).get(rAttr);
					config.getResourceAttrInfo().get(resource).remove(rAttr);
					//config.getResourceAttrInfo().get(resource).put(rAttr, new HashSet<String>());
					affectedUPTuples = computeAffectedUPTriples(config,
							originalUP);
					if (affectedUPTuples > numANoise) {
						config.getResourceAttrInfo().get(resource)
								.put(rAttr, rAttrValueSetOld);
						changedResourceAttrPairs.remove(resourceAttrPair);
						break;
					}
				}
			}
		}
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
			// System.out.println(config.getLogToProbability());
			for (LogEntry log : logs) {
				Triple<String, String, String> logTriple = new Triple<String, String, String>(
						log.user, log.op, log.resource);

				bw.write(log.toString() + " "
						+ config.getLogToProbability().get(logTriple));
				// System.out.println(logTriple + " "
				// + config.getLogToProbability().get(logTriple));
				bw.write("\n");
			}
			bw.write("\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		Double defaultLogCompleteness = 0.6;

		if (args[0].charAt(1) == 's') {
			double[][][][] numUsers = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_NOISES][NUMBER_OF_TEST_CASES];
			double[][][][] numResources = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_NOISES][NUMBER_OF_TEST_CASES];
			double[][][][] numOps = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_NOISES][NUMBER_OF_TEST_CASES];
			double[][][][] numCoveredUP = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_NOISES][NUMBER_OF_TEST_CASES];

			double[][][][] syntacticSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_NOISES][NUMBER_OF_TEST_CASES];
			double[][][][] overAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_NOISES][NUMBER_OF_TEST_CASES];
			double[][][][] underAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_NOISES][NUMBER_OF_TEST_CASES];
			double[][][][] jaccardSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_NOISES][NUMBER_OF_TEST_CASES];

			double[][][][] overAssignmentSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_NOISES][NUMBER_OF_TEST_CASES];
			double[][][][] underAssignmentSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_NOISES][NUMBER_OF_TEST_CASES];
            //iterate over test cases first
			for (int i = 0; i < NUMBER_OF_TEST_CASES; i++) {
				int Nrule = Integer.parseInt(args[1]);
				String outputFile = "synthetic_noise/synthetic_noise_" + i
						+ "_" + Nrule + ".abac";
				boolean debugMode = false;

				int minNumConjuncts = 0;
				int minNumConstraints = 0;
				double pOverlapRule = 0.0;

				SyntheticPolicyCaseStudyGenerator.generateCaseStudy(outputFile,
						null, Nrule, debugMode, true, minNumConjuncts,
						minNumConstraints, 0, 0, pOverlapRule);

				for (int j = 0; j < NUMBER_OF_NOISES; j++) {
					ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
					for (int k = 6; k < 6 + NUMBER_OF_STEPS; k++) {
						Parser.config = new Config();
						Parser.parseInputABACFile(outputFile);
						

						String distOutputFile = "synthetic_noise/synthetic_noise"
								+ "_" + i + "_" + Nrule + "_" + 25 + ".dist";
						SyntheticDistGenerator.distributionGenerator(
								Parser.config, 25.0, distOutputFile);
						Parser.parseInputDistFile(distOutputFile);
						double logCompleteness = ((double) k) / 10;
						
						double noiseRatio = noiseRatios[j];
						// only changes attribute date
						addAttributeNoise(Parser.config, noiseRatio / 6);		
						logs = LogGenerator.generateLog(Parser.config,
								logCompleteness, logs, noiseRatio * 5 / 6);

						String logFile = "logs/synthetic_noise/synthetic_noise"
								+ args[1] + "_" + j + "_" + i + ".log";
						outputLogsToFile(logs, Parser.config, logFile);
											
						HashSet<Triple<String, String, String>> logInducedUP = new HashSet<Triple<String, String, String>>();
						HashMap<Triple<String, String, String>, Double> logToFrequency = Parser.config
								.getLogToProbability();
						for (LogEntry log : logs) {
							Triple<String, String, String> userperm = new Triple<String, String, String>(
									log.user, log.op, log.resource);
							logInducedUP.add(userperm);
						}
						Parser.config.setLogInducedUP(logInducedUP);
						Parser.config.setLogToProbability(logToFrequency);
						Parser.config.setLogs(logs);

						for (int n = 0; n < NUMBER_RULE_METRICS; n++) {
							RuleQualityMetricType type = RuleQualityMetricType
									.values()[n];
							System.out.println("RuleQualityMetricType: "
									+ type.toString());
							System.out.println("Fraction: " + (double) k / 10);
							ABACMiner.W0_policy = weights[k - 6];
							ABACMiner.W0_rule = ((double) weights[k - 6]) / 10.0;
							ABACMiner.overAssignThres = 8;
							ABACMiner.underAssignFrac = 0.08;
							ABACMiner.mineABACPolicy(Parser.config, false, n);
							ABACMiner.outputResult(Parser.config,
									ABACMiner.resultRules);
							double similarity = ABACMiner
									.symmetricSyntacticSimilarityOfPolicies(
											Parser.config.getRuleList(),
											ABACMiner.resultRules,
											Parser.config);
							HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
							for (Rule r : ABACMiner.resultRules) {
								coveredUP.addAll(r.getCoveredUPTriple());
							}
							HashSet<Triple<String, String, String>> overAssignment = new HashSet<Triple<String, String, String>>(
									coveredUP);
							overAssignment.removeAll(Parser.config
									.getCoveredUP());
							HashSet<Triple<String, String, String>> underAssignment = new HashSet<Triple<String, String, String>>(
									Parser.config.getCoveredUP());
							underAssignment.removeAll(coveredUP);
							double overAssignmentRatio = (double) overAssignment
									.size()
									/ Parser.config.getCoveredUP().size();
							double underAssignmentRatio = (double) underAssignment
									.size()
									/ Parser.config.getCoveredUP().size();
							syntacticSimilarities[k - 6][n][j][i] = similarity;
							overAssignmentRatios[k - 6][n][j][i] = overAssignmentRatio;
							underAssignmentRatios[k - 6][n][j][i] = underAssignmentRatio;

							numUsers[k - 6][n][j][i] = Parser.config
									.getNumUsers();
							numResources[k - 6][n][j][i] = Parser.config
									.getResources().size();
							numOps[k - 6][n][j][i] = Parser.config.getOps()
									.size();
							numCoveredUP[k - 6][n][j][i] = coveredUP.size();
							jaccardSimilarities[k - 6][n][j][i] = jaccardSimilarity(
									coveredUP, Parser.config.getCoveredUP());
						}
					}
				}
			}
			String outputFile = "output/synthetic_noise/synthetic_" + args[1]
					+ "_" + ".output";
			try {
				File file = new File(outputFile);
				// if file doesnt exists, then create it
				if (!file.exists()) {
					file.createNewFile();
				}

				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				for (int i = 0; i < NUMBER_RULE_METRICS; i++) {
					bw.write((RuleQualityMetricType.values()[i]).toString()
							+ "\n");
					for (int j = 0; j < NUMBER_OF_STEPS; j++) {
						Double fraction = j * 0.1 + 0.6;
						bw.write(fraction + " ");
						for (int k = 0; k < NUMBER_OF_NOISES; k++) {
							Double noise = noiseRatios[k];
							bw.write(noise
									+ " "
									+ doubleArrayAverage(numUsers[j][i][k])
									+ " "
									+ doubleArraySTDV(numUsers[j][i][k])
									+ " "
									+ doubleArrayAverage(numResources[j][i][k])
									+ " "
									+ doubleArraySTDV(numResources[j][i][k])
									+ " "
									+ doubleArrayAverage(numOps[j][i][k])
									+ " "
									+ doubleArraySTDV(numOps[j][i][k])
									+ " "
									+ doubleArrayAverage(numCoveredUP[j][i][k])
									+ " "
									+ doubleArraySTDV(numCoveredUP[j][i][k])
									+ " "
									+ doubleArrayAverage(syntacticSimilarities[j][i][k])
									+ " "
									+ doubleArraySTDV(syntacticSimilarities[j][i][k])
									+ " "
									+ doubleArrayAverage(overAssignmentRatios[j][i][k])
									+ " "
									+ doubleArraySTDV(overAssignmentRatios[j][i][k])
									+ " "
									+ doubleArrayAverage(underAssignmentRatios[j][i][k])
									+ " "
									+ doubleArraySTDV(underAssignmentRatios[j][i][k])
									+ " "
									+ doubleArrayAverage(jaccardSimilarities[j][i][k])
									+ " "
									+ doubleArraySTDV(jaccardSimilarities[j][i][k])
									+ "\n");
						}
					}
				}
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		else if (args[0].charAt(1) == 'n') {
			// noise version
			if (args.length < 4) {
				System.err
						.println("Two more arguments needed for -m mode. 1. abac file (String) 2. dist file (String) 3. noise ratio"
								+ "Optional arguments 4.rule quality 5. log file 6. output file 7. log completeness");
				System.exit(1);
			}
			String inputABACFile = args[1];
			String inputDistFile = args[2];
			double noiseRatio = Double.parseDouble(args[3]);

			int ruleQuality = 0;
			if (args.length >= 5) {
				ruleQuality = Integer.parseInt(args[4]);
			}

			String outputLogFile;
			if (args.length >= 6) {
				outputLogFile = args[5];
			} else {
				outputLogFile = "logs/"
						+ args[1].substring(0, args[1].length() - 5) + ".log";
			}

			String outputFile;
			if (args.length >= 7) {
				outputFile = args[6];
			} else {
				outputFile = "output/"
						+ args[1].substring(0, args[1].length() - 5)
						+ ".output";
			}

			double logCompleteness;
			if (args.length >= 8) {
				logCompleteness = Double.parseDouble(args[7]);
			} else {
				logCompleteness = defaultLogCompleteness;
			}

			Parser.config = new Config();

			Parser.parseInputABACFile(inputABACFile);

			Parser.parseInputDistFile(inputDistFile);

			ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
			logs = LogGenerator.generateLog(Parser.config, logCompleteness,
					logs, noiseRatio * 5 / 6);

			addAttributeNoise(Parser.config, noiseRatio / 6);

			HashSet<Triple<String, String, String>> logInducedUP = new HashSet<Triple<String, String, String>>();
			for (LogEntry log : logs) {
				Triple<String, String, String> userperm = new Triple<String, String, String>(
						log.user, log.op, log.resource);
				logInducedUP.add(userperm);
			}
			Parser.config.setLogInducedUP(logInducedUP);
			Parser.config.setLogs(logs);
			ABACMiner.mineABACPolicy(Parser.config, false, ruleQuality);
			outputResult(Parser.config, ABACMiner.resultRules);
		}
		// else if (args[0].charAt(1) == 'm') {
		// if (args.length < 3) {
		// System.err
		// .println("Two more arguments needed for -m mode. 1. abac file (String) 2. dist file (String)"
		// +
		// "Optional arguments 3.rule quality 4. log file 5. output file 6. log completeness 7. noise ratio");
		// System.exit(1);
		// }
		// String inputABACFile = args[1];
		// String inputDistFile = args[2];
		//
		// int ruleQuality = 0;
		// if (args.length >= 4) {
		// ruleQuality = Integer.parseInt(args[3]);
		// }
		//
		// String outputLogFile;
		// if (args.length >= 5) {
		// outputLogFile = args[4];
		// } else {
		// outputLogFile = "logs/"
		// + args[1].substring(0, args[1].length() - 5) + ".log";
		// }
		//
		// String outputFile;
		// if (args.length >= 6) {
		// outputFile = args[5];
		// } else {
		// outputFile = "output/"
		// + args[1].substring(0, args[1].length() - 5)
		// + ".output";
		// }
		//
		// double logCompleteness;
		// if (args.length >= 7) {
		// logCompleteness = Double.parseDouble(args[6]);
		// } else {
		// logCompleteness = defaultLogCompleteness;
		// }
		//
		// Parser.config = new Config();
		//
		// Parser.parseInputABACFile(inputABACFile);
		//
		// Parser.parseInputDistFile(inputDistFile);
		//
		// ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
		// logs = LogGenerator.generateLog(Parser.config, logCompleteness,
		// logs, 0.0);
		//
		// HashSet<Triple<String, String, String>> logInducedUP = new
		// HashSet<Triple<String, String, String>>();
		// for (LogEntry log : logs) {
		// Triple<String, String, String> userperm = new Triple<String, String,
		// String>(
		// log.user, log.op, log.resource);
		// logInducedUP.add(userperm);
		// }
		// Parser.config.setLogInducedUP(logInducedUP);
		// Parser.config.setLogs(logs);
		// ABACMiner.mineABACPolicy(Parser.config, false, ruleQuality);
		// outputResult(Parser.config, ABACMiner.resultRules);
		// } else if (args[0].charAt(1) == 'r') {
		// // Generate case studies with synthetic attribute data
		// if (args.length < 3) {
		// System.err
		// .println("Two arguments are required for -r mode. 1. case-study type(university, healthcare, projectmanagement) 2. Size N "
		// + "Optional arguments 3. outputABACFile 4. outputDistFile");
		// System.exit(1);
		// }
		// Parser.config = new Config();
		// CaseStudyGenerator c = CaseStudyGenerator
		// .createGenerator(args[1]);
		// int N = Integer.parseInt(args[2]);
		//
		// String outputABACFile;
		// if (args.length >= 4) {
		// outputABACFile = args[3];
		// } else {
		// outputABACFile = "case-studies-synthetic/" + args[1] + "_"
		// + N + ".abac";
		// }
		//
		// c.generateCaseStudy(outputABACFile, N, 0, 0);
		// Parser.parseInputABACFile(outputABACFile);
		// String outputDistFile;
		// if (args.length >= 5) {
		// outputDistFile = args[4];
		// } else {
		// outputDistFile = "case-studies-synthetic/" + args[1] + "_"
		// + N + "_" + 25 + ".dist";
		// ;
		// }
		// SyntheticDistGenerator.distributionGenerator(Parser.config,
		// 25.0, outputDistFile);
		// } else if (args[0].charAt(1) == 's') {
		// if (args.length < 2) {
		// System.err
		// .println("One argument is required for -s mode. 1. Number of rules "
		// + "Optional arguments 2. outputABACFile 3. outputDistFile");
		// System.exit(1);
		// }
		// Parser.config = new Config();
		// int Nrule = Integer.parseInt(args[1]);
		// String outputABACFile;
		// if (args.length >= 3) {
		// outputABACFile = args[2];
		// } else {
		// outputABACFile = "synthetic/synthetic_" + "_" + Nrule
		// + ".abac";
		// }
		//
		// String outputDistFile;
		// if (args.length >= 4) {
		// outputDistFile = args[3];
		// } else {
		// outputDistFile = "synthetic/synthetic_" + "_" + "_" + Nrule
		// + "_" + 25 + ".dist";
		// }
		//
		// boolean debugMode = false;
		//
		// int minNumConjuncts = 1;
		//
		// int minNumConstraints = 1;
		//
		// double pOverlapRule = 0.0;
		//
		// SyntheticPolicyCaseStudyGenerator.generateCaseStudy(
		// outputABACFile, null, Nrule, debugMode, true,
		// minNumConjuncts, minNumConstraints, 0, 0, pOverlapRule);
		// Parser.parseInputABACFile(outputABACFile);
		//
		// SyntheticDistGenerator.distributionGenerator(Parser.config,
		// 25.0, outputDistFile);
		// }
	}
}
