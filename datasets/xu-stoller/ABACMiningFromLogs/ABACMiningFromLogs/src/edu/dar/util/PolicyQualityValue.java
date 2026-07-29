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

public class PolicyQualityValue implements Comparable<PolicyQualityValue>{
	public double firstComponent;
	public double secondComponent;
	public PolicyQualityValue(double v1, double v2) {
		firstComponent = v1;
		secondComponent = v2;
	}
	public PolicyQualityValue() {
		firstComponent = 0.0;
		secondComponent = 0.0;
	}
	
	public PolicyQualityValue(PolicyQualityValue v) {
		firstComponent = v.firstComponent;
		secondComponent = v.secondComponent;
	}
	@Override
	public int compareTo(PolicyQualityValue v) {
		if (this == v) {
			return 0;
		}
		
		if (Double.compare(this.firstComponent, v.firstComponent) > 0) {
			return 1;
		}
		if (Double.compare(this.firstComponent, v.firstComponent) < 0) {
			return -1;
		}
		if (Double.compare(this.secondComponent, v.secondComponent) > 0) {
			return 1;
		}
		if (Double.compare(this.secondComponent, v.secondComponent) < 0) {
			return -1;
		}	
		return 0;
	}
}

