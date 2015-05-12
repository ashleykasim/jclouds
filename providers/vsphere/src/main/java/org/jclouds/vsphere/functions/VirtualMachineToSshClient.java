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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineToolsStatus;
import com.vmware.vim25.mo.VirtualMachine;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.logging.Logger;
import org.jclouds.ssh.SshClient;
import org.jclouds.util.Predicates2;
import org.jclouds.vsphere.config.VSphereConstants;

import javax.annotation.Resource;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class VirtualMachineToSshClient implements Function<VirtualMachine, SshClient> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   @Named(VSphereConstants.JCLOUDS_VSPHERE_VM_PASSWORD)
   protected String password = null;

   private final SshClient.Factory sshClientFactory;

   @Inject
   public VirtualMachineToSshClient(SshClient.Factory sshClientFactory) {
      this.sshClientFactory = checkNotNull(sshClientFactory, "sshClientFactory");
   }

   @Override
   public SshClient apply(final VirtualMachine vm) {
      SshClient client = null;
      String clientIpAddress = vm.getGuest().getIpAddress();
      String sshPort = "22";
      while (!vm.getGuest().getToolsStatus().equals(VirtualMachineToolsStatus.toolsOk) || clientIpAddress.isEmpty()) {
         int timeoutValue = 1000;
         int timeoutUnits = 500;
         Predicate<String> tester = Predicates2.retry(
                 ipAddressTester, timeoutValue, timeoutUnits,
                 TimeUnit.MILLISECONDS);
         boolean passed = false;
         while (vm.getRuntime().getPowerState()
                 .equals(VirtualMachinePowerState.poweredOn)
                 && !passed) {
            clientIpAddress = Strings.nullToEmpty(vm.getGuest()
                    .getIpAddress());
            passed = tester.apply(clientIpAddress);
         }
      }
      LoginCredentials loginCredentials = LoginCredentials.builder()
              .user("root").password(password)
              .build();
      checkNotNull(clientIpAddress, "clientIpAddress");
      client = sshClientFactory.create(
              HostAndPort.fromParts(clientIpAddress, Integer.parseInt(sshPort)),
              loginCredentials);
      checkNotNull(client);
      return client;
   }

   Predicate<String> ipAddressTester = new Predicate<String>() {

      @Override
      public boolean apply(String input) {
         return !input.isEmpty();
      }

   };

}
