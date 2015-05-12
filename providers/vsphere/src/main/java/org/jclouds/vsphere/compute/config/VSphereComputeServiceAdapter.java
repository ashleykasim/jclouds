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

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.Description;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.FileTransferInformation;
import com.vmware.vim25.GuestProcessInfo;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.NoPermission;
import com.vmware.vim25.ParaVirtualSCSIController;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualController;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualFloppy;
import com.vmware.vim25.VirtualFloppyImageBackingInfo;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VirtualVmxnet3;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.DistributedVirtualPortgroup;
import com.vmware.vim25.mo.DistributedVirtualSwitch;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.GuestAuthManager;
import com.vmware.vim25.mo.GuestFileManager;
import com.vmware.vim25.mo.GuestOperationsManager;
import com.vmware.vim25.mo.GuestProcessManager;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import org.jclouds.compute.ComputeServiceAdapter;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.Volume;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.domain.Location;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.logging.Logger;
import org.jclouds.util.Predicates2;
import org.jclouds.vsphere.VSphereApiMetadata;
import org.jclouds.vsphere.compute.internal.GuestFilesUtils;
import org.jclouds.vsphere.compute.options.VSphereTemplateOptions;
import org.jclouds.vsphere.compute.strategy.NetworkConfigurationForNetworkAndOptions;
import org.jclouds.vsphere.compute.util.ListsUtils;
import org.jclouds.vsphere.config.VSphereConstants;
import org.jclouds.vsphere.domain.HardwareProfiles;
import org.jclouds.vsphere.domain.VSphereHost;
import org.jclouds.vsphere.domain.VSphereServiceInstance;
import org.jclouds.vsphere.domain.network.NetworkConfig;
import org.jclouds.vsphere.functions.FolderNameToFolderManagedEntity;
import org.jclouds.vsphere.functions.MasterToVirtualMachineCloneSpec;
import org.jclouds.vsphere.functions.VirtualMachineToImage;
import org.jclouds.vsphere.predicates.VSpherePredicate;
import org.jclouds.vsphere.util.ComputerNameValidator;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.jclouds.vsphere.config.VSphereConstants.CLONING;

/**
 */
