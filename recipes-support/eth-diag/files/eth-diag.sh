#!/bin/sh
# iPool ethernet result check -> serial ttyS8 (busybox-safe).
# After the mac_clk_refout RGMII patch: is eth0 up and is refout now enabled?
sleep 22
mount -t debugfs none /sys/kernel/debug 2>/dev/null
exec > /dev/ttyS8 2>&1

echo ""
echo "========================= ETH-DIAG4 ========================="
echo "--- gmac dmesg (want: NO 'Failed to reset the dma', want 'Link is Up') ---"
dmesg | grep -iE "gmac|stmmac|dwmac|Failed to reset|Link is|mdio" | tail -n 16
echo "--- ip link ---"
ip -br link
echo "--- MAC out clocks (want clk_mac1_out enable_cnt>=1 @ 125MHz) ---"
grep -iE "clk_mac1_out|clk_mac1_refout|clk_mac1_2top|gmac1_clkin|clk_gmac1 " /sys/kernel/debug/clk/clk_summary
echo "--- PHY id (want a real value, NOT 0xffff....) ---"
cat /sys/class/mdio_bus/stmmac-1/stmmac-1:00/phy_id 2>/dev/null
echo "--- try DHCP (only if link is up) ---"
ip link set eth0 up 2>&1
sleep 5
udhcpc -i eth0 -n -q -t 3 2>&1 | tail -n 5
ip -br addr show eth0
echo "======================= ETH-DIAG4 END ======================="
