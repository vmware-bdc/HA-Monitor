#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#  
# 
# #this is an RPM spec
# see : http://docs.fedoraproject.org/en-US/Fedora_Draft_Documentation/0.1/html/RPM_Guide/ch-creating-rpms.html


# scripts are executed
# -Run %pre of new package
# -Install new files
# -%post of new package
# -%preun of old package
# -Delete any old files not overwritten by newer ones
# -%postun of old package
# what that means is that your %post code runs before the old version is
# uninstalled, and before its %postun runs. So you are in trouble if you need
# to run anything after the old version has uninstalled. 



# if menu entries are created, define Summary here, and use it in the summary
# tag, and the menu entries' descriptions

%define section         free

%define approot         %{_datadir}/monitor
%define basedir         ${rpm.install.dir}
%define bindir          %{basedir}
%define libdir          %{basedir}/lib


#some shortcuts
%define hmonitor.jar hmonitor-${hmonitor.version}.jar


# -----------------------------------------------------------------------------

Summary:        Hadoop Monitoring Framework
Name:           hmonitor
Version:        ${hmonitor.version}
Release:        ${rpm.release.version}%{?dist}
Group:          Applications/System
License:        Apache
URL:            http://hadoop.apache.org/
Vendor:         ${rpm.vendor}
Packager:       ${rpm.packager}

BuildArch:      x86_64
Source0:        %{name}-%{version}.tar.gz
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root
Prefix:         ${rpm.prefix}
Provides:       hmonitor

# build and runtime requirements here
Requires(rpmlib): rpmlib(CompressedFileNames) <= 3.0.4-1 rpmlib(PayloadFilesHavePrefix) <= 4.0-1

%description
HMonitor monitors Apache Hadoop master services and their health,
 
 
# -----------------------------------------------------------------------------

%package vsphere-monitoring
Group:         ${rpm.framework}
Summary: monitor daemon for VMWare vSphere
Requires:       %{name} = %{version}-%{release}

%description vsphere-monitoring

Provides the native binaries necessary for vsphere integration
 

# -----------------------------------------------------------------------------

%package vsphere-namenode-daemon
Group:         ${rpm.framework}
Summary: monitor daemon for VMWare vSphere
Requires:       %{name} = %{version}-%{release}, %{name}-vsphere-monitoring

%description vsphere-namenode-daemon

Integrates NameNode/HDFS monitoring with VMWare vSphere by adding 
an init.d daemon to report health to vSphere. 

# -----------------------------------------------------------------------------

%package vsphere-jobtracker-daemon
Group:         ${rpm.framework}
Summary: monitor daemon for VMWare vSphere
Requires:       %{name} = %{version}-%{release}, %{name}-vsphere-monitoring

%description vsphere-jobtracker-daemon

Integrates JobTracker monitoring with VMWare vSphere by adding 
an init.d daemon to report health to vSphere. 


# -----------------------------------------------------------------------------

%package resource-agent
Group:         ${rpm.framework}
Summary: Linux HA clustering support
Requires:       %{name} = %{version}-%{release}
#For RHEL6+, the resource-agents RPM should be a declared dependency, 
#it is not in RHEL5.8, hence not referenced as of July 31, 2012 
#Requires:       %{name} = %{version}-%{release}, resource-agents

%description resource-agent

Integrates availability monitoring with Linux Clustering. 


# -----------------------------------------------------------------------------

%package test
Group:         ${rpm.framework}
Summary: monitor test files
Requires:       %{name} = %{version}-%{release}

%description test

Extra configuration files and scripts for testing -this is not needed
or intended for redistribution except for people testing the monitor 
itself.

# -----------------------------------------------------------------------------

%prep
%setup -q -c

# -----------------------------------------------------------------------------

%build
rm -rf $RPM_BUILD_ROOT
pwd
cp -PpR . $RPM_BUILD_ROOT


# -----------------------------------------------------------------------------

%clean
rm -rf $RPM_BUILD_ROOT

# -----------------------------------------------------------------------------

%files
%defattr(0644,${rpm.username},${rpm.groupname},0755)


%dir %{basedir}

%{basedir}/hmonitor.jar
%{basedir}/lib
%{basedir}/core

#Configuration files
%config(noreplace) %{basedir}/log4j.properties



#scripts

%attr(755, root,root) %{basedir}/ham.sh

#extra binaries
%dir %{basedir}/extras
%{basedir}/extras/groovy-all-${groovy.version}.jar





# =============================================================================
# vSphere specific artifacts
# =============================================================================

%files vsphere-monitoring


# JNI library and symlink for the hadoop command to pick up
%{basedir}/libVMGuestAppMonitorNative.so
/usr/lib/hadoop/lib/native/Linux-amd64-64/libVMGuestAppMonitorNative.so

