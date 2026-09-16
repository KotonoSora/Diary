#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <sstream>
#include <regex>
#include <unordered_map>
#include <cctype>

/**
 * RAII Guard helper for JNI string UTF chars management.
 * Automatically releases GetStringUTFChars memory on destruction.
 */
class JniStringGuard {
public:
    JniStringGuard(JNIEnv *env, jstring jstr) : env_(env), jstr_(jstr) {
        if (jstr != nullptr) {
            chars_ = env_->GetStringUTFChars(jstr_, nullptr);
        } else {
            chars_ = nullptr;
        }
    }

    ~JniStringGuard() {
        if (chars_ != nullptr && jstr_ != nullptr) {
            env_->ReleaseStringUTFChars(jstr_, chars_);
        }
    }

    const char* c_str() const { return chars_ ? chars_ : ""; }
    bool valid() const { return chars_ != nullptr; }

private:
    JNIEnv *env_;
    jstring jstr_;
    const char *chars_;
};

/**
 * Sorts an array of strings alphabetically using std::sort (C++ performance enhancement).
 */
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_kotonosora_todolist_data_native_MainNativeHelper_sortStrings(
        JNIEnv *env,
        jobject /* this */,
        jobjectArray arr) {

    jsize len = env->GetArrayLength(arr);
    std::vector<std::string> strings(len);

    for (jsize i = 0; i < len; i++) {
        auto jStr = (jstring) env->GetObjectArrayElement(arr, i);
        JniStringGuard guard(env, jStr);
        strings[i] = guard.c_str();
        env->DeleteLocalRef(jStr);
    }

    std::sort(strings.begin(), strings.end());

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(len, stringClass, nullptr);

    for (jsize i = 0; i < len; i++) {
        jstring str = env->NewStringUTF(strings[i].c_str());
        env->SetObjectArrayElement(result, i, str);
        env->DeleteLocalRef(str);
    }

    return result;
}

/**
 * Parses the title from a Markdown string (first "# " heading).
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_kotonosora_todolist_data_native_MainNativeHelper_parseMdTitle(
        JNIEnv *env,
        jobject /* this */,
        jstring mdContent) {

    JniStringGuard guard(env, mdContent);
    if (!guard.valid()) return env->NewStringUTF("");

    std::string result;
    std::istringstream stream(guard.c_str());
    std::string line;

    while (std::getline(stream, line)) {
        if (line.size() >= 2 && line[0] == '#' && line[1] == ' ') {
            result = line.substr(2);
            while (!result.empty() && (result.back() == '\r' || result.back() == ' ')) {
                result.pop_back();
            }
            break;
        }
    }

    return env->NewStringUTF(result.c_str());
}

/**
 * Filters a list of titles by a prefix (case-insensitive) using C++ performance.
 */
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_kotonosora_todolist_data_native_MainNativeHelper_filterByPrefix(
        JNIEnv *env,
        jobject /* this */,
        jobjectArray arr,
        jstring jPrefix) {

    JniStringGuard prefixGuard(env, jPrefix);
    std::string prefixLower(prefixGuard.c_str());
    std::transform(prefixLower.begin(), prefixLower.end(), prefixLower.begin(), ::tolower);

    jsize len = env->GetArrayLength(arr);
    std::vector<std::string> matched;

    for (jsize i = 0; i < len; i++) {
        auto jStr = (jstring) env->GetObjectArrayElement(arr, i);
        JniStringGuard guard(env, jStr);
        std::string s(guard.c_str());
        std::string sLower = s;
        std::transform(sLower.begin(), sLower.end(), sLower.begin(), ::tolower);
        if (prefixLower.empty() || sLower.find(prefixLower) == 0) {
            matched.push_back(s);
        }
        env->DeleteLocalRef(jStr);
    }

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray((jsize)matched.size(), stringClass, nullptr);

    for (jsize i = 0; i < (jsize)matched.size(); i++) {
        jstring str = env->NewStringUTF(matched[i].c_str());
        env->SetObjectArrayElement(result, i, str);
        env->DeleteLocalRef(str);
    }

    return result;
}

// ── MdNativeHelper JNI Implementations ────────────────────────────────────────

