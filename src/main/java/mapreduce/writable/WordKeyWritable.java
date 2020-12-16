package mapreduce.writable;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 自定义序列化类，并实现二次排序，主关键字升序， 次关键字降序
 * 
 * @author 郑松松
 * @time   2019年8月4日 下午4:31:20
 */
public class WordKeyWritable implements WritableComparable<WordKeyWritable>{
	
	// 主关键字
	private Text firstKey = new Text();
	
	// 次关键字
	private long secondKey = 0L;

	@Override
	public void write(DataOutput out) throws IOException {
		this.firstKey.write(out);
		out.writeLong(this.secondKey);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.firstKey.readFields(in);
		this.secondKey = in.readLong();
	}

	@Override
	public int compareTo(WordKeyWritable o) {
		System.out.println("----------wordkeywritable compareTo()---------");
		// 在此方法中实现二次排序逻辑
		// 比较主次关系
		int result1 = this.firstKey.compareTo(o.firstKey);
		// 如果主关键字相同比较次关键字
		if(result1 == 0){
			// 降序
			return -ascSecondKey(o);
		}
		return result1;
	}

	/**
	 * 升序比较
	 */
	private int ascSecondKey(WordKeyWritable o) {
		if(this.secondKey == o.secondKey){
			return 0;
		}else if(this.secondKey > o.secondKey){
			return 1;
		}else{
			return -1;
		}
	}

	public Text getFirstKey() {
		return firstKey;
	}

	public void setFirstKey(Text firstKey) {
		this.firstKey = firstKey;
	}

	public long getSecondKey() {
		return secondKey;
	}

	public void setSecondKey(long secondKey) {
		this.secondKey = secondKey;
	}

	@Override
	public String toString() {
		return this.firstKey.toString() + "\001" + this.secondKey;
	}
}
