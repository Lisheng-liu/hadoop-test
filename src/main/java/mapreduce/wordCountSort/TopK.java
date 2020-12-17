package mapreduce.wordCountSort;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * 统计词频 排序输出top 10
 */
public class TopK extends Configured implements Tool {

    public static void main(String[] args) throws Exception {
        ToolRunner.run(new TopK(),new String[]{});
    }



    @Override
    public int run(String[] strings) throws Exception {
        String in = "/tmp/mr/wordcount/";
        String wordCountOut = "/tmp/mr/wordcount/wordCountOut";
        String sort = "/tmp/mr/wordcount/sort";
        String topK = "/tmp/mr/wordcount/topK";
        Configuration conf = getConf();
        FileSystem fileSystem = FileSystem.get(conf);
        if(fileSystem.exists(new Path(wordCountOut))){
            fileSystem.delete(new Path(wordCountOut),true);
        }
        if(fileSystem.exists(new Path(sort))){
            fileSystem.delete(new Path(sort),true);
        }
        if(fileSystem.exists(new Path(topK))){
            fileSystem.delete(new Path(topK),true);
        }

        if(WordCount.run(in,wordCountOut)){
            Sort.run(wordCountOut,sort,topK);
        }
        return 0;
    }
}
