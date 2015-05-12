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
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.CustomFieldValue;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.GuestProcessInfo;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineToolsStatus;
import com.vmware.vim25.mo.DistributedVirtualPortgroup;
import com.vmware.vim25.mo.GuestAuthManager;
import com.vmware.vim25.mo.GuestOperationsManager;
import com.vmware.vim25.mo.GuestProcessManager;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadata.Status;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.logging.Logger;
import org.jclouds.util.InetAddresses2;
import org.jclouds.util.Predicates2;
import org.jclouds.vsphere.config.VSphereConstants;
import org.jclouds.vsphere.domain.VSphereServiceInstance;
import org.jclouds.vsphere.predicates.VSpherePredicate;

import javax.annotation.Resource;
import javax.inject.Named;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Sets.newHashSet;

@Singleton
public class VirtualMachineToNodeMetadata implements Function<VirtualMachine, NodeMetadata> {

   public static final Splitter COMMA_SPLITTER = Splitter.on(",");
   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final Map<VirtualMachinePowerState, Status> toPortableNodeStatus;
   private final Supplier<Map<String, CustomFieldDef>> customFields;
   private final Supplier<VSphereServiceInstance> serviceInstanceSupplier;
   private final Function<String, DistributedVirtualPortgroup> distributedVirtualPortgroupFunction;
   protected String vmInitPassword = null;

   @Inject
   public VirtualMachineToNodeMetadata(Map<VirtualMachinePowerState, NodeMetadata.Status> toPortableNodeStatus,
                                       Supplier<Map<String, CustomFieldDef>> customFields,
                                       Supplier<VSphereServiceInstance> serviceInstanceSupplier,
                                       Function<String, DistributedVirtualPortgroup> distributedVirtualPortgroupFunction,
                                       @Named(VSphereConstants.JCLOUDS_VSPHERE_VM_PASSWORD) String vmInitPassword) {
      this.toPortableNodeStatus = checkNotNull(toPortableNodeStatus, "PortableNodeStatus");
      this.customFields = checkNotNull(customFields, "customFields");
      this.serviceInstanceSupplier = checkNotNull(serviceInstanceSupplier, "serviceInstanceSupplier");
      this.distributedVirtualPortgroupFunction = checkNotNull(distributedVirtualPortgroupFunction, "distributedVirtualPortgroupFunction");
      this.vmInitPassword = vmInitPassword;
   }

