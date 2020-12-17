package mapreduce;



import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 *
 */
public class SameFriendsMR {

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
//        conf.addResource("hdfs-site.xml");//使用配置文件
//        System.setProperty("HADOOP_USER_NAME", "hadoop");//使用集群
        ///////////////////第一步////////////////////////
        FileSystem fs = FileSystem.get(conf);//默认使用本地

        Job job = Job.getInstance(conf);
        job.setJarByClass(SameFriendsMR.class);
        job.setMapperClass(SameFriendsMR_Mapper.class);
        job.setReducerClass(SameFriendsMR_Reducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        String input = "/tmp/mr/sameFriend";
        String out = "/tmp/mr/sameFriend/out";
        String out2 = "/tmp/mr/sameFriend/out2";
        Path inputPath = new Path(input);
        Path outputPath = new Path(out);
        if (fs.exists(inputPath)) {
            fs.delete(outputPath, true);
        }

        FileInputFormat.setInputPaths(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);
        ////////////////////第二步//////////////////////////////////
        FileSystem fs2 = FileSystem.get(conf);//默认使用本地

        Job job2 = Job.getInstance(conf);
        job2.setJarByClass(SameFriendsMR.class);
        job2.setMapperClass(SameFriends2MR_Mapper.class);
        job2.setReducerClass(SameFriends2MR_Reducer.class);

        job2.setMapOutputKeyClass(Text.class);
        job2.setMapOutputValueClass(Text.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(Text.class);

        Path inputPath2 = new Path(out);
        Path outputPath2 = new Path(out2);
        if (fs2.exists(inputPath2)) {
            fs2.delete(outputPath2, true);
        }

        FileInputFormat.setInputPaths(job2, inputPath2);
        FileOutputFormat.setOutputPath(job2, outputPath2);

        //采用jobcontrol进行2个MapReduce串行
        ControlledJob aJob = new ControlledJob(job.getConfiguration());
        ControlledJob bJob = new ControlledJob(job2.getConfiguration());

        aJob.setJob(job);
        bJob.setJob(job2);
        bJob.addDependingJob(aJob);//指定依赖关系

        JobControl jc = new JobControl("jcF");
        jc.addJob(aJob);
        jc.addJob(bJob);

        Thread thread = new Thread(jc);
        thread.start();
        while(!jc.allFinished()){
            thread.sleep(1000);
        }
        jc.stop();
    }
    ///////////////////////////////第一步/////////////////////////////////////////
    public static class SameFriendsMR_Mapper extends Mapper<LongWritable, Text, Text, Text>{
        Text kout = new Text();
        Text valueout = new Text();
        @Override
        protected void map(LongWritable key, Text value,Context context)throws IOException, InterruptedException {
            //A:B,C,D,F,E,O
            String [] reads = value.toString().trim().split(":");
            //以用户为value值，好友为key值，求取以某用户为共同好友的人
            String vv = reads[0];
            valueout.set(vv);
            String [] friends = reads[1].split(",");
            for(int i = 0; i < friends.length; i++){
                String kk = friends[i];
                kout.set(kk);
                context.write(kout, valueout);
            }
        }
    }
    public static class SameFriendsMR_Reducer extends Reducer<Text, Text, Text, Text>{
        Text kout = new Text();
        Text valueout = new Text();
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context)throws IOException, InterruptedException {
            //将用户共同好友的人组合起来
            StringBuilder sb = new StringBuilder();
            for(Text text : values){
                sb.append(text.toString()).append(",");
            }
            String vv = sb.substring(0, sb.length() - 1);
            valueout.set(vv);
            context.write(key, valueout);
        }

    }
    //////////////////////////////////////第二步///////////////////////////////////////////////////////////
    public static class SameFriends2MR_Mapper extends Mapper<LongWritable, Text, Text, Text>{
        Text kout = new Text();
        Text valueout = new Text();
        @Override
        protected void map(LongWritable key, Text value,Context context)throws IOException, InterruptedException {
            //A    F,I,O,K,G,D,C,H,B ：意思是F,I,O,K,G,D,C,H,B的共同好友为A,第二步执行求取两两好友,A为value，两两对为key
            String [] reads = value.toString().trim().split("\t");
            //A为value
            String vv = reads[0];
            valueout.set(vv);
            //求两两好友对，为了防止重复，需要对好友进行排序
            String [] friends = reads[1].split(",");
            Arrays.sort(friends);
            //使用两重循环，寻找A-B等好友对组合
            for(int i = 0; i < friends.length - 1; i++){
                for(int j = i + 1; j < friends.length; j++ ){
                    String kk = friends[i] + "-" + friends[j];
                    kout.set(kk);
                    context.write(kout, valueout);
                }
            }
        }
    }
    public static class SameFriends2MR_Reducer extends Reducer<Text, Text, Text, Text>{
        Text kout = new Text();
        Text valueout = new Text();
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context)throws IOException, InterruptedException {
            //好友对组合起来
            StringBuilder sb = new StringBuilder();
            for(Text text : values){
                sb.append(text.toString()).append(",");
            }
            String vv = sb.substring(0, sb.length() - 1);
            valueout.set(vv);
            context.write(key, valueout);
        }

    }
}
