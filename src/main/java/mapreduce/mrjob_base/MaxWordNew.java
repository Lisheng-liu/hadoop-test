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
 * maxword任务， 继承 BaseMR，实现 getjob() 和 getJobName() 
 */
public class MaxWordNew extends BaseMR{
	
	/**
	 * map 阶段
	 */
	public static class MaxWordMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
		Text keyOut = new Text();
		long max = 0L;
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split("\t");

			if(splits == null || splits.length != 2){
				context.getCounter("zss", "bad line num").increment(1L);
			}
			String name = splits[0];
			long num = Long.parseLong(splits[1]);
			if(max < num){
				max = num;
				keyOut.set(name);
			}
		}
		
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			// 将每个map值最大的输出
			context.write(keyOut, new LongWritable(max));
		}
	}
	
	/**
	 * reduce阶段
	 */
	public static class MaxWordReducer extends Reducer<Text, LongWritable, Text, LongWritable>{
		
		Text keyOut = new Text(); 
		long max = 0L;

		@Override
		protected void reduce(Text key, Iterable<LongWritable> values,Context context) throws IOException, InterruptedException {

			for(LongWritable w : values){
				long n = w.get();
				if(max < n){
					max = n;
					keyOut.set(key.toString());
				}
			}
		}
		
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			context.write(keyOut, new LongWritable(max));
		}
	}

	@Override
	public Job getJob(Configuration conf2) throws IOException {
//		创建Job对象
		Job job = Job.getInstance(conf, getJobNameWithTaskid());
		
//		设置job运行的类
		job.setJarByClass(MaxWordNew.class);
		
//		设置map reduce 运行类
		job.setMapperClass(MaxWordMapper.class);
		job.setReducerClass(MaxWordReducer.class);
		
//		设置map输出的key value 的类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
		
//		设置reduce最终输出的key value 的类型
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
		
//		设置任务的输入目录，其实就是上个依赖任务的输出目录
		WordCountNew wc = new WordCountNew();
		FileInputFormat.addInputPath(job, wc.getJobOutputPath(wc.getJobNameWithTaskid()));
		
//		设置任务的输出目录
		FileOutputFormat.setOutputPath(job, getJobOutputPath(getJobNameWithTaskid()));
		
		return job;
	}

	@Override
	public String getJobName() {
		return "maxword";
	}
}
