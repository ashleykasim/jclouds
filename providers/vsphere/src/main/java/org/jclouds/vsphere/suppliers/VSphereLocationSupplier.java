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

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationScope;
import org.jclouds.domain.internal.LocationImpl;
import org.jclouds.location.suppliers.LocationsSupplier;
import org.jclouds.logging.Logger;
import org.jclouds.vsphere.domain.VSphereServiceInstance;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class VSphereLocationSupplier implements LocationsSupplier {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private Supplier<VSphereServiceInstance> serviceInstance;

   @Inject
   public VSphereLocationSupplier(Supplier<VSphereServiceInstance> serviceInstance) {
      this.serviceInstance = checkNotNull(serviceInstance, "serviceInstance");
   }

   private Set<? extends Location> getClusters() {
      Set<Location> hosts = Sets.newHashSet();
      try (VSphereServiceInstance instance = serviceInstance.get();) {

         ManagedEntity[] clusterEntities = new InventoryNavigator(instance.getInstance().getRootFolder()).searchManagedEntities("ClusterComputeResource");

         for (ManagedEntity cluster : clusterEntities) {
            Location location = new LocationImpl(LocationScope.ZONE, cluster.getName(), cluster.getName(), null, ImmutableSet.of(""), Maps.<String, Object>newHashMap());
            hosts.add(location);
         }

         hosts.add(new LocationImpl(LocationScope.ZONE, "default", "default", null, ImmutableSet.of(""), Maps.<String, Object>newHashMap()));

         return hosts;
      } catch (Exception e) {
         logger.error("Problem in finding a valid cluster", e);
         Throwables.propagateIfPossible(e);
      }
      return hosts;
   }

   @Override
   public Set<? extends Location> get() {
      return ImmutableSet.copyOf(getClusters());
   }

}
