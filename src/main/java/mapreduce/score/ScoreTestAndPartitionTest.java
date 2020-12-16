package mapreduce.score;

import mapreduce.writable.NumWritable;
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
 * 从1-100 分数， 按照及格、不及格 两种分别降序排序，并输出到两个文件中
 * 
 * 步骤：
 * 1）map（）输出：
 * 		key:   score  --> NumWritable（实现降序排序，是通过内部比较器实现的）
 * 		value: name^Ascore  --> Text
 * 
 * 2）定义自定义partition类继承 Partitioner，并实现getPartition(), 完成个性化分区
 * 
 * 3）reduce:用 hadoop 自带的reduce
 * 
 * 4）job参数设置
 * 		任务允许时，指定的自定义分区类。
 * 
 * @author 郑松松
 * @time   2019年8月4日 下午1:31:41
 */
public class ScoreTestAndPartitionTest extends Configured implements Tool{
	
	/**
	 * map 阶段
	 */
	public static class ScoreTestMapper extends Mapper<LongWritable, Text, NumWritable, Text>{
		
		NumWritable keyOut = new NumWritable();
		Text valueOut = new Text();
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split("\t");
			String name = splits[0];
			long score = Long.parseLong(splits[1]);
			
			keyOut.setNum(score);
			valueOut.set(name + "\001" + score);
			context.write(keyOut, valueOut);
		}
	}
	
	
	/**
	 * partioner 分区
	 */
	public static class ScorePartitioner extends Partitioner<NumWritable, Text>{

		@Override
		public int getPartition(NumWritable key, Text value, int numPartitions) {
			long score = key.getNum();
			// 及格的数据
			if(score >= 60){
				return 0;
			}
			// 不及格的数据
			return 1;
		}
	}
	
	
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		Job job = Job.getInstance(conf, "ScoreTest");
		job.setJarByClass(ScoreTestAndPartitionTest.class);
		
		job.setMapperClass(ScoreTestMapper.class);
		job.setReducerClass(Reducer.class);
		
		//【重要】
		job.setNumReduceTasks(2);
		//【重要】
		job.setPartitionerClass(ScorePartitioner.class);
		
		job.setMapOutputKeyClass(NumWritable.class);
		job.setMapOutputValueClass(Text.class);
	
		job.setOutputKeyClass(NumWritable.class);
		job.setOutputValueClass(Text.class);

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
		ToolRunner.run(new ScoreTestAndPartitionTest(), args);
	}
}

