//
//  FFmpegDemuxer.cpp
//  QVOD
//
//  Created by bigbug on 11-11-13.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "GUIDs.h"
#include "FFmpegDemuxer.h"
#include "FFmpegCallbacks.h"

#define MAX_BIT_RATE            10000000
#define REBUILD_INDEX_THRESHOLD 15
#define AUDIO_FRAME_DURATION    0.023 // second
#define TS_JUMP_THRESHOLD       10    // second

#define AUDIO_POOL_VOLUME       360
#define VIDEO_POOL_VOLUME       360
#define SUBTITLE_POOL_VOLUME    (AUDIO_POOL_VOLUME / 5) // far more than enough

////////////////////////////////////////////////////////////

CFFmpegDemuxer::CFFmpegDemuxer(const GUID& guid, IDependency* pDepend, int* pResult)
    : CSource(guid, pDepend), m_fOffset(0), m_bRemote(FALSE), m_nJumpLimit(3), 
      m_fJumpBack(0), m_llStartTime(AV_NOPTS_VALUE), m_bDiscard(FALSE)
{
    m_llLastAudioTS    = AV_NOPTS_VALUE;
    m_llLastVideoTS    = AV_NOPTS_VALUE;
    m_llLastSubtitleTS = AV_NOPTS_VALUE;
    
    av_register_all();
//    avio_set_remoteprobe_cb(avio_is_remote);
//    avio_set_interrupt_cb(avio_interrupt_cb);
//    av_set_notify_cb(AV_NOTIFY_READ_INDEX,    notify_read_index_cb, this);
//    av_set_notify_cb(AV_NOTIFY_BUFFER_SIZE,   notify_buf_size_cb,   this);
//    av_set_notify_cb(AV_NOTIFY_SEEK_POSITION, notify_seek_pos_cb,   this);
}

CFFmpegDemuxer::~CFFmpegDemuxer()
{
    Release();
}

// IFFmpegDemuxer
int CFFmpegDemuxer::InitialConfig(const char* szURL, float fOffset, BOOL bRemote)
{
    if (strlen(szURL) <= 0) 
        return E_FAIL;
    
    m_strURL  = szURL;
    m_fOffset = fOffset;
    m_bRemote = bRemote;
    
    avio_set_remote(bRemote);
//    av_set_notify_cb(AV_NOTIFY_RECONNECT, m_bRemote ? notify_reconnect_cb : NULL, &m_format);
    
    return S_OK;
}

int CFFmpegDemuxer::SwitchAudioTrack(int nTrackID)
{
    if (nTrackID < 0 || nTrackID >= m_format.tracksA.size()) {
        Log("track id error\n");
        return E_FAIL;
    }
    
    if (nTrackID == m_format.nCurTrackA) {
        return S_OK;
    }
    
    m_sync.Wait();
    m_PoolsA.SetCurPool(nTrackID);
    m_format.nCurTrackA = nTrackID;
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    m_llLastAudioTS = AV_NOPTS_VALUE;
    m_fConvertA = m_format.bDecodeV ? m_format.GetCurVideoTrack()->fTimebase / pTrackA->fTimebase : 1;
    Dispatch(GUID_AUDIO_DECODER, DISPATCH_SWITCH_AUDIO, pTrackA);
    m_sync.Signal();
    
    return S_OK;
}

int CFFmpegDemuxer::SwitchSubtitleTrack(int nTrackID)
{
    if (nTrackID < 0 || nTrackID >= m_format.tracksS.size()) {
        Log("track id error\n");
        return E_FAIL;
    }
    
    if (nTrackID == m_format.nCurTrackS) {
        return S_OK;
    }
    
    m_sync.Wait();
    m_PoolsS.SetCurPool(nTrackID);
    m_format.nCurTrackS = nTrackID;
    SubtitleTrack* pTrackS = m_format.GetCurSubtitleTrack();
    m_llLastSubtitleTS = AV_NOPTS_VALUE;
    m_fConvertS = m_format.bDecodeV ? m_format.GetCurVideoTrack()->fTimebase / pTrackS->fTimebase : 1;
    Dispatch(GUID_SUBTITLE_DECODER, DISPATCH_SWITCH_SUBTITLE, pTrackS);
    m_sync.Signal();
    
    return S_OK;
}

int CFFmpegDemuxer::RespondFeedback(const GUID& sender, int nType, void* pUserData)
{
    if (sender == GUID_AUDIO_DECODER) {
        m_bDiscard = (nType == FEEDBACK_INSUFFICIENT_DATA);
    }
    
    return S_OK;
}

int CFFmpegDemuxer::SetSeekPosition(float fOffset)
{
    m_fOffset = fOffset;
    
    return S_OK;
}

int CFFmpegDemuxer::GetMediaDuration(float* pDuration)
{
    AssertValid(pDuration);
    float fDuration = 0;

    if (m_format.bDecodeV) {
        VideoTrack* pTrackV = m_format.GetCurVideoTrack();
        if (pTrackV) {
            fDuration = (float)pTrackV->llDuration / AV_TIME_BASE;
        }
    } else if (m_format.bDecodeA) {
        AudioTrack* pTrackA = m_format.GetCurAudioTrack();
        if (pTrackA) {
            fDuration = (float)pTrackA->llDuration / AV_TIME_BASE;
        }
    }
    *pDuration = fDuration;

    return S_OK;
}

int CFFmpegDemuxer::GetMediaStartTime(float* pTime)
{
    AssertValid(pTime);
    *pTime = m_llStartTime;
    
    return S_OK;
}

int CFFmpegDemuxer::GetMediaBitrate(int* pBitrate)
{
    AssertValid(pBitrate);
    VideoTrack* pTrackV = m_format.GetCurVideoTrack();
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    
    *pBitrate = 0;
    if (pTrackA && pTrackV) {
        *pBitrate = pTrackV->nBitrate + pTrackA->nBitrate;
    } else if (!pTrackA && pTrackV) {
        *pBitrate = pTrackV->nBitrate;
    } else if (pTrackA && !pTrackV) {
        *pBitrate = pTrackA->nBitrate;
    }
    
    int64_t llFileSize = avio_size(m_format.pFmtCtx->pb);
    float fDuration = (float)pTrackV->llDuration / AV_TIME_BASE;
    int nBitrate = llFileSize / fDuration * 8;
    
    if (FFABS(nBitrate - *pBitrate) > 300000) {
        *pBitrate = nBitrate;
    }
    
    return S_OK;
}

