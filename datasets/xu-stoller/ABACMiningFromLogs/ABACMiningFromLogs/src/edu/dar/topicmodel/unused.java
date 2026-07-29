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

// WD is not needed.  I thought it was needed based on the comments in
// GibbsSamplerAT.m, but now I think those comments are out-of-date.
// the implementation is in GibbsSamplerAT.cpp, not in that .m file.

// AD is needed, but I decided to generate MATLAB code defining it
// directly, instead of using MATLAB code that reads the data for it from a
// data file.
  
  // output WD, a words sparse count matrix of size |W| x |D|, where
  // |W|=vocabulary size, |D|=number of documents.  WD(i,j) contains the
  // frequency of word i in document j.
  public static void outputWD(String outFile, ArrayList<String> wordList, HashSet<Document> documents) {
    try {
      File file = new File(outFile+"WD.dat");
      if (!file.exists()) {
	file.createNewFile();
      }
      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);

      for (Document doc : documents) {
	for (Map.Entry<Integer, Integer> entry : doc.wordCount.entrySet()) {
	  int wordIdx = entry.getKey().intValue();
	  int count = entry.getValue().intValue();
	  String line = wordIdx + "\t" + doc.index + "\t" + count;
	  // need to add a newline?
	  bw.write(line);
	}
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }    

  // output AD, the author document sparse matrix, of size |A| x |D|,
  // where |A|=number of authors.  AD(i,j) contains binary values
  // indicating the presence and absence of author i on document j.
  // according to GibbsSamplerAT, entries in AD should have "double
  // precision" (not really binary).
  public static void outputAD(String outFile, HashSet<Document> documents) {
    try {
      File file = new File(outFile+"AD.dat");
      if (!file.exists()) {
	file.createNewFile();
      }
      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);

      for (Document doc : documents) {
	for (Author author : doc.authors) {
	  String line = author.index + "\t" + doc.index + "\t" + "1";
	}
      }
      bw.close();

    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

