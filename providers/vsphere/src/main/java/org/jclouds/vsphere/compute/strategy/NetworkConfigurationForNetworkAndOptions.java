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
package org.jclouds.vsphere.compute.strategy;

import org.jclouds.vsphere.compute.options.VSphereTemplateOptions;
import org.jclouds.vsphere.domain.network.NetworkConfig;

import java.util.Set;

/**
 */
public class NetworkConfigurationForNetworkAndOptions {
   protected final NetworkConfig defaultNetworkConfig;

   protected NetworkConfigurationForNetworkAndOptions() {
      this.defaultNetworkConfig = new NetworkConfig("default");

   }

   public NetworkConfig apply(String networkName, VSphereTemplateOptions vOptions) {
      NetworkConfig config;
      String userDefinedAddressType = vOptions.getAddressType();
      Set<String> userDefinedNetworks = vOptions.getNetworks();
      if (userDefinedAddressType != null) {
         config = NetworkConfig.builder().networkName(networkName).addressType(userDefinedAddressType).build();
      } else {
         config = defaultNetworkConfig.toBuilder().networkName(networkName).addressType("generated").build();
      }

      return config;
   }

}
