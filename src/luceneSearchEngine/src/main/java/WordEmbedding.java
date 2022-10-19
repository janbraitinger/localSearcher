import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.util.ArrayList;
import java.util.Collection;

public class WordEmbedding {
    private Word2Vec w2vModel = null;

    public void loadModel(String path) {
        this.w2vModel = WordVectorSerializer.readWord2VecModel(path);
    }

    public ArrayList<SimilarObject> getSimilarObjects(String word, int count) {
        Collection<String> simWordsList = this.w2vModel.wordsNearest(word, count);
        ArrayList<SimilarObject> list = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (String simWord:simWordsList) {
            SimilarObject similarObject = new SimilarObject();
            similarObject.similarity = this.w2vModel.similarity(simWord, word);
            similarObject.term = simWord;
            list.add(similarObject);
        }

        long endTime = System.currentTimeMillis();


        return list;

    }

    public Collection<String> getSimilarWords(String word, int count) {
        ArrayList tmp = new ArrayList();
        Collection a = this.w2vModel.wordsNearest(word, count);
        tmp.add(word);
        for(Object b : a){
            tmp.add(b);
        }

        return tmp;

    }


}




