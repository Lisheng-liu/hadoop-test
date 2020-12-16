package mapreduce.sort;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * 降序排序
 * 
 * 步骤：
 * 
 * 1）自定义外部比较器，并继承WritableComparator ，实现compare()
 * 
 * 2) 在类内部，通过构造方法通知父类你要比较的类型，这个类型是map输出的key的类型。
 * 
 * 3）在 compare() 实现自定义降序排序
 * 
 * 4）job参数设置时，需要设置降序排序的外部比较器类。
 * 
 * @author 郑松松
 * @time   2019年8月4日 上午11:58:38
 */
public class DescSort extends Configured implements Tool{

	/**
	 * map 阶段
	 */
	public static class DescSortMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
		
		/**
		 * key	: name
		 * value: id
		 */
		LongWritable valueOut = new LongWritable();
		Text keyOut = new Text();
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split(" ");
			String name = splits[0];
			long id = Long.parseLong(splits[1]);
			
			keyOut.set(name);
			valueOut.set(id);
			context.write(keyOut, valueOut);
		}
	}
	
	
	/**
	 * 实现自定义外部比较器，实现降序逻辑
	 * 步骤：
	 * 1）通过调用父类的构造方法，指定要比较的类型是什么类型
	 * 
	 * 2）实现外部比较器的compare(), 在compare（）内部实现降序逻辑
	 */
	public static class DescSortComparator extends WritableComparator{
		// 通过调用父类的方法，指定要比较的类型是什么类型
		public DescSortComparator(){
			// true 代表创建实例
			super(Text.class, true);
		}

		@Override
		public int compare(WritableComparable a, WritableComparable b) {
			
			// 方式一     加个-号就好了
			return -super.compare(a, b);
			
			// 方式二
//			Text ta = (Text)a;
//			Text tb = (Text)b;
//			return -ta.compareTo(tb);
		}
	}


	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		Job job = Job.getInstance(conf, "DescSort");
		job.setJarByClass(DescSort.class);
		
		job.setMapperClass(DescSortMapper.class);
		
		// 【重要】指定任务的排序的外部比较器
		job.setSortComparatorClass(DescSortComparator.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		Path outputDir = new Path(args[1]);
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
			System.out.println("delete ouotput path:" + outputDir.toString() + " successed!");
		}
		FileOutputFormat.setOutputPath(job, outputDir);
		
		boolean status = job.waitForCompletion(true);
		return status ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
		// 运行参数：D:/tmp/input/sort C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new DescSort(), args);
	}
	
}
