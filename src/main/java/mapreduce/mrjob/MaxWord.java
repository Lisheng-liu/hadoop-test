package mapreduce.mrjob;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class MaxWord extends Configured{
	
	/**
	 * map 阶段
	 */
	public static class MaxWordMapper extends Mapper<LongWritable, Text, Text, LongWritable>{
		Text keyOut = new Text();
		long max = 0L;
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] splits = value.toString().split("\t");
			
			String name = splits[0];
			long num = Long.parseLong(splits[1]);
			if(max < num){
				max = num;
				keyOut.set(name);
			}
		}
		
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			// 将每个map值最大的输出
			context.write(keyOut, new LongWritable(max));
		}
	}


	/**
	 * reduce阶段
	 */
	public static class MaxWordReducer extends Reducer<Text, LongWritable, Text, LongWritable>{
		
		Text keyOut = new Text(); 
		long max = 0L;

		@Override
		protected void reduce(Text key, Iterable<LongWritable> values,Context context) throws IOException, InterruptedException {

			for(LongWritable value : values){
				long n = value.get();
				if(max < n){
					max = n;
					keyOut.set(key.toString());
				}
			}
		}
		
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {

			context.write(keyOut, new LongWritable(max));
		}
	}
}
