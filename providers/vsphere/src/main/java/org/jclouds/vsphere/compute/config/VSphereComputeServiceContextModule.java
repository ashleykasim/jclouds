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
package org.jclouds.vsphere.compute.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.DistributedVirtualPortgroup;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import org.jclouds.compute.ComputeServiceAdapter;
import org.jclouds.compute.config.ComputeServiceAdapterContextModule;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.Location;
import org.jclouds.functions.IdentityFunction;
import org.jclouds.location.suppliers.LocationsSupplier;
import org.jclouds.ssh.SshClient;
import org.jclouds.vsphere.FileManagerApi;
import org.jclouds.vsphere.compute.options.VSphereTemplateOptions;
import org.jclouds.vsphere.domain.VSphereHost;
import org.jclouds.vsphere.domain.VSphereServiceInstance;
import org.jclouds.vsphere.functions.CreateAndConnectVSphereClient;
import org.jclouds.vsphere.functions.CreateOrGetTagsId;
import org.jclouds.vsphere.functions.GetRecommendedVSphereHost;
import org.jclouds.vsphere.functions.HostSystemToVSphereHost;
import org.jclouds.vsphere.functions.NetworkConfigSupplier;
import org.jclouds.vsphere.functions.VLanNameToDistributedVirtualPortgroup;
import org.jclouds.vsphere.functions.VirtualMachineToImage;
import org.jclouds.vsphere.functions.VirtualMachineToNodeMetadata;
import org.jclouds.vsphere.functions.VirtualMachineToSshClient;
import org.jclouds.vsphere.internal.VSphereFileManager;
import org.jclouds.vsphere.suppliers.VSphereHostSupplier;
import org.jclouds.vsphere.suppliers.VSphereLocationSupplier;

import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;

/**
 */
public class VSphereComputeServiceContextModule extends
        ComputeServiceAdapterContextModule<VirtualMachine, Hardware, Image, Location> {

   @Override
   protected void configure() {
      super.configure();

      //bind(ComputeService.class).to(VSphereComputeService.class);
      bind(TemplateOptions.class).to(VSphereTemplateOptions.class);
      bind(LocationsSupplier.class).to(VSphereLocationSupplier.class);
      bind(FileManagerApi.class).to(VSphereFileManager.class);

      bind(new TypeLiteral<ComputeServiceAdapter<VirtualMachine, Hardware, Image, Location>>() {
      }).to(VSphereComputeServiceAdapter.class);

      bind(new TypeLiteral<Function<String, DistributedVirtualPortgroup>>() {
      }).to(Class.class.cast(VLanNameToDistributedVirtualPortgroup.class));

      bind(new TypeLiteral<Function<Location, Location>>() {
      }).to(Class.class.cast(IdentityFunction.class));

      bind(new TypeLiteral<Function<Image, Image>>() {
      }).to(Class.class.cast(IdentityFunction.class));

      bind(new TypeLiteral<Function<Hardware, Hardware>>() {
      }).to(Class.class.cast(IdentityFunction.class));

      bind(new TypeLiteral<Function<VirtualMachine, NodeMetadata>>() {
      }).to(VirtualMachineToNodeMetadata.class);

      bind(new TypeLiteral<Function<HostSystem, VSphereHost>>() {
      }).to(HostSystemToVSphereHost.class);

      bind(new TypeLiteral<Supplier<VSphereServiceInstance>>() {
      }).to((Class) CreateAndConnectVSphereClient.class);

//        bind(new TypeLiteral<Supplier<Set<? extends Location>>>() {
//        }).to((Class) VSphereLocationSupplier.class);

      bind(new TypeLiteral<Supplier<VSphereHost>>() {
      }).to((Class) VSphereHostSupplier.class);

      bind(new TypeLiteral<Supplier<Map<String, CustomFieldDef>>>() {
      }).to((Class) CreateOrGetTagsId.class);

      bind(new TypeLiteral<Supplier<NetworkConfigSupplier>>() {
      }).to((Class) NetworkConfigSupplier.class);

      bind(new TypeLiteral<Function<VirtualMachine, SshClient>>() {
      }).to(VirtualMachineToSshClient.class);

      bind(new TypeLiteral<Function<VirtualMachine, Image>>() {
      }).to(VirtualMachineToImage.class);

      bind(new TypeLiteral<Function<String, VSphereHost>>() {
      }).to(GetRecommendedVSphereHost.class);
   }

   @VisibleForTesting
   public static final Map<VirtualMachinePowerState, NodeMetadata.Status> toPortableNodeStatus = ImmutableMap
           .<VirtualMachinePowerState, NodeMetadata.Status>builder()
           .put(VirtualMachinePowerState.poweredOff, NodeMetadata.Status.TERMINATED)
           .put(VirtualMachinePowerState.poweredOn, NodeMetadata.Status.RUNNING)
           .put(VirtualMachinePowerState.suspended, NodeMetadata.Status.SUSPENDED).build();

   @Singleton
   @Provides
   protected Map<VirtualMachinePowerState, NodeMetadata.Status> toPortableNodeStatus() {
      return toPortableNodeStatus;
   }

   @Provides
   @Singleton
   protected Function<Supplier<NodeMetadata>, ServiceInstance> client() {
      return new Function<Supplier<NodeMetadata>, ServiceInstance>() {

         @Override
         public ServiceInstance apply(Supplier<NodeMetadata> nodeSupplier) {
            try {
               return new ServiceInstance(new URL("https://localhost/sdk"), "root", "", true);
            } catch (RemoteException e) {
               Throwables.propagate(e);
               return null;
            } catch (MalformedURLException e) {
               Throwables.propagate(e);
               return null;
            }
         }

         @Override
         public String toString() {
            return "createInstanceByNodeId()";
         }

      };
   }

   @VisibleForTesting
   public static final Map<VirtualMachinePowerState, Image.Status> toPortableImageStatus = ImmutableMap
           .<VirtualMachinePowerState, Image.Status>builder().put(VirtualMachinePowerState.poweredOn, Image.Status.PENDING)
           .put(VirtualMachinePowerState.poweredOff, Image.Status.AVAILABLE)
           .put(VirtualMachinePowerState.suspended, Image.Status.PENDING)
           .build();

   @Singleton
   @Provides
   protected Map<VirtualMachinePowerState, Image.Status> toPortableImageStatus() {
      return toPortableImageStatus;
   }

   @Override
   protected TemplateOptions provideTemplateOptions(Injector injector, TemplateOptions options) {
      return options.as(VSphereTemplateOptions.class);
   }
}
