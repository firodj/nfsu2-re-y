cmake_minimum_required(VERSION 3.20)
project(nfsu2-re-hooks C)

# build with generator: -A Win32

set (SOURCE_FILES
    a_main.c
)

set (EXCLUDED_SOURCE_FILES
    a_fields.c
    a_funcs.c
    a_log.c
    a_structs.c
    a_utils.c
    d3d9.c
    dbgw_a_main.c
    a_fields.c
    a_funcs.c
    a_log.c
    a_structs.c
    a_utils.c
    d3d9.c
    dbgw_a_main.c
    debug-custom-dump-markers.c
    debug-custom-dump-smsdata.c
    debug-custom-endoutrun.c
    debug-custom-find-nearest-marker.c
    debug-custom-getkeyname.c
    debug-custom-iterate-carpresets.c
    debug-custom-iterate-races.c
    debug-custom-iterate-shops.c
    debug-custom-iterate-somethingui.c
    debug-custom-markers-debug.c
    debug-custom-printmailboxthings.c
    debug-custom-remove-all-received-engage-markers.c
    debug-custom-send-instruction-sms.c
    debug-custom-uielementvisitor.c
    debug-hook-beforepresent.c
    debug-hook-doui.c
    debug-hook-wm_char.c
    faux-enable-console.c
    focusloss-nopause.c
    fun-car-customize-preset.c
    fun-car-customize-sponsor.c
    fun-destroy-garage-backdrop.c
    fun-use-preset-cars-in-quickrace.c
    generate-fngdata-list.c
    generate-joyname-list.c
    hook-43DB50-hash-cs.c
    hook-440B40-AllocateAndInitPool.c
    hook-4691C0-DebugPrint.c
    hook-505450-hash-ci.c
    hook-50B9C0-hash-ci.c
    hook-511E60-GetLogoForCarModel.c
    hook-52CEF0-GetFNGInfoByName.c
    hook-535FE0-UILabel__setString.c
    hook-555D00-AddFNGToUIObject.c
    hook-55B5C0-FNGObject__BaseNotify.c
    hook-55DC20-SendSomethingToFNG.c
    hook-57CAC0-SomethingWithABinFile.c
    hook-74A6ED-recv.c
    hook-fileapi.c
    hook-realcore-filesystem.c
    ignorefocuslossintextboxandlist.c
    ol-replace-7583E0.c
    ol-replace-759B4A.c
    replace-440BB0-Pool__Extend.c
    replace-440DF0-CreatePool.c
    replace-497760-GetSmsMessageSubjectHeader.c
    replace-4EED10-CarSelectFNGObject__ChangeCategory.c
    replace-50B790-CreateFNGObject.c
    replace-50CD00-IsPointInRect.c
    replace-50CDB0-DidCursorPressInRect.c
    replace-50D510-DebugPrint.c
    replace-5149C0-GetSmsMessageSubject.c
    replace-514B00-GetSmsMessageBody.c
    replace-514CB0-SmsMessageList__IsMessageAlreadyReceived.c
    replace-51D9F0-GetUIElementSize-Maybe-TODO.c
    replace-526C40-GetFNGForDialog.c
    replace-5BF750-UpdateCursorPosition.c
    replace-5BF860-UpdateMouseState.c
    replace-5BF940-HaveCursorActivity.c
    replace-5C8600-MouseData__ctor.c
    replaceloadingscreen.c
    runwindowed.c
    skipinsertdisc2.c
    speedyboot.c
    switch-joyids-in-career.c
    winconsole.c
)

set_property(SOURCE ${EXCLUDED_SOURCE_FILES} PROPERTY VS_SETTINGS "ExcludedFromBuild=true")
add_library(nfsu2-re-hooks SHARED ${SOURCE_FILES})
target_link_libraries(nfsu2-re-hooks user32.lib gdi32.lib comctl32.lib)
set_target_properties(nfsu2-re-hooks PROPERTIES
    SUFFIX ".asi"
    ARCHIVE_OUTPUT_DIRECTORY "E:/Games/NFS Underground 2/scripts/$<0:>"
    LIBRARY_OUTPUT_DIRECTORY "E:/Games/NFS Underground 2/scripts/$<0:>"
    RUNTIME_OUTPUT_DIRECTORY "E:/Games/NFS Underground 2/scripts/$<0:>"
)
