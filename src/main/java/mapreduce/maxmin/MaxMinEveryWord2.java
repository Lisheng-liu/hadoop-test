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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * 坑坑坑坑坑坑
 * 
 * 同时求每组词的最大值和每组词的最小值, 利用自定义序列化类来作为map输出的value类型
 * 
 * 步骤：
 * map阶段：
 * key:     单词   Text
 * value:   num WordWritable
 * 
 * map(): 正常输出
 * 
 * 
 * reduce 阶段：
 * reduce()：有多少种单词就调用多少次reduce()
 * 计算每个单词的最大值最小值并输出【全局】
 * 
 * job参数设置：
 *    设置combiner的class
 */
public class MaxMinEveryWord2 extends Configured implements Tool{
	
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
	 * reduce 阶段
	 */
	public static class MaxMinEveryWordReducer extends Reducer<Text, WordWritable, Text, Text>{
		Text valueOut = new Text();
		
		@Override
		protected void reduce(Text key, Iterable<WordWritable> values, Context context) throws IOException, InterruptedException {

			List<WordWritable> list = new ArrayList<WordWritable>();
			List<Long> list2 = new ArrayList<Long>();

			StringBuilder sb = new StringBuilder("reduce input ==> key:" + key.toString() + "; values[");
			for (WordWritable value : values) {
				long num = value.getNum();
				sb.append(num).append(", ");

				// 当把Iterable<WordWritable> 里面的 WordWritable 对象装到list里，其实这个对象都指向一块内存地址，
				// 导致list添加的数据不是自己想要的。
				list.add(value);
				// 如果想添加想要的数据，最好创建一个跟它一样的对象，将数据拷贝到 新对象里；如果想要某个属性值，可以把属性值拷贝出来，
				// 以免用同一块地址
				list2.add(value.getNum());
			}
			sb.deleteCharAt(sb.length() - 1).append("]");
			System.out.println(sb.toString());
			// 输出结果与预期结果不符
			for (WordWritable w : list) {
				System.out.println("==========>" + w.getNum());
			}
			// 输出结果与预期结果相符			
			System.err.println(Arrays.toString(list2.toArray()));
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
		job.setJarByClass(MaxMinEveryWord2.class);
		
		// 设置map reduce combiner 运行的类
		job.setMapperClass(MaxMinEveryWordMapper.class);
		job.setReducerClass(MaxMinEveryWordReducer.class);
		
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
		boolean status = job.waitForCompletion(false);
		
		return status ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
		// 运行参数 ：  D:/tmp/input/maxmin C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new MaxMinEveryWord2(), args);
	}
	
}
