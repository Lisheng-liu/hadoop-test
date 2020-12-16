package mapreduce.wordCountSort;

import java.io.IOException;

public class TopK {

    public static void main(String[] args) throws InterruptedException, IOException, ClassNotFoundException {
        String in = "/tmp/mr/wordcount/ambari.properties.1";
        String wordCountOut = "/tmp/mr/wordcount/wordCountOut";
        String sort = "/tmp/mr/wordcount/sort";
        String topK = "/tmp/mr/wordcount/topK";
        if(WordCount.run(in,wordCountOut)){
            Sort.run(wordCountOut,sort,topK);
        }
    }


}
