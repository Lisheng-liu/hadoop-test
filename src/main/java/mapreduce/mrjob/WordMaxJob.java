package mapreduce.mrjob;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.List;


/**
 * 通加高频单词任务链
 */
public class WordMaxJob extends Configured implements Tool{

	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		// 创建任务连对象
		JobControl jobc = new JobControl("wordmax");
		// 创建 WordCount 的 ControlledJob 对象
		ControlledJob wcJob = getWordCountControlledJob(args, conf);
		// 创建 MaxWord 的 ControlledJob 对象
		ControlledJob mwJob = getMaxWordControlledJob(args, conf);
		// 设置任务的依赖关系
		mwJob.addDependingJob(wcJob);
		// 把每个任务的 ControlledJob 对象 添加到任务链里
		jobc.addJob(wcJob);
		jobc.addJob(mwJob);
		
		Thread thread = new Thread() {
			@Override
			public void run(){
				long startTime = System.currentTimeMillis();
				// 循环判断任务连的任务是否完成，如果没有完成，就睡一会儿继续判断
				while(jobc.allFinished()){
					try{
						Thread.sleep(1000L);
					}catch (InterruptedException e){
						e.printStackTrace();
					}
				}
				long endTime = System.currentTimeMillis();
				// 运行到这里，代表任务连的任务已经完成
				
				System.out.println("运行时长：" + (endTime - startTime)/1000 + " s");
				
				// 获取失败的列表
				List<ControlledJob> failedJobList = jobc.getFailedJobList();
				if(failedJobList.isEmpty()){
					System.out.println("任务连运行： SUCCESS");
				}else{
					System.out.println("任务连运行： FALL");
					for (ControlledJob cjob : failedJobList) {
						String jobName = cjob.getJobName();
						System.out.println(jobName);
					}
				}
				// 停止任务链
				jobc.stop();
			}
		};
		// 运行监控线程，监控任务链执行完成， 并关闭任务连线程
		thread.start();
		// 运行任务连
		jobc.run();
		return 0;
	}

	
	/**
	 * 获取MaxWord任务的 ControlledJob对象
	 * 方法内的步骤
	 * 1）创建ControlledJob对象
	 * 2）创建指定任务的job对象
	 * 3）设置各种job运行参数
	 * 4）设置job任务的输入输出目录
	 * 5）删除job任务的输出目录
	 * @param args 运行时传递过来的目录
	 * @param conf 配置对象
	 * @return 带有任务关联关系的 ControlledJob对象
	 * @throws IOException 
	*/
	private ControlledJob getMaxWordControlledJob(String[] args, Configuration conf) throws IOException {
		
		ControlledJob mwJob = new ControlledJob(conf);
		Job job = Job.getInstance(conf, "MaxWord");
		job.setJarByClass(MaxWord.class);
		
		job.setMapperClass(MaxWord.MaxWordMapper.class);
		job.setReducerClass(MaxWord.MaxWordReducer.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
		
		// 设置任务的输入目录，其实就是上个依赖任务的输出目录
		FileInputFormat.addInputPath(job, new Path(args[1]));
		Path outputDir = new Path(args[2]);
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
			System.out.println("delete output path:" + outputDir.toString() + " successed!");
		}
		FileOutputFormat.setOutputPath(job, outputDir);
		
		// 【重要】将 maxword的job任务和mwJob 关联
		mwJob.setJob(job);
		return mwJob;
	}


	/**
	 * 获取WordCount 任务的 ControlledJob对象
	 * 方法内的步骤
	 * 1）创建ControlledJob对象
	 * 2）创建指定任务的job对象
	 * 3）设置各种job运行参数
	 * 4）设置job任务的输入输出目录
	 * 5）删除job任务的输出目录
	 * @param args 运行时传递过来的目录
	 * @param conf 配置对象
	 * @return 带有任务关联关系的 ControlledJob对象
	 * @throws IOException 
	*/
	private ControlledJob getWordCountControlledJob(String[] args, Configuration conf) throws IOException {

		ControlledJob wcJob = new ControlledJob(conf);
		
		Job job = Job.getInstance(conf, "wordcount");
		job.setJarByClass(WordCount.class);
		
		job.setMapperClass(WordCount.WordCountMapper.class);
		job.setReducerClass(WordCount.WordCountReducer.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		Path outputDir = new Path(args[1]);
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
			System.out.println("delete output path:" + outputDir.toString() + " successed!");
		}
		FileOutputFormat.setOutputPath(job, outputDir);
		
		//【重要】 将wordcount任务和 wcJob 关联
		wcJob.setJob(job);
		return wcJob;
	}
	
	public static void main(String[] args) throws Exception {
		// 运行参数： D:/tmp/input/wordcount C:/Users/song/Desktop/output/wordcount C:/Users/song/Desktop/output/wordcount1
		System.exit(ToolRunner.run(new WordMaxJob(), args));
	}
}
