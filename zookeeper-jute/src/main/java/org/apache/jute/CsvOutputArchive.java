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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.TreeMap;

/**
 *
 */
public class CsvOutputArchive implements OutputArchive {
    // PrintStream为其他输出流添加了功能，使它们能够方便地打印各种数据值表示形式
    private PrintStream stream;
    // 默认为第一次
    private boolean isFirst = true;
    // 获取Archive
    static CsvOutputArchive getArchive(OutputStream strm)
    throws UnsupportedEncodingException {
        return new CsvOutputArchive(strm);
    }
    // 私有函数，抛出异常
    private void throwExceptionOnError(String tag) throws IOException {
        if (stream.checkError()) {
            throw new IOException("Error serializing "+tag);
        }
    }
    // 私有函数，除第一次外，均打印","
    private void printCommaUnlessFirst() {
        if (!isFirst) {
            stream.print(",");
        }
        isFirst = false;
    }
    
    /** Creates a new instance of CsvOutputArchive  // 构造函数*/
    public CsvOutputArchive(OutputStream out)
    throws UnsupportedEncodingException {
        stream = new PrintStream(out, true, "UTF-8");
    }
    // 写Byte类型
    public void writeByte(byte b, String tag) throws IOException {
        writeLong((long)b, tag);
    }
    // 写boolean类型
    public void writeBool(boolean b, String tag) throws IOException {
        // 打印","
        printCommaUnlessFirst();
        String val = b ? "T" : "F";
        // 打印值
        stream.print(val);
        // 抛出异常
        throwExceptionOnError(tag);
    }
    // 写int类型
    public void writeInt(int i, String tag) throws IOException {
        writeLong((long)i, tag);
    }
    // 写long类型
    public void writeLong(long l, String tag) throws IOException {
        printCommaUnlessFirst();
        stream.print(l);
        throwExceptionOnError(tag);
    }
    // 写float类型
    public void writeFloat(float f, String tag) throws IOException {
        writeDouble((double)f, tag);
    }
    // 写double类型
    public void writeDouble(double d, String tag) throws IOException {
        printCommaUnlessFirst();
        stream.print(d);
        throwExceptionOnError(tag);
    }
    // 写String类型
    public void writeString(String s, String tag) throws IOException {
        printCommaUnlessFirst();
        stream.print(Utils.toCSVString(s));
        throwExceptionOnError(tag);
    }
    // 写Buffer类型
    public void writeBuffer(byte buf[], String tag)
    throws IOException {
        printCommaUnlessFirst();
        stream.print(Utils.toCSVBuffer(buf));
        throwExceptionOnError(tag);
    }
    // 写Record类型
    public void writeRecord(Record r, String tag) throws IOException {
        if (r == null) {
            return;
        }
        r.serialize(this, tag);
    }
    // 开始写Record
    public void startRecord(Record r, String tag) throws IOException {
        if (tag != null && !"".equals(tag)) {
            printCommaUnlessFirst();
            stream.print("s{");
            isFirst = true;
        }
    }
    // 结束写Record
    public void endRecord(Record r, String tag) throws IOException {
        if (tag == null || "".equals(tag)) {
            stream.print("\n");
            isFirst = true;
        } else {
            stream.print("}");
            isFirst = false;
        }
    }
    // 开始写Vector
    public void startVector(List<?> v, String tag) throws IOException {
        printCommaUnlessFirst();
        stream.print("v{");
        isFirst = true;
    }
    // 结束写Vector
    public void endVector(List<?> v, String tag) throws IOException {
        stream.print("}");
        isFirst = false;
    }
    // 开始写Map
    public void startMap(TreeMap<?,?> v, String tag) throws IOException {
        printCommaUnlessFirst();
        stream.print("m{");
        isFirst = true;
    }
    // 结束写Map
    public void endMap(TreeMap<?,?> v, String tag) throws IOException {
        stream.print("}");
        isFirst = false;
    }
}
