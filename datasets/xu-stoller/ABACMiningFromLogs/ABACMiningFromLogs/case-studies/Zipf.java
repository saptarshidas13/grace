import java.util.Random;

interface ProbabilityDistribution {
	//public void setSize();
	public Integer getNextDistVal();
	public double getProbability(int rank); 
}

/* http://code.google.com/p/haggle/source/browse/android/LuckyMe/src/org/haggle/LuckyMe/Zipf.java?spec=svnf77395981c6e37ed2818509222364331001fd86e&r=092516d3d6672fe662051b1ef625345c265408af
 * 
 */
//Based on code by Hyunsik Choi
//http://diveintodata.org/2009/09/zipf-distribution-generator-in-java/
class ZipfDistrib implements ProbabilityDistribution {
	private Random 	rnd;
	private int 	size;
	private double 	skew;
	private double 	bottom = 0;

	/* @size: the number of elements
	 * @skew: value of exponent characterizing the distribution,
	 *        (assume 1 for now)
	 * @seed: seed for Zipfian Distribution 
	 */
	public ZipfDistrib(int size, double skew, long seed) {
		this.rnd = new Random(seed);
		this.size = size;
		this.skew = skew;

		for (int i = 1; i < size; i++) {
			this.bottom += (1 / Math.pow(i, this.skew));
		}
	}

	public void setSeed(long seed) {
		rnd = new Random(seed);
	}

	// the next() method returns an rank id. The frequency of returned rank ids
	// are follows Zipf distribution.
	public Integer getNextDistVal() {
		int rank;
		double friquency = 0;
		double dice;

		rank = rnd.nextInt(size);
		friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
		dice = rnd.nextDouble();

		while (!(dice < friquency)) {
			rank = rnd.nextInt(size);
			friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
			dice = rnd.nextDouble();
		}

		return rank;
	}

	// This method returns a probability that the given rank occurs.
	public double getProbability(int rank) {
		return (1.0d / Math.pow(rank, this.skew)) / this.bottom;
	}
}

public class Zipf {
  // usage: java Zipf <size>
  // prints a table of values of the Zipf distribution with the specified size 
  // and skew=1.
  public static void main(String[] args){
    int size = Integer.parseInt(args[0]);
    double skew = 1.0;
    long seed = 1;
    ProbabilityDistribution dist = new ZipfDistrib(size, skew, seed);
    double sum=0;
    for (int rank=1; rank <= size; rank++) {
   
      System.out.println("zipf("+rank+")="+dist.getProbability(rank));
    }
  }
}
