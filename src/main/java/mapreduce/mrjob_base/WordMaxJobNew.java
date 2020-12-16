package mapreduce.mrjob_base;

import mapreduce.util.JobRunResult;
import mapreduce.util.JobRunUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * 通加高频单词任务链
 */
public class WordMaxJobNew extends Configured implements Tool{

	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		// 创建任务连对象
		JobControl jobc = new JobControl("wordmax");
		
		// 创建 WordCount 的 对象
		WordCountNew wc = new WordCountNew();
		// 设置conf对象，只设置一次就行
		wc.setConf(conf);
		
		// 获取wordcount 任务的 ControlledJob 对象
		ControlledJob wcJob = wc.getControlledJob();
		
		// 创建 MaxWord 的对象
		MaxWordNew mw = new MaxWordNew();
		// 获取maxword 任务的ControlledJob对象
		ControlledJob mwJob = mw.getControlledJob();
		
		// 设置任务的依赖关系
		mwJob.addDependingJob(wcJob);
		
		// 把每个任务的 ControlledJob 对象 添加到任务链里
		jobc.addJob(wcJob);
		jobc.addJob(mwJob);
		
		JobRunResult result = JobRunUtil.run(jobc);
		result.print(true);
		
		return 0;
	}

	public static void main(String[] args) throws Exception {
		// 运行参数： -Dtask.input.dir=D:/tmp/input/wordcount -Dtask.id=0804 -Dtask.base.dir=C:/Users/song/Desktop/output/wordcount
		System.exit(ToolRunner.run(new WordMaxJobNew(), args));
	}
}
