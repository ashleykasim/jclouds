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

import com.google.common.base.Supplier;
import com.vmware.vim25.FileFault;
import com.vmware.vim25.InvalidDatastore;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.UserNotFound;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.Task;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;
import org.jclouds.vsphere.FileManagerApi;
import org.jclouds.vsphere.domain.VSphereHost;
import org.jclouds.vsphere.domain.VSphereServiceInstance;
import org.jclouds.vsphere.suppliers.VSphereHostSupplier;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class VSphereFileManager implements FileManagerApi {
   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final Supplier<VSphereServiceInstance> serviceInstance;
   private final VSphereHostSupplier hostSupplier;

   @Inject
   public VSphereFileManager(Supplier<VSphereServiceInstance> serviceInstance, VSphereHostSupplier hostSupplier) {
      this.serviceInstance = checkNotNull(serviceInstance, "serviceInstance");
      this.hostSupplier = checkNotNull(hostSupplier, "hostSupplier");
   }

   private String getDatacenterName(ManagedEntity managedEntity) {
      if (managedEntity.getMOR().getType().equals("Datacenter"))
         return managedEntity.getName();
      else
         return getDatacenterName(managedEntity.getParent());
   }

   @Override
   public void uploadFile(String srcFilePath, String destDirectory) throws IOException {

      VSphereServiceInstance instance = serviceInstance.get();
      String serverUrl = instance.getInstance().getServerConnection().getUrl().toString().replaceAll("/sdk", "");
      String cookie = instance.getInstance().getServerConnection().getSessionStr();
      VSphereHost vSphereHost = hostSupplier.get();
      String dsName = vSphereHost.getDatastore().getSummary().getName();
      String dcPath = getDatacenterName(vSphereHost.getHost());

      VSphereRestClient client = new VSphereRestClient(serverUrl);
      File file = new File(srcFilePath);
      client.putFile(cookie, destDirectory, dcPath, dsName, file);
   }

   @Override
   public void changeOwner(String name, Datacenter datacenter, String owner) throws InvalidDatastore, FileFault, UserNotFound, RuntimeFault, RemoteException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public Task copyDatastoreFileTask(String sourceName, Datacenter sourceDatacenter, String destinationName, Datacenter destinationDatacenter, boolean force) throws FileFault, InvalidDatastore, RuntimeFault, RemoteException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public Task deleteDatastoreFileTask(String name, Datacenter datacenter) throws FileFault, InvalidDatastore, RuntimeFault, RemoteException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void makeDirectory(String name, Datacenter datacenter, boolean createParentDirectories) throws FileFault, InvalidDatastore, RuntimeFault, RemoteException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public Task moveDatastoreFileTask(String sourceName, Datacenter sourceDatacenter, String destinationName, Datacenter destinationDatacenter, boolean force) throws FileFault, InvalidDatastore, RuntimeFault, RemoteException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }
}
