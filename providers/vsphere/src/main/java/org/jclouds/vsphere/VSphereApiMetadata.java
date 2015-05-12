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
package org.jclouds.vsphere;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.rest.internal.BaseHttpApiMetadata;
import org.jclouds.vsphere.compute.config.VSphereComputeServiceContextModule;
import org.jclouds.vsphere.config.VSphereConstants;

import java.net.URI;
import java.util.Properties;

import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;
import static org.jclouds.reflect.Reflection2.typeToken;

/**
 * Implementation of {@link BaseHttpApiMetadata} for vSphere 5.1 API
 * <p/>
 */
public class VSphereApiMetadata extends BaseHttpApiMetadata<VSphereApi> {


   @Override
   public Builder toBuilder() {
      return new Builder().fromApiMetadata(this);
   }

   public VSphereApiMetadata() {
      this(new Builder());
   }

   protected VSphereApiMetadata(Builder builder) {
      super(builder);
   }

   public static Properties defaultProperties() {
      Properties properties = BaseHttpApiMetadata.defaultProperties();
      properties.setProperty(PROPERTY_SESSION_INTERVAL, 8 * 60 + "");
      properties.setProperty("jclouds.dns_name_length_min", "1");
      properties.setProperty("jclouds.dns_name_length_max", "80");
      properties.setProperty(PROPERTY_SESSION_INTERVAL, 300 + "");
      properties.setProperty(VSphereConstants.JCLOUDS_VSPHERE_VM_PASSWORD, "master");
      properties.setProperty(VSphereConstants.CLONING, "full");
      return properties;
   }

   public static class Builder extends BaseHttpApiMetadata.Builder<VSphereApi, Builder> {
      protected Builder() {
         id("vsphere")
                 .name("vSphere 5.1 API")
                 .identityName("user")
                 .credentialName("password")
                 .endpointName("ESXi endpoint or vCenter server")
                         // .defaultEndpoint("https://localhost/sdk")
                 .documentation(URI.create("http://www.vmware.com/support/pubs/vcd_pubs.html"))
                 .version("5.1")
                 .defaultProperties(VSphereApiMetadata.defaultProperties())
                 .view(typeToken(ComputeServiceContext.class))
                 .defaultModules(ImmutableSet.<Class<? extends Module>>of(VSphereComputeServiceContextModule.class));
      }

      @Override
      public VSphereApiMetadata build() {
         return new VSphereApiMetadata(this);
      }

      @Override
      protected Builder self() {
         return this;
      }
   }
}
