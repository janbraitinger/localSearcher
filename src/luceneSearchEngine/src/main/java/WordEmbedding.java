import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.util.Collection;

public class WordEmbedding {
    private Word2Vec w2vModel = null;

    public void loadModel(String path){
        this.w2vModel = WordVectorSerializer.readWord2VecModel(path);
    }

    public Collection<String> getSimWords(String word, int count){
        return this.w2vModel.wordsNearest(word, count);
    }




}