   @Override
   public NodeMetadata apply(VirtualMachine vm) {

      VirtualMachine freshVm = null;
      String virtualMachineName = "";
      NodeMetadataBuilder nodeMetadataBuilder = new NodeMetadataBuilder();
      try (VSphereServiceInstance instance = serviceInstanceSupplier.get();) {
         String vmMORId = vm.getMOR().get_value();
         ManagedEntity[] vms = new InventoryNavigator(instance.getInstance().getRootFolder()).searchManagedEntities("VirtualMachine");
         for (ManagedEntity machine : vms) {

            if (machine.getMOR().getVal().equals(vmMORId)) {
               freshVm = (VirtualMachine) machine;
               break;
            }
         }
         LocationBuilder locationBuilder = new LocationBuilder();
         locationBuilder.description("");
         locationBuilder.id("");
         locationBuilder.scope(LocationScope.HOST);

         if (freshVm == null) {
            nodeMetadataBuilder.status(Status.ERROR).id("");
            return nodeMetadataBuilder.build();
         }
         virtualMachineName = freshVm.getName();

         logger.trace("<< converting vm (" + virtualMachineName + ") to NodeMetadata");

         VirtualMachinePowerState vmState = freshVm.getRuntime().getPowerState();
         NodeMetadata.Status nodeState = toPortableNodeStatus.get(vmState);
         if (nodeState == null)
            nodeState = Status.UNRECOGNIZED;


         nodeMetadataBuilder.name(virtualMachineName).ids(virtualMachineName)
                 .location(locationBuilder.build())
                 .hostname(virtualMachineName);

         String host = freshVm.getServerConnection().getUrl().getHost();

         try {
            nodeMetadataBuilder.uri(new URI("https://" + host + ":9443/vsphere-client/vmrc/vmrc.jsp?vm=urn:vmomi:VirtualMachine:" + vmMORId + ":" + freshVm.getSummary().getConfig().getUuid()));
         } catch (URISyntaxException e) {
         }


         Set<String> ipv4Addresses = newHashSet();
         Set<String> ipv6Addresses = newHashSet();

         if (nodeState == Status.RUNNING && !freshVm.getConfig().isTemplate() &&
                 VSpherePredicate.IsToolsStatusEquals(VirtualMachineToolsStatus.toolsOk).apply(freshVm) &&
                 VSpherePredicate.isNicConnected.apply(freshVm)) {
            Predicates2.retry(new Predicate<VirtualMachine>() {
               @Override
               public boolean apply(VirtualMachine vm) {
                  try {
                     return !Strings.isNullOrEmpty(vm.getGuest().getIpAddress());
                  } catch (Exception e) {
                     return false;
                  }
               }
            }, 60 * 1000 * 10, 10 * 1000, TimeUnit.MILLISECONDS).apply(freshVm);
         }


         if (VSpherePredicate.IsToolsStatusIsIn(Lists.newArrayList(VirtualMachineToolsStatus.toolsNotInstalled, VirtualMachineToolsStatus.toolsNotRunning)).apply(freshVm))
            logger.trace("<< No VMware tools installed or not running ( " + virtualMachineName + " )");
         else if (nodeState == Status.RUNNING && not(VSpherePredicate.isTemplatePredicate).apply(freshVm)) {
            int retries = 0;
            while (ipv4Addresses.size() < 1) {
               ipv4Addresses.clear();
               ipv6Addresses.clear();
               GuestNicInfo[] nics = freshVm.getGuest().getNet();
               boolean nicConnected = false;
               if (null != nics) {
                  for (GuestNicInfo nic : nics) {
                     nicConnected = nicConnected || nic.connected;

                     String[] addresses = nic.getIpAddress();
                     if (null != addresses) {
                        for (String address : addresses) {
                           if (logger.isTraceEnabled())
                              logger.trace("<< find IP addresses " + address + " for " + virtualMachineName);
                           if (isInet4Address.apply(address)) {
                              ipv4Addresses.add(address);
                           } else if (isInet6Address.apply(address)) {
                              ipv6Addresses.add(address);
                           }
                        }
                     }
                  }
               }

               if (toPortableNodeStatus.get(freshVm.getRuntime().getPowerState()) != Status.RUNNING) {
                  logger.trace(">> Node is not running. EXIT IP search.");
                  break;
               }

               if (freshVm.getGuest().getToolsVersionStatus2().equals("guestToolsUnmanaged") && nics == null) {
                  String ip = freshVm.getGuest().getIpAddress();
                  if (!Strings.isNullOrEmpty(ip)) {
                     if (isInet4Address.apply(ip)) {
                        ipv4Addresses.add(ip);
                     } else if (isInet6Address.apply(ip)) {
                        ipv6Addresses.add(ip);
                     }
                  }
                  break;
               }

               if (!nicConnected && retries == 5) {
                  logger.trace("<< VM does NOT have any NIC connected.");
                  break;
               }

               if (ipv4Addresses.size() < 1 && null != nics) {
                  //nicConfigurationRecovery(instance, freshVm);
                  logger.warn("<< can't find IPv4 address for vm: " + virtualMachineName);
                  retries++;
                  Thread.sleep(6000);
               }
               if (ipv4Addresses.size() < 1 && retries == 15) {
                  logger.error("<< can't find IPv4 address after " + retries + " retries for vm: " + virtualMachineName);
                  break;
               }
            }
            nodeMetadataBuilder.publicAddresses(filter(ipv4Addresses, not(isPrivateAddress)));
            nodeMetadataBuilder.privateAddresses(filter(ipv4Addresses, isPrivateAddress));
         }

         CustomFieldValue[] customFieldValues = freshVm.getCustomValue();
         if (customFieldValues != null) {
            for (CustomFieldValue customFieldValue : customFieldValues) {
               if (customFieldValue.getKey() == customFields.get().get(VSphereConstants.JCLOUDS_TAGS).getKey()) {
                  nodeMetadataBuilder.tags(COMMA_SPLITTER.split(((CustomFieldStringValue) customFieldValue).getValue()));
               } else if (customFieldValue.getKey() == customFields.get().get(VSphereConstants.JCLOUDS_GROUP).getKey()) {
                  nodeMetadataBuilder.group(((CustomFieldStringValue) customFieldValue).getValue());
               }
            }
         }
         nodeMetadataBuilder.status(nodeState);
         return nodeMetadataBuilder.build();
      } catch (Throwable t) {
         logger.error("Got an exception for virtual machine name : " + virtualMachineName);
         logger.error("The exception is : " + t.toString());
         Throwables.propagate(t);
         return nodeMetadataBuilder.build();
      }
   }

