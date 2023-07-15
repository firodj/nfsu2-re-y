static
void mkjmp(int at, void *to)
{
	/*at -= 0x400000; at += base;*/
	*((unsigned char*) at) = 0xE9;
	at++;
	*((int*) at) = (int) to - (at + 4);
}

static
void redirectjmp(int at, void *to)
{
	/*at -= 0x400000; at += base;*/
	at++;
	*((int*) at) = (int) to - (at + 4);
}

static
void nop(int at, int num)
{
	/*at -= 0x400000; at += base;*/
	while (num--) {
		*((unsigned char*) at++) = 0x90;
	}
}

static
int isprocstaticmem(int loc)
{
	return base < loc && loc < base + 0x4BB000;
}

static
__declspec(naked)
void*
__stdcall
ThisCallNoArgs(int addr, void *this)
{
	_asm { pop eax } // ret addr
	_asm { pop ecx } // target addr
	_asm { pop ecx } // this
	_asm { push eax } // ret addr
	_asm { jmp dword ptr [esp-4] }
}

static
__declspec(naked)
void*
__stdcall
ThisCallOneArg(int addr, void *this, void *arg0)
{
	_asm { jmp ThisCallNoArgs }
}

static
__declspec(naked)
void*
__stdcall
ThisCallThreeArgs(int addr, void *this, void *arg0, void *arg1, void *arg2)
{
	_asm { jmp ThisCallNoArgs }
}

static
__declspec(naked) int nfsu2_stricmp(char *a, char *b)
{
	_asm {
		mov eax, 0x43DCC0
		jmp eax
	}
}

static
__declspec(naked) char *nfsu2_GetLanguageString(int hash)
{
	_asm {
		mov eax, 0x4FFA80
		jmp eax
	}
}

unsigned int hatoi(char *text)
{
	unsigned int col;
	int j, c;

	col = 0;
	for (j = 0; text[j] && j < 8; j++) {
		c = text[j] - '0';
		if (c < 0 || 9 < c) {
			c = text[j] - 'A' + 10;
			if (c < 10 || 15 < c) {
				c = text[j] - 'a' + 10;
				if (c < 10 || 15 < c) {
					return 0;
				}
			}
		}
		col <<= 4;
		col |= c;
	}
	return col;
}

static
char *languagelabel(unsigned int key)
{
	static char *data = 0;
	static struct {
		int wcharTableOffset;
		int numStrings;
		int tableOffset;
		int stringsOffset;
	} *language_header;
	static char *strings;
	static struct {
		unsigned int hash;
		int stringOffset;
	} *table;

	FILE *in;
	struct {
		int magic;
		int size;
	} section_header;
	int offset;
	int i;

	if (!data) {
		if (!(in = fopen("../LANGUAGES/Labels.bin", "rb"))) {
			return "<failed to open labels file>";
		}

		offset = 0;
		while (fread(&section_header, 1, sizeof(section_header), in)) {
			offset += sizeof(section_header);
			if (section_header.magic == 0x39000) {
				data = malloc(section_header.size);
				fread(data, section_header.size, 1, in);
				language_header = (void*) data;
				strings = data + language_header->stringsOffset;
				table = (void*) (data + language_header->tableOffset);
				break;
			}
			offset += section_header.size;
			fseek(in, section_header.size, SEEK_CUR);
		}
		fclose(in);
	}

	for (i = 0; i < language_header->numStrings; i++) {
		if (table[i].hash == key) {
			return strings + table[i].stringOffset;
		}
	}
	return "<not found>";
}

static
char *animationname(unsigned int key)
{
	static struct {
		unsigned int hash;
		char *name;
	} animations[] = {
		{ 0x001744B3, "Init" },
		{ 0x5B0D9106, "FadeIn" },
		{ 0xBCBFCC87, "FadeOut" },
		{ 0xBCC00F05, "Fade_In" }, // with squish, like playername/money swap effect
		{ 0x54C20A66, "Fade_Out" }, // with squish, like playername/money swap effect
		{ 0x0016A259, "HIDE" },
		{ 0x001CA7C0, "SHOW" },
		{ 0x249DB7B7, "HIGHLIGHT" },
		{ 0x7AB5521A, "UnHighlight" },
		{ 0x0013C37B, "CALL" },
		{ 0xDE6EFF34, "FORWARD" },
		{ 0x03826A28, "Pulse" },
		{ 0x7AB70D67, "STATIC" },
		{ 0x6EBBFB68, "Normal" },
		{ 0x4F90CF9B, "Active" },
		{ 0x03D8EABC, "UNDIM" },
		{ 0x00009E99, "DIM" },
		{ 0x925D53E4, "Dramatic" },
		{ 0x5A8E4EBE, "animate" }, // like in animate dialog progress bar
		{ 0x375D11BF, "animation" }, // untested
		{ 0x8AB83EDB, "ZoomIn" }, // untested
		{ 0xA19BB14C, "ZoomInGreen" }, // untested
		{ 0x5230FAF6, "ZoomInRed" }, // untested
		// questionable ones:
		{ 0xA95DB9D5, "Pause_Menu_Main" },
		{ 0x2B5A03A8, "Pause_Options" },
		{ 0xFCC020D2, "Event_Results" },
	};
	static int num = sizeof(animations)/sizeof(animations[0]);

	int i;
	for (i = 0; i < num; i++) {
		if (animations[i].hash == key) {
			return animations[i].name;
		}
	}
	return "<unknown>";
}

static
void print_ui_element_and_children(struct UIElement *element, char *prefix, char printsiblings)
{
	struct UIElement *containerchild;
	struct UILabel *label;
	char locbuf[100];
	char locbuf2[100];

	sprintf(locbuf, "%s    ", prefix);
	if (element) do {
		locbuf2[0] = 0;
		if (element->type == 2) {
			label = (void*) element;
			if ((element->someFlags & USE_CUSTOM_TEXT) && label->string.ptrString) {
				locbuf2[0] = '+';
				locbuf2[1] = ' ';
				wchar2char(locbuf2 + 2, label->string.ptrString);
			} else {
				sprintf(locbuf2, "- %s", GetLanguageString(label->textLanguageString));
			}
		}
		//element->someFlags |= 0x2; // set visible
		//element->someFlags &= ~1; // set visible
		log(buf, sprintf(buf, "%selement %p type %d hash %08X flags %8X:%s",
			prefix, element, element->type, element->hash, element->someFlags, locbuf2));
		if (element->type == 5) {
			containerchild = ((struct UIContainer*) element)->children;
			print_ui_element_and_children(containerchild, locbuf, 1);
		}
		element = element->nextSibling;
	} while (element && printsiblings);
}