int CFFmpegDemuxer::GetMediaFormatName(char *pName)
{
    AssertValid(pName);
    AVInputFormat* pInputFormat = m_format.pFmtCtx->iformat;
    
    if (!pInputFormat) {
        return E_FAIL;
    }
    
    strcpy(pName, pInputFormat->name);
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioChannelCount(int* pCount)
{
    if (!m_format.bDecodeA) {
        return E_FAIL;
    }
    
    AssertValid(pCount);
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    if (pTrackA) {
        *pCount = pTrackA->nChannelsPerFrame;
    } else {
        *pCount = 0;
    }
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioSampleFormat(int* pFormat)
{
    AssertValid(pFormat);
    if (!pFormat) {
        return E_FAIL;
    }
    
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    if (pTrackA) {
        *pFormat = pTrackA->nSampleFormat;
    } else {
        *pFormat = AV_SAMPLE_FMT_NONE;
    }
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioTrackCount(int* pCount)
{
    if (!m_format.bDecodeA) {
        return E_FAIL;
    }
    
    AssertValid(pCount);
    *pCount = m_format.tracksA.size();
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioTimebase(float* fTimebase)
{
    if (!m_format.bDecodeA) {
        return E_FAIL;
    }
    
    AssertValid(lfTimebase);
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    if (pTrackA) {
        *fTimebase = pTrackA->fTimebase;
    } else {
        *fTimebase = 0;
    }
    
    return S_OK;
}

int CFFmpegDemuxer::GetVideoTimebase(float* fTimebase)
{
    if (!m_format.bDecodeV) {
        return E_FAIL;
    }
    
    AssertValid(fTimebase);
    VideoTrack* pTrackV = m_format.GetCurVideoTrack();
    if (pTrackV) {
        *fTimebase = pTrackV->fTimebase;
    } else {
        *fTimebase = 0;
    }
    
    return S_OK;
}

int CFFmpegDemuxer::GetCurAudioTrack(int* pTrack)
{
    if (!m_format.bDecodeA) {
        return E_FAIL;
    }
    
    AssertValid(pTrack);
    *pTrack = m_format.nCurTrackA;
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioSampleRate(float* pSampleRate)
{
    if (!m_format.bDecodeA) {
        return E_FAIL;
    }
    
    AssertValid(pSampleRate);
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    if (pTrackA) {
        *pSampleRate = pTrackA->nSampleRate;
    } else {
        *pSampleRate = 0;
    }
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioCodecID(int* pCodecID)
{
    if (!m_format.bDecodeA) {
        return E_FAIL;
    }
    
    AssertValid(pCodecID);
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    if (pTrackA) {
        *pCodecID = pTrackA->nCodecID;
    } else {
        *pCodecID = 0;
    }
    
    return S_OK;
}

int CFFmpegDemuxer::GetVideoCodecID(int* pCodecID)
{
    if (!m_format.bDecodeV) {
        return E_FAIL;
    }
    
    AssertValid(pCodecID);
    VideoTrack* pTrackV = m_format.GetCurVideoTrack();
    if (pTrackV) {
        *pCodecID = pTrackV->nCodecID;
    } else {
        *pCodecID = 0;
    }
    
    return S_OK;
}

int CFFmpegDemuxer::GetVideoFPS(float* pFPS)
{
    if (!m_format.bDecodeV) {
        return E_FAIL;
    }
    
    AssertValid(pFPS);
    VideoTrack* pTrackV = m_format.GetCurVideoTrack();
    if (pTrackV) {
        *pFPS = pTrackV->fFPS;
    } else {
        *pFPS = 0;
    }
    
    return S_OK;
}

// IBufferingProbe
BOOL CFFmpegDemuxer::IsProbing()
{
    return TRUE;
}

BOOL CFFmpegDemuxer::IsNeedBuffering()
{    
    if (!(GetState() & STATE_LOADED)) {
        return TRUE;
    }
    
    if (m_bEOS) {
        return FALSE;
    }
    
    int nAudioSize = m_PoolsA.GetCurPoolSize();
    int nVideoSize = m_PoolsV.GetCurPoolSize();
    
    //Log("audio: %d, video: %d\n", nAudioSize, nVideoSize);
    if ((m_format.bDecodeA && nAudioSize <= 50) || 
        (m_format.bDecodeV && nVideoSize <= 35)) 
    {
        if (nVideoSize >= 150 || nAudioSize >= 220) {
            return FALSE;
        }

        return TRUE;
    }
    
    return FALSE;
}

BOOL CFFmpegDemuxer::IsRemoteFile()
{
    return m_bRemote;
}

BOOL CFFmpegDemuxer::ReadPacket(AVFormatContext* pFmtCtx, AVPacket* pPacket)
{
    int nRet = av_read_frame(pFmtCtx, pPacket);
    
    if (nRet == 0) {
        return TRUE;
    }
    
    switch (nRet) {
    case AVERROR(EPERM):
    case AVERROR_EOF:
        SetEOS();
        break;
    case AVERROR(EIO):
        if (m_bRemote) {
            if (!avio_interrupt_cb()) { // seek operation will interrupt avio
                SetEOS();
            }
        } else {
            SetEOS();
        }
        break;
    case AVERROR_EXIT:
        // caused by interrupt function
        break;
    default:
        break;
    }
    
    return FALSE;
}

inline
void CFFmpegDemuxer::DuplicatePacket(AVPacket* pTo, const AVPacket* pFrom)
{
    BYTE* pData = (BYTE*)align_malloc(pFrom->size + FF_INPUT_BUFFER_PADDING_SIZE, 16);
    
    memcpy(pData, pFrom->data, pFrom->size);
    memset(pData + pFrom->size, 0, FF_INPUT_BUFFER_PADDING_SIZE);
    memcpy(pTo, pFrom, sizeof(AVPacket));
    pTo->data = pData;
    
    if (pTo->pts == AV_NOPTS_VALUE && pTo->dts != AV_NOPTS_VALUE) {
        pTo->pts = pTo->dts;
    }
}

inline
BOOL CFFmpegDemuxer::FillPacketPool(AVPacket* pPktFrom)
{
    AssertValid(pPktFrom);
    CPacketPool* pPool = NULL;
    int nStreamID = pPktFrom->stream_index;

    if (m_format.bDecodeA) {
        for (int i = 0; i < m_format.tracksA.size(); ++i) {
            AudioTrack& refTrackA = m_format.tracksA[i];
            if (refTrackA.nStreamID == nStreamID) {
                pPool = m_PoolsA.GetPoolFromTrackID(i);
                FillAudioPacketPool(pPool, pPktFrom);
            }
        }
    }
    if (m_format.bDecodeV) {
        for (int i = 0; i < m_format.tracksV.size(); ++i) {
            VideoTrack& refTrackV = m_format.tracksV[i];
            if (refTrackV.nStreamID == nStreamID) {
                pPool = m_PoolsV.GetPoolFromTrackID(i);
                FillVideoPacketPool(pPool, pPktFrom);
            }
        }
    }
    if (m_format.bDecodeS) {
        for (int i = 0; i < m_format.tracksS.size(); ++i) {
            SubtitleTrack& refTrackS = m_format.tracksS[i];
            if (refTrackS.nStreamID == nStreamID) {
                pPool = m_PoolsS.GetPoolFromTrackID(i);
                FillSubtitlePacketPool(pPool, pPktFrom);
            }
        }
    }
    
    av_free_packet(pPktFrom);
    
    return TRUE;
}

inline
void CFFmpegDemuxer::FillVideoPacketPool(CPacketPool* pPool, AVPacket* pPktFrom)
{
    AVPacket packet;
    CMediaSample sample;
    VideoTrack* pTrackV = m_format.GetCurVideoTrack();
 
    AssertValid(pPool);
    DuplicatePacket(&packet, pPktFrom);
    
    if (pPool->GetSize() == VIDEO_POOL_VOLUME && pPool != m_PoolsV.GetCurPool()) {
        pPool->GetUnused(sample);
        pPool->Recycle(sample);
    }
    pPool->GetEmpty(sample); // no need to check here
    
    sample.m_Type = SAMPLE_PACKET;
    memcpy(sample.m_pBuf, &packet, sizeof(AVPacket));
    sample.m_pSpecs = pTrackV;
    sample.m_pExten = pTrackV->pCodecCtx;
    sample.m_llTimestamp = packet.pts != AV_NOPTS_VALUE ? packet.pts : m_llLastVideoTS + packet.duration;
    if (FFABS(sample.m_llTimestamp - m_llLastVideoTS) > m_llDisconThreshold) {
        if (m_llLastVideoTS != AV_NOPTS_VALUE) {
            sample.m_bDiscon = TRUE;
            m_llSyncPoint = sample.m_llTimestamp;
        }
    }
    sample.m_llSyncPoint = m_llSyncPoint;
    sample.m_bIgnore = m_fOffset - (sample.m_llTimestamp - m_llStartTime) * pTrackV->fTimebase > m_nJumpLimit;

    m_llLastVideoTS = sample.m_llTimestamp;
    //Log("video pts: %lld, syncpt: %lld, ignore: %d\n", sample.m_llTimestamp, sample.m_llSyncPoint, sample.m_bIgnore);
    pPool->Commit(sample);
}

inline
void CFFmpegDemuxer::FillAudioPacketPool(CPacketPool* pPool, AVPacket* pPktFrom)
{
    AVPacket packet;
    CMediaSample sample;
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    
    AssertValid(pPool);
    DuplicatePacket(&packet, pPktFrom);
    
    if (pPool->GetSize() == AUDIO_POOL_VOLUME && pPool != m_PoolsA.GetCurPool()) {
        pPool->GetUnused(sample);
        pPool->Recycle(sample);
    }
    pPool->GetEmpty(sample);
    
    sample.m_Type = SAMPLE_PACKET;
    if (packet.duration == 0) packet.duration = AUDIO_FRAME_DURATION / pTrackA->fTimebase;
    memcpy(sample.m_pBuf, &packet, sizeof(AVPacket));
    sample.m_pSpecs = pTrackA;
    sample.m_pExten = pTrackA->pCodecCtx;
    sample.m_llTimestamp = packet.pts != AV_NOPTS_VALUE ? packet.pts : m_llLastAudioTS + packet.duration;
    sample.m_llSyncPoint = m_format.bDecodeV ? m_llSyncPoint * m_fConvertA : m_llSyncPoint;
    float fStartTime = m_format.bDecodeV ? m_llStartTime * m_fConvertA : m_llStartTime;
    sample.m_bIgnore = m_fOffset - (sample.m_llTimestamp - fStartTime) * pTrackA->fTimebase > m_nJumpLimit;
    
    m_llLastAudioTS = sample.m_llTimestamp;
    //Log("audio pts: %lld, syncpt: %lld, ignore: %d\n", sample.m_llTimestamp, sample.m_llSyncPoint, sample.m_bIgnore);
    pPool->Commit(sample);
}

inline
void CFFmpegDemuxer::FillSubtitlePacketPool(CPacketPool* pPool, AVPacket* pPktFrom)
{
    AVPacket packet;
    CMediaSample sample;
    SubtitleTrack* pTrackS = m_format.GetCurSubtitleTrack();
    
    AssertValid(pPool);
    DuplicatePacket(&packet, pPktFrom);
    
    if (pPool->GetSize() == SUBTITLE_POOL_VOLUME && pPool != m_PoolsS.GetCurPool()) {
        pPool->GetUnused(sample);
        pPool->Recycle(sample);
    }
    pPool->GetEmpty(sample);
    
    sample.m_Type = SAMPLE_PACKET;
    memcpy(sample.m_pBuf, &packet, sizeof(AVPacket));
    sample.m_pSpecs = pTrackS;
    sample.m_pExten = pTrackS->pCodecCtx;
    sample.m_llTimestamp = packet.pts;
    sample.m_llSyncPoint = m_llSyncPoint * m_fConvertS;
    sample.m_bIgnore = FALSE;
    
    m_llLastSubtitleTS = sample.m_llTimestamp;
    //Log("subtitle pts: %lld, syncpt: %lld, ignore: %d\n", sample.m_llTimestamp, sample.m_llSyncPoint, sample.m_bIgnore);
    pPool->Commit(sample);
}

THREAD_RETURN CFFmpegDemuxer::ThreadProc()
{
    int nWait = 0;
    AVPacket packet;
   
    while (m_bRun) {
        m_sync.Wait();
        
        if (m_bEOS) {
            m_sync.Signal(); Sleep(50);
            continue;
        }
        
        int nAudioSize = m_PoolsA.GetCurPoolSize(), nVideoSize = m_PoolsV.GetCurPoolSize();
        // the subtitle pool is big enough and should never be full, so don't care about it
        if (nAudioSize < AUDIO_POOL_VOLUME && nVideoSize < VIDEO_POOL_VOLUME) {
            if (ReadPacket(m_format.pFmtCtx, &packet)) {
                FillPacketPool(&packet);
                nWait = 0;
            } else {
                nWait = 50;
            }
        } else if (nAudioSize == 0 && nVideoSize == VIDEO_POOL_VOLUME) {
            if (m_format.bDecodeA && m_bDiscard) {
                int nCount = VIDEO_POOL_VOLUME * 0.95;
                Dispatch(GUID_VIDEO_DECODER, DISPATCH_DISCARD_PACKETS, &nCount);
                nWait = 5;
            } else {
                nWait = 30;
            }
        } else {
            nWait = 30; // wait
        }
        
        m_sync.Signal();
        
        Sleep(nWait);
    }
    
    return 0;
}

int CFFmpegDemuxer::Load()
{
    Log("CFFmpegDemuxer::Load\n");
    maintain_avio();

    if (avformat_open_input(&m_format.pFmtCtx, m_strURL.c_str(), NULL, NULL) != 0) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_IO, 0, NULL);
        return E_FAIL;
    }
    if (avformat_find_stream_info(m_format.pFmtCtx, NULL) < 0) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADSTREAM, 0, NULL);
        return E_FAIL;
    }
    if (!PrepareCodecs(m_format.pFmtCtx)) { // audio/video codec都找不到
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_NOCODECS, 0, NULL);
        return E_FAIL;
    }
    if (!PrepareStreamData(m_format.pFmtCtx)) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADSTREAM, 0, NULL);
        return E_FAIL;
    }

    BOOL bSupport = TRUE;
    NotifyEvent(EVENT_CHECK_DEVICE, 0, 0, &bSupport);
    if (!bSupport) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_UNSUPPORTED, 0, NULL);
        return E_FAIL;
    }

    UpdateSyncPoint(0);
    m_llStartTime = m_llSyncPoint; // first key frame timestamp
    LONGLONG llDuration = m_format.bDecodeV ? 
        m_format.GetCurVideoTrack()->llDuration : m_format.GetCurAudioTrack()->llDuration;
    if (m_fOffset > 0 && m_fOffset <= llDuration / AV_TIME_BASE) {
        Seek(m_fOffset);
    }
    
    Create();
    m_sync.Signal();

    CMediaObject::Load();
    return S_OK;
}

