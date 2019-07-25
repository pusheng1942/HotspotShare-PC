//
// Created by gp on 7/23/19.
//
#include <jni.h>
#include <cstring>
#include <cstdio>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_hotspotshare_SsidAndPreshareKeyToJNI_transmitSsidAndPreshareKeyToJNI(JNIEnv *env,
                                                                                        jobject instance,
                                                                                        jstring prompt){
    const char *str;
    str = env->GetStringUTFChars(prompt, JNI_FALSE);  //flase --> JNI_FALSE
    env->ReleaseStringUTFChars(prompt,str);

    char result[80];
    strcpy(result,"Hello");
    strcat(result,str);
    puts(result);

    jstring rtstr = env->NewStringUTF(result);
    return rtstr;
}
