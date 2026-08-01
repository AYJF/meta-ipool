FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Route U-Boot proper's console to the board's UART3 header (SoC uart8/ttyS8)
# at 115200, matching the kernel console so one FTDI on UART3 shows
# U-Boot proper -> kernel. Applied to the sz3568 machine only.
SRC_URI:append:sz3568 = " file://sz3568-uboot-console.dtsi"

do_configure:prepend:sz3568() {
    # 1) Console baud 1.5M -> 115200 (must match stdout-path; also keeps the
    #    'baudrate' env var consistent so U-Boot doesn't re-switch the console).
    sed -i 's/^CONFIG_BAUDRATE=.*/CONFIG_BAUDRATE=115200/' \
        ${S}/configs/evb-rk3568_defconfig

    # 2) Append our uart8 console overlay to the board's -u-boot.dtsi (U-Boot
    #    auto-includes *-u-boot.dtsi after the main DT, so our chosen/stdout-path
    #    override wins). Guarded so re-runs don't duplicate.
    if ! grep -q 'sz3568-console' \
        ${S}/arch/arm/dts/rk3568-evb1-v10-u-boot.dtsi; then
        cat ${UNPACKDIR}/sz3568-uboot-console.dtsi \
            >> ${S}/arch/arm/dts/rk3568-evb1-v10-u-boot.dtsi
    fi
}
