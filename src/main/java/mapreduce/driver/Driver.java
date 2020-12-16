package mapreduce.driver;

import mapreduce.mrjob_base.WordMaxJobNew;
import org.apache.hadoop.util.ProgramDriver;


/**
 * 打包的类
 */
public class Driver {
	
	public static void main(String[] args) {
		int exitCode = -1;
	    ProgramDriver pgd = new ProgramDriver(); 
	    try{
//	    	wordmax -Dtask.input.dir=D:/workspace/tmp/input/wordcount -Dtask.id=0804 -Dtask.base.dir=D:/workspace/tmp/output
	    	pgd.addClass("wordmax", WordMaxJobNew.class, "统计高频单词");
	    	
//	    	descsort -Dtask.input.dir=/tmp/mr/task/input -Dtask.id=0123 -Dtask.base.dir=/tmp/mr/task	    	
//	    	pgd.addClass("descsort", WordCountAndDescSortJob.class, "统计单词数量，并对数量降序排序任务链");
	    	
//	    	scorejob -Dtask.input.dir=/tmp/mr/task/input_score -Dtask.id=0125 -Dtask.base.dir=/tmp/mr/task	
//	    	pgd.addClass("scorejob", ScoreJob.class, "统计每个学生的总成绩，并按照总成绩降序排序");
	    	
//	    	orc2hfile -Dtask.input.dir=/tmp/hbase/input -Dtask.id=0221 -Dtask.base.dir=/tmp/hbase -Dhbase.zookeeper.quorum=nn1.hadoop:2181,nn2.hadoop:2181,s1.hadoop:2181
//	    	pgd.addClass("orc2hfile", Orc2HfileJob.class, "orc2hfile 文件");
	    	
//	    	create_import_hbase -Dtask.input.dir=/tmp/hbase/input -Dtask.id=0221 -Dtask.base.dir=/tmp/hbase -Dhbase.zookeeper.quorum=nn1.hadoop:2181,nn2.hadoop:2181,s1.hadoop:2181
//	    	pgd.addClass("create_import_hbase", CreateSplitTableImportHbaseJob.class, "建表并将orc文件导入到hbase表中");
	    	
//	    	scan_orc -Dtask.id=0222 -Dtask.base.dir=/tmp/hbase -Dhbase.zookeeper.quorum=nn1.hadoop:2181,nn2.hadoop:2181,s1.hadoop:2181
//	    	pgd.addClass("scan_orc", ScanTableResult2OrcJob.class, "通过scan hbase表数据结果，导出到orc文件");
	    	
//	    	scan_orc_new -Dtask.id=0222 -Dtask.base.dir=/tmp/hbase -Dhbase.zookeeper.quorum=nn1.hadoop:2181,nn2.hadoop:2181,s1.hadoop:2181
//	    	pgd.addClass("scan_orc_new", ScanTableResult2OrcJobNew.class, "通过scan hbase表数据结果，导出到orc文件");
	    	
//	    	hfile_orc -Dtask.id=0223 -Dtask.hbase.table.dir=/tmp/hbase/input_hfile -Dtask.base.dir=/tmp/hbase
//	    	pgd.addClass("hfile_orc", HFile2OrcJob.class, "读取hbase表hfile文件，导出到orc文件");
	    	
	    	exitCode = pgd.run(args);
	    }
	    catch (Throwable e) {
	    	e.printStackTrace();
	    }
	    System.exit(exitCode);		
	}
}

