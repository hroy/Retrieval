import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * 
 * @author H. Roy
 *
 */
public class Index {

	private String pathCranfield = "";
	private String pathStopWords = "";
	
	private Set<String> stopWords = new HashSet<String>();
	private HashMap<String, Integer> stemmedTokens = new HashMap<String, Integer>();
	private HashMap<Integer, Doc> docsList = new HashMap<Integer, Doc>();
	
	Tokenizer tokenizer = new Tokenizer();
	Stemmer stemmer = new Stemmer();
	
	IndexUncompressed indexUncompressed = new IndexUncompressed();
	IndexCompressed indexCompressed = new IndexCompressed();
	
	int serial = 0;
	
	int totalNoFiles = 0;
    long totalDocLen = 0;
    
	public String getStemmer(String token) {
        stemmer.add(token.toCharArray(), token.length());
        stemmer.stem();

        return stemmer.toString();
    }
	
	private void uncompressedIndexing(File file) throws FileNotFoundException
	{
		int docLen = 0;
		int max = -1;
		String stemMAX = "";
		
		stemmedTokens.clear();
		
		tokenizer.tokenizeFile(file);
		String title = tokenizer.getFileTitle();
		HashMap<String, Integer> tokens = tokenizer.getAllTokens();
		
		serial++;
		
		docLen += tokens.size();
		
		for (String token : tokens.keySet())
		{
			//checking stopwords
			if (isStopWord(token)) {
                continue;
            }
			
			//finding stem
			String stemmed = getStemmer(token);
			stemmedTokens.put(stemmed, tokens.get(token));
		}		
		
		for (String token : stemmedTokens.keySet()) {
            indexUncompressed.put(token, serial, stemmedTokens.get(token));
            if (max < stemmedTokens.get(token)) {
            	max = stemmedTokens.get(token);
            	stemMAX = token;
            }
        }
		
//		docsList.put(serial, new Doc(serial, file.getName(), tokens.size(), max, stemMAX));
		docsList.put(serial, new Doc(serial, file.getName(), title, tokens.size(), max, stemMAX));
		totalDocLen += docLen;
	}
	
	public PostingList getPostingList(String term) {
        return indexUncompressed.keySet().contains(term) ? indexUncompressed.get(term) : null;
    }
	
	public Set<String> getStemmedTokens(String[] tokens)
	{
		Set<String> stemmerSet = new LinkedHashSet<String>();
		
		for (String token : tokens) {
            if (this.isStopWord(token)) {
                continue;
            }

            String stemmed = this.getStemmer(token);
            stemmerSet.add(stemmed);
        }
		return stemmerSet;
	}
	
	private boolean isStopWord(String word) {
        return stopWords.contains(word);
    }
	
