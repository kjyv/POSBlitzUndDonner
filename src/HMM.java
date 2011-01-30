import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.Vector;

class HMM
{
	String[] taglist;	// sorted states' tags, defines states' order. but it doesn't matter because we never perform a binary search on this array
	float[][] adjacencyMatrix;

	HMMState[] statelist;
	
	final float missingTokenEmissionProbability = -20.0f;
	final float missingEdgeTransitionProbability = -20.0f;
	
	public HMM(){}
	
	public HMM(String serializedFile) throws IOException, ClassNotFoundException
	{
		deserialize(serializedFile);
	}
	
	public void train(Vector<String> tokens, Vector<String> tags)
	{
		// tag -> state mapping for finding what we already have, slow, don't use for decoding
		HashMap<String, HMMState> graph = new HashMap<String, HMMState>();
			
		//create state graph from given data
		int ngram_length = assignment5.ngram_length;
		HMMState lastState = null;
		NGrams ngrams = createNGramsFromTokens(tokens, tags, ngram_length);
		
		//generate sorted list of tags (index->tag n-gram mapping)
		TreeSet<String> tagsSet = new TreeSet<String>();
		for(Vector<String> tag: ngrams.tags){
			tagsSet.add(assignment5.join(tag, " "));
		}
		taglist = (String[])tagsSet.toArray(new String[tagsSet.size()]);
		//Arrays.sort(taglist); // TreeSet sorts automatically when adding an element
		
		statelist = new HMMState[taglist.length];
		
		for(int ngramIndex=0; ngramIndex<ngrams.tags.size(); ngramIndex++)
		{
			Vector<String> ngram_tokens = ngrams.tokens.get(ngramIndex);
			//Vector<String> ngram_end_tokens = ngrams.endTokens.get(ngramIndex);
			Vector<String> ngram_tags = ngrams.tags.get(ngramIndex);
			String ngram_tags_joined = assignment5.join(ngram_tags, " ");
			HMMState state = graph.get(ngram_tags_joined);
			
			if(state == null) {
				//create new state
				state = new HMMState();
				state.tags = ngram_tags.toArray(new String[0]);
				state.firstTag = ngram_tags.get(0);

				//state.probabilities = new HashMap<Vector<String>, Double>();	// already done in construcctor
				graph.put(ngram_tags_joined, state);
			}

			//add absolute probability
			Double prob = state.probabilities.get(ngram_tokens);
			if(prob != null){
				// prob++ does not work
				// TODO: see if state.emissionProbabilites is really faster than bin search
				state.probabilities.put(new Vector<String>(ngram_tokens), prob+1);
			} else {
				state.probabilities.put(new Vector<String>(ngram_tokens), 1.0);
			}

			/*if(!ngram_end_tokens.firstElement().equals(" ")){
				//add probabilities for endings
				Double endProb = state.ending_probabilities.get(ngram_end_tokens);
				if(endProb != null){
					state.ending_probabilities.put(new Vector<String>(ngram_end_tokens), endProb+1);
				} else {
					state.ending_probabilities.put(new Vector<String>(ngram_end_tokens), 1.0);
				}
			}*/
			
			//add or update edge to this state
			Integer currentStateIndex = Arrays.binarySearch(taglist, ngram_tags_joined); 
			if(lastState != null){
				HMMEdge edge = lastState.outgoing_map.get(currentStateIndex);
				if (edge != null){
					edge.probability++;
				} else {
					lastState.outgoing_map.put(currentStateIndex, new HMMEdge(/*state, */1.0f));
				}
			}
			lastState = state;
		}
		
		//normalize probabilities
		//System.out.println("normalizing");
		for (String key: graph.keySet()){
			HMMState state = graph.get(key);
			//System.out.println("-----STATE:" + state.tags+"--------");
			
			int total_emissions = 0;
			//get number of emissions
			for (Vector<String> p_key : state.probabilities.keySet()){
				Double p = state.probabilities.get(p_key);
				total_emissions += p.intValue();
			}
			//System.out.println("state #" + ngram_tags_joined +  "# has " +total_emissions + " emissions, keysetSize = " + state.probabilities.keySet().size());
			for (Vector<String> p_key : state.probabilities.keySet()){
				//System.out.println("iterator: key !" + assignment5.join(p_key, "#")+"!");
				Double p = state.probabilities.get(p_key);
				p /= total_emissions;
				p = Math.log(p);
				state.probabilities.put(p_key, p);
			}
			
			/*total_emissions = 0;
			//get number of emissions
			for (Vector<String> p_key : state.ending_probabilities.keySet()){
				Double p = state.ending_probabilities.get(p_key);
				total_emissions += p.intValue();
			}
			//System.out.println("state #" + ngram_tags_joined +  "# has " +total_emissions + " emissions, keysetSize = " + state.probabilities.keySet().size());
			for (Vector<String> p_key : state.ending_probabilities.keySet()){
				//System.out.println("iterator: key !" + assignment5.join(p_key, "#")+"!");
				Double p = state.ending_probabilities.get(p_key);
				p /= total_emissions;
				p = Math.log(p);
				state.ending_probabilities.put(p_key, p);
			}*/			


			//get sum of all edges
			int sum_edge_probs = 0;
			for(Integer e_key: state.outgoing_map.keySet()){
				HMMEdge edge = state.outgoing_map.get(e_key);
				sum_edge_probs += (int)edge.probability;
			}

			//normalize edges
			for(Integer e_key: state.outgoing_map.keySet()){
				HMMEdge edge = state.outgoing_map.get(e_key);
				edge.probability /= sum_edge_probs;
				edge.probability = (float)Math.log(edge.probability);
			}
		}
		
		//finished training, now generate static data		
		for (int tag = 0; tag < taglist.length; tag++) {
			//System.out.println(taglist[tag]);

			//set index for each state
			HMMState currState = graph.get(taglist[tag]);
			currState.tagindex = tag;

			//create arrays for vectors
			statelist[tag] = currState;
			String[] emittedTokens = new String[currState.probabilities.size()];
			float[] emissionProbs = new float[emittedTokens.length];
			
			int counter = 0;
			for(Entry<Vector<String>, Double> emissionEntry : currState.probabilities.entrySet())
			{
				emittedTokens[counter++] = assignment5.join(emissionEntry.getKey(), " ");
			}
			Arrays.sort(emittedTokens);
			
			// get emission probabilites in sorted order
			counter = 0;
			for(String emittedToken : emittedTokens)
			{
				Vector<String> vec = new Vector<String>(Arrays.asList(emittedToken.split(" ")));
				emissionProbs[counter++] = currState.probabilities.get(vec).floatValue();
			}
			currState.seenTokens = emittedTokens;
			currState.seenTokenEmissionProbabilities = emissionProbs;
			currState.probabilities = null;
			
			// again for endings
			/*String[] emittedEndTokens = new String[currState.ending_probabilities.size()];
			float[] emissionEndProbs = new float[emittedEndTokens.length];
			
			counter = 0;
			for(Entry<Vector<String>, Double> emissionEntry : currState.ending_probabilities.entrySet())
			{
				emittedEndTokens[counter++] = assignment5.join(emissionEntry.getKey(), " ");
			}
			Arrays.sort(emittedEndTokens);
			
			// get emission probabilites in sorted order
			counter = 0;
			for(String emittedToken : emittedEndTokens)
			{
				Vector<String> vec = new Vector<String>(Arrays.asList(emittedToken.split(" ")));
				emissionEndProbs[counter++] = currState.ending_probabilities.get(vec).floatValue();
			}
			currState.seenEndTokens = emittedEndTokens;
			currState.seenEndTokenEmissionProbabilities = emissionEndProbs;
			currState.ending_probabilities = null;
			*/
			
		}

		// using the state's tag index, fill adjacency matrix to get rid of HMMEdge and HMMState.outgoing
		// matrix is not symmetric since we have directed edges
		adjacencyMatrix = new float[taglist.length][taglist.length];
		for (int i = 0; i < taglist.length; i++) {
			HMMState fromState = graph.get(taglist[i]);
			for (int j = 0; j <  taglist.length; j++) {
				HMMEdge outgoingEdgeToJ = fromState.outgoing_map.get(j);
				if(outgoingEdgeToJ == null)
					adjacencyMatrix[i][j] = missingEdgeTransitionProbability;
				else
					adjacencyMatrix[i][j] = fromState.outgoing_map.get(j).probability;
			}
			fromState.outgoing_map = null;
		}
		
		// HMMState.outgoing_map and HMMState.probabilites have been set to null, ready for serialization
		System.gc();
	}
	
