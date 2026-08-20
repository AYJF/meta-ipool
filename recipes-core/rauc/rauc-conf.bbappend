# Ship OUR OWN RAUC verification keyring instead of meta-rockchip's rk-rauc-demo
# ca.cert.pem.
#
# The demo keyring trusts "O = Test Org, CN = Test Org rauc CA Development", whose
# PRIVATE KEY is published in the public meta-rockchip repository and is valid
# until year 9999. Any device trusting it will install a bundle signed by anyone
# who downloads that key -- i.e. remote root. Unacceptable once the updater talks
# to a hawkBit server over the internet.
#
# Installed over the top of whatever the base recipe/other bbappends put there, so
# this does not depend on FILESEXTRAPATHS ordering between layers. The filename on
# device stays ca.cert.pem because rk-rauc-demo's system.conf hard-codes
# [keyring] path=/etc/rauc/ca.cert.pem.
#
# Only the CA CERTIFICATE (public) lives in this git repo. The CA private key and
# the bundle signing key live outside it -- see IPOOL_RAUC_KEY_DIR in
# recipes-core/rauc-bundles/ipool-rauc-bundle.bb.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "file://ipool-ca.cert.pem"

do_install:append() {
    install -d ${D}${sysconfdir}/rauc
    install -m 0644 ${UNPACKDIR}/ipool-ca.cert.pem ${D}${sysconfdir}/rauc/ca.cert.pem
}
