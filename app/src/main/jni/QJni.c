#include <assert.h>
#include "QJniMisc.h"
#include "QvodPlayerCallbacks.h"

#define QDebug 0
#if QDebug
#include <android/log.h>
#  define  D(x...)  __android_log_print(ANDROID_LOG_INFO, "tracker_player", x)
#else
#  define  D(...)  do {} while (0)
#endif

#define CLASS_NAME  "com/localytics/android/itracker/player/TrackerNativeMediaPlayer"

SyncContext g_sc;

static PREVIEWINFO* s_pPreview = NULL; // global, but used only in local area

static jint create(JNIEnv *env, jobject thiz)
{
	CreatePlayer("/sdcard");
	
	SetCallback(CALLBACK_CREATE_AUDIO_SERVICE, CreateAudioService, NULL, NULL);
	SetCallback(CALLBACK_CREATE_VIDEO_SERVICE, CreateVideoService, NULL, NULL);
	SetCallback(CALLBACK_RESET_AUDIO_SERVICE, ResetAudioService, NULL, NULL);
	SetCallback(CALLBACK_OPEN_FINISHED, OpenFinished, NULL, NULL);
	SetCallback(CALLBACK_CLOSE_FINISHED, CloseFinished, NULL, NULL);
	SetCallback(CALLBACK_PREVIEW_STARTED, PreviewStarted, NULL, NULL);
	SetCallback(CALLBACK_PREVIEW_STOPPED, PreviewStopped, NULL, NULL);
	SetCallback(CALLBACK_PREVIEW_CAPTURED, PreviewCaptured, NULL, NULL);
	SetCallback(CALLBACK_UPDATE_PICTURE_SIZE, UpdatePictureSize, NULL, NULL);
	SetCallback(CALLBACK_DELIVER_FRAME, DeliverFrame, NULL, NULL);
	SetCallback(CALLBACK_FRAME_CAPTURED, FrameCaptured, NULL, NULL);
	SetCallback(CALLBACK_PLAYBACK_FINISHED, PlaybackFinished, NULL, NULL);
	SetCallback(CALLBACK_ERROR, ErrorHandler, NULL, NULL);
	SetCallback(CALLBACK_BEGIN_BUFFERING, BeginBuffering, NULL, NULL);
	SetCallback(CALLBACK_ON_BUFFERING, OnBuffering, NULL, NULL);
	SetCallback(CALLBACK_END_BUFFERING, EndBuffering, NULL, NULL);
	SetCallback(CALLBACK_BEGIN_SUBTITLE, BeginSubtitle, NULL, NULL);
	SetCallback(CALLBACK_END_SUBTITLE, EndSubtitle, NULL, NULL);
	SetCallback(CALLBACK_SEEK_POSITION, NotifySeekPosition, NULL, NULL);
	SetCallback(CALLBACK_READ_INDEX, NotifyReadIndex, NULL, NULL);
	SetCallback(CALLBACK_CHECK_DEVICE, CheckDevice, NULL, NULL);
	SetCallback(CALLBACK_GET_DOWNLOAD_SPEED, GetDownloadSpeed, NULL, NULL);

	return 0;
}

static jint open(JNIEnv *env, jobject thiz, jstring file, jdouble offset, jint remote)
{
	D("open");
    int result;
    const char *f;
    jboolean copy;

    f = (*env)->GetStringUTFChars(env, file, &copy);
    result = Open(f, offset, remote);
    (*env)->ReleaseStringUTFChars(env, file, f);

    return result;
}

static jint close(JNIEnv *env, jobject thiz)
{
    D("close");
    return Close();
}

static jint play(JNIEnv *env, jobject thiz)
{
	D("play");
	return Play();
}

static jint pause(JNIEnv *env, jobject thiz)
{
	D("pause");	
    return Pause();
}

static jint seek(JNIEnv *env, jobject thiz, jdouble time)
{
	D("seek %f", time);
	return Seek(time);
}

static jint startpreview(JNIEnv *env, jobject thiz, jstring file, jdouble offset, jint framecount)
{
    int result;
    const char *f;
    jboolean copy;

    f = (*env)->GetStringUTFChars(env, file, &copy);
    result = StartPreview(f, offset, 1);
    (*env)->ReleaseStringUTFChars(env, file, f);

    return result;
}

static jint stoppreview(JNIEnv *env, jobject thiz)
{
	D("stoppreview");
    int result = StopPreview();

    return result;
}

