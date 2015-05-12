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

package org.jclouds.vsphere.domain;

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;

import java.io.Closeable;
import java.io.IOException;
import java.rmi.RemoteException;

import static com.google.common.base.Preconditions.checkNotNull;

public class VSphereHost implements Closeable {
   private Supplier<VSphereServiceInstance> serviceInstanceSupplier;
   private VSphereServiceInstance serviceInstance;
   private HostSystem host;

   public VSphereHost(HostSystem host, VSphereServiceInstance serviceInstance) {
      this.host = checkNotNull(host, "host");
      this.serviceInstance = checkNotNull(serviceInstance, "serviceInstance");
   }

   public VSphereHost(String hostName, VSphereServiceInstance serviceInstance) {
      this.serviceInstance = checkNotNull(serviceInstance, "serviceInstance");
      try {
         ManagedEntity hostEntity = new InventoryNavigator(serviceInstance.getInstance().getRootFolder()).searchManagedEntity("HostSystem", hostName);
         this.host = checkNotNull((HostSystem)hostEntity, "host");
      } catch (RemoteException e) {
         e.printStackTrace();
      }
   }

   public HostSystem getHost() {
      return host;
   }


   public Datastore getDatastore() {
      Datastore datastore = null;
      long freeSpace = 0;
      try {
         for (Datastore d : host.getDatastores()) {
            if (d.getSummary().getFreeSpace() > freeSpace) {
               freeSpace = d.getSummary().getFreeSpace();
               datastore = d;
            }
         }
      } catch (Throwable e) {
         Throwables.propagate(e);
      }
      return datastore;
   }

   @Override
   public void close() throws IOException {
      serviceInstance.close();
   }
}
