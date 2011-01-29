import java.util.HashMap;
import java.util.Vector;

public class HMMState
{
	Vector<String> tags;
	HashMap<Integer, HMMEdge> outgoing_map;  //for training code
	HashMap<Vector<String>, Double> probabilities;
	
	//duplicate primitives for quicker decoding 
	String[] seenTokens;
	float[] seenTokenEmissionProbabilities;
	int tagindex;
	//HMMEdge[] outgoing;
	//int[] outgoingIndexByTagIndex;
	
	public HMMState(){
		outgoing_map = new HashMap<Integer, HMMEdge>();
		probabilities = new HashMap<Vector<String>, Double>();
		tags = new Vector<String>();
		tagindex = -1;
	}
}
