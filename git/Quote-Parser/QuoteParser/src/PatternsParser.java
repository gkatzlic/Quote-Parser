import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

import java.util.regex.*;
import java.nio.charset.*;
import org.apache.log4j.xml.*;
import joquery.core.QueryException;
import joquery.*;

/**
 * 
 * @author gkatz
 *
 */
public class PatternsParser{
	
	private static String SOURCE_DIR;
	
	static Logger logger = Logger.getLogger(PatternsParser.class.getName());
	
	public static void main(String args[])
	{
		DOMConfigurator.configure("log4j.xml");
		usage(args);
		new PatternsParser(args[0],args[1],args[2],args[3]);
		
	}
	
	private static void usage(String[] args)
	{
		if(args.length<4)
		{
			System.err.println("PatternsParser <patternFile> <textFile> <outputFile> <sourceDir>");
			System.exit(1);
		}else
		{
			log("Patterns File " + args[0] + "\n text Directory: " + args[1] +
			"\n output file: " + args[2] + "\n source directory: " + args[3]);
		}
	}
	

	public PatternsParser(String patternFile_,String textDir_
			,String outputFile_,String sourceDir_)
	{
		
		try{
			log("started with pattern: " +patternFile_ + ", input dir: " +textDir_);		
			SOURCE_DIR=sourceDir_;
			List<String> quotePatterns = readFile(SOURCE_DIR + "/QuoteRegex.txt");
			List<String> lookupPatterns = readFile(patternFile_);		
			Map<String,List<String>> mapTextLines = readAllFiles(textDir_);
		
			Map<String,String> allQuotesMap =new HashMap<String,String>();
			
			
			for (Map.Entry<String, List<String>> entry : mapTextLines.entrySet()) {					
				List<String> quotes = applyGroupRegex(entry.getValue(),quotePatterns);
				for(String quote : quotes){
					allQuotesMap.put(entry.getKey(),quote);
				}
				}
			
			Set<MatchedQuote> matchResults = applyMatchRegex(allQuotesMap,lookupPatterns);
			log("number of lookup matches found is: " + matchResults.size());
			
			reportResults(outputFile_,matchResults);
			
			}catch(Exception ex)
			{
				ex.printStackTrace();
			}
	}
	
	
	
	
	
		
	void reportResults(String reportFile,Set<MatchedQuote> quoteSet)
	throws IOException, QueryException{
		log("in reportResults for " + reportFile);
		File file = new File(reportFile);
		
		if(file.exists() && !file.isDirectory())
		{
			file.delete();
		}
		
		try (Writer writer = new 
		BufferedWriter(new OutputStreamWriter(new 
		    FileOutputStream(reportFile), StandardCharsets.UTF_8))) 
		{
			String html = GetReportHtmlTable(quoteSet);
			writer.write(html);
} catch (IOException ex) {
   ex.printStackTrace();
}  
	}
	
	//Read either single file or all files in directory
	 Map<String,List<String>> readAllFiles(String loc) throws IOException
	{
		log("in readAllFiles for " + loc);
		 Map<String,List<String>> results = new HashMap<String,List<String>>();
		File file = new File(loc);
		
		if(!file.isDirectory()){
			List<String> lines = readFile(loc);
			
			lines = replaceSpecialChars(lines);
			results.put(loc,lines);
		}else{
			String[] files = file.list(FILTER);
			for(String fileName : files)
			{
				Map<String,List<String>> dirMap = readAllFiles(loc + "/" + fileName);
				results.putAll(dirMap);
			}
		}
		
		return results;
	}
	
