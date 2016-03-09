//
//  FFmpegSubtitleDecoder.cpp
//  QvodPlayer
//
//  Created by bigbug on 12-6-10.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include <iostream>
#include "GUIDs.h"
#include "Global.h"
#include "FFmpegData.h"
#include "FFmpegSubtitleDecoder.h"


CFFmpegSubtitleDecoder::CFFmpegSubtitleDecoder(const GUID& guid, IDependency* pDepend, int* pResult)
    : CMediaObject(guid, pDepend)
{
    m_pSubtitlePool = NULL;
}

CFFmpegSubtitleDecoder::~CFFmpegSubtitleDecoder()
{
    
}

// IFFmpegSubtitleDecoder


int CFFmpegSubtitleDecoder::Load()
{
    Log("CFFmpegSubtitleDecoder::Load\n");
    CMediaObject* pDemuxer = NULL;
    
    for (int i = 0; i < m_vecInObjs.size(); ++i) {
        const GUID& guid = m_vecInObjs[i]->GetGUID();
        if (guid == GUID_DEMUXER) {
            pDemuxer = m_vecInObjs[i];
        }
    }
    if (!pDemuxer) {
        return E_FAIL;
    }
    pDemuxer->GetOutputPool(GetGUID(), &m_pSubtitlePool);
    if (!m_pSubtitlePool) {
        Log("No Subtitle Pool!\n");
    } else {
        Create();
    }
    m_sync.Signal();
    
    CMediaObject::Load();
    return S_OK;
}

int CFFmpegSubtitleDecoder::WaitForResources(BOOL bWait)
{
    Log("CFFmpegSubtitleDecoder::WaitForResources\n");
    CMediaObject::WaitForResources(bWait);
    
    return S_OK;
}

int CFFmpegSubtitleDecoder::Idle()
{
    Log("CFFmpegSubtitleDecoder::Idle\n");
    Start();
    
    CMediaObject::Idle();
    return S_OK;
}

int CFFmpegSubtitleDecoder::Execute()
{
    Log("CFFmpegSubtitleDecoder::Execute\n");
    
    CMediaObject::Execute();
    return S_OK;
}

int CFFmpegSubtitleDecoder::Pause()
{
    Log("CFFmpegSubtitleDecoder::Pause\n");
    
    CMediaObject::Pause();
    return S_OK;
}

int CFFmpegSubtitleDecoder::BeginFlush()
{
    Log("CFFmpegSubtitleDecoder::BeginFlush\n");
    CMediaObject::BeginFlush();
    
    m_sync.Wait();
    
    return S_OK;
}

int CFFmpegSubtitleDecoder::EndFlush()
{
    Log("CFFmpegSubtitleDecoder::EndFlush\n");
    m_sync.Signal();
    
    return CMediaObject::EndFlush();
}

int CFFmpegSubtitleDecoder::Invalid()
{
    Log("CFFmpegSubtitleDecoder::Invalid\n");
    CMediaObject::Invalid();
    
    Close();
    
    return S_OK;
}

int CFFmpegSubtitleDecoder::Unload()
{
    Log("CFFmpegSubtitleDecoder::Unload\n");
    Close();
    
    m_pSubtitlePool = NULL;
    
    CMediaObject::Unload();
    return S_OK;
}

int CFFmpegSubtitleDecoder::SetEOS()
{
    if (m_bEOS) {
        return S_OK;
    }
    
    return CMediaObject::SetEOS();
}

int CFFmpegSubtitleDecoder::RespondDispatch(const GUID& sender, int nType, void* pUserData)
{
    if (nType == DISPATCH_SWITCH_SUBTITLE) {
        m_sync.Wait();
        SwitchSubtitleTrack();
        m_sync.Signal();
    }
    
    return S_OK;
}

BOOL CFFmpegSubtitleDecoder::SwitchSubtitleTrack()
{
    CMediaObject* pDemuxer = NULL;
    
    for (int i = 0; i < m_vecInObjs.size(); ++i) {
        const GUID& guid = m_vecInObjs[i]->GetGUID();
        if (guid == GUID_DEMUXER) {
            pDemuxer = m_vecInObjs[i];
        }
    }
    if (!pDemuxer) {
        return FALSE;
    }
    
    ISamplePool* pSubtitlePool = NULL;
    pDemuxer->GetOutputPool(GetGUID(), &pSubtitlePool);
    if (!pSubtitlePool) {
        return FALSE;
    } else {
        m_pSubtitlePool = pSubtitlePool;
    }
    
    return TRUE;
}

THREAD_RETURN CFFmpegSubtitleDecoder::ThreadProc()
{
    int nWait = 0;
    
    while (m_bRun) {
        m_sync.Wait();
         
        if (Receive(m_pSubtitlePool) == E_RETRY) {
            nWait = 125;
        } else {
            nWait = 0;
        }
        
        m_sync.Signal();
        
        if (m_vecInObjs[0]->IsEOS() && !m_pSubtitlePool->GetSize()) {
            SetEOS();
        }
        
        Sleep(nWait);
    }
    
    return 0;
}

int CFFmpegSubtitleDecoder::OnReceive(CMediaSample& sample)
{
    AssertValid(sample.m_nSize == sizeof(AVPacket));
    AVPacket* pPacket = (AVPacket*)sample.m_pBuf;
    AVCodecContext* pCodecCtx = (AVCodecContext*)sample.m_pExten;
    m_pSubtitle = (SubtitleTrack*)sample.m_pSpecs;
    
    int nResult = Decode(pPacket, pCodecCtx, sample);
    if (nResult != E_RETRY) {
        align_free(pPacket->data);
        pPacket->data = NULL;
    }
    
    return nResult;
}

inline
int CFFmpegSubtitleDecoder::Decode(AVPacket* pPacket, AVCodecContext* pCodecCtx, const CMediaSample& sampleIn)
{    
    AVSubtitle sub;
    
    if (sampleIn.m_bIgnore) {
        return S_OK;
    }

    int nGotSub = 0;
    avcodec_decode_subtitle2(pCodecCtx, &sub, &nGotSub, pPacket); 
    
    if (nGotSub) {
        for (int i = 0; i < sub.num_rects; ++i) {
            AVSubtitleRect* pSubRect = sub.rects[i];
            
            if (pSubRect) {
                Log("%s\n", pSubRect->text);
                Log("%s\n", pSubRect->ass);
            }
        }
        avsubtitle_free(&sub);
    }
    
    return S_OK;
}




