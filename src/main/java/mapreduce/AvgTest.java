package mapreduce;

import mapreduce.wordcount.WordCount;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
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

public class AvgTest extends Configured implements Tool {

    static class AvgMapper extends Mapper<LongWritable,Text,Text,LongWritable>{

        Text outk = new Text();
        LongWritable outV = new LongWritable();

        @Override
        public void map(LongWritable key,Text value,Context context) throws IOException, InterruptedException {
            String[] splits = value.toString().split(" ");
            if(splits.length == 2){
                String name = splits[0];
                int score = Integer.parseInt(splits[1]);
                outk.set(name);
                outV.set(score);
                context.write(outk,outV);
            }
        }
    }

    static class AvgReduce extends Reducer<Text,LongWritable,Text, FloatWritable>{
        Text outk = new Text();
        FloatWritable outV = new FloatWritable();
        @Override
        public void reduce(Text key,Iterable<LongWritable> values,Context context) throws IOException, InterruptedException {
            int sum = 0;
            int count = 0;
            for(LongWritable val: values){
                sum += val.get();
                count++;
            }
            float avg = sum/count*1.0f;
            outV.set(avg);
            context.write(key,outV);
        }

    }




    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = getConf();

        // 创建 job 对象
        Job job = Job.getInstance(conf, "avg");

        // 设置 job 运行类
        job.setJarByClass(AvgTest.class);

        // 设置 map reduce 的运行类
        job.setMapperClass(AvgTest.AvgMapper.class);
        job.setReducerClass(AvgTest.AvgReduce.class);

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
        ToolRunner.run(new AvgTest(),new String[]{"/tmp/mr/avg/","/tmp/mr/avg/out"});
    }
}