static jint getMediaDuration(JNIEnv *env, jobject thiz)
{
	D("getMediaDuration");
    float fDuration = 0;

    GetParameter(PLAYER_GET_MEDIA_DURATION, &fDuration);

	return (int)fDuration;
}

static jint getCurrentTime(JNIEnv *env, jobject thiz)
{
	D("getCurrentTime");
    float fCurTime = 0;

    GetParameter(PLAYER_GET_MEDIA_CURRENT_TIME, &fCurTime);
    D("getCurrentTime: %f", fCurTime);

	return (int)fCurTime;
}

static jint getVideoWidth(JNIEnv *env, jobject thiz)
{
	D("getVideoWidth");
	int nWidth = 0;

	GetParameter(PLAYER_GET_VIDEO_WIDTH, &nWidth);

	return nWidth;
}

static jint getVideoHeight(JNIEnv *env, jobject thiz)
{
	D("getVideoHeight");
	int nHeight = 0;

	GetParameter(PLAYER_GET_VIDEO_HEIGHT, &nHeight);

	return nHeight;
}

static jboolean isPreviewing(JNIEnv *env, jobject thiz)
{
	D("isPreviewing");
	int nState = STATE_NONE;

	GetParameter(PLAYER_GET_PREVIEW_STATE, &nState);

	return !(nState & STATE_UNLOADED);
}

static jboolean isOpened(JNIEnv *env, jobject thiz)
{
	D("isLoaded");
    int nState = STATE_NONE;

    GetParameter(PLAYER_GET_STATE, &nState);

	return nState & STATE_LOADED;
}

static jboolean isPlaying(JNIEnv *env, jobject thiz)
{
	D("isPlaying");
    int nState = STATE_NONE;

    GetParameter(PLAYER_GET_STATE, &nState);

	return nState & STATE_EXECUTE;
}

static jboolean isPaused(JNIEnv *env, jobject thiz)
{
	D("isPaused");
    int nState = STATE_NONE;

    GetParameter(PLAYER_GET_STATE, &nState);

	return nState & STATE_PAUSE;
}

static jboolean isClosed(JNIEnv *env, jobject thiz)
{
	D("isClosed");
    int nState = STATE_NONE;

    GetParameter(PLAYER_GET_STATE, &nState);

	return nState & STATE_UNLOADED;
}

static jint player_attach(JNIEnv *env, jobject thiz, jobject obj)
{
    return attach(env, thiz, obj);
}

static jint player_detach(JNIEnv *env, jobject thiz)
{
    return detach(env, thiz);
}

static void native_setup(JNIEnv *env, jobject thiz)
{
	D("native_setup");
	// initialize the callback information:
	jclass clazz = (*env)->GetObjectClass(env, thiz);
	
	if (clazz == NULL) {
		D("Can't find player_native_setup when setting up callback.");
	} else {
		if (g_sc.object == NULL) {
			pthread_mutex_init(&g_sc.mutex, 0);
			g_sc.object = (*env)->NewGlobalRef(env, thiz);
			D("native_setup ok");
		}
	}
}

static void native_release(JNIEnv *env, jobject thiz)
{
	D("native_release");
    // do everything a call to finalize would
    
    if (g_sc.object != NULL) {
	    (*env)->DeleteGlobalRef(env, g_sc.object);
	    g_sc.object = NULL;
	    pthread_mutex_destroy(&g_sc.mutex);
    }
    D("native_release ok");
}

static JavaVM *jvm;
static jmethodID method_postEvent = NULL;
static jmethodID method_notifySeekPos = NULL;
static jmethodID method_notifyIndexPos = NULL;
static jmethodID method_getDownloadSpeed = NULL;
static jmethodID method_fillPreviewField = NULL;

static void classInitNative(JNIEnv* env, jclass clazz)
{
	D("classInitNative");
	if ((*env)->GetJavaVM(env, &jvm) < 0) {
		D("classInitNative failed");		
    }

    method_postEvent = (*env)->GetMethodID(env, clazz, "postEventFromNative", "(III)V");
//    method_fillPreviewField = (*env)->GetMethodID(env, clazz, "fillPreviewField", "(IIIIIIDLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
//    if (!method_postEvent || !method_notifySeekPos || !method_notifyIndexPos || !method_getDownloadSpeed || !method_fillPreviewField) {
//    	D("classInitNative failed 2");
//    	assert(method_postEvent && method_notifySeekPos && method_notifyIndexPos && method_getDownloadSpeed && method_fillPreviewField);
//    }
	if (!method_postEvent) {
		D("classInitNative failed 2");
		assert(method_postEvent);
	}
}

