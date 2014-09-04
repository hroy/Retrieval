import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * 
 * @author H. Roy
 *
 */
public class Main {
	
	static String outRankedDocs = "ranked-docs.out";
	static FileWriter fstream;
	static BufferedWriter out;
	
	private static void printTop10(RankedResults rankedResults,BufferedWriter out) throws IOException {

        for (Result result : rankedResults.getResultList()) {
        	result.displayResult(result,out);
        }
    }
	
	private static void printResult(String query, String[] terms, RankedResults[] rankedResultsList) throws IOException {
		
		out.write("\nQuery      : " + query+"\n");
		out.write("Stemmed Query: " + Index.join(",", terms)+"\n\n");
		
        System.out.println();
        System.out.println("Query      : " + query);
        System.out.println("Stemmed Query: " + Index.join(",", terms));
        System.out.println();

        out.write("Top 10 Documents (Weight-1): "+"\n");
        System.out.println("Top 10 Documents (Weight-1): ");
        printTop10(rankedResultsList[0],out);

        out.write("\nTop 10 Documents (Weight-2): "+"\n");
        System.out.println();
        System.out.println("Top 10 Documents (Weight-2): ");
        printTop10(rankedResultsList[1],out);
    }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String[] inFilesDr = {"","",""};
				
		if(args.length == 3)
		{
//			System.out.println(args[0]+", " + args[1]);
			inFilesDr[0] = args[0];
			inFilesDr[1] = args[1];
			inFilesDr[2] = args[2];
		}
		else
		{
			System.err.println("Please provide [cranfield] [stopwords] [query] locations-");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (true) {				
				try {					
					inFilesDr = br.readLine().split("\\s+");			
					if(inFilesDr.length == 3) 
						break;
					else 
						System.err.println("Sorry! Please enter again...");
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
		}
		
		if(inFilesDr[0].equalsIgnoreCase("exit")) System.exit(1);
		
		if(inFilesDr.length==3)System.out.println("Given input directory: "+inFilesDr[0]+", "+inFilesDr[1]+", "+ inFilesDr[2]);
				
		Index obIndex = new Index();
		try {
			obIndex.indexFiles(inFilesDr);
		} catch (IOException e) {
			System.err.println("Exception in indexing file.");
			e.printStackTrace();
		}
		
		System.out.println("Indexing done...");
		System.out.println("Results...");
		Retriever retriever = new Retriever(obIndex);

		try {
			fstream = new FileWriter(outRankedDocs);
			out = new BufferedWriter(fstream);
			System.out.println("Results also be written to " + outRankedDocs);
			out.write("This is the output file for SP2014 IR HW3. Student: Harichandan Roy, hxr121830\n\n");			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
        if (inFilesDr.length==3) {
            try {
                List<String> queries = Retriever.readQueriesFromFile(inFilesDr[2]);
                for (String query : queries) {
                    RankedResults[] rankedResultsList = retriever.retrieveRankedDocuments(query);
                    String[] qTerms = retriever.getQueryTerms(query);
                    printResult(query, qTerms, rankedResultsList);
                }
            } catch (IOException e) {
                System.err.println("Error in reading query file.");
                System.exit(1);
            }
        }
        try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}        
	}
}
