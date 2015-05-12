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
package org.jclouds.vsphere.compute.options;

import com.google.common.base.Objects;
import org.jclouds.compute.options.TemplateOptions;

import java.util.Map;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

/**
 * Contains options supported in the {@code ComputeService#runNode} operation on
 * the "vSphere" provider. <h2>
 * Usage</h2> The recommended way to instantiate a VSphereTemplateOptions object
 * is to statically import VSphereTemplateOptions.* and invoke a static creation
 * method followed by an instance mutator (if needed):
 * <p/>
 * <code>
 * import static org.jclouds.compute.options.VSphereTemplateOptions.Builder.*;
 * <p/>
 * ComputeService client = // get connection
 * templateBuilder.options(inboundPorts(22, 80, 8080, 443));
 * Set<NodeMetadata> set = client.createNodesInGroup(tag, 2, templateBuilder.build());
 * </code>
 * <p/>
 */
public class VSphereTemplateOptions extends TemplateOptions implements Cloneable {
   @Override
   public VSphereTemplateOptions clone() {
      VSphereTemplateOptions options = new VSphereTemplateOptions();
      copyTo(options);
      return options;
   }

   @Override
   public void copyTo(TemplateOptions to) {
      super.copyTo(to);
      if (to instanceof VSphereTemplateOptions) {
         VSphereTemplateOptions eTo = VSphereTemplateOptions.class.cast(to);
         if (getCustomizationScript() != null)
            eTo.customizationScript(getCustomizationScript());
         if (getDescription() != null)
            eTo.description(getDescription());
         if (getAddressType() != null)
            eTo.addressType(getAddressType());
         if (isoFileName() != null)
            eTo.isoFileName(isoFileName());
         if (flpFileName() != null)
            eTo.flpFileName(flpFileName());
         if (datacenterName() != null)
            eTo.datacenterName(datacenterName());
         if (waitOnPort() != null)
            eTo.waitOnPort(waitOnPort());
         if (vmFolder() != null)
            eTo.vmFolder(vmFolder());
         eTo.postConfiguration(postConfiguration());
         eTo.distributedVirtualSwitch(distributedVirtualSwitch());
      }
   }

   public VSphereTemplateOptions vmFolder(String vmFolder) {
      this.folder = vmFolder;
      return this;
   }

   public VSphereTemplateOptions distributedVirtualSwitch(boolean distributedVirtualSwitch) {
      this.distributedVirtualSwitch = distributedVirtualSwitch;
      return this;
   }

   private String description = null;
   private String customizationScript = null;
   private String addressType = null;
   private String datacenterName = null;
   private String isoFileName = null;
   private String flpFileName = null;
   private boolean postConfiguration = true;
   private boolean distributedVirtualSwitch = false;
   private Integer waitOnPort = null;
   private String folder = null;

   public Integer waitOnPort() {
      return waitOnPort;
   }

   public VSphereTemplateOptions waitOnPort(Integer waitOnPort) {
      this.waitOnPort = waitOnPort;
      return this;
   }

   public boolean postConfiguration() {
      return postConfiguration;

   }

   public VSphereTemplateOptions postConfiguration(boolean postConfiguration) {
      this.postConfiguration = postConfiguration;
      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      VSphereTemplateOptions that = VSphereTemplateOptions.class.cast(o);
      return super.equals(that) && equal(this.description, that.description)
              && equal(this.customizationScript, that.customizationScript)
              && equal(this.addressType, that.addressType)
              && equal(this.datacenterName, that.datacenterName);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(super.hashCode(), description, customizationScript,
              addressType, datacenterName);
   }

   public Objects.ToStringHelper string() {
      return super.string().add("description", description).add("customizationScript", customizationScript)
              .add("addressType", addressType).add("datacenterName", datacenterName);
   }

   /**
    * Optional description. Used for the Description of the vApp created by this
    * instantiation.
    */
   public VSphereTemplateOptions description(String description) {
      this.description = description;
      return this;
   }

   /**
    * Specifies the customizationScript used to run instances with
    */
   public VSphereTemplateOptions customizationScript(String customizationScript) {
      this.customizationScript = checkNotNull(emptyToNull(customizationScript), "customizationScript must be defined");
      return this;
   }


   /**
    * Specifies the parentNetwork to connect the the network interfaces on the
    * VMs to.
    */
   public VSphereTemplateOptions addressType(String addressType) {
      this.addressType = addressType;
      return this;
   }

   public VSphereTemplateOptions datacenterName(String datacenterName) {
      this.datacenterName = datacenterName;
      return this;
   }

   public boolean distributedVirtualSwitch() {
      return this.distributedVirtualSwitch;
   }

   public String vmFolder() {
      return folder;
   }


   public static class Builder {
      /**
       * @see VSphereTemplateOptions#description
       */
      public static VSphereTemplateOptions description(String description) {
         return new VSphereTemplateOptions().description(description);
      }

      /**
       * @see VSphereTemplateOptions#customizationScript
       */
      public static VSphereTemplateOptions customizationScript(String customizationScript) {
         return new VSphereTemplateOptions().customizationScript(customizationScript);
      }

      /**
       * @see VSphereTemplateOptions#addressType(String addressType)
       */
      public static VSphereTemplateOptions addressType(String addressType) {
         return new VSphereTemplateOptions().addressType(addressType);
      }

