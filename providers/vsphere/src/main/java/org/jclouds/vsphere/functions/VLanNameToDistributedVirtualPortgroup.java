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
import com.google.inject.Inject;
import com.vmware.vim25.mo.DistributedVirtualPortgroup;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;
import org.jclouds.vsphere.domain.VSphereServiceInstance;

import javax.annotation.Resource;
import javax.inject.Named;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class VLanNameToDistributedVirtualPortgroup implements Function<String, DistributedVirtualPortgroup> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final Supplier<VSphereServiceInstance> serviceInstance;

   @Inject
   public VLanNameToDistributedVirtualPortgroup(Supplier<VSphereServiceInstance> serviceInstance) {
      this.serviceInstance = checkNotNull(serviceInstance, "serviceInstance");
   }

   @Override
   public DistributedVirtualPortgroup apply(final String vlanName) {
      try {
         VSphereServiceInstance instance = serviceInstance.get();
         ManagedEntity entity = new InventoryNavigator(instance.getInstance().getRootFolder()).searchManagedEntity("DistributedVirtualPortgroup", vlanName);
         return (DistributedVirtualPortgroup) entity;
      } catch (Exception e) {
         logger.error("Problem in finding a valid DistributedVirtualPortgroup with name " + vlanName, e);
      }
      return null;
   }

}
