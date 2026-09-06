#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <sstream>
#include <regex>

/**
 * Sorts an array of strings alphabetically using std::sort (C++ performance enhancement).
 */
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_kotonosora_todolist_data_native_TodoNativeHelper_sortStrings(
        JNIEnv *env,
        jobject /* this */,
        jobjectArray arr) {

    jsize len = env->GetArrayLength(arr);
    std::vector<std::string> strings(len);

    for (jsize i = 0; i < len; i++) {
        auto jStr = (jstring) env->GetObjectArrayElement(arr, i);
        const char *chars = env->GetStringUTFChars(jStr, nullptr);
        if (chars != nullptr) {
            strings[i] = chars;
            env->ReleaseStringUTFChars(jStr, chars);
        }
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
 * Returns an empty string if no heading is found.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_kotonosora_todolist_data_native_TodoNativeHelper_parseMdTitle(
        JNIEnv *env,
        jobject /* this */,
        jstring mdContent) {

    const char *content = env->GetStringUTFChars(mdContent, nullptr);
    if (content == nullptr) return env->NewStringUTF("");

    std::string result;
    std::istringstream stream(content);
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

    env->ReleaseStringUTFChars(mdContent, content);
    return env->NewStringUTF(result.c_str());
}

/**
 * Filters a list of todo titles by a prefix (case-insensitive) using C++ performance.
 */
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_kotonosora_todolist_data_native_TodoNativeHelper_filterByPrefix(
        JNIEnv *env,
        jobject /* this */,
        jobjectArray arr,
        jstring jPrefix) {

    const char *prefix = env->GetStringUTFChars(jPrefix, nullptr);
    std::string prefixLower(prefix ? prefix : "");
    std::transform(prefixLower.begin(), prefixLower.end(), prefixLower.begin(), ::tolower);
    if (prefix) env->ReleaseStringUTFChars(jPrefix, prefix);

    jsize len = env->GetArrayLength(arr);
    std::vector<std::string> matched;

    for (jsize i = 0; i < len; i++) {
        auto jStr = (jstring) env->GetObjectArrayElement(arr, i);
        const char *chars = env->GetStringUTFChars(jStr, nullptr);
        if (chars) {
            std::string s(chars);
            std::string sLower = s;
            std::transform(sLower.begin(), sLower.end(), sLower.begin(), ::tolower);
            if (prefixLower.empty() || sLower.find(prefixLower) == 0) {
                matched.push_back(s);
            }
            env->ReleaseStringUTFChars(jStr, chars);
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

    const char *content = env->GetStringUTFChars(mdContent, nullptr);
    std::vector<std::string> links;

    if (content != nullptr) {
        std::string text(content);
        std::regex wikiLinkRegex(R"(\[\[([^\|\]]+)(?:\|([^\]]+))?\]\])");
        auto words_begin = std::sregex_iterator(text.begin(), text.end(), wikiLinkRegex);
        auto words_end = std::sregex_iterator();

        for (std::sregex_iterator i = words_begin; i != words_end; ++i) {
            std::smatch match = *i;
            if (match.size() > 1) {
                std::string target = match[1].str();
                // Trim whitespace
                target.erase(0, target.find_first_not_of(" \t\r\n"));
                target.erase(target.find_last_not_of(" \t\r\n") + 1);
                if (!target.empty()) {
                    links.push_back(target);
                }
            }
        }
        env->ReleaseStringUTFChars(mdContent, content);
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

    const char *content = env->GetStringUTFChars(mdContent, nullptr);
    std::vector<std::string> tags;

    if (content != nullptr) {
        std::string text(content);
        // Regex matches #tag where tag starts with letter or underscore and contains alphanumeric/underscore/dash
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
        env->ReleaseStringUTFChars(mdContent, content);
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

    const char *content = env->GetStringUTFChars(mdContent, nullptr);
    if (content == nullptr) return env->NewStringUTF("");

    std::string text(content);
    std::string title = "";

    // Check for YAML frontmatter title: "title: My Note"
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

    env->ReleaseStringUTFChars(mdContent, content);
    return env->NewStringUTF(title.c_str());
}
