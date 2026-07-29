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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Pattern;

import edu.dar.util.CaseStudyGenerator;
import edu.dar.util.Config;
import edu.dar.util.LogEntry;
import edu.dar.util.Pair;
import edu.dar.util.Parser;
import edu.dar.util.Rule;
import edu.dar.util.RuleQualityMetricType;
import edu.dar.util.SyntheticPolicyCaseStudyGenerator;
import edu.dar.util.Triple;
import edu.dar.util.ValueType;

public class Experiment {
	public static int NUMBER_OF_TEST_CASES = 10;
	public static int NUMBER_OF_OVERASSIGNMENT_TEST_CASES = 10;
	public static int NUMBER_RULE_METRICS = 10;
	public static int NUMBER_OF_STEPS = 5;
	public static final String VERBOSE_OPTION = "-verbose";
	public static final int [] weights = {15, 20, 25, 30, 35};
	
//	public static final int[][] weights = {{ 12, 15, 15, 18, 25 },
//	{ 40, 37, 40, 41, 41 },
//	{ 14, 16, 19, 25, 25 },
//	{ 15, 17, 21, 23, 25 },
//	{ 38, 38, 44, 40, 42 },
//	{ 12, 19, 24, 34, 34 }};
	
	//public static final int[] ruleMetrics = {2/*1,2,3,7,8,9*/};

	public static final double[] noiseRatios = new double[] { /* 0.01, 0.02, */0.03 
																				  ,
																				  0.06
																				  ,
																				  0.09
																				  ,
																				  0.12
																				  ,
																				  0.15
																				  ,
																				  0.18
																				  ,
																				  0.21
																				 };

	public static final double[] TAU = new double[] { 1, 2, 3, 4, 5, 6, 7, 8 /*
																			 * ,
																			 * 9
																			 * ,
																			 * 10
																			 * ,
																			 * 11
																			 * ,
																			 * 12
																			 * ,
																			 * 13
																			 * ,
																			 * 14
																			 * ,
																			 * 15
																			 */};
	public static final double[] ALPHA = new double[] { 0.01, 0.02, 0.03, 0.04,
			0.05, 0.06, 0.07, 0.08 /*
									 * , 0.09, 0.10, 0.15, 0.18, 0.21
									 */};

