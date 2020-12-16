package mapreduce.multioutput;

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
 * compress 输出压缩
 * 
 * @author 郑松松
 * @time   2019年7月30日 下午10:26:51
 */
public class WordCountCompressMultiOutput  extends Configured implements Tool{
	
	/**
	 * map 阶段
	 */
	public static class WordCountMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
		Text keyOut = new Text();
		LongWritable valueOut = new LongWritable(1L);
		MultipleOutputs<Text, LongWritable> outputs = null;
		
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			outputs = new MultipleOutputs<Text, LongWritable>(context);
		}
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String line = value.toString();
			String[] splits = line.split(" ");
			for (String word : splits) {
				keyOut.set(word);

				// map输出数据
				context.write(keyOut, valueOut);
				outputs.write(keyOut, valueOut, "mapoutputs/out");
			}
		}
		
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			outputs.close();
		}
	}
	
	
	/**
	 * reduce 阶段
	 */
	public static class WordCountReduce extends Reducer<Text, LongWritable, Text, LongWritable>{
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {

			long sum = 0L;
			for (LongWritable w : values) {
				long n = w.get();
				sum += n;
			}
			
			valueOut.set(sum);
			context.write(key, valueOut);
		}
	}

	

	public int run(String[] args) throws Exception {
		// 获取 Configuration 对象，用于创建 MapReduce 任务的Job 对象
		Configuration conf = getConf();
		
		// 创建 job 对象
		Job job = Job.getInstance(conf, "WordCountCompressMultiOutput");
		
		// 设置 job 运行类
		job.setJarByClass(WordCountCompressMultiOutput.class);
		
		// 设置 map reduce 运行类
		job.setMapperClass(WordCountMapper.class);
		job.setReducerClass(WordCountReduce.class);
		
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
		boolean status = job.waitForCompletion(false);
		return status ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
		// 运行参数： D:/tmp/input/wordcount C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new WordCountCompressMultiOutput(), args);
	}
}
