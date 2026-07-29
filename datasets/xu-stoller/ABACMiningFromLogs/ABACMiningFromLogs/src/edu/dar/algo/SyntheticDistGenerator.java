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
import java.util.HashMap;

import edu.dar.util.Config;

public class SyntheticDistGenerator {
	public static double userRatio = 3.0;
	public static void distributionGenerator(Config config, double ratio, String outputFile) {
		HashMap<String, Double> ruleToFreq = new HashMap<String, Double>();
		HashMap<String, Double> userToFreq = new HashMap<String, Double>();
		HashMap<String, Double> resourceToFreq = new HashMap<String, Double>();
		HashMap<String, Double> opToFreq = new HashMap<String, Double>();
		
		int NRules = config.getRuleList().size();
		int NUsers = config.getUsers().size();
		int NResources = config.getResources().size();
		int NOps = config.getOps().size();
		
		double a_rule = computeCoefficientA(ratio, NRules);
		double b_rule = computeCoefficientB(ratio, NRules);
		
		for (int i = 1; i <= NRules; i++) {
			double dist = a_rule + b_rule * i;
			String ruleName = config.getRuleList().get(i - 1).getName();
			if (ruleName == null || ruleName.isEmpty()) {
				ruleName = "Rule" + i;
				config.getRuleList().get(i-1).setName(ruleName);
			}
			ruleToFreq.put(ruleName, dist);
		}
		
		double a_user = computeCoefficientA(userRatio, NUsers);
		double b_user = computeCoefficientB(userRatio, NUsers);
		
		int i = 1;
		for (String user : config.getUsers()) {
			double dist = a_user + b_user * i;
			userToFreq.put(user, dist);
			i++;
		}
				
		double a_res = computeCoefficientA(ratio, NResources);
		double b_res = computeCoefficientB(ratio, NResources);
		
		i = 1;
		for (String resource : config.getResources()) {
			double dist = a_res + b_res * i;
			resourceToFreq.put(resource, dist);
			i++;
		}
		
		double a_op = computeCoefficientA(ratio, NOps);
		double b_op = computeCoefficientB(ratio, NOps);

		i = 1;
		for (String op : config.getOps()) {
			double dist = a_op + b_op * i;
			opToFreq.put(op, dist);
			i++;
		}
		// output to file
		try {		 
			File file = new File(outputFile);
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
 
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (String ruleName : ruleToFreq.keySet()) {
				bw.write("ruleDist(" + ruleName + ", " + ruleToFreq.get(ruleName) + ")\n");
			}
			bw.write("\n");
			
			for (String user : userToFreq.keySet()) {
				bw.write("userDist(" + user + ", " + userToFreq.get(user) + ")\n");
			}
			bw.write("\n");
			
			for (String res : resourceToFreq.keySet()) {
				bw.write("resDist(" + res + ", " + resourceToFreq.get(res) + ")\n");
			}
			bw.write("\n");
			
			for (String op : opToFreq.keySet()) {
				bw.write("opDist(" + op + ", " + opToFreq.get(op) + ")\n");
			}
			bw.write("\n");					
			bw.close(); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static double computeCoefficientB(double ratio, int N) {
		return ((double) 2 * (ratio - 1)) / ((ratio + 1) * N * (N - 1));
	}
	
	public static double computeCoefficientA(double ratio, int N) {
		return ((double) 2) / ((ratio + 1) * N);
	}
}
