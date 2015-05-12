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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class VSphereRestClient {
   public static final int CHUNKLEN = 1024 * 10 * 10 * 10;
   private final String baseUrl;

   public String getBaseUrl() {
      return baseUrl;
   }

   public VSphereRestClient(String serverUrl) {
      this.baseUrl = checkNotNull(serverUrl, "serverUrl");
   }

   private String buildPutUrl(String urlStr, String dcPath, String dsName) {
      StringBuilder builder = new StringBuilder(this.getBaseUrl());
      builder.append("/folder/").append(urlStr).append("?dcPath=").append(dcPath).append("&dsName=").append(dsName);
      return builder.toString();
   }

   public int putFile(String cookie, String urlStr, String dcPath, String dsName, File file) throws IOException {
      String putUrl = buildPutUrl(urlStr, dcPath, dsName);

      HttpURLConnection putCon = (HttpURLConnection) new URL(putUrl).openConnection();
      putCon.setRequestMethod("PUT");
      putCon.setDoOutput(true);
      putCon.setDoInput(true);
      putCon.setRequestProperty("Cookie", cookie);
      long fileSize = file.length();
      putCon.setRequestProperty("Content-Length", Long.toString(fileSize));
      byte[] buffer = new byte[CHUNKLEN];
      if (fileSize > CHUNKLEN)
         putCon.setChunkedStreamingMode(CHUNKLEN);
      else {
         putCon.setChunkedStreamingMode((int) fileSize);
         buffer = new byte[(int) fileSize];
      }

      DataOutputStream out = new DataOutputStream(putCon.getOutputStream());
      FileInputStream in = new FileInputStream(file);
      int len = 0;
      while ((len = in.read(buffer)) > 0) {
         out.write(buffer, 0, len);
      }
      if (in != null)
         in.close();
      if (out != null) {
         out.flush();
         out.close();
      }
      return 201;
   }

}
