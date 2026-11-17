#include "swifts_core.h"
#include <cctype>
#include <cstdlib>
#include <cstring>
#include <string>

extern "C" char* swifts_normalize(const char* utf8_text) {
    if (!utf8_text) return nullptr;
    std::string out;
    bool space = false;
    for (const unsigned char* p = reinterpret_cast<const unsigned char*>(utf8_text); *p; ++p) {
        if (std::isspace(*p)) {
            space = !out.empty();
            continue;
        }
        if (space) { out.push_back(' '); space = false; }
        out.push_back(static_cast<char>(std::tolower(*p)));
    }
    char* result = static_cast<char*>(std::malloc(out.size() + 1));
    if (result) std::memcpy(result, out.c_str(), out.size() + 1);
    return result;
}

extern "C" void swifts_free(void* p) { std::free(p); }
