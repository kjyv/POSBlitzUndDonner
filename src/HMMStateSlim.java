import java.util.List;

// like HMMState, but stripped off baggage. To be used only for decoding
public class HMMStateSlim
{
	List<String> tags;
	
	//duplicate primitives for quicker decoding 
	String[] seenTokens;
	float[] seenTokenEmissionProbabilities;
	int tagindex;
}