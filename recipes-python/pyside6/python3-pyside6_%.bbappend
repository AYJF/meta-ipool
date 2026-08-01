# iPool Pilot: don't build PySide6 bindings for modules we've dropped. pyside6
# build-depends on every module in PYSIDE_QT_MODULES, so without this it would
# pull qt3d and qtsensors back into the build even though we removed them from
# the image packagegroup. shiboken only generates bindings for modules present
# in the sysroot, so removing them from DEPENDS is sufficient.
PYSIDE_QT_MODULES:remove = "qt3d qtsensors"
