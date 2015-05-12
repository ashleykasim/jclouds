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

package org.jclouds.vsphere.compute.internal;

import com.vmware.vim25.GuestPosixFileAttributes;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.mo.VirtualMachine;
import org.jclouds.vsphere.domain.VSphereServiceInstance;
import org.jclouds.vsphere.internal.VSphereRestClient;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 */
public class GuestFilesUtils {
   public static boolean loadFileToGuest(VirtualMachine vm, InputStream in, NamePasswordAuthentication npa, VSphereServiceInstance serviceInstance, String fullPathToFileInGuest) {
      try {
         GuestPosixFileAttributes posixFileAttributes = new GuestPosixFileAttributes();
         posixFileAttributes.setPermissions(Long.valueOf(500));
         posixFileAttributes.setAccessTime(GregorianCalendar.getInstance());
         Calendar modCal = Calendar.getInstance();
         modCal.setTimeInMillis(System.currentTimeMillis());
         posixFileAttributes.setModificationTime(modCal);
         int fileSize = in.available();
         String upUrlStr = serviceInstance.getInstance().getGuestOperationsManager().getFileManager(vm).initiateFileTransferToGuest(npa, fullPathToFileInGuest, posixFileAttributes, fileSize, true);
         //upUrlStr.replace("\\*", serviceInstance.getInstance().getServerConnection().getUrl().getHost());
         HttpURLConnection putCon = (HttpURLConnection) new URL(upUrlStr).openConnection();
         putCon.setDoInput(true);
         putCon.setDoOutput(true);
         putCon.setRequestProperty("Content-Type", "application/octet-stream");
         putCon.setRequestMethod("PUT");
         putCon.setRequestProperty("Content-Length", Long.toString(fileSize));

         byte[] buffer = new byte[VSphereRestClient.CHUNKLEN];
         if (fileSize > VSphereRestClient.CHUNKLEN)
            putCon.setChunkedStreamingMode(VSphereRestClient.CHUNKLEN);
         else {
            putCon.setChunkedStreamingMode(fileSize);
            buffer = new byte[fileSize];
         }

         DataOutputStream out = new DataOutputStream(putCon.getOutputStream());
         int len = 0;
         while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
         }
         if (out != null) {
            out.flush();
            out.close();
         }
         if (in != null)
            in.close();
      return true;
      } catch (Exception e) {
         return false;
      }
   }
}
