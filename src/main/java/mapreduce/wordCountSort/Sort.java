package mapreduce.wordCountSort;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class Sort {

    public static class Map extends Mapper<Object, Text, IntWritable, Text> {
        IntWritable outk = new IntWritable();
        Text outV = new Text();

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer st = new StringTokenizer(value.toString());
            while (st.hasMoreTokens()) {
                String element = st.nextToken();
                if (Pattern.matches("\\d+", element)) {
                    outk.set(Integer.parseInt(element));
                } else {
                    outV.set(element);
                }
            }
            context.write(outk, outV);
        }
    }


    static class SortInt implements Comparable<SortInt>{

        private Integer integer;

        public SortInt(Integer integer) {
            this.integer = integer;
        }
        public Integer getInteger(){
            return this.integer;
        }

        @Override
        public int compareTo(SortInt o) {
            return o.getInteger()-this.getInteger();
        }
    }

    public static class Reduce extends Reducer<IntWritable, Text, Text, IntWritable>{

        private static final int TOP = 10;

        private static MultipleOutputs<Text,IntWritable> mos = null;

        private TreeMap<SortInt,String> treeMap = new TreeMap<SortInt, String>();

        @Override
         public void reduce(IntWritable key,Iterable<Text> values,Context context){
          for(Text value:values){
              treeMap.put(new SortInt(key.get()),value.toString());
              if(treeMap.size()>TOP){
                  treeMap.remove(treeMap.lastKey());
              }
          }
         }

         @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            String path = context.getConfiguration().get("topKPath");
            mos = new MultipleOutputs<>(context);
            for(java.util.Map.Entry<SortInt,String> entry: treeMap.entrySet()){
                mos.write("topKMOS",new Text(entry.getValue()),new IntWritable(entry.getKey().getInteger()),path);
            }
            mos.close();
        }


    }

    public static void run(String in,String out,String topKout) throws IOException, ClassNotFoundException, InterruptedException {
        Path outPath = new Path(out);
        Configuration configuration = new Configuration();
        configuration.set("topKPath",topKout);
        Job job = new Job(configuration,"sort");
        job.setJarByClass(Sort.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        // 设置Map输出类型
                job.setMapOutputKeyClass(IntWritable.class);
                job.setMapOutputValueClass(Text.class);

                 // 设置Reduce输出类型
                job.setOutputKeyClass(Text.class);
                job.setOutputValueClass(IntWritable.class);

        MultipleOutputs.addNamedOutput(job,"topKMOS", TextOutputFormat.class,Text.class,Text.class);
        FileInputFormat.addInputPath(job,new Path(in));
        FileOutputFormat.setOutputPath(job,outPath);
        job.waitForCompletion(true);
    }
}
