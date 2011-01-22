import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

class assignment5
{
	static int ngram_length = 3;
	
	public static void main(String[] args)
	{
		if(args[0].equals("learn"))
		{
			CrossValidator cv;
			try {
				cv = new CrossValidator(args[1]);
				for(int foldIndex = 0; foldIndex < 10; foldIndex++)
				{
					HMM hmm = cv.learn();
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