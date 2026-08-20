FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Override the upstream config.conf.example with our own (gateway-token auto-
# registration), plus a boot-time script + systemd drop-in that set a per-board
# controller id. The base recipe's do_install:append installs the example first;
# ours (parsed later) overwrites it.
SRC_URI += "file://config.conf \
            file://hawkbit-set-target-id \
            file://10-target-id.conf \
            file://20-network-online.conf"

# The hawkBit gateway token is a SHARED CREDENTIAL: whoever holds it can register
# devices and download bundles. It therefore must not live in this repo, which is
# pushed to github.com/AYJF/meta-ipool. config.conf ships a @GATEWAY_TOKEN@
# placeholder and the real value is substituted here from a file kept alongside
# the RAUC signing material. Override on another build host in local.conf.
#
# Rotating the token = update this file, rebuild, reflash, and PUT the new value to
#   /rest/v1/system/configs/authentication.gatewaytoken.key
IPOOL_HAWKBIT_TOKEN_FILE ?= "/home/ayjf/Documents/yocto/rockchip/secrets/hawkbit-gateway-token"

python () {
    f = d.getVar('IPOOL_HAWKBIT_TOKEN_FILE')
    if not f or not os.path.exists(f):
        raise bb.parse.SkipRecipe(
            "IPOOL_HAWKBIT_TOKEN_FILE not found: '%s'. Point it at the hawkBit "
            "gateway token (kept outside this repo) in conf/local.conf." % f)
}

# Not in the sstate signature by default -- make the token's CONTENT part of the
# task hash so rotating it actually triggers a rebuild instead of silently
# reusing a cached config.conf with the old token.
do_install[vardeps] += "IPOOL_HAWKBIT_TOKEN_FILE"
do_install[file-checksums] += "${IPOOL_HAWKBIT_TOKEN_FILE}:True"

do_install:append() {
    # 0640 root:rauc-hawkbit -- NOT 0600 root:root.
    # config.conf now contains the gateway token, so it must not be world
    # readable (the base recipe installs it 0644). But the service runs as
    # User=rauc-hawkbit, so root-only means the daemon cannot read its own
    # config and dies with:
    #   Loading config file failed: Permission denied
    #   status=4/NOPERMISSION
    # root still needs write access because hawkbit-set-target-id rewrites
    # target_name as ExecStartPre=+ (i.e. as root). The rauc-hawkbit user/group
    # come from USERADD_PARAM in meta-rauc's rauc-hawkbit-updater.inc.
    install -m 0640 ${UNPACKDIR}/config.conf ${D}${sysconfdir}/${PN}/config.conf
    chown root:rauc-hawkbit ${D}${sysconfdir}/${PN}/config.conf

    # Substitute the real gateway token for the placeholder.
    token="$(cat ${IPOOL_HAWKBIT_TOKEN_FILE} | tr -d '\n\r')"
    if [ -z "$token" ]; then
        bbfatal "IPOOL_HAWKBIT_TOKEN_FILE (${IPOOL_HAWKBIT_TOKEN_FILE}) is empty"
    fi
    sed -i "s|@GATEWAY_TOKEN@|${token}|" ${D}${sysconfdir}/${PN}/config.conf
    if grep -q '@GATEWAY_TOKEN@' ${D}${sysconfdir}/${PN}/config.conf; then
        bbfatal "gateway token substitution failed"
    fi

    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/hawkbit-set-target-id ${D}${bindir}/hawkbit-set-target-id

    install -d ${D}${systemd_system_unitdir}/rauc-hawkbit-updater.service.d
    install -m 0644 ${UNPACKDIR}/10-target-id.conf \
        ${D}${systemd_system_unitdir}/rauc-hawkbit-updater.service.d/10-target-id.conf
    install -m 0644 ${UNPACKDIR}/20-network-online.conf \
        ${D}${systemd_system_unitdir}/rauc-hawkbit-updater.service.d/20-network-online.conf
}

FILES:${PN} += "${systemd_system_unitdir}/rauc-hawkbit-updater.service.d"
