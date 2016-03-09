//
//  PreviewDemuxer.cpp
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include <iostream>
#include "PreviewDemuxer.h"
#include "GUIDs.h"
#include "FFmpegData.h"
#include "FFmpegCallbacks.h"

#define TS_JUMP_THRESHOLD       10    // second

#define SCREENSHOT_VIDEO_POOL_SIZE  15
#define SCREENSHOT_AUDIO_POOL_SIZE  1   // audio pool is not used now, leave it for further extending

CPreviewDemuxer::CPreviewDemuxer(const GUID& guid, IDependency* pDepend, int* pResult)
    : CFFmpegDemuxer(guid, pDepend, pResult)
{
    m_nJumpLimit = 0x7FFFFFFF;
    m_llLastVideoTS = AV_NOPTS_VALUE;
    
    POOL_PROPERTIES request, actual;
    request.nSize  = sizeof(AVPacket);
    request.nCount = SCREENSHOT_VIDEO_POOL_SIZE;
    m_VideoPool.SetProperties(&request, &actual);
    request.nSize  = sizeof(AVPacket);
    request.nCount = SCREENSHOT_AUDIO_POOL_SIZE;
    m_AudioPool.SetProperties(&request, &actual);
}

CPreviewDemuxer::~CPreviewDemuxer()
{
    ReleaseResources();
}

int CPreviewDemuxer::InitialConfig(const char* szURL, double lfOffset, BOOL bRemote)
{
    if (strlen(szURL) <= 0) 
        return E_FAIL;
    
    m_strURL   = szURL;
    m_lfOffset = FFMIN(FFMAX(0, lfOffset), 1.0);
    m_bRemote  = bRemote;
    
//    avio_set_remote(bRemote);
//    av_set_notify_cb(AV_NOTIFY_RECONNECT, m_bRemote ? notify_reconnect_cb : NULL, &m_format);
    
    return S_OK;
}

int CPreviewDemuxer::GetSamplePool(const GUID& guid, ISamplePool** ppPool)
{
    AssertValid(ppPool);
    
    if (!memcmp(&guid, &GUID_PREVIEW_VIDEO_DECODER, sizeof(GUID))) {
        *ppPool = &m_VideoPool;
    } else {
        *ppPool = NULL;
    }
    
    return S_OK;
}

int CPreviewDemuxer::Load()
{
    Log("CPreviewDemuxer::Load\n");
//    maintain_avio();
    ReleaseResources();
    
    if (avformat_open_input(&m_format.pFormatContext, m_strURL.c_str(), NULL, NULL) != 0) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADPREVIEW, 0, NULL);
        return E_FAIL;
    }
    if (avformat_find_stream_info(m_format.pFormatContext, NULL) < 0) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADPREVIEW, 0, NULL);
        return E_FAIL;
    }
    if (!PrepareCodecs(m_format.pFormatContext)) { // audio/video codec都找不到
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADPREVIEW, 0, NULL);
        return E_FAIL;
    }
    m_format.bDecodeAudio = PrepareAudioData(m_format.pFormatContext);
    m_format.bDecodeVideo = PrepareVideoData(m_format.pFormatContext);
    if (m_format.bDecodeVideo && !m_format.bDecodeAudio) {
        m_llDisconThreshold = TS_JUMP_THRESHOLD / m_video.lfTimebase;
    } else if (m_format.bDecodeAudio && m_format.bDecodeVideo) {
        m_llDisconThreshold = TS_JUMP_THRESHOLD / m_video.lfTimebase;
        m_lfConvert = m_video.lfTimebase / m_audio.lfTimebase;
    } else {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADPREVIEW, 0, NULL);
        return E_FAIL;
    }
    m_format.bDecodeAudio = FALSE; // audio is not used now for previewing

    UpdateSyncPoint(0);
    m_llStartTime = m_llSyncPoint; // first key frame timestamp
    m_lfOffset = m_lfOffset * m_video.llFormatDuration / AV_TIME_BASE;
    Seek(m_lfOffset);
    
    Create();
    m_sync.Signal();
    
    CMediaObject::Load();
    return S_OK;
}

int CPreviewDemuxer::WaitForResources(BOOL bWait)
{
    Log("CPreviewDemuxer::WaitForResources\n");
    
    return CFFmpegDemuxer::WaitForResources(bWait);
}

int CPreviewDemuxer::Idle()
{
    Log("CPreviewDemuxer::Idle\n");
   
    return CFFmpegDemuxer::Idle();
}

int CPreviewDemuxer::Execute()
{
    Log("CPreviewDemuxer::Execute\n");
    
    return CFFmpegDemuxer::Execute();
}

int CPreviewDemuxer::Pause()
{
    Log("CPreviewDemuxer::Pause\n");
    
    return CFFmpegDemuxer::Pause();
}

int CPreviewDemuxer::BeginFlush()
{
    Log("CPreviewDemuxer::BeginFlush\n");
    
    return CFFmpegDemuxer::BeginFlush();
}

int CPreviewDemuxer::EndFlush()
{
    Log("CPreviewDemuxer::EndFlush\n");
    
    return CFFmpegDemuxer::EndFlush();
}

int CPreviewDemuxer::Invalid()
{
    Log("CPreviewDemuxer::Invalid\n");
    
    return CFFmpegDemuxer::Invalid();
}

int CPreviewDemuxer::Unload()
{
    Log("CPreviewDemuxer::Unload\n");
    
    return CFFmpegDemuxer::Unload();
}

int CPreviewDemuxer::SetEOS()
{
    Log("CPreviewDemuxer::SetEOS\n");
    
    return CFFmpegDemuxer::SetEOS();
}

int CPreviewDemuxer::RebuildIndexEntries(AVFormatContext* pFmtCtx, AVCodecContext* pVideoCtx, AVCodecContext* pAudioCtx)
{
    return E_FAIL;
}

THREAD_RETURN CPreviewDemuxer::ThreadProc()
{
    int nWait = 20;
    AVPacket packet;
    
    while (m_bRun) {
        m_sync.Wait();
        
        if (m_bEOS) {
            m_sync.Signal(); Sleep(50);
            continue;
        }
        
        if (m_VideoPool.Size() < SCREENSHOT_VIDEO_POOL_SIZE) {
            if (ReadPacket(m_format.pFormatContext, &packet)) {
                FillPacketPool(&packet);
                nWait = 0;
            } else {
                nWait = 50;
            }
        } else {
            nWait = 50; // wait
        }
        
        m_sync.Signal();
        
        Sleep(nWait);
    }
    
    return 0;
}






