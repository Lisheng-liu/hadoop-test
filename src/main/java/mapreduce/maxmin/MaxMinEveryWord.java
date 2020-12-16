package mapreduce.maxmin;

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
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;


/**
 * 同时求每组词的最大值和每组词的最小值, 利用自定义序列化类来作为map输出的value类型
 * 
 * 步骤：
 * map阶段：
 * key:     单词   Text
 * value:   num WordWritable
 * 
 * map(): 正常输出
 * 
 * combiner：
 * 计算每个单词的最大值最小值并输出【局部】
 * 
 * 
 * reduce 阶段：
 * reduce()：有多少种单词就调用多少次reduce()
 * 计算每个单词的最大值最小值并输出【全局】
 * 
 * job参数设置：
 *    设置combiner的class
 */
public class MaxMinEveryWord extends Configured implements Tool{
	
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
	
	/**
	 * combiner
	 */
	public static class MaxMinEveryWordCombiner extends Reducer<Text, WordWritable, Text, WordWritable>{
		WordWritable valueOut = new WordWritable();
		
		@Override
		protected void reduce(Text key, Iterable<WordWritable> values, Context context) throws IOException, InterruptedException {
			long max = 0L;
			long min = 0L;
			boolean isFirst = true;

			for (WordWritable value : values) {
				long num = value.getNum();
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
	
	
	/**
	 * reduce 阶段
	 */
	public static class MaxMinEveryWordReducer extends Reducer<Text, WordWritable, Text, Text>{
		Text valueOut = new Text();
		
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
			valueOut.set(max + "\001" + min);
			context.write(key, valueOut);
		}
	}
	
	/**
	 * job 运行类
	 */
	public int run(String[] args) throws Exception {
		// 获取 configuration 对象
		Configuration conf = getConf();
		
		// 创建job对象
		Job job = Job.getInstance(conf, "MaxMinEveryWord");
		
		// 设置job运行类
		job.setJarByClass(MaxMinEveryWord.class);
		
		// 设置map reduce combiner 运行的类
		job.setMapperClass(MaxMinEveryWordMapper.class);
		job.setReducerClass(MaxMinEveryWordReducer.class);
		job.setCombinerClass(MaxMinEveryWordCombiner.class);
		
		// 设置 map 输出的key value的类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(WordWritable.class);
		
		// 设置 reduce 输出的key value的类型
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		// 设置任务的输入、输出目录
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
		// 运行参数 ：  D:/tmp/input/maxmin C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new MaxMinEveryWord(), args);
	}
	
}
