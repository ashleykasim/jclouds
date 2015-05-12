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

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;
import org.jclouds.vsphere.domain.VSphereHost;
import org.jclouds.vsphere.domain.VSphereServiceInstance;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class VSphereHostSupplier implements Supplier<VSphereHost> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private Supplier<VSphereServiceInstance> serviceInstance;
   private Function<HostSystem, VSphereHost> systemHostToVSphereHost;

   @Inject
   public VSphereHostSupplier(Supplier<VSphereServiceInstance> serviceInstance,
                              Function<HostSystem, VSphereHost> systemHostToVSphereHost) {
      this.serviceInstance = checkNotNull(serviceInstance, "serviceInstance");
      this.systemHostToVSphereHost = checkNotNull(systemHostToVSphereHost, "systemHostToVSphereHost");
   }

   private HostSystem getSystemHost() {
      Iterable<HostSystem> hosts = ImmutableSet.<HostSystem>of();
      try {
         VSphereServiceInstance instance = serviceInstance.get();
         ManagedEntity[] hostEntities = new InventoryNavigator(instance.getInstance().getRootFolder()).searchManagedEntities("HostSystem");
         hosts = Iterables.transform(Arrays.asList(hostEntities), new Function<ManagedEntity, HostSystem>() {
            public HostSystem apply(ManagedEntity input) {
               return (HostSystem) input;
            }
         });

         HostSystem curHostSystem = null;
         long maxMemory = Integer.MIN_VALUE;
         for (HostSystem hostSystem : hosts) {
            int currentMemory = hostSystem.getSummary().getQuickStats().getOverallMemoryUsage();
            long currentTotalMemory = hostSystem.getConfig().getSystemResources().getConfig().getMemoryAllocation().getLimit();
            if (currentTotalMemory - currentMemory > maxMemory) {
               curHostSystem = hostSystem;
               maxMemory = currentTotalMemory - currentMemory;
            }
         }

         return curHostSystem;
      } catch (Exception e) {
         logger.error("Problem in finding a valid host: " + e.toString(), e);
      }
      return null;
   }

   @Override
   public VSphereHost get() {
      return systemHostToVSphereHost.apply(getSystemHost());
   }

}
