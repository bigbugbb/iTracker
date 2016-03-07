#ifndef _JNI_UTILS_H_
#define _JNI_UTILS_H_

#include <stdlib.h>
#include <jni.h>
#include <pthread.h>
#include "QvodPlayerInterface.h"

#ifdef __cplusplus
extern "C"
{
#endif

typedef struct {
	jobject   		object;
    pthread_mutex_t mutex;
} SyncContext;

int jniThrowException(JNIEnv* env, const char* className, const char* msg);
int QPostEventToJava(JNIEnv * env, jobject obj,int what, int arg1, int arg2);
int QThreadPostEventToJava(int what, int arg1, int arg2);
int attach(JNIEnv* env, jobject thiz, jobject surf);
int detach(JNIEnv* env, jobject thiz);
int notifySeekPosition(int64_t llPos);
int notifyIndexPosition(int64_t llPos);
int getDownloadSpeed();
int previewCaptured(PREVIEWINFO* pPreview);

JNIEnv* getJNIEnv();

/*
int getArgs(JNIEnv* env, jobjectArray *args, int *argc, char **argv) {
	int i = 0;
	int _argc = 0;
	char **_argv = NULL;
	if (args == NULL) {
		return 1;
	}
	
	_argc = (*env)->GetArrayLength(env, args);
	_argv = (char **) malloc(sizeof(char *) * _argc);
	for(i=0;i<_argc;i++) {
		jstring str = (jstring)(*env)->GetObjectArrayElement(env, args, i);
		_argv[i] = (char *)(*env)->GetStringUTFChars(env, str, NULL);   
	}
	
	argc = _argc;
	argv = _argv;
	
	return 0;
};
*/

#ifdef __cplusplus
}
#endif

#endif /* _JNI_UTILS_H_ */
