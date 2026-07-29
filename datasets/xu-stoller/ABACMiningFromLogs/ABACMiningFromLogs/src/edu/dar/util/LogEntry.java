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

public class LogEntry implements Comparable<LogEntry> {
	public String user;
	public String resource;
	public String op;
	public int id;
	public double freq;
	public LogEntry(String user, String resource, String op, int id) {
		this.user = user;
		this.resource = resource;
		this.op = op;
		this.id = id;
	}public LogEntry()
	{
		
	}
	@Override
	public String toString() {
		return "<" + id + "," + user + "," + resource + "," + op  + ">";
	}
	public String toProgolString()
	{
		return "up(" + user + "," + resource + "," + op + ").\n";
	}
	
	public void fromString(String lg)
	{
		lg=lg.substring(1);
		lg=lg.substring(0, lg.length()-1);
		
		user=lg.split(",")[1];
		resource=lg.split(",")[2];
		op=lg.split(",")[3];
		id=Integer.parseInt(lg.split(",")[0]);
	}
	public void fromStringWithFreq(String s)
	{
		try
		{
			freq=Double.parseDouble(s.split(" ")[1]);
		} catch (NumberFormatException e)
		{
			freq=0;
		}
		
		this.fromString(s.split(" ")[0]);
	}
	
	public boolean equals(LogEntry log)
	{
		if (log.user.equals(this.user) && log.resource.equals(this.resource) && log.op.equals(this.op))
			return true;
		else
			return false;
	}
	
	public int compareTo(LogEntry log) 
	{
		return Double.compare(this.freq, log.freq);
	}
}
