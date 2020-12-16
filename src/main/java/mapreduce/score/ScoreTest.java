package mapreduce.score;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * 从1-100 分数， 统计及格的人数和不及格的人数，需要分别在两个文件中输出。
 * 
 * 步骤：
 * 
 * 1）map（）输出：
 * 		key:   name
 * 		value: score
 * 
 * 2）定义自定义partition类继承 Partitioner，并实现getPartition(), 完成个性化分区
 * 
 * 3）reduce
 * 		统计总数，并判断当前reduce是哪个的分区的，在cleanup（） 最终输出
 * 
 * 4）job参数设置
 * 		任务允许时，指定的自定义分区类。
 * 
 * @author 郑松松
 * @time   2019年8月4日 下午1:31:41
 */
public class ScoreTest extends Configured implements Tool{
	
	/**
	 * map 阶段
	 * 
	 * @author 郑松松
	 * @time   2019年8月4日 下午1:33:17
	 */
	public static class ScoreTestMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
		
		Text keyOut = new Text();
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split("\t");
			String name = splits[0];
			long score = Long.parseLong(splits[1]);
			
			valueOut.set(score);
			keyOut.set(name);
			context.write(keyOut, valueOut);
		}
	}
	
	
	/**
	 * partioner 分区
	 */
	public static class ScorePartitioner extends Partitioner<Text, LongWritable>{

		@Override
		public int getPartition(Text key, LongWritable value, int numPartitions) {

			long score = value.get();
			// 及格的数据
			if(score >= 60){
				return 0;
			}
			// 不及格的数据
			return 1;
		}
	}
	
	
	/**
	 * reduce阶段
	 */
	public static class ScoreTestReducer extends Reducer<Text, LongWritable, Text, LongWritable>{
		long count = 0L;
		String type = "";
		boolean isFirst = true;
		
		@Override
		protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
			
			for (LongWritable value : values) {
				long score = value.get();
				if(score >= 60 && isFirst){
					type = "及格人数";
					isFirst = false;
				}else if(score < 60 && isFirst){
					type = "不及格人数";
					isFirst = false;
				}
				count++;
			}
		}

		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			context.write(new Text(type), new LongWritable(count));
		}
	}
	
	
	
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		Job job = Job.getInstance(conf, "ScoreTest");
		job.setJarByClass(ScoreTest.class);
		
		job.setMapperClass(ScoreTestMapper.class);
		job.setReducerClass(ScoreTestReducer.class);
		
		job.setNumReduceTasks(2);
		
		job.setPartitionerClass(ScorePartitioner.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
	
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);

		// 删除目录
		FileInputFormat.addInputPath(job, new Path(args[0]));
		Path outputDir = new Path(args[1]);
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
			System.out.println("delete output path:" + outputDir.toString() + " successed!");
		}
		// 设置任务的输出目录
		FileOutputFormat.setOutputPath(job, outputDir);
		
		// 运行job任务, 不打印counter  
		boolean status = job.waitForCompletion(true);
		
		return status ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
//		运行参数  D:/tmp/input/score C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new ScoreTest(), args);
	}
}

