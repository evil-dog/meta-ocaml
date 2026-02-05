DESCRIPTION = "Objective Caml Compiler"
SECTION = "devel"
LICENSE = "QPL"

SRC_URI = " \
    git://github.com/ocaml/ocaml.git;protocol=https;tag=4.14.2;nobranch=1 \
    file://ocaml-redirect \
    "

SRCREV = "8eb41f72ded84df884c3671734c947f612091f84"

LIC_FILES_CHKSUM = "file://LICENSE;md5=4f72f33f302a53dc329f4d3819fe14f9"

S = "${WORKDIR}/git"

PACKAGES += "${PN}-opt"

inherit native

# we will install OCaml binaries and libraries in work-shared. The sysroot
# will contain symlinks only. This is necessary because OCaml does not handle
# relative paths very well: for example the compiler has a built-in hard-coded
# path to its libraries ('ocamlc -where')

SHARED_D = "${TMPDIR}/work-shared/ocaml/ocaml-${PV}-${PR}/${BUILD_SYS}"

do_configure () {
    cd ${S}
    ./configure  \
                -bindir ${SHARED_D}/usr/bin \
                -libdir ${SHARED_D}/usr/lib \
                -mandir ${SHARED_D}/usr/share/man \
                -docdir ${SHARED_D}/usr/share/doc/ocaml \
                -host ${BUILD_SYS} \
                -verbose
}

do_compile() {
	oe_runmake world
    oe_runmake bootstrap
    oe_runmake opt
# NOTE:
#   we currently intentionally don't build native *.opt tools. Reasoning:
#   the corresponding ocaml-cross *.opt tools are usually not available. When
#   doing opam builds, both -native and -cross tools are in the search path, with
#   -cross tools having priority (as it should be). However, in the absence of
#   *.opt -cross packages, some opam packages will fall back to -native *.opt
#   tools instead. This seems to be mostly true for packages using oasis. 
#
#   oe_runmake opt.opt
}

do_install() {
    # this will install OCaml in the work-shared directory
    oe_runmake install

    # create regular sysroot directory which contains links to actual OCaml
    # binaries in work-shared
    rm -rf ${D}${bindir}
    install -d  ${D}${bindir}

    # ideally we would want direct symlinks to the work-shared binaries, however
    # Yocto does not allow absolute paths. So need to patch the path into a
    # shell script and symlink to the shell script instead.
    sed -i "s|^WORK_SHARED_PATH.*|WORK_SHARED_PATH=${SHARED_D}/usr/bin|" ${UNPACKDIR}/ocaml-redirect
    install -m 755 ${UNPACKDIR}/ocaml-redirect ${D}${bindir}
    cd ${D}${bindir}
    for i in `ls ${SHARED_D}/usr/bin`; do
        # ocamlrun and ocamlyacc are the exception, they are relocatable
        if [ "$i" = "ocamlrun" ] || [ "$i" = "ocamlyacc" ]; then
            cp ${SHARED_D}/usr/bin/$i .
        else
            ln -s ocaml-redirect $i
        fi
    done
}
