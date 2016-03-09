//
//  FFmpegVideoDecoder.h
//  QVOD
//
//  Created by bigbug on 11-11-14.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_FFmpegVideoDecoder_h
#define QVOD_FFmpegVideoDecoder_h

#include "../SamplePool.h"
#include "../MediaObject.h"
#include "Frame.h"
#include "CompInterfaces.h"


class CFFmpegVideoDecoder : public CMediaObject,
                            public CThread,
                            public IFFmpegVideoDecoder,
                            public IQualityControl
{
public:
    CFFmpegVideoDecoder(const GUID& guid, IDependency* pDepend, int* pResult);
    virtual ~CFFmpegVideoDecoder();
    
    // IFFmpegVideoDecoder
    virtual int GetVideoWidth(int* pWidth);
    virtual int GetVideoHeight(int* pHeight);
    virtual int SetDecodeMode(int nDecMode);
    virtual int DiscardPackets(int nCount);
    virtual int EnableLoopFilter(BOOL bEnable);
    
    // IQualityControl
    virtual int AlterQuality(LONGLONG llLate);
    
    int WaitKeyFrame(BOOL bWait);
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
    int GetSamplePool(const GUID& guid, ISamplePool** ppPool);
    
    int OnReceive(CMediaSample& sample);
    
    virtual int OnVideoSizeChanged();
    virtual THREAD_RETURN ThreadProc();
    int Resize(int nWidth, int nHeight, AVPixelFormat eSrcFmt);
    int EnableLoopFilter2(AVCodecContext* pCodecCtx);
    BOOL IsWaitingKeyFrame();
    BOOL IsIntraOnly(AVCodecID id);
    LONGLONG AdjustTimestamp(LONGLONG llTimestamp, int nDuration);
    int Decode(AVPacket* pPacket, AVCodecContext* pCodecCtx, const CMediaSample& sampleIn);
    
    int             m_nWidth;
    int             m_nHeight;
    BOOL            m_bWaitI;
    BOOL            m_bJumpBack;
    BOOL            m_bLoopFilter;
    CLock           m_csDecode;
    CEvent          m_sync;
    
    LONGLONG        m_llLastInputTS;
    LONGLONG        m_llLastOutputTS;
    
    ISamplePool*    m_pFramePool;
    ISamplePool*    m_pVideoPool;
    
    AVFrame         m_YUV;
    AVPixelFormat   m_eDstFmt;
    CMediaObject*   m_pRenderer;
#ifdef iOS
    SwsContext*     m_pSwsCtx;
#endif
    VideoInfo*      m_pVideo;
    AVCodecContext* m_pCodecCtx;
};

#endif
