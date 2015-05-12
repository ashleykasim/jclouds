/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jclouds.vsphere.internal;

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class VSphereRestClientTest {
   public static final String PREFIX = "stream2file";
   public static final String SUFFIX = ".tmp";

   @Test(expectedExceptions = {java.net.ConnectException.class})
   public void sendFileTest() throws IOException {
      final File tempFile = File.createTempFile(PREFIX, SUFFIX);
      tempFile.deleteOnExit();
      FileOutputStream out = new FileOutputStream(tempFile);
      IOUtils.copy(VSphereRestClientTest.class.getResourceAsStream("/jclouds.properties"), out);
      VSphereRestClient client = new VSphereRestClient("http://localhost:1234");
      int i = client.putFile("cookies", "files", "dcPath", "dsName", tempFile);
      Assert.assertEquals(201, i, "should be 201");
   }
}
