package mapreduce.writable;

import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 自定义序列化类实现序列化和比较
 * 
 * @author 郑松松
 * @time   2019年8月4日 下午1:15:50
 */
public class NumWritable implements WritableComparable<NumWritable>{
	
	private long num = 0L;

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(num);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.num = in.readLong();
	}

	@Override
	public int compareTo(NumWritable o) {
		// 降序
		return -asc(o);
	}

	private int asc(NumWritable o) {
		if(this.num == o.num){
			return 0;
		}else if(this.num > o.num){
			return 1;
		}
		return -1;
	}

	public long getNum() {
		return num;
	}

	public void setNum(long num) {
		this.num = num;
	}

	@Override
	public String toString() {
		return String.valueOf(this.num);
	}
	
}