int CFFmpegDemuxer::WaitForResources(BOOL bWait)
{
    Log("CFFmpegDemuxer::WaitForResources\n");
    CMediaObject::WaitForResources(bWait);
    

    return S_OK;
}

int CFFmpegDemuxer::Idle()
{
    Log("CFFmpegDemuxer::Idle\n");
    Start();
    
    
    CMediaObject::Idle();
    return S_OK;
}

int CFFmpegDemuxer::Execute()
{
    Log("CFFmpegDemuxer::Execute\n");
    // TODO:
    
    CMediaObject::Execute();
    return S_OK;
}

int CFFmpegDemuxer::Pause()
{
    Log("CFFmpegDemuxer::Pause\n");
    // TODO:
    
    CMediaObject::Pause();
    return S_OK;
}

int CFFmpegDemuxer::BeginFlush()
{
    Log("CFFmpegDemuxer::BeginFlush\n");
    CMediaObject::BeginFlush();
    
    m_sync.Wait();
    
    return S_OK;
}

int CFFmpegDemuxer::EndFlush()
{
    Log("CFFmpegDemuxer::EndFlush\n");
    m_PoolsA.Flush();
    m_PoolsV.Flush();
    m_PoolsS.Flush();
    maintain_avio();
    Seek(m_fOffset);
    //Log("seek end\n");
    m_sync.Signal();
    
    return CMediaObject::EndFlush();
}

