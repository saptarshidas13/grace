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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import edu.dar.util.Config;
import edu.dar.util.LogEntry;
import edu.dar.util.Parser;
import edu.dar.util.Rule;
import edu.dar.util.Triple;

public class LogGenerator {
	public static ArrayList<LogEntry> generateLog(Config config,
			double fraction, ArrayList<LogEntry> existingLogs,
			double noiseFraction) {
		HashMap<String, Rule> nameRuleMap = new HashMap<String, Rule>();
		HashMap<String, HashMap<String, HashSet<String>>> ruleResUsers = new HashMap<String, HashMap<String, HashSet<String>>>();

		Random rand = new Random(System.currentTimeMillis());
		for (Rule r : config.getRuleList()) {
			nameRuleMap.put(r.getName(), r);
			ruleResUsers.put(r.getName(),
					new HashMap<String, HashSet<String>>());
			HashSet<String> satRes = new HashSet<String>();
			for (String res : config.getResources()) {
				if (Parser.satisfyingRule(res, r, config, false)) {
					satRes.add(res);
				}
			}
			r.setSatResources(satRes);
			HashMap<String, HashSet<String>> resUser = new HashMap<String, HashSet<String>>();
			for (String res : satRes) {
				HashSet<String> users = new HashSet<String>();
				for (String user : config.getUsers()) {
					if (Parser.satisfyingRule(user, r, config, true)
							&& Parser.satisfyingRuleConstraints(user, res, r,
									config)) {
						users.add(user);
					}
				}
				resUser.put(res, users);
			}
			ruleResUsers.put(r.getName(), resUser);
		}

		HashMap<Triple<String, String, String>, Double> upTripleToFreq = new HashMap<Triple<String, String, String>, Double>();
		for (Rule r : config.getRuleList()) {
			double ruleProb = config.getRuleFreqMap().get(r.getName());
			for (String res : ruleResUsers.get(r.getName()).keySet()) {
				double resProb = selectRestrictedProb(config.resFreqMap,
						r.getSatResources(), res);
				for (String user : ruleResUsers.get(r.getName()).get(res)) {
					double userProb = selectRestrictedProb(config.userFreqMap,
							ruleResUsers.get(r.getName()).get(res), user);
					for (String op : r.getOps()) {
						double opProb = selectRestrictedProb(config.opFreqMap,
								r.getOps(), op);
						double prob = ruleProb * resProb * userProb * opProb;
						Triple<String, String, String> upTriple = new Triple<String, String, String>(
								user, op, res);
						if (upTripleToFreq.containsKey(upTriple)) {
							prob += upTripleToFreq.get(upTriple);
							upTripleToFreq.put(upTriple, prob);
						} else {
							upTripleToFreq.put(upTriple, prob);
						}
					}
				}
			}
		}

		config.setLogToProbability(new HashMap<Triple<String, String, String>, Double>(
				upTripleToFreq));

		HashSet<Triple<String, String, String>> currentLogEntries = new HashSet<Triple<String, String, String>>();

		for (LogEntry e : existingLogs) {
			Triple<String, String, String> triple = new Triple<String, String, String>(
					e.user, e.op, e.resource);
			currentLogEntries.add(triple);
		}

		upTripleToFreq.keySet().removeAll(currentLogEntries);

		ArrayList<LogEntry> logEntries = new ArrayList<LogEntry>(existingLogs);
		HashSet<Triple<String, String, String>> generatedUP = new HashSet<Triple<String, String, String>>();

		int i = currentLogEntries.size();
		while (true) {
			if (((double) logEntries.size()) / config.getCoveredUP().size() >= fraction) {
				break;
			}
			if (rand.nextDouble() < noiseFraction) {
				while (true) {
					String user = ABACMiner.randomElement(config.getUsers());
					String resource = ABACMiner.randomElement(config
							.getResources());
					String op = ABACMiner.randomElement(config.getOps());
					Triple<String, String, String> upTriple = new Triple<String, String, String>(
							user, op, resource);
					if (!config.getCoveredUP().contains(upTriple)) {
						LogEntry log = new LogEntry(upTriple.getFirst(),
								upTriple.getThird(), upTriple.getSecond(), i++);
						logEntries.add(log);
						generatedUP.add(upTriple);
						Double probability = (double)1 / (config.getUsers().size() * config.getResources().size() * config.getOps().size());
						config.getLogToProbability().put(upTriple, probability);
						break;
					}
				}

			} else {
				Triple<String, String, String> upTriple = select(
						upTripleToFreq, rand);
				LogEntry log = new LogEntry(upTriple.getFirst(),
						upTriple.getThird(), upTriple.getSecond(), i++);
				logEntries.add(log);
				generatedUP.add(upTriple);
				upTripleToFreq.remove(upTriple);
			}
		}

		normalizeProbability(config, logEntries);

		return logEntries;
	}

