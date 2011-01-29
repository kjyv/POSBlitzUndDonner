import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NonAnnotatedStringParser
{
	private Vector<String> tokens;
	private Pattern p;
	private Matcher m;
	
	public NonAnnotatedStringParser()
	{
		init();
	}
	
	public NonAnnotatedStringParser(String strToParse)
	{
		init();
		parse(strToParse);
	}
	
	public void parse(String strToParse)
	{
		m = p.matcher(strToParse);
		while(m.find())
		{
			//System.out.println(m.group(1) + " : " + m.group(2));
			tokens.add(m.group(1));
		}
	}
	
	public Vector<String>getTokens() { return tokens; }
	
	private void init()
	{
		tokens = new Vector<String>();
		p = Pattern.compile("(\\S+)");	// tokens separated by whitespaces..?
	}
}


