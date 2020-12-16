package mapreduce.wordCountSort;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * 统计词频
 * @author zx
 * zhangxian1991@qq.com
 */
public class WordCount {

    /**
     * 读取单词
     *
     *
     */
    public static class Map extends Mapper<Object,Text,Text,IntWritable>{

        IntWritable count = new IntWritable(1);

        @Override
        protected void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            StringTokenizer st = new StringTokenizer(value.toString());
            while(st.hasMoreTokens()){
                String word = st.nextToken().replaceAll("\"", "").replace("'", "").replace(".", "");
                context.write(new Text(word), count);
            }
        }

    }

    /**
     * 统计词频
     *
     *
     */
    public static class Reduce extends Reducer<Text,IntWritable,Text,IntWritable>{

        @SuppressWarnings("unused")
        @Override
        protected void reduce(Text key, Iterable<IntWritable> values,Context context)
                throws IOException, InterruptedException {
            int count = 0;
            for (IntWritable intWritable : values) {
                count ++;
            }
            context.write(key,new IntWritable(count));
        }

    }

    @SuppressWarnings("deprecation")
    public static boolean run(String in,String out) throws IOException, ClassNotFoundException, InterruptedException{

        Configuration conf = new Configuration();

        Job job = new Job(conf,"WordCount");
        job.setJarByClass(WordCount.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        // 设置Map输出类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);

        // 设置Reduce输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // 设置输入和输出目录
        FileInputFormat.addInputPath(job, new Path(in));
        FileOutputFormat.setOutputPath(job, new Path(out));

        return job.waitForCompletion(true);
    }

}
