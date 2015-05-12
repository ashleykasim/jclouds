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

import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.VirtualMachineToolsStatus;
import com.vmware.vim25.mo.VirtualMachine;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ssh.SshClient;
import org.powermock.api.easymock.PowerMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

/**
 */
public class VirtualMachineToSshClientTest {
   public static class StubSshClientModule extends AbstractModule {

      protected void configure() {
         bind(SshClient.Factory.class).to(Factory.class).in(Scopes.SINGLETON);
      }

      private static class Factory implements SshClient.Factory {
         public SshClient create(HostAndPort socket, LoginCredentials credentials) {
            return createNiceMock(SshClient.class);
         }

         public boolean isAgentAvailable() {
            return false;
         }
      }
   }

   @Test
   public void applyTest() {
      VirtualMachineToSshClient virtualMachineToSshClient = new VirtualMachineToSshClient(new StubSshClientModule.Factory());

      VirtualMachine vm = PowerMock.createMock(VirtualMachine.class);
      GuestInfo guest = PowerMock.createMock(GuestInfo.class);
      expect(vm.getGuest()).andReturn(guest).anyTimes();
      expect(guest.getIpAddress()).andReturn("10.10.0.2").anyTimes();
      expect(guest.getToolsStatus()).andReturn(VirtualMachineToolsStatus.toolsOk).anyTimes();
      replay(vm, guest);
      SshClient sshClient = virtualMachineToSshClient.apply(vm);

      Assert.assertNotNull(sshClient);
   }
}
