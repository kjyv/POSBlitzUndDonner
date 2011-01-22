import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

class assignment5
{
	static int ngram_length = 3;
	
	public static void main(String[] args)
	{
		try {
			CrossValidator cv = new CrossValidator("../assignment5/brown_learn");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String join(Collection<String> s, String delimiter)
	{
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }
}