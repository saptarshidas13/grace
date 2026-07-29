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

package edu.dar.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Configuration class is used for maintaining the configuration of various
 * input parameters
 * 
 * @author Zhongyuan Xu
 * 
 */

public class Config {
	/* the following fileds are created for new parser */
	// user set
	private HashSet<String> users;
	// resource set
	private HashSet<String> resources;
    // permission set where a permission is a pair of <resource, operation>
	private HashSet<Pair<String, String>> perms;
	// operation set
	private HashSet<String> ops;

	// user attribute name => user attribute info
	private HashMap<String, Attribute> userAttrSet;
	// resource attribute name => resource attribute info
	private HashMap<String, Attribute> resourceAttrSet;

	// user id => {user attribute name => user attribute value set}
	private HashMap<String, HashMap<String, HashSet<String>>> userAttrInfo;
	// resource id => {resource attribute name => resource attribute value set}
	private HashMap<String, HashMap<String, HashSet<String>>> resourceAttrInfo;

	// original rule set
	private ArrayList<Rule> ruleList;

	// user => permissions of the user
	private HashMap<String, HashSet<Pair<String, String>>> userPerms;
	// permission => users that has this permission
	private HashMap<Pair<String, String>, HashSet<String>> permUsers;

	// resource => user set
	private HashMap<String, HashSet<String>> resourceUsers;
	// user => resource set
	private HashMap<String, HashSet<String>> userResources;

	//UP induced by the original rules
	private HashSet<Triple<String, String, String>> coveredUP;
	// overassignment injected into the coveredUP, only used in experiment with noise
	private HashSet<Triple<String, String, String>> overassignmentUP;
	// underassignment injected into the coveredUP, only used in experiment with noise
	private HashSet<Triple<String, String, String>> underassignmentUP;
	// UP induced by logs
	private HashSet<Triple<String, String, String>> logInducedUP;
	
	// log => probability that this log is to appear in logs
	public HashMap<Triple<String, String, String>, Double> logToProbability;
	
	// the set of log entries
	public ArrayList<LogEntry> logs;
	
	//user => probability that this user will be selected in log generation process
	public HashMap<String, Double> userFreqMap;
	// resource => probability that this resource will be selected in log generation process
	public HashMap<String, Double> resFreqMap;
	// rule => probability that this rule will be selected in log generation process
	public HashMap<String, Double> ruleFreqMap;
	// operation => probability that this operation will be selected in log generation process
	public HashMap<String, Double> opFreqMap;
	
	public Config() {
		users = new HashSet<String>();
		resources = new HashSet<String>();
		ops = new HashSet<String>();
		perms = new HashSet<Pair<String, String>>();

		userAttrInfo = new HashMap<String, HashMap<String, HashSet<String>>>();
		resourceAttrInfo = new HashMap<String, HashMap<String, HashSet<String>>>();

		userAttrSet = new HashMap<String, Attribute>();
		resourceAttrSet = new HashMap<String, Attribute>();

		ruleList = new ArrayList<Rule>();

		userPerms = new HashMap<String, HashSet<Pair<String, String>>>();
		permUsers = new HashMap<Pair<String, String>, HashSet<String>>();

		resourceUsers = new HashMap<String, HashSet<String>>();
		userResources = new HashMap<String, HashSet<String>>();

		coveredUP = new HashSet<Triple<String, String, String>>();
		overassignmentUP = new HashSet<Triple<String, String, String>>();
		underassignmentUP = new HashSet<Triple<String, String, String>>();
		logInducedUP = new HashSet<Triple<String, String, String>>();
			
		userFreqMap = new HashMap<String, Double>();
		resFreqMap = new HashMap<String, Double>();
		ruleFreqMap = new HashMap<String, Double>();
		opFreqMap = new HashMap<String, Double>();
				
		logToProbability = new HashMap<Triple<String, String, String>, Double>();
	}

	public int getNumUsers() {
		return users.size();
	}

	public int getNumPerms() {
		return perms.size();
	}

	public int getNumUserAttribs() {
		return userAttrSet.size();
	}

	public int getNumPermAttribs() {
		return resourceAttrSet.size();
	}

	public void addUser(String user) {
		this.users.add(user);
	}

	public void addPerm(Pair<String, String> perm) {
		this.perms.add(perm);
	}

	public void addResource(String resource) {
		this.resources.add(resource);
	}

	public HashSet<String> getUsers() {
		return users;
	}