int CFFmpegDemuxer::Invalid()
{
    Log("CFFmpegDemuxer::Invalid\n");
    CMediaObject::Invalid();

    Close();
    
    return S_OK;
}

int CFFmpegDemuxer::Unload()
{
    Log("CFFmpegDemuxer::Unload\n");
    Close();
    
//    av_set_notify_cb(AV_NOTIFY_RECONNECT, NULL, &m_format);
    
    m_llStartTime      = AV_NOPTS_VALUE;
    m_llLastAudioTS    = AV_NOPTS_VALUE;
    m_llLastVideoTS    = AV_NOPTS_VALUE;
    m_llLastSubtitleTS = AV_NOPTS_VALUE;

    m_bDiscard = FALSE;
    
    CMediaObject::Unload();
    return S_OK;
}

int CFFmpegDemuxer::Release()
{
    for (int i = 0; i < m_format.tracksV.size(); ++i) {
        if (m_format.tracksV[i].pCodecCtx) {
            avcodec_close(m_format.tracksV[i].pCodecCtx);
        }
    }
    for (int i = 0; i < m_format.tracksA.size(); ++i) {
        if (m_format.tracksA[i].pCodecCtx) {
            avcodec_close(m_format.tracksA[i].pCodecCtx);
        }
    }
    for (int i = 0; i < m_format.tracksS.size(); ++i) {
        if (m_format.tracksS[i].pCodecCtx) {
            avcodec_close(m_format.tracksS[i].pCodecCtx);
        }
    }
    if (m_format.pFmtCtx) {
        avformat_close_input(&m_format.pFmtCtx);
    }
    
    m_format.Clear();
    m_PoolsA.Clear();
    m_PoolsV.Clear();
    m_PoolsS.Clear();
    
    return S_OK;
}