	public static void normalizeProbability(Config config,
			ArrayList<LogEntry> logEntries) {
		HashMap<Triple<String, String, String>, Double> upTripleToFreq = new HashMap<Triple<String, String, String>, Double>();
		double totalProbability = 0.0;
		for (LogEntry e : logEntries) {
			Triple<String, String, String> triple = new Triple<String, String, String>(
					e.user, e.op, e.resource);
			if (config.getLogToProbability().containsKey(triple)) {
				totalProbability += config.getLogToProbability().get(triple);
			} else {
				Double probability = (double)1 / (config.getUsers().size() * config.getResources().size() * config.getOps().size());
				totalProbability += probability;
				config.getLogToProbability().put(triple, probability);
			}
		}
		for (LogEntry e : logEntries) {
			Triple<String, String, String> triple = new Triple<String, String, String>(
					e.user, e.op, e.resource);
				upTripleToFreq.put(triple, config.getLogToProbability().get(triple) / totalProbability);
		}
		
		totalProbability = 0.0;
		for (Triple<String, String, String> triple : upTripleToFreq.keySet()) {
			totalProbability += upTripleToFreq.get(triple);
		}
		
		//System.out.println("Total Probability: " + totalProbability);
		
		config.setLogToProbability(upTripleToFreq);
	}

	public static double selectRestrictedProb(
			HashMap<String, Double> eleFreqMap, HashSet<String> elements,
			String element) {
		if (elements.isEmpty()) {
			return 0;
		}
		double sum = 0.0;
		for (String e : elements) {
			sum += eleFreqMap.get(e);
		}
		return eleFreqMap.get(element) / sum;
	}

	public static <T> T selectRestricted(HashMap<T, Double> eleFreqMap,
			HashSet<T> elements, Random rand) {
		if (elements.isEmpty()) {
			return null;
		}
		double sum = 0.0;
		for (T e : elements) {
			sum += eleFreqMap.get(e);
		}

		double p = rand.nextDouble() * sum;
		int i = 0;
		sum = 0.0;
		for (T element : elements) {
			double dist = eleFreqMap.get(element);
			sum += dist;
			if (Double.compare(p, sum) < 0 || i == elements.size() - 1) {
				return element;
			}
			i++;
		}
		return null;
	}

	public static <T> T select(HashMap<T, Double> eleFreqMap, Random rand) {

		double p = rand.nextDouble();
		int i = 0;
		double sum = 0.0;
		for (T element : eleFreqMap.keySet()) {
			double dist = eleFreqMap.get(element);
			sum += dist;
			if (Double.compare(p, sum) < 0
					|| i == eleFreqMap.keySet().size() - 1) {
				return element;
			}
			i++;
		}
		return null;
	}

	public static void main(String[] args) {
		Parser.parseInputABACFile(args[0]);
		Parser.parseInputDistFile(args[1]);
		LogGenerator.generateLog(Parser.config, 1.0, new ArrayList<LogEntry>(),
				0.0);
	}

}
