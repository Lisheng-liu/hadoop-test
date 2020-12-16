package mapreduce.score;

import mapreduce.writable.WordKeyWritable;
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
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;


/**
 * 二次排序：主关键字升序，次关键字降序
 * 
 * 1） 自定义序列化类 WordKeyWritable 实现二次排序的逻辑。
 * 
 * 2）map阶段输出
 * 		key: WordKeyWritable  word^Anum
 * 		value:  Text          word#num
 * 
 * 3）reduce阶段输出
 * 		key: Text   word
 * 		value: LongWriable num
 * 
 * 4）reduce 阶段，在调用reduce() 前，会调用分组排序，来按照key进行聚合
 * 			默认的情况下，是直接调用 WordKeyWritable（map的key）的 排序逻辑；
 * 
 * 			如果想按照主关键字来分组，这时需要外部比价器来实现，同时需要设置 分组排序的比较器类
 * 			SortGroupComparator extends WritableComparator
 * 			job.setGroupingComparatorClass(SortGroupComparator.class);
 * 
 * @author 郑松松
 * @time   2019年8月4日 下午4:23:29
 */
public class SecondarySort extends Configured implements Tool{
	
	/**
	 * map阶段
	 */
	public static class SecondarySortMapper extends Mapper<LongWritable, Text, WordKeyWritable, Text>{
		WordKeyWritable keyOut = new WordKeyWritable();
		Text valueOut = new Text();
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split("\t");

			if(splits == null || splits.length != 2){
				context.getCounter("zss", "bad line num").increment(1L);
				return;
			}
			
			String word = splits[0];
			long num = Long.parseLong(splits[1]);
			
			keyOut.setFirstKey(new Text(word));
			keyOut.setSecondKey(num);
			
			valueOut.set(word + "#" + num);
			context.write(keyOut, valueOut);
		}
	}
	
	/**
	 * 该逻辑比较主关键字，用于reduce排序分组时按照主关键字排序
	 */
	public static class SortGroupComparator extends WritableComparator{

		public SortGroupComparator(){
			super(WordKeyWritable.class, true);
		}

		@Override
		public int compare(WritableComparable a, WritableComparable b) {
			WordKeyWritable wa = (WordKeyWritable)a;
			WordKeyWritable wb = (WordKeyWritable)b;
			return wa.getFirstKey().compareTo(wb.getFirstKey());
		}
	}
	
	
	public static class SecondarySortReducer extends Reducer<WordKeyWritable, Text, Text, LongWritable>{
		Text keyOut = new Text();
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void reduce(WordKeyWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

			for (Text t : values) {
				String[] splits = t.toString().split("#");
				
				String word = splits[0];
				long num = Long.parseLong(splits[1]);
				
				keyOut.set(word);
				valueOut.set(num);
				context.write(keyOut, valueOut);
			}
		}
	}

	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		Job job = Job.getInstance(conf, "SecondarySort");
		job.setJarByClass(SecondarySort.class);
		
		job.setMapperClass(SecondarySortMapper.class);
		job.setReducerClass(SecondarySortReducer.class);
		
		//【重点】 指定reduce分组排序比较器类
		job.setGroupingComparatorClass(SortGroupComparator.class);
		
		job.setMapOutputKeyClass(WordKeyWritable.class);
		job.setMapOutputValueClass(Text.class);
	
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		Path outputDir = new Path(args[1]);
		// 增加自动删除输出目录
		// 获取文件系统对象，通过文件系统对象来操作删除目录
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
		// 运行参数  D:/tmp/input/score C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new SecondarySort(), args);
	}

}