int CFFmpegDemuxer::SetEOS()
{
    CMediaObject::SetEOS();

    
    return S_OK;
}

int CFFmpegDemuxer::Seek(float fOffset)
{
    VideoTrack* pTrackV = m_format.GetCurVideoTrack(); 
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    float fTimebase = m_format.bDecodeV ? pTrackV->fTimebase : pTrackA->fTimebase;
    int nStreamID = m_format.bDecodeV ? pTrackV->nStreamID : pTrackA->nStreamID;
    AVStream* pStream = m_format.pFmtCtx->streams[nStreamID];

    int64_t llStartTS;
    for (int i = 0; i < pStream->nb_index_entries; ++i) {
        llStartTS = pStream->index_entries ? pStream->index_entries[i].timestamp : 0;
        if (llStartTS != AV_NOPTS_VALUE) break;
    }
    if (pStream->nb_index_entries == 0) {
        llStartTS = pStream->index_entries ? pStream->index_entries[0].timestamp : 0;
    }
    int64_t llTargetStart = (fOffset + llStartTS * fTimebase) * AV_TIME_BASE;
    int64_t llSeekStart = av_rescale_q(llTargetStart, AV_TIME_BASE_Q, pStream->time_base);
    avformat_seek_file(m_format.pFmtCtx, m_format.bDecodeV ? pTrackV->nStreamID : pTrackA->nStreamID, INT64_MIN, llSeekStart, INT64_MAX, AVSEEK_FLAG_BACKWARD);
    if (m_format.bDecodeA) {
        for (int i = 0; i < m_format.tracksA.size(); ++i) {
            if (m_format.tracksA[i].pCodecCtx) {
                avcodec_flush_buffers(m_format.tracksA[i].pCodecCtx);
            }
        }
    }
    if (m_format.bDecodeV) {
        for (int i = 0; i < m_format.tracksV.size(); ++i) {
            if (m_format.tracksV[i].pCodecCtx) {
                avcodec_flush_buffers(m_format.tracksV[i].pCodecCtx);
            }
        }
    }
    if (m_format.bDecodeS) {
        for (int i = 0; i < m_format.tracksS.size(); ++i) {
            if (m_format.tracksS[i].pCodecCtx) {
                avcodec_flush_buffers(m_format.tracksS[i].pCodecCtx);
            }
        }
    }

    UpdateSyncPoint2(llSeekStart);
    
    m_llLastAudioTS = m_format.bDecodeV && pTrackA ? 
        FFMAX(m_llSyncPoint * pTrackV->fTimebase / pTrackA->fTimebase, 0) : FFMAX(m_llSyncPoint, 0);
    m_llLastVideoTS = FFMAX(m_llSyncPoint, 0);
    m_llLastSubtitleTS = AV_NOPTS_VALUE;
    Log("m_llLastAudioTS = %lld\n", m_llLastAudioTS);
    Log("m_llLastVideoTS = %lld\n", m_llLastVideoTS);
    
    return S_OK;
}

int CFFmpegDemuxer::GetOutputPool(const GUID& requestor, ISamplePool** ppPool)
{
    AssertValid(ppPool);
    
    if (requestor == GUID_AUDIO_DECODER) {
        *ppPool = m_PoolsA.GetCurPool();
    } else if (requestor == GUID_VIDEO_DECODER) {
        *ppPool = m_PoolsV.GetCurPool();
    } else if (requestor == GUID_SUBTITLE_DECODER) {
        *ppPool = m_PoolsS.GetCurPool();
    } else {
        *ppPool = NULL;
    }
    
    return S_OK;
}

void CFFmpegDemuxer::UpdateSyncPoint(LONGLONG llTime)
{
    VideoTrack* pTrackV = m_format.GetCurVideoTrack(); 
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    float fTimebase = m_format.bDecodeV ? pTrackV->fTimebase : pTrackA->fTimebase;
    int nStreamIdx = m_format.bDecodeV ? pTrackV->nStreamID : pTrackA->nStreamID;
    AVStream* pStream = m_format.pFmtCtx->streams[nStreamIdx];
    if (!pStream) {
        return;
    }
    
    int64_t llStartTS = pStream->index_entries ? pStream->index_entries[0].timestamp : 0;
    int64_t llTargetStart = (llTime + llStartTS * fTimebase) * AV_TIME_BASE;
    int64_t llStartTime = av_rescale_q(llTargetStart, AV_TIME_BASE_Q, pStream->time_base);
    
    UpdateSyncPoint2(llStartTime);
}

