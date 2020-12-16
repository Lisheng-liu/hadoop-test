package mapreduce.maxmin;

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
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * 求最大数
 *
 * 步骤：
 * map阶段：
 * key:   name
 * value: num
 *
 * map(): 找值最大的name和num
 *
 * cleanup()： 将最大的name和num输出
 *
 * reduce 阶段：
 * reduce()：找值最大的全局的name和num
 *
 * cleanup()：输出全局的最大
 */
public class MaxWord extends Configured implements Tool{

    /**
     * Map 阶段
     */
    public static class MaxWordMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
        Text keyOut = new Text();
        long max = 0L;

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String[] split = value.toString().split("\t");
            if(split == null || split.length != 2){
                context.getCounter("zss", "bad line num").increment(1L);
                return;
            }
            String name = split[0];
            long num = Long.parseLong(split[1]);
            if(max < num){
                keyOut.set(name);
                max = num;
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            // 将每个map值最大的输出
            context.write(keyOut, new LongWritable(max));
        }
    }


    /**
     * Reduce 阶段
     */
    public static class MaxWordReducer extends Reducer<Text, LongWritable, Text, LongWritable>{

        Text keyOut = new Text();
        long max = 0L;

        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            for(LongWritable value : values){
                long n = value.get();
                if(max < n){
                    max = n;
                    keyOut.set(key.toString());
                }
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            context.write(keyOut, new LongWritable(max));
        }
    }


    /**
     * job 运行类
     */
    public int run(String[] args) throws Exception {
        // 获取configuration 对象， 用于创建MapReduce任务的job对象
        Configuration conf = getConf();

        // 创建job 对象
        Job job = Job.getInstance(conf, "MaxWord");

        // 设置job运行类
        job.setJarByClass(MaxWord.class);

        // 设置map reduce 运行类
        job.setMapperClass(MaxWordMapper.class);
        job.setReducerClass(MaxWordReducer.class);

        // 设置map输出的key  value 的类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);

        // 设置reduce 输出的key value 的类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        // 设置任务的输入输出目录
        FileInputFormat.addInputPath(job, new Path(args[0]));
        Path outputDir = new Path(args[1]);
        // 自动删除输出目录
        FileSystem fs = FileSystem.get(conf);
        if(fs.exists(outputDir)){
            fs.delete(outputDir, true);
            System.out.println("delete output path:" + outputDir.toString() + " success!");
        }
        FileOutputFormat.setOutputPath(job, outputDir);

        // counter 日志
        boolean status = job.waitForCompletion(true);

        return status ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        // 运行参数： D:/tmp/input/maxmin C:/Users/song/Desktop/output/wordcount
        ToolRunner.run(new MaxWord(), args);
    }


}
