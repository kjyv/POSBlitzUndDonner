import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
	static int ngram_length = 1;
	
	public static void main(String[] args) throws IOException
	{
		long startTime = System.currentTimeMillis();

		/*
		Pattern p = Pattern.compile("(\\S+)/([^\\-/\\s]+)(-[^/\\s]+)?\\s");
		Matcher m = p.matcher("token/np ");
		System.out.println(m.find());
		System.out.println(m.group(1)+ "#" + m.group(2) + "#" + m.group(3));
		*/
		
		/*
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
		hmm2.train(tokensVector, tagsVector);
		hmm2.printGraph();
		Vector<String> tagsDecoded = hmm2.decode(tokensVector);
		System.out.println("done");
		System.out.println(tagsVector);
		System.out.println(tagsDecoded);
		*/
		
		File dir = new File(args[1]);
		if(!dir.isDirectory())
		{
			throw new IllegalArgumentException("path is not a directory");
		}
		File[] annotatedFiles = dir.listFiles();
		StringBuilder trainString = new StringBuilder();
		for (int i = 0; i < annotatedFiles.length; i++) {
			File f = annotatedFiles[i];
			trainString.append(CrossValidator.readFileAsString(f));
		}
		Parser p = new Parser(trainString.toString());
		Vector<String> tokens = p.getTokens();
		Vector<String> tags = p.getTags();
		HMM hmm3 = new HMM();
		System.out.println("training...");
		hmm3.train(tokens, tags);
		
		System.out.println("decoding:");
		String[] testTokens = {"Check", "this" , "shit", "out", "to" ,"test", "our", "model", "."};
		Vector<String> testTags = hmm3.decode(new Vector<String>(Arrays.asList(testTokens)));
		System.out.println("decoded tags:");
		System.out.println(testTags);
		System.out.println("real tags:");
		System.out.println("[dt, bez, at, nn, to, vb, pp$, nn, .]");
		
		System.out.println("done, decode took:");
		System.out.println((System.currentTimeMillis() - startTime)/1000.0f + "s");

		
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
					System.out.println("number of hmm states: " + hmm.statelist.length);
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