void CFFmpegDemuxer::UpdateSyncPoint2(LONGLONG llTime)
{
    AVFormatContext* pFmtCtx = m_format.pFmtCtx;
    VideoTrack* pTrackV = m_format.GetCurVideoTrack(); 
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    AVStream* pStream = pFmtCtx->streams[m_format.bDecodeV ? pTrackV->nStreamID : pTrackA->nStreamID];
    
    int index = av_index_search_timestamp(pStream, llTime, AVSEEK_FLAG_BACKWARD); 
    if (index == -1) {
        index = av_index_search_timestamp(pStream, llTime, AVSEEK_FLAG_ANY);
    }
    index = FFMAX(index, 0);
    AVIndexEntry* pEntry = &pStream->index_entries[index];
    //Log("index = %d, pEntry = %x\n", index, pEntry);
    
    if (pEntry == NULL) {
        m_llSyncPoint = 0; // assumes that the first pts == 0, fixme
    } else if (pEntry->timestamp <= llTime || pEntry->pos == pEntry->min_distance){
        m_llSyncPoint = pStream->index_entries[index].timestamp;
    } else {
        m_llSyncPoint = pStream->index_entries[0].timestamp;
    }
    
    if (m_llStartTime == AV_NOPTS_VALUE) { // first time
        return;
    }
    
    float fTimebase = m_format.bDecodeV ? pTrackV->fTimebase : pTrackA->fTimebase;
    m_nJumpLimit = m_bRemote ? INT32_MAX : m_nJumpLimit;
    m_fJumpBack  = m_fOffset - (m_llSyncPoint - m_llStartTime) * fTimebase;
    AssertValid(m_fJumpBack >= 0);
    m_llSyncPoint = m_fJumpBack <= m_nJumpLimit ? 
        m_llSyncPoint : (m_fOffset - m_nJumpLimit) / fTimebase + m_llStartTime;
}

BOOL CFFmpegDemuxer::PrepareCodecs(AVFormatContext* pFmtCtx)
{
    AVStream** pStreams = pFmtCtx->streams;
    AudioTracks&    refTracksA = m_format.tracksA;
    VideoTracks&    refTracksV = m_format.tracksV;
    SubtitleTracks& refTracksS = m_format.tracksS;
    AssertValid(!m_format.nCurTrackV && !m_format.nCurTrackA && !m_format.nCurTrackS);
    
    for (int i = 0; i < pFmtCtx->nb_streams; ++i) {
        if (!pStreams[i] || !pStreams[i]->codec) { continue; }
        
        AVCodec* pCodec = avcodec_find_decoder(pStreams[i]->codec->codec_id);
        if (!pCodec) { continue; }
        
        AssertValid(pCodec && pStreams[i]->codec);
        AVMediaType nCodecType = pStreams[i]->codec->codec_type;
        if (AVMEDIA_TYPE_AUDIO == nCodecType) {
            pStreams[i]->codec->request_channel_layout = av_get_default_channel_layout(
                    pStreams[i]->codec->channels > 0 ? FFMIN(2, pStreams[i]->codec->channels) : 2);
            pStreams[i]->codec->request_sample_fmt = AV_SAMPLE_FMT_S16;
            if (avcodec_open2(pStreams[i]->codec, pCodec, NULL) < 0)
                continue;
            pStreams[i]->codec->codec = pCodec;
            
            AudioTrack audio;
            audio.pCodec    = pCodec;
            audio.pCodecCtx = pStreams[i]->codec;
            audio.nStreamID = i;
            refTracksA.push_back(audio); ++m_format.nCurTrackA;
        } else if (AVMEDIA_TYPE_VIDEO == nCodecType) {
            if (avcodec_open2(pStreams[i]->codec, pCodec, NULL) < 0)
                continue;
            pStreams[i]->codec->skip_loop_filter = AVDISCARD_ALL;
            
            VideoTrack video;
            video.pCodec    = pCodec;
            video.pCodecCtx = pStreams[i]->codec;
            video.nStreamID = i;
            refTracksV.push_back(video); ++m_format.nCurTrackV;
        } else if (AVMEDIA_TYPE_SUBTITLE == nCodecType) {
            if (avcodec_open2(pStreams[i]->codec, pCodec, NULL) < 0)
                continue;
            
            SubtitleTrack subtitle;
            subtitle.pCodec    = pCodec;
            subtitle.pCodecCtx = pStreams[i]->codec;
            subtitle.nStreamID = i;
            refTracksS.push_back(subtitle); ++m_format.nCurTrackS;
        }
    }
    m_format.nCurTrackA = FFMAX(m_format.nCurTrackA - 1, 0);
    m_format.nCurTrackV = FFMAX(m_format.nCurTrackV - 1, 0);
    m_format.nCurTrackS = FFMAX(m_format.nCurTrackS - 1, 0);
    
    AudioTrack* pTrackA = m_format.GetCurAudioTrack();
    VideoTrack* pTrackV = m_format.GetCurVideoTrack();
    if (!pTrackA && !pTrackV) { // can NOT find both a/v codecs
        return FALSE;
    }
    
    return TRUE;
}

BOOL CFFmpegDemuxer::PrepareStreamData(AVFormatContext* pFmtCtx)
{
    m_format.bDecodeA = PrepareAudioData(pFmtCtx);
    m_format.bDecodeV = PrepareVideoData(pFmtCtx);
    m_format.bDecodeS = PrepareSubtitleData(pFmtCtx);
    m_format.bDecodeS = m_format.bDecodeS && m_format.bDecodeV;
    
    VideoTrack*    pTrackV = m_format.GetCurVideoTrack();
    AudioTrack*    pTrackA = m_format.GetCurAudioTrack();
    SubtitleTrack* pTrackS = m_format.GetCurSubtitleTrack();
    
    if (m_format.bDecodeA && !m_format.bDecodeV) {
        m_llDisconThreshold = TS_JUMP_THRESHOLD / pTrackA->fTimebase;
        NotifyEvent(EVENT_AUDIO_ONLY, 0, 0, NULL);
    } else if (m_format.bDecodeV && !m_format.bDecodeA) {
        m_llDisconThreshold = TS_JUMP_THRESHOLD / pTrackV->fTimebase;
        NotifyEvent(EVENT_VIDEO_ONLY, 0, 0, NULL);
    } else if (m_format.bDecodeA && m_format.bDecodeV) {
        m_llDisconThreshold = TS_JUMP_THRESHOLD / pTrackV->fTimebase;
        m_fConvertA = pTrackV->fTimebase / pTrackA->fTimebase;
    } else {
        return FALSE;
    }
    
    if (m_format.bDecodeS) {
        m_fConvertS = pTrackV->fTimebase / pTrackS->fTimebase;
        // TODO: notify the subtitle count and information
    }
    if (m_format.bDecodeA) {
    	NotifyEvent(EVENT_CREATE_AUDIO, 0, 0, NULL);
        // TODO: notify the audio track count and information
    }
    if (m_format.bDecodeV) {
        NotifyEvent(EVENT_CREATE_VIDEO, 0, 0, NULL);
    }
    
    return TRUE;
}

