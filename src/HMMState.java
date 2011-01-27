import java.util.HashMap;
import java.util.Vector;

public class HMMState
{
	Vector<String> tags;
	HashMap<Integer, HMMEdge> outgoing;
	HashMap<Vector<String>, Double> probabilities;
	int tagindex;
	
	public HMMState(){
		outgoing = new HashMap<Integer, HMMEdge>();	
		probabilities = new HashMap<Vector<String>, Double>();
		tags = new Vector<String>();
		tagindex = -1;
	}
}
