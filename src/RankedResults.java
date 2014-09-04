import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author H. Roy
 *
 */

public class RankedResults {

	private List<Result> resultList;

    public RankedResults() {
        resultList = new ArrayList<Result>();
    }

    public boolean add(Result result) {
        return resultList.add(result);
    }

    public List<Result> getResultList() {
        return resultList;
    }
}
