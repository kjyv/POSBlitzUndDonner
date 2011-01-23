import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

class HMM
{	
	HashMap<String, HMMState> graph;
	
	public HMM(){
	
		//tag -> state mapping for finding what we already have
		graph = new HashMap<String, HMMState>(); 
	}
	
	public void train(Vector<String> tokens, Vector<String> tags)
	{
		// TODO: smoothing
		
		//create state graph from given data
		int ngram_length = assignment5.ngram_length;
		HMMState lastState = null;
		NGrams ngrams = createNGramsFromTokens(tokens, tags, ngram_length);
		
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
				HMMEdge edge = lastState.outgoing.get(state.tags);
				if (edge != null){
					edge.probability++;
				} else {
					lastState.outgoing.put(state.tags, new HMMEdge(state, 1));
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
			for(Vector<String> e_key: state.outgoing.keySet()){
				HMMEdge edge = state.outgoing.get(e_key);
				sum_edge_probs += (int)edge.probability;
			}

			//normalize edges
			for(Vector<String> e_key: state.outgoing.keySet()){
				HMMEdge edge = state.outgoing.get(e_key);
				edge.probability /= sum_edge_probs;
			}
		}
	}
	
	public Vector<String> decode(Vector<String> tokens)
	{
		//System.out.println("decoding");
		int ngram_length = assignment5.ngram_length;
		NGrams ngrams = createNGramsFromTokens(tokens, null, ngram_length);
		int numStates = graph.size()
			, numNGrams = ngrams.tokens.size();
		// states' order is defined by graph.keySet()
		String[] graphKeysOrdered = graph.keySet().toArray(new String[0]);
		double[][] viterbi = new double[numStates][numNGrams];
		// first column (ngram): no transition probabilities, no previous probabilites => only emission probs
		for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
		{
			Double prob = graph.get(graphKeysOrdered[currStateIndex])
							.probabilities.get(ngrams.tokens.get(0));
			// TODO: should not occur if smoothing was applied after learning
			if(prob == null)
				prob = 0.0;
			viterbi[currStateIndex][0] = prob;
		}
		
		for(int ngramIndex=1; ngramIndex<numNGrams; ngramIndex++)
		{
			System.out.println("decoding: column " + ngramIndex + " / " + numNGrams);
			Vector<String> ngram_tokens = ngrams.tokens.get(ngramIndex);
			for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
			{
				HMMState currState = graph.get(graphKeysOrdered[currStateIndex]);
				Double probEmission = currState.probabilities.get(ngram_tokens);
				// TODO: should not occur, when smoothing was applied after learning
				if(probEmission == null || probEmission == 0.0)
					probEmission = 0.0;
				else
					probEmission = Math.log(probEmission);
				double maxProb = 0;
				// determine maximum probability to reach currState (from any previous state)
				Double transitionProb;
				for(int previousStateIndex = 0; previousStateIndex < numStates; previousStateIndex++)
				{
					HMMState prevState = graph.get(graphKeysOrdered[previousStateIndex]);
					HMMEdge edge = prevState.outgoing.get(currState.tags);
					// TODO: should not occur, when smoothing was applied after learning
					if(edge == null)
						transitionProb = 0.0;
					else
						transitionProb = edge.probability;
					// TODO: should not occur, when smoothing was applied after learning
					if(transitionProb == null || transitionProb == 0.0)
						transitionProb = 0.0;
					else
						transitionProb = Math.log(transitionProb);
					double prob = viterbi[previousStateIndex][ngramIndex-1] + transitionProb;
					if(prob > maxProb)
						maxProb = prob;
				}
				viterbi[currStateIndex][ngramIndex] = maxProb + probEmission;
			}
		}
		
		//System.out.println(Arrays.deepToString(viterbi));
		
		// backtracking to find the optimal (most probable) path
		LinkedList<String> tags = new LinkedList<String>();	// to be able to prepend items efficiently
		// determine maximum of last column to use as initial state for backtracking
		double max = -1;
		int maxStateIndex = -1;
		for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
		{
			if(viterbi[currStateIndex][numNGrams-1] > max)
			{
				max = viterbi[currStateIndex][numNGrams-1];
				maxStateIndex = currStateIndex;
			}
		}
		
		tags.addAll(0,graph.get(graphKeysOrdered[maxStateIndex]).tags);	// prepends tags
		
		for(int ngramIndex=numNGrams-2; ngramIndex>=0; ngramIndex--)	// go backwards in time
		{
			System.out.println("decoding: backtracking: column " + ngramIndex);
			HMMState maxState = graph.get(graphKeysOrdered[maxStateIndex]);
			Vector<String> ngram_tokens = ngrams.tokens.get(ngramIndex+1);	// from "next" timestep
			Double probEmission = maxState.probabilities.get(ngram_tokens);
			// TODO: should not occur, when smoothing was applied after learning
			if(probEmission == null || probEmission == 0.0)
				probEmission = 0.0;
			else
				probEmission = Math.log(probEmission);
			// loop all states and find the one that transitioned to maxState
			for(int currStateIndex = 0; currStateIndex < numStates; currStateIndex++)
			{
				HMMState currState = graph.get(graphKeysOrdered[currStateIndex]);
				HMMEdge edge = currState.outgoing.get(maxState.tags);
				Double transitionProb;
				if(edge == null)
					transitionProb = 0.0;
				else
					transitionProb = edge.probability;
				// TODO: should not occur, when smoothing was applied after learning
				if(transitionProb == null || transitionProb == 0.0)
					transitionProb = 0.0;
				else
					transitionProb = Math.log(transitionProb);
				double prob = viterbi[currStateIndex][ngramIndex] + transitionProb + probEmission;
				// check if found
				if(prob == max)
				{
					// add this state's tags to output list and set this state as next "maxState"
					tags.addAll(0, currState.tags);
					max = viterbi[currStateIndex][ngramIndex];
					maxStateIndex = currStateIndex;
					break;
				}
			}
		}
		
		// handle too many tags, if last nGram is overlapping with ngram before => delete from last ngram until sizes match
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
	
	public void serialize(){}
}