	public HashSet<Pair<String, String>> getPerms() {
		return perms;
	}

	public HashSet<String> getResources() {
		return resources;
	}

	public void setResources(HashSet<String> resources) {
		this.resources = resources;
	}

	public HashSet<String> getOps() {
		return ops;
	}

	public void setOps(HashSet<String> ops) {
		this.ops = ops;
	}

	public HashMap<String, HashMap<String, HashSet<String>>> getUserAttrInfo() {
		return userAttrInfo;
	}

	public HashMap<String, HashMap<String, HashSet<String>>> getResourceAttrInfo() {
		return resourceAttrInfo;
	}

	public HashMap<String, Attribute> getUserAttrSet() {
		return userAttrSet;
	}

	public HashMap<String, Attribute> getResourceAttrSet() {
		return resourceAttrSet;
	}

	public ArrayList<Rule> getRuleList() {
		return ruleList;
	}

	public void setRuleList(ArrayList<Rule> ruleList) {
		this.ruleList = ruleList;
	}

	public HashMap<String, HashSet<Pair<String, String>>> getUserPerms() {
		return userPerms;
	}

	public void setUserPerms(
			HashMap<String, HashSet<Pair<String, String>>> userPerms) {
		this.userPerms = userPerms;
	}

	public HashMap<Pair<String, String>, HashSet<String>> getPermUsers() {
		return permUsers;
	}

	public void setPermUsers(
			HashMap<Pair<String, String>, HashSet<String>> permUsers) {
		this.permUsers = permUsers;
	}

	public HashSet<Triple<String, String, String>> getCoveredUP() {
		return coveredUP;
	}

	public void setCoveredUP(HashSet<Triple<String, String, String>> coveredUP) {
		this.coveredUP = coveredUP;
	}

	public HashSet<Triple<String, String, String>> getLogInducedUP() {
		return logInducedUP;
	}

	public void setLogInducedUP(HashSet<Triple<String, String, String>> logInducedUP) {
		this.logInducedUP = new HashSet<Triple<String, String, String>>(logInducedUP);
	}

	public HashMap<String, HashSet<String>> getResourceUsers() {
		return resourceUsers;
	}

	public void setResourceUsers(HashMap<String, HashSet<String>> resourceUsers) {
		this.resourceUsers = resourceUsers;
	}

	public HashMap<String, HashSet<String>> getUserResources() {
		return userResources;
	}

	public void setUserResources(HashMap<String, HashSet<String>> userResources) {
		this.userResources = userResources;
	}

	public HashSet<Triple<String, String, String>> getOverassignmentUP() {
		return overassignmentUP;
	}

	public void setOverassignmentUP(
			HashSet<Triple<String, String, String>> overassignmentUP) {
		this.overassignmentUP = overassignmentUP;
	}

	public HashSet<Triple<String, String, String>> getUnderassignmentUP() {
		return underassignmentUP;
	}

	public void setUnderassignmentUP(
			HashSet<Triple<String, String, String>> underassignmentUP) {
		this.underassignmentUP = underassignmentUP;
	}

	public HashMap<String, Double> getUserFreqMap() {
		return userFreqMap;
	}

	public void setUserFreqMap(HashMap<String, Double> userFreqMap) {
		this.userFreqMap = userFreqMap;
	}

	public HashMap<String, Double> getResFreqMap() {
		return resFreqMap;
	}

	public void setResFreqMap(HashMap<String, Double> resFreqMap) {
		this.resFreqMap = resFreqMap;
	}

	public HashMap<String, Double> getRuleFreqMap() {
		return ruleFreqMap;
	}

	public void setRuleFreqMap(HashMap<String, Double> ruleFreqMap) {
		this.ruleFreqMap = ruleFreqMap;
	}

	public HashMap<String, Double> getOpFreqMap() {
		return opFreqMap;
	}

	public void setOpFreqMap(HashMap<String, Double> opFreqMap) {
		this.opFreqMap = opFreqMap;
	}

	public ArrayList<LogEntry> getLogs() {
		return logs;
	}

	public void setLogs(ArrayList<LogEntry> logs) {
		this.logs = new ArrayList<LogEntry>(logs);
	}

	public HashMap<Triple<String, String, String>, Double> getLogToProbability() {
		return logToProbability;
	}

	public void setLogToProbability(
			HashMap<Triple<String, String, String>, Double> logToProbability) {
		this.logToProbability = logToProbability;
	}
}
