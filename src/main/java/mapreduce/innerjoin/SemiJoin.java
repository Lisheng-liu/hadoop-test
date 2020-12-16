package mapreduce.innerjoin;

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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 小文件放内存，大文件走map，在map()和内存中的小文件join。
 * 
 * 步骤：
 * 运行参数： 
 * 将小文件用 -Dmapreduce.job.cache.files 参数传递
 * -Dmapreduce.job.cache.files=/tmp/input_semijoin/small.txt /tmp/input_semijoin/big.txt /tmp/output_semijoin
 * 
 * map阶段：
 * setup():将小文件数据读取到内存；
 * 
 * map():和内存中的小文件join
 * 
 * job参数设置：
 * 		无reduce,设置reduce个数为0
 * 
 * @author 郑松松
 * @time   2019年8月4日 上午9:47:43
 */
public class SemiJoin extends Configured implements Tool{
	
	/**
	 * map 阶段
	 */
	public static class SemiJoinMapper extends Mapper<LongWritable, Text, LongWritable, Text>{
		
		/**
		 * key	: id
		 * value:name
		 */
		static Map<String, String> cacheMap = new HashMap<String, String>();
		LongWritable keyOut = new LongWritable();
		Text valueOut = new Text();
		
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {

			URI[] cacheFiles = context.getCacheFiles();
			if(cacheFiles == null || cacheFiles.length != 1){
				return;
			}
			String path = cacheFiles[0].getPath().toString();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
				String line = null;
				while((line = reader.readLine()) != null){
					String[] splits = line.split("\t");
					if(splits == null || splits.length != 2){
						continue;
					}
					String id = splits[0];
					String name = splits[1];
					cacheMap.put(id, name);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(reader != null){
					reader.close();
				}
			}
		}
		
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split("\t");

			long id = Long.parseLong(splits[0]);
			String age = splits[1];
			
			// 通过 id 上cacheMap 里找到对应的name
			String nameSmall = cacheMap.get(String.valueOf(id));
			if(nameSmall == null){
				return;
			}
			keyOut.set(id);
			valueOut.set(nameSmall + "\001" + age);
			context.write(keyOut, valueOut);
		}
	}

	/**
	 * job 运行类
	 */
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		Job job = Job.getInstance(conf, "SemiJoin");
		job.setJarByClass(SemiJoin.class);
		
		job.setMapperClass(SemiJoinMapper.class);
		
		// reduce数量是0
		job.setNumReduceTasks(0);
		
		job.setMapOutputKeyClass(LongWritable.class);
		job.setMapOutputValueClass(Text.class);
		
		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(Text.class);
		
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
		// 运行参数：-Dmapreduce.job.cache.files=file:///D:/tmp/input/innerjoin/m1.txt D:/tmp/input/innerjoin/m2.txt C:/Users/song/Desktop/output/wordcount
		ToolRunner.run(new SemiJoin(), args);
	}

}
