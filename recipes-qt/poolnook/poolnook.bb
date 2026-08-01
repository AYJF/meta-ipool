SUMMARY = "Poolnook pool-controller Qt6 application (iPool SZ3568)"
DESCRIPTION = "The iPool 'Poolnook' pool-controller UI -- a Qt6/QML application \
(target appPoolnook). Built straight from the local checkout of the private \
poolnook-firmware repo (branch multiport-ivy, commit c1f14f1) via externalsrc. \
Installed as the board's single fullscreen app: /usr/bin/b2qt -> appPoolnook, so \
b2qt.service launches it on eglfs (hardware GPU via libmali) at boot."
LICENSE = "CLOSED"

# Build from the local private checkout (no network fetch). Slimmed copy at:
#   /yocto_tutorial/rockchip/local_repos/poolnook-firmware  (.git + NDI removed)
inherit qt6-cmake externalsrc
EXTERNALSRC = "/yocto_tutorial/rockchip/local_repos/poolnook-firmware"
EXTERNALSRC_BUILD = "${WORKDIR}/build"

# Qt6 modules the app find_package()s + the native QML tooling (qmlimportscanner,
# qmlcachegen, shader tools) needed to build the qt_add_qml_module.
DEPENDS += " \
    qtbase \
    qtdeclarative qtdeclarative-native \
    qtserialport \
    qtserialbus \
    qtvirtualkeyboard \
    qtmultimedia \
    qtcharts \
    qthttpserver \
    qtshadertools qtshadertools-native \
"

# Runtime QML modules the app imports (Qt Quick + Controls + VK + Charts + MM +
# Shapes/Layouts, etc.) plus the C++ modules. Pull the whole b2qt Qt module set to
# be safe -- missing a QML import shows as a blank/again-crash at runtime.
RDEPENDS:${PN} += " \
    qtbase qtdeclarative \
    qtvirtualkeyboard qtcharts qtmultimedia qtserialport qtserialbus qthttpserver \
    qtwayland \
"

# b2qt.service runs '/usr/bin/appcontroller /usr/bin/b2qt' on eglfs when /usr/bin/b2qt
# exists. Symlink it to our app so the board boots straight into Poolnook fullscreen.
do_install:append() {
    if [ -e ${D}${bindir}/appPoolnook ]; then
        ln -sf appPoolnook ${D}${bindir}/b2qt
    fi
}

FILES:${PN} += "${bindir}/appPoolnook ${bindir}/b2qt"

COMPATIBLE_MACHINE = "sz3568"
