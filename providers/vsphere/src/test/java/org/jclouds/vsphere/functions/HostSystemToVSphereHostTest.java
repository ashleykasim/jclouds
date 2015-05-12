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

package org.jclouds.vsphere.functions;

import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ServerConnection;
import com.vmware.vim25.mo.ServiceInstance;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.replay;

@PrepareForTest({ServerConnection.class})
public class HostSystemToVSphereHostTest extends PowerMockTestCase {
   @Test
   public void applyTest() {
      HostSystemToVSphereHost hostSystemToVSphereHost = new HostSystemToVSphereHost();
      Assert.assertNull(hostSystemToVSphereHost.apply(null));
      HostSystem vm = PowerMock.createMock(HostSystem.class);
      ServiceInstance si = PowerMock.createMock(ServiceInstance.class);
      ServerConnection sc = PowerMock.createMock(ServerConnection.class);
      expect(vm.getServerConnection()).andReturn(sc).anyTimes();
      expect(sc.getServiceInstance()).andReturn(si).anyTimes();
      replay(vm, sc, ServerConnection.class);
      Assert.assertNotNull(hostSystemToVSphereHost.apply(vm));


   }
}
