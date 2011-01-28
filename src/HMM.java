import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

class HMM
{
	HMMState[] statelist;
	String[] taglist;
	
	final double missingTokenEmissionProbability = -10.0;
	final double missingEdgeTransitionProbability = -10.0;
	
	public HMM(){
	}
	
	public void train(Vector<String> tokens, Vector<String> tags)
	{
		// TODO: smoothing
		//System.out.println("training");
		
		//tag -> state mapping for finding what we already have
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
		statelist = new HMMState[taglist.length];
		
		for(int ngramIndex=0; ngramIndex<ngrams.tokens.size(); ngramIndex++)
		{
			Vector<String> ngram_tokens = ngrams.tokens.get(ngramIndex);
			Vector<String> ngram_tags = ngrams.tags.get(ngramIndex);
			String ngram_tags_joined = assignment5.join(ngram_tags, " ");
			HMMState state = graph.get(ngram_tags_joined);
			
			if(state == null) {
				//create new state
				//System.out.println("state #"+ngram_tags_joined +"# not found");
				state = new HMMState();
				state.tags = new Vector<String>(ngram_tags);

				state.probabilities = new HashMap<Vector<String>, Double>();
				graph.put(ngram_tags_joined, state);
			}
			else
			{
				//System.out.println("state #"+ngram_tags_joined+"# found");
			}

			//System.out.println("keyset before");
			//for(Vector<String> tmp : state.probabilities.keySet())
			//{
			//	System.out.print(tmp + " ");
			//}
			//System.out.println();
			//add absolute probability
			Double prob = state.probabilities.get(ngram_tokens);
			Integer currentStateIndex = Arrays.binarySearch(taglist, ngram_tags_joined); 
			if(prob != null){
				// prob++ does not work
				state.probabilities.put(new Vector<String>(ngram_tokens), prob+1);
			} else {
				state.probabilities.put(new Vector<String>(ngram_tokens), new Double(1));
			}
			//System.out.println("keyset after");
			//for(Vector<String> tmp : state.probabilities.keySet())
			//{
			//	System.out.print(tmp + " ");
			//}
			//System.out.println();

			//add or update edge to this state
			if(lastState != null){
				HMMEdge edge = lastState.outgoingm.get(currentStateIndex);
				if (edge != null){
					edge.probability++;
				} else {
					lastState.outgoingm.put(currentStateIndex, new HMMEdge(state, 1));
				}
			}
			
			lastState = state;
		}
		
		//normalize probabilities
		System.out.println("normalizing");
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
				state.probabilities.put(p_key, p);
			}

			//get sum of all edges
			int sum_edge_probs = 0;
			for(Integer e_key: state.outgoingm.keySet()){
				HMMEdge edge = state.outgoingm.get(e_key);
				sum_edge_probs += (int)edge.probability;
			}

