import java.util.Vector;

class NGrams
{
	Vector<Vector<String>> tokens = null;	// Vector of token ngrams
	String[] tokensJoined = null;			// each element of tokens (that is, a Vector<String, ) was joined by a blank
	Vector<Vector<String>> tags = null;		// Vector of tags ngrams, if exist
	public NGrams(boolean hasTags)
	{
		tokens = new Vector<Vector<String>>();
		if(hasTags)
			tags = new Vector<Vector<String>>();
	}
}