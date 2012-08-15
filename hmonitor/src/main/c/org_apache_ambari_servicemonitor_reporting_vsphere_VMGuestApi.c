/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
 
/*
  Header for class vSphere guest application monitoring.
  This header file comes with the vSphere SDK and is not
  included in the OSS repository.
*/
#include <vmGuestAppMonitorLib.h>
 
#include "org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi.h"

/* Implement class org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi */

/*
 * Class:     org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi
 * Method:    enable
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi_enable
  (JNIEnv *jenv, jobject jobj) {
   return VMGuestAppMonitor_Enable();
  }

/*
 * Class:     org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi
 * Method:    disable
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi_disable
  (JNIEnv *jenv, jobject jobj){
    return VMGuestAppMonitor_Disable();
 }

/*
 * Class:     org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi
 * Method:    isEnabled
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi_isEnabled
  (JNIEnv *jenv, jobject jobj) {
    return VMGuestAppMonitor_IsEnabled();
  }

/*
 * Class:     org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi
 * Method:    markActive
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi_markActive
  (JNIEnv *jenv, jobject jobj){
    return VMGuestAppMonitor_MarkActive();
  }


/*
 * Class:     org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi
 * Method:    getAppStatus
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_apache_ambari_servicemonitor_reporting_vsphere_VMGuestApi_getAppStatus
  (JNIEnv *jenv, jobject jobj) {
    char *buf = VMGuestAppMonitor_GetAppStatus();
    jstring jstrBuf = (*jenv)->NewStringUTF(jenv, buf);
    VMGuestAppMonitor_Free(buf);
    return jstrBuf;
  }

