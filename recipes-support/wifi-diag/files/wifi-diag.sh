#!/bin/sh
# iPool WiFi (RTL8723DS) connect diagnostic -> serial ttyS8 (busybox-safe: head -n).
# Loops ~3 min so you can connect via the app; shows each service's State/Error so
# we can see WHY it won't connect (invalid-key = wrong pass, connect-failed = assoc).
sleep 20
exec > /dev/ttyS8 2>&1
echo ""
echo "========================= WIFI-DIAG ========================="
echo "--- power-save params (want 0/0) ---"
echo "rtw_power_mgnt=$(cat /sys/module/8723ds/parameters/rtw_power_mgnt 2>/dev/null) rtw_ips_mode=$(cat /sys/module/8723ds/parameters/rtw_ips_mode 2>/dev/null)"

i=1
while [ $i -le 8 ]; do
  echo ""
  echo "===== pass $i  (connect to your SSID via the app now) ====="
  echo "-- wlan0 addr (want real IP) / carrier --"
  ip -br addr show wlan0 2>/dev/null
  cat /sys/class/net/wlan0/operstate 2>/dev/null
  echo "-- default route --"
  ip route 2>/dev/null | grep default || echo "(no default route)"
  echo "-- ConnMan wifi service states (flag col: R=ready O=online a/c=connecting) --"
  connmanctl services 2>/dev/null | grep "wifi_" | head -n 8
  echo "-- detail of each connecting/ready service (State/Error/IPv4) --"
  # any service whose 3-char flag column contains a letter (connecting or up)
  for id in $(connmanctl services 2>/dev/null | awk 'substr($0,1,3) ~ /[a-zA-Z*]/ {print $NF}' | grep '^wifi_' | head -n 3); do
    echo "  [$id]"
    connmanctl services "$id" 2>/dev/null | grep -iE "State =|Error =|IPv4 =|Method =|Security =|Strength =" | head -n 8
  done
  echo "-- last wifi/8723 dmesg --"
  dmesg | grep -iE "8723|wlan0|deauth|assoc|4-way|handshake|WPA" | tail -n 4
  i=$((i + 1))
  sleep 22
done
echo "======================= WIFI-DIAG END ======================="
