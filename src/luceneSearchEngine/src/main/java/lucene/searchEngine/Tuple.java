package lucene.searchEngine;

public class Tuple {
    public String term;
    public int freq;

    public Tuple(String term, int freq){
        this.freq = freq;
        this.term = term;
    }

    public int getFreq() {
        return this.freq;
    }
}
