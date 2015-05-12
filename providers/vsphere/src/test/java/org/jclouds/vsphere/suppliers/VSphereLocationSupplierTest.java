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

package org.jclouds.vsphere.suppliers;

import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.PropertyCollector;
import com.vmware.vim25.mo.ServerConnection;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.ws.WSClient;
import org.jclouds.domain.Location;
import org.jclouds.vsphere.domain.VSphereServiceInstance;
import org.jclouds.vsphere.functions.CreateAndConnectVSphereClient;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Set;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

@PrepareForTest({ServerConnection.class,
        WSClient.class})
public class VSphereLocationSupplierTest extends PowerMockTestCase {
   @Test
   public void vSphereLocationSupplierTest() throws IOException {
      ServerConnection serverConnection = PowerMock.createMock(ServerConnection.class);
      WSClient wsClient = PowerMock.createMock(WSClient.class);
      ManagedObjectReference managedObjectReference = PowerMock.createMock(ManagedObjectReference.class);
      ServiceInstance serviceInstance = PowerMock.createMock(ServiceInstance.class);
      CreateAndConnectVSphereClient supplier = PowerMock.createMock(CreateAndConnectVSphereClient.class);
      VSphereServiceInstance vSphereServiceInstance = PowerMock.createMock(VSphereServiceInstance.class);
      Folder rootFolder = PowerMock.createMock(Folder.class);

      expect(supplier.get()).andReturn(vSphereServiceInstance);

      expect(vSphereServiceInstance.getInstance()).andReturn(serviceInstance);

      expect(serviceInstance.getRootFolder()).andReturn(rootFolder);

      expect(rootFolder.getServerConnection()).andReturn(serverConnection).anyTimes();
      expect(rootFolder.getMOR()).andReturn(managedObjectReference);
      expect(serverConnection.getServiceInstance()).andReturn(serviceInstance).anyTimes();
      expect(serverConnection.getVimService()).andReturn(new VimPortType(wsClient)).anyTimes();
      AboutInfo aboutInfo = new AboutInfo();
      aboutInfo.setApiVersion("5.1");
      expect(serviceInstance.getPropertyCollector()).andReturn(new PropertyCollector(serverConnection, managedObjectReference));
      expect(serviceInstance.getAboutInfo()).andReturn(aboutInfo);
      vSphereServiceInstance.close();
      replay(supplier, vSphereServiceInstance, serviceInstance, rootFolder, serverConnection);


      VSphereLocationSupplier vSphereLocationSupplier = new VSphereLocationSupplier(supplier);
      Set<? extends Location> location = vSphereLocationSupplier.get();

      Assert.assertEquals(1, location.size());
      for (Location l : location) {
         Assert.assertEquals("default", l.getId());
      }


      verify(supplier, vSphereServiceInstance, serviceInstance, rootFolder, serverConnection);
   }
}
