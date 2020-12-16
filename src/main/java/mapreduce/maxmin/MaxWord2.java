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
 * maxword 例子
 * 
 * 步骤：
 * map阶段：
 * key:     max
 * value:   name#num
 * 
 * map(): 找值最大的name和num
 * 
 * cleanup()： 将最大的name和num输出,  格式： max, name#num
 * 
 * reduce 阶段：
 * reduce()：key只有"max",只调用一次reduce()就能找出全局最大，并输出。  
 *  
 */
public class MaxWord2 extends Configured implements Tool{
	
	/**
	 * map 阶段
	 */
	public static class MaxWordMapper extends Mapper<LongWritable, Text, Text, Text>{
		Text valueOut = new Text();
		long max = 0L;
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			
			String[] splits = value.toString().split("\t");
			if(splits == null || splits.length != 2){
				context.getCounter("zss", "bad line num").increment(1L);
				return;
			}
			String name = splits[0];
			long num = Long.parseLong(splits[1]);
			if(max < num){
				max = num;
				valueOut.set(name + "#" + max);
			}
		}
		
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			// 将每个map值最大的输出
			context.write(new Text("max"), valueOut);
		}
	}
	
	
	/**
	 * reduce 阶段
	 */
	public static class MaxWordReducer extends Reducer<Text, Text, Text, LongWritable>{
		Text keyOut = new Text();
		long max = 0L;
		
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			for (Text value : values) {
				String v = value.toString();
				
				String[] splits = v.split("#");
				String name = splits[0];
				long num = Long.parseLong(splits[1]);
				if(max < num){
					max = num;
					keyOut.set(name);
				}
			}
			context.write(keyOut, new LongWritable(max));
		}
	}

	
	/**
	 * job 运行类
	 */
	public int run(String[] args) throws Exception {
		// 获取 configuration 对象
		Configuration conf = getConf();
		
		// 创建job对象
		Job job = Job.getInstance(conf, "MaxWord2");
		
		// 设置job运行类
		job.setJarByClass(MaxWord2.class);
		
		// 设置map reduce运行的类
		job.setMapperClass(MaxWordMapper.class);
		job.setReducerClass(MaxWordReducer.class);
		
		// 设置 map 输出的key value的类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		
		// 设置 map 输出的key value的类型
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
		
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
		ToolRunner.run(new MaxWord2(), args);
	}
}