			//normalize edges
			for(Integer e_key: state.outgoingm.keySet()){
				HMMEdge edge = state.outgoingm.get(e_key);
				edge.probability /= sum_edge_probs;
			}
		}
		
		//finished training, now generate static data		
		//set index for each state
		for (int tag = 0; tag < taglist.length; tag++) {
			HMMState currState = graph.get(taglist[tag]);
			currState.tagindex = tag;
			String[] emittedTokens = new String[currState.probabilities.size()];
			double[] emissionProbs = new double[emittedTokens.length];
			int counter = 0;
			
			for(Entry<Vector<String>, Double> emissionEntry : currState.probabilities.entrySet())
			{
				emittedTokens[counter++] = assignment5.join(emissionEntry.getKey(), " ");
			}
			Arrays.sort(emittedTokens);
			counter = 0;
			for(String emittedToken : emittedTokens)
			{
				Vector<String> vec = new Vector<String>(Arrays.asList(emittedToken.split(" ")));
				emissionProbs[counter++] = currState.probabilities.get(vec);
			}
			currState.seenTokens = emittedTokens;
			currState.seenTokenEmissionProbabilities = emissionProbs;
						
			//create outgoing arrays
			Integer[] keys = (Integer[])currState.outgoingm.keySet().toArray(new Integer[0]);
			Arrays.sort(keys);

			currState.outgoing = new HMMEdge[keys.length];
			currState.outgoingTags = new int[keys.length];
			for (int edgeIndex = 0; edgeIndex < currState.outgoingm.size(); edgeIndex++){
				currState.outgoing[edgeIndex] = currState.outgoingm.get(keys[edgeIndex]);
				currState.outgoingTags[edgeIndex] = keys[edgeIndex];
			}				
		}
		
		//create array from graph
		for(int tag = 0; tag < taglist.length; tag++){
			statelist[tag] = graph.get(taglist[tag]);
		}
	}
	
	public Vector<String> decode(Vector<String> tokens)
	{
		//System.out.println("decoding");
		int ngram_length = assignment5.ngram_length;
		NGrams ngrams = createNGramsFromTokens(tokens, null, ngram_length);
		int numStates = statelist.length
			, numNGrams = ngrams.tokens.size();
		// states' order is defined by graph.keySet()
		String[] graphKeysOrdered = taglist;
		double[][] viterbi = new double[numStates][numNGrams];
		// first column (ngram): no transition probabilities, no previous probabilites => only emission probs
		for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
		{
			HMMState currState = statelist[currStateIndex];
			int emissionTokenIndex = Arrays.binarySearch(
										currState.seenTokens,
										assignment5.join(ngrams.tokens.get(0), " ")
										);
			double prob;
			if(emissionTokenIndex < 0)
				prob = missingTokenEmissionProbability;
			else
				prob = Math.log(currState.seenTokenEmissionProbabilities[emissionTokenIndex]);
			/*Double prob = graph.get(graphKeysOrdered[currStateIndex])
							.probabilities.get(ngrams.tokens.get(0));
			// TODO: should not occur if smoothing was applied after learning
			if(prob == null || prob == 0.0)
			{
				prob = missingTokenEmissionProbability;
			}
			else
			{
				System.out.println("col 0, EmissionFound: log(" + prob + ") in state " + graphKeysOrdered[currStateIndex] + ", for tokens " + ngrams.tokens.get(0));
				prob = Math.log(prob);
			}
			*/
			viterbi[currStateIndex][0] = prob;
		}
		
		for(int ngramIndex=1; ngramIndex<numNGrams; ngramIndex++)
		{
			System.out.println("decoding: column " + ngramIndex + " / " + numNGrams);
			Vector<String> ngram_tokens = ngrams.tokens.get(ngramIndex);
			for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
			{
				HMMState currState = statelist[currStateIndex];
				
				int emissionTokenIndex = Arrays.binarySearch(
						currState.seenTokens,
						assignment5.join(ngram_tokens, " ")
						);
				double probEmission;
				if(emissionTokenIndex < 0)
					probEmission = missingTokenEmissionProbability;
				else
				{
					probEmission = Math.log(currState.seenTokenEmissionProbabilities[emissionTokenIndex]);
					System.out.println("col "+ngramIndex+", EmissionFound: log(" + probEmission + ") in state #"+currStateIndex+" " + graphKeysOrdered[currStateIndex] + ", for tokens " + ngram_tokens);
				}
				
				/*
				Double probEmission = currState.probabilities.get(ngram_tokens);
				// TODO: should not occur, when smoothing was applied after learning
				if(probEmission == null || probEmission == 0.0)
					probEmission = missingTokenEmissionProbability;
				else
				{
					System.out.println("col "+ngramIndex+", EmissionFound: log(" + probEmission + ") in state #"+currStateIndex+" " + graphKeysOrdered[currStateIndex] + ", for tokens " + ngram_tokens);
					probEmission = Math.log(probEmission);
				}
				*/
				double maxProb = -Double.MAX_VALUE;
				// determine maximum probability to reach currState (from any previous state)
				double transitionProb;
				for(int previousStateIndex = 0; previousStateIndex < numStates; previousStateIndex++)
				{
					HMMState prevState = statelist[previousStateIndex];
					int outgoingIndex = Arrays.binarySearch(prevState.outgoingTags, currState.tagindex);
					
					// TODO: should not occur, when smoothing was applied after learning
					if(outgoingIndex < 0)
						transitionProb = 0.0;
					else {
						HMMEdge edge = prevState.outgoing[outgoingIndex];
						transitionProb = edge.probability;
					}
					// TODO: should not occur, when smoothing was applied after learning
					if(transitionProb == 0.0)
						transitionProb = missingEdgeTransitionProbability;
					else
					{
						transitionProb = Math.log(transitionProb);
					}
					double prob = viterbi[previousStateIndex][ngramIndex-1] + transitionProb;
					if(prob > maxProb)
						maxProb = prob;
				}
				viterbi[currStateIndex][ngramIndex] = maxProb + probEmission;
			}
		}
		
		System.out.println("viterbi table");
		print2dArray(viterbi);
		
		// backtracking to find the optimal (most probable) path
		LinkedList<String> tags = new LinkedList<String>();	// to be able to prepend items efficiently
		// determine maximum of last column to use as initial state for backtracking
		double max = -Double.MAX_VALUE;
		int maxStateIndex = -1;
		for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
		{
			if(viterbi[currStateIndex][numNGrams-1] > max)
			{
				max = viterbi[currStateIndex][numNGrams-1];
				maxStateIndex = currStateIndex;
			}
		}
		
		tags.addAll(0,statelist[maxStateIndex].tags);	// prepends tags
		
		for(int ngramIndex=numNGrams-2; ngramIndex>=0; ngramIndex--)	// go backwards in time
		{
			System.out.println("decoding: backtracking: column " + ngramIndex);
			HMMState maxState = statelist[maxStateIndex];
			Vector<String> ngram_tokens = ngrams.tokens.get(ngramIndex+1);	// from "next" timestep
			//Double probEmission = maxState.probabilities.get(ngram_tokens);
			
			// TODO outsource ... or replace with hashing
			int emissionTokenIndex = Arrays.binarySearch(
					maxState.seenTokens,
					assignment5.join(ngram_tokens, " ")
					);
			double probEmission;
			if(emissionTokenIndex < 0)
				probEmission = missingTokenEmissionProbability;
			else
			{
				probEmission = Math.log(maxState.seenTokenEmissionProbabilities[emissionTokenIndex]);
			}
			
			/*
			if(probEmission == null || probEmission == 0.0)
				probEmission = missingTokenEmissionProbability;
			else
			{
				probEmission = Math.log(probEmission);
			}
			*/
			// loop all states and find the one that transitioned to maxState
			for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
			{
				HMMState currState = statelist[currStateIndex];
				int outgoingIndex = Arrays.binarySearch(currState.outgoingTags, currState.tagindex);

				Double transitionProb;
				if(outgoingIndex < 0){
					transitionProb = 0.0;
				} else {
					HMMEdge edge = currState.outgoing[outgoingIndex];
					transitionProb = edge.probability;
				}
				// TODO: should not occur, when smoothing was applied after learning
				if(transitionProb == null || transitionProb == 0.0)
					transitionProb = missingEdgeTransitionProbability;
				else
					transitionProb = Math.log(transitionProb);
				double prob = viterbi[currStateIndex][ngramIndex] + transitionProb + probEmission;
				// check if found
				if(prob == max)
				{
					// add this state's tags to output list and set this state as next "maxState"
					tags.add(0, currState.tags.get(0));
					//tags.addAll(0, currState.tags);
					max = viterbi[currStateIndex][ngramIndex];
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
	
	public void serialize(){}
	
	public void printGraph()
	{
		if(statelist == null || statelist.length==0)
		{
			System.out.println("[HMM is empty]");
			return;
		}
		String[] graphKeysOrdered = taglist;
		Arrays.sort(graphKeysOrdered);
		for(String key:graphKeysOrdered) System.out.println(key);
		// adjacency matrix
		double[][] adj = new double[graphKeysOrdered.length][graphKeysOrdered.length];
		for(int i = 0; i < graphKeysOrdered.length; i++)
		{
			HMMState state = statelist[i];
			for(int toState = 0; toState < state.outgoing.length; toState++) {
				int outgoingIndex = Arrays.binarySearch(state.outgoingTags, toState);
				if(outgoingIndex < 0)
					continue;
				HMMEdge edge = state.outgoing[outgoingIndex];
				Double weight = edge.probability;
				String toStateString = taglist[toState];
				// look for index j in graphKeysOrdered with graphKeysOrdered[j].equals(toStateStr)
				int toStateIndex = -1;
				for(int j = 0; j < graphKeysOrdered.length; j++)
				{
					if(graphKeysOrdered[j].equals(toStateString))
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
			//TODO: maybe stop adding to ngram if senetence end?
			ret.tokens.add(ngramTokens);
			if(ngramTags != null)
				ret.tags.add(ngramTags);
		}
		return ret;
	}
	
	private static NGrams createOverlappingNGramsFromTokens(Vector<String> tokens, Vector<String> tags, int n)
	{
		NGrams ret = new NGrams(tags != null);
		Vector<String> ngramTokens = null;
		Vector<String> ngramTags = null;
		
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
			if(ngramTags != null)
				ret.tags.add(ngramTags);
		}
		return ret;
	}
	
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
}





