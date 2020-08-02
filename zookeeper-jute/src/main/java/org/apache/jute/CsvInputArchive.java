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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.UnsupportedEncodingException;

/**
 *
 */
class CsvInputArchive implements InputArchive {
    // 推回字节流
    private PushbackReader stream;
    // 内部类，对应CsvInputArchive索引
    private class CsvIndex implements Index {
        // 是否已经完成
        public boolean done() {
            char c = '\0';
            try {
                // 读取字符
                c = (char) stream.read();
                // 推回缓冲区
                stream.unread(c);
            } catch (IOException ex) {
            }
            return (c == '}') ? true : false;
        }
        // 什么都不做
        public void incr() {}
    }
    // 私有方法，读取字段
    private String readField(String tag) throws IOException {
        try {
            StringBuilder buf = new StringBuilder();
            while (true) {
                // 读取并转化为字符
                char c = (char) stream.read();
                switch (c) { // 判断字符
                    case ',':
                        // 读取字段完成，可直接返回
                        return buf.toString();
                    case '}':
                    case '\n':
                    case '\r':
                        // 推回缓冲区
                        stream.unread(c);
                        return buf.toString();
                    default: // 默认添加至buf中
                        buf.append(c);
                }
            }
        } catch (IOException ex) {
            throw new IOException("Error reading "+tag);
        }
    }
    // 获取CsvInputArchive
    static CsvInputArchive getArchive(InputStream strm)
    throws UnsupportedEncodingException {
        return new CsvInputArchive(strm);
    }
    
    /** Creates a new instance of CsvInputArchive  // 构造函数*/
    public CsvInputArchive(InputStream in)
    throws UnsupportedEncodingException {
        stream = new PushbackReader(new InputStreamReader(in, "UTF-8"));
    }
    // 读取byte类型
    public byte readByte(String tag) throws IOException {
        return (byte) readLong(tag);
    }
    // 读取boolean类型
    public boolean readBool(String tag) throws IOException {
        String sval = readField(tag);
        return "T".equals(sval) ? true : false;
    }
    // 读取int类型
    public int readInt(String tag) throws IOException {
        return (int) readLong(tag);
    }
    // 读取long类型
    public long readLong(String tag) throws IOException {
        // 读取字段
        String sval = readField(tag);
        try {
            // 转化
            long lval = Long.parseLong(sval);
            return lval;
        } catch (NumberFormatException ex) {
            throw new IOException("Error deserializing "+tag);
        }
    }
    // 读取float类型
    public float readFloat(String tag) throws IOException {
        return (float) readDouble(tag);
    }
    // 读取double类型
    public double readDouble(String tag) throws IOException {
        // 读取字段
        String sval = readField(tag);
        try {
            // 转化
            double dval = Double.parseDouble(sval);
            return dval;
        } catch (NumberFormatException ex) {
            throw new IOException("Error deserializing "+tag);
        }
    }
    // 读取String类型
    public String readString(String tag) throws IOException {
        // 读取字段
        String sval = readField(tag);
        // 转化
        return Utils.fromCSVString(sval);
        
    }
    // 读取缓冲类型
    public byte[] readBuffer(String tag) throws IOException {
        // 读取字段
        String sval = readField(tag);
        // 转化
        return Utils.fromCSVBuffer(sval);
    }
    // 读取记录
    public void readRecord(Record r, String tag) throws IOException {
        // 反序列化
        r.deserialize(this, tag);
    }
    // 开始读取记录
    public void startRecord(String tag) throws IOException {
        if (tag != null && !"".equals(tag)) {
            // 读取并转化为字符
            char c1 = (char) stream.read();
            // 读取并转化为字符
            char c2 = (char) stream.read();
            if (c1 != 's' || c2 != '{') {  // 进行判断
                throw new IOException("Error deserializing "+tag);
            }
        }
    }
    // 结束读取记录
    public void endRecord(String tag) throws IOException {
        // 读取并转化为字符
        char c = (char) stream.read();
        if (tag == null || "".equals(tag)) {
            if (c != '\n' && c != '\r') {// 进行判断
                throw new IOException("Error deserializing record.");
            } else {
                return;
            }
        }

        if (c != '}') { // 进行判断
            throw new IOException("Error deserializing "+tag);
        }
        // 读取并转化为字符
        c = (char) stream.read();
        if (c != ',') {
            // 推回缓冲区
            stream.unread(c);
        }
        
        return;
    }
    // 开始读取vector
    public Index startVector(String tag) throws IOException {
        char c1 = (char) stream.read();
        char c2 = (char) stream.read();
        if (c1 != 'v' || c2 != '{') {
            throw new IOException("Error deserializing "+tag);
        }
        return new CsvIndex();
    }
    // 结束读取vector
    public void endVector(String tag) throws IOException {
        char c = (char) stream.read();
        if (c != '}') {
            throw new IOException("Error deserializing "+tag);
        }
        c = (char) stream.read();
        if (c != ',') {
            stream.unread(c);
        }
        return;
    }
    // 开始读取Map
    public Index startMap(String tag) throws IOException {
        char c1 = (char) stream.read();
        char c2 = (char) stream.read();
        if (c1 != 'm' || c2 != '{') {
            throw new IOException("Error deserializing "+tag);
        }
        return new CsvIndex();
    }
    // 结束读取Map
    public void endMap(String tag) throws IOException {
        char c = (char) stream.read();
        if (c != '}') {
            throw new IOException("Error deserializing "+tag);
        }
        c = (char) stream.read();
        if (c != ',') {
            stream.unread(c);
        }
        return;
    }
}
