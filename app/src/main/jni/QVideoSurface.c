#include <pthread.h>
#include <dlfcn.h>
#include <jni.h>
#include "QVideo.h"
#include "QvodPlayerInterface.h"
//#include "QPlayer.h"


#define QVOD_DEBUG 0
#define QDebug 0

#if QDebug
#include <android/log.h>
#  define  D(x...)  __android_log_print(ANDROID_LOG_INFO,"qplayer",x)
#else
#  define  D(...)
#endif

#ifndef ANDROID_SYM_S_LOCK_FALLBACK
#define ANDROID_SYM_S_LOCK_FALLBACK "_ZN7android7Surface4lockEPNS0_11SurfaceInfoEb"
#endif
#ifndef ANDROID_SYM_S_LOCK
#define ANDROID_SYM_S_LOCK "_ZN7android7Surface4lockEPNS0_11SurfaceInfoEPNS_6RegionE"
#endif
#ifndef ANDROID_SYM_S_UNLOCK
# define ANDROID_SYM_S_UNLOCK "_ZN7android7Surface13unlockAndPostEv"
#endif

// _ZN7android7Surface4lockEPNS0_11SurfaceInfoEb
typedef void (*Surface_lock_fallback)(void *, void *, int);
// _ZN7android7Surface4lockEPNS0_11SurfaceInfoEPNS_6RegionE
typedef void (*Surface_lock)(void *, void *, void *);
// _ZN7android7Surface13unlockAndPostEv
typedef void (*Surface_unlockAndPost)(void *);

static int surfacemutexinit = 0;
static pthread_mutex_t surfacemutex;

typedef struct _SurfaceInfo {
    uint32_t    w;
    uint32_t    h;
    uint32_t    s;
    uint32_t    usage;
    uint32_t    format;
    uint32_t*   bits;
    uint32_t    reserved[2];
} SurfaceInfo;

static SurfaceInfo info;
static Surface_lock s_lock_fallback = NULL;
static Surface_lock s_lock = NULL;
static Surface_unlockAndPost s_unlockAndPost = NULL;
void *surface = NULL;

static void *LoadSurface(const char *psz_lib)
{
    void *p_library = dlopen(psz_lib, RTLD_NOW);

    if (p_library) {
    	s_lock_fallback = dlsym(p_library, ANDROID_SYM_S_LOCK_FALLBACK);
        s_lock = dlsym(p_library, ANDROID_SYM_S_LOCK);
        s_unlockAndPost = dlsym(p_library, ANDROID_SYM_S_UNLOCK);
        if ((s_lock || s_lock_fallback) && s_unlockAndPost) {
            return p_library;
        }
        dlclose(p_library);
    }

    return NULL;
}

static void *InitLibrary()
{
	void *p_library;

	if ((p_library = LoadSurface("libgui.so"))) {
		D("libgui.so load ok");
		return p_library;
	}
	if ((p_library = LoadSurface("libsurfaceflinger_client.so"))) {
		D("libsurfaceflinger_client.so load ok");
		return p_library;
	}
	if ((p_library = LoadSurface("libui.so"))) {
		D("libui.so load ok");
		return p_library;
	}

	D("load all libs failed");
	return NULL;
}

void createSurfaceLock()
{
	if (surfacemutexinit == 0) {
    	pthread_mutex_init(&surfacemutex, 0);
		surfacemutexinit = 1;
    	D("createSurfaceLock");
	} else {
		D("surfaceLock has created already");
	}
}

void destroySurfaceLock()
{
	if (surfacemutexinit != 0) {
		pthread_mutex_destroy(&surfacemutex);
		surfacemutexinit = 0;
		D("destroySurfaceLock");
	} else {
		D("surfaceLock has destroyed already");
	}
}

int lockSurface()
{
#if QVOD_DEBUG
	D("lockSurface **************");
#endif
    if (surface) {
    	if (s_lock) {
    		s_lock(surface, &info, NULL);
    	} else if (s_lock_fallback) {
    		s_lock_fallback(surface, &info, (void*)1);
    	}
        D("lockSurface ok");
		return 1;
    }

    D("lockSurface fail");
	return 0;
}

void unlockSurface()
{
    if (surface && s_unlockAndPost) {
        s_unlockAndPost(surface);		
		#if QVOD_DEBUG
	    D("unlockSurface **************");
        #endif
    }
}

jint attach(JNIEnv* env, jobject thiz, jobject surf)
{
	D("attach");
    jclass cls = (*env)->GetObjectClass(env, surf);
    jfieldID fid;
	jthrowable exp;

	void* ptr;
	fid = (*env)->GetFieldID(env, cls, "mSurface", "I");
    if (fid == NULL) {
    	D("attach exception occured");
		exp = (*env)->ExceptionOccurred(env);
		if (exp) {
			(*env)->DeleteLocalRef(env, exp);
			(*env)->ExceptionClear(env);
		}
        fid = (*env)->GetFieldID(env, cls, "mNativeSurface", "I");
        if (fid == NULL) {
        	(*env)->ExceptionClear(env);
        }
    }
	
    ptr = (void*)(*env)->GetIntField(env, surf, fid);
    if (ptr) {
	    void *p_library;
	    createSurfaceLock();
        pthread_mutex_lock(&surfacemutex);
        surface = ptr;
        p_library = InitLibrary();
        pthread_mutex_unlock(&surfacemutex);

        if (!p_library) {
             D("attach failed no surface\n\r");
             return -1;
        }
        D("attach ok");
        return 0;
    } else {
    	D("attach failed no surfacemutexinit or ptr");
    }

    return -1;
}

jint detach(JNIEnv* env, jobject thiz)
{
	D("detach");
	if (!surfacemutexinit) {
		D("surfacemutex == 0 when detach");
		return 0;
	}

	pthread_mutex_lock(&surfacemutex);
    surface = NULL;
    s_lock = NULL;
    s_lock_fallback = NULL;
    s_unlockAndPost = NULL;
	pthread_mutex_unlock(&surfacemutex);
	destroySurfaceLock();

	D("detach ok\n\r");
	return 0;
}

static int video_init_android(int w,int h)
{
    return 0;
}

static int video_display_android(void* pFrame, int displaymode)
{
	D("video_display_android");
    int mode, ret = 0;
    void *pScreen = 0;
    int sw = 0;

    if (!lockSurface()) {
		D("no surface **************");
		return 0;
    }

    D("s = %d, w = %d", info.s, info.w);
	sw = (info.s >= info.w ? info.s : info.w);	//info.w;
	pScreen = (void *)(info.s >= info.w ? info.bits : NULL);

    if (!sw || !pScreen) {
    	D("fail in video_display_android, sw = %d, pScreen = %x", sw, pScreen);
    	ret = -1;
        goto fail;
    }

    SendRequest(REQUEST_OUTPUT_VIDEO, sw << 1, info.h, pScreen, pFrame);

fail:
    unlockSurface();
    return ret;
}

static void video_free_android()
{
}

video_t video_android = {
    .name	 = "android",
    .init 	 = video_init_android,
    .display = video_display_android,
    .free	 = video_free_android,
};

