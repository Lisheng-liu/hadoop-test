package mapreduce.score;

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
 * 解决key不均衡问题
 * 
 * 步骤：
 * 1）map（）输出：
 * 		key:  song#随机数  或  song#序列
 * 		value: num
 * 
 * 通过这样的输出，多个reduce拉取不同分区的数据
 * 
 * 2）reduce阶段：
 *    把song字段给摘出来
 *    
 * 3）job参数设置：
 * 		设置reduce个数。
 * 
 * @author 郑松松
 * @time   2019年8月4日 下午3:59:28
 */
public class PartitionTest extends Configured implements Tool{
	
	/**
	 * map 阶段
	 */
	public static class PartitionTestMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
		Text keyOut = new Text();
		LongWritable valueOut = new LongWritable();
		long sequence = 10000L;
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split("\t");
			String word = splits[0];
			long num = Long.parseLong(splits[1]);
			
//			keyOut.set(word + "\001" + sequence++);
			keyOut.set(word + "\001" + Math.random());
//			keyOut.set(word);  // 5个输出只有一个有信息，    不可取
			valueOut.set(num);
			context.write(keyOut, valueOut);
		}
	}
	
	
	/**
	 * partition 阶段
	 */
	public static class PartitionTestReducer extends Reducer<Text, LongWritable, Text, LongWritable>{
		Text keyOut = new Text();
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {

			String[] splits = key.toString().split("\001");
			String word = splits[0];

			for (LongWritable value : values) {
				long num = value.get();
				keyOut.set(word);
				valueOut.set(num);
				context.write(keyOut, valueOut);
			}
		}
	}
	

	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		Job job = Job.getInstance(conf, "PartitionTest");
		job.setJarByClass(PartitionTest.class);
		
		job.setMapperClass(PartitionTestMapper.class);
		job.setReducerClass(PartitionTestReducer.class);
		
		//【重要】
		job.setNumReduceTasks(5);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
	
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		Path outputDir = new Path(args[1]);
//		增加自动删除输出目录
//		获取文件系统对象，通过文件系统对象来操作删除目录
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
			System.out.println("delete output path:" + outputDir.toString() + " successed!");
		}
//		设置任务的输出目录
		FileOutputFormat.setOutputPath(job, outputDir);
		
//		运行job任务, 不打印counter  
		boolean status = job.waitForCompletion(true);
		
		return status ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
//		运行参数   D:/tmp/input/score C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new PartitionTest(), args);
	}

}
