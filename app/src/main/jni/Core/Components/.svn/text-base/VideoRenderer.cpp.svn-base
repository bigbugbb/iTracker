//
//  VideoRenderer.cpp
//  QVOD
//
//  Created by bigbug on 11-11-14.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "GUIDs.h"
#include "FFmpegData.h"
#include "VideoRenderer.h"

///////////////////////////////////////////////////////////////
CVideoRenderer::CVideoRenderer(const GUID& guid, IDependency* pDepend, int* pResult)
    : CSink(guid, pDepend), m_bEnable(TRUE), m_bCapture(FALSE)
{
    m_llCurPTS   = 0;
    m_llStartPTS = 0;
    m_lfTimebase = 0;
    m_lfSeekTime = 0;
    m_pQCtrl  = NULL;
#ifdef ANDROID
    m_pSwsCtx = NULL;
    m_eDstFmt = PIX_FMT_RGB565;
#endif

    m_pCapturer = CFrameCapturer::GetInstance(pDepend);
    AssertValid(m_pCapturer);
    m_pCapturer->SetDependency(pDepend);
    
    PrepareSeek(FALSE);
}

CVideoRenderer::~CVideoRenderer()
{
}

// IVideoRenderer
int CVideoRenderer::SetTimebase(double lfTimebase)
{
    m_lfTimebase = lfTimebase;
    
    return S_OK;
}

int CVideoRenderer::SetMediaStartTime(double lfTime)
{
    m_llStartPTS = lfTime;
    
    return S_OK;
}

int CVideoRenderer::SetMediaSeekTime(double lfTime)
{
    PrepareSeek(TRUE);
    m_lfSeekTime = lfTime;
    m_llCurPTS = lfTime / m_lfTimebase;
    //Log("SetMediaSeekTime: %lf\n", m_lfSeekTime);
    
    return S_OK;
}

int CVideoRenderer::GetMediaCurrentTime(double* pTime)
{
    AssertValid(pTime);
    *pTime = (m_llCurPTS - m_llStartPTS) * m_lfTimebase;

    //Log("GetMediaCurrentTime: %lf\n", *pTime);
    
    return S_OK;
}

int CVideoRenderer::EnableCaptureFrame(BOOL bCapture)
{
    m_bCapture = bCapture;
}

int CVideoRenderer::Receive(ISamplePool* pPool)
{
    CMediaSample sample;
    
    int nResult;
    if ((nResult = pPool->GetUnused(sample)) != S_OK) {
        return nResult;
    }
    
    nResult = OnReceive(sample);
    
    if (nResult != E_RETRY) {
        pPool->Recycle(sample);
    }
    
    return nResult;
}

void CVideoRenderer::SetQualityController(IQualityControl* pQCtrl)
{
    m_pQCtrl = pQCtrl;
}

// CMediaObject
int CVideoRenderer::Load()
{
    Log("CVideoRenderer::Load\n");
    m_llCurPTS = 0;
    m_bTired   = FALSE;
    m_bClose   = FALSE;
    
    Create();
    m_sync.Signal();
    
    CMediaObject::Load();
    return S_OK;
}

int CVideoRenderer::WaitForResources(BOOL bWait)
{
    Log("CVideoRenderer::WaitForResources\n");
    CMediaObject::WaitForResources(bWait);
    
    
    return S_OK;
}

int CVideoRenderer::Idle()
{
    Log("CVideoRenderer::Idle\n");
    Start();
    
    CMediaObject::Idle();
    return S_OK;
}

int CVideoRenderer::Execute()
{
    Log("CVideoRenderer::Execute\n");
    m_VSync.Signal();
    
    CMediaObject::Execute();
    return S_OK;
}

int CVideoRenderer::Pause()
{
    Log("CVideoRenderer::Pause\n");

    CMediaObject::Pause();
    return S_OK;
}

int CVideoRenderer::BeginFlush()
{
    Log("CVideoRenderer::BeginFlush\n");
    CMediaObject::BeginFlush();
    
    m_VSync.Signal();
    m_sync.Wait();
    m_FramePool.Flush();
    
    return S_OK;
}

int CVideoRenderer::EndFlush()
{
    Log("CVideoRenderer::EndFlush\n");
    m_bTired = FALSE;
    PrepareSeek(FALSE);
    m_sync.Signal();
    
    return CMediaObject::EndFlush();
}

int CVideoRenderer::Invalid()
{
    Log("CVideoRenderer::Invalid\n");
    CMediaObject::Invalid();
    
    m_VSync.Signal();
    Close();
    
    return S_OK;
}

int CVideoRenderer::Unload()
{
    Log("CVideoRenderer::Unload\n");
    m_bClose = TRUE;
    m_VSync.Signal();
    Close();
 
    m_FramePool.Reset();
    PrepareSeek(FALSE);
#ifdef ANDROID
    if (m_pSwsCtx) { // used on android
        sws_freeContext(m_pSwsCtx);
        m_pSwsCtx = NULL;
    }
#endif
    CMediaObject::Unload();
    return S_OK;
}

int CVideoRenderer::SetEOS()
{
    if (m_bEOS) {
        return S_OK;
    }
    m_bEOS = TRUE;
    
    NotifyEvent(EVENT_VIDEO_EOS, 0, 0, NULL);
    
    return S_OK;
}

