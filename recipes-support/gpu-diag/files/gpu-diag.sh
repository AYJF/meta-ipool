#!/bin/sh
# iPool GPU bring-up diagnostic: dump the GPU/EGL state to the serial console
# (ttyS8 TX works even though RX/ethernet don't), so we can see WHY Qt fails to
# render without an interactive shell. Runs ~25s after boot to catch the launcher.
sleep 25
{
  echo ""
  echo "========================= GPU-DIAG ========================="
  echo "--- kernel GPU/Mali probe (dmesg) ---"
  dmesg | grep -iE "mali|panfrost|lima|gpu|drm" | tail -25
  echo "--- device nodes ---"
  ls -l /dev/mali0 /dev/dri/* 2>&1
  echo "--- which libEGL/GLESv2/libgbm does ld.so resolve? ---"
  ldconfig -p 2>/dev/null | grep -iE "libEGL\.so|libGLESv2\.so|libgbm\.so|libmali"
  echo "--- libEGL the app actually resolves (with LD_LIBRARY_PATH) ---"
  APP=/usr/bin/appPoolnook; [ -e "$APP" ] || APP=/usr/bin/qtlauncher
  echo "app: $APP"
  LD_LIBRARY_PATH=/usr/lib/mali ldd "$APP" 2>/dev/null | grep -iE "libEGL|libGLESv2|libgbm|libmali"
  echo "--- systemd DefaultEnvironment (LD_LIBRARY_PATH) ---"
  cat /etc/systemd/system.conf.d/00-libmali.conf 2>&1
  echo "--- /usr/lib/mali contents + shadow conf ---"
  ls -l /usr/lib/mali/ 2>&1 | head
  cat /etc/ld.so.conf.d/00-aarch64-mali.conf 2>&1
  echo "--- libmali linkage (what the blob needs) ---"
  ldd /usr/lib/mali/libmali.so.1 2>&1 | grep -iE "not found|=>" | head
  echo "--- eglinfo (gbm) ---"
  if command -v eglinfo >/dev/null 2>&1; then
    EGL_PLATFORM=gbm eglinfo 2>&1 | head -25
  else
    echo "eglinfo not installed"
  fi
  echo "--- b2qt / demolauncher unit status ---"
  systemctl status b2qt demolauncher appcontroller --no-pager 2>&1 | head -50
  echo "--- journal for the launcher (last 50) ---"
  journalctl -u b2qt -u demolauncher -u appcontroller --no-pager -n 50 2>&1
  echo "======================= GPU-DIAG END ======================="
} > /dev/ttyS8 2>&1