@Singleton
public class VSphereComputeServiceAdapter implements
        ComputeServiceAdapter<VirtualMachine, Hardware, Image, Location> {

   private final ReentrantLock lock = new ReentrantLock();
   private final Function<String, DistributedVirtualPortgroup> distributedVirtualPortgroupFunction;

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   protected String vmInitPassword = null;

   private Supplier<VSphereServiceInstance> serviceInstance;
   private Supplier<Map<String, CustomFieldDef>> customFields;

   private final VirtualMachineToImage virtualMachineToImage;
   protected final NetworkConfigurationForNetworkAndOptions networkConfigurationForNetworkAndOptions;
   private final Supplier<VSphereHost> vSphereHost;
   private final Function<String, VSphereHost> hostFunction;

   @Inject
   public VSphereComputeServiceAdapter(Supplier<VSphereServiceInstance> serviceInstance, Supplier<Map<String, CustomFieldDef>> customFields, Supplier<VSphereHost> vSphereHost,
                                       VirtualMachineToImage virtualMachineToImage,
                                       Function<String, DistributedVirtualPortgroup> distributedVirtualSwitchFunction,
                                       NetworkConfigurationForNetworkAndOptions networkConfigurationForNetworkAndOptions,
                                       Function<String, VSphereHost> hostFunction,
                                       @Named(VSphereConstants.JCLOUDS_VSPHERE_VM_PASSWORD) String vmInitPassword) {
      this.serviceInstance = checkNotNull(serviceInstance, "serviceInstance");
      this.customFields = checkNotNull(customFields, "customFields");
      this.virtualMachineToImage = virtualMachineToImage;
      this.vmInitPassword = checkNotNull(vmInitPassword, "vmInitPassword");
      this.networkConfigurationForNetworkAndOptions = checkNotNull(networkConfigurationForNetworkAndOptions, "networkConfigurationForNetworkAndOptions");
      this.vSphereHost = checkNotNull(vSphereHost, "vSphereHost");
      this.distributedVirtualPortgroupFunction = distributedVirtualSwitchFunction;
      this.hostFunction = hostFunction;
   }

   @Override
   public NodeAndInitialCredentials<VirtualMachine> createNodeWithGroupEncodedIntoName(String tag, String name, Template template) {

      VSphereTemplateOptions vOptions = VSphereTemplateOptions.class.cast(template.getOptions());

      String datacenterName = vOptions.datacenterName();
      try (VSphereServiceInstance instance = this.serviceInstance.get();
           VSphereHost sphereHost = hostFunction.apply(datacenterName);
           /*VSphereHost sphereHost = vSphereHost.get();*/) {
         Folder rootFolder = instance.getInstance().getRootFolder();

         ComputerNameValidator.INSTANCE.validate(name);

         VirtualMachine master = getVMwareTemplate(template.getImage().getId(), rootFolder);
         ResourcePool resourcePool = checkNotNull(tryFindResourcePool(rootFolder, sphereHost.getHost().getName()).orNull(), "resourcePool");

         logger.trace("<< trying to use ResourcePool: " + resourcePool.getName());
        // VSphereTemplateOptions vOptions = VSphereTemplateOptions.class.cast(template.getOptions());


         VirtualMachineCloneSpec cloneSpec = new MasterToVirtualMachineCloneSpec(resourcePool, sphereHost.getDatastore(),
                 VSphereApiMetadata.defaultProperties().getProperty(CLONING), name, vOptions.postConfiguration()).apply(master);


         Set<String> networks = vOptions.getNetworks();

         VirtualMachineConfigSpec virtualMachineConfigSpec = new VirtualMachineConfigSpec();
         virtualMachineConfigSpec.setMemoryMB((long) template.getHardware().getRam());
         if (template.getHardware().getProcessors().size() > 0)
            virtualMachineConfigSpec.setNumCPUs((int) template.getHardware().getProcessors().get(0).getCores());
         else
            virtualMachineConfigSpec.setNumCPUs(1);


         Set<NetworkConfig> networkConfigs = Sets.newHashSet();
         for (String network : networks) {
            NetworkConfig config = networkConfigurationForNetworkAndOptions.apply(network, vOptions);
            networkConfigs.add(config);
         }


         List<VirtualDeviceConfigSpec> updates = configureVmHardware(name, template, master, vOptions, networkConfigs);
         virtualMachineConfigSpec.setDeviceChange(updates.toArray(new VirtualDeviceConfigSpec[updates.size()]));

         cloneSpec.setConfig(virtualMachineConfigSpec);

         vOptions.getPublicKey();

         VirtualMachine cloned = null;
         try {
            cloned = cloneMaster(master, tag, name, cloneSpec, vOptions.vmFolder());
            Set<String> tagsFromOption = vOptions.getTags();
            if (tagsFromOption.size() > 0) {
               String tags = Joiner.on(",").join(vOptions.getTags());
               cloned.getServerConnection().getServiceInstance().getCustomFieldsManager().setField(cloned, customFields.get().get(VSphereConstants.JCLOUDS_TAGS).getKey(), tags);
               cloned.getServerConnection().getServiceInstance().getCustomFieldsManager().setField(cloned, customFields.get().get(VSphereConstants.JCLOUDS_GROUP).getKey(), tag);
               if (vOptions.postConfiguration())
                  postConfiguration(cloned, name, tag, networkConfigs);
               else {
                  VSpherePredicate.WAIT_FOR_VMTOOLS(1000 * 60 * 60 * 2, TimeUnit.MILLISECONDS).apply(cloned);
               }
            }
         } catch (Exception e) {
            logger.error("Can't clone vm " + master.getName() + ", Error message: " + e.toString(), e);
            propagate(e);
         }

         checkAndRecoverNicConfiguration(serviceInstance.get(), cloned);

         NodeAndInitialCredentials<VirtualMachine> nodeAndInitialCredentials = new NodeAndInitialCredentials<VirtualMachine>(cloned, cloned.getName(),
                 LoginCredentials.builder().user("root")
                         .password(vmInitPassword)
                         .build());
         return nodeAndInitialCredentials;
      } catch (Throwable t) {
         logger.error("Got ERROR while create new VM : " + t.toString());
         Throwables.propagateIfPossible(t);
      }
      return null;
   }

   private void checkAndRecoverNicConfiguration(VSphereServiceInstance instance, VirtualMachine vm) throws RemoteException, InterruptedException {
      try {
         List<VirtualDeviceConfigSpec> updates = Lists.newArrayList();
         for (VirtualDevice device : vm.getConfig().getHardware().getDevice()) {
            if (device instanceof VirtualEthernetCard) {
               VirtualEthernetCard ethernetCard = (VirtualEthernetCard) device;
               if (ethernetCard.getConnectable().connected)
                  continue;

               VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
               ethernetCard.getConnectable().setConnected(true);
               ethernetCard.getConnectable().setStartConnected(true);

               nicSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);
               nicSpec.setDevice(device);

               updates.add(nicSpec);
            }
         }

         if (updates.size() == 0)
            return;

         VirtualMachineConfigSpec spec = new VirtualMachineConfigSpec();
         spec.setDeviceChange(updates.toArray(new VirtualDeviceConfigSpec[updates.size()]));
         Task task = vm.reconfigVM_Task(spec);
         String result = task.waitForTask();
         if (result.equals(Task.SUCCESS)) {
            GuestOperationsManager gom = instance.getInstance().getGuestOperationsManager();
            GuestAuthManager gam = gom.getAuthManager(vm);
            final NamePasswordAuthentication npa = new NamePasswordAuthentication();
            npa.setUsername("root");
            npa.setPassword(vmInitPassword);
            GuestProgramSpec gps = new GuestProgramSpec();
            gps.programPath = "/bin/sh";
            gps.arguments = "-c \"service network restart\"";
            List<String> env = Lists.newArrayList("PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin", "SHELL=/bin/bash");
            gps.setEnvVariables(env.toArray(new String[env.size()]));
            GuestProcessManager gpm = gom.getProcessManager(vm);
            final long pid = gpm.startProgramInGuest(npa, gps);
            Predicates2.retry(new Predicate<GuestProcessManager>() {
               @Override
               public boolean apply(GuestProcessManager o) {
                  try {
                     GuestProcessInfo[] guestProcessInfos = o.listProcessesInGuest(npa, new long[]{pid});
                     return guestProcessInfos == null || guestProcessInfos.length == 0;
                  } catch (RemoteException e) {
                     return false;
                  }

               }
            }, 20 * 1000, 1000, TimeUnit.MILLISECONDS).apply(gpm);
         }
      } catch (Exception e) {
         logger.warn("Got Exception while checking NIC status : " + e.toString());
      }
   }

   private List<VirtualDeviceConfigSpec> configureVmHardware(String name, Template template, VirtualMachine master, VSphereTemplateOptions vOptions, Set<NetworkConfig> networkConfigs) {
      List<VirtualDeviceConfigSpec> updates = Lists.newArrayList();
      long currentDiskSize = 0;
      int numberOfHardDrives = 0;

      for (VirtualDevice device : master.getConfig().getHardware().getDevice()) {
         if (device instanceof VirtualDisk) {
            VirtualDisk vd = (VirtualDisk) device;
            currentDiskSize += vd.getCapacityInKB();
            numberOfHardDrives++;
         }
      }

      for (VirtualDevice device : master.getConfig().getHardware().getDevice()) {

         if (device instanceof VirtualEthernetCard) {
            VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.remove);
            nicSpec.setDevice(device);
            updates.add(nicSpec);
         } else if (device instanceof VirtualCdrom) {
            if (vOptions.isoFileName() != null) {
               VirtualCdrom vCdrom = (VirtualCdrom) device;
               VirtualDeviceConfigSpec cdSpec = new VirtualDeviceConfigSpec();
               cdSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);

               VirtualCdromIsoBackingInfo iso = new VirtualCdromIsoBackingInfo();
               Datastore datastore = vSphereHost.get().getDatastore();
               VirtualDeviceConnectInfo cInfo = new VirtualDeviceConnectInfo();
               cInfo.setStartConnected(true);
               cInfo.setConnected(true);
               iso.setDatastore(datastore.getMOR());
               iso.setFileName("[" + datastore.getName() + "] " + vOptions.isoFileName());

               vCdrom.setConnectable(cInfo);
               vCdrom.setBacking(iso);
               cdSpec.setDevice(vCdrom);
               updates.add(cdSpec);
            }
         } else if (device instanceof VirtualFloppy) {
            if (vOptions.flpFileName() != null) {
               VirtualFloppy vFloppy = (VirtualFloppy) device;
               VirtualDeviceConfigSpec floppySpec = new VirtualDeviceConfigSpec();
               floppySpec.setOperation(VirtualDeviceConfigSpecOperation.edit);

               VirtualFloppyImageBackingInfo image = new VirtualFloppyImageBackingInfo();
               Datastore datastore = vSphereHost.get().getDatastore();
               VirtualDeviceConnectInfo cInfo = new VirtualDeviceConnectInfo();
               cInfo.setStartConnected(true);
               cInfo.setConnected(true);
               image.setDatastore(datastore.getMOR());
               image.setFileName("[" + datastore.getName() + "] " + vOptions.flpFileName());

               vFloppy.setConnectable(cInfo);
               vFloppy.setBacking(image);
               floppySpec.setDevice(vFloppy);
               updates.add(floppySpec);
            }
         } else if (device instanceof VirtualLsiLogicController || device instanceof ParaVirtualSCSIController) {
            //int unitNumber = master.getConfig().getHardware().getDevice().length;
            int unitNumber = numberOfHardDrives;
            List<? extends Volume> volumes = template.getHardware().getVolumes();
            VirtualController controller = (VirtualController) device;
            String dsName = this.hostFunction.apply(vOptions.datacenterName()).getDatastore().getName();
            for (Volume volume : volumes) {

               long currentVolumeSize = 1024 * 1024 * volume.getSize().longValue();

               if (currentVolumeSize <= currentDiskSize)
                  continue;

               VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();

               VirtualDisk disk = new VirtualDisk();
               VirtualDiskFlatVer2BackingInfo diskFileBacking = new VirtualDiskFlatVer2BackingInfo();

               int ckey = controller.getKey();
               if (controller.getDevice().length >= 15)
                  throw new IllegalArgumentException("Can't add more then 15 HHD to controller.");
               unitNumber = controller.getDevice().length + 1;

               String fileName = "[" + dsName + "] " + name + "/" + name + unitNumber + ".vmdk";

               diskFileBacking.setFileName(fileName);
               diskFileBacking.setDiskMode("persistent");
               diskFileBacking.setThinProvisioned(true);

               disk.setControllerKey(ckey);
               disk.setUnitNumber(unitNumber);
               disk.setBacking(diskFileBacking);
               long size = currentVolumeSize - currentDiskSize;
               logger.trace("<< current disk size: " + currentDiskSize + "KB. (" + name + ")");
               logger.trace("<< adding disk size: " + size + "KB. (" + name + ")");
               disk.setCapacityInKB(size);
               disk.setKey(-1);

               diskSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
               diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.create);
               diskSpec.setDevice(disk);
               updates.add(diskSpec);
            }

         }
      }
      updates.addAll(createNicSpec(networkConfigs, vOptions.postConfiguration(), vOptions.distributedVirtualSwitch()));

      return updates;
   }

   private Iterable<VirtualMachine> listNodes(VSphereServiceInstance instance) {
      Iterable<VirtualMachine> vms = ImmutableSet.of();
      try {
         Folder nodesFolder = instance.getInstance().getRootFolder();
         ManagedEntity[] managedEntities = new InventoryNavigator(nodesFolder).searchManagedEntities("VirtualMachine");
         vms = Iterables.transform(Arrays.asList(managedEntities), new Function<ManagedEntity, VirtualMachine>() {
            public VirtualMachine apply(ManagedEntity input) {
               return (VirtualMachine) input;
            }
         });
      } catch (Throwable e) {
         logger.error("Can't find vm", e);
      }
      return vms;
   }

   @Override
   public Iterable<VirtualMachine> listNodes() {
      try (VSphereServiceInstance instance = serviceInstance.get();) {
         return listNodes(instance);
      } catch (Throwable e) {
         logger.error("Can't find vm", e);
         Throwables.propagateIfPossible(e);
         return ImmutableSet.of();
      }
   }

   @Override
   public Iterable<VirtualMachine> listNodesByIds(Iterable<String> ids) {

      Iterable<VirtualMachine> vms = ImmutableSet.of();
      try (VSphereServiceInstance instance = serviceInstance.get();) {
         Folder nodesFolder = instance.getInstance().getRootFolder();
         List<List<String>> list = new ArrayList<List<String>>();
         Iterator<String> idsIterator = ids.iterator();

         while (idsIterator.hasNext()) {
            list.add(Lists.newArrayList("VirtualMachine", idsIterator.next()));
         }

         String[][] typeInfo = ListsUtils.ListToArray(list);

         ManagedEntity[] managedEntities = new InventoryNavigator(nodesFolder).searchManagedEntities(
                 typeInfo, true);
         vms = Iterables.transform(Arrays.asList(managedEntities), new Function<ManagedEntity, VirtualMachine>() {
            public VirtualMachine apply(ManagedEntity input) {
               return (VirtualMachine) input;
            }
         });
      } catch (Throwable e) {
         logger.error("Can't find vms ", e);
      }
      return vms;


//      Iterable<VirtualMachine> nodes = listNodes();
//      Iterable<VirtualMachine> selectedNodes = Iterables.filter(nodes, VSpherePredicate.isNodeIdInList(ids));
//      return selectedNodes;
   }


   @Override
   public Iterable<Hardware> listHardwareProfiles() {
      Set<org.jclouds.compute.domain.Hardware> hardware = Sets.newLinkedHashSet();
      hardware.add(HardwareProfiles.C1_M1_D10.getHardware());
      hardware.add(HardwareProfiles.C2_M2_D30.getHardware());
      hardware.add(HardwareProfiles.C2_M2_D50.getHardware());
      hardware.add(HardwareProfiles.C2_M4_D50.getHardware());
      hardware.add(HardwareProfiles.C2_M10_D80.getHardware());
      hardware.add(HardwareProfiles.C3_M10_D80.getHardware());
      hardware.add(HardwareProfiles.C4_M4_D20.getHardware());
      hardware.add(HardwareProfiles.C2_M6_D40.getHardware());
      hardware.add(HardwareProfiles.C8_M16_D30.getHardware());
      hardware.add(HardwareProfiles.C8_M16_D80.getHardware());

      return hardware;
   }

   @Override
   public Iterable<Image> listImages() {
      try (VSphereServiceInstance instance = serviceInstance.get();) {
         Iterable<VirtualMachine> nodes = listNodes(instance);
         Iterable<VirtualMachine> templates = Iterables.filter(nodes, VSpherePredicate.isTemplatePredicate);
         Iterable<Image> images = Iterables.transform(templates, virtualMachineToImage);
         return FluentIterable.from(images).toList();

      } catch (Throwable t) {
         Throwables.propagateIfPossible(t);
         return ImmutableSet.of();
      }
   }

   @Override
   public Iterable<Location> listLocations() {
      // Not using the adapter to determine locations
      return ImmutableSet.<Location>of();
   }

   @Override
   public VirtualMachine getNode(String vmName) {
      try (VSphereServiceInstance instance = serviceInstance.get()) {
         return getVM(vmName, instance.getInstance().getRootFolder());
      } catch (Throwable e) {
         Throwables.propagateIfPossible(e);
      }
      return null;
   }

   @Override
   public void destroyNode(String vmName) {
      try (VSphereServiceInstance instance = serviceInstance.get();) {
         VirtualMachine virtualMachine = getVM(vmName, instance.getInstance().getRootFolder());
         Task powerOffTask = virtualMachine.powerOffVM_Task();
         if (powerOffTask.waitForTask().equals(Task.SUCCESS))
            logger.debug(String.format("VM %s powered off", vmName));
         else
            logger.debug(String.format("VM %s could not be powered off", vmName));

         Task destroyTask = virtualMachine.destroy_Task();
         if (destroyTask.waitForTask().equals(Task.SUCCESS))
            logger.debug(String.format("VM %s destroyed", vmName));
         else
            logger.debug(String.format("VM %s could not be destroyed", vmName));
      } catch (Exception e) {
         logger.error("Can't destroy vm " + vmName, e);
         Throwables.propagateIfPossible(e);
      }
   }

   @Override
   public void rebootNode(String vmName) {
      VirtualMachine virtualMachine = getNode(vmName);

      try {
         virtualMachine.rebootGuest();
      } catch (Exception e) {
         logger.error("Can't reboot vm " + vmName, e);
         propagate(e);
      }
      logger.debug(vmName + " rebooted");
   }

   @Override
   public void resumeNode(String vmName) {
      VirtualMachine virtualMachine = getNode(vmName);

      if (virtualMachine.getRuntime().getPowerState().equals(VirtualMachinePowerState.poweredOff)) {
         try {
            Task task = virtualMachine.powerOnVM_Task(null);
            if (task.waitForTask().equals(Task.SUCCESS))
               logger.debug(virtualMachine.getName() + " resumed");
         } catch (Exception e) {
            logger.error("Can't resume vm " + vmName, e);
            propagate(e);
         }

      } else
         logger.debug(vmName + " can't be resumed");
   }

   @Override
   public void suspendNode(String vmName) {
      VirtualMachine virtualMachine = getNode(vmName);

      try {
         Task task = virtualMachine.suspendVM_Task();
         if (task.waitForTask().equals(Task.SUCCESS))
            logger.debug(vmName + " suspended");
         else
            logger.debug(vmName + " can't be suspended");
      } catch (Exception e) {
         logger.error("Can't suspend vm " + vmName, e);
         propagate(e);
      }
   }

   @Override
   public Image getImage(String imageName) {
      try (VSphereServiceInstance instance = serviceInstance.get();) {
         return virtualMachineToImage.apply(getVMwareTemplate(imageName, instance.getInstance().getRootFolder()));
      } catch (Throwable t) {
         Throwables.propagateIfPossible(t);
         return null;
      }
   }

   private VirtualMachine cloneMaster(VirtualMachine master, String tag, String name, VirtualMachineCloneSpec cloneSpec, String folderName) {

      VirtualMachine cloned = null;
      try {
         FolderNameToFolderManagedEntity toFolderManagedEntity = new FolderNameToFolderManagedEntity(serviceInstance, master);
         Folder folder = toFolderManagedEntity.apply(folderName);
         Task task = master.cloneVM_Task(folder, name, cloneSpec);
         String result = task.waitForTask();
         if (result.equals(Task.SUCCESS)) {
            logger.trace("<< after clone search for VM with name: " + name);
            Retryer<VirtualMachine> retryer = RetryerBuilder.<VirtualMachine>newBuilder()
                    .retryIfResult(Predicates.<VirtualMachine>isNull())
                    .withStopStrategy(StopStrategies.stopAfterAttempt(5))
                    .retryIfException().withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                    .build();
            cloned = retryer.call(new GetVirtualMachineCallable(name, folder, serviceInstance.get().getInstance().getRootFolder()));
         } else {
            String errorMessage = task.getTaskInfo().getError().getLocalizedMessage();
            logger.error(errorMessage);
         }
      } catch (Exception e) {
         if (e instanceof NoPermission){
            NoPermission noPermission = (NoPermission)e;
            logger.error("NoPermission: " + noPermission.getPrivilegeId());
         }
         logger.error("Can't clone vm: " + e.toString(), e);
         propagate(e);
      }
      if (cloned == null)
         logger.error("<< Failed to get cloned VM. " + name);
      return checkNotNull(cloned, "cloned");
   }

   public class GetVirtualMachineCallable implements Callable<VirtualMachine> {
      private String vmName = null;
      private Folder folder = null;
      private Folder rootFolder = null;

      GetVirtualMachineCallable(String vmName, Folder folder, Folder rootFolder) {
         this.vmName = vmName;
         this.folder = folder;
         this.rootFolder = rootFolder;
      }

      @Override
      public VirtualMachine call() throws Exception {
         VirtualMachine cloned = null;
         cloned = getVM(vmName, folder);
         if (cloned == null)
            cloned = getVM(vmName, rootFolder);
         return cloned;
      }
   }

   private static void sleep(long time) {
      try {
         Thread.sleep(time);
      } catch (InterruptedException e) {

      }
   }

   private Optional<ResourcePool> tryFindResourcePool(Folder folder, String hostname) {
      Iterable<ResourcePool> resourcePools = ImmutableSet.<ResourcePool>of();
      try {
         ManagedEntity[] resourcePoolEntities = new InventoryNavigator(folder).searchManagedEntities("ResourcePool");
         resourcePools = Iterables.transform(Arrays.asList(resourcePoolEntities), new Function<ManagedEntity, ResourcePool>() {
            public ResourcePool apply(ManagedEntity input) {
               return (ResourcePool) input;
            }
         });
         Optional<ResourcePool> optionalResourcePool = Iterables.tryFind(resourcePools, VSpherePredicate.isResourcePoolOf(hostname));
         return optionalResourcePool;
      } catch (Exception e) {
         logger.error("Problem in finding a valid resource pool", e);
      }
      return Optional.absent();
   }

   private VirtualMachine getVM(String vmName, Folder nodesFolder) {
      logger.trace(">> search for vm with name : " + vmName);
      VirtualMachine vm = null;
      try {
         vm = (VirtualMachine) new InventoryNavigator(nodesFolder).searchManagedEntity("VirtualMachine", vmName);
      } catch (Exception e) {
         logger.error("Can't find vm", e);
         propagate(e);
      }
      return vm;
   }

   private VirtualMachine getVMwareTemplate(String imageName, Folder rootFolder) {
      VirtualMachine image = null;
      try {
         VirtualMachine node = getVM(imageName, rootFolder);
         if (VSpherePredicate.isTemplatePredicate.apply(node))
            image = node;
      } catch (Exception e) {
         logger.error("cannot find an image called " + imageName, e);
         propagate(e);
      }
      return checkNotNull(image, "image with name " + imageName + " not found.");
   }

   private List<VirtualDeviceConfigSpec> createNicSpec(Set<NetworkConfig> networks, boolean hasVMWareTools, boolean distributedSwitch) {
      List<VirtualDeviceConfigSpec> nics = Lists.newArrayList();
      int i = 0;
      for (NetworkConfig net : networks) {
         VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
         nicSpec.setOperation(VirtualDeviceConfigSpecOperation.add);

         VirtualEthernetCard nic = null;
         if (hasVMWareTools)
            nic = new VirtualVmxnet3();
         else
            nic = new VirtualPCNet32();

         VirtualDeviceBackingInfo nicBacking = null;
         if (distributedSwitch) {

            DistributedVirtualPortgroup virtualPortgroup = distributedVirtualPortgroupFunction.apply(net.getNetworkName());

            nicBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
            DistributedVirtualSwitchPortConnection port = new DistributedVirtualSwitchPortConnection();
            port.setPortgroupKey(virtualPortgroup.getKey());
            DistributedVirtualSwitch distributedVirtualSwitch = new DistributedVirtualSwitch(this.serviceInstance.get().getInstance().getServerConnection(), virtualPortgroup.getConfig().getDistributedVirtualSwitch());
            port.setSwitchUuid(distributedVirtualSwitch.getUuid());
            ((VirtualEthernetCardDistributedVirtualPortBackingInfo) nicBacking).setPort(port);
         } else {
            nicBacking = new VirtualEthernetCardNetworkBackingInfo();
            ((VirtualEthernetCardNetworkBackingInfo) nicBacking).setDeviceName(net.getNetworkName());
            Description info = new Description();
            info.setLabel(net.getNicName());
            info.setLabel("" + i);
            info.setSummary(net.getNetworkName());
            nic.setDeviceInfo(info);
            nic.setAddressType(net.getAddressType());
         }
         nic.setWakeOnLanEnabled(true);

         VirtualDeviceConnectInfo deviceConnectInfo = new VirtualDeviceConnectInfo();
         deviceConnectInfo.setConnected(true);
         deviceConnectInfo.setStartConnected(true);
         deviceConnectInfo.setAllowGuestControl(true);

         nic.setConnectable(deviceConnectInfo);
         nic.setBacking(nicBacking);
         nic.setKey(i);
         nicSpec.setDevice(nic);
         nics.add(nicSpec);
         i++;
      }
      return nics;
   }

   private void waitForPort(VirtualMachine vm, int port, long timeout) {
      GuestOperationsManager gom = serviceInstance.get().getInstance().getGuestOperationsManager();
      GuestAuthManager gam = gom.getAuthManager(vm);
      NamePasswordAuthentication npa = new NamePasswordAuthentication();
      npa.setUsername("root");
      npa.setPassword(vmInitPassword);
      GuestProgramSpec gps = new GuestProgramSpec();
      gps.programPath = "/bin/sh";

      StringBuilder openPortScript = new StringBuilder("netstat -nat | grep LIST | grep -q ':" + port + " ' && touch /tmp/portopen.txt");


      gps.arguments = "-c \"" + openPortScript.toString() + "\"";

      List<String> env = Lists.newArrayList("PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin",
              "SHELL=/bin/bash");

      gps.setEnvVariables(env.toArray(new String[env.size()]));
      GuestProcessManager gpm = gom.getProcessManager(vm);
      try {
         long pid = gpm.startProgramInGuest(npa, gps);

         GuestFileManager guestFileManager = vm.getServerConnection().getServiceInstance().getGuestOperationsManager().getFileManager(vm);
         FileTransferInformation fti = guestFileManager.initiateFileTransferFromGuest(npa, "/tmp/portopen.txt");
         if (fti.getSize() == 0)
            logger.debug(" ");
      } catch (RemoteException e) {
         logger.error(e.getMessage(), e);
         Throwables.propagate(e);
      }
   }


   private void postConfiguration(VirtualMachine vm, String name, String group, Set<NetworkConfig> networkConfigs) {
      if (!vm.getConfig().isTemplate())
         VSpherePredicate.WAIT_FOR_VMTOOLS(10 * 1000 * 60, TimeUnit.MILLISECONDS).apply(vm);

      GuestOperationsManager gom = serviceInstance.get().getInstance().getGuestOperationsManager();
      NamePasswordAuthentication npa = new NamePasswordAuthentication();
      npa.setUsername("root");
      npa.setPassword(vmInitPassword);
      GuestProgramSpec gps = new GuestProgramSpec();

      InputStream in = VSphereComputeServiceAdapter.class.getResourceAsStream("/postConfigurationScript.sh");
      GuestFilesUtils.loadFileToGuest(vm, in, npa, serviceInstance.get(), "/tmp/ping_dns.sh");

      gps.programPath = "/bin/sh";

      StringBuilder ethScript = new StringBuilder();

      ethScript.append("/tmp/ping_dns.sh > jclouds.log 2>&1");

      gps.arguments = "-c \"" + ethScript.toString() + "\"";

      List<String> env = Lists.newArrayList("PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin",
              "SHELL=/bin/bash");

      waitForStateToChange();
      if (!vm.getConfig().isTemplate())
         VSpherePredicate.WAIT_FOR_VMTOOLS(10 * 1000 * 60, TimeUnit.MILLISECONDS).apply(vm);
      waitForStateToChange();

      gps.setEnvVariables(env.toArray(new String[env.size()]));
      GuestProcessManager gpm = gom.getProcessManager(vm);
      try {
         long pid = gpm.startProgramInGuest(npa, gps);
         GuestProcessInfo[] processInfos = gpm.listProcessesInGuest(npa, new long[]{pid});
         if (null != processInfos) {
            for (GuestProcessInfo processInfo : processInfos) {
               while (processInfo.getExitCode() == null) {
                  processInfos = gpm.listProcessesInGuest(npa, new long[]{pid});
                  processInfo = processInfos[0];
               }
               if (processInfo.getExitCode() != 0) {
                  logger.warn("failed to run init script on node ( " + name + " ) exit code : " + processInfo.getExitCode());
                  //Throwables.propagate(new Exception("Failed to customize vm ( " + name + " )"));
               }
            }
         }
         logger.trace("<< process pid : " + pid);
      } catch (Exception e) {
      }

   }

   private void waitForStateToChange() {
      try {
         Thread.sleep(10 * 1000);
      } catch (InterruptedException e) {

      }
   }
}
