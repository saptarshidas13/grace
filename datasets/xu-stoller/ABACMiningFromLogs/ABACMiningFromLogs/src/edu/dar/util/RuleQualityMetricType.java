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

public enum RuleQualityMetricType {
  // Q(rho) = (|covered(rho)| / |rho|) * (1 - w'_o * |overAssign(rho)| / |covered(rho)|)
	Q_RELATIVE_TO_COVERED, 

  // Q(rho) = (|covered(rho)| / |rho|) * (1 - w'_o * |overAssign(rho)| / |meaning(rho)|)
	Q_RELATIVE_TO_MEANING, 

  // Q(rho) = (|covered(rho)| / |rho|) * (1 - w'_o * |overAssign(rho)| / |UP_0|)
	Q_RELATIVE_TO_UP0, 

  // Q(rho) = (|covered(rho)| / |rho|) * (1 - w'_o * |overAssign(rho)| / |UPforPerms(rho)|)
  // where UPforPerms(rho) = { <u,r,o> in UP_0 | r models rae(rho) and o in ops(rho) }
	Q_RELATIVE_TO_PERMISSIONS, 

  //  Q(\rho) = (|covered(\rho)| - w'_o * |overassign(rho)|) / (UPforPerms(rho) * |\rho|)
	Q_RELATIVE_TO_SIZE_AND_PERMISSIONS,

  // ILP-based metric, denoted Q^{ILP} in the paper
	Q_PROGOL,

  // same as above, except weight each user-permission typle by its relative frequency in 
  // the log, by replacing |covered(rho)| with |covered(rho)|_L, which is defined in 
  // Section 3 of the paper.
	Q_RELATIVE_TO_COVERED_FREQ, 
	Q_RELATIVE_TO_MEANING_FREQ, 
	Q_RELATIVE_TO_UP0_FREQ, 
	Q_RELATIVE_TO_PERMISSIONS_FREQ, 
	Q_RELATIVE_TO_SIZE_AND_PERMISSIONS_FREQ,
}
