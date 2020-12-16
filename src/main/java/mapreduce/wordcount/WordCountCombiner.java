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
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * combiner 输出压缩
 * 
 * 步骤：
 * 1） 定义一个combiner 类 extends Reducer， Reducer 输入类型和输出类型都和mapper输出类型一致
 * 
 * 2）需要在job参数设置时增加
 *      job.setCombinerClass(WordCountCombiner.class);
 *      
 * @author 郑松松
 * @time   2019年7月30日 下午9:53:58
 */
public class WordCountCombiner extends Configured implements Tool{
	
	/**
	 * map 阶段
	 */
	public static class WordCountMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
		Text keyOut = new Text();
		LongWritable valueOut = new LongWritable(1L);
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String line = value.toString();
			String[] splits = line.split(" ");
			for (String word : splits) {
				keyOut.set(word);
				context.write(keyOut, valueOut);
			}
		}
	}

	/**
	 * combiner 阶段
	 */
	public static class WordCountWithCombiner extends Reducer<Text, LongWritable, Text, LongWritable>{
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
			
			long sum = 0L;
			for (LongWritable value : values) {
				sum += value.get();
			}
			valueOut.set(sum);
			context.write(key, valueOut);
		}
	}
	
	
	/**
	 * reduce 阶段
	 */
	public static class WordCountReducer extends Reducer<Text, LongWritable, Text, LongWritable>{
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {

			long sum = 0L;
			for (LongWritable value : values) {
				long n = value.get();
				sum += n;
			}
			valueOut.set(sum);
			context.write(key, valueOut);
		}
	}
	
	/**
	 * job 对象
	 */
	public int run(String[] args) throws Exception {
		// 获取configuration 对象，用于创建Mapreduce任务的Job对象
		Configuration conf = getConf();
		
		// 创建Job对象
		Job job = Job.getInstance(conf, "wordcountcombiner");
		
		// 设置job运行的类
		job.setJarByClass(WordCountCombiner.class);
		
		// 设置map  reduce  combiner运行类
		job.setMapperClass(WordCountMapper.class);
		job.setReducerClass(WordCountReducer.class);
		job.setCombinerClass(WordCountWithCombiner.class);
		
		// 【设置reduce的个数】默认一个可以不写
		job.setNumReduceTasks(1);
		
		// 设置map reduce 输出类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
		
		// 设置任务的输入、输出目录
		FileInputFormat.addInputPath(job, new Path(args[0]));
		Path outputDir = new Path(args[1]);
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
		}
		FileOutputFormat.setOutputPath(job, outputDir);
		
		// 运行job任务, 不打印counter  
		boolean status = job.waitForCompletion(true);
	
		return status ? 0 : 1;
	}
	
	
	public static void main(String[] args) throws Exception {
		// 运行参数  D:/tmp/input/wordcount C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new WordCountCombiner(), args);
	}
}

