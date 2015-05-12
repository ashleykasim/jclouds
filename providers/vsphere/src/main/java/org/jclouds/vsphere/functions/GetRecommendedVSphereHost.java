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
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;
import org.jclouds.vsphere.domain.VSphereHost;
import org.jclouds.vsphere.domain.VSphereServiceInstance;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by igreenfi on 26/10/2014.
 */
public class GetRecommendedVSphereHost implements Function<String, VSphereHost> {
   @Nullable
   @Override
   public VSphereHost apply(@Nullable String dataCenter) {
      try (VSphereServiceInstance instance = serviceInstance.get();) {

         ManagedEntity[] clusterEntities = new InventoryNavigator(instance.getInstance().getRootFolder()).searchManagedEntities("ClusterComputeResource");
         Iterable<ClusterComputeResource> clusterComputeResources = Iterables.transform(Arrays.asList(clusterEntities), new Function<ManagedEntity, ClusterComputeResource>() {
            public ClusterComputeResource apply(ManagedEntity input) {
               return (ClusterComputeResource) input;
            }
         });

         HostSystem curHostSystem = null;
         for (ClusterComputeResource cluster : clusterComputeResources) {
            if (cluster.getName().equals(dataCenter) || dataCenter.equals("default")) {
               HostSystem[] hostSystems = cluster.getHosts();
               long maxMemory = Integer.MIN_VALUE;
               for (HostSystem hostSystem : hostSystems) {
                  int currentMemory = hostSystem.getSummary().getQuickStats().getOverallMemoryUsage();
                  long currentTotalMemory = hostSystem.getConfig().getSystemResources().getConfig().getMemoryAllocation().getLimit();
                  if (currentTotalMemory - currentMemory > maxMemory) {
                     curHostSystem = hostSystem;
                     maxMemory = currentTotalMemory - currentMemory;
                  }
               }
               break;
            }
         }
         return new VSphereHost(curHostSystem.getName(), serviceInstance.get());
//         return this.systemHostToVSphereHost.apply(curHostSystem);
      } catch (Exception e) {
         logger.error("Problem in finding a valid host: " + e.toString(), e);
      }
      return null;
   }

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private Supplier<VSphereServiceInstance> serviceInstance;
   private Function<HostSystem, VSphereHost> systemHostToVSphereHost;

   @Inject
   public GetRecommendedVSphereHost(Supplier<VSphereServiceInstance> serviceInstance,
                                    Function<HostSystem, VSphereHost> systemHostToVSphereHost) {
      this.serviceInstance = checkNotNull(serviceInstance, "serviceInstance");
      this.systemHostToVSphereHost = checkNotNull(systemHostToVSphereHost, "systemHostToVSphereHost");
   }

}
