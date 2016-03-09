//
//  QvodPlayerCallbacks.c
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//


#include "QvodPlayerInterface.h"
#include "QvodPlayerCallbacks.h"
//#include "QAudio.h"
#include "QVideo.h"
#include "QJniMisc.h"
#include <stdint.h>

#define QDebug 0
#if QDebug
#include <android/log.h>
#  define  D(x...)  __android_log_print(ANDROID_LOG_INFO, "qplayer", x)
#else
#  define  D(...)
#endif

//static audio_t* audio = 0;
static video_t* video = 0;


int CreateAudioService(void* pUserData, void* pReserved)
{
    return 1;
}

int CreateVideoService(void* pUserData, void* pReserved)
{
	//D("CreateVideoService");
	unsigned int uSize = *(unsigned int*)pReserved;
	int nWidth  = uSize & 0xFFFF;
	int nHeight = uSize >> 16;

	video_register_all();
	video = video_find_by_name("android");

	//D("CreateVideoService end");
	return 1;
}

int ResetAudioService(void* pUserData, void* pReserved)
{
	// TODO:

	return 1;
}

int OpenFinished(void* pUserData, void* pReserved)
{
	D("OpenFinished");
	QThreadPostEventToJava(ON_OPENED, 0, 0);
	D("QThreadPostEventToJava(ON_OPENED, 0, 0) end");
    
    return 1;
}

int CloseFinished(void* pUserData, void* pReserved)
{
	D("CloseFinished");
	QThreadPostEventToJava(ON_CLOSED, 0, 0);
	D("QThreadPostEventToJava(ON_CLOSED, 0, 0) end");

    return 1;
}

int PreviewStarted(void* pUserData, void* pReserved)
{
    //D("int PreviewStarted(void* pUserData, void* pReserved)\n");
    QThreadPostEventToJava(ON_PREVIEW_STARTED, 0, 0);
    //D("int PreviewStarted(void* pUserData, void* pReserved) end\n");

    return 1;
}

int PreviewStopped(void* pUserData, void* pReserved)
{
    //D("int PreviewStopped(void* pUserData, void* pReserved)\n");
    QThreadPostEventToJava(ON_PREVIEW_STOPPED, 0, 0);
    //D("int PreviewStopped(void* pUserData, void* pReserved) end\n");

    return 1;
}

int PreviewCaptured(void* pUserData, void* pReserved)
{
    //QVFFmpegPlayer* pFFmpegPlayer = (QVFFmpegPlayer*)pUserData;
    PREVIEWINFO* pPreview = (PREVIEWINFO*)pReserved;

    if (!pPreview) {
    	return 0;
    }

    //D("int PreviewCaptured(void* pUserData, void* pReserved)\n");
    previewCaptured(pPreview);
    //D("int PreviewCaptured(void* pUserData, void* pReserved) end\n");

    return 1;
}

int FrameCaptured(void* pUserData, void* pReserved)
{
    FRAMEINFO* pFrame = (FRAMEINFO*)pReserved;

    D("int FrameCaptured(void* pUserData, void* pReserved)\n");

    D("int FrameCaptured(void* pUserData, void* pReserved) end\n");

    return 1;
}

int UpdatePictureSize(void* pUserData, void* pReserved)
{
	//D("UpdatePictureSize");
    unsigned int uSize = *(unsigned int*)pReserved;
    int nWidth  = uSize & 0xFFFF;
    int nHeight = uSize >> 16;

    //D("video size changing");
    QThreadPostEventToJava(ON_VIDEO_SIZE_CHANGED, nWidth, nHeight);
    //D("video size changed end");

    return 1;
}

int DeliverFrame(void* pUserData, void* pReserved)
{
	//D("DeliverFrame");
	if (video) {
		video->display(pReserved, 0);
	}
    //D("DeliverFrame end");
    
    return 1;
}

int PlaybackFinished(void* pUserData, void* pReserved)
{
	//D("PlaybackFinished");
	QThreadPostEventToJava(ON_COMPLETION, 0, 0);
	//D("PlaybackFinished end");
    
    return 1;
}

int CheckDevice(void* pUserData, void* pReserved)
{
	int* pSupport = (int*)pReserved;

	*pSupport = 1;

    return 1;
}

int ErrorHandler(void* pUserData, void* pReserved)
{
	EMSG emsg = { (int)pReserved, 0, 0 };

	//D("ErrorHandler %d", emsg.eID);
	QThreadPostEventToJava(ON_ERROR, emsg.eID, 0);
	//D("ErrorHandler end");
    
    return 1;
}

int BeginBuffering(void* pUserData, void* pReserved)
{
	D("BeginBuffering");
	QThreadPostEventToJava(ON_BEGIN_BUFFERING, 0, 0);
    
    return 1;
}

int OnBuffering(void* pUserData, void* pReserved)
{
    int nProgress = *(float*)pReserved * 100;

    D("OnBuffering, %d%%", nProgress);
    QThreadPostEventToJava(ON_BUFFERING, nProgress, 0);
    
    return 1;
}

int EndBuffering(void* pUserData, void* pReserved)
{
	D("EndBuffering");
	QThreadPostEventToJava(ON_END_BUFFERING, 0, 0);
    
    return 1;
}

int BeginSubtitle(void* pUserData, void* pReserved)
{
    // TODO:

    return 1;
}

int EndSubtitle(void* pUserData, void* pReserved)
{
    // TODO:

    return 1;
}

int NotifyReadIndex(void* pUserData, void* pReserved)
{
	//D("NotifyReadIndex");
    int64_t* pIndexPos = (int64_t*)pReserved;

    notifyIndexPosition(*pIndexPos);
    
    return 1;
}

int NotifySeekPosition(void* pUserData, void* pReserved)
{
	//D("NotifySeekPosition");
    int64_t* pSeekPos = (int64_t*)pReserved;

    notifySeekPosition(*pSeekPos);
    
    return 1;
}

int GetDownloadSpeed(void* pUserData, void* pReserved)
{
	int* pSpeed = (int*)pReserved;

	if (pSpeed) {
		*pSpeed = getDownloadSpeed();
	} else {
		D("pSpeed == NULL");
	}

    return 1;
}
