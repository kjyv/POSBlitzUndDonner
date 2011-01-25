import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class assignment5
{
	static int ngram_length = 2;
	
	public static void main(String[] args)
	{
		/*
		Pattern p = Pattern.compile("(\\S+)/([^\\-/\\s]+)(-[^/\\s]+)?\\s");
		Matcher m = p.matcher("token/np ");
		System.out.println(m.find());
		System.out.println(m.group(1)+ "#" + m.group(2) + "#" + m.group(3));
		*/
		
		
		HMM hmm2 = new HMM();
		int numTokens = 12;
		String[] tokens = new String[numTokens];
		String[] tags = new String[numTokens];
		for(int i=0; i+3< numTokens; i+=4)
		{
			tokens[i] = "a";
			tokens[i+1] = "a";
			tokens[i+2] = "b";
			tokens[i+3] = "b";
			
			tags[i] = "s1";
			tags[i+1] = "s1";
			tags[i+2] = "s2";
			tags[i+3] = "s2";
		}
		//String[] tokens = {"a", "a", "a", "b", "b", "b", "a"};
		Vector<String> tokensVector = new Vector<String>(Arrays.asList(tokens));
		Vector<String> tagsVector = new Vector<String>(Arrays.asList(tags));
		//String[] tags = {"s1", "s1", "s1", "s2", "s2", "s2", "s1"};
		hmm2.train(tokensVector, new Vector<String>(Arrays.asList(tags)));
		hmm2.printGraph();
		Vector<String> tagsDecoded = hmm2.decode(tokensVector);
		System.out.println("done");
		System.out.println(tagsVector);
		System.out.println(tagsDecoded);
		
		if(true) return;
		
		if(args[0].equals("learn"))
		{
			CrossValidator cv;
			CrossValidation[] allValidations = new CrossValidation[10];
			double mean = 0.0, stdev = 0.0;
			try {
				cv = new CrossValidator(args[1]);
				for(int foldIndex = 0; foldIndex < 10; foldIndex++)
				{
					HMM hmm = cv.learn();
					System.out.println("number of hmm states: " + hmm.graph.size());
					allValidations[foldIndex] = cv.evaluate(hmm);
					mean += allValidations[foldIndex].accuracy;
					System.out.println("Fold " + (foldIndex+1) + ", " + allValidations[foldIndex].numSentences + " sentences, accuracy " + allValidations[foldIndex].accuracy);
					cv.createFolds(args[1]);	// choose randomly from training set to obtain training and test files
				}
				mean /= 10.0;
				for(int foldIndex = 0; foldIndex < 10; foldIndex++)
				{
					stdev += Math.pow((allValidations[foldIndex].accuracy - mean), 2);
				}
				stdev = Math.sqrt(stdev/10.0);
				System.out.println("Average accuracy " + mean + ", standard deviation " + stdev);
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