	public Vector<String> decode(Vector<String> tokens)
	{
		//System.out.println("decoding");
		int ngram_length = assignment5.ngram_length;
		NGrams ngrams = createNGramsFromTokens(tokens, null, ngram_length);

		int numStates = statelist.length
			, numNGrams = ngrams.tokens.size();

		float[][] viterbi = new float[numNGrams][numStates];

		// first column (ngram): no transition probabilities, no previous probabilites => only emission probs
		for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
		{
			HMMState currState = statelist[currStateIndex];

			int emissionTokenIndex = Arrays.binarySearch(
										currState.seenTokens,
										ngrams.tokensJoined[0]
										);
			float probEmission;
			if(emissionTokenIndex < 0){
				//if we don't have the token itself, try its ending
				/*emissionTokenIndex = Arrays.binarySearch(
						currState.seenEndTokens,
						ngrams.endTokensJoined[0]
						);
				if(emissionTokenIndex < 0){*/
					probEmission = missingTokenEmissionProbability;
				/*} else {
					//System.out.println("Found better prob than missing for: "+ currState.seenEndTokens[emissionTokenIndex]);
					probEmission = currState.seenEndTokenEmissionProbabilities[emissionTokenIndex];
				}*/
			} else {
				probEmission = currState.seenTokenEmissionProbabilities[emissionTokenIndex];
			}
			
			viterbi[0][currStateIndex] = probEmission;
		}
		
		for(int ngramIndex=1; ngramIndex<numNGrams; ngramIndex++)
		{
			//System.out.println("decoding: column " + ngramIndex + " / " + numNGrams);
			String ngram_tokens_joined = ngrams.tokensJoined[ngramIndex];
			for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
			{
				HMMState currState = statelist[currStateIndex];
				
				int emissionTokenIndex = Arrays.binarySearch(currState.seenTokens, ngram_tokens_joined);

				float probEmission;
				if(emissionTokenIndex < 0){
					/*emissionTokenIndex = Arrays.binarySearch(
							currState.seenEndTokens,
							ngrams.endTokensJoined[ngramIndex]
							);
					if(emissionTokenIndex < 0){*/
						probEmission = missingTokenEmissionProbability;
					/*} else {
						//System.out.println("Found better prob than missing for: "+ currState.seenEndTokens[emissionTokenIndex]);
						probEmission = currState.seenEndTokenEmissionProbabilities[emissionTokenIndex];
					}*/
				} else {
					probEmission = currState.seenTokenEmissionProbabilities[emissionTokenIndex];
					//System.out.println("col "+ngramIndex+", EmissionFound: " + probEmission + ") in state #"+currStateIndex+" " + taglist[currStateIndex] + ", for tokens " + ngram_tokens_joined);
				}
				
				float maxProb = Float.NEGATIVE_INFINITY;
				// determine maximum probability to reach currState (from any previous state)
				float prob;
				for(int previousStateIndex = 0; previousStateIndex < numStates; previousStateIndex++)
				{
					prob = viterbi[ngramIndex-1][previousStateIndex] + adjacencyMatrix[previousStateIndex][currStateIndex];
					if(prob > maxProb)
					{
						maxProb = prob;
					}
				}
				viterbi[ngramIndex][currStateIndex] = maxProb + probEmission;
			}
		}
		
		//System.out.println("viterbi table");
		//print2dArray(viterbi);
		
		// backtracking to find the optimal (most probable) path
		LinkedList<String> tags = new LinkedList<String>();	// to be able to prepend items efficiently
		// determine maximum of last column to use as initial state for backtracking
		float max = Float.NEGATIVE_INFINITY;
		int maxStateIndex = -1;
		for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
		{
			if(viterbi[numNGrams-1][currStateIndex] > max)
			{
				max = viterbi[numNGrams-1][currStateIndex];
				maxStateIndex = currStateIndex;
			}
		}
		
		tags.addAll(0,Arrays.asList(statelist[maxStateIndex].tags));	// prepends tags
		
		for(int ngramIndex=numNGrams-2; ngramIndex>=0; ngramIndex--)	// go backwards in time
		{
			//System.out.println("decoding: backtracking: column " + ngramIndex);
			HMMState maxState = statelist[maxStateIndex];
			String ngram_tokens_joined = ngrams.tokensJoined[ngramIndex+1];	// from "next" timestep

			int emissionTokenIndex = Arrays.binarySearch(maxState.seenTokens, ngram_tokens_joined);
			
			float probEmission;
			if(emissionTokenIndex < 0){
				/*emissionTokenIndex = Arrays.binarySearch(
						maxState.seenEndTokens,
						ngrams.endTokensJoined[ngramIndex+1]
						);
				if(emissionTokenIndex < 0){*/
					probEmission = missingTokenEmissionProbability;
				/*} else {
					//System.out.println("Found better prob than missing for: "+ maxState.seenEndTokens[emissionTokenIndex]);
					probEmission = maxState.seenEndTokenEmissionProbabilities[emissionTokenIndex];
				}*/
			} else {
				probEmission = maxState.seenTokenEmissionProbabilities[emissionTokenIndex];
			}
			
			// loop all states and find the one that transitioned to maxState
			for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
			{
				HMMState currState = statelist[currStateIndex];
				float prob = viterbi[ngramIndex][currStateIndex] + adjacencyMatrix[currStateIndex][maxStateIndex] + probEmission;
				// check if found
				if(prob == max)
				{
					// add this state's tags to output list and set this state as next "maxState"
					tags.add(0, currState.firstTag);	// only get first tag, because tags are overlapping
					max = viterbi[ngramIndex][currStateIndex];
					maxStateIndex = currStateIndex;
					break;
				}
			}
		}
		
		// handle too many tags, if last nGram is overlapping with ngram before => delete from last ngram until sizes match
		if(tokens.size() < tags.size())
			System.out.println("remove superflous tags");
		while(tokens.size() < tags.size())
		{
			// always remove first tag of last tag-ngram (which are the overlapping, redundant tags) until sizes match
			tags.remove(tags.size() - ngram_length);
		}
		
		return new Vector<String>(tags);
	}
	
