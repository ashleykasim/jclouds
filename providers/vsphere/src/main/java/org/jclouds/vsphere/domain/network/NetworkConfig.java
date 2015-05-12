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

package org.jclouds.vsphere.domain.network;

import org.jclouds.javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class NetworkConfig {

   public Builder toBuilder() {
      return builder().fromNetworkConfig(this);
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private String networkName;
      private String nicName;
      private String addressType;


      public Builder networkName(String networkName) {
         this.networkName = networkName;
         return this;
      }

      public Builder nicName(String nicName) {
         this.nicName = nicName;
         return this;
      }

      public Builder addressType(String addressType) {
         this.addressType = addressType;
         return this;
      }

      public Builder fromNetworkConfig(NetworkConfig in) {
         return networkName(in.getNetworkName()).nicName(in.getNicName()).addressType(in.getAddressType());
      }

      public NetworkConfig build() {
         return new NetworkConfig(networkName, nicName, addressType);
      }
   }

   @Nullable
   private final String networkName;
   private String nicName;
   @Nullable
   private String addressType;

   /**
    * Create a new NetworkConfig.
    *
    * @param networkName a valid {@networkConfig
    *                    org.jclouds.vsphere.domain.VAppTemplate#getNetworkSection network in the vapp
    *                    template}, or null to have us choose default
    */
   public NetworkConfig(String networkName, String nicName, String addressType) {
      this.networkName = checkNotNull(networkName, "networkName");
      this.nicName = nicName;
      this.addressType = addressType;
   }

   public NetworkConfig(String networkName) {
      this(networkName, null, null);
   }

   /**
    * A name for the network.
    *
    * @return
    */
   public String getNetworkName() {
      return networkName;
   }

   /**
    * @return A reference to the organization network to which this network connects.
    */
   public String getNicName() {
      return nicName;
   }

   public String getAddressType() {
      return addressType;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      NetworkConfig that = (NetworkConfig) o;

      if (addressType != null ? !addressType.equals(that.addressType) : that.addressType != null) return false;
      if (!networkName.equals(that.networkName)) return false;
      if (nicName != null ? !nicName.equals(that.nicName) : that.nicName != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = networkName.hashCode();
      result = 31 * result + (nicName != null ? nicName.hashCode() : 0);
      result = 31 * result + (addressType != null ? addressType.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "NetworkConfig{" +
              "networkName='" + networkName + '\'' +
              ", nicName='" + nicName + '\'' +
              ", addressType='" + addressType + '\'' +
              '}';
   }
}
