cmake_minimum_required(VERSION 3.20)
project(nfsu2-re-ext C)

set (nfsu2-xtended-input_source_files
    dllmain.cpp
)

add_library(nfsu2-xtended-input SHARED NFSU-XtendedInput/dllmain.cpp)
target_link_libraries(nfsu2-xtended-input winmm.lib)
# Win7: XINPUT_OLD
target_compile_definitions(nfsu2-xtended-input PRIVATE GAME_UG2)

set_target_properties(nfsu2-xtended-input PROPERTIES
    SUFFIX ".asi"
    OUTPUT_NAME "NFSU_XtendedInput"
    ARCHIVE_OUTPUT_DIRECTORY "E:/Games/NFS Underground 2/scripts/$<0:>"
    LIBRARY_OUTPUT_DIRECTORY "E:/Games/NFS Underground 2/scripts/$<0:>"
    RUNTIME_OUTPUT_DIRECTORY "E:/Games/NFS Underground 2/scripts/$<0:>"
)