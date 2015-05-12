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
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.vmware.vim25.CustomizationAdapterMapping;
import com.vmware.vim25.CustomizationDhcpIpGenerator;
import com.vmware.vim25.CustomizationFixedName;
import com.vmware.vim25.CustomizationGlobalIPSettings;
import com.vmware.vim25.CustomizationIPSettings;
import com.vmware.vim25.CustomizationLinuxPrep;
import com.vmware.vim25.CustomizationSpec;
import com.vmware.vim25.FileFault;
import com.vmware.vim25.InvalidName;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.SnapshotFault;
import com.vmware.vim25.TaskInProgress;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer1BackingInfo;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskRawDiskMappingVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer2BackingInfo;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineRelocateDiskMoveOptions;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRelocateSpecDiskLocator;
import com.vmware.vim25.VmConfigFault;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineSnapshot;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Named;
import javax.inject.Singleton;
import java.rmi.RemoteException;
import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;


@Singleton
public class MasterToVirtualMachineCloneSpec implements Function<VirtualMachine, VirtualMachineCloneSpec> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final ResourcePool resourcePool;
   private final Datastore datastore;
   private String cloningStrategy;
   private String linuxName;
   private final boolean postConfiguration;

   @Inject
   public MasterToVirtualMachineCloneSpec(ResourcePool resourcePool, Datastore datastore, String cloningStrategy, String linuxName, boolean postConfiguration) {
      this.resourcePool = resourcePool;
      this.datastore = datastore;
      this.cloningStrategy = cloningStrategy;
      this.linuxName = linuxName;
      this.postConfiguration = postConfiguration;
   }

   @Override
   public VirtualMachineCloneSpec apply(VirtualMachine master) {
      return prepareCloneSpec(master, resourcePool, datastore, linuxName, postConfiguration);
   }

   private VirtualMachineCloneSpec prepareCloneSpec(VirtualMachine master, ResourcePool resourcePool, Datastore datastore, String linuxName, boolean postConfiguration) {
      VirtualMachineRelocateSpec relocateSpec = null;
      VirtualMachineCloneSpec cloneSpec = null;

      try {
         relocateSpec = checkNotNull(configureRelocateSpec(resourcePool, datastore, master), "relocateSpec");
      } catch (Exception e) {
         logger.error("Can't configure relocate spec from vm " + master.getName(), e);
         throw propagate(e);
      }

      try {
         cloneSpec = checkNotNull(configureVirtualMachineCloneSpec(relocateSpec, linuxName, postConfiguration), "cloneSpec");
      } catch (Exception e) {
         logger.error("Can't configure clone spec from vm " + master.getName(), e);
         throw propagate(e);
      }
      return cloneSpec;
   }

   private VirtualMachineSnapshot getCurrentSnapshotOrCreate(String snapshotName, String snapshotDescription,
                                                             VirtualMachine master) throws InvalidName, VmConfigFault, SnapshotFault, TaskInProgress, FileFault,
           InvalidState, RuntimeFault, RemoteException {
      if (master.getSnapshot() == null) {
         Task task = master.createSnapshot_Task(snapshotName, snapshotDescription, false, false);
         try {
            if (task.waitForTask().equals(Task.SUCCESS)) {
               logger.debug(String.format("snapshot taken for '%s'", master.getName()));
            }
         } catch (Exception e) {
            logger.debug(String.format("Can't take snapshot for '%s'", master.getName()), e);
            throw propagate(e);
         }
      } else
         logger.debug(String.format("snapshot already available for '%s'", master.getName()));
      return master.getCurrentSnapShot();
   }

   private VirtualMachineRelocateSpec configureRelocateSpec(ResourcePool resourcePool, Datastore datastore, VirtualMachine master)
           throws Exception, InvalidProperty, RuntimeFault, RemoteException {
      VirtualMachineRelocateSpec rSpec = new VirtualMachineRelocateSpec();
      if (cloningStrategy.equals("linked")) {
         ArrayList<Integer> diskKeys = getIndependentVirtualDiskKeys(master);
         if (diskKeys.size() > 0) {
            Datastore[] dss = master.getDatastores();

            VirtualMachineRelocateSpecDiskLocator[] diskLocator = new VirtualMachineRelocateSpecDiskLocator[diskKeys.size()];
            int count = 0;
            for (Integer key : diskKeys) {
               diskLocator[count] = new VirtualMachineRelocateSpecDiskLocator();
               diskLocator[count].setDatastore(dss[0].getMOR());
               diskLocator[count]
                       .setDiskMoveType(VirtualMachineRelocateDiskMoveOptions.moveAllDiskBackingsAndDisallowSharing
                               .toString());
               diskLocator[count].setDiskId(key);
               count = count + 1;
            }
            rSpec.setDiskMoveType(VirtualMachineRelocateDiskMoveOptions.createNewChildDiskBacking.toString());
            rSpec.setDisk(diskLocator);
         } else {
            rSpec.setDiskMoveType(VirtualMachineRelocateDiskMoveOptions.createNewChildDiskBacking.toString());
         }
      } else if (cloningStrategy.equals("full")) {
         rSpec.setDatastore(datastore.getMOR());
         rSpec.setPool(resourcePool.getMOR());
         //rSpec.setHost();
      } else
         throw new Exception(String.format("Cloning strategy %s not supported", cloningStrategy));
      return rSpec;
   }

   private VirtualMachineCloneSpec configureVirtualMachineCloneSpec(VirtualMachineRelocateSpec rSpec, String linuxName, boolean postConfiguration) throws Exception {

      VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
      cloneSpec.setPowerOn(true);
      cloneSpec.setTemplate(false);
      //cloneSpec.setSnapshot(currentSnapshot.getMOR());
      cloneSpec.setLocation(rSpec);
      if (postConfiguration) {
         CustomizationSpec customizationSpec = new CustomizationSpec();
         CustomizationLinuxPrep linuxPrep = new CustomizationLinuxPrep();
         CustomizationFixedName fixedName = new CustomizationFixedName();
         fixedName.setName(linuxName);
         linuxPrep.setHostName(fixedName);
         linuxPrep.setDomain("");
         linuxPrep.setHwClockUTC(true);
         //linuxPrep.setTimeZone("Etc/UTC");
         customizationSpec.setIdentity(linuxPrep);
         customizationSpec.setGlobalIPSettings(new CustomizationGlobalIPSettings());
         CustomizationAdapterMapping[] nicSettingMap = new CustomizationAdapterMapping[1];
         nicSettingMap[0] = new CustomizationAdapterMapping();
         nicSettingMap[0].adapter = new CustomizationIPSettings();
         nicSettingMap[0].adapter.setIp(new CustomizationDhcpIpGenerator());
         customizationSpec.setNicSettingMap(nicSettingMap);
         cloneSpec.setCustomization(customizationSpec);
      }


      return cloneSpec;
   }

   private static ArrayList<Integer> getIndependentVirtualDiskKeys(VirtualMachine vm) throws Exception {
      ArrayList<Integer> diskKeys = Lists.newArrayList();

      VirtualDevice[] devices = (VirtualDevice[]) vm.getPropertyByPath("config.hardware.device");

      for (int i = 0; i < devices.length; i++) {
         if (devices[i] instanceof VirtualDisk) {
            VirtualDisk vDisk = (VirtualDisk) devices[i];
            String diskMode = "";
            VirtualDeviceBackingInfo vdbi = vDisk.getBacking();

            if (vdbi instanceof VirtualDiskFlatVer1BackingInfo) {
               diskMode = ((VirtualDiskFlatVer1BackingInfo) vdbi).getDiskMode();
            } else if (vdbi instanceof VirtualDiskFlatVer2BackingInfo) {
               diskMode = ((VirtualDiskFlatVer2BackingInfo) vdbi).getDiskMode();
            } else if (vdbi instanceof VirtualDiskRawDiskMappingVer1BackingInfo) {
               diskMode = ((VirtualDiskRawDiskMappingVer1BackingInfo) vdbi).getDiskMode();
            } else if (vdbi instanceof VirtualDiskSparseVer1BackingInfo) {
               diskMode = ((VirtualDiskSparseVer1BackingInfo) vdbi).getDiskMode();
            } else if (vdbi instanceof VirtualDiskSparseVer2BackingInfo) {
               diskMode = ((VirtualDiskSparseVer2BackingInfo) vdbi).getDiskMode();
            }

            if (diskMode.indexOf("independent") != -1) {
               diskKeys.add(vDisk.getKey());
            }
         }
      }
      return diskKeys;
   }

}