      /**
       * @see VSphereTemplateOptions#datacenterName(String datacenterName)
       */
      public static VSphereTemplateOptions datacenterName(String datacenterName) {
         return new VSphereTemplateOptions().datacenterName(datacenterName);
      }

      // methods that only facilitate returning the correct object type

      /**
       * @see TemplateOptions#inboundPorts
       */
      public static VSphereTemplateOptions inboundPorts(int... ports) {
         VSphereTemplateOptions options = new VSphereTemplateOptions();
         return VSphereTemplateOptions.class.cast(options.inboundPorts(ports));
      }

      /**
       * @see TemplateOptions#port
       */
      public static VSphereTemplateOptions blockOnPort(int port, int seconds) {
         VSphereTemplateOptions options = new VSphereTemplateOptions();
         return VSphereTemplateOptions.class.cast(options.blockOnPort(port, seconds));
      }

      /**
       * @see TemplateOptions#userMetadata(Map)
       */
      public static VSphereTemplateOptions userMetadata(Map<String, String> userMetadata) {
         VSphereTemplateOptions options = new VSphereTemplateOptions();
         return VSphereTemplateOptions.class.cast(options.userMetadata(userMetadata));
      }

      /**
       * @see TemplateOptions#userMetadata(String, String)
       */
      public static VSphereTemplateOptions userMetadata(String key, String value) {
         VSphereTemplateOptions options = new VSphereTemplateOptions();
         return VSphereTemplateOptions.class.cast(options.userMetadata(key, value));
      }

      /**
       * @see TemplateOptions#nodeNames(Iterable)
       */
      public static VSphereTemplateOptions nodeNames(Iterable<String> nodeNames) {
         VSphereTemplateOptions options = new VSphereTemplateOptions();
         return VSphereTemplateOptions.class.cast(options.nodeNames(nodeNames));
      }

      /**
       * @see TemplateOptions#networks(Iterable)
       */
      public static VSphereTemplateOptions networks(Iterable<String> networks) {
         VSphereTemplateOptions options = new VSphereTemplateOptions();
         return VSphereTemplateOptions.class.cast(options.networks(networks));
      }

      public static VSphereTemplateOptions distributedVirtualSwitch(boolean distributedVirtualSwitch) {
         return new VSphereTemplateOptions().distributedVirtualSwitch(distributedVirtualSwitch);
      }

      public static VSphereTemplateOptions vmFolder(String vmFolder) {
         return new VSphereTemplateOptions().vmFolder(vmFolder);
      }

   }

   /**
    * @return description of the vApp
    */
   public String getDescription() {
      return description;
   }

   /**
    * @return customizationScript on the vms
    */
   public String getCustomizationScript() {
      return customizationScript;
   }

   /**
    * @return addressType
    */
   public String getAddressType() {
      return addressType;
   }

   /**
    * @return datacenterName
    */
   public String datacenterName() {
      return datacenterName;
   }


   // methods that only facilitate returning the correct object type

   /**
    * @see TemplateOptions#blockOnPort
    */
   @Override
   public VSphereTemplateOptions blockOnPort(int port, int seconds) {
      return VSphereTemplateOptions.class.cast(super.blockOnPort(port, seconds));
   }

   /**
    * special thing is that we do assume if you are passing groups that you have
    * everything you need already defined. for example, our option inboundPorts
    * normally creates ingress rules accordingly but if we notice you've
    * specified securityGroups, we do not mess with rules at all
    *
    * @see TemplateOptions#inboundPorts
    */
   @Override
   public VSphereTemplateOptions inboundPorts(int... ports) {
      return VSphereTemplateOptions.class.cast(super.inboundPorts(ports));
   }

   /**
    * @see TemplateOptions#authorizePublicKey(String)
    */
   @Override
   public VSphereTemplateOptions authorizePublicKey(String publicKey) {
      return VSphereTemplateOptions.class.cast(super.authorizePublicKey(publicKey));
   }

   /**
    * @see TemplateOptions#installPrivateKey(String)
    */
   @Override
   public VSphereTemplateOptions installPrivateKey(String privateKey) {
      return VSphereTemplateOptions.class.cast(super.installPrivateKey(privateKey));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public VSphereTemplateOptions userMetadata(Map<String, String> userMetadata) {
      return VSphereTemplateOptions.class.cast(super.userMetadata(userMetadata));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public VSphereTemplateOptions userMetadata(String key, String value) {
      return VSphereTemplateOptions.class.cast(super.userMetadata(key, value));
   }

   public String isoFileName() {
      return isoFileName;
   }

   public VSphereTemplateOptions isoFileName(String isoFileName) {
      this.isoFileName = isoFileName;
      return this;
   }

   public String flpFileName() {
      return flpFileName;
   }

   public VSphereTemplateOptions flpFileName(String flpFileName) {
      this.flpFileName = flpFileName;
      return this;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public VSphereTemplateOptions nodeNames(Iterable<String> nodeNames) {
      return VSphereTemplateOptions.class.cast(super.nodeNames(nodeNames));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public VSphereTemplateOptions networks(Iterable<String> networks) {
      return VSphereTemplateOptions.class.cast(super.networks(networks));
   }

}