BOOL CFFmpegDemuxer::PrepareVideoData(AVFormatContext* pFmtCtx)
{
    VideoTrack* pTrackV = NULL;
    
    if (m_format.tracksV.size() == 0) { 
        return FALSE; 
    }
    
    POOL_PROPERTIES request, actual;
    for (int i = 0; i < m_format.tracksV.size(); ++i) {
        pTrackV = &m_format.tracksV[i];
        
        // the validation of the following values is ensured in PrepareCodecs
        AVCodecContext* pCodecCtx = pTrackV->pCodecCtx;
        AVStream* pStream = pFmtCtx->streams[pTrackV->nStreamID];
        
        pTrackV->nWidth     = pCodecCtx->width;
        pTrackV->nHeight    = pCodecCtx->height;
        pTrackV->nCodecID   = pCodecCtx->codec_id;
        pTrackV->nBitrate   = pCodecCtx->bit_rate;
        pTrackV->llDuration = pFmtCtx->duration;
        if (pStream->r_frame_rate.den) {
            pTrackV->fFPS = (float)pStream->r_frame_rate.num / pStream->r_frame_rate.den;
        }
        if (pStream->time_base.num) {
            pTrackV->fTimebase = pStream->time_base.num / (float)pStream->time_base.den;
        }
        
        CPacketPool* pPool = new CPacketPool();
        request.nSize  = sizeof(AVPacket);
        request.nCount = VIDEO_POOL_VOLUME;
        pPool->SetProperties(&request, &actual); 
        m_PoolsV.Add(i, pPool);
    }
    AssertValid(m_format.nCurTrackV < m_format.tracksV.size());
    m_PoolsV.SetCurPool(m_format.nCurTrackV);
    
    if (!m_bRemote) {
        AudioTrack* pTrackA = m_format.GetCurAudioTrack();
        VideoTrack* pTrackV = m_format.GetCurVideoTrack();
        RebuildIndexEntries(pFmtCtx, pTrackV->pCodecCtx, pTrackA ? pTrackA->pCodecCtx : NULL);
    }
    int nDuration = FFMAX(pTrackV->llDuration / AV_TIME_BASE, 0);
    m_nJumpLimit = nDuration <= 60 ? 2 : nDuration <= 270 ? 4 : INT32_MAX;
 
    return TRUE;
}

int CFFmpegDemuxer::RebuildIndexEntries(AVFormatContext* pFmtCtx, AVCodecContext* pVideoCtx, AVCodecContext* pAudioCtx)
{
    VideoTrack* pTrackV = m_format.GetCurVideoTrack();
    AVStream* pVideoStream = pFmtCtx->streams[pTrackV->nStreamID];
    AVInputFormat* pInputFormat = pFmtCtx->iformat;
    BOOL bAddIndex = FALSE;
    int nTotalFrames = 0, nStartFrame = 0, nEndFrame = 0;
    float fVideoDuration = 0;
    
    // flv files do not have and need index
    if (strcmp(pInputFormat->name, "flv") == 0) {
        return E_FAIL;
    }
    if (strcmp(pInputFormat->name, "qmv") == 0) {
        return E_FAIL;
    }
    if (strcmp(pInputFormat->name, "matroska,webm") == 0) {
        av_seek_frame(pFmtCtx, pTrackV->nStreamID, 0, AVSEEK_FLAG_ANY);
    }
    // file types that index should be added by ourselves
    if (strcmp(pInputFormat->name, "asf") == 0) {
        bAddIndex = TRUE;
    } else if (strcmp(pInputFormat->name, "rm") == 0) { // adjust rm files' wrong media duration if needed
        bAddIndex = FALSE; // do nothing
    } 
    
    // scan the whole file if index is in need
    if (pVideoStream->nb_index_entries <= REBUILD_INDEX_THRESHOLD) {
        AVPacket pkt;
        int64_t llStartTS = AV_NOPTS_VALUE, llEndTS = AV_NOPTS_VALUE;
        
        while (!url_feof(pFmtCtx->pb)) {
            if (av_read_frame(pFmtCtx, &pkt) < 0 || avio_interrupt_cb())
                break;
            if (pkt.stream_index == pTrackV->nStreamID) { // handle video stream is enough now
                if (pkt.pos > 0) {
                    ++nTotalFrames; // used for adjusting media duration
                    if (llStartTS == AV_NOPTS_VALUE) {
                        // assume the first available pts or dts does not have time jump
                        if (pkt.pts != AV_NOPTS_VALUE) { 
                            llStartTS = pkt.pts; 
                            nStartFrame = nTotalFrames;
                        } else if (pkt.dts != AV_NOPTS_VALUE) {
                            llStartTS = pkt.dts;
                            nStartFrame = nTotalFrames;
                        }
                    }
                    if (pkt.pts != AV_NOPTS_VALUE) { 
                        llEndTS = pkt.pts; 
                        nEndFrame = nTotalFrames;
                    } else if (pkt.dts != AV_NOPTS_VALUE) {
                        llEndTS = pkt.dts;
                        nEndFrame = nTotalFrames;
                    }
                }
                if (bAddIndex) {
                    LONGLONG llTS = (pkt.pts != AV_NOPTS_VALUE) ? pkt.pts : pkt.dts;
                    av_add_index_entry(pFmtCtx->streams[pTrackV->nStreamID], pkt.pos, llTS,
                                       0, 0, pkt.flags & AV_PKT_FLAG_KEY ? AVINDEX_KEYFRAME : 0);
                } 
            }
            av_free_packet(&pkt);
        }
        
        // correct wrong fps or media duration if it is available
        if (llStartTS != AV_NOPTS_VALUE && llEndTS != AV_NOPTS_VALUE && nEndFrame > nStartFrame) {
            float fFPS = (nEndFrame - nStartFrame) / ((llEndTS - llStartTS) * pTrackV->fTimebase);
            if (fFPS > 0 && FFABS(fFPS - pTrackV->fFPS) >= 2) {
                pTrackV->fFPS = fFPS;
            }
            
            fVideoDuration = nTotalFrames / pTrackV->fFPS;
            fVideoDuration = FFMAX(fVideoDuration, 0);
            if (fVideoDuration > 0 && FFABS(fVideoDuration - (float)pTrackV->llDuration / AV_TIME_BASE) >= 8) {
                pTrackV->llDuration = fVideoDuration * AV_TIME_BASE;
            }
        }
        // return to the start position at last
        av_seek_frame(pFmtCtx, pTrackV->nStreamID, FFMAX(llStartTS, 0), AVSEEK_FLAG_ANY);
    } else { // if we have enough index data, adjust wrong media duration only
        AVIndexEntry* pVideoIE = pVideoStream->index_entries;
        int n = pVideoStream->nb_index_entries - 1;
        if (pVideoIE) {
            int64_t llMaxTS, llMinTS;
            int nSkipFrame = 0;
            
            while (n >= 0) {
                if ((llMaxTS = pVideoIE[n--].timestamp) != AV_NOPTS_VALUE) {
                    break;
                }
                ++nSkipFrame;
            }
            n = 0;
            while (n < pVideoStream->nb_index_entries) {
                if ((llMinTS = pVideoIE[n++].timestamp) != AV_NOPTS_VALUE) {
                    break;
                }
                ++nSkipFrame;
            }
            fVideoDuration = (llMaxTS - llMinTS) * pTrackV->fTimebase + (1000 / pTrackV->fFPS) * nSkipFrame * 0.001;
            fVideoDuration = FFMAX(fVideoDuration, 0);
        }
        
        if (fVideoDuration > 0 && FFABS(fVideoDuration - (float)pTrackV->llDuration / AV_TIME_BASE) >= 8) {
            int64_t llFileSize = avio_size(pFmtCtx->pb);
            if (pVideoCtx->bit_rate > 0 && pVideoCtx->bit_rate < MAX_BIT_RATE) { 
                int nBitrate = pVideoCtx->bit_rate + (m_format.bDecodeA ? pAudioCtx->bit_rate : 0);
                if (FFABS((nBitrate >> 3) * fVideoDuration - llFileSize) < llFileSize * 0.2) {
                    pTrackV->llDuration = fVideoDuration * AV_TIME_BASE;
                } 
            } 
        }
    }
    
    return S_OK;
}

