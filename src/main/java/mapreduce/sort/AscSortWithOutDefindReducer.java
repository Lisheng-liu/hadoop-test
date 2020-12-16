package mapreduce.sort;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * 无自定义reduce的排序
 * 
 * @author 郑松松
 * @time   2019年8月4日 上午11:45:35
 */
public class AscSortWithOutDefindReducer extends Configured implements Tool {

	/**
	 * map 阶段
	 */
	public static class AscSortMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
		
		/**
		 * key	: name
		 * value: id
		 */
		Text keyOut = new Text();
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split("\t");
			String name = splits[0];
			long id = Long.parseLong(splits[1]);
			
			keyOut.set(name);
			valueOut.set(id);
			context.write(keyOut, valueOut);
		}
	}
	
	
	/**
	 * job 运行类
	 */
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		Job job = Job.getInstance(conf, "AscSort");
		job.setJarByClass(AscSortWithOutDefindReducer.class);
		
		job.setMapperClass(AscSortMapper.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		Path outputDir = new Path(args[1]);
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
			System.out.println("delete ouotput path:" + outputDir.toString() + " successed!");
		}
		FileOutputFormat.setOutputPath(job, outputDir);
		
		boolean status = job.waitForCompletion(true);
		return status ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
		// 运行参数：D:/tmp/input/sort C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new AscSortWithOutDefindReducer(), args);
	}
}
