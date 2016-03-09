//
//  QvodPlayerCallbacks.h
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_QvodPlayerCallbacks_h
#define QVOD_QvodPlayerCallbacks_h

int CreateAudioService(void* pUserData, void* pReserved);
int CreateVideoService(void* pUserData, void* pReserved);
int ResetAudioService(void* pUserData, void* pReserved);
int UpdatePictureSize(void* pUserData, void* pReserved);
int DeliverFrame(void* pUserData, void* pReserved); 
int FrameCaptured(void* pUserData, void* pReserved);
int PlaybackFinished(void* pUserData, void* pReserved);
int NotifyReadIndex(void* pUserData, void* pReserved);
int NotifySeekPosition(void* pUserData, void* pReserved);
int ErrorHandler(void* pUserData, void* pReserved);
int BeginBuffering(void* pUserData, void* pReserved);
int OnBuffering(void* pUserData, void* pReserved);
int EndBuffering(void* pUserData, void* pReserved);
int BeginSubtitle(void* pUserData, void* pReserved);
int EndSubtitle(void* pUserData, void* pReserved);
int OpenFinished(void* pUserData, void* pReserved);
int CloseFinished(void* pUserData, void* pReserved);
int PreviewStarted(void* pUserData, void* pReserved);
int PreviewCaptured(void* pUserData, void* pReserved);
int PreviewStopped(void* pUserData, void* pReserved);
int CheckDevice(void* pUserData, void* pReserved);
int AudioFillBuffer(void* pUserData, void* pReserved);
int GetDownloadSpeed(void* pUserData, void* pReserved);

#endif
