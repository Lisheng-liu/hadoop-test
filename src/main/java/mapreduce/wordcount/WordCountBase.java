package mapreduce.wordcount;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * WordCountBase  单词统计（打印日志）
 * 
 * @author 郑松松
 * @time   2019年7月30日 下午4:57:54
 */
public class WordCountBase extends Configured implements Tool{
	
	/**
	 * map 阶段
	 */
	public static class WordCountMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
		Text keyOut = new Text();
		LongWritable valueOut = new LongWritable(1L);
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			System.out.println("-----------map()-------------");
			
			// 日志
			// 一行加一次， 用counert 统计mapreduce输入的数据的行数
			context.getCounter("zss", "line num").increment(1L);
			
			System.out.println("map input ==> keyin:" + key.get() + "; valuein:" + value.toString());
			String line = value.toString();
			String[] splits = line.split(" ");
			for (String word : splits) {
				keyOut.set(word);
				
				// map输出数据
				context.write(keyOut, valueOut);
				System.out.println("map output ==> key:" + word + "; value:" + valueOut.get());
			}
		}
	}
	
	
	/**
	 * reduce 阶段
	 */
	public static class WordCountReducer extends Reducer<Text, LongWritable, Text, LongWritable>{
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
			System.out.println("-----------reduce()-----------");
			
			// 日志
			// 一种key调用一次reduce(), 调用一次加一次
			context.getCounter("zss", "key types num").increment(1L);
			
			StringBuilder sb = new StringBuilder("reduce input ==> key:" + key.toString() + "; values[");
			long sum = 0L;
			for (LongWritable w : values) {
				long n = w.get();
				sum += n;
				sb.append(n).append(",");
			}
			sb.deleteCharAt(sb.length() - 1).append("]");
			System.out.println(sb.toString());
			valueOut.set(sum);
			context.write(key, valueOut);
		}
	}
	
	
	public int run(String[] args) throws Exception {
		// 获取 Configuration 对象，用于创建 MapReduce 任务的Job 对象
		Configuration conf = getConf();

		// 创建 job 对象
		Job job = Job.getInstance(conf, "wordcountbase");

		// 设置 job 运行类
		job.setJarByClass(WordCountBase.class);

		// 设置 map reduce 的运行类
		job.setMapperClass(WordCountMapper.class);
		job.setReducerClass(WordCountReducer.class);

		// 设置【reduce】运行的个数, 默认一个可以不写
		job.setNumReduceTasks(1);

		// 设置map 输出的key value 的类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);

		// 设置reduce 输出的key value 的类型
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);

		// 设置任务的输入目录
		FileInputFormat.addInputPath(job, new Path(args[0]));
		Path outputDir = new Path(args[1]);
		// 自动删除输出目录
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
			System.out.println("delete output path:" + outputDir.toString() + "success!");
		}
		// 设置目录的输出目录
		FileOutputFormat.setOutputPath(job, outputDir);

		// 运行的时候，不打印counter = false
		boolean status = job.waitForCompletion(true);

		// 自定义counter
		// 【提交任务后】
		Counters counters = job.getCounters();
		CounterGroup group = counters.getGroup("zss");

		StringBuilder sb = new StringBuilder();
		sb.append("\t").append("zss").append("\n");
		// 遍历 CounterGroup 下的所有的counter
		for (Counter counter : group) {
			sb.append("\t\t").append(counter.getDisplayName()).append("=").append(counter.getValue()).append("\n");
		}
		// 找指定的counter
		long num = group.findCounter("line num").getValue();
		System.out.println("\t\tline num1 = " + num);

		System.out.println("--------------------");
		System.out.println(sb.toString());

		return status ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
		// 运行参数： D:/tmp/input/wordcount C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new WordCountBase(), args);
	}
}

