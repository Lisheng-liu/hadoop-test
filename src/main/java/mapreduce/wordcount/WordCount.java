package mapreduce.wordcount;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * WordCount 单词统计
 *
 * @author 郑松松
 * @time   2019年7月30日 下午3:09:48
 */
public class WordCount extends Configured implements Tool {

    /**
     * map 阶段
     */
    // 												     字节偏移量  一行的数据
    public static class WordCountMapper extends Mapper<LongWritable, Text, Text, LongWritable> {

        Text keyOut = new Text();
        LongWritable valueOut = new LongWritable(1L);

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            System.out.println("------------map()-----------");
            System.out.println("map input ==> keyin:" + key.get() + "; valuein:" + value.toString());

            String line = value.toString();
            String[] split = line.split(" ");
            for (String word : split) {
                keyOut.set(word);

                // map 输出数据
                context.write(keyOut, valueOut);
                System.out.println("map output ==> key:" + word + "; value:" + valueOut.get());
            }
        }
    }


    /**
     * reduce 阶段
     */
    public static class WordCountReduce extends Reducer<Text, LongWritable, Text, LongWritable> {
        LongWritable valueOut = new LongWritable();

        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            System.out.println("-------------reduce()------------");
            StringBuffer sb = new StringBuffer("reduce inup ==> key:" + key.toString() + "; values [");

            Long sum = 0L;
            for (LongWritable value : values) {
                long number = value.get();
                sum += number;
                sb.append(number).append(",");
            }
            sb.deleteCharAt(sb.length() - 1).append("]");
            System.out.println(sb.toString());
            valueOut.set(sum);
            context.write(key, valueOut);
        }
    }

    /**
     * 设置 job 运行类
     */
    public int run(String[] args) throws Exception {
        // 获取 Configuration 对象，用于创建 MapReduce 任务的Job 对象
        Configuration conf = getConf();

        // 创建 job 对象
        Job job = Job.getInstance(conf, "wordcount");

        // 设置 job 运行类
        job.setJarByClass(WordCount.class);

        // 设置 map reduce 的运行类
        job.setMapperClass(WordCountMapper.class);
        job.setReducerClass(WordCountReduce.class);

        // 设置【reduce】运行的个数, 默认一个可以不写
        job.setNumReduceTasks(2);

        // 设置map 输出的key value 的类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);

        // 设置reduce 输出的key value 的类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        // 设置map输入的format class类型， 默认是TextInputFormat.class【文本】，可以不写
        job.setInputFormatClass(TextInputFormat.class);
        // 设置reduce输出的format class 类型，默认是TextOutputFormat.class【文本】， 可以不写
        job.setOutputFormatClass(TextOutputFormat.class);

        // 设置任务的输入目录
        FileInputFormat.addInputPath(job, new Path(args[0]));
        Path outputDir = new Path(args[1]);
        // 自动删除输出目录
        FileSystem fs = FileSystem.get(conf);
        if(fs.exists(outputDir)){
            fs.delete(outputDir, true);
            System.out.println("delete output path:" + outputDir.toString() + "success!");
        }
        // 设置目录的输出目录
        FileOutputFormat.setOutputPath(job, outputDir);

        // 运行的时候，不打印counter = false
        boolean status = job.waitForCompletion(false);

        return status ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        // 运行参数： D:/tmp/input/wordcount C:/Users/song/Desktop/output/wordcount
        // 集群运行:  packkage打包, hadoop jar hadoop-1.0-SNAPSHOT.jar com.songsong.mapreduce.wordcount.WordCount /user/hadoop/mapreduce/input/wordcount/word.txt /user/hadoop/mapreduce/output
        ToolRunner.run(new WordCount(), args);
    }
}
