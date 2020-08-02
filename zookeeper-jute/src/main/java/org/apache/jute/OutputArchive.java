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
import java.util.List;
import java.util.TreeMap;

/**
 * Interface that alll the serializers have to implement.
 * 所有序列化器都需要实现此接口
 */
public interface OutputArchive {
    // 写Byte类型
    public void writeByte(byte b, String tag) throws IOException;
    // 写boolean类型
    public void writeBool(boolean b, String tag) throws IOException;
    // 写int类型
    public void writeInt(int i, String tag) throws IOException;
    // 写long类型
    public void writeLong(long l, String tag) throws IOException;
    // 写float类型
    public void writeFloat(float f, String tag) throws IOException;
    // 写double类型
    public void writeDouble(double d, String tag) throws IOException;
    // 写String类型
    public void writeString(String s, String tag) throws IOException;
    // 写Buffer类型
    public void writeBuffer(byte buf[], String tag)
        throws IOException;
    // 写Record类型
    public void writeRecord(Record r, String tag) throws IOException;
    // 开始写Record
    public void startRecord(Record r, String tag) throws IOException;
    // 结束写Record
    public void endRecord(Record r, String tag) throws IOException;
    // 开始写Vector
    public void startVector(List<?> v, String tag) throws IOException;
    // 结束写Vector
    public void endVector(List<?> v, String tag) throws IOException;
    // 开始写Map
    public void startMap(TreeMap<?,?> v, String tag) throws IOException;
    // 结束写Map
    public void endMap(TreeMap<?,?> v, String tag) throws IOException;

}
