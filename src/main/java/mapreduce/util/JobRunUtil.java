package mapreduce.util;

import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 运行任务链和监控任务链的执行情况及停止任务链的工具类
 */
public class JobRunUtil {

	/**
	 * 开启任务链的线程,开启监控任务链的线程，并返回任务链执行结果
	 * @param jobc
	 * @return
	 * @throws Exception 
	*/
	public static JobRunResult run(JobControl jobc) throws Exception {
		// 开启任务链的线程
		new Thread(jobc).start();
		
		// 开启监控任务链的线程，并返回任务链执行结果
		// 创建MonitorAndStopJobControlCallable对象
		MonitorAndStopJobControlCallable monitorCallable = new MonitorAndStopJobControlCallable(jobc);
		
		// 用FutureTask 包装
		FutureTask<JobRunResult> futureTask = new FutureTask<JobRunResult>(monitorCallable);
		
		// 再用线程包装，并启动 
		new Thread(futureTask).start();
		
		// 通过.get()获取线程的返回对象， 并且此方法是个阻塞的方法，直到任务链运行完成后，才会返回结果
		return futureTask.get();
		
	}
	
	public static class MonitorAndStopJobControlCallable implements Callable<JobRunResult>{
		private JobControl jobc;
		
		public MonitorAndStopJobControlCallable(JobControl jobc) {
			this.jobc = jobc;
		}
		
		@Override
		public JobRunResult call() throws Exception {
			long startTime = System.currentTimeMillis();
			//循环判断任务链的任务是否完成，如果没完成，就睡一会继续判断
			while (!jobc.allFinished()){
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			long endTime = System.currentTimeMillis();
			//运行到这，代表任务链的任务运行完成。
			
			JobRunResult result = new JobRunResult();
			result.setRunTime(formatTime(endTime - startTime));
			
			//获取失败列表
			List<ControlledJob> failedJobList = jobc.getFailedJobList();
			if(failedJobList.isEmpty()){
				//任务链上的任务都运行成功
				result.setSuccessFlag(true);
			}else{
				//任务链有任务运行失败
				result.setSuccessFlag(false);
				for(ControlledJob cjob : failedJobList){
					String jobName = cjob.getJobName();
					result.addFailedJobName(jobName);
				}
			}
			
			//将成功的任务的counters放到map中
			List<ControlledJob> successfulJobList = jobc.getSuccessfulJobList();
			for(ControlledJob cjob : successfulJobList){
				String jobName = cjob.getJobName();
				Counters counters = cjob.getJob().getCounters();
				//添加对应任务名称的counters对象
				result.putCounters(jobName, counters);
			}
			//停止任务链
			jobc.stop();
			return result;
		}
	}

	
	/**
	 * 将毫秒数格式化成 x天x小时x分x秒
	 * @param runTime
	 * @return TODO(这里描述每个参数,如果有返回值描述返回值,如果有异常描述异常)
	*/
	public static String formatTime(long runTime) {
		long days = runTime/(1000 * 60 * 60 * 24); 
//		System.out.println("day:" + day);
		long hours = runTime % (1000 * 60 * 60 * 24) / (1000 * 60 * 60);
//		System.out.println("hours:" + hours);
		long minutes = runTime % (1000 * 60 * 60) / (1000 * 60);
//		System.out.println("minutes:" + minutes);
		long seconds = runTime % (1000 * 60) / 1000;
//		System.out.println("seconds:" + seconds);
		StringBuilder sb = new StringBuilder();
		if(days != 0){
			sb.append(days).append("天");
		}
		if(hours != 0){
			sb.append(hours).append("小时");
		}
		if(minutes != 0){
			sb.append(minutes).append("分");
		}
		if(seconds != 0){
			sb.append(seconds).append("秒");
		}
		return sb.toString();
	}
}

