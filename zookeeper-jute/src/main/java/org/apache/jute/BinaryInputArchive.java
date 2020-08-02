/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jute;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class BinaryInputArchive implements InputArchive {
    static public final String UNREASONBLE_LENGTH= "Unreasonable length = ";
    // DataInput接口，用于从二进制流中读取字节
    private DataInput in;
    // 静态方法，用于获取Archive
    static public BinaryInputArchive getArchive(InputStream strm) {
        return new BinaryInputArchive(new DataInputStream(strm));
    }
    // 内部类，对应BinaryInputArchive索引
    static private class BinaryIndex implements Index {
        // 元素个数
        private int nelems;
        // 构造函数
        BinaryIndex(int nelems) {
            this.nelems = nelems;
        }
        // 是否已经完成
        public boolean done() {
            return (nelems <= 0);
        }
        // 移动一项
        public void incr() {
            nelems--;
        }
    }
    /** Creates a new instance of BinaryInputArchive */
    public BinaryInputArchive(DataInput in) {
        this.in = in;
    }
    // 读取字节
    public byte readByte(String tag) throws IOException {
        return in.readByte();
    }
    // 读取boolean类型
    public boolean readBool(String tag) throws IOException {
        return in.readBoolean();
    }
    // 读取int类型
    public int readInt(String tag) throws IOException {
        return in.readInt();
    }
    // 读取long类型
    public long readLong(String tag) throws IOException {
        return in.readLong();
    }
    // 读取float类型
    public float readFloat(String tag) throws IOException {
        return in.readFloat();
    }
    // 读取double类型
    public double readDouble(String tag) throws IOException {
        return in.readDouble();
    }
    // 读取String类型
    public String readString(String tag) throws IOException {
        // 确定长度
    	int len = in.readInt();
    	if (len == -1) return null;
        checkLength(len);
    	byte b[] = new byte[len];
        // 从输入流中读取一些字节，并将它们存储在缓冲区数组b中
    	in.readFully(b);
    	return new String(b, "UTF8");
    }
    // 最大缓冲值
    static public final int maxBuffer = Integer.getInteger("jute.maxbuffer", 0xfffff);
    // 读取缓冲
    public byte[] readBuffer(String tag) throws IOException {
        // 确定长度
        int len = readInt(tag);
        if (len == -1) return null;
        checkLength(len);
        byte[] arr = new byte[len];
        // 从输入流中读取一些字节，并将它们存储在缓冲区数组arr中
        in.readFully(arr);
        return arr;
    }
    // 读取记录
    public void readRecord(Record r, String tag) throws IOException {
        // 反序列化，动态调用
        r.deserialize(this, tag);
    }
    // 开始读取记录，实现为空
    public void startRecord(String tag) throws IOException {}
    // 结束读取记录，实现为空
    public void endRecord(String tag) throws IOException {}
    // 开始读取向量
    public Index startVector(String tag) throws IOException {
        // 确定长度
        int len = readInt(tag);
        if (len == -1) {
        	return null;
        }
        // 返回索引
		return new BinaryIndex(len);
    }
    // 结束读取向量
    public void endVector(String tag) throws IOException {}
    // 开始读取Map
    public Index startMap(String tag) throws IOException {
        // 返回索引
        return new BinaryIndex(readInt(tag));
    }
    // 结束读取Map，实现为空
    public void endMap(String tag) throws IOException {}

    // Since this is a rough sanity check, add some padding to maxBuffer to
    // make up for extra fields, etc. (otherwise e.g. clients may be able to
    // write buffers larger than we can read from disk!)
    private void checkLength(int len) throws IOException {
        // 检查长度是否合理
        if (len < 0 || len > maxBuffer + 1024) {
            throw new IOException(UNREASONBLE_LENGTH + len);
        }
    }
}