	private void parseStopWords(String path) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(path));
		
        String line;
        while ((line = br.readLine()) != null) {
            stopWords.add(line.trim().toLowerCase());
        }
	}
	
	public static String join(String separator, String[] strings) {

        if (strings == null) {
            return null;
        }

        if (strings.length == 0) {
            return "";
        }

        if (strings.length == 1) {
            return strings[0];
        }

        StringBuilder stringBuilder = new StringBuilder(strings[0]);
        for (int i = 1; i < strings.length; i++) {
            stringBuilder.append(separator).append(strings[i]);
        }

        return stringBuilder.toString();
    }
	
	public Tokenizer getTokenizerObject()
	{
		return this.tokenizer;
	}
	
	public void indexFiles(String[] directories) throws IOException
	{
		if(directories.length>=2)
		{
			this.pathCranfield = directories[0];
			this.pathStopWords = directories[1];
		}
		
		parseStopWords(this.pathStopWords);
		
		File[] listFiles = tokenizer.readFiles(this.pathCranfield);
		
		if (listFiles.length == 0) {
            throw new RuntimeException("No file in this directory!");
        }
		
		for (File file : listFiles) {
            try {
            	if(file.isFile()){
            		totalNoFiles++;
            		uncompressedIndexing(file);
            	}
            } catch (Exception ex) {
                System.err.println("Exception in file reading! - " + file.getName() + ", ex- "+ex.getMessage());
            }
        }
		
		compressedIndexing();
	}
	
	public void compressedIndexing()
	{
		for (String term : indexUncompressed.keySet()) {

            PostingList pList = indexUncompressed.get(term);

            List<PostingListCell> docsList = pList.getDocsList();
            List<PostingListCellCompressed> docsListCompressed = new ArrayList<PostingListCellCompressed>();

            int prevDocID = -1;
            for (PostingListCell wc : docsList) {

                int docId = wc.getDocId();
                int stemFreq = wc.getStemFreq();
                
                if (prevDocID == -1) {
                    docsListCompressed.add(new PostingListCellCompressed(Encoder.delta(docId), Encoder.gamma(stemFreq)));
                    prevDocID = docId;
                    continue;
                }

                int gap = docId - prevDocID;
                docsListCompressed.add(new PostingListCellCompressed(Encoder.delta(gap), Encoder.gamma(stemFreq)));
                prevDocID = docId;
            }

            indexCompressed.put(term, new PostingListCompressed(docsListCompressed));
        }
	}
	
	public long getUncompressedIndexSize(){
		
		return indexUncompressed.size();
    }
	
	public long getCompressedIndexSize(){
		
		return indexCompressed.size();
    }
    
    public int getIndexLength() {
        return indexUncompressed.keySet().size();
    }
    
    public int getUncompressedInvertedListLength(String term) 
    {
    	String stem = getStemmer(term.toLowerCase()); 
    	PostingList token = getStemDetails(term);
    	
        return (2*stem.length()) + token.getPostingListLength()+ 4;
    }
    
    public int getCompressedInvertedListLength(String term) 
    {
    	int pListSize = 0;
    	int stringSize = 0;
    	String stem = getStemmer(term.toLowerCase()); 
    	PostingListCompressed ptList = indexCompressed.get(stem);
		Iterator docsIterator = ptList.getDocsList().iterator();
		while(docsIterator.hasNext())
		{
			PostingListCellCompressed ptCell = (PostingListCellCompressed)docsIterator.next();
			pListSize += ptCell.docId.length + ptCell.stemFreq.length;
		}
		stringSize += (2*stem.length());
    	
        return pListSize + stringSize;
    }
    
    public PostingList getStemDetails(String term) {
    	String stem = getStemmer(term.toLowerCase());
        return indexUncompressed.keySet().contains(stem) ? indexUncompressed.get(stem) : null;
    }
    
    public HashMap<Integer, Doc> getDocsList()
    {
    	return docsList;
    }
    
    public int byteArrayToInt(byte[] b) 
    {
        int value = 0;
        for (int i = 0; i < b.length; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }
    
    public String byteArrayToString(byte[] b) 
    {
        if(b.length==0) return "0";
        return new BigInteger(b).toString(2);
    }
    
    public void generateUncompressedIndexOutFile(String file) throws IOException
    {
    	FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		
		System.out.println("Uncompressed index details have been written to " + file);
		for (String stem : indexUncompressed.keySet()) {
			PostingList obPt = indexUncompressed.get(stem);
			out.write("stem: " + stem + ", df: " + obPt.getNumberofDocs());
			
			Iterator docsIterator = obPt.getDocsList().iterator();
			while(docsIterator.hasNext())
			{
				PostingListCell ptCell = (PostingListCell)docsIterator.next();
				out.write(", (id: " + ptCell.getDocId() +", tf: " + ptCell.getStemFreq() + ")");
			}
			out.write(", total freqency: " + obPt.getFrequency() + "\n");
		}
		
		out.close();
    }
    
    public void generateCompressedIndexOutFile(String file) throws IOException
    {
    	FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		
		System.out.println("Compressed index details have been written to " + file);
		for (String stem : indexCompressed.keySet()) {
			PostingListCompressed obPt = indexCompressed.get(stem);
			out.write("stem: " + stem + ", df: " + obPt.getNumberofDocs());
			
			Iterator docsIterator = obPt.getDocsList().iterator();
			while(docsIterator.hasNext())
			{
				PostingListCellCompressed ptCell = (PostingListCellCompressed)docsIterator.next();
				out.write(", (delta-gap: " + byteArrayToString(ptCell.getDocId()) +", gamma-tf: " + byteArrayToString(ptCell.getStemFreq()) + ")");
			}
			out.write("\n");
		}
		
		out.close();
    }
    
    public void generateDocsStatOutFile(String file) throws IOException
    {
    	FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		
		System.out.println("Documents' statistics has been written to " + file);
		for (int id : docsList.keySet()) {
			Doc obDoc = docsList.get(id);
			out.write("Id: " + obDoc.getDocumentId()+", Name: " + obDoc.getDocumentName() + ", Total terms: " + obDoc.getTotalTerms() + ", Most Frequent Term: "+ obDoc.getMaxOccurringStem() + ", Max Frequency: "+ obDoc.getMaxTermFrequency()+"\n");
		}
		
		out.close();
    }
    
    public double getAvgDocLength() {
        return (double) totalDocLen / (double) totalNoFiles;
    }
    
    public int getTotalNoFiles() {
        return totalNoFiles;
    }
}