	// chunks tokens and tags to groups of n. tags can be null, if no tags exist
	public NGrams createNGramsFromTokens(Vector<String> tokens, Vector<String> tags, int n)
	{
		//return createNonOverlappingNGramsFromTokens(tokens, tags, n);
		return createOverlappingNGramsFromTokens(tokens, tags, n);
	}
	
	public void serialize(String fileName) throws IOException
	{
		System.out.print("serializing...");
		FileOutputStream fout = new FileOutputStream(fileName);
		ObjectOutputStream out = new ObjectOutputStream(fout);
		out.writeObject(taglist);
		out.writeObject(adjacencyMatrix);
		out.writeObject(statelist);
		fout.close();
		System.out.println("done.");
	}
	
	private void deserialize(String fileName) throws IOException, ClassNotFoundException
	{
		// try using: FileInputStream fin = new FileInputStream(fileName); FileChannel ch = fin.getChannel(); ch.getMap();   for speed (mem-mapped files, only better for big files > 1M or so)
		System.out.print("deserializing...");
		FileInputStream fin = new FileInputStream(fileName);
		ObjectInputStream in = new ObjectInputStream(fin);
		taglist = (String[])in.readObject();
		adjacencyMatrix = (float[][])in.readObject();
		statelist = (HMMState[])in.readObject();
		fin.close();
		System.out.println("done.");
	}
	
