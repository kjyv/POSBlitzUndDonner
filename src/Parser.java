import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Parser
{
	private Vector<String> tokens, tags;
	private Pattern p;
	private Matcher m;
	
	public Parser()
	{
		init();
	}
	
	public Parser(String strToParse)
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
			tags.add(m.group(2));
		}
	}
	
	public Vector<String>getTokens() { return tokens; }
	public Vector<String>getTags() { return tags; }
	
	private void init()
	{
		tokens = new Vector<String>();
		tags = new Vector<String>();
		p = Pattern.compile("(\\S+)/([^\\-+*/\\s]+)([\\-+][^/\\s]+)?\\s");	// token/tag(-suffix)?
	}
}


