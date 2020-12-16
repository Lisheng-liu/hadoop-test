package mapreduce.mrjob_base;

import mapreduce.util.Constants;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;

import java.io.IOException;


/**
 * 公共基类
 * 
 * @author 郑松松
 * @time   2019年8月4日 下午6:28:06
 */
public abstract class BaseMR {
	
	public static Configuration conf = null;

	public void setConf(Configuration conf){
		BaseMR.conf = conf;
	}
	
	/**
	 * 获取相应任务的ControlledJob对象
	 * 步骤：
	 * 1）创建ControlledJob对象
	 * 2）删除job任务的输出目录
	 * 3）获取相应任务的job对象
	 * 4）将job对象和 ControlledJob对象 关联
	*/
	public ControlledJob getControlledJob() throws IOException{
		// 创建ControlledJob对象
		ControlledJob cjob = new ControlledJob(conf);
		// 删除job任务的输出目录
		Path outputDir = getJobOutputPath(getJobNameWithTaskid());
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
			System.out.println("delete output path :" + outputDir.toString() + " successed!");
		}
		// 有子类去个性化实现
		Job job = getJob(conf);
		
		// 设置job任务对象和 ControlledJob 对象关联关系
		cjob.setJob(job);
		
		return cjob;
	}



	/**
	 * 提供抽象方法，供子类个性化实现，实现步骤：
	 * 1）创建job对象
	 * 2）设置job运行参数
	 * 3）设置job运行的输入输出目录
	*/
	public abstract Job getJob(Configuration conf2) throws IOException;


	/**
	 * 获取job任务名称,往往是单纯的任务名称
	*/
	public abstract String getJobName();

	
	/**
	 * 提供一个个性化的任务名称
	 * 用单纯的任务名称 + _ + -Dtask.id
	*/
	public String getJobNameWithTaskid() {
		return getJobName() + "_" + conf.get(Constants.TASK_ID_ATTR);
	}
	
	
	/**
	 * 获取首个任务的输入path对象，这个输入目录是通过-D参数传过来的
	*/
	public Path getFirstJobInputPath(){
		return new Path(conf.get(Constants.TASK_INPUT_DIR_ATTR));
	}
	
	
	/**
	 * 获取每个任务的输出目录path<br/>
	 * 例如：<br/>
	 * 如果jobname：wordcount, /tmp/mr/task/wordcount<br/>
	 * 如果jobname:wordcount_0123, /tmp/mr/task/wordcout_0123<br/>
	 * @param jobName 单纯的任务名称或 个性化的任务名称
	 * @return 
	*/
	public Path getJobOutputPath(String jobName) {
		return new Path(conf.get(Constants.TASK_BASE_DIR_ATTR) + "/" + jobName);
	}
	
}