/**
 * Extracts WikiLinks [[Target Title]] or [[Target Title|Alias]] using C++ regex.
 */
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_kotonosora_todolist_data_native_MdNativeHelper_extractWikiLinks(
        JNIEnv *env,
        jobject /* this */,
        jstring mdContent) {

    JniStringGuard guard(env, mdContent);
    std::vector<std::string> links;

    if (guard.valid()) {
        std::string text(guard.c_str());
        std::regex wikiLinkRegex(R"(\[\[([^\|\]]+)(?:\|([^\]]+))?\]\])");
        auto words_begin = std::sregex_iterator(text.begin(), text.end(), wikiLinkRegex);
        auto words_end = std::sregex_iterator();

        for (std::sregex_iterator i = words_begin; i != words_end; ++i) {
            std::smatch match = *i;
            if (match.size() > 1) {
                std::string target = match[1].str();
                target.erase(0, target.find_first_not_of(" \t\r\n"));
                target.erase(target.find_last_not_of(" \t\r\n") + 1);
                if (!target.empty()) {
                    links.push_back(target);
                }
            }
        }
    }

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray((jsize)links.size(), stringClass, nullptr);

    for (jsize i = 0; i < (jsize)links.size(); i++) {
        jstring str = env->NewStringUTF(links[i].c_str());
        env->SetObjectArrayElement(result, i, str);
        env->DeleteLocalRef(str);
    }

    return result;
}

/**
 * Extracts tags (#tag_name) from Markdown text using C++ regex.
 */
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_kotonosora_todolist_data_native_MdNativeHelper_extractTags(
        JNIEnv *env,
        jobject /* this */,
        jstring mdContent) {

    JniStringGuard guard(env, mdContent);
    std::vector<std::string> tags;

    if (guard.valid()) {
        std::string text(guard.c_str());
        std::regex tagRegex(R"((?:^|\s)#([a-zA-Z_][a-zA-Z0-9_\-]*))");
        auto words_begin = std::sregex_iterator(text.begin(), text.end(), tagRegex);
        auto words_end = std::sregex_iterator();

        for (std::sregex_iterator i = words_begin; i != words_end; ++i) {
            std::smatch match = *i;
            if (match.size() > 1) {
                std::string tag = "#" + match[1].str();
                if (std::find(tags.begin(), tags.end(), tag) == tags.end()) {
                    tags.push_back(tag);
                }
            }
        }
    }

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray((jsize)tags.size(), stringClass, nullptr);

    for (jsize i = 0; i < (jsize)tags.size(); i++) {
        jstring str = env->NewStringUTF(tags[i].c_str());
        env->SetObjectArrayElement(result, i, str);
        env->DeleteLocalRef(str);
    }

    return result;
}

/**
 * Parses title from YAML frontmatter or first H1 heading using C++.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_kotonosora_todolist_data_native_MdNativeHelper_parseTitle(
        JNIEnv *env,
        jobject /* this */,
        jstring mdContent) {

    JniStringGuard guard(env, mdContent);
    if (!guard.valid()) return env->NewStringUTF("");

    std::string text(guard.c_str());
    std::string title = "";

    std::regex titleRegex(R"(^title:\s*["']?([^"'\n\r]+)["']?)", std::regex_constants::icase);
    std::istringstream stream(text);
    std::string line;
    bool inFrontmatter = false;
    int lineNum = 0;

    while (std::getline(stream, line)) {
        lineNum++;
        if (lineNum == 1 && (line.rfind("---", 0) == 0)) {
            inFrontmatter = true;
            continue;
        }
        if (inFrontmatter) {
            if (line.rfind("---", 0) == 0) {
                inFrontmatter = false;
                continue;
            }
            std::smatch match;
            if (std::regex_search(line, match, titleRegex)) {
                if (match.size() > 1) {
                    title = match[1].str();
                    break;
                }
            }
        } else if (line.size() >= 2 && line[0] == '#' && line[1] == ' ') {
            title = line.substr(2);
            while (!title.empty() && (title.back() == '\r' || title.back() == ' ')) {
                title.pop_back();
            }
            break;
        }
    }

    return env->NewStringUTF(title.c_str());
}

/**
 * High-performance Native C++ Text Stats Analytics (word count, char count, line count).
 * Returns int array: [wordCount, charCount, lineCount, readingTimeMinutes].
 */
extern "C" JNIEXPORT jintArray JNICALL
Java_com_kotonosora_todolist_data_native_MdNativeHelper_calculateTextStatsNative(
        JNIEnv *env,
        jobject /* this */,
        jstring mdContent) {

    JniStringGuard guard(env, mdContent);
    if (!guard.valid()) {
        jintArray emptyResult = env->NewIntArray(4);
        return emptyResult;
    }

    std::string text(guard.c_str());
    int charCount = (int)text.length();
    int wordCount = 0;
    int lineCount = 0;
    bool inWord = false;

    for (char c : text) {
        if (c == '\n') {
            lineCount++;
        }
        if (std::isspace(static_cast<unsigned char>(c))) {
            if (inWord) {
                wordCount++;
                inWord = false;
            }
        } else {
            inWord = true;
        }
    }
    if (inWord) wordCount++;
    if (!text.empty()) lineCount++;

    int readingTimeMinutes = std::max(1, wordCount / 200);

    jintArray result = env->NewIntArray(4);
    jint stats[4] = {wordCount, charCount, lineCount, readingTimeMinutes};
    env->SetIntArrayRegion(result, 0, 4, stats);

    return result;
}
