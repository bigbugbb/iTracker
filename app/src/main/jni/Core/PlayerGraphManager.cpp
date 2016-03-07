//
//  QvodGraphManager.cpp
//  QVOD
//
//  Created by bigbug on 11-11-15.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "SysConsts.h"
#include "Components.h"
#include "BufferingManager.h"
#include "PlayerGraphManager.h"
#include "Components/FFmpegData.h"
#include "Components/FFmpegCallbacks.h"


CPlayerGraphManager::CPlayerGraphManager(IDependency* pDepend, int* pResult)
    : CGraphManager(pDepend, pResult)
{
    int nResult = S_OK;
    
    nResult = BuildGraph();
    if (nResult != S_OK) {
        if (pResult) {
            *pResult = nResult;
        }
        return;
    }

    m_pBufMgr = CBufferingManager::GetInstance();
    AssertValid(m_pBufMgr);
    if (!m_pBufMgr) {
        if (pResult) {
            *pResult = E_OUTOFMEMORY;
        }
        DestroyGraph();
        return;
    }
    m_pBufMgr->SetDependency(pDepend);
    
    SetAudioEOS(FALSE);
    SetVideoEOS(FALSE);

    Start();
}

CPlayerGraphManager::~CPlayerGraphManager()
{
    DestroyGraph();
}

int CPlayerGraphManager::BuildGraph()
{
    CGraphManager::BuildGraph();
    
    int nResult = S_OK;
    CMediaObject *pDemuxer, *pVideoDecoder, *pAudioDecoder, *pSubtitleDecoder, *pVideoRenderer, *pAudioRenderer;
    
    if (!(pDemuxer = new CFFmpegDemuxer(GUID_DEMUXER, m_pDepend, &nResult)) && nResult != S_OK) {
        return E_FAIL;
    }
    
    if (!(pVideoDecoder = new CFFmpegVideoDecoder(GUID_VIDEO_DECODER, m_pDepend, &nResult)) && nResult != S_OK) {
        return E_FAIL;
    }
    
    if (!(pAudioDecoder = new CFFmpegAudioDecoder(GUID_AUDIO_DECODER, m_pDepend, &nResult)) && nResult != S_OK) {
        return E_FAIL;
    }
    
    if (!(pSubtitleDecoder = new CFFmpegSubtitleDecoder(GUID_SUBTITLE_DECODER, m_pDepend, &nResult)) && nResult != S_OK) {
        return E_FAIL;
    }
    
    if (!(pVideoRenderer = new CVideoRenderer(GUID_VIDEO_RENDERER, m_pDepend, &nResult)) && nResult != S_OK) {
        return E_FAIL;
    }
    
    if (!(pAudioRenderer = new CAudioRenderer(GUID_AUDIO_RENDERER, m_pDepend, &nResult)) && nResult != S_OK) {
        return E_FAIL;
    }
    
    m_vecObjs.push_back(pDemuxer);
    m_vecObjs.push_back(pVideoDecoder);
    m_vecObjs.push_back(pAudioDecoder);
    m_vecObjs.push_back(pSubtitleDecoder);
    m_vecObjs.push_back(pVideoRenderer);
    m_vecObjs.push_back(pAudioRenderer);
    
    if (!m_Graph.Insert(pDemuxer))         return E_FAIL;
    if (!m_Graph.Insert(pVideoDecoder))    return E_FAIL;
    if (!m_Graph.Insert(pAudioDecoder))    return E_FAIL;
    if (!m_Graph.Insert(pSubtitleDecoder)) return E_FAIL;
    if (!m_Graph.Insert(pVideoRenderer))   return E_FAIL;
    if (!m_Graph.Insert(pAudioRenderer))   return E_FAIL;
    
    if (!m_Graph.Connect(pDemuxer, pVideoDecoder))       return E_FAIL;
    if (!m_Graph.Connect(pVideoDecoder, pVideoRenderer)) return E_FAIL;
    if (!m_Graph.Connect(pDemuxer, pAudioDecoder))       return E_FAIL;
    if (!m_Graph.Connect(pAudioDecoder, pAudioRenderer)) return E_FAIL;
    if (!m_Graph.Connect(pDemuxer, pSubtitleDecoder))    return E_FAIL;
    
    if (!m_Graph.Prepare()) return E_FAIL;
    m_pSource = m_Graph.GetSource();
    
//    av_set_notify_cb(AV_NOTIFY_READ_SIZE, notify_recv_size_cb, this);
    
    return S_OK;
}

int CPlayerGraphManager::DestroyGraph()
{
    for (int i = 0; i < m_vecObjs.size(); ++i) {
        if (m_vecObjs[i]) {
            delete m_vecObjs[i];
            m_vecObjs[i] = NULL;
        }
    }
    
    CGraphManager::DestroyGraph();
    
    return S_OK;
}