	 FilenameFilter FILTER = new FilenameFilter(){
		 public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".txt");
        }
	};
	
	 String GetReportHtmlTable(Set<MatchedQuote> quoteSet) throws IOException, QueryException{
		StringBuilder sb = new StringBuilder();
		String header = readString(SOURCE_DIR+"/HtmlHeader.txt");
		sb.append(header);
		
		sb.append("<h1>Quote Occurance Count Report</h1>");
		
		String countTable = GetCountTable(quoteSet);
		
		sb.append(countTable);
		sb.append("<br>\n");
		sb.append("<table>\n");
		sb.append("<tr><th>Source File/Link</th><th>Pattern</th><th>Quote</th></tr>\n");
		for (MatchedQuote quote : quoteSet) {
			
		 	sb.append("<tr><td>" +  quote.getSource() 
		 			+ "</td><td>" + quote.getPattern() + "</td><td>" + quote.getQuote()
		 			+ "</td></tr>\n");
					}
		sb.append("</table>\n");
		
		
		return sb.toString();
	}
	
	  String GetCountTable(Set<MatchedQuote> quoteSet) throws QueryException
	 {
		 StringBuilder sb = new StringBuilder();
		
		 GroupQuery<Integer,MatchedQuote> query = 
				 CQ.<MatchedQuote,MatchedQuote>query(quoteSet)
				    .group();
		 query = query.groupBy("pattern");
		
		 
				  
		Collection<Grouping<Integer,MatchedQuote>> grouped = query.list();
		 sb.append("<table>\n");
		 sb.append("<tr><th>Pattern</th><th>Count</th></tr>\n");
			
		 for(Grouping<Integer, MatchedQuote>  group : grouped){
			 sb.append("<tr><td>" +  group.getKey() 
			 			+ "</td><td>" + group.getValues().size() + "</td>"
			 					+ "</tr>\n");
		 }
		 sb.append("</table>\n");
		 return sb.toString();
	 }

	
	private Set<MatchedQuote> applyMatchRegex(Map<String,String> lines
	, List<String> regexList)
	{
		log("in applyMatchRegex for " + lines.size() + " lines and " + regexList.size() + " expressions");
		Set<MatchedQuote> results = new HashSet<MatchedQuote>();
		for(String regex : regexList)
		{
			log("regex is: " + regex);
			Pattern pattern = Pattern.compile(regex);		
			for(Map.Entry<String, String> lineEntry : lines.entrySet())
			{
					log("line to match is: " +  lineEntry.getValue());
					Matcher matcher = pattern.matcher(lineEntry.getValue());
					if(matcher.find()){
						log("matched pattern: " + pattern + " to line: " + lineEntry.getValue());
						MatchedQuote matchedQuote = new 
								MatchedQuote(lineEntry.getKey(),
										regex,lineEntry.getValue());
						results.add(matchedQuote);
						}
						
					}
				
		}		
		log("applyMatchRegex returning: " + results.size() + " quotes" );
		return results;
	}
	
	private List<String> applyGroupRegex(List<String> lines, List<String> regexList)
	{
		List<String> results = new LinkedList<String>();
		for(String regex : regexList)
		{
			log("regex is: " + regex);
			Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);		
			for(String line : lines)
			{
					log("line to group capture is: " +  line);
					Matcher matcher = pattern.matcher(line);
					if(matcher.find()){
						log("group count is: " + matcher.groupCount());
						for(int i=0; i < matcher.groupCount(); i++)
						{
							String quote   = matcher.group(i);
							log("matched group is: " + quote);
							results.add(quote);
						}				
					}
			}
		}		
		log("applyRegex returning: " + results.size() + " quotes" );
		return results;
	}
	
	private static void log(String s)
	{
		logger.info( s );
	}
		
	
	static String readString(String fileName) throws IOException {
		StringBuilder results = new StringBuilder();
		FileReader reader = new FileReader(fileName);	
	    BufferedReader br = new BufferedReader(reader);
	    try {
	      String line = br.readLine();

	        while (line != null) {
	        	results.append(line);       
	            line = br.readLine();
	        }	       
	    } finally {
	        br.close();
	    }
	    return results.toString();
	}
	
	
	static Map<Character,Character> SPECIAL_CHARS_REPLACEMENT_MAP = new HashMap<Character,Character>();
	static{
		Character c1 = '“';
		Character c2 = '”';
		SPECIAL_CHARS_REPLACEMENT_MAP.put(c1,'"');
		SPECIAL_CHARS_REPLACEMENT_MAP.put(c2,'"');
	}
	
	private List<String> replaceSpecialChars( List<String> lines){
		 List<String> results = new  LinkedList<String>();
		 for(String line: lines){
			 for(Map.Entry<Character, Character> cEntry: SPECIAL_CHARS_REPLACEMENT_MAP.entrySet()){
				 line = line.replace(cEntry.getKey(), cEntry.getValue());
			 }
			 results.add(line);
		 }		
		 return results;
	}
	

	
	private List<String> readFile(String fileName) throws IOException {
		List<String>  results = new LinkedList<String>();
	    BufferedReader br = new BufferedReader(new FileReader(fileName));
	    try {
	        String line = br.readLine();

	        while (line != null) {
	        	results.add(line);	          
	            line = br.readLine();
	        }	       
	    } finally {
	        br.close();
	    }
	    return results;
	}
	
	
}