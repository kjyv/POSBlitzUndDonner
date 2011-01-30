import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;

public class HMMState implements Serializable
{
	private static final long serialVersionUID = -6545044048487657632L;
	String[] tags;
	String firstTag;
	HashMap<Integer, HMMEdge> outgoing_map;  //for training code
	HashMap<Vector<String>, Double> probabilities;
	HashMap<Vector<String>, Double> ending_probabilities;
	
	//duplicate primitives for quicker decoding 
	String[] seenTokens;
	float[] seenTokenEmissionProbabilities;
	String[] seenEndTokens;
	float[] seenEndTokenEmissionProbabilities;
	int tagindex;
	//HMMEdge[] outgoing;
	//int[] outgoingIndexByTagIndex;
	
	public HMMState(){
		outgoing_map = new HashMap<Integer, HMMEdge>();
		probabilities = new HashMap<Vector<String>, Double>();
		ending_probabilities = new HashMap<Vector<String>, Double>();
		tagindex = -1;
	}
}