#the shared library
/usr/lib64/libappmonitorlib.so


#Configuration files

%config(noreplace) %{basedir}/template.xml

#scripts

%attr(755, root,root) %{basedir}/exec-monitor.sh


# =============================================================================
# namenode-daemon Installation
# =============================================================================

%files vsphere-namenode-daemon

#Configuration files

%config(noreplace) %{basedir}/vm-namenode.xml

#scripts

%attr(755, root,root) %{basedir}/vsphere-ha-namenode-monitor.sh

#init.d daemon
%attr(755, root,root) /etc/init.d/${rpm.daemon.name}

# -----------------------------------------------------------------------------
# post-installation actions  - the daemon is set up to autorun
# -----------------------------------------------------------------------------

%post vsphere-namenode-daemon
if [ -x /sbin/chkconfig ]; then
  /sbin/chkconfig --add ${rpm.daemon.name}
fi



# -----------------------------------------------------------------------------
# Uninstall Logic
# -----------------------------------------------------------------------------

%preun vsphere-namenode-daemon

# shut down the daemon before the uninstallation
/etc/init.d/${rpm.daemon.name} stop

if  [ -x /sbin/chkconfig ]; then
    /sbin/chkconfig --del ${rpm.daemon.name} || echo "trouble shutting down the NN daemon"
fi

# =============================================================================
# vsphere-jobtracker-daemon Installation
# =============================================================================

%files vsphere-jobtracker-daemon

#Configuration files

%config(noreplace) %{basedir}/vm-jobtracker.xml

#scripts

%attr(755, root,root) %{basedir}/vsphere-ha-jobtracker-monitor.sh

#init.d daemon
%attr(755, root,root) /etc/init.d/${rpm.jtdaemon.name}

# -----------------------------------------------------------------------------
# post-installation actions  - the daemon is set up to autorun
# -----------------------------------------------------------------------------

%post vsphere-jobtracker-daemon
if [ -x /sbin/chkconfig ]; then
  /sbin/chkconfig --add ${rpm.jtdaemon.name}
fi


# -----------------------------------------------------------------------------
# Uninstall Logic
# -----------------------------------------------------------------------------

%preun vsphere-jobtracker-daemon

# shut down the daemon before the uninstallation
/etc/init.d/${rpm.jtdaemon.name} stop

if  [ -x /sbin/chkconfig ]; then
    /sbin/chkconfig --del ${rpm.jtdaemon.name} || echo "trouble shutting down the JT daemon"
fi

# =============================================================================
# Package: resource-agent
# =============================================================================

%files resource-agent

#Documentation

#Configuration files


#scripts
%attr(755, root,root) %{basedir}/haprobe.sh

%attr(755, root,root) /etc/cluster/cluster.conf.template.xml
%attr(755, root,root) /usr/share/cluster/hadoop.sh

# the layout below is defined in the OCF specs
# http://www.linux-ha.org/doc/dev-guides/_packaging_resource_agents.html
#%defattr(755,root,root,-)
#%dir /usr/lib/ocf/resource.d/hadoop/
#/usr/lib/ocf/resource.d/hadoop/hadoop.sh

# =============================================================================
# Package: test
# =============================================================================
%files test

#Documentation
%{basedir}/commands.txt

#Configuration files
%config(noreplace) %{basedir}/bulk-job-submission.xml

#scripts
%attr(755, root,root) %{basedir}/bulkjobs.sh
%attr(755, root,root) %{basedir}/exec-ls.sh
%attr(755, root,root) %{basedir}/logmonitor.sh
%attr(755, root,root) %{basedir}/lsblocking.sh
%attr(755, root,root) %{basedir}/lsdir.sh
%attr(755, root,root) %{basedir}/suicide.sh
%attr(755, root,root) %{basedir}/haprobe-old.sh




# -----------------------------------------------------------------------------

# to get the date, run:   date +"%a %b %d %Y"
%changelog
* Wed Jun 20 2012 Steve Loughran <stevel@apache.org> 0.1-2.el11
- converged redhat and vsphere RPMs with specific modules and a
  shared core RPM
- add jobtracker monitoring RPM
* Wed Jun 13 2012 Steve Loughran <stevel@apache.org> 0.1-2.el10
- split test scripts out into own package
* Mon Jun 11 2012 Steve Loughran <stevel@apache.org> 0.1-2.el9
- added new namenode-daemon RPM to start and stop the namenode -then reverted it
* Tue May 29 2012 Steve Loughran <stevel@apache.org> 0.1-2.el5
- New bulkjobs.sh script
* Fri May 11 2012 Steve Loughran <stevel@apache.org> 0.1-1.el5
- Initial release
