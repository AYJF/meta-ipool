DESCRIPTION = "iPool RAUC OTA bundle -- packages the Boot2Qt image as the A/B rootfs slot"
LICENSE = "CLOSED"

inherit bundle

RAUC_BUNDLE_COMPATIBLE = "${MACHINE}"
RAUC_BUNDLE_VERSION = "v1.0.3"
RAUC_BUNDLE_DESCRIPTION = "iPool Poolnook OTA"
RAUC_BUNDLE_FORMAT = "verity"

RAUC_BUNDLE_SLOTS = "rootfs"
RAUC_SLOT_rootfs = "b2qt-embedded-qt6-image"
RAUC_SLOT_rootfs[fstype] = "ext4"

# --- Signing keys -----------------------------------------------------------
# Signed by OUR OWN CA ("O = QEI Inc, CN = iPool RAUC Root CA"), whose certificate
# is shipped as the device keyring by recipes-core/rauc/rauc-conf.bbappend.
#
# The PRIVATE KEY DELIBERATELY LIVES OUTSIDE THIS GIT REPO. meta-ipool is pushed to
# github.com/AYJF/meta-ipool, and a bundle signing key in a pushed repo is the same
# mistake as the meta-rockchip demo key it replaces. Override IPOOL_RAUC_KEY_DIR in
# build-*/conf/local.conf on any other build host.
#
# Previously: RAUC_KEY_FILE pointed at files/development-1.key.pem, the PUBLIC
# meta-rockchip rk-rauc-demo key. Those files are deleted.
IPOOL_RAUC_KEY_DIR ?= "/home/ayjf/Documents/yocto/rockchip/secrets/rauc-ca"
RAUC_KEY_FILE  = "${IPOOL_RAUC_KEY_DIR}/ipool-signing-1.key.pem"
RAUC_CERT_FILE = "${IPOOL_RAUC_KEY_DIR}/ipool-signing-1.cert.pem"

# Fail at parse time with a readable message rather than deep inside do_bundle if
# the signing material is not present on this host.
python () {
    for v in ('RAUC_KEY_FILE', 'RAUC_CERT_FILE'):
        p = d.getVar(v)
        if not p or not os.path.exists(p):
            raise bb.parse.SkipRecipe(
                "%s not found: '%s'. Point IPOOL_RAUC_KEY_DIR at the iPool RAUC "
                "signing material (kept outside this repo) in conf/local.conf." % (v, p))
}

COMPATIBLE_MACHINE = "sz3568"
