package mapreduce.sort;

import mapreduce.writable.NumWritable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;


/**
 * 降序排序 ： map输出的key是自定义系列化类
 * 步骤：
 * 		1） 创建一个带有内部比较器的自定义序列化类 （NumWritable）
 * 		1: 自定义序列化类自实现降序， 此时不需要外部比较器来处理
 * 
 * 步骤：
 * 处理方法参考hadoop 自带的序列化类
 * 		自定义序列化类自实现升序， 通过外部比较器实现降序
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
public class DescSort2 extends Configured implements Tool{

	/**
	 * map 阶段
	 */
	public static class DescSortMapper extends Mapper<LongWritable, Text, NumWritable, Text>{
		
		/**
		 * key	: name
		 * value: id
		 */
		NumWritable keyOut = new NumWritable();
		Text valueOut = new Text();
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split("\t");
			String name = splits[0];
			long num = Long.parseLong(splits[1]);
			
			keyOut.setNum(num);
			valueOut.set(name + "\001" + num);
			context.write(keyOut, valueOut);
		}
	}
	
	
	
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		Job job = Job.getInstance(conf, "DescSort2");
		job.setJarByClass(DescSort2.class);
		
		job.setMapperClass(DescSortMapper.class);
		
		job.setMapOutputKeyClass(NumWritable.class);
		job.setMapOutputValueClass(Text.class);
		
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
		ToolRunner.run(new DescSort2(), args);
	}
	
}
