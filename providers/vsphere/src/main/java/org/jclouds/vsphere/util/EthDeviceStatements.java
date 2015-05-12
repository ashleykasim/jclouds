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
package org.jclouds.vsphere.util;

import com.google.common.collect.ImmutableList;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import org.jclouds.vsphere.domain.network.NetworkConfig;

import java.util.Set;

import static org.jclouds.scriptbuilder.domain.Statements.exec;

public class EthDeviceStatements implements Statement {
   private Set<NetworkConfig> configs;

   public EthDeviceStatements(Set<NetworkConfig> configs) {
      this.configs = configs;
   }

   private String getBOOTPROTO(String type) {
      if ("generated".equals(type))
         return "dhcp";
      else
         return "none";
   }

   private ImmutableList.Builder<Statement> statements = ImmutableList.builder();

   public void addStatement(Statement element) {
      statements.add(element);
   }

   public Iterable<String> functionDependencies(OsFamily family) {
      return ImmutableList.of();
   }

   public String render(OsFamily family) {
      if (family.equals(OsFamily.WINDOWS))
         throw new UnsupportedOperationException("windows not yet implemented");

      statements.add(exec("rm -f /etc/sysconfig/network-scripts/ifcfg-eth*"));
      int index = 0;
      for (NetworkConfig config : configs) {
         statements.add(exec("echo 'DEVICE=eth" + index));
         statements.add(exec("TYPE=Ethernet"));
         statements.add(exec("ONBOOT=yes"));
         statements.add(exec("NM_CONTROLLED=yes"));
         statements.add(exec("BOOTPROTO=" + getBOOTPROTO(config.getAddressType() + "' > /etc/sysconfig/network-scripts/ifcfg-eth" + index)));
         index++;
      }
      statements.add(exec("service network reload"));

      return new StatementList(statements.build()).render(family);
   }
}