int CVideoRenderer::GetSamplePool(const GUID& guid, ISamplePool** ppPool)
{
    AssertValid(ppPool);
    
    if (!memcmp(&guid, &GUID_VIDEO_DECODER, sizeof(GUID))) {
        *ppPool = &m_FramePool;
    } else {
        *ppPool = NULL;
    }
    
    return S_OK;
}

void CVideoRenderer::PrepareSeek(BOOL bPrepare)
{
    CAutoLock cObjectLock(&m_csPreSeek);
    
    m_bPreSeek = bPrepare;
}

inline
BOOL CVideoRenderer::IsPreparingSeek()
{
    CAutoLock cObjectLock(&m_csPreSeek);
    
    return m_bPreSeek;
}

inline
void CVideoRenderer::UpdateCurTimestamp(const CMediaSample& sample)
{
    if (sample.m_llTimestamp - sample.m_llSyncPoint < 0) {
        m_llCurPTS = sample.m_llSyncPoint;
    } else {
        m_llCurPTS = sample.m_llTimestamp; // used for getting current media time
    }
}

inline
int CVideoRenderer::OnReceive(CMediaSample& sample)
{
    LONGLONG llNow, llShow, llLate;
    CFrame& frame = *reinterpret_cast<CFrame*>(sample.m_pExten);
    
    llNow  = m_pRefClock->GetTime(); // ms
    llShow = (sample.m_llTimestamp - sample.m_llSyncPoint) * m_lfTimebase * 1000; // ms
    llLate = m_pRefClock->IsStarted() ? llNow - llShow : 0;
    //Log("pts: %lld, sync: %lld, now: %lld, show: %lld, late: %lld, frame type: %d, %d\n", 
    //    sample.m_llTimestamp, sample.m_llSyncPoint, llNow, llShow, llLate, frame.m_nType, frame.m_bShow);
    
    if (sample.m_bDiscon) {
        m_pRefClock->UpdateStartTime();
        llShow = llNow = llLate = 0;
    }
    
    if (frame.m_bShow) {
        LONGLONG llDelta, llTmp = GetCurrentTime();
        DeliverFrame(&frame);
        llDelta = GetCurrentTime() - llTmp + 1000;
        
        while (!m_pRefClock->IsStarted() || GetState() & STATE_WAITFORRESOURCES) {
            //Log("state = %d\n", GetState());
            m_VSync.Wait(5000);
            llShow = llNow = 0;
            if (m_bFlush || m_bClose) break;
        }
        if (llShow > llNow) {
            //Log("m_VSync.Wait: %lld", llShow - llNow);
            m_VSync.Wait((llShow - llNow) * 1000 - llDelta);
        }
    } else {
        if (sample.m_bIgnore) {
            llLate = 0;
        }
    }
    
    if (!IsPreparingSeek()) {
        UpdateCurTimestamp(sample);
    }
    
    if (m_pQCtrl) {
        m_pQCtrl->AlterQuality(llLate);
    }
    
    return frame.m_bShow ? S_OK : E_SHOWNEXT;
}

inline
void CVideoRenderer::DeliverFrame(CFrame* pFrame)
{
#ifdef iOS
    NotifyEvent(EVENT_DELIVER_FRAME, 0, 0, pFrame->m_frame.data[0]);
#else
    NotifyEvent(EVENT_DELIVER_FRAME, 0, 0, &pFrame->m_frame);
#endif
    if (m_bCapture) { // no need to lock
        m_pCapturer->CaptureFrame(this, &pFrame->m_frame);
        m_bCapture = FALSE;
    }
}

// DeliverFrameReflection is used on android only
int CVideoRenderer::DeliverFrameReflection(BYTE* pDst, void* pSrc, int nStride)
{
    //Log("DisplayVideoFrame\n");
#ifdef ANDROID
    AVFrame* pYUV = (AVFrame*)pSrc;
    BYTE* pOut[4] = { pDst, NULL, NULL, NULL };
    int nLinesize[4] = { nStride, 0, 0, 0 };

	m_pSwsCtx = sws_getCachedContext(m_pSwsCtx, pYUV->width, pYUV->height, (PixelFormat)pYUV->format,
            pYUV->width, pYUV->height, m_eDstFmt, SWS_FAST_BILINEAR, NULL, NULL, NULL);
	if (!m_pSwsCtx) {
		Log("DisplayVideoFrame m_pSwsCtx NULL\n");
		return E_FAIL;
	}
    int nResult = sws_scale(m_pSwsCtx, pYUV->data, pYUV->linesize, 0, pYUV->height, pOut, nLinesize);
#endif
    //Log("DisplayVideoFrame end\n");
	return S_OK;
}

inline
LONGLONG CVideoRenderer::GetCurrentTime()
{
    struct timeval tmNow;
    gettimeofday(&tmNow, NULL);
    
    return tmNow.tv_sec * 1000000 + tmNow.tv_usec;
}

THREAD_RETURN CVideoRenderer::ThreadProc()
{
    int nWait = 0;
    int nResult;

    while (m_bRun) {
        m_sync.Wait();
        
        if ((nResult = Receive(&m_FramePool)) == E_RETRY) {
            nWait = 20;
        } else {
            nWait = 0;
            m_bTired = nResult == S_OK;
        }
        
        m_sync.Signal();
        
        while (GetState() & STATE_PAUSE) {
            Sleep(20);
            if (!m_bRun || !m_bTired) 
                break;
        }
        
        if (m_vecInObjs[0]->IsEOS() && m_FramePool.GetSize() == 0) {
            SetEOS();
        }
        
        Sleep(nWait);
    }
    
    return 0;
}






