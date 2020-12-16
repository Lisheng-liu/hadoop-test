package mapreduce.util;

/**
 * 常量类
 */
public class Constants {
	
	/**
	 * 首个任务的输入目录， -D参数传递
	 */
	public static final String TASK_INPUT_DIR_ATTR = "task.input.dir";
	
	/**
	 * 个性化的任务id，-D参数传递
	 */
	public static final String TASK_ID_ATTR = "task.id";
	
	
	/**
	 * 任务输出目录的公共父目录，-D参数传递
	 */
	public static final String TASK_BASE_DIR_ATTR = "task.base.dir";
	
	public static final String HBASE_TABLE_DIR_ATTR = "task.hbase.table.dir";

}
