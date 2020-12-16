package mapreduce.innerjoin;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 两个文件进行innerjoin
 * 
 * 步骤：
 * 
 * job参数设置时，每个文件指定一个map类：
 * 		MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, M1Mapper.class);
		MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, M2Mapper.class);
		
	注意：传入参数的变化
 * 
 * map阶段：每个map类的对象处理指定的文件，设置标记。 m1.txt  --> m1  ,  m2.txt --> m2
 * map(): 输出时，
 * 		key: id
 * 		value:  字符串数据  + \001 + 哪个文件的标记
 * 
 * reduce阶段：
 * 		对每个key，按照不同的标记，将对应的value值放到每个list里面
 * 		再将两个list进行join
 */
public class InnerJoinMultiInput extends Configured implements Tool{
	
	/**
	 * map 阶段
	 */
	public static class M1Mapper extends Mapper<LongWritable, Text, LongWritable, Text>{
		String type = "m1";
		LongWritable keyOut = new LongWritable();
		Text valueOut = new Text();
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split("\t");

			long id = Long.parseLong(splits[0]);
			String name = splits[1];
			
			keyOut.set(id);
			valueOut.set(name + "\001" + type);
			context.write(keyOut, valueOut);
		}
	}
	
	
	/**
	 * map 阶段
	 */
	public static class M2Mapper extends Mapper<LongWritable, Text, LongWritable, Text>{
		String type = "m2";
		LongWritable keyOut = new LongWritable();
		Text valueOut = new Text();
		
		@Override
		protected void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String[] splits = value.toString().split("\t");
			long id = Long.parseLong(splits[0]);
			String name = splits[1];
			
			keyOut.set(id);
			valueOut.set(name + "\001" + type);
			context.write(keyOut, valueOut);
		}
	}
	
	
	/**
	 * reducer
	 */
	public static class InnerJoinReducer extends Reducer<LongWritable, Text, LongWritable, Text>{
		// 装 m1 的数据
		List<String> list1 = new ArrayList<String>();
		// 装 m2 的数据
		List<String> list2 = new ArrayList<String>();
		Text valueOut = new Text();
		
		@Override
		protected void reduce(LongWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			// 清除之前的数据
			list1.clear();
			list2.clear();

			for (Text t : values) {
				String[] splits = t.toString().split("\001");
				String name = splits[0];
				String type = splits[1];
				if("m1".equals(type)){
					list1.add(name);
				}else{
					list2.add(name);
				}
			}
			// join的逻辑
			for (String t1 : list1) {
				for (String t2 : list2) {
					valueOut.set(t1 + "\001" + t2);
					context.write(key, valueOut);
				}
			}
		}
	}


	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		Job job = Job.getInstance(conf, "innerjoinMultiInput");
		job.setJarByClass(InnerJoinMultiInput.class);
		
		job.setReducerClass(InnerJoinReducer.class);
		
		job.setMapOutputKeyClass(LongWritable.class);
		job.setMapOutputValueClass(Text.class);
		
		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(Text.class);
		
		MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, M1Mapper.class);
		MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, M2Mapper.class);
		
		Path outputDir = new Path(args[2]);
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
		// 运行参数： D:/tmp/input/innerjoin/m1.txt D:/tmp/input/innerjoin/m2.txt C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new InnerJoinMultiInput(), args);
	}
}















