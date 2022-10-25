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
        ArrayList<String> tmp = new ArrayList();
        for(Object b : simWordsList){
            if(b.toString().chars().allMatch(Character::isLetter)){
                tmp.add(b.toString().toLowerCase());
            }

        }
        ArrayList<SimilarObject> list = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (String simWord:tmp) {
            SimilarObject similarObject = new SimilarObject();
            similarObject.similarity = this.w2vModel.similarity(simWord, word);
            similarObject.term = simWord;
            list.add(similarObject);
        }

        long endTime = System.currentTimeMillis();


        return list;

    }

    public Collection<String> getSimilarWords(String word, int count) {
        long startTime = System.currentTimeMillis();
        ArrayList tmp = new ArrayList();
        Collection a = this.w2vModel.wordsNearest(word, count);
        tmp.add(word);
        for(Object b : a){
            if(b.toString().chars().allMatch(Character::isLetter)){
                tmp.add(b.toString().toLowerCase());
            }

        }
        long endTime = System.currentTimeMillis();
        Console.print("Generating similar words needed " + (endTime-startTime) + " ms", 0);
        return tmp;

    }

    public double getSimilarity(String wordA, String wordB){
        return this.w2vModel.similarity(wordA, wordB);
    }


}




