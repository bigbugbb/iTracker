//
//  PreviewVideoDecoder.h
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#ifndef QvodPlayer_PreviewVideoDecoder_h
#define QvodPlayer_PreviewVideoDecoder_h

#include "FFmpegVideoDecoder.h"

class CPreviewVideoDecoder : public CFFmpegVideoDecoder
{
public:
    CPreviewVideoDecoder(const GUID& guid, IDependency* pDepend, int* pResult);
    virtual ~CPreviewVideoDecoder();
    
    // IPreviewVideoDecoder
    
protected:
    // CMediaObject
    int Load();
    int WaitForResources(BOOL bWait);
    int Idle();
    int Execute();
    int Pause();
    int BeginFlush();
    int EndFlush();
    int Invalid();
    int Unload();
    int SetEOS();
    
    virtual BOOL IsWaitingKeyFrameCanceled();
    virtual int InterceptEvent(int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData);
    virtual THREAD_RETURN ThreadProc();
};

#endif