	public static double intArrayAverage(int[] a) {
		int sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i];
		}
		return (double) sum / a.length;
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
			//System.out.println(config.getLogToProbability());
			for (LogEntry log : logs) {
				Triple<String, String, String> logTriple = new Triple<String, String, String>(
						log.user, log.op, log.resource);

				bw.write(log.toString() + " "
						+ config.getLogToProbability().get(logTriple));
				//System.out.println(logTriple + " "
						//+ config.getLogToProbability().get(logTriple));
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

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Too few arguments!");
			System.exit(1);
		} else {
			if (args[0].length() != 2 || args[0].charAt(0) != '-') {
				System.err
						.println("The first argument specifies the excution mode, which should begin with '-' ");
				System.exit(1);
			}
			// synthetic policies with noise
			if (args[0].charAt(1) == 'n') {
				for (double noise : noiseRatios) {
					double[][][] numUsers = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
					double[][][] numResources = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
					double[][][] numOps = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
					double[][][] numCoveredUP = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

					double[][][] syntacticSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
					double[][][] overAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
					double[][][] underAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
					double[][][] jaccardSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

					for (int i = 0; i < NUMBER_OF_TEST_CASES; i++) {
						Parser.config = new Config();
						int Nrule = Integer.parseInt(args[1]);
						String outputFile = "synthetic_noise/synthetic_noise_" + i + "_"
								+ Nrule + ".abac";
						boolean debugMode = false;

						int minNumConjuncts = 0;

						int minNumConstraints = 0;

						double pOverlapRule = 0.0;

						SyntheticPolicyCaseStudyGenerator.generateCaseStudy(
								outputFile, null, Nrule, debugMode, true,
								minNumConjuncts, minNumConstraints, 0, 0,
								pOverlapRule);
						Parser.parseInputABACFile(outputFile);

						Config config = Parser.config;

						HashSet<Triple<String, String, String>> originalUP = new HashSet<Triple<String, String, String>>(
								config.getCoveredUP());

						// attribute noise

						int numANoise = (int) (originalUP.size() * noise / 6);
						int affectedUPTuples = 0;
						HashSet<Pair<String, String>> changedUserAttrPairs = new HashSet<Pair<String, String>>();
						HashSet<Pair<String, String>> changedResourceAttrPairs = new HashSet<Pair<String, String>>();
						Random rand = new Random(System.currentTimeMillis());
						while (true) {
							if (rand.nextFloat() <= 0.5) {
								// change user attribute
								String user = Experiment.randomElement(config
										.getUsers());
								if (rand.nextFloat() <= 0.5) {
									// replace a non-bottom value with the value
									// of the
									// same attribute from a randomly selected
									// user
									if (config.getUserAttrInfo().get(user)
											.keySet().size() == 1) {
										continue;
									} else {
										String uAttr = Experiment
												.randomElement(new HashSet<String>(
														config.getUserAttrInfo()
																.get(user)
																.keySet()));
										while (uAttr.equals("uid")) {
											uAttr = Experiment
													.randomElement(new HashSet<String>(
															config.getUserAttrInfo()
																	.get(user)
																	.keySet()));
										}
										Pair<String, String> userAttrPair = new Pair<String, String>(
												user, uAttr);
										if (changedUserAttrPairs
												.contains(userAttrPair)) {
											continue;
										} else {
											changedUserAttrPairs
													.add(userAttrPair);
										}
										HashSet<String> uAttrValueSet = new HashSet<String>();
										if (config.getUserAttrSet().get(uAttr)
												.getvType() == ValueType.Single) {
											if (config.getUserAttrSet()
													.get(uAttr).getDomain()
													.size() == 1) {
												continue;
											}
											uAttrValueSet.add(Experiment
													.randomElement(config
															.getUserAttrSet()
															.get(uAttr)
															.getDomain()));
										} else {
											if (config.getUserAttrSet()
													.get(uAttr).getSetDomain()
													.size() == 1) {
												continue;
											}
											uAttrValueSet = new HashSet<String>(
													Experiment
															.randomElement(config
																	.getUserAttrSet()
																	.get(uAttr)
																	.getSetDomain()));
										}
										if (uAttrValueSet.equals(config
												.getUserAttrInfo().get(user)
												.get(uAttr))) {
											continue;
										}
										HashSet<String> uAttrValueSetOld = config
												.getUserAttrInfo().get(user)
												.get(uAttr);
										config.getUserAttrInfo().get(user)
												.put(uAttr, uAttrValueSet);
										// System.out.println(changedAttrs +
										// " Add User: "
										// + user + " uAttr: " + uAttr
										// + "uAttrValue: " + uAttrValueSet);
										affectedUPTuples = computeAffectedUPTriples(
												config, originalUP);
										if (affectedUPTuples > numANoise) {
											config.getUserAttrInfo()
													.get(user)
													.put(uAttr,
															uAttrValueSetOld);
											changedUserAttrPairs
													.remove(userAttrPair);
											break;
										}
									}
								} else {
									// replace a non-bottom value with bottom
									if (config.getUserAttrInfo().get(user)
											.keySet().size() == 1) {
										continue;
									}
									String uAttr = Experiment
											.randomElement(new HashSet<String>(
													config.getUserAttrInfo()
															.get(user).keySet()));
									while (uAttr.equals("uid")) {
										uAttr = Experiment
												.randomElement(new HashSet<String>(
														config.getUserAttrInfo()
																.get(user)
																.keySet()));
									}
									Pair<String, String> userAttrPair = new Pair<String, String>(
											user, uAttr);
									if (changedUserAttrPairs
											.contains(userAttrPair)) {
										continue;
									} else {
										changedUserAttrPairs.add(userAttrPair);
									}
									HashSet<String> uAttrValueSetOld = config
											.getUserAttrInfo().get(user)
											.get(uAttr);
									config.getUserAttrInfo().get(user)
											.remove(uAttr);
									// System.out.println(changedAttrs +
									// " Remove User: "
									// + user + " uAttr: " + uAttr);
									affectedUPTuples = computeAffectedUPTriples(
											config, originalUP);
									if (affectedUPTuples > numANoise) {
										config.getUserAttrInfo().get(user)
												.put(uAttr, uAttrValueSetOld);
										changedUserAttrPairs
												.remove(userAttrPair);
										break;
									}
								}
							} else {
								// change resource attribute
								String resource = Experiment
										.randomElement(config.getResources());
								if (rand.nextFloat() <= 0.5) {
									// replace a non-bottom value with the value
									// of the
									// same attribute from a randomly selected
									// user
									if (config.getResourceAttrInfo()
											.get(resource).keySet().size() == 1) {
										continue;
									} else {
										String rAttr = Experiment
												.randomElement(new HashSet<String>(
														(config.getResourceAttrSet()
																.keySet())));
										while (rAttr.equals("rid")) {
											rAttr = Experiment
													.randomElement(new HashSet<String>(
															config.getResourceAttrSet()
																	.keySet()));
										}
										Pair<String, String> resourceAttrPair = new Pair<String, String>(
												resource, rAttr);
										if (changedResourceAttrPairs
												.contains(resourceAttrPair)) {
											continue;
										} else {
											changedResourceAttrPairs
													.add(resourceAttrPair);
										}
										HashSet<String> rAttrValueSet = new HashSet<String>();
										if (config.getResourceAttrSet()
												.get(rAttr).getvType() == ValueType.Single) {
											if (config.getResourceAttrSet()
													.get(rAttr).getDomain()
													.size() == 1) {
												continue;
											}
											rAttrValueSet
													.add(Experiment
															.randomElement(config
																	.getResourceAttrSet()
																	.get(rAttr)
																	.getDomain()));
										} else {
											if (config.getResourceAttrSet()
													.get(rAttr).getSetDomain()
													.size() == 1) {
												continue;
											}
											rAttrValueSet = new HashSet<String>(
													Experiment
															.randomElement(config
																	.getResourceAttrSet()
																	.get(rAttr)
																	.getSetDomain()));
										}
										if (rAttrValueSet.equals(config
												.getResourceAttrInfo()
												.get(resource).get(rAttr))) {
											continue;
										}
										HashSet<String> rAttrValueSetOld = config
												.getResourceAttrInfo()
												.get(resource).get(rAttr);
										config.getResourceAttrInfo()
												.get(resource)
												.put(rAttr, rAttrValueSet);
										// System.out.println(changedAttrs +
										// " Add Resource: "
										// + resource + " rAttr: " + rAttr
										// + "rAttrValue: " + rAttrValueSet);
										affectedUPTuples = computeAffectedUPTriples(
												config, originalUP);
										if (affectedUPTuples > numANoise) {
											config.getResourceAttrInfo()
													.get(resource)
													.put(rAttr,
															rAttrValueSetOld);
											changedResourceAttrPairs
													.remove(resourceAttrPair);
											break;
										}
									}
								} else {
									if (config.getResourceAttrInfo()
											.get(resource).keySet().size() == 1) {
										continue;
									}
									String rAttr = Experiment
											.randomElement(new HashSet<String>(
													config.getResourceAttrInfo()
															.get(resource)
															.keySet()));
									while (rAttr.equals("rid")) {
										rAttr = Experiment
												.randomElement(new HashSet<String>(
														config.getResourceAttrInfo()
																.get(resource)
																.keySet()));
									}
									Pair<String, String> resourceAttrPair = new Pair<String, String>(
											resource, rAttr);
									if (changedResourceAttrPairs
											.contains(resourceAttrPair)) {
										continue;
									} else {
										changedResourceAttrPairs
												.add(resourceAttrPair);
									}
									HashSet<String> rAttrValueSetOld = config
											.getResourceAttrInfo()
											.get(resource).get(rAttr);
									config.getResourceAttrInfo().get(resource)
											.remove(rAttr);
									// System.out.println(changedAttrs +
									// " Remove Resource: "
									// + resource + " rAttr: " + rAttr);
									affectedUPTuples = computeAffectedUPTriples(
											config, originalUP);
									if (affectedUPTuples > numANoise) {
										config.getResourceAttrInfo()
												.get(resource)
												.put(rAttr, rAttrValueSetOld);
										changedResourceAttrPairs
												.remove(resourceAttrPair);
										break;
									}
								}
							}
						}

						// Construct the UP relation entailed by the new
						// attribtue data
						// Construct UP relations from rules
						HashSet<Triple<String, String, String>> newCoveredUP = new HashSet<Triple<String, String, String>>();

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
									if (Parser.satisfyingRuleConstraints(u, p,
											r, config)) {
										for (String op : r.getOps()) {
											config.getUserPerms()
													.get(u)
													.add(new Pair<String, String>(
															op, p));
											Pair<String, String> perm = new Pair<String, String>(
													op, p);
											if (!config.getPermUsers()
													.containsKey(perm)) {
												config.getPermUsers().put(perm,
														new HashSet<String>());
											}
											config.getPermUsers().get(perm)
													.add(u);
											newCoveredUP
													.add(new Triple<String, String, String>(
															u, op, p));
										}
									}
								}
							}
						}

						HashSet<Triple<String, String, String>> attrOverAssignment = new HashSet<Triple<String, String, String>>(
								originalUP);
						attrOverAssignment.removeAll(newCoveredUP);

						HashSet<Triple<String, String, String>> attrUnderAssignment = new HashSet<Triple<String, String, String>>(
								newCoveredUP);
						attrUnderAssignment.removeAll(originalUP);

						String distOutputFile = "synthetic_noise/synthetic_noise"
								+ "_" + i + "_" + Nrule + "_" + 25 + ".dist";
						SyntheticDistGenerator.distributionGenerator(
								Parser.config, 25.0, distOutputFile);

						ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
						for (int j = 6; j <= 10; j += 1) {
							// Parser.config = new Config();
							// Parser.parseInputABACFile(outputFile);
							Parser.parseInputDistFile(distOutputFile);
							logs = LogGenerator.generateLog(Parser.config,
									(double) j / 10, logs, noise * 5 / 6);
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
								// if
								// (!logToFrequency.keySet().contains(userperm))
								// {
								// logToFrequency.put(userperm, 1);
								// } else {
								// logToFrequency.put(userperm,
								// logToFrequency.get(userperm) + 1);
								// }
							}
							Parser.config.setLogInducedUP(logInducedUP);
							// Parser.config.setLogToFrequency(logToFrequency);
							Parser.config.setLogs(logs);
							for (int k = 0; k < NUMBER_RULE_METRICS; k++) {
								// Parser.config = new Config();
								// Parser.parseInputABACFile(outputFile);
								// Parser.parseInputDistFile(distOutputFile);

								RuleQualityMetricType type = RuleQualityMetricType
										.values()[k];
								ABACMiner.W0_policy = weights[j - 6];
								ABACMiner.W0_rule = ((double) weights[j - 6]) / 10.0;
								ABACMiner.overAssignThres = 8;
								ABACMiner.underAssignFrac = 0.08;
								ABACMiner.mineABACPolicy(Parser.config, false,
										i);
								System.out.println("RuleQualityMetricType: "
										+ type.toString());
								System.out.println("Fraction: " + (double) j
										/ 10);
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
								overAssignment.removeAll(originalUP);
								HashSet<Triple<String, String, String>> underAssignment = new HashSet<Triple<String, String, String>>(
										originalUP);
								underAssignment.removeAll(coveredUP);
								double overAssignmentRatio = (double) overAssignment
										.size()
										/ Parser.config.getCoveredUP().size();
								double underAssignmentRatio = (double) underAssignment
										.size()
										/ Parser.config.getCoveredUP().size();
								syntacticSimilarities[j - 6][k][i] = similarity;
								overAssignmentRatios[j - 6][k][i] = overAssignmentRatio;
								underAssignmentRatios[j - 6][k][i] = underAssignmentRatio;

								numUsers[j - 6][k][i] = Parser.config
										.getNumUsers();
								numResources[j - 6][k][i] = Parser.config
										.getResources().size();
								numOps[j - 6][k][i] = Parser.config.getOps()
										.size();
								numCoveredUP[j - 6][k][i] = coveredUP.size();
								jaccardSimilarities[j - 6][k][i] = jaccardSimilarity(
										coveredUP, Parser.config.getCoveredUP());
							}
						}
					}
					String outputFile = "output/synthetic_noise/synthetic_"
							+ args[1] + "_" + (int) (noise * 100) + "_" + 1
							+ ".output";
					try {
						File file = new File(outputFile);
						// if file doesnt exists, then create it
						if (!file.exists()) {
							file.createNewFile();
						}

						FileWriter fw = new FileWriter(file.getAbsoluteFile());
						BufferedWriter bw = new BufferedWriter(fw);
						for (int i = 0; i < NUMBER_RULE_METRICS; i++) {
							bw.write((RuleQualityMetricType.values()[i])
									.toString() + "\n");
							for (int j = 0; j < NUMBER_OF_STEPS; j++) {
								Double fraction = j * 0.1 + 0.6;
								bw.write(fraction
										+ " "
										+ doubleArrayAverage(numUsers[j][i])
										+ " "
										+ doubleArraySTDV(numUsers[j][i])
										+ " "
										+ doubleArrayAverage(numResources[j][i])
										+ " "
										+ doubleArraySTDV(numResources[j][i])
										+ " "
										+ doubleArrayAverage(numOps[j][i])
										+ " "
										+ doubleArraySTDV(numOps[j][i])
										+ " "
										+ doubleArrayAverage(numCoveredUP[j][i])
										+ " "
										+ doubleArraySTDV(numCoveredUP[j][i])
										+ " "
										+ doubleArrayAverage(syntacticSimilarities[j][i])
										+ " "
										+ doubleArraySTDV(syntacticSimilarities[j][i])
										+ " "
										+ doubleArrayAverage(overAssignmentRatios[j][i])
										+ " "
										+ doubleArraySTDV(overAssignmentRatios[j][i])
										+ " "
										+ doubleArrayAverage(underAssignmentRatios[j][i])
										+ " "
										+ doubleArraySTDV(underAssignmentRatios[j][i])
										+ " "
										+ doubleArrayAverage(jaccardSimilarities[j][i])
										+ " "
										+ doubleArraySTDV(jaccardSimilarities[j][i])
										+ "\n");
							}
						}
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			// synthetic policies
			if (args[0].charAt(1) == 's') {
				double[][][] numUsers = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numResources = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numOps = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numCoveredUP = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				double[][][] syntacticSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] overAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] underAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] jaccardSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				double[][][] compressionRatio = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				int ruleMetric = 0;
				if (args.length >= 3) {
					ruleMetric = Integer.parseInt(args[2]);
				}
				for (int i = 0; i < NUMBER_OF_TEST_CASES; i++) {
					Parser.config = new Config();
					int Nrule = Integer.parseInt(args[1]);
					String outputFile = "synthetic/synthetic_" + i + "_"
							+ Nrule + "_new.abac";
					boolean debugMode = false;

					int minNumConjuncts = 1;

					int minNumConstraints = 1;

					double pOverlapRule = 0.0;

					SyntheticPolicyCaseStudyGenerator.generateCaseStudy(
							outputFile, null, Nrule, debugMode, true,
							minNumConjuncts, minNumConstraints, 0, 0,
							pOverlapRule);
					Parser.parseInputABACFile(outputFile);
					String distOutputFile = "synthetic/synthetic_" + "_" + i
							+ "_" + Nrule + "_" + 25 + "_new.dist";
					SyntheticDistGenerator.distributionGenerator(Parser.config,
							25.0, distOutputFile);

					ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
					for (int j = 6; j <= 10; j += 1) {
						Parser.config = new Config();
						Parser.parseInputABACFile(outputFile);
						Parser.parseInputDistFile(distOutputFile);
						logs = LogGenerator.generateLog(Parser.config,
								(double) j / 10, logs, 0.0);
						String logFile = "logs/synthetic/synthetic_" + args[1]
								+ "_" + j + "_" + i + "_new.log";
						outputLogsToFile(logs, Parser.config, logFile);
						HashSet<Triple<String, String, String>> logInducedUP = new HashSet<Triple<String, String, String>>();
						HashMap<Triple<String, String, String>, Double> logToFrequency = Parser.config
								.getLogToProbability();
						for (LogEntry log : logs) {
							Triple<String, String, String> userperm = new Triple<String, String, String>(
									log.user, log.op, log.resource);
							logInducedUP.add(userperm);
							// if (!logToFrequency.keySet().contains(userperm))
							// {
							// logToFrequency.put(userperm, 1);
							// } else {
							// logToFrequency.put(userperm,
							// logToFrequency.get(userperm) + 1);
							// }
						}
						for (int k = 0; k < NUMBER_RULE_METRICS; k++) {
							Parser.config = new Config();
							Parser.parseInputABACFile(outputFile);
							Parser.parseInputDistFile(distOutputFile);
							Parser.config.setLogInducedUP(logInducedUP);
							Parser.config
							.setLogToProbability(logToFrequency);
							// Parser.config.setLogToFrequency(logToFrequency);
							Parser.config.setLogs(logs);
							RuleQualityMetricType type = RuleQualityMetricType
									.values()[k];
							ABACMiner.W0_policy = weights[j - 6];
							ABACMiner.W0_rule = ((double) weights[j - 6]) / 10.0;
							ABACMiner.mineABACPolicy(Parser.config, false,
									k);
							
							System.out.println("RuleQualityMetricType: "
									+ type.toString());
							System.out.println("Fraction: " + (double) j / 10);
							ABACMiner.outputResult(Parser.config,
									ABACMiner.resultRules);
							double similarity = ABACMiner
									.symmetricSyntacticSimilarityOfPolicies(
											Parser.config.getRuleList(),
											ABACMiner.resultRules,
											Parser.config);

							int oldWSC = 0;
							int newWSC = 0;
							for (Rule r : Parser.config.getRuleList()) {
								oldWSC += r.getSize();
							}
							for (Rule r : ABACMiner.resultRules) {
								newWSC += r.getSize();
							}

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
							syntacticSimilarities[j - 6][k][i] = similarity;
							overAssignmentRatios[j - 6][k][i] = overAssignmentRatio;
							underAssignmentRatios[j - 6][k][i] = underAssignmentRatio;

							numUsers[j - 6][k][i] = Parser.config.getNumUsers();
							numResources[j - 6][k][i] = Parser.config
									.getResources().size();
							numOps[j - 6][k][i] = Parser.config.getOps().size();
							numCoveredUP[j - 6][k][i] = coveredUP.size();
							jaccardSimilarities[j - 6][k][i] = jaccardSimilarity(
									coveredUP, Parser.config.getCoveredUP());
							compressionRatio[j - 6][k][i] = ((double) newWSC)
									/ oldWSC;
						}
					}
				}
				String outputFile = "output/synthetic/synthetic_" + args[1]
						+ "_new.output";
				try {
					File file = new File(outputFile);
					// if file doesnt exists, then create it
					if (!file.exists()) {
						file.createNewFile();
					}

					FileWriter fw = new FileWriter(file.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);
					for (int i = 0; i < NUMBER_RULE_METRICS; i++) {
						bw.write((RuleQualityMetricType.values()[i])
								.toString() + "\n");
						for (int j = 0; j < NUMBER_OF_STEPS; j++) {
							Double fraction = j * 0.1 + 0.6;
							bw.write(fraction
									+ " "
									+ doubleArrayAverage(numUsers[j][i])
									+ " "
									+ doubleArraySTDV(numUsers[j][i])
									+ " "
									+ doubleArrayAverage(numResources[j][i])
									+ " "
									+ doubleArraySTDV(numResources[j][i])
									+ " "
									+ doubleArrayAverage(numOps[j][i])
									+ " "
									+ doubleArraySTDV(numOps[j][i])
									+ " "
									+ doubleArrayAverage(numCoveredUP[j][i])
									+ " "
									+ doubleArraySTDV(numCoveredUP[j][i])
									+ " "
									+ doubleArrayAverage(syntacticSimilarities[j][i])
									+ " "
									+ doubleArraySTDV(syntacticSimilarities[j][i])
									+ " "
									+ doubleArrayAverage(overAssignmentRatios[j][i])
									+ " "
									+ doubleArraySTDV(overAssignmentRatios[j][i])
									+ " "
									+ doubleArrayAverage(underAssignmentRatios[j][i])
									+ " "
									+ doubleArraySTDV(underAssignmentRatios[j][i])
									+ " "
									+ doubleArrayAverage(jaccardSimilarities[j][i])
									+ " "
									+ doubleArraySTDV(jaccardSimilarities[j][i])
									+ " "
									+ doubleArrayAverage(compressionRatio[j][i])
									+ " "
									+ doubleArraySTDV(compressionRatio[j][i])
									+ "\n");
						}
					}
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (args[0].charAt(1) == 'r') {
				// Generate case studies with synthetic attribtue data
				if (args.length < 3) {
					System.err
							.println("Two arguments are required for -r mode. 1. case-study type(university, healthcare, projectmanagement) 2.size N");
					System.exit(1);
				}
				// int ruleMetric = 0;
				// if (args.length >= 4) {
				// ruleMetric = Integer.parseInt(args[3]);
				// }
				double[][][] numUsers = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numResources = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numOps = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numCoveredUP = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				double[][][] syntacticSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] overAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] underAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] jaccardSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				for (int i = 0; i < NUMBER_OF_TEST_CASES; i++) {
					Parser.config = new Config();
					CaseStudyGenerator c = CaseStudyGenerator
							.createGenerator(args[1]);
					int N = Integer.parseInt(args[2]);

					String outputFile = "case-studies-synthetic/" + args[1]
							+ "_" + N + "_" + i + "_newest.abac";

					int Nir = 0;

					int NdomainSize = 0;

					c.generateCaseStudy(outputFile, N, Nir, NdomainSize);
					Parser.parseInputABACFile(outputFile);
					String distOutputFile = "case-studies-synthetic/" + args[1]
							+ "_" + args[2] + "_" + i + "_" + 25 + "_newest.dist";
					SyntheticDistGenerator.distributionGenerator(Parser.config,
							25.0, distOutputFile);

					ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
					for (int j = 6; j <= 10; j += 1) {
						Parser.config = new Config();
						Parser.parseInputABACFile(outputFile);
						Parser.parseInputDistFile(distOutputFile);
						logs = LogGenerator.generateLog(Parser.config,
								((double) j) / 10, logs, 0.0);
						String logFile = "logs/case-studies-synthetic/"
								+ args[1] + "_" + N + "_" + j + "_" + i
								+ "_new.log";
						outputLogsToFile(logs, Parser.config, logFile);
						HashSet<Triple<String, String, String>> logInducedUP = new HashSet<Triple<String, String, String>>();
						HashMap<Triple<String, String, String>, Double> logToFrequency = Parser.config
								.getLogToProbability();
						for (LogEntry log : logs) {
							Triple<String, String, String> userperm = new Triple<String, String, String>(
									log.user, log.op, log.resource);
							logInducedUP.add(userperm);
							// if
							// (!logToFrequency.keySet().contains(userperm))
							// {
							// logToFrequency.put(userperm, 1);
							// } else {
							// logToFrequency.put(userperm,
							// logToFrequency.get(userperm) + 1);
							// }
						}
						for (int k = 0; k < NUMBER_RULE_METRICS; k++) {
							Parser.config = new Config();
							Parser.parseInputABACFile(outputFile);
							Parser.parseInputDistFile(distOutputFile);
							Parser.config.setLogInducedUP(logInducedUP);
							Parser.config.setLogToProbability(logToFrequency);
							Parser.config.setLogs(logs);
							RuleQualityMetricType type = RuleQualityMetricType
									.values()[k];
							System.out.println("RuleQualityMetricType: "
									+ type.toString());
							System.out.println("Fraction: " + (double) j / 10);
							ABACMiner.W0_policy = weights[j - 6];
							ABACMiner.W0_rule = ((double) weights[j - 6]) / 10.0;
							ABACMiner.mineABACPolicy(Parser.config, false, k);
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
							syntacticSimilarities[j - 6][k][i] = similarity;
							overAssignmentRatios[j - 6][k][i] = overAssignmentRatio;
							underAssignmentRatios[j - 6][k][i] = underAssignmentRatio;

							numUsers[j - 6][k][i] = Parser.config.getNumUsers();
							numResources[j - 6][k][i] = Parser.config
									.getResources().size();
							numOps[j - 6][k][i] = Parser.config.getOps().size();
							numCoveredUP[j - 6][k][i] = coveredUP.size();
							jaccardSimilarities[j - 6][k][i] = jaccardSimilarity(
									coveredUP, Parser.config.getCoveredUP());
						}
					}
				}
				String outputFile = "output/case-studies-synthetic/" + args[1]
						+ "_" + args[2] + "_newest.output";
				try {
					File file = new File(outputFile);
					// if file doesn't exists, then create it
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
							bw.write(fraction
									+ " "
									+ doubleArrayAverage(numUsers[j][i])
									+ " "
									+ doubleArraySTDV(numUsers[j][i])
									+ " "
									+ doubleArrayAverage(numResources[j][i])
									+ " "
									+ doubleArraySTDV(numResources[j][i])
									+ " "
									+ doubleArrayAverage(numOps[j][i])
									+ " "
									+ doubleArraySTDV(numOps[j][i])
									+ " "
									+ doubleArrayAverage(numCoveredUP[j][i])
									+ " "
									+ doubleArraySTDV(numCoveredUP[j][i])
									+ " "
									+ doubleArrayAverage(syntacticSimilarities[j][i])
									+ " "
									+ doubleArraySTDV(syntacticSimilarities[j][i])
									+ " "
									+ doubleArrayAverage(overAssignmentRatios[j][i])
									+ " "
									+ doubleArraySTDV(overAssignmentRatios[j][i])
									+ " "
									+ doubleArrayAverage(underAssignmentRatios[j][i])
									+ " "
									+ doubleArraySTDV(underAssignmentRatios[j][i])
									+ " "
									+ doubleArrayAverage(jaccardSimilarities[j][i])
									+ " "
									+ doubleArraySTDV(jaccardSimilarities[j][i])
									+ "\n");
						}
					}
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			// To generate overassignments_analysis reports for analysis. Takes ABAC file, DIST file and log file path/file_prefix name as input 
			if (args[0].charAt(1) == 'o') {
				if (args.length < 2) {
					System.err
							.println("Two more argument needed for -o mode. 1. abac file (String) 2. dist file (String)");
					System.exit(1);
				}
				
				double[][][] numUsers = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numResources = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numOps = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numCoveredUP = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				double[][][] syntacticSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] overAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] underAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] jaccardSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				
				PrintStream out = null;
				try {
					//out = new PrintStream(new FileOutputStream("output.txt"));
					//System.setOut(out);
				
					final  Comparator<Rule> overAssignmentSize = new Comparator<Rule>() {
						public int compare(Rule r1, Rule r2) {
							HashSet<Triple<String, String, String>> overAssignment1 = new HashSet<Triple<String, String, String>>(
								r1.getCoveredUPTriple());
							HashSet<Triple<String, String, String>> overAssignment2 = new HashSet<Triple<String, String, String>>(
								r2.getCoveredUPTriple());
					
							overAssignment1.removeAll(Parser.config.getCoveredUP());
							overAssignment2.removeAll(Parser.config.getCoveredUP());
					
							if (overAssignment1.size() > overAssignment2.size())
								return -1;
							else if (overAssignment1.size() < overAssignment2.size())
								return 1;
							else
								return 0;
						}
					};
					
					for (int i = 0; i < NUMBER_OF_OVERASSIGNMENT_TEST_CASES; i++) {
						ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
						for (int j = 6; j <= 10; j += 1) {
							Parser.config = new Config();
							Parser.parseInputABACFile(args[1] );
							Parser.parseInputDistFile(args[2] );
			
							String logFile = args[3] + "_" + j + "_" + i + ".log";
									
							HashSet<Triple<String, String, String>> logInducedUP = new HashSet<Triple<String, String, String>>();	
							HashMap<Triple<String, String, String>, Double> logToFrequency = new HashMap<Triple<String, String, String>, Double>();
							
							BufferedReader br;
							try {
								br = new BufferedReader(new FileReader(logFile));
								
								String line = br.readLine();
								while (line != null ) {
									final Pattern pattern = Pattern.compile("[\\<,,,>]");
									final String[] result = pattern.split(line);
									
									if (result.length != 1 ) {
										Triple<String, String, String> userperm = new Triple<String, String, String>(
											result[2], result[4], result[3]);
										if (result[5].trim().compareTo("null") != 0){
											logToFrequency.put(userperm,Double.valueOf( result[5]));
										}
										logInducedUP.add(userperm);
										LogEntry l = new LogEntry(result[2], result[3], result[4], Integer.parseInt(result[1]));
										logs.add(l);
									}
									
						            line = br.readLine();
						        }
								 br.close();
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							
							
							try{
							
								FileOutputStream fstream = new FileOutputStream("overassignments_analysis/reports/" + args[1].substring(0, args[1].length() - 5) + "_" + j + "_" + i + ".txt");
								fstream.getChannel().truncate(0);
								fstream.close();
								
								FileWriter wstream = new FileWriter("overassignments_analysis/reports/" + args[1].substring(0, args[1].length() - 5) + "_" +j + "_" + i + ".txt");
								BufferedWriter output = new BufferedWriter(wstream);
								
								Vector<HashSet<Triple<String,String, String>>> overAssign = new Vector<HashSet<Triple<String,String, String>>> (NUMBER_RULE_METRICS);
								Vector<HashSet<Triple<String,String, String>>> underAssign = new Vector<HashSet<Triple<String,String, String>>>(NUMBER_RULE_METRICS);
								
								
								for (int k = 0; k < NUMBER_RULE_METRICS; k++) {
									Parser.config = new Config();
									Parser.parseInputABACFile(args[1]);
									Parser.parseInputDistFile(args[2]);
									Parser.config.setLogInducedUP(logInducedUP);
									Parser.config.setLogToProbability(logToFrequency);
									Parser.config.setLogs(logs);
									RuleQualityMetricType type = RuleQualityMetricType
											.values()[k];
									System.out.println("RuleQualityMetricType: "
											+ type.toString());
									System.out.println("Fraction: " + (double) j / 10);
									ABACMiner.W0_policy = ((double) weights[j - 6]);
									ABACMiner.W0_rule = ((double) weights[j - 6]) / 10.0;
									System.out.println(Parser.config.getLogInducedUP().size());
									ABACMiner.mineABACPolicy(Parser.config, false, k);
									double similarity = ABACMiner
											.symmetricSyntacticSimilarityOfPolicies(
													Parser.config.getRuleList(),
													ABACMiner.resultRules,
													Parser.config);
									/////////////
									int rule_no = 1;
									output.write("OverAssignments\n");
									output.write("---------------------\n");
				
									output.write("Log Completeness: " + (float)j/10.0 + "\nRule Quality Metric: " + type.toString() + "\n");
				
									Collections.sort(ABACMiner.resultRules, overAssignmentSize);
				
									for (Rule r : ABACMiner.resultRules) {
										HashSet<Triple<String, String, String>> overAssignment = new HashSet<Triple<String, String, String>>(
										r.getCoveredUPTriple());
					
										overAssignment.removeAll(Parser.config.getCoveredUP());	
									
										output.write(rule_no + ".\n");
										output.write(String.valueOf(r) + "\n");
										output.write(String.valueOf(overAssignment.size())+ "\n");
										
										for ( Triple<String, String, String>  temp : overAssignment) {
											System.out.println("<" + temp.getFirst() + "," + temp.getThird() + "," + temp.getSecond() + ">");
											output.write("<" + temp.getFirst() + "," + temp.getThird() + "," + temp.getSecond() + ">" + "\n");
										}
										output.write("-----------");
										output.newLine();
										rule_no++;
									}			
									output.write("------------------------------------------------------------\n");	
									/////////////
	
									HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
									for (Rule r : ABACMiner.resultRules) {
										coveredUP.addAll(r.getCoveredUPTriple());
									}
									HashSet<Triple<String, String, String>> overAssignment = new HashSet<Triple<String, String, String>>(
											coveredUP);
									overAssignment.removeAll(Parser.config
											.getCoveredUP());
									overAssign.add(k, overAssignment);
									
									HashSet<Triple<String, String, String>> underAssignment = new HashSet<Triple<String, String, String>>(
											Parser.config.getCoveredUP());
									underAssignment.removeAll(coveredUP);
									underAssign.add(k, underAssignment);
									
									output.write("\n Under Assignments");
									output.write("\n---------------------\n");
									
									for ( Triple<String, String, String>  temp :underAssignment) {
										output.write("<" + temp.getFirst() + "," + temp.getThird() + "," + temp.getSecond() + ">" + "\n");
									}
									output.newLine();
									
									double overAssignmentRatio = (double) overAssignment
											.size()
											/ Parser.config.getCoveredUP().size();
									double underAssignmentRatio = (double) underAssignment
											.size()
											/ Parser.config.getCoveredUP().size();
									syntacticSimilarities[j - 6][k][i] = similarity;
									overAssignmentRatios[j - 6][k][i] = overAssignmentRatio;
									underAssignmentRatios[j - 6][k][i] = underAssignmentRatio;

									numUsers[j - 6][k][i] = Parser.config
											.getNumUsers();
									numResources[j - 6][k][i] = Parser.config
											.getResources().size();
									numOps[j - 6][k][i] = Parser.config.getOps()
											.size();
									numCoveredUP[j - 6][k][i] = coveredUP.size();
									jaccardSimilarities[j - 6][k][i] = jaccardSimilarity(
											coveredUP, Parser.config.getCoveredUP());
									
								
								}
								output.newLine();
								output.write("OverAssignments Summary Report");
								output.newLine();
								output.write("-------------------------\n");
								
								for ( int k=0; k < NUMBER_RULE_METRICS; k++) {
									RuleQualityMetricType type = RuleQualityMetricType
											.values()[k];
									output.write(type.toString() + ": " + String.valueOf(overAssign.elementAt(k).size()));
									output.newLine();
								}
								
								output.newLine();
								output.write("UnderAssignments Summary Report");
								output.newLine();
								output.write("-------------------------\n");
								
								for ( int k=0; k < NUMBER_RULE_METRICS; k++) {
									RuleQualityMetricType type = RuleQualityMetricType
											.values()[k];
									output.write(type.toString() + ": " + String.valueOf(underAssign.elementAt(k).size()));
									output.newLine();
								}		
								output.close();
								wstream.close();
							} catch (Exception e) {
								System.out.println(e.getMessage());
							}
						}
					}
					
					String outputFile = "output/"
							+ args[1].substring(0, args[1].length() - 5) 
							+ ".output";
					try {
						File file = new File(outputFile);
						// if file doesnt exists, then create it
						if (!file.exists()) {
							file.createNewFile();
						}

						FileWriter fw = new FileWriter(file.getAbsoluteFile());
						BufferedWriter bw = new BufferedWriter(fw);
						for (int i = 0; i < NUMBER_RULE_METRICS; i++) {
							bw.write((RuleQualityMetricType.values()[i])
									.toString() + "\n");
							for (int j = 0; j < NUMBER_OF_STEPS; j++) {
//								double averageJaccardSimilarity = doubleArrayAverage(jaccardSimilarities[j][i]);
//								if (averageJaccardSimilarity > bestJaccardSimilarity[j][i]) {
//									bestJaccardSimilarity[j][i] = averageJaccardSimilarity;
//									bestWo[j][i] = wo;
//								}
								Double fraction = j * 0.1 + 0.6;
								bw.write(fraction.toString()
										+ " "
										+ doubleArrayAverage(numUsers[j][i])
										+ " "
										+ doubleArraySTDV(numUsers[j][i])
										+ " "
										+ doubleArrayAverage(numResources[j][i])
										+ " "
										+ doubleArraySTDV(numResources[j][i])
										+ " "
										+ doubleArrayAverage(numOps[j][i])
										+ " "
										+ doubleArraySTDV(numOps[j][i])
										+ " "
										+ doubleArrayAverage(numCoveredUP[j][i])
										+ " "
										+ doubleArraySTDV(numCoveredUP[j][i])
										+ " "
										+ doubleArrayAverage(syntacticSimilarities[j][i])
										+ " "
										+ doubleArraySTDV(syntacticSimilarities[j][i])
										+ " "
										+ doubleArrayAverage(overAssignmentRatios[j][i])
										+ " "
										+ doubleArraySTDV(overAssignmentRatios[j][i])
										+ " "
										+ doubleArrayAverage(underAssignmentRatios[j][i])
										+ " "
										+ doubleArraySTDV(underAssignmentRatios[j][i])
										+ " "
										+ doubleArrayAverage(jaccardSimilarities[j][i])
										+ " "
										+ doubleArraySTDV(jaccardSimilarities[j][i])
										+ "\n");
							}
						}
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}catch ( Exception e) {
					System.out.println(e.getMessage());
				}
			}
			
			// to generate distribution file from the ABAC file
			if (args[0].charAt(1) == 'd') {
				Parser.config = new Config();
				Parser.parseInputABACFile(args[1]);
				
				String distOutputFile = args[1].substring(0, args[1].length() - 5) + ".dist";
				SyntheticDistGenerator.distributionGenerator(Parser.config,
						25.0, distOutputFile);
			}
			
			// read case-studies abac file and dist files, output logs and
			// experiment result
			if (args[0].charAt(1) == 'm') {
				if (args.length < 3) {
					System.err
							.println("Two more argument needed for -w mode. 1. abac file (String) 2. dist file (String)");
					System.exit(1);
				}

				int[][] bestWo = new int[NUMBER_OF_STEPS][NUMBER_RULE_METRICS];
				double[][] bestJaccardSimilarity = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS];

				double[][][] numUsers = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numResources = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numOps = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numCoveredUP = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				double[][][] syntacticSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] overAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] underAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] jaccardSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				for (int wo = 1; wo <= 50; wo++) {
					System.out.println("WO: " + wo);
					int ruleMetric = 0;
					if (args.length >= 4) {
						ruleMetric = Integer.parseInt(args[3]);
					}
					for (int i = 0; i < NUMBER_OF_TEST_CASES; i++) {
						ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
						for (int j = 6; j <= 10; j += 1) {
							Parser.config = new Config();
							Parser.parseInputABACFile(args[1]);
							Parser.parseInputDistFile(args[2]);
							logs = LogGenerator.generateLog(Parser.config,
									((double) j) / 10 > 1.0 ? 1.0
											: ((double) j) / 10, logs, 0.0);
							String logFile = "logs/"
									+ args[1]
											.substring(0, args[1].length() - 5)
									+ "_" + j + "_" + i + "_new_test" + ".log";
							outputLogsToFile(logs, Parser.config, logFile);
							HashSet<Triple<String, String, String>> logInducedUP = new HashSet<Triple<String, String, String>>();
							HashMap<Triple<String, String, String>, Double> logToFrequency = Parser.config
									.getLogToProbability();
							for (LogEntry log : logs) {
								Triple<String, String, String> userperm = new Triple<String, String, String>(
										log.user, log.op, log.resource);
								logInducedUP.add(userperm);
							}
							for (int k = 0; k < NUMBER_RULE_METRICS; k++) {
								Parser.config = new Config();
								Parser.parseInputABACFile(args[1]);
								Parser.parseInputDistFile(args[2]);
								Parser.config.setLogInducedUP(logInducedUP);
								Parser.config
										.setLogToProbability(logToFrequency);
								Parser.config.setLogs(logs);
								RuleQualityMetricType type = RuleQualityMetricType
										.values()[k];
								System.out.println("RuleQualityMetricType: "
										+ type.toString());
								System.out.println("Fraction: " + (double) j
										/ 10);
//								 ABACMiner.W0_policy = ((double) weights[j -
//								 6]);
//								 ABACMiner.W0_rule = ((double) weights[j - 6])
//								 / 10.0;

								ABACMiner.W0_policy = ((double) wo);
								ABACMiner.W0_rule = ((double) wo) / 10.0;
								ABACMiner.mineABACPolicy(Parser.config, false,
										k);
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
								syntacticSimilarities[j - 6][k][i] = similarity;
								overAssignmentRatios[j - 6][k][i] = overAssignmentRatio;
								underAssignmentRatios[j - 6][k][i] = underAssignmentRatio;

								numUsers[j - 6][k][i] = Parser.config
										.getNumUsers();
								numResources[j - 6][k][i] = Parser.config
										.getResources().size();
								numOps[j - 6][k][i] = Parser.config.getOps()
										.size();
								numCoveredUP[j - 6][k][i] = coveredUP.size();
								jaccardSimilarities[j - 6][k][i] = jaccardSimilarity(
										coveredUP, Parser.config.getCoveredUP());
							}
						}
					}
					String outputFile = "output/"
							+ args[1].substring(0, args[1].length() - 5)
							+ "_new.output";
					try {
						File file = new File(outputFile);
						// if file doesnt exists, then create it
						if (!file.exists()) {
							file.createNewFile();
						}

						FileWriter fw = new FileWriter(file.getAbsoluteFile());
						BufferedWriter bw = new BufferedWriter(fw);
						for (int i = 0; i < NUMBER_RULE_METRICS; i++) {
							bw.write((RuleQualityMetricType.values()[i])
									.toString() + "\n");
							for (int j = 0; j < NUMBER_OF_STEPS; j++) {
								double averageJaccardSimilarity = doubleArrayAverage(jaccardSimilarities[j][i]);
								if (averageJaccardSimilarity > bestJaccardSimilarity[j][i]) {
									bestJaccardSimilarity[j][i] = averageJaccardSimilarity;
									bestWo[j][i] = wo;
								}
								Double fraction = j * 0.1 + 0.6;
								bw.write(fraction.toString()
										+ " "
										+ doubleArrayAverage(numUsers[j][i])
										+ " "
										+ doubleArraySTDV(numUsers[j][i])
										+ " "
										+ doubleArrayAverage(numResources[j][i])
										+ " "
										+ doubleArraySTDV(numResources[j][i])
										+ " "
										+ doubleArrayAverage(numOps[j][i])
										+ " "
										+ doubleArraySTDV(numOps[j][i])
										+ " "
										+ doubleArrayAverage(numCoveredUP[j][i])
										+ " "
										+ doubleArraySTDV(numCoveredUP[j][i])
										+ " "
										+ doubleArrayAverage(syntacticSimilarities[j][i])
										+ " "
										+ doubleArraySTDV(syntacticSimilarities[j][i])
										+ " "
										+ doubleArrayAverage(overAssignmentRatios[j][i])
										+ " "
										+ doubleArraySTDV(overAssignmentRatios[j][i])
										+ " "
										+ doubleArrayAverage(underAssignmentRatios[j][i])
										+ " "
										+ doubleArraySTDV(underAssignmentRatios[j][i])
										+ " "
										+ doubleArrayAverage(jaccardSimilarities[j][i])
										+ " "
										+ doubleArraySTDV(jaccardSimilarities[j][i])
										+ "\n");
							}
						}
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				

				for (int i = 0; i < NUMBER_OF_STEPS; i++) {
					System.out.println("Log Completeness: " + (i * 0.1 + 0.6));
					for (int j = 0; j < NUMBER_RULE_METRICS; j++) {
						System.out.print(bestWo[i][j] + " ");
					}
					System.out.println();
					for (int j = 0; j < NUMBER_RULE_METRICS; j++) {
						System.out.print(bestJaccardSimilarity[i][j] + " ");
					}
					System.out.println();
				}
				}
			}
			if (args[0].charAt(1) == 't') {
				if (args.length < 3) {
					System.err
							.println("Two more argument needed for -w mode. 1. abac file (String) 2. dist file (String)");
					System.exit(1);
				}

				NUMBER_OF_STEPS = 40;
				NUMBER_RULE_METRICS = 1;
				int fraction = 9;

				double[][][] numUsers = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numResources = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numOps = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numCoveredUP = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				double[][][] syntacticSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] overAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] underAssignmentRatios = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] jaccardSimilarities = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				double[][][] numMerges = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];
				double[][][] numUnMerges = new double[NUMBER_OF_STEPS][NUMBER_RULE_METRICS][NUMBER_OF_TEST_CASES];

				for (int i = 0; i < NUMBER_OF_TEST_CASES; i++) {
					ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
					for (int j = fraction; j <= fraction; j += 1) {
						for (int weight = 1; weight <= NUMBER_OF_STEPS; weight++) {
							System.out.println(weight + "++++++++++++++++++++");
							Parser.config = new Config();
							Parser.parseInputABACFile(args[1]);
							Parser.parseInputDistFile(args[2]);
							logs = LogGenerator.generateLog(Parser.config,
									((double) j) / 10 > 1.0 ? 1.0
											: ((double) j) / 10, logs, 0.0);
							String logFile = "logs/"
									+ args[1]
											.substring(0, args[1].length() - 5)
									+ "_" + j + "_" + i + "_" + weight
									+ "_test" + ".log";
							outputLogsToFile(logs, Parser.config, logFile);
							HashSet<Triple<String, String, String>> logInducedUP = new HashSet<Triple<String, String, String>>();
							HashMap<Triple<String, String, String>, Double> logToFrequency = Parser.config
									.getLogToProbability();
							for (LogEntry log : logs) {
								Triple<String, String, String> userperm = new Triple<String, String, String>(
										log.user, log.op, log.resource);
								logInducedUP.add(userperm);
								// if
								// (!logToFrequency.keySet().contains(userperm))
								// {
								// logToFrequency.put(userperm, 1);
								// } else {
								// logToFrequency.put(userperm,
								// logToFrequency.get(userperm) + 1);
								// }
							}
							ABACMiner.W0_policy = (double) weight;
							ABACMiner.W0_rule = ((double) weight) / 10;
							for (int k = 0; k < NUMBER_RULE_METRICS; k++) {
								Parser.config = new Config();
								Parser.parseInputABACFile(args[1]);
								Parser.parseInputDistFile(args[2]);
								Parser.config.setLogInducedUP(logInducedUP);
								// Parser.config.setLogToFrequency(logToFrequency);
								Parser.config.setLogs(logs);
								System.out.println("LogInducedUP Size: "
										+ logInducedUP.size());
								System.out.println("log Size: " + logs.size());
								RuleQualityMetricType type = RuleQualityMetricType
										.values()[k];
								System.out.println("RuleQualityMetricType: "
										+ type.toString());
								System.out.println("Fraction: " + (double) j
										/ 10);
								ABACMiner.mineABACPolicy(Parser.config, false,
										i);

								double similarity = ABACMiner
										.symmetricSyntacticSimilarityOfPolicies(
												Parser.config.getRuleList(),
												ABACMiner.resultRules,
												Parser.config);
								HashSet<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
								for (Rule r : ABACMiner.resultRules) {
									coveredUP.addAll(ABACMiner
											.computeCoveredUPTriple(r,
													Parser.config));
								}
								HashSet<Triple<String, String, String>> overAssignment = new HashSet<Triple<String, String, String>>(
										coveredUP);
								overAssignment.removeAll(Parser.config
										.getCoveredUP());
								HashSet<Triple<String, String, String>> underAssignment = new HashSet<Triple<String, String, String>>(
										Parser.config.getCoveredUP());
								System.out.println("CoveredUP size:"
										+ coveredUP.size());
								underAssignment.removeAll(coveredUP);
								double overAssignmentRatio = (double) overAssignment
										.size()
										/ Parser.config.getCoveredUP().size();
								double underAssignmentRatio = (double) underAssignment
										.size()
										/ Parser.config.getCoveredUP().size();
								syntacticSimilarities[weight - 1][k][i] = similarity;
								overAssignmentRatios[weight - 1][k][i] = overAssignmentRatio;
								underAssignmentRatios[weight - 1][k][i] = underAssignmentRatio;

								numUsers[weight - 1][k][i] = Parser.config
										.getNumUsers();
								numResources[weight - 1][k][i] = Parser.config
										.getResources().size();
								numOps[weight - 1][k][i] = Parser.config
										.getOps().size();
								numCoveredUP[weight - 1][k][i] = coveredUP
										.size();
								jaccardSimilarities[weight - 1][k][i] = jaccardSimilarity(
										coveredUP, Parser.config.getCoveredUP());

								numMerges[weight - 1][k][i] = ABACMiner.numMerges;
								numUnMerges[weight - 1][k][i] = ABACMiner.numUnsuccessfulMerges;

								ABACMiner.numMerges = 0;
								ABACMiner.numUnsuccessfulMerges = 0;
								ABACMiner.resultRules.clear();
							}
						}
					}
				}
				String outputFile = "output/"
						+ args[1].substring(0, args[1].length() - 5) + "_test"
						+ "_" + fraction + ".output";
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
							// Double fraction = 0.6;
							bw.write(j
									+ 1
									+ " "
									// + doubleArrayAverage(numUsers[j][i])
									// + " "
									// + doubleArraySTDV(numUsers[j][i])
									// + " "
									// + doubleArrayAverage(numResources[j][i])
									// + " "
									// + doubleArraySTDV(numResources[j][i])
									// + " "
									// + doubleArrayAverage(numOps[j][i])
									// + " "
									// + doubleArraySTDV(numOps[j][i])
									// + " "
									// + doubleArrayAverage(numCoveredUP[j][i])
									// + " "
									// + doubleArraySTDV(numCoveredUP[j][i])
									// + " "
									// +
									// doubleArrayAverage(syntacticSimilarities[j][i])
									// + " "
									// +
									// doubleArraySTDV(syntacticSimilarities[j][i])
									// + " "
									// +
									// doubleArrayAverage(overAssignmentRatios[j][i])
									// + " "
									// +
									// doubleArraySTDV(overAssignmentRatios[j][i])
									// + " "
									// +
									// doubleArrayAverage(underAssignmentRatios[j][i])
									// + " "
									// +
									// doubleArraySTDV(underAssignmentRatios[j][i])
									// + " "
									+ doubleArrayAverage(jaccardSimilarities[j][i])
									+ " "
									+ doubleArraySTDV(jaccardSimilarities[j][i])
									// + " "
									// + doubleArrayAverage(numMerges[j][i])
									// + " "
									// + doubleArrayAverage(numUnMerges[j][i])
									+ "\n");
						}
					}
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
