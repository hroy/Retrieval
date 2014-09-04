import java.io.BufferedWriter;
import java.io.IOException;

/**
 * 
 * @author H. Roy
 *
 */

public class Result {

    private int rank;
    private double score;
    private int documentId;
    private String documentName;
    private String documentTitle;


    public Result(int rank, double score, int documentId, String documentName, String documentTitle) {
        this.rank = rank;
        this.score = score;
        this.documentId = documentId;
        this.documentName = documentName;
        this.documentTitle = documentTitle;
    }

    public int getRank() {
        return rank;
    }

    public double getScore() {
        return score;
    }

    public int getDocumentId() {
        return documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }
    
    public String getDocumentName() {
        return documentName;
    }
    
    public void displayResult(Result result,BufferedWriter out) throws IOException
    {
    	System.out.println("Rank          : " + result.getRank());
        System.out.println("Score         : " + result.getScore());
        System.out.println("Document Id   : " + result.getDocumentId());
        System.out.println("Document Name   : " + result.getDocumentName());
        System.out.println("Document Title: " + result.getDocumentTitle());
        
        out.write("Rank          : " + result.getRank()+"\n");
        out.write("Score         : " + result.getScore()+"\n");
        out.write("Document Id   : " + result.getDocumentId()+"\n");
        out.write("Document Name   : " + result.getDocumentName()+"\n");
        out.write("Document Title: " + result.getDocumentTitle()+"\n");
    }
}