static jint getSamplingRate(JNIEnv *env, jobject thiz)
{
	D("getSamplingRate");
	float fSampleRate = 0;

	GetParameter(PLAYER_GET_AUDIO_SAMPLE_RATE, &fSampleRate);
	D("fSampleRate = %f", fSampleRate);
	int nSampleRate = (int)fSampleRate;
	D("nSampleRate = %d", nSampleRate);

	return nSampleRate;
}

static jint getChannelCount(JNIEnv *env, jobject thiz)
{
	D("getChannelCount");
	int nChannels = 0;

	GetParameter(PLAYER_GET_AUDIO_CHANNEL_COUNT, &nChannels);

	return nChannels;
}

static jint getBytePerSample(JNIEnv *env, jobject thiz)
{
	D("getChannelCount");
	int nSampleFormat = 0;
	int nBytePerSample = 2;

	GetParameter(PLAYER_GET_AUDIO_SAMPLE_FORMAT, &nSampleFormat);
	if (nSampleFormat == 0/*AV_SAMPLE_FMT_U8*/) {
		nBytePerSample = 1;
	} else if (nSampleFormat == 1/*AV_SAMPLE_FMT_S16*/) {
		nBytePerSample = 2;
	}

	return nBytePerSample;
}

static int getAudioData(JNIEnv *env, jobject thiz, jbyteArray byteData, jint len)
{
	char buffer[192000];
	unsigned int uLength = len;
	SendRequest(REQUEST_OUTPUT_AUDIO, 0, 0, buffer, &uLength);
	(*env)->SetByteArrayRegion(env, byteData, 0, uLength, buffer);

	return uLength;
}

static int getPreviewData(JNIEnv *env, jobject thiz, jbyteArray byteData, jint len)
{
	if (s_pPreview) {
		D("copy preview content from C to Java array, %d", len);
		(*env)->SetByteArrayRegion(env, byteData, 0, len, s_pPreview->fi.pContent);
		return len;
	}

	return 0;
}

const int METHODSCOUNT = 27;
static JNINativeMethod methods[] = {
  {"playercreate", "()I", (void*)create },
  {"playeropen", "(Ljava/lang/String;DI)I", (void*)open },
  {"playerclose", "()I", (void*)close },
  {"playerplay", "()I", (void*)play },
  {"playerpause", "()I", (void*)pause },
  {"playerseek", "(D)I", (void*)seek },
  {"startpreview", "(Ljava/lang/String;DI)I", (void*)startpreview },
  {"stoppreview", "()I", (void*)stoppreview },
  {"playergetDuration", "()I", (void*)getMediaDuration },
  {"playergetCurrentTime", "()I", (void*)getCurrentTime},
  {"playergetVideoWidth", "()I", (void*)getVideoWidth },
  {"playergetVideoHeight", "()I", (void*)getVideoHeight },
  {"playerIsPreviewing", "()Z", (void*)isPreviewing },
  {"playerIsOpened", "()Z", (void*)isOpened },
  {"playerIsPlaying", "()Z", (void*)isPlaying },
  {"playerIsPaused", "()Z", (void*)isPaused },
  {"playerIsClosed", "()Z", (void*)isClosed },
  {"playerattach", "(Landroid/view/Surface;)I", (void*)player_attach },
  {"playerdetach", "()I", (void*)player_detach },
  {"playernative_setup", "()V", (void*)native_setup },
  {"playernative_release", "()V", (void*)native_release },
  {"classInitNative", "()V", (void*)classInitNative },
  {"getSamplingRate", "()I",(void*)getSamplingRate},
  {"getChannelCount", "()I", (void*)getChannelCount},
  {"getBytePerSample", "()I", (void*)getBytePerSample},
  {"getNativeAudioData", "([BI)I",(void*)getAudioData},
  {"getNativePreviewData", "([BI)I",(void*)getPreviewData},
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv *env;
	int result = -1;

	if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_4) != JNI_OK) {
		D("JNI_OnLoad result %d ", result);
		return result;
	}
	jclass clazz = (*env)->FindClass(env, CLASS_NAME);
	if (clazz == NULL) {
		D("Can't find the class");
	    return result;
	}
	D("OnLoad ok ");
	(*env)->RegisterNatives(env, clazz, methods, METHODSCOUNT);

	return JNI_VERSION_1_4;
}

