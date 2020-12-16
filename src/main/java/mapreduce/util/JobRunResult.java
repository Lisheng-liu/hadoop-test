package mapreduce.util;


import org.apache.hadoop.mapreduce.Counters;

import java.util.*;
import java.util.Map.Entry;

/**
 * 任务链运行结果封装类
 */
public class JobRunResult {
	/**
	 * 任务链运行任务是否都成功
	 * true：成功； false:失败
	 */
	private boolean successFlag;
	
	
	/**
	 * 任务链运行时长， 格式化： x天x小时x分x秒
	 */
	private String runTime;
	
	
	/**
	 * 失败的任务名称列表
	 */
	private List<String> failedJobNames = new ArrayList<String>();
	
	/**
	 * 装的是成功任务的counters信息
	 * key:jobname
	 * value:对应jobname的counters对象
	 */
	private Map<String, Counters> counterMap = new HashMap<String, Counters>();


	public boolean isSuccessFlag() {
		return successFlag;
	}


	public void setSuccessFlag(boolean successFlag) {
		this.successFlag = successFlag;
	}


	public String getRunTime() {
		return runTime;
	}


	public void setRunTime(String runTime) {
		this.runTime = runTime;
	}


	public List<String> getFailedJobNames() {
		return failedJobNames;
	}


	public void addFailedJobName(String jobName) {
		this.failedJobNames.add(jobName);
	}
	
	
	public void putCounters(String jobName, Counters counters){
		this.counterMap.put(jobName, counters);
	}
	
	public void print(boolean isPrintCounter){
		StringBuilder sb = new StringBuilder();
		sb.append("任务链运行时长：").append(this.runTime).append("\n");
		sb.append("任务链运行：").append(this.successFlag ? "SUCCESS" : "FAIL").append("\n");
		if(!this.successFlag){
			sb.append("失败任务名称列表：").append(Arrays.toString(this.failedJobNames.toArray())).append("\n");
		}
		
		if(isPrintCounter){
			sb.append("打印任务的counters信息:\n----------------------\n");
			for(Entry<String, Counters> entry : this.counterMap.entrySet()){
				String jobName = entry.getKey();
				Counters counters = entry.getValue();
				sb.append("任务名称：").append(jobName).append("\n");
				sb.append(counters.toString()).append("\n");
				sb.append("-----------------------------\n");
			}
		}
		System.out.println(sb.toString());
	}
}

