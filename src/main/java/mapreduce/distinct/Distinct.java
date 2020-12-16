package mapreduce.distinct;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
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
 * distinct 去重
 *
 * 步骤：
 * 1）将你要去重的数据，放到map输出的key上
 * 2）通过reduce的聚合特性，进行去重处理
 * 3）输出的时候，可以设置单输出key或者单输出value
 *
 * map阶段：
 *   key  : 单词       Text
 *   value: null  NullWritable
 *
 * reduce阶段：
 *   key :  单词       Text
 *   value: null  NullWritable
 *
 * @author 郑松松
 * @time   2019年7月31日 下午10:17:53
 */

public class Distinct extends Configured implements Tool{

    /**
     * map 阶段
     */
    public static class DistinctMapper extends Mapper<LongWritable, Text, Text, NullWritable>{
        Text keyOut = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            String[] splits = value.toString().split(" ");
            for (String word : splits) {
                keyOut.set(word);
                context.write(keyOut, NullWritable.get());
            }
        }
    }


    /**
     * reduce 阶段
     */
    public static class DistinctReduce extends Reducer<Text, NullWritable, Text, NullWritable> {
        @Override
        protected void reduce(Text key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {

            context.write(key, NullWritable.get());
        }
    }


    /**
     * job 阶段
     */
    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        Job job = Job.getInstance(conf, "distinct");
        job.setJarByClass(Distinct.class);

        job.setMapperClass(DistinctMapper.class);
        job.setReducerClass(DistinctReduce.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(NullWritable.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        Path outputDir  = new Path(args[1]);
        FileOutputFormat.setOutputPath(job, outputDir);
        FileSystem fs = FileSystem.get(conf);
        if(fs.exists(outputDir)){
            fs.delete(outputDir, true);
            System.out.println("delete output path: " + outputDir.toString() + " successed!");
        }

        boolean status = job.waitForCompletion(true);
        return status ? 0 : 1;
    }

    /**
     * 主函数
     */
    public static void main(String[] args) throws Exception {
        // 运行参数：D:/tmp/input/wordcount C:/Users/song/Desktop/output/wordcount
        System.out.println(ToolRunner.run(new Distinct(), args));
    }
}
