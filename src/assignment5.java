import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class assignment5
{
	static int ngram_length = 3;
	
	public static void main(String[] args)
	{
		/*
		Pattern p = Pattern.compile("(\\S+)/([^\\-/\\s]+)(-[^/\\s]+)?\\s");
		Matcher m = p.matcher("token/np ");
		System.out.println(m.find());
		System.out.println(m.group(1)+ "#" + m.group(2) + "#" + m.group(3));
		*/
		
		/*
		HMM hmm2 = new HMM();
		String[] tokens = {"token1", "token2", "token3", "token4", "token1", "token2", "token3"};
		String[] tags = {"tag1", "tag1", "tag1", "tag1", "tag1", "tag1", "tag1"};
		hmm2.train(new Vector<String>(Arrays.asList(tokens)), new Vector<String>(Arrays.asList(tags)));
		*/
		/*
		Vector<String> v1 = new Vector<String>();
		v1.add("test1");
		Vector<String> v2 = new Vector<String>();
		v2.add("test1");
		HashMap<Vector<String>,Double> hm = new HashMap<Vector<String>,Double>();
		hm.put(v1, new Double(1));
		Double d;
		d = hm.get(v1);
		System.out.println(d);
		v1 = null;
		System.gc();
		d = hm.get(v2);
		System.out.println(d);
		*/
		//if(true) return;
		
		if(args[0].equals("learn"))
		{
			CrossValidator cv;
			try {
				cv = new CrossValidator(args[1]);
				for(int foldIndex = 0; foldIndex < 10; foldIndex++)
				{
					HMM hmm = cv.learn();
					System.out.println("number of hmm states: " + hmm.graph.size());
					CrossValidation validated = cv.evaluate(hmm);
					System.out.println("Fold " + (foldIndex+1) + ", " + validated.numSentences + " sentences, accuracy " + validated.accuracy);
					cv.createFolds(args[1]);	// choose randomly from training set to obtain training and test files
				}
				// TODO: mean / stdev
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static String join(Collection<String> s, String delimiter)
	{
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }
}