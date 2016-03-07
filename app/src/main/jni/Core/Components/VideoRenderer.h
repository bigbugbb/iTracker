//
//  VideoRenderer.h
//  QVOD
//
//  Created by bigbug on 11-11-14.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_VideoRenderer_h
#define QVOD_VideoRenderer_h

#include "../MediaObject.h"
#include "Pools.h"
#include "FrameCapturer.h"


class CVideoRenderer : public CSink,
                       public CThread,
                       public IVideoRenderer
{
public:
    CVideoRenderer(const GUID& guid, IDependency* pDepend, int* pResult);
    virtual ~CVideoRenderer();
    
    // IVideoRenderer
    virtual int SetTimebase(float fTimebase);
    virtual int SetMediaSeekTime(float lfTime);
    virtual int GetMediaCurrentTime(float* pTime);
    virtual int SetMediaStartTime(float fTime);
    virtual int EnableCaptureFrame(BOOL bCapture);
    virtual int DeliverFrameReflection(BYTE* pDst, void* pSrc, int nStride, int nHeight); // used on android
    
    void SetQualityController(IQualityControl* pQCtrl);

protected:
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
    int GetInputPool(const GUID& requestor, ISamplePool** ppPool);
    
    int Receive(ISamplePool* pPool);
    int OnReceive(CMediaSample& sample);
    LONGLONG GetCurrentTime();
    void UpdateCurTimestamp(const CMediaSample& sample);
    
    virtual void DeliverFrame(CFrame* pFrame);
    virtual THREAD_RETURN ThreadProc();
    
    CEvent      m_sync;
    CEvent      m_VSync;
    BOOL        m_bTired;
    BOOL        m_bEnable;
    BOOL        m_bClose;
    BOOL        m_bCapture;

    float       m_fTimebase;
    LONGLONG    m_llCurPTS;
    LONGLONG    m_llStartPTS;
    float       m_fSeekTime;
    
    CFramePool  m_FramePool;
    
    IQualityControl*   m_pQCtrl;
    CFrameCapturer*    m_pCapturer;
    
#ifdef ANDROID
    AVPixelFormat m_eDstFmt;
    struct SwsContext* m_pSwsCtx;
#endif
private:
    void PrepareSeek(BOOL bPrepare = TRUE);
    BOOL IsPreparingSeek();
    
    BOOL        m_bPreSeek;
    CLock       m_csPreSeek;
};

#endif
