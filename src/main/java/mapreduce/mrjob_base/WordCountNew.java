package mapreduce.mrjob_base;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

/**
 * wordcount任务， 继承 BaseMR，实现 getjob() 和 getJobName()
 */
public class WordCountNew extends BaseMR{
	
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

			for(String word : splits){
				keyOut.set(word);
				context.write(keyOut, valueOut);
			}
		}
	}
	
	/**
	 * reduce 阶段
	 */
	public static class WordCountReducer extends Reducer<Text, LongWritable, Text, LongWritable>{
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void reduce(Text key, Iterable<LongWritable> values,Context context) throws IOException, InterruptedException {

			long sum = 0L;
			for(LongWritable w : values){
				long n = w.get();
				sum += n;
			}
			valueOut.set(sum);
			context.write(key, valueOut);
		}
	}

	@Override
	public Job getJob(Configuration conf) throws IOException {
		//【重点】
		Job job = Job.getInstance(conf, getJobNameWithTaskid());
		
//		设置job运行的类
		job.setJarByClass(WordCountNew.class);
		
//		设置map reduce 运行类
		job.setMapperClass(WordCountMapper.class);
		job.setReducerClass(WordCountReducer.class);
		
//		设置map输出的key  value 的类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
		
//		设置reduce最终输出的key value 的类型
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
		
//		【重点】设置任务的输入目录,首个任务的输入
		FileInputFormat.addInputPath(job, getFirstJobInputPath());

//		【重点】设置任务的输出目录： 例如：/tmp/mr/task/wordcount_0123
		FileOutputFormat.setOutputPath(job, getJobOutputPath(getJobNameWithTaskid()));
		
		return job;
	}

	@Override
	public String getJobName() {
		return "wordcount";
	}
}