int QPostEventToJava(JNIEnv * env, jobject obj,int what, int arg1, int arg2)
{
	D("QPostEventToJava");
	int err = 0;

	if (method_postEvent != NULL) {
	    (*env)->CallVoidMethod(env, obj, method_postEvent, what, arg1, arg2);
	} else {
		D("Can't find the method_postEvent");
		err = 1;
	}

	return err;
}

int QThreadPostEventToJava(int what, int arg1, int arg2)
{
	D("QThreadPostEventToJava");
	JNIEnv* env;
	int err = 0, nDetached = 0;

    pthread_mutex_lock(&g_sc.mutex);
	jint rc = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6);
	// check if we should attach
	if (rc != JNI_OK) {
		nDetached = 1;
		rc = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
		if (rc < 0) {
			err = -1;
			nDetached = 0;
			D("QPostEventToJava,can't find jvm");
			goto done;
		}
	}

	(*env)->CallVoidMethod(env, g_sc.object, method_postEvent, what, arg1, arg2);
done:
	if (nDetached) {
		(*jvm)->DetachCurrentThread(jvm);
	}
	pthread_mutex_unlock(&g_sc.mutex);

	return err;
}

int notifySeekPosition(int64_t llPos)
{
	D("notifySeekPosition");
	int err = 0;
	JNIEnv* env;

    pthread_mutex_lock(&g_sc.mutex);
	err = (*jvm)->AttachCurrentThread(jvm, &env, 0);
	if (err < 0) {
		err = -1;
		D("notifySeekPosition,can't find jvm");
		goto done;
	}
//	(*env)->CallIntMethod(env, g_sc.object, method_notifySeekPos, llPos);
done:
	(*jvm)->DetachCurrentThread(jvm);
	pthread_mutex_unlock(&g_sc.mutex);

	return err;
}

int notifyIndexPosition(int64_t llPos)
{
	D("notifyIndexPosition");
	int err = 0;
	JNIEnv* env;

    pthread_mutex_lock(&g_sc.mutex);
	err = (*jvm)->AttachCurrentThread(jvm, &env, 0);
	if (err < 0) {
		err = -1;
		D("notifyIndexPosition,can't find jvm");
		goto done;
	}
//	(*env)->CallIntMethod(env, g_sc.object, method_notifyIndexPos, llPos);
done:
	(*jvm)->DetachCurrentThread(jvm);
	pthread_mutex_unlock(&g_sc.mutex);

	return err;
}

int getDownloadSpeed()
{
	D("getDownloadSpeed");
	JNIEnv* env;
	int nSpeed = 0;

    pthread_mutex_lock(&g_sc.mutex);
	if ((*jvm)->AttachCurrentThread(jvm, &env, 0) < 0) {
		D("getDownloadSpeed,can't find jvm");
		goto done;
	}
//	nSpeed = (*env)->CallIntMethod(env, g_sc.object, method_getDownloadSpeed);
done:
	(*jvm)->DetachCurrentThread(jvm);
	pthread_mutex_unlock(&g_sc.mutex);

	return nSpeed;
}

int previewCaptured(PREVIEWINFO* pPreview)
{
	D("previewCaptured(PREVIEWINFO* pPreview)");
	JNIEnv* env;

    pthread_mutex_lock(&g_sc.mutex);
	if ((*jvm)->AttachCurrentThread(jvm, &env, 0) < 0) {
		D("previewCaptured,can't find jvm");
		goto done;
	}

	s_pPreview = pPreview;
	jstring strArtist = (*env)->NewStringUTF(env, pPreview->mi.szArtist);
	jstring strTitle  = (*env)->NewStringUTF(env, pPreview->mi.szTitle);
	jstring strAlbum  = (*env)->NewStringUTF(env, pPreview->mi.szAlbum);

	(*env)->CallVoidMethod(env, g_sc.object, method_fillPreviewField,
		pPreview->fi.nWidth, pPreview->fi.nHeight, pPreview->fi.nStride, pPreview->fi.nFormat,
		pPreview->nBitRate, pPreview->mi.nSampleRate, (double)pPreview->fDuration, strArtist, strTitle, strAlbum);
	(*env)->CallVoidMethod(env, g_sc.object, method_postEvent, ON_PREVIEW_CAPTURED, 0, 0);
	s_pPreview = NULL;
	D("previewCaptured(PREVIEWINFO* pPreview) successful");
done:
	D("previewCaptured(PREVIEWINFO* pPreview) done");
	(*jvm)->DetachCurrentThread(jvm);
	pthread_mutex_unlock(&g_sc.mutex);

	return 0;
}