	/*
	public void printGraph()
	{
		if(statelist == null || statelist.length==0)
		{
			System.out.println("[HMM is empty]");
			return;
		}

		for(String key:taglist) System.out.println(key);
		// adjacency matrix
		double[][] adj = new double[taglist.length][taglist.length];
		for(int i = 0; i < taglist.length; i++)
		{
			HMMState state = statelist[i];
			for(int toState = 0; toState < state.outgoing.length; toState++) {
				int outgoingIndex = Arrays.binarySearch(state.outgoingIndexByTagIndex, toState);
				if(outgoingIndex < 0)
					continue;
				HMMEdge edge = state.outgoing[outgoingIndex];
				Double weight = new Double(edge.probability);
				String toStateString = taglist[toState];
				// look for index j in graphKeysOrdered with graphKeysOrdered[j].equals(toStateStr)
				int toStateIndex = -1;
				for(int j = 0; j < taglist.length; j++)
				{
					if(taglist[j].equals(toStateString))
					{
						toStateIndex = j;
						break;
					}
				}
				adj[i][toStateIndex] = weight;
			}
		}
		print2dArray(adj);
	}
	*/
	
	/*
	private NGrams createNonOverlappingNGramsFromTokens(Vector<String> tokens, Vector<String> tags, int n)
	{
		NGrams ret = new NGrams(tags != null);
		Vector<String> ngramTokens = null;
		Vector<String> ngramTags = null;
		
		for(int i=0; i<tokens.size(); i+=n)
		{
			// create current ngram
			ngramTokens = new Vector<String>(n);
			if(tags != null)
				ngramTags = new Vector<String>(n);
			int startOffset = 0;	// offset to the left, if i+nGramLength would be out of range (end of token list)
			if(i+n-1 >= tokens.size())
				startOffset = tokens.size() - i - n;	// is negative
			//System.out.println("i: " + i + "  startOffset: " + startOffset);
			for(int j = startOffset; j < n+startOffset; j++)
			{
				ngramTokens.add(tokens.get(i+j));
				if(ngramTags != null)
					ngramTags.add(tags.get(i+j));
			}
			//TODO: maybe stop adding to ngram if senetence ends?
			ret.tokens.add(ngramTokens);
			if(ngramTags != null)
				ret.tags.add(ngramTags);
		}
		return ret;
	}
	*/
	
