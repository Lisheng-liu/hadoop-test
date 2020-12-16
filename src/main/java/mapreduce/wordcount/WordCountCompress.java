package mapreduce.wordcount;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * compress 输出压缩
 * 
 * @author 郑松松
 * @time   2019年7月30日 下午10:26:51
 */
public class WordCountCompress  extends Configured implements Tool{
	
	/**
	 * map 阶段
	 */
	public static class WordCountMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
		Text keyOut = new Text();
		LongWritable valueOut = new LongWritable(1L);
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String line = value.toString();
			String[] splits = line.split(" ");
			for (String word : splits) {
				keyOut.set(word);
				context.write(keyOut, valueOut);
			}
		}
	}
	
	
	/**
	 * reduce 阶段
	 */
	public static class WordCountReduce extends Reducer<Text, LongWritable, Text, LongWritable>{
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {

			long sum = 0L;
			for (LongWritable value : values) {
				long n = value.get();
				sum += n;
			}
			valueOut.set(sum);
			context.write(key, valueOut);
		}
	}

	

	public int run(String[] args) throws Exception {
		// 获取 Configuration 对象，用于创建 MapReduce 任务的Job 对象
		Configuration conf = getConf();
		
//在创建Job对象之前，通过conf.set() 方式可以设置job参数。
//当创建Job对象时，会将conf 复制出来一份作为job对象的一个属性，此时，conf 就没有用了。
//所以在创建job对象后，如果想直接通过configuration设置job参数，需要用job.getConfiguration()
		
//【reduce】输出压缩
//		conf.set("mapreduce.output.fileoutputformat.compress", "true");
//		conf.set("mapreduce.output.fileoutputformat.compress.codec", GzipCodec.class.getName());
		
//【map】输出压缩  通过日志查看
//		conf.set(MRJobConfig.MAP_OUTPUT_COMPRESS, "true");
//		conf.set(MRJobConfig.MAP_OUTPUT_COMPRESS_CODEC, SnappyCodec.class.getName());		
		
		// 创建 job 对象
		Job job = Job.getInstance(conf, "wordcountcompress");
		
		// 设置 job 运行类
		job.setJarByClass(WordCountCompress.class);
		
		// 设置 map reduce 运行类
		job.setMapperClass(WordCountMapper.class);
		job.setReducerClass(WordCountReduce.class);
		
		// 设置map reduce 输出类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
		
// 【reduce】输出压缩
//		设置reduce 输出压缩
//		FileOutputFormat.setCompressOutput(job, true);
//		设置reduce 输出压缩格式
//		FileOutputFormat.setOutputCompressorClass(job, SnappyCodec.class);
		
// 【reduce】输出压缩(于上雷同)     (-D参数)
//		job.getConfiguration().set("mapreduce.output.fileoutputformat.compress", "true");
//		job.getConfiguration().set("mapreduce.output.fileoutputformat.compress.codec", SnappyCodec.class.getName());
		
		// 设置任务的输入、输出目录
		FileInputFormat.addInputPath(job, new Path(args[0]));
		Path outputDir = new Path(args[1]);
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outputDir)){
			fs.delete(outputDir, true);
			System.out.println("delete output path:" + outputDir.toString() + "success!");
		}
		FileOutputFormat.setOutputPath(job, outputDir);
		
		// 运行的时候，不打印counter = false
		boolean status = job.waitForCompletion(false);
		return status ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
		// 运行参数： D:/tmp/input/wordcount D:/workspace/tmp/output
		// -D参数       -Dmapreduce.output.fileoutputformat.compress=true -Dmapreduce.output.fileoutputformat.compress.codec=org.apache.hadoop.io.compress.SnappyCodec D:/tmp/input/wordcount C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new WordCountCompress(), args);
	}
}
