package mapreduce.multioutput;

import mapreduce.writable.WordWritable;
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
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;


/**
 * 同时求每组词的最大值和每组词的最小值, 利用自定义序列化类来作为map输出的value类型
 * 如何设置reduce多目录输出
 * 
 * 步骤：
 * 1）在reduce类里定义多目录输出对象；
 * 
 * 2）在setup() 创建多目录输出对象；
 * 
 * 3）在map() 通过多目录输出对象，指定输出；
 * 
 * 4）在cleanup() 关闭多目录输出的流；
 */
public class MaxMinEveryWordMultiOutput extends Configured implements Tool {
	
	/**
	 * map 阶段
	 */
	public static class MaxMinEveryWordMapper extends Mapper<LongWritable, Text, Text, WordWritable>{
		Text keyOut = new Text();
		WordWritable valueOut = new WordWritable();
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] splits = value.toString().split("\t");

			if(splits == null || splits.length != 2){
				context.getCounter("zss", "bad line num").increment(1L);
				return;
			}
			String word = splits[0];
			long num = Long.parseLong(splits[1]);
			keyOut.set(word);
			valueOut.setNum(num);
			context.write(keyOut, valueOut);
		}
	}
	
	public static class MaxMinEveryWordCombiner extends Reducer<Text, WordWritable, Text, WordWritable>{
		WordWritable valueOut = new WordWritable();
		
		@Override
		protected void reduce(Text key, Iterable<WordWritable> values, Context context) throws IOException, InterruptedException {
			long max = 0L;
			long min = 0L;
			boolean isFirst = true;

			for (WordWritable w : values) {
				long num = w.getNum();
				
				if(max < num){
					max = num;
				}
				if(min > num || isFirst){
					isFirst = false;
					min = num;
				}
			}
			// 输出map局部每个单词的最大值
			valueOut.setNum(max);
			context.write(key, valueOut);
			// 输出map局部每个单词的最小值
			valueOut.setNum(min);
			context.write(key, valueOut);
		}
	}
	
	
	public static class MaxMinEveryWordReducer extends Reducer<Text, WordWritable, Text, Text>{
		Text valueOut = new Text();
		// 定义一个多目录输出对象
		MultipleOutputs<Text, Text> outputs = null;
		
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			// 创建多目录输出对象
			outputs = new MultipleOutputs<Text, Text>(context);
		}
		
		@Override
		protected void reduce(Text key, Iterable<WordWritable> values, Context context) throws IOException, InterruptedException {
			long max = 0L;
			long min = 0L;
			boolean isFirst = true;

			for (WordWritable w : values) {
				long num = w.getNum();

				if(max < num){
					max = num;
				}
				if(min > num || isFirst){
					isFirst = false;
					min = num;
				}
			}
			
			// 输出每个单词的最大值
			valueOut.set(String.valueOf(max));
			outputs.write(key, valueOut, "maxout/max");
			// 输出每个单词的最小值
			valueOut.set(String.valueOf(min));
			outputs.write(key, valueOut, "minout/min");
		}
		
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			// 关闭多目录输出
			outputs.close();
		}
	}
	
	public int run(String[] args) throws Exception {
		// 获取configuration 对象
		Configuration conf = getConf();
		// 创建 job 对象
		Job job = Job.getInstance(conf, "MaxMinEveryWordMultiOutput");
		// 设置job运行类
		job.setJarByClass(MaxMinEveryWordMultiOutput.class);
		
		// 设置map  reduce 运行类
		job.setMapperClass(MaxMinEveryWordMapper.class);
		job.setReducerClass(MaxMinEveryWordReducer.class);
		job.setCombinerClass(MaxMinEveryWordCombiner.class);
		
		// 设置 map输出的key  value 类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(WordWritable.class);
		
		// 设置reduce 最终输出的 key  value 类型
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		// 设置任务的输入  输出目录
		FileInputFormat.addInputPath(job, new Path(args[0]));
		Path outputDir = new Path(args[1]);
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
			System.out.println("delete output path:" + outputDir.toString() + " successed!");
		}
		FileOutputFormat.setOutputPath(job, outputDir);
		
		// 打印日志
		boolean status = job.waitForCompletion(true);
		
		return status ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		// 运行参数  D:/tmp/input/maxmin C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new MaxMinEveryWordMultiOutput(), args);
	}
	
}