   private void nicConfigurationRecovery(VSphereServiceInstance instance, VirtualMachine freshVm) throws RemoteException, InterruptedException {
      List<VirtualDeviceConfigSpec> updates = Lists.newArrayList();
      String originalKey = "";
      for (VirtualDevice device : freshVm.getConfig().getHardware().getDevice()) {
         if (device instanceof VirtualEthernetCard) {
            VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
            VirtualEthernetCard ethernetCard = (VirtualEthernetCard) device;
            ethernetCard.getConnectable().setConnected(true);
            VirtualDeviceBackingInfo backingInfo = ethernetCard.getBacking();

            logger.trace(">> VirtualDeviceBackingInfo: " + backingInfo.getClass().getName());
            if (backingInfo instanceof VirtualEthernetCardDistributedVirtualPortBackingInfo) {
               ManagedEntity[] virtualPortgroups = new InventoryNavigator(instance.getInstance().getRootFolder()).searchManagedEntities("DistributedVirtualPortgroup");
               VirtualEthernetCardDistributedVirtualPortBackingInfo virtualPortBackingInfo = (VirtualEthernetCardDistributedVirtualPortBackingInfo) backingInfo;
               DistributedVirtualPortgroup virtualPortgroup = null;
               originalKey = virtualPortBackingInfo.getPort().getPortgroupKey();
               for (ManagedEntity entity : virtualPortgroups) {
                  virtualPortgroup = (DistributedVirtualPortgroup) entity;
                  if (virtualPortgroup.getKey() != originalKey) {
                     break;
                  }
               }

               DistributedVirtualSwitchPortConnection port = new DistributedVirtualSwitchPortConnection();
               DistributedVirtualSwitchPortConnection origPort = virtualPortBackingInfo.getPort();
               port.setPortgroupKey(virtualPortgroup.getKey());
               port.setSwitchUuid(origPort.getSwitchUuid());
               virtualPortBackingInfo.setPort(port);
            } else {
               VirtualEthernetCardNetworkBackingInfo networkBackingInfo = (VirtualEthernetCardNetworkBackingInfo) backingInfo;
               originalKey = networkBackingInfo.getDeviceName();
               networkBackingInfo.setDeviceName("VM Network");
            }

            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);
            nicSpec.setDevice(device);

            updates.add(nicSpec);
         }
      }
      VirtualMachineConfigSpec spec = new VirtualMachineConfigSpec();
      spec.setDeviceChange(updates.toArray(new VirtualDeviceConfigSpec[updates.size()]));
      Task task = freshVm.reconfigVM_Task(spec);
      String result = task.waitForTask();
      if (result.equals(Task.SUCCESS)) {
         updates.clear();
         for (VirtualDevice device : freshVm.getConfig().getHardware().getDevice()) {
            if (device instanceof VirtualEthernetCard) {
               VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
               VirtualEthernetCard ethernetCard = (VirtualEthernetCard) device;
               ethernetCard.getConnectable().setConnected(true);
               VirtualDeviceBackingInfo backingInfo = ethernetCard.getBacking();

               if (backingInfo instanceof VirtualEthernetCardDistributedVirtualPortBackingInfo) {
                  VirtualEthernetCardDistributedVirtualPortBackingInfo virtualPortBackingInfo = (VirtualEthernetCardDistributedVirtualPortBackingInfo) backingInfo;
                  DistributedVirtualSwitchPortConnection port = new DistributedVirtualSwitchPortConnection();
                  DistributedVirtualSwitchPortConnection origPort = virtualPortBackingInfo.getPort();
                  port.setPortgroupKey(originalKey);
                  port.setSwitchUuid(origPort.getSwitchUuid());
                  virtualPortBackingInfo.setPort(port);
               } else {
                  VirtualEthernetCardNetworkBackingInfo networkBackingInfo = (VirtualEthernetCardNetworkBackingInfo) backingInfo;
                  networkBackingInfo.setDeviceName(originalKey);
               }

               nicSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);
               nicSpec.setDevice(device);

               updates.add(nicSpec);
            }
         }
         spec = new VirtualMachineConfigSpec();
         spec.setDeviceChange(updates.toArray(new VirtualDeviceConfigSpec[updates.size()]));
         task = freshVm.reconfigVM_Task(spec);
         result = task.waitForTask();

         if (result.equals(Task.SUCCESS)) {
            GuestOperationsManager gom = serviceInstanceSupplier.get().getInstance().getGuestOperationsManager();
            GuestAuthManager gam = gom.getAuthManager(freshVm);
            final NamePasswordAuthentication npa = new NamePasswordAuthentication();
            npa.setUsername("root");
            npa.setPassword(vmInitPassword);
            GuestProgramSpec gps = new GuestProgramSpec();
            gps.programPath = "/bin/sh";
            gps.arguments = "-c \"service network restart\"";
            List<String> env = Lists.newArrayList("PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin", "SHELL=/bin/bash");
            gps.setEnvVariables(env.toArray(new String[env.size()]));
            GuestProcessManager gpm = gom.getProcessManager(freshVm);
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
      }
   }

   Predicate<String> ipAddressTester = new Predicate<String>() {

      @Override
      public boolean apply(String input) {
         return !input.isEmpty();
      }

   };

   private static final Predicate<String> isPrivateAddress = new Predicate<String>() {
      public boolean apply(String in) {
         return InetAddresses2.IsPrivateIPAddress.INSTANCE.apply(in);
      }
   };

   public static final Predicate<String> isInet4Address = new Predicate<String>() {
      @Override
      public boolean apply(String input) {
         try {
            // Note we can do this, as InetAddress is now on the white list
            return InetAddresses.forString(input) instanceof Inet4Address;
         } catch (IllegalArgumentException e) {
            // could be a hostname
            return false;
         }
      }

   };
   public static final Predicate<String> isInet6Address = new Predicate<String>() {
      @Override
      public boolean apply(String input) {
         try {
            // Note we can do this, as InetAddress is now on the white list
            return InetAddresses.forString(input) instanceof Inet6Address;
         } catch (IllegalArgumentException e) {
            // could be a hostname
            return false;
         }
      }

   };
}
