import java.util.HashMap;
import java.util.Vector;

public class HMMState
{
	Vector<String> tags;
	HashMap<Integer, HMMEdge> outgoingm;  //for training code
	HashMap<Vector<String>, Double> probabilities;
	
	//duplicate primitives for quicker decoding 
	String[] seenTokens;
	double[] seenTokenEmissionProbabilities;
	int tagindex;
	HMMEdge[] outgoing;
	int[] outgoingTags;
	
	public HMMState(){
		outgoingm = new HashMap<Integer, HMMEdge>();
		probabilities = new HashMap<Vector<String>, Double>();
		tags = new Vector<String>();
		tagindex = -1;
	}
}
