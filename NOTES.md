Matrix Identity
===============


.text:0043C780 sub_43C780      proc near               ; CODE XREF: sub_40F0D0+11↑p
.text:0043C780                                         ; sub_431990+28↑p ...
.text:0043C780
.text:0043C780 arg_0           = dword ptr  4
.text:0043C780
.text:0043C780                 mov     eax, [esp+arg_0]
.text:0043C784                 xor     ecx, ecx
.text:0043C786                 mov     edx, 3F800000h
.text:0043C78B                 mov     [eax], edx
.text:0043C78D                 mov     [eax+4], ecx
.text:0043C790                 mov     [eax+8], ecx
.text:0043C793                 mov     [eax+0Ch], ecx
.text:0043C796                 mov     [eax+10h], ecx
.text:0043C799                 mov     [eax+14h], edx
.text:0043C79C                 mov     [eax+18h], ecx
.text:0043C79F                 mov     [eax+1Ch], ecx
.text:0043C7A2                 mov     [eax+20h], ecx
.text:0043C7A5                 mov     [eax+24h], ecx
.text:0043C7A8                 mov     [eax+28h], edx
.text:0043C7AB                 mov     [eax+2Ch], ecx
.text:0043C7AE                 mov     [eax+30h], ecx
.text:0043C7B1                 mov     [eax+34h], ecx
.text:0043C7B4                 mov     [eax+38h], ecx
.text:0043C7B7                 mov     [eax+3Ch], edx
.text:0043C7BA                 retn
.text:0043C7BA sub_43C780      endp


Another Resources
=================
https://github.com/SpeedReflect


nfsu2-re-gamefile-explorer
==========================

Just open the folder with IntelliJ and run U2gfeMain, alternatively add Jar Archive.
