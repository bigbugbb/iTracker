//
//  PreviewGraphManager.cpp
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include <iostream>
#include "PreviewGraphManager.h"
#include "Components.h"


CPreviewGraphManager::CPreviewGraphManager(IDependency* pDepend, int* pResult)
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
    
    Start();
}

CPreviewGraphManager::~CPreviewGraphManager()
{
    DestroyGraph();
}

int CPreviewGraphManager::BuildGraph()
{
    CGraphManager::BuildGraph();
    
    int nResult = S_OK;
    CMediaObject *pDemuxer, *pVideoDecoder, *pVideoRenderer;
    
    if (!(pDemuxer = new CPreviewDemuxer(GUID_PREVIEW_DEMUXER, m_pDepend, &nResult)) && nResult != S_OK) {
        return E_FAIL;
    }
    
    if (!(pVideoDecoder = new CPreviewVideoDecoder(GUID_PREVIEW_VIDEO_DECODER, m_pDepend, &nResult)) && nResult != S_OK) {
        return E_FAIL;
    }
    
    if (!(pVideoRenderer = new CPreviewVideoRenderer(GUID_PREVIEW_VIDEO_RENDERER, m_pDepend, &nResult)) && nResult != S_OK) {
        return E_FAIL;
    }
    
    m_vecObjs.push_back(pDemuxer);
    m_vecObjs.push_back(pVideoDecoder);
    m_vecObjs.push_back(pVideoRenderer);
    
    if (!m_Graph.Insert(pDemuxer)) return E_FAIL;
    if (!m_Graph.Insert(pVideoDecoder)) return E_FAIL;
    if (!m_Graph.Insert(pVideoRenderer)) return E_FAIL;
    
    if (!m_Graph.Connect(pDemuxer, pVideoDecoder)) return E_FAIL;
    if (!m_Graph.Connect(pVideoDecoder, pVideoRenderer)) return E_FAIL;
    
    if (!m_Graph.Prepare()) return E_FAIL;
    m_pSource = m_Graph.GetSource();
    
    return S_OK;
}

int CPreviewGraphManager::DestroyGraph()
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

CMediaObject* CPreviewGraphManager::GetComponent(const GUID& guid)
{
    for (int i = 0; i < m_vecObjs.size(); ++i) {
        GUID guid2 = m_vecObjs[i]->GetGUID();
        if (!memcmp(&guid2, &guid, sizeof(GUID))) {
            return m_vecObjs[i];
        }
    }
    
    return NULL;
}

int CPreviewGraphManager::EnableComponent(const GUID& guid, BOOL bEnable)
{
    CMediaObject* pObj = GetComponent(guid);
    
    pObj->Enable(bEnable);
    
    return S_OK;
}

void CPreviewGraphManager::OnInit(Argument& arg)
{
}

void CPreviewGraphManager::OnLoaded(Argument& arg)
{
    CPreviewDemuxer* pDemuxer = dynamic_cast<CPreviewDemuxer*>(GetComponent(GUID_PREVIEW_DEMUXER));
    CPreviewVideoDecoder* pVideoDecoder = dynamic_cast<CPreviewVideoDecoder*>(GetComponent(GUID_PREVIEW_VIDEO_DECODER));
    CPreviewVideoRenderer* pVideoRenderer = dynamic_cast<CPreviewVideoRenderer*>(GetComponent(GUID_PREVIEW_VIDEO_RENDERER));
    
    pVideoRenderer->SetQualityController(pVideoDecoder);
    pVideoRenderer->SetSyncSource(m_pRefClock);

    double lfStartTime;
    pDemuxer->GetMediaStartTime(&lfStartTime);
    pVideoRenderer->SetMediaStartTime(lfStartTime);
    double lfTimebase;
    pDemuxer->GetVideoTimebase(&lfTimebase);
    pVideoRenderer->SetTimebase(lfTimebase);
    
    NotifyEvent(EVENT_PREVIEW_STARTED, 0, 0, NULL);
}

void CPreviewGraphManager::OnOpened(Argument& arg)
{
}

void CPreviewGraphManager::OnPlay(Argument& arg)
{
}

void CPreviewGraphManager::OnPlayed(Argument& arg)
{
}

void CPreviewGraphManager::OnPause(Argument& arg)
{
}

void CPreviewGraphManager::OnPaused(Argument& arg)
{
}

void CPreviewGraphManager::OnClose(Argument& arg)
{
}

void CPreviewGraphManager::OnClosed(Argument& arg)
{
    NotifyEvent(EVENT_PREVIEW_STOPPED, 0, 0, NULL);
}

void CPreviewGraphManager::OnFlush(Argument& arg)
{
}

void CPreviewGraphManager::OnInvalid(Argument& arg)
{
}



