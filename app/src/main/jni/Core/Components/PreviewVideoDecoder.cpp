//
//  PreviewVideoDecoder.cpp
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include <iostream>
#include "PreviewVideoDecoder.h"
#include "GUIDs.h"
#include "FFmpegData.h"

#define MAX_WAITING_COUNT_FOR_PREVIEW   120


CPreviewVideoDecoder::CPreviewVideoDecoder(const GUID& guid, IDependency* pDepend, int* pResult)
    : CFFmpegVideoDecoder(guid, pDepend, pResult)
{
}

CPreviewVideoDecoder::~CPreviewVideoDecoder()
{
    
}

// IPreviewVideoDecoder


int CPreviewVideoDecoder::Load()
{
    Log("CPreviewVideoDecoder::Load\n");
    CMediaObject* pDemuxer = NULL;
    
    for (int i = 0; i < m_vecInObjs.size(); ++i) {
        const GUID& guid = m_vecInObjs[i]->GetGUID();
        if (guid == GUID_PREVIEW_DEMUXER) {
            pDemuxer = m_vecInObjs[i];
        }
    }
    if (!pDemuxer) {
        return E_FAIL;
    }
    pDemuxer->GetOutputPool(GetGUID(), &m_pVideoPool);
    if (!m_pVideoPool) {
        return E_FAIL;
    }
    
    for (int i = 0; i < m_vecOutObjs.size(); ++i) {
        const GUID& guid = m_vecOutObjs[i]->GetGUID();
        if (guid == GUID_PREVIEW_VIDEO_RENDERER) {
            m_pRenderer = m_vecOutObjs[i];
        }
    }
    if (!m_pRenderer) {
        return E_FAIL;
    }
    m_pRenderer->GetInputPool(GetGUID(), &m_pFramePool);
    if (!m_pFramePool) {
        return E_FAIL;
    }
    
    WaitKeyFrame(TRUE);
    Create();
    m_sync.Signal();
    
    CMediaObject::Load();
    return S_OK;
}

int CPreviewVideoDecoder::WaitForResources(BOOL bWait)
{
    Log("CPreviewVideoDecoder::WaitForResources\n");
    
    return CFFmpegVideoDecoder::WaitForResources(bWait);
}

int CPreviewVideoDecoder::Idle()
{
    Log("CPreviewVideoDecoder::Idle\n");
    
    return CFFmpegVideoDecoder::Idle();
}

int CPreviewVideoDecoder::Execute()
{
    Log("CPreviewVideoDecoder::Execute\n");
    
    return CFFmpegVideoDecoder::Execute();
}

int CPreviewVideoDecoder::Pause()
{
    Log("CPreviewVideoDecoder::Pause\n");
    
    return CFFmpegVideoDecoder::Pause();
}

int CPreviewVideoDecoder::BeginFlush()
{
    Log("CPreviewVideoDecoder::BeginFlush\n");
    
    return CFFmpegVideoDecoder::BeginFlush();
}

int CPreviewVideoDecoder::EndFlush()
{
    Log("CPreviewVideoDecoder::EndFlush\n");
    
    return CFFmpegVideoDecoder::EndFlush();
}

int CPreviewVideoDecoder::Invalid()
{
    Log("CPreviewVideoDecoder::Invalid\n");
    
    return CFFmpegVideoDecoder::Invalid();
}

int CPreviewVideoDecoder::Unload()
{
    Log("CPreviewVideoDecoder::Unload\n");
    
    return CFFmpegVideoDecoder::Unload();
}

int CPreviewVideoDecoder::SetEOS()
{
    Log("CPreviewVideoDecoder::SetEOS\n");
    
    return CFFmpegVideoDecoder::SetEOS();
}

int CPreviewVideoDecoder::InterceptEvent(int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData)
{
    if (nEvent == EVENT_UPDATE_PICTURE_SIZE) {
        return E_HANDLED;
    }
    
    return S_OK;
}

BOOL CPreviewVideoDecoder::IsWaitingKeyFrameCanceled()
{
    if (m_nNonKeyCount >= MAX_WAITING_COUNT_FOR_PREVIEW) {
        return TRUE;
    }
    
    return FALSE;
}

THREAD_RETURN CPreviewVideoDecoder::ThreadProc()
{
    return CFFmpegVideoDecoder::ThreadProc();
}

