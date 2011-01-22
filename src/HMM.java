import java.util.HashMap;
import java.util.List;
import java.util.Vector;

class HMM
{	
	HashMap<String, HMMState> graph;
	
	public HMM(){
	
		//tag -> state mapping for finding what we already have
		graph = new HashMap<String, HMMState>(); 
	}
	
	public void train(Vector<String> tokens, Vector<String> tags){
		//create state graph from given data
		int n = assignment5.ngram_length;
		Vector<String> ngram = new Vector<String>(n);
		HMMState lastState = null;
		
		for(int i=0; i<tokens.size(); i+=n)
		{
			// create current ngram
			ngram.clear();
			int startOffset = 0;	// offset to the left, if i+nGramLength would be out of range (end of token list)
			if(i+n-1 >= tokens.size())
				startOffset = tokens.size() - i - n;	// is negative
			//System.out.println("i: " + i + "  startOffset: " + startOffset);
			for(int j = startOffset; j < n+startOffset; j++)
			{
				ngram.add(tokens.get(i+j));
			}
			//TODO: maybe stop adding to ngram if senetence end?
			//create a new state or add to existing
			List<String> ngram_tags = tags.subList(i+startOffset, i+n+startOffset);
			HMMState state = graph.get(assignment5.join(ngram_tags, " "));
			
			if(state == null) {
				//System.out.println("state #"+assignment5.join(ngram_tags, " ") +"# not found");
				//create new state
				state = new HMMState();
				state.tags = new Vector<String>(ngram_tags);

				state.probabilities = new HashMap<Vector<String>, Double>();
				graph.put(assignment5.join(state.tags, " "), state);
			}
			else
			{
				//System.out.println("state #"+assignment5.join(ngram_tags, " ") +"# found");
			}

			//System.out.println("keyset before");
			//for(Vector<String> tmp : state.probabilities.keySet())
			//{
			//	System.out.print(tmp + " ");
			//}
			//System.out.println();
			//add absolute probability
			Double prob = state.probabilities.get(ngram);
			if(prob != null){
				// prob++ does not work
				state.probabilities.put(new Vector<String>(ngram), prob+1);
			} else {
				state.probabilities.put(new Vector<String>(ngram), new Double(1));
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
			//System.out.println("state " + assignment5.join(state.tags, "#") +  " has " +total_emissions + " emissions, keysetSize = " + state.probabilities.keySet().size());
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
	
	public Vector<String> decode(Vector<String> tokens){return null;}
	public void serialize(){}
}