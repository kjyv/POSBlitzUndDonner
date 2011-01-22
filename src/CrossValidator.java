import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

class CrossValidator
{
	private Vector<String> trainFolds, testFolds;
	
	public CrossValidator(String dirName) throws IOException
	{
		trainFolds = new Vector<String>(500);
		testFolds = new Vector<String>(50);
		createFolds(dirName);
	}
	
	public void createFolds(String dirName) throws IOException
	{
		trainFolds.clear();
		testFolds.clear();
		File dir = new File(dirName);
		if(!dir.isDirectory())
		{
			throw new IllegalArgumentException("path is not a directory");
		}
		File[] annotatedFiles = dir.listFiles();
		int numFiles = annotatedFiles.length;
		// create array 0..len-1 and permute it randomly
		Vector<Integer> remainingIndices = new Vector<Integer>(numFiles);
		for(int i = 0; i < numFiles; i++)
			remainingIndices.add(i);
		Vector<Integer> permutatedIndices = new Vector<Integer>(numFiles);
		while(remainingIndices.size() > 0)
		{
			int ran = (int)Math.round(Math.random()*(remainingIndices.size()-1));
			permutatedIndices.add(remainingIndices.get(ran));
			remainingIndices.remove(ran);
		}
		remainingIndices = null;
		// permutatedIndices are now randomly permuted
		// 10 fold cross validation => use last 10% for testing
		int lastTrainIndex = (int)Math.round(0.9*(numFiles-1));
		System.out.println(lastTrainIndex);
		for(int trainIndex = 0; trainIndex <= lastTrainIndex; trainIndex++)
		{
			trainFolds.add(readFileAsString(annotatedFiles[permutatedIndices.get(trainIndex)]));
		}
		for(int testIndex = lastTrainIndex + 1; testIndex < numFiles; testIndex++)
		{
			testFolds.add(readFileAsString(annotatedFiles[permutatedIndices.get(testIndex)]));
		}
	}
	
	public HMM learn() throws UnsupportedOperationException
	{
		if(trainFolds == null || trainFolds.size() == 0)
			throw new UnsupportedOperationException("no training folds. you have to call createFolds() first");
		// join all training folds into one string to be parsed
		StringBuilder trainString = new StringBuilder(20000*trainFolds.size());	// 20000 chars per fold
		for(int i = 0; i < trainFolds.size(); i++)
		{
			trainString.append(trainFolds.get(i));
		}
		Parser p = new Parser(trainString.toString());
		Vector<String> tokens = p.getTokens(), tags = p.getTags();
		HMM hmm = new HMM();
		hmm.train(tokens, tags);
		return hmm;
	}
	
	// returns the accuracy (= percentage of correct tags)
	public CrossValidation evaluate(HMM hmm)
	{
		if(testFolds == null || testFolds.size() == 0)
			throw new UnsupportedOperationException("no test folds. you have to call createFolds() first. also make sure to have at least 10 files in the test/train set");
		StringBuilder testString = new StringBuilder(20000*testFolds.size());	// 20000 chars per fold
		for(int i = 0; i < testFolds.size(); i++)
		{
			testString.append(testFolds.get(i));
		}
		Parser p = new Parser(testString.toString());
		Vector<String> tokens = p.getTokens(), tagsTrue = p.getTags();
		Vector<String> tagsDecoded = hmm.decode(tokens);
		
		double accuracy = 0;
		int numSentences = 0;
		for(int i = 0; i < tagsDecoded.size(); i++)
		{
			String tagTrue = tagsTrue.get(i);
			if(tagsDecoded.get(i).equals(tagTrue))
				accuracy++;
			if(tagTrue.equals("."))
				numSentences++;
		}
		accuracy = accuracy / tagsDecoded.size();
		return new CrossValidation(accuracy, numSentences);
	}
	
	private static String readFileAsString(File filePath) throws java.io.IOException
	{
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(
                new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
}


