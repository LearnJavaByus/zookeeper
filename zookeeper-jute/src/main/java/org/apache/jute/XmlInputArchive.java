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
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
/**
 *
 */
class XmlInputArchive implements InputArchive {
    // 内部类，值（包含类型和值）
    static private class Value {
        private String type;
        private StringBuffer sb;
        
        public Value(String t) {
            type = t;
            sb = new StringBuffer();
        }
        // 添加chars
        public void addChars(char[] buf, int offset, int len) {
            sb.append(buf, offset, len);
        }
        // 返回value
        public String getValue() { return sb.toString(); }
        // 返回type
        public String getType() { return type; }
    }
    // 内部类，XML解析器
    private static class XMLParser extends DefaultHandler {
        private boolean charsValid = false;
        
        private ArrayList<Value> valList;
        
        private XMLParser(ArrayList<Value> vlist) {
            valList = vlist;
        }
        // 文档开始，空的实现
        public void startDocument() throws SAXException {}
        // 文档结束，空的实现
        public void endDocument() throws SAXException {}
        // 开始解析元素
        public void startElement(String ns,
                String sname,
                String qname,
                Attributes attrs) throws SAXException {
            charsValid = false;
            if ("boolean".equals(qname) || // boolean类型
                    "i4".equals(qname) ||    // 四个字节
                    "int".equals(qname) ||   // int类型
                    "string".equals(qname) ||   // String类型
                    "double".equals(qname) ||   // double类型
                    "ex:i1".equals(qname) ||  // 一个字节
                    "ex:i8".equals(qname) ||  // 八个字节
                    "ex:float".equals(qname)) {  // 基本类型
                charsValid = true;
                // 添加至列表
                valList.add(new Value(qname));
            } else if ("struct".equals(qname) ||
                "array".equals(qname)) { // 结构体或数组类型
                // 添加至列表
                valList.add(new Value(qname));
            }
        }
        // 结束解析元素
        public void endElement(String ns,
                String sname,
                String qname) throws SAXException {
            charsValid = false;
            if ("struct".equals(qname) || // 结构体或数组类型
                    "array".equals(qname)) {
                // 添加至列表
                valList.add(new Value("/"+qname));
            }
        }
        
        public void characters(char buf[], int offset, int len)
        throws SAXException {
            if (charsValid) { // 是否合法
                // 从列表获取value
                Value v = valList.get(valList.size()-1);
                // 将buf添加至value
                v.addChars(buf, offset,len);
            }
        }
        
    }
    // 内部类，对应XmlInputArchive
    private class XmlIndex implements Index {
        // 是否已经完成
        public boolean done() {
            // 根据索引获取value
            Value v = valList.get(vIdx);
            if ("/array".equals(v.getType())) { // 类型为/array
                // 设置开索引值为null
                valList.set(vIdx, null);
                // 增加索引值
                vIdx++;
                return true;
            } else {
                return false;
            }
        }
        // 增加索引值，空的实现
        public void incr() {}
    }
    // 值列表
    private ArrayList<Value> valList;
    // 值长度
    private int vLen;
    // 索引
    private int vIdx;
    // 下一项
    private Value next() throws IOException {
        if (vIdx < vLen) {  // 下一项
            // 获取值
            Value v = valList.get(vIdx);
            // 设置索引值为null
            valList.set(vIdx, null);
            // 增加索引值
            vIdx++;
            return v;
        } else {
            throw new IOException("Error in deserialization.");
        }
    }
    // 获取XmlInputArchive
    static XmlInputArchive getArchive(InputStream strm)
    throws ParserConfigurationException, SAXException, IOException {
        return new XmlInputArchive(strm);
    }
    
    /** Creates a new instance of BinaryInputArchive  // 构造函数 */
    public XmlInputArchive(InputStream in)
    throws ParserConfigurationException, SAXException, IOException {
        valList = new ArrayList<Value>();
        DefaultHandler handler = new XMLParser(valList);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        SAXParser parser = factory.newSAXParser();
        parser.parse(in, handler);
        vLen = valList.size();
        vIdx = 0;
    }
    // 读取byte类型
    public byte readByte(String tag) throws IOException {
        Value v = next();
        if (!"ex:i1".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
        return Byte.parseByte(v.getValue());
    }
    // 读取Boolean类型
    public boolean readBool(String tag) throws IOException {
        Value v = next();
        if (!"boolean".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
        return "1".equals(v.getValue());
    }
    // 读取int类型
    public int readInt(String tag) throws IOException {
        Value v = next();
        if (!"i4".equals(v.getType()) &&
                !"int".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
        return Integer.parseInt(v.getValue());
    }
    // 读取long类型
    public long readLong(String tag) throws IOException {
        Value v = next();
        if (!"ex:i8".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
        return Long.parseLong(v.getValue());
    }
    // 读取float类型
    public float readFloat(String tag) throws IOException {
        Value v = next();
        if (!"ex:float".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
        return Float.parseFloat(v.getValue());
    }
    // 读取double类型
    public double readDouble(String tag) throws IOException {
        Value v = next();
        if (!"double".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
        return Double.parseDouble(v.getValue());
    }
    // 读取String类型
    public String readString(String tag) throws IOException {
        Value v = next();
        if (!"string".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
        return Utils.fromXMLString(v.getValue());
    }
    // 读取Buffer类型
    public byte[] readBuffer(String tag) throws IOException {
        Value v = next();
        if (!"string".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
        return Utils.fromXMLBuffer(v.getValue());
    }
    // 读取Record类型
    public void readRecord(Record r, String tag) throws IOException {
        r.deserialize(this, tag);
    }
    // 开始读取Record
    public void startRecord(String tag) throws IOException {
        Value v = next();
        if (!"struct".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
    }
    // 结束读取Record
    public void endRecord(String tag) throws IOException {
        Value v = next();
        if (!"/struct".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
    }
    // 开始读取vector
    public Index startVector(String tag) throws IOException {
        Value v = next();
        if (!"array".equals(v.getType())) {
            throw new IOException("Error deserializing "+tag+".");
        }
        return new XmlIndex();
    }
    // 结束读取vector
    public void endVector(String tag) throws IOException {}
    // 开始读取Map
    public Index startMap(String tag) throws IOException {
        return startVector(tag);
    }
    // 停止读取Map
    public void endMap(String tag) throws IOException { endVector(tag); }

}
