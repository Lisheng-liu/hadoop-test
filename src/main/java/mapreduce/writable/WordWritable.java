package mapreduce.writable;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 自定义序列化类<br/>
 * 
 * 步骤：<br/>
 * 1） 该类需要实现Writable 接口，并实现该接口的 write() 和  readFields()<br/>
 * 
 * 2） 类中的属性可以是基本类型、引用类型及 hadoop自带的序列化类型，要赋初始值，否则容易报空指针<br/>
 * 
 * 3） write() 和  readFields() 序列化，反序列化顺序要一致<br/> 
 */
public class WordWritable implements Writable{

	private String word = "";
	private long num = 0L;
	private Text type = new Text();

	public void write(DataOutput out) throws IOException {
		out.writeUTF(this.word);
		out.writeLong(this.num);
		this.type.write(out);
	}

	public void readFields(DataInput in) throws IOException {
		this.word = in.readUTF();
		this.num = in.readLong();
		this.type.readFields(in);
	}
	public long getNum() {
		return num;
	}
	public void setNum(long num) {
		this.num = num;
	}
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public Text getType() {
		return type;
	}
	public void setType(Text type) {
		this.type = type;
	}
}