	private static NGrams createOverlappingNGramsFromTokens(Vector<String> tokens, Vector<String> tags, int n)
	{
		NGrams ret = new NGrams(tags != null);
		Vector<String> ngramTokens = null; //, ngramEndTokens = null;
		Vector<String> ngramTags = null;
		String[] ngramTokensJoined = new String[tokens.size() - n + 1];
		/*String[] ngramEndTokensJoined = new String[tokens.size() - n + 1];

		int ending_length = assignment5.ending_length;*/
		
		for(int i=0; i<=tokens.size() - n; i++)
		{
			// create current ngram
			ngramTokens = new Vector<String>(n);
			if(tags != null)
				ngramTags = new Vector<String>(n);
			for(int j = 0; j < n; j++)
			{
				ngramTokens.add(tokens.get(i+j));
				if(ngramTags != null)
					ngramTags.add(tags.get(i+j));
			}
			ret.tokens.add(ngramTokens);
			ngramTokensJoined[i] = assignment5.join(ngramTokens, " ");
			if(ngramTags != null)
				ret.tags.add(ngramTags);
			
			//create ending ngram
			/*ngramEndTokens = new Vector<String>(n);
			for(int j = 0; j < n; j++)
			{
				String token = tokens.get(i+j);
				if (token.length() >= ending_length*2){
					ngramEndTokens.add(token.substring(token.length() - ending_length));
				} else {
					ngramEndTokens.add(" ");
				}
			}
			ret.endTokens.add(ngramEndTokens);
			ngramEndTokensJoined[i] = assignment5.join(ngramEndTokens, " ");*/
		}
		ret.tokensJoined = ngramTokensJoined;
		//ret.endTokensJoined = ngramEndTokensJoined;
		return ret;
	}
	
	/*
	private void print2dArray(double[][] table)
	{
		for (int i = 0; i < table.length; i++)
		{
			for (int j = 0; j < table[i].length; j++)
			{
				System.out.print(table[i][j] + "\t");
			}
			System.out.println();
		}
	}
	*/
}





