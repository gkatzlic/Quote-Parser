
public class MatchedQuote {
	
	String _source;
	String _pattern;
	String _quote;
	
	public MatchedQuote(String src_, String pattern_, String quote_){
	 _source = src_;
	 _pattern = pattern_;
	 _quote = quote_;
	}
	
	public String getSource(){return _source;}
	public String getPattern(){return _pattern;}
	public void setPattern(String s){_pattern = s;}
	public String getQuote(){return _quote;}
}
