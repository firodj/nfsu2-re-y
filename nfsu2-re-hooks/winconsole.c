static
void winconsole()
{
	AllocConsole();
	freopen("CONOUT$", "w+", stdout);
	printf("FERAPPEE\n");

	INIT_FUNC();
#undef INIT_FUNC
#define INIT_FUNC winconsole
}
