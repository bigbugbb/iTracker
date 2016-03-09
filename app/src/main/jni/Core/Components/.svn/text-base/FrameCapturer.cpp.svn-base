//
//  FrameCapturer.cpp
//  QvodPlayer
//
//  Created by bigbug on 12-2-21.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include <iostream>
#include "GUIDs.h"
#include "FFmpegData.h"
#include "FrameCapturer.h"

#define FRAME_CAPTURED      0
#define PREVIEW_CAPTURED    1

CFrameCapturer::CFrameCapturer(IDependency* pDepend)
    : CDependencyObject(pDepend)
{
    SetCaptureFormat(PIX_FMT_RGB565);
    
    Create();
    Start();
}

CFrameCapturer::~CFrameCapturer()
{
    Close();
    
    if (m_pSwsCtx) {
        sws_freeContext(m_pSwsCtx);
        m_pSwsCtx = NULL;
    }
}

CFrameCapturer* CFrameCapturer::GetInstance(IDependency* pDepend)
{
    static CFrameCapturer s_FrmCap(pDepend);
    
    return &s_FrmCap;
}

void CFrameCapturer::SetCaptureFormat(PixelFormat eFormat)
{
    CAutoLock cObjectLock(&m_csCapture);
    
    m_eDstFmt = eFormat;
}

int CFrameCapturer::CaptureFrame(CMediaObject* pSender, void* pData)
{
    CAutoLock cObjectLock(&m_csCapture);
    
    AVFrame* pSrc = static_cast<AVFrame*>(pData);
    PixelFormat eSrcFmt = (PixelFormat)pSrc->format;
    Message msg(MSG_OTHER, FALSE, FRAME_CAPTURED);
    
    AVFrame* pCapture = avcodec_alloc_frame();
    if (!pCapture || avpicture_alloc((AVPicture*)pCapture, m_eDstFmt, pSrc->width, pSrc->height) < 0) {
        goto fail;
    }
#ifdef iOS
    // eSrcFmt should be equal to m_eDstFmt on iOS
    av_picture_copy((AVPicture*)pCapture, (AVPicture*)pSrc, eSrcFmt, pSrc->width, pSrc->height);
#else
	m_pSwsCtx = sws_getCachedContext(m_pSwsCtx, pSrc->width, pSrc->height, eSrcFmt, 
            pSrc->width, pSrc->height, m_eDstFmt, SWS_FAST_BILINEAR, NULL, NULL, NULL);
    if (!m_pSwsCtx) {
        goto fail;
    }
    if (!sws_scale(m_pSwsCtx, pSrc->data, pSrc->linesize, 0, pSrc->height, pCapture->data, pCapture->linesize)) {
        goto fail;
    }
#endif
    pCapture->width  = pSrc->width;
    pCapture->height = pSrc->height;
    pCapture->format = m_eDstFmt;
    
    if (pSender->GetGUID() == GUID_PREVIEW_VIDEO_RENDERER) {
        msg.dwParam1 = PREVIEW_CAPTURED; // change the default value
    }
    msg.pUserData = pCapture;
    SendMessage(msg);
    
    return S_OK;
fail:
    if (pCapture) {
        if (pCapture->data[0]) {
            avpicture_free((AVPicture*)pCapture); pCapture->data[0] = NULL;
        }
        av_free(pCapture);
    }
    
    return E_FAIL;
}

int CFrameCapturer::SendMessage(const Message& msg)
{
    m_MsgQueue.AddMessage(msg);
    
    Signal();
    
    m_etCapture.Wait();
    
    return S_OK;
}

int CFrameCapturer::RecvMessage(Message& msg)
{
    if (!m_MsgQueue.Size()) {
        Wait();
    }
    m_MsgQueue.GetMessage(msg);
    
    return S_OK;
}

THREAD_RETURN CFrameCapturer::ThreadProc()
{
    Message msg;
    
    while (m_bRun) {
        RecvMessage(msg);

        if (msg.nID == MSG_OTHER) {
            AVFrame* pCapture = (AVFrame*)msg.pUserData;
            
            switch (msg.dwParam1) {
            case FRAME_CAPTURED:
                NotifyEvent(EVENT_FRAME_CAPTURED, 0, 0, pCapture);
                break;
            case PREVIEW_CAPTURED:
                NotifyEvent(EVENT_PREVIEW_CAPTURED, 0, 0, pCapture);
                break;
            }
            avpicture_free((AVPicture*)pCapture);
            av_free(pCapture);
            
            m_etCapture.Signal();
        }
    }
    
    return 0;
}

