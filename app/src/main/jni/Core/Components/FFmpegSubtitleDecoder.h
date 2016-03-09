//
//  FFmpegSubtitleDecoder.h
//  QvodPlayer
//
//  Created by bigbug on 12-6-10.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#ifndef QvodPlayer_FFmpegSubtitleDecoder_h
#define QvodPlayer_FFmpegSubtitleDecoder_h

#include "../SamplePool.h"
#include "../MediaObject.h"
#include "Frame.h"
#include "CompInterfaces.h"


class CFFmpegSubtitleDecoder : public CMediaObject,
                               public CThread,
                               public IFFmpegSubtitleDecoder
{
public:
    CFFmpegSubtitleDecoder(const GUID& guid, IDependency* pDepend, int* pResult);
    virtual ~CFFmpegSubtitleDecoder();
    
    // IFFmpegSubtitleDecoder

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
    int RespondDispatch(const GUID& sender, int nType, void* pUserData);

    int OnReceive(CMediaSample& sample);
    
    virtual THREAD_RETURN ThreadProc();
    virtual int Decode(AVPacket* pPacket, AVCodecContext* pCodecCtx, const CMediaSample& sampleIn);
    
    BOOL SwitchSubtitleTrack();
    
    CEvent          m_sync;
    CEvent          m_SSync;
    
    ISamplePool*    m_pSubtitlePool;
    ISamplePool*    m_pFramePool;

    SubtitleTrack*  m_pSubtitle;
    AVCodecContext* m_pCodecCtx;
};

#endif
