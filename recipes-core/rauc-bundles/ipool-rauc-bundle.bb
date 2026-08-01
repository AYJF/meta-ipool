DESCRIPTION = "iPool RAUC OTA bundle -- packages the Boot2Qt image as the A/B rootfs slot"
LICENSE = "CLOSED"

inherit bundle

RAUC_BUNDLE_COMPATIBLE = "${MACHINE}"
RAUC_BUNDLE_VERSION = "v1.0.0"
RAUC_BUNDLE_DESCRIPTION = "iPool Poolnook OTA"
RAUC_BUNDLE_FORMAT = "verity"

RAUC_BUNDLE_SLOTS = "rootfs"
RAUC_SLOT_rootfs = "b2qt-embedded-qt6-image"
RAUC_SLOT_rootfs[fstype] = "ext4"

# SPIKE ONLY: reuse the meta-rockchip rk-rauc-demo development keys so the bundle
# is signed by the same CA the device trusts (ca.cert.pem, installed on-device by
# the demo). BEFORE PRODUCTION: generate your own CA + signing keypair and point
# these at them (and ship your CA in the device keyring).
RAUC_KEY_FILE = "${THISDIR}/files/development-1.key.pem"
RAUC_CERT_FILE = "${THISDIR}/files/development-1.cert.pem"

COMPATIBLE_MACHINE = "sz3568"
