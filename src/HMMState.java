import java.util.HashMap;
import java.util.Vector;

public class HMMState {
	Vector<String> tags;
	HashMap<Vector<String>, HMMEdge> outgoing;
	HashMap<String[], Double> probabilities;
	
	public HMMState(){
		outgoing = new HashMap<Vector<String>, HMMEdge>();	
		probabilities = new HashMap<String[], Double>();
		tags = new Vector<String>();
	}
}