BOOL CFFmpegDemuxer::PrepareAudioData(AVFormatContext* pFmtCtx)
{
    AudioTrack* pTrackA = NULL;
    
    if (m_format.tracksA.size() == 0) { 
        return FALSE; 
    }
    
    POOL_PROPERTIES request, actual;
    for (int i = 0; i < m_format.tracksA.size(); ++i) {
        pTrackA = &m_format.tracksA[i];
 
        // the validation of the following values is ensured in PrepareCodecs
        AVCodecContext* pCodecCtx = pTrackA->pCodecCtx;
        AVStream* pStream = pFmtCtx->streams[pTrackA->nStreamID];
        
        pTrackA->nSampleRate       = pCodecCtx->sample_rate;
        pTrackA->nCodecID		   = pCodecCtx->codec_id;
        pTrackA->nChannelsPerFrame = pCodecCtx->channels;
        pTrackA->nFramesPerPacket  = pCodecCtx->frame_size;
        pTrackA->nBitrate	       = pCodecCtx->bit_rate;
        pTrackA->nSampleFormat     = pCodecCtx->sample_fmt;
        pTrackA->llDuration        = pFmtCtx->duration;
        if (pStream->time_base.den) {
            pTrackA->fTimebase = (float)pStream->time_base.num / pStream->time_base.den;
        }
        
        AVDictionaryEntry* pEntry = NULL;
        pEntry = av_dict_get(pFmtCtx->metadata, "artist", NULL, 0);
        if (pEntry) { strcpy(pTrackA->szArtist, pEntry->value); }
        pEntry = av_dict_get(pFmtCtx->metadata, "title", NULL, 0);
        if (pEntry) { strcpy(pTrackA->szTitle, pEntry->value);  }
        pEntry = av_dict_get(pFmtCtx->metadata, "album", NULL, 0);
        if (pEntry) { strcpy(pTrackA->szAlbum, pEntry->value);  }
        
        CPacketPool* pPool = new CPacketPool();
        request.nSize  = sizeof(AVPacket);
        request.nCount = AUDIO_POOL_VOLUME;
        pPool->SetProperties(&request, &actual); 
        m_PoolsA.Add(i, pPool);
    }
    AssertValid(m_format.nCurTrackA < m_format.tracksA.size());
    m_PoolsA.SetCurPool(m_format.nCurTrackA);

    return TRUE;
}

BOOL CFFmpegDemuxer::PrepareSubtitleData(AVFormatContext* pFmtCtx)
{
    SubtitleTrack* pTrackS = NULL;
    
    if (m_format.tracksS.size() == 0) { 
        return FALSE; 
    }
    
    POOL_PROPERTIES request, actual;
    for (int i = 0; i < m_format.tracksS.size(); ++i) {
        pTrackS = &m_format.tracksS[i];
        
        // the validation of the following values is ensured in PrepareCodecs
        AVCodecContext* pCodecCtx = pTrackS->pCodecCtx;
        AVStream* pStream = pFmtCtx->streams[pTrackS->nStreamID];
        
        pTrackS->nCodecID = pCodecCtx->codec_id;
        if (pStream->time_base.den) {
            pTrackS->fTimebase = (float)pStream->time_base.num / pStream->time_base.den;
        }
        
        CPacketPool* pPool = new CPacketPool();
        request.nSize  = sizeof(AVPacket);
        request.nCount = SUBTITLE_POOL_VOLUME;
        pPool->SetProperties(&request, &actual); 
        m_PoolsS.Add(i, pPool);
    }
    AssertValid(m_format.nCurTrackS < m_format.tracksS.size());
    m_PoolsS.SetCurPool(m_format.nCurTrackS);
    
    return TRUE;
}

