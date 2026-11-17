#ifndef SWIFTS_CORE_H
#define SWIFTS_CORE_H
#ifdef __cplusplus
extern "C" {
#endif
/* Normalizes UTF-8 text for search. Caller frees with swifts_free. */
char* swifts_normalize(const char* utf8_text);
void swifts_free(void* p);
#ifdef __cplusplus
}
#endif
#endif
