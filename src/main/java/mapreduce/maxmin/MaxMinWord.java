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
 * 同时求出全局最大最小
 * 
 * 步骤：
 * map阶段：
 * key:     max/min
 * value:   name\001num
 * 
 * map(): 找值最大最小的name和num
 * 
 * cleanup()： 将最大最小的name和num输出,  格式： max/min, name\001num
 * 
 * reduce 阶段：
 * reduce()：key有"max" 和 "min" 两种key,只调用两次reduce()就能找出全局最大最小，并输出。  
 * 具体逻辑，根据key不同找出最大的或者最小的 
 */
public class MaxMinWord extends Configured implements Tool{
	
	/**
	 * map 阶段
	 */
	public static class MaxMinWordMapper extends Mapper<LongWritable, Text, Text, Text>{
		Text maxValueOut = new Text();
		Text minValueOut = new Text();
		long max = 0L;
		long min = 0L;
		// 判读是不是第一次比较
		boolean isFirst = true;
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			
			String[] splits = value.toString().split("\t");
			
			if(splits == null || splits.length != 2){
				context.getCounter("zss", "bad line num").increment(1L);
				return;
			}
			
			String name = splits[0];
			long num = Long.parseLong(splits[1]);
			// 计算局部最大值
			if(max < num){
				max = num;
				maxValueOut.set(name + "\001" + max);
			}
			// 计算出局部最小值，当第一次比较的时候无论如何都必须给Min值赋值，并将isFirst设置为false
			if(min > num || isFirst){
				isFirst = false;
				min = num;
				minValueOut.set(name + "\001" + min);
			}
		}
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			// 将每个map值最大的输出
			context.write(new Text("max"), maxValueOut);
			// 将每个map值最大的输出
			context.write(new Text("min"), minValueOut);
		}
	}
	
	
	/**
	 * reduce 阶段
	 */
	public static class MaxMinWordReducer extends Reducer<Text, Text, Text, Text>{
		Text valueOut = new Text();
		long max = 0L;
		
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

			if("max".equals(key.toString())){
				for (Text value : values) {
					String v = value.toString();
					
					String[] splits = v.split("\001");
					String name = splits[0];
					long num = Long.parseLong(splits[1]);
					if(max < num){
						max = num;
						valueOut.set(v);
					}
				}
				context.write(new Text("max"), valueOut);
			}else{
				long min = 0L;
				boolean isFirst =true;
				for (Text value : values) {
					String v = value.toString();
					
					String[] splits = v.split("\001");
					String name = splits[0];
					long num = Long.parseLong(splits[1]);
					if(min > num || isFirst){
						isFirst = false;  // 第一次无论如何都要进行赋值
						min = num;
						valueOut.set(v);
					}
				}
				context.write(new Text("min"), valueOut);
			}
		}
	}
	

	/**
	 * job 运行类
	 */
	public int run(String[] args) throws Exception {
		// 获取 configuration 对象
		Configuration conf = getConf();
		
		// 创建job对象
		Job job = Job.getInstance(conf, "MaxMinWord");
		
		// 设置job运行类
		job.setJarByClass(MaxMinWord.class);
		
		// 设置map reduce运行的类
		job.setMapperClass(MaxMinWordMapper.class);
		job.setReducerClass(MaxMinWordReducer.class);
		
		// 设置 map 输出的key value的类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		
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
		// 运行参数： D:/tmp/input/maxmin C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new MaxMinWord(), args);
	}

}
