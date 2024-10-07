static FILE *logfile;
static char buf[4096], buf2[4096];

static char newline = 10;

static
void logging(char *msg, int len)
{
#ifdef LOG_TO_LOGFILE
	fwrite(msg, len, 1, logfile);
	if (len && msg[len - 1] != '\n') {
		fwrite(&newline, 1, 1, logfile);
	}
	fflush(logfile);
#endif
#ifdef LOG_TO_DEBUGSTRING
	OutputDebugString(msg);
#endif
#ifdef LOG_TO_PRINTF
	printf(len && msg[len - 1] != '\n' ? "%s\n" : "%s", msg);
#endif
}

static
void hash_func_logging(char *arg, int result, int address)
{
	if (arg != NULL) {
		logging(buf, sprintf(buf, "hash\t%s\t0x%x\t%X", arg, result, address));
	}
}

static
void logvalue(char *lineprefix, char *varname, int value)
{
	float floatval;
	int derefval;

	floatval = *((float*) &value);
	logging(buf, sprintf(buf, "%s   value '%s'", lineprefix, varname));
	logging(buf, sprintf(buf, "%s     char %d 0x%X short 0x%d %X int %d 0x%X float %.1f",
		lineprefix, value & 0xFF, value & 0xFF, value & 0xFFFF, value & 0xFFFF,
		value, value, floatval));
	if (!IsBadReadPtr((void*) value, 4)) {
		logging(buf, sprintf(buf, "%s      is valid pointer to:", lineprefix));
		derefval = *((int*) value);
		floatval = *((float*) &derefval);
		logging(buf, sprintf(buf, "%s        char %d 0x%X short %d 0x%X int %d 0x%X float %.1f",
			lineprefix, value & 0xFF, value & 0xFF, value & 0xFFFF, value & 0xFFFF,
			value, value, floatval));
	}
	if (!IsBadStringPtrA((char*) value, sizeof(buf) - 10)) {
		logging(buf, sprintf(buf, "%s      strptr: 0x%x", lineprefix, value));
	}
}