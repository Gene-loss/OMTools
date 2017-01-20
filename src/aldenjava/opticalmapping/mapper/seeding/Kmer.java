/**************************************************************************
**  OMTools
**  A software package for processing and analyzing optical mapping data
**  
**  Version 1.2 -- January 1, 2017
**  
**  Copyright (C) 2017 by Alden Leung, Ting-Fung Chan, All rights reserved.
**  Contact:  alden.leung@gmail.com, tf.chan@cuhk.edu.hk
**  Organization:  School of Life Sciences, The Chinese University of Hong Kong,
**                 Shatin, NT, Hong Kong SAR
**  
**  This file is part of OMTools.
**  
**  OMTools is free software; you can redistribute it and/or 
**  modify it under the terms of the GNU General Public License 
**  as published by the Free Software Foundation; either version 
**  3 of the License, or (at your option) any later version.
**  
**  OMTools is distributed in the hope that it will be useful,
**  but WITHOUT ANY WARRANTY; without even the implied warranty of
**  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**  GNU General Public License for more details.
**  
**  You should have received a copy of the GNU General Public 
**  License along with OMTools; if not, see 
**  <http://www.gnu.org/licenses/>.
**************************************************************************/


package aldenjava.opticalmapping.mapper.seeding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import aldenjava.opticalmapping.data.data.DataNode;
/**
 * Kmer, or K-tuple, is a class to represent k consecutive segments
 * @author Alden
 *
 */
public class Kmer {

	public final String source;
	public final int pos;
	public final List<Long> sizelist;
	public final List<Long> realsizelist;
	public final List<Integer> errorposlist;

	public Kmer(String source, int pos, List<Long> sizelist, List<Long> realsizelist, List<Integer> errorposlist) {
		this.source = source;
		this.pos = pos;
		this.sizelist = sizelist;
		this.realsizelist = realsizelist;
		this.errorposlist = errorposlist;
	}

	public Kmer(String source, int pos, List<Long> realsizelist, List<Integer> errorposlist) {
		this.source = source;
		this.pos = pos;
		this.sizelist = getSizeList(realsizelist, errorposlist);
		this.realsizelist = realsizelist;
		this.errorposlist = errorposlist;
	}

	public Kmer(String source, int pos, List<Long> sizelist) {
		this.source = source;
		this.pos = pos;
		this.sizelist = sizelist;
		this.realsizelist = sizelist;
		this.errorposlist = new ArrayList<Integer>();
	}

	public Kmer(Kmer kmer) {
		this.source = kmer.source;
		this.pos = kmer.pos;
		this.sizelist = new ArrayList<Long>(kmer.sizelist);
		this.realsizelist = new ArrayList<Long>(kmer.realsizelist);
		this.errorposlist = new ArrayList<Integer>(kmer.errorposlist);
	}

	private List<Long> getSizeList(List<Long> realsizelist, List<Integer> errorposlist) {
		List<Long> sizelist = new ArrayList<Long>();
		long cumSize = 0;
		for (int i = 0; i < realsizelist.size(); i++) {
			cumSize += realsizelist.get(i);
			if (!errorposlist.contains(i)) {
				sizelist.add(cumSize);
				cumSize = 0;
			}
		}
		return sizelist;
	}

	public long get(int pos) {
		return sizelist.get(pos);
	}

	public int compare(Kmer k, int pos) {
		return Long.valueOf(this.get(pos)).compareTo(Long.valueOf(k.get(pos)));
	}

	public int k() {
		return sizelist.size();
	}

	/**
	 * Length of the whole kmer, including the franking signals
	 * @return
	 */
	public long length() {
		long total = 0;
		for (Long i : sizelist)
			total += i + 1;
		total += 1;
		return total;
	}

	public int getErrorNo() {
		return errorposlist.size();
	}

	public Kmer newKmer(double sizeratio, int extrasize) {
		List<Long> newsizelist = new ArrayList<Long>();
		for (int i = 0; i < this.sizelist.size(); i++)
			newsizelist.add((long) (this.sizelist.get(i) * sizeratio) + extrasize);
		return new Kmer(this.source, this.pos, newsizelist, realsizelist, errorposlist);
	}

	public boolean limitRange(Kmer kmer, int measure, double ear) {
		double ubound = 1 + ear;
		double lbound = 1 - ear;
		for (int pos = 0; pos < k(); pos++) {
			double newubound = (kmer.get(pos) + measure) / (double) get(pos);
			double newlbound = (kmer.get(pos) - measure) / (double) get(pos);
			if (newubound < ubound)
				ubound = newubound;
			if (newlbound > lbound)
				lbound = newlbound;
		}
		return (ubound >= lbound);
	}

	public DataNode toDataNode() {
		return toDataNode(this.source + "_" + this.pos);
	}
	public DataNode toDataNode(String name) {
		long[] refl = new long[sizelist.size() + 2];
		for (int i = 0; i < sizelist.size(); i++)
			refl[i + 1] = sizelist.get(i);
		return new DataNode(name, refl);
	}
	@Override
	public String toString() {
		String s = "";
		for (int i = 0; i < sizelist.size(); i++)
			s += Long.toString(this.get(i)) + " ";
		return s;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Kmer))
			return false;
		Kmer kmer = (Kmer) obj;
		if (!this.source.equals(kmer.source))
			return false;
		if (this.pos != kmer.pos)
			return false;
		return true;
		
	}
	@Override
	public int hashCode() {
		return this.source.hashCode() + this.pos;
	}
	
	public static Comparator<Kmer> comparator(int pos) {
		final int x = pos;
		return new Comparator<Kmer>() {
			@Override
			public int compare(Kmer k1, Kmer k2) {
				return Long.valueOf(k1.get(x)).compareTo(Long.valueOf(k2.get(x)));
			}
		};

	}

	public static Comparator<Kmer> comparatorWithSource(int pos) {
		final int x = pos;
		return new Comparator<Kmer>() {
			@Override
			public int compare(Kmer k1, Kmer k2) {
				int now;
				now = Long.valueOf(k1.get(x)).compareTo(Long.valueOf(k2.get(x)));
				if (now != 0)
					return now;
				else {
					now = k1.source.compareTo(k2.source);
					if (now != 0)
						return now;
					else
						return Integer.valueOf(k1.pos).compareTo(Integer.valueOf(k2.pos));
				}

			}
		};
	}

	public static Comparator<Kmer> comparatorSourcePos() {
		return new Comparator<Kmer>() {
			@Override
			public int compare(Kmer k1, Kmer k2) {
				int now;
				now = k1.source.compareTo(k2.source);
				if (now != 0)
					return now;
				else
					return Integer.valueOf(k1.pos).compareTo(Integer.valueOf(k2.pos));
			}

		};

	}
}
