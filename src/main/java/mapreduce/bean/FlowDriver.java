package mapreduce.bean;

import mapreduce.writable.FlowBean;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class FlowDriver {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException{
        // 1.获取job实例
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf);

        // 2.设置类路径
        job.setJarByClass(FlowBean.class);

        // 3.设置map reduce
        job.setMapperClass(FlowMapper.class);
        job.setReducerClass(FlowReducer.class);

        // 4. 设置输入输出路径
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(FlowBean.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);

        // 5. 设置输入输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        Path outputDir = new Path(args[1]);
        // 自动删除输出目录
        FileSystem fs = FileSystem.get(conf);
        if(fs.exists(outputDir)){
            fs.delete(outputDir, true);
            System.out.println("delete output path:" + outputDir.toString() + "success!");
        }
        // 设置目录的输出目录
        FileOutputFormat.setOutputPath(job, outputDir);

        // 6.提交
        boolean b = job.waitForCompletion(true);
        System.exit(b ? 0 : 1);
    }
}
