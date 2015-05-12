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
package org.jclouds.vsphere.predicates;

import com.google.common.base.Predicate;
import com.google.common.net.InetAddresses;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.VirtualMachineToolsStatus;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.VirtualMachine;
import org.jclouds.util.Predicates2;

import javax.annotation.Nullable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 */
public class VSpherePredicate {

   public static final Predicate<VirtualMachine> isTemplatePredicate = new Predicate<VirtualMachine>() {
      @Override
      public boolean apply(VirtualMachine virtualMachine) {
         try {
            return virtualMachine.getConfig().isTemplate();
         } catch (Exception e) {
            return true;
         }
      }

      @Override
      public String toString() {
         return "IsTemplatePredicate()";
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

   public static final Predicate<VirtualMachine> isNicConnected = new Predicate<VirtualMachine>() {
      @Override
      public boolean apply(@Nullable VirtualMachine input) {
         if (input == null)
            return false;
         GuestNicInfo[] nics = input.getGuest().getNet();
         boolean nicConnected = false;
         if (null != nics) {
            for (GuestNicInfo nic : nics) {
               nicConnected = nicConnected || nic.connected;
            }
         }
         return nicConnected;
      }
   };

   public static final Predicate<VirtualMachine> WAIT_FOR_NIC(Integer timeout, TimeUnit timeUnit) {
      return new WaitForNic(timeout, timeUnit);
   }

   public static final Predicate<VirtualMachine> WAIT_FOR_VMTOOLS(Integer timeout, TimeUnit timeUnit) {
      return new WaitForVmTools(timeout, timeUnit);
   }

   public static Predicate<ResourcePool> isResourcePoolOf(String hostname) {
      return new IsResourcePoolOf(hostname);
   }

   public static Predicate<VirtualMachine> IsToolsStatusEquals(VirtualMachineToolsStatus status) {
      return new IsToolsStatusEquals(status);
   }

   public static Predicate<VirtualMachine> IsToolsStatusIsIn(List<VirtualMachineToolsStatus> statuses) {
      return new IsToolsStatusIsIn(statuses);
   }

   public static Predicate<VirtualMachine> hasMORid(String morId) {
      return new HasMORid(morId);
   }

   public static Predicate<VirtualMachine> isNodeIdInList(Iterable<String> ids) {
      return new IsNodeIdInList(ids);
   }
}

class WaitForVmTools implements Predicate<VirtualMachine> {
   private final Integer timeout;
   private final TimeUnit timeUnit;

   private final Predicate<VirtualMachine> delegate;

   WaitForVmTools(Integer timeout, TimeUnit timeUnit) {
      this.timeout = timeout;
      this.timeUnit = timeUnit;
      delegate = Predicates2.retry(new Predicate<VirtualMachine>() {
         @Override
         public boolean apply(VirtualMachine vm) {
            try {
               return vm.getGuest().getToolsStatus().equals(VirtualMachineToolsStatus.toolsOk) || vm.getGuest().getToolsStatus().equals(VirtualMachineToolsStatus.toolsOld);
            } catch (Exception e) {
               return false;
            }
         }
      }, timeout, 1000, timeUnit);

   }

   @Override
   public boolean apply(VirtualMachine vm) {
      try {
         return delegate.apply(vm);
      } catch (Exception e) {
         return false;
      }
   }
}

class WaitForNic implements Predicate<VirtualMachine> {
   private final Integer timeout;
   private final TimeUnit timeUnit;

   private final Predicate<VirtualMachine> delegate;

   WaitForNic(Integer timeout, TimeUnit timeUnit) {
      this.timeout = timeout;
      this.timeUnit = timeUnit;
      delegate = Predicates2.retry(new Predicate<VirtualMachine>() {
         @Override
         public boolean apply(VirtualMachine vm) {
            try {
               return vm.getGuest().getNet() != null;
            } catch (Exception e) {
               return false;
            }
         }
      }, timeout, 1000, timeUnit);

   }

   @Override
   public boolean apply(VirtualMachine vm) {
      try {
         return delegate.apply(vm);
      } catch (Exception e) {
         return false;
      }
   }
}

class HasMORid implements Predicate<VirtualMachine> {
   private String morId = null;

   HasMORid(String morId) {
      this.morId = morId;
   }

   @Override
   public boolean apply(VirtualMachine input) {
      return input.getMOR().getVal().equals(morId);
   }

   @Override
   public String toString() {
      return "HasMORid";
   }
}

class IsResourcePoolOf implements Predicate<ResourcePool> {

   private String hostname;

   IsResourcePoolOf(String hostname) {
      this.hostname = hostname;
   }

   @Override
   public boolean apply(ResourcePool input) {
      try {
         for (HostSystem hostSystem : input.getOwner().getHosts()) {
            if (hostSystem.getName().equals(hostname))
               return true;
         }
      } catch (RemoteException e) {
         return false;
      }
      return false;
   }

   @Override
   public String toString() {
      return "IsResourcePoolOf";
   }

}

class IsToolsStatusEquals implements Predicate<VirtualMachine> {

   private VirtualMachineToolsStatus status;

   IsToolsStatusEquals(VirtualMachineToolsStatus status) {
      this.status = status;
   }

   @Override
   public boolean apply(VirtualMachine input) {
      try {
         if (input.getGuest().getToolsStatus().equals(status))
            return true;
      } catch (Exception e) {
         return false;
      }
      return false;
   }

   @Override
   public String toString() {
      return "IsToolsStatusEquals";
   }

}

class IsToolsStatusIsIn implements Predicate<VirtualMachine> {

   private List<VirtualMachineToolsStatus> statuses;

   IsToolsStatusIsIn(List<VirtualMachineToolsStatus> statuses) {
      this.statuses = statuses;
   }

   @Override
   public boolean apply(VirtualMachine input) {
      try {
         if (statuses.contains(input.getGuest().getToolsStatus()))
            return true;
      } catch (Exception e) {
         return false;
      }
      return false;
   }

   @Override
   public String toString() {
      return "IsToolsStatusIsIn";
   }

}

class IsNodeIdInList implements Predicate<VirtualMachine> {

   private Iterable<String> ids;

   IsNodeIdInList(Iterable<String> ids) {
      this.ids = ids;
   }

   @Override
   public boolean apply(VirtualMachine input) {
      try {
         Iterator<String> iterator = ids.iterator();
         while (iterator.hasNext()) {
            String id = iterator.next();
            if (input.getName().equals(id))
               return true;
         }
      } catch (Exception e) {
         return false;
      }
      return false;
   }

   @Override
   public String toString() {
      return "IsNodeIdInList";
   }

}
