package mapreduce.mrjob;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class WordCount extends Configured{
	
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

			for(String word : splits){
				keyOut.set(word);
				context.write(keyOut, valueOut);
			}
		}
	}
	
	/**
	 * reduce 阶段
	 */
	public static class WordCountReducer extends Reducer<Text, LongWritable, Text, LongWritable>{
		LongWritable valueOut = new LongWritable();
		
		@Override
		protected void reduce(Text key, Iterable<LongWritable> values,Context context) throws IOException, InterruptedException {

			long sum = 0L;
			for(LongWritable value : values){
				long n = value.get();
				sum += n;
			}
			valueOut.set(sum);
			context.write(key, valueOut);
		}
	}
}
