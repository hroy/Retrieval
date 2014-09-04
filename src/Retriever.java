import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author H. Roy
 *
 */

public class Retriever {
	
	Tokenizer obTokenizer;
	Index obIndex;
	
	static Pattern qLine = Pattern.compile("^Q\\d+:$");

	public Retriever(Index index)
	{
		obTokenizer = index.getTokenizerObject();
		obIndex = index;
	}
	
    public static List<String> readQueriesFromFile(String filename) throws IOException {

        List<String> queryList = new ArrayList<String>();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
        String line = null;
        StringBuilder tempQuery = new StringBuilder();

        while ((line = bufferedReader.readLine()) != null) {

            Matcher m = qLine.matcher(line);
            if (m.matches()) {
                if (!tempQuery.toString().isEmpty()) {
                    queryList.add(tempQuery.toString());
                }
                tempQuery.delete(0, tempQuery.length());
            } else {
                tempQuery.append(line.trim()).append(" ");
            }
        }

        queryList.add(tempQuery.toString());

        return queryList;
    }

    public String[] getQueryTerms(String query) {

        String[] tokens = obTokenizer.getTokensFromString(query);        
        Set<String> terms = obIndex.getStemmedTokens(tokens);

        return terms.toArray(new String[terms.size()]);
    }

    public RankedResults[] retrieveRankedDocuments(String query) {

        String[] terms = getQueryTerms(query);
        Map<Integer, Doc> documentList = obIndex.getDocsList();

        double avgDocLen = obIndex.getAvgDocLength();
        long collectionSize = obIndex.getTotalNoFiles();

        Map<Integer, Double> weight1Map = new TreeMap<Integer, Double>();
        Map<Integer, Double> weight2Map = new TreeMap<Integer, Double>();

        for (String term : terms) {
        	PostingList postingList = obIndex.getPostingList(term);

            if (postingList == null) {
                System.out.println("Term: " + term + " doesn't exist in document collection");
                continue;
            }

            int df = postingList.getNumberofDocs();

            for (PostingListCell postingListCell : postingList.getDocsList()) {

                Doc doc = documentList.get(postingListCell.getDocId());

                int maxTf = doc.getMaxTermFrequency();
                int docLen = doc.getTotalTerms();

                int tf = postingListCell.getStemFreq();

                double w1 = (0.4 + 0.6 * Math.log(tf + 0.5) / Math.log(maxTf + 0.5))
                        * (Math.log(collectionSize / df) / Math.log(collectionSize));

                double w2 = 0.4 + 0.6 * (tf / (tf + 0.5 + 1.5 * docLen / avgDocLen))
                        * (Math.log(collectionSize / df) / Math.log(collectionSize));

                int docId = doc.getDocumentId();

                if (!weight1Map.containsKey(docId)) {
                    weight1Map.put(docId, 0.0);
                }
                weight1Map.put(docId, weight1Map.get(docId) + w1);

                if (!weight2Map.containsKey(docId)) {
                    weight2Map.put(docId, 0.0);
                }
                weight2Map.put(docId, weight2Map.get(docId) + w2);
            }
        }

        Comparator<Map.Entry<Integer, Double>> comparator = new Comparator<Map.Entry<Integer, Double>>() {

            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                return o1.getValue().equals(o2.getValue()) ?
                        o1.getKey().compareTo(o2.getKey()) :
                        o2.getValue().compareTo(o1.getValue());
            }
        };

        List<Map.Entry<Integer, Double>> weight1List = new ArrayList<Map.Entry<Integer, Double>>(weight1Map.entrySet());
        List<Map.Entry<Integer, Double>> weight2List = new ArrayList<Map.Entry<Integer, Double>>(weight2Map.entrySet());

        Collections.sort(weight1List, comparator);
        Collections.sort(weight2List, comparator);

        RankedResults w1Results = new RankedResults();
        RankedResults w2Results = new RankedResults();
        
        for (int i = 0; i < 10; i++) {
        	if(weight1List.size()>=i){
	            Doc docForW1 = documentList.get(weight1List.get(i).getKey());	
	            w1Results.add(new Result((i + 1), weight1List.get(i).getValue(), weight1List.get(i).getKey(),
	                    docForW1.getDocumentName(), docForW1.getDocumentTitle()));
        	}
        	
        	if(weight2List.size()>=i){
	            Doc docForW2 = documentList.get(weight2List.get(i).getKey());	
	            w2Results.add(new Result((i + 1), weight2List.get(i).getValue(), weight2List.get(i).getKey(),
	                    docForW2.getDocumentName(), docForW2.getDocumentTitle()));
        	}
        }

        return new RankedResults[]{w1Results, w2Results};
    }
}
