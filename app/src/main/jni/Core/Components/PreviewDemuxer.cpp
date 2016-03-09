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
    m_nJumpLimit = INT32_MAX;
    m_llLastVideoTS = AV_NOPTS_VALUE;
}

CPreviewDemuxer::~CPreviewDemuxer()
{
    Release();
}

int CPreviewDemuxer::InitialConfig(const char* szURL, float fOffset, BOOL bRemote)
{
    if (strlen(szURL) <= 0) 
        return E_FAIL;
    
    m_strURL  = szURL;
    m_fOffset = FFMIN(FFMAX(0, fOffset), 1.0);
    m_bRemote = bRemote;
    
    avio_set_remote(bRemote);
//    av_set_notify_cb(AV_NOTIFY_RECONNECT, m_bRemote ? notify_reconnect_cb : NULL, &m_format);
    
    return S_OK;
}

int CPreviewDemuxer::GetOutputPool(const GUID& requestor, ISamplePool** ppPool)
{
    AssertValid(ppPool);
    
    if (requestor == GUID_PREVIEW_VIDEO_DECODER) {
        *ppPool = m_PoolsV.GetCurPool();
    } else {
        *ppPool = NULL;
    }
    
    return S_OK;
}

int CPreviewDemuxer::Load()
{
    Log("CPreviewDemuxer::Load\n");
    maintain_avio();

    if (avformat_open_input(&m_format.pFmtCtx, m_strURL.c_str(), NULL, NULL) != 0) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADPREVIEW, 0, NULL);
        return E_FAIL;
    }
    if (avformat_find_stream_info(m_format.pFmtCtx, NULL) < 0) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADPREVIEW, 0, NULL);
        return E_FAIL;
    }
    if (!PrepareCodecs(m_format.pFmtCtx)) { // audio/video codec都找不到
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADPREVIEW, 0, NULL);
        return E_FAIL;
    }
    m_format.bDecodeA = PrepareAudioData(m_format.pFmtCtx);
    m_format.bDecodeV = PrepareVideoData(m_format.pFmtCtx);
    m_format.bDecodeS = FALSE;
    VideoTrack* pTrackV = m_format.GetCurVideoTrack(); 
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    if (m_format.bDecodeV && !m_format.bDecodeA) {
		m_llDisconThreshold = TS_JUMP_THRESHOLD / pTrackV->fTimebase;
	} else if (m_format.bDecodeA && m_format.bDecodeV) {
		m_llDisconThreshold = TS_JUMP_THRESHOLD / pTrackV->fTimebase;
		m_format.bDecodeA = FALSE;
	} else if (m_format.bDecodeA && !m_format.bDecodeV) {
		m_llDisconThreshold = TS_JUMP_THRESHOLD / pTrackA->fTimebase;
	} else {
		NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADPREVIEW, 0, NULL);
		return E_FAIL;
	}
    
    UpdateSyncPoint(0);
    m_llStartTime = m_llSyncPoint; // first key frame timestamp
    LONGLONG llDuration = m_format.bDecodeV ? pTrackV->llDuration : pTrackA->llDuration;
    if (m_fOffset > 0 && m_fOffset <= llDuration / AV_TIME_BASE) {
        Seek(m_fOffset);
    }
    
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

int CPreviewDemuxer::Release()
{
    CFFmpegDemuxer::Release();
    
    return S_OK;
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
        
        if (m_format.bDecodeA && !m_format.bDecodeV) {
            NotifyEvent(EVENT_PREVIEW_CAPTURED, 1, 0, m_format.GetCurAudioTrack());
            m_bEOS = TRUE;
        }
        
        if (m_PoolsV.GetCurPoolSize() < SCREENSHOT_VIDEO_POOL_SIZE) {
            if (ReadPacket(m_format.pFmtCtx, &packet)) {
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






