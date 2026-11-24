SUMMARY = "Datadog IoT Agent for embedded Linux"
DESCRIPTION = "Lightweight flavor of the Datadog Agent optimized for IoT and embedded Linux devices, packaged from official Datadog release binaries."
HOMEPAGE = "https://docs.datadoghq.com/agent/iot/"
SECTION = "console/utils"
LICENSE = "Apache-2.0"

LIC_FILES_CHKSUM = "file://${S}/opt/datadog-agent/LICENSE;md5=05fd462922dde500b68aac4caed4cfa8"

COMPATIBLE_HOST = "(aarch64|arm|x86_64).*-linux"

DD_ARCH:aarch64 = "arm64"
DD_ARCH:arm = "armhf"
DD_ARCH:x86-64 = "amd64"

# Configurable defaults (override via local.conf or a .bbappend)
DD_SITE ?= "datadoghq.com"
DD_SECRETS_BACKEND ?= "file.yaml"
DD_SECRETS_FILE ?= "/etc/datadog-agent/secrets.yaml"
DD_API_KEY_SECRET_ID ?= "datadog_api_key"

SRC_URI = " \
    https://apt.datadoghq.com/pool/d/da/datadog-iot-agent_${PV}-1_${DD_ARCH}.deb;name=${DD_ARCH} \
    file://datadog.yaml \
    file://datadog-agent.service \
    file://datadog-agent.init \
"

SRC_URI[arm64.sha256sum] = "5d855e43c8beb52bfa0f7a26cafcfbb0d883ad81aa22dd1e4714884667af663a"
SRC_URI[armhf.sha256sum] = "63b8902ba437ec9d31a87214182fb8dc8bc40a82084202acb9a05ef3888fb992"
SRC_URI[amd64.sha256sum] = "c42034fa339584892bd111e62ab8906681f0dd0da0bc9ba23e136ce83fd734d1"

inherit bin_package useradd systemd update-rc.d

USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM:${PN} = "--system dd-agent"
USERADD_PARAM:${PN} = "--system --no-create-home --home-dir /opt/datadog-agent --shell /sbin/nologin --gid dd-agent dd-agent"

SYSTEMD_SERVICE:${PN} = "datadog-agent.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

INITSCRIPT_NAME = "datadog-agent"
INITSCRIPT_PARAMS = "defaults 99 10"

RDEPENDS:${PN} += "ca-certificates"

# Prebuilt Go binary package
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INSANE_SKIP:${PN} += "already-stripped ldflags buildpaths file-rdeps"

do_install:append() {
    # Remove upstream Upstart and init/systemd files bundled in the .deb so we control them cleanly
    rm -rf ${D}${sysconfdir}/init \
           ${D}${sysconfdir}/init.d/datadog-agent \
           ${D}${nonarch_base_libdir}/systemd/system/datadog-agent*.service \
           ${D}${systemd_system_unitdir}/datadog-agent*.service

    # Symlink agent CLI into PATH (matches upstream .deb postinst)
    install -d ${D}${bindir}
    ln -sf /opt/datadog-agent/bin/agent/agent ${D}${bindir}/datadog-agent

    # Install secret-free datadog.yaml configured for native file secrets
    install -d -m 0750 -o dd-agent -g dd-agent ${D}${sysconfdir}/datadog-agent
    install -m 0640 -o dd-agent -g dd-agent ${WORKDIR}/datadog.yaml ${D}${sysconfdir}/datadog-agent/datadog.yaml
    sed -i \
        -e "s|@DD_SITE@|${DD_SITE}|g" \
        -e "s|@DD_SECRETS_BACKEND@|${DD_SECRETS_BACKEND}|g" \
        -e "s|@DD_SECRETS_FILE@|${DD_SECRETS_FILE}|g" \
        -e "s|@DD_API_KEY_SECRET_ID@|${DD_API_KEY_SECRET_ID}|g" \
        ${D}${sysconfdir}/datadog-agent/datadog.yaml

    # Record Yocto installation metadata
    cat > ${D}${sysconfdir}/datadog-agent/install_info << 'EOF'
---
install_method:
  tool: yocto
  tool_version: meta-datadog
  installer_version: meta-datadog
EOF

    # Runtime & log directories
    install -d -m 0755 -o dd-agent -g dd-agent ${D}/opt/datadog-agent/run
    install -d -m 0755 -o dd-agent -g dd-agent ${D}${localstatedir}/log/datadog

    # Ensure agent files are owned by dd-agent
    chown -R dd-agent:dd-agent ${D}/opt/datadog-agent ${D}${sysconfdir}/datadog-agent

    # Init manager integration
    if ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'true', 'false', d)}; then
        install -d ${D}${systemd_system_unitdir}
        install -m 0644 ${WORKDIR}/datadog-agent.service ${D}${systemd_system_unitdir}/datadog-agent.service
    fi

    if ${@bb.utils.contains('DISTRO_FEATURES', 'sysvinit', 'true', 'false', d)}; then
        install -d ${D}${sysconfdir}/init.d
        install -m 0755 ${WORKDIR}/datadog-agent.init ${D}${sysconfdir}/init.d/datadog-agent
    fi
}

FILES:${PN} += " \
    /opt/datadog-agent \
    ${bindir}/datadog-agent \
    ${sysconfdir}/datadog-agent \
    ${localstatedir}/log/datadog \
    ${systemd_system_unitdir}/datadog-agent.service \
"

CONFFILES:${PN} += "${sysconfdir}/datadog-agent/datadog.yaml"
