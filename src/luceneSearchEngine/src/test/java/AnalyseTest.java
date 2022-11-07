import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

public class AnalyseTest {

    @Test
    public void testEmbeddingCounter(){
        WordEmbedding wordEmbedding = new WordEmbedding();
        wordEmbedding.loadModel(Path.EMBEDDINGS + "pubmed.bin");
        assertEquals("Test size of returned embeddings",11, wordEmbedding.getSimilarWords("cancer", 10).size());
    }

    @Test
    public void testCartesianProduct(){
        SearchObject sO = new SearchObject("cancer", null);
        ArrayList<String> a1 = new ArrayList();
        a1.add("A");
        a1.add("B");
        ArrayList<String> b1 = new ArrayList<>();
        b1.add("C");
        b1.add("D");

        List<List<String>> tmp = new ArrayList();
        tmp.add(a1);
        tmp.add(b1);

        List<List<String>> result = sO.cartesian(tmp);



        assertEquals("Test cartesian product size of input list",4, result.size());

    }

}
