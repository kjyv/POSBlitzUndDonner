
public class HMMEdge {
	double probability;
	HMMState state;
	
	public HMMEdge(HMMState state, double probability){
		this.state = state;
		this.probability = probability;
	}
}
