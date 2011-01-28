
public class HMMEdge {
	float probability;
	HMMState state;
	
	public HMMEdge(HMMState state, float probability){
		this.state = state;
		this.probability = probability;
	}
}
