#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <sstream>

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
            // Trim trailing whitespace/CR
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
