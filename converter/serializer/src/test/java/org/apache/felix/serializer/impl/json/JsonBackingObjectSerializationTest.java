/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.serializer.impl.json;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.osgi.util.converter.Converters;

import static org.junit.Assert.assertEquals;

public class JsonBackingObjectSerializationTest {
    @Test
    @SuppressWarnings( "rawtypes" )
    @Ignore("Trying to set up failing test case, but is not failing the way I intended!!")
    public void testComplexMapSerialization() {
        final MyDTOishObject obj = MyDTOishObject.factory( "A", "B" );
        final Map m = Converters
                .standardConverter()
                .convert(obj)
                .sourceAsDTO()
                .to(Map.class);

        String expected = 
                "{\"a\":\"A\","
                + "\"o\":XXX"
                + "\"b\":\"B\"}";

        assertEquals(expected, new JsonSerializerImpl().serialize(m).toString());
    }

    public static class MyDTOishObject {
        public String a;
        public String b;
        public OtherObject o;

        public MyDTOishObject( String a, String b ) {
            this.a = a;
            this.b = b;
            o = OtherObject.factory( a + a, b + b );
        }

        public static MyDTOishObject factory( String a, String b ) {
            return new MyDTOishObject( a, b );
        }
    }

    public static class OtherObject {
        public String a;
        public String b;

        public OtherObject( String a, String b ) {
            this.a = a;
            this.b = b;
        }

        public static OtherObject factory( String a, String b ) {
            return new OtherObject( a, b );
        }
    }
}