# iPool Pilot: the pool controller does not use these Qt modules. Removing them
# from this packagegroup keeps them out of the image AND, since nothing else in
# the image depends on them, prevents them from being built at all — notably
# qtwebengine, which is a very large Chromium build.
#
# qtwebview is removed alongside qtwebengine: qtwebview has a build-time
# DEPENDS on qtwebengine (qtwebview_git.bb), so leaving it in would rebuild
# Chromium anyway, and qtwebview is non-functional without it. This mirrors
# meta-qt6's own coin/test-no-webengine.inc, which drops both together.
RDEPENDS:${PN}:remove = "qt3d qtsensors qtwebengine qtwebview"
