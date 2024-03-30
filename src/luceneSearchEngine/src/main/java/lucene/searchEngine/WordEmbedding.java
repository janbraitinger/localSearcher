package lucene.searchEngine;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;


import java.util.ArrayList;
import java.util.Collection;

import static java.util.stream.Collectors.toCollection;

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
        for (String simWord:tmp) {
            SimilarObject similarObject = new SimilarObject();
            similarObject.similarity = this.w2vModel.similarity(simWord, word);
            similarObject.term = simWord;
            list.add(similarObject);
        }

        return list;

    }

    public ArrayList<String> getSimilarWords(String word, int count) {
        ArrayList wordList = new ArrayList();
        Collection similarWordsCollection = this.w2vModel.wordsNearest(word, count);
        wordList.add(word);
        for(Object tuple : similarWordsCollection){
            if(tuple.toString().chars().allMatch(Character::isLetter)){
                wordList.add(tuple.toString().toLowerCase());
            }
        }
        return (ArrayList<String>) wordList.stream().collect(toCollection(ArrayList::new));
    }

    public double getSimilarity(String wordA, String wordB){
        return this.w2vModel.similarity(wordA, wordB);
    }

}