CMediaObject* CPlayerGraphManager::GetComponent(const GUID& guid)
{
    for (int i = 0; i < m_vecObjs.size(); ++i) {
        GUID guid2 = m_vecObjs[i]->GetGUID();
        if (guid2 == guid) {
            return m_vecObjs[i];
        }
    }

    return NULL;
}

int CPlayerGraphManager::EnableComponent(const GUID& guid, BOOL bEnable)
{
    CMediaObject* pObj = GetComponent(guid);

    pObj->Enable(bEnable);

    return S_OK;
}

BOOL CPlayerGraphManager::IsComponentEnabled(const GUID& guid)
{
	CMediaObject* pObj = GetComponent(guid);

	if (!pObj) {
		return FALSE;
	}

	return pObj->IsEnabled();
}

void CPlayerGraphManager::OnInit(Argument& arg)
{
    CFFmpegDemuxer* pDemuxer = dynamic_cast<CFFmpegDemuxer*>(GetComponent(GUID_DEMUXER));
    
    SetAudioEOS(FALSE);
    SetVideoEOS(FALSE);
    
    if (pDemuxer->IsRemoteFile()) {
        m_pBufMgr->Initialize();
        m_pBufMgr->Create();
        m_pBufMgr->Start();
    }
}

void CPlayerGraphManager::OnLoaded(Argument& arg)
{
    CFFmpegDemuxer* pDemuxer = dynamic_cast<CFFmpegDemuxer*>(GetComponent(GUID_DEMUXER));
    CVideoRenderer* pVideoRenderer = dynamic_cast<CVideoRenderer*>(GetComponent(GUID_VIDEO_RENDERER));
    CAudioRenderer* pAudioRenderer = dynamic_cast<CAudioRenderer*>(GetComponent(GUID_AUDIO_RENDERER));
    CFFmpegVideoDecoder* pVideoDecoder = dynamic_cast<CFFmpegVideoDecoder*>(GetComponent(GUID_VIDEO_DECODER));
    CFFmpegSubtitleDecoder* pSubtitleDecoder = dynamic_cast<CFFmpegSubtitleDecoder*>(GetComponent(GUID_SUBTITLE_DECODER));
    
    pVideoRenderer->SetQualityController(pVideoDecoder);
    
    m_pBufMgr->SetProbe(pDemuxer);

    pVideoRenderer->SetSyncSource(m_pRefClock);
    pAudioRenderer->SetSyncSource(m_pRefClock);
    pSubtitleDecoder->SetSyncSource(m_pRefClock);
    
    int nChannelCount = 0;
    pDemuxer->GetAudioChannelCount(&nChannelCount);
    pAudioRenderer->SetChannelCount(nChannelCount);
    int nSampleFormat;
    pDemuxer->GetAudioSampleFormat(&nSampleFormat);
    pAudioRenderer->SetSampleFormat(nSampleFormat);
    float fStartTime;
    pDemuxer->GetMediaStartTime(&fStartTime);
    pVideoRenderer->SetMediaStartTime(fStartTime);
    pAudioRenderer->SetMediaStartTime(fStartTime);
    float fTimebase;
    pDemuxer->GetAudioTimebase(&fTimebase);
    pAudioRenderer->SetTimebase(fTimebase);
    pDemuxer->GetVideoTimebase(&fTimebase);
    pVideoRenderer->SetTimebase(fTimebase);
    float fSampleRate;
    pDemuxer->GetAudioSampleRate(&fSampleRate);
    pAudioRenderer->SetSampleRate(fSampleRate);
}

void CPlayerGraphManager::OnOpened(Argument& arg)
{
    NotifyEvent(EVENT_OPEN_FINISHED, 0, 0, NULL);
}

void CPlayerGraphManager::OnPlay(Argument& arg)
{
}

void CPlayerGraphManager::OnPlayed(Argument& arg)
{
    NotifyEvent(EVENT_EXECUTE_FINISHED, 0, 0, NULL);
}

void CPlayerGraphManager::OnPause(Argument& arg)
{
}

void CPlayerGraphManager::OnPaused(Argument& arg)
{
    NotifyEvent(EVENT_PAUSE_FINISHED, 0, 0, NULL);
}

void CPlayerGraphManager::OnFlush(Argument& arg)
{
    CFFmpegDemuxer* pDemuxer = dynamic_cast<CFFmpegDemuxer*>(GetComponent(GUID_DEMUXER));
    
    if (pDemuxer->IsRemoteFile()) {
        m_pBufMgr->Reset();
    }
}

void CPlayerGraphManager::OnInvalid(Argument& arg)
{
}

void CPlayerGraphManager::OnClose(Argument& arg)
{
    CFFmpegDemuxer* pDemuxer = dynamic_cast<CFFmpegDemuxer*>(GetComponent(GUID_DEMUXER));
    
    if (pDemuxer->IsRemoteFile()) {
        m_pBufMgr->Close();
    }
}

void CPlayerGraphManager::OnClosed(Argument& arg)
{
    NotifyEvent(EVENT_CLOSE_FINISHED, 0, 0, NULL);
}




