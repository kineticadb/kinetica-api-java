# Variables
%define user     gpudb
%define owner    gpudb
%define prefix   /opt/gpudb/api/java

# Directives
#%define __os_install_post  %{nil}
%define __jar_repack      %{nil}

# RPM Information
Summary:                  Java client libraries for GPUdb.
Name:                     gpudb-api-java
Version:                  TEMPLATE_RPM_VERSION
Release:                  TEMPLATE_RPM_RELEASE
License:                  This product is licensed to use alongside GPUdb.
Group:                    Applications/Databases
Prefix:                   %{prefix}
BuildArch:                noarch
Packager:                 GPUdb <support@gpudb.com>
AutoReqProv:              no
URL:                      http://www.gpudb.com

Source0:                  %{_sourcedir}/files.tgz

%description
Java client libraries for GPUdb.

# ---------------------------------------------------------------------------
%install
mkdir -p $RPM_BUILD_ROOT%{prefix}
pushd $RPM_BUILD_ROOT%{prefix}
tar xzvf %{SOURCE0}
popd

# ---------------------------------------------------------------------------
%files
%defattr(0644,%{owner},%{user},0755)
TEMPLATE_RPM_FILES
