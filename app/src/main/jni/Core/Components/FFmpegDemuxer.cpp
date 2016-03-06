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

#define REBUILD_INDEX_THRESHOLD 15
#define MAX_BIT_RATE    10000000
#define AUDIO_FRAME_DURATION    0.023 // second
#define TS_JUMP_THRESHOLD       10    // second

#define AUDIO_POOL_VOLUME       360
#define VIDEO_POOL_VOLUME       360

////////////////////////////////////////////////////////////

CFFmpegDemuxer::CFFmpegDemuxer(const GUID& guid, IDependency* pDepend, int* pResult)
    : CSource(guid, pDepend), m_lfOffset(0), m_bRemote(FALSE), m_nJumpLimit(3), 
      m_lfJumpBack(0), m_llStartTime(AV_NOPTS_VALUE), m_bDiscard(FALSE)
{
    m_llLastAudioTS = AV_NOPTS_VALUE;
    m_llLastVideoTS = AV_NOPTS_VALUE;
    
    POOL_PROPERTIES request, actual;
    request.nSize  = sizeof(AVPacket);
    request.nCount = VIDEO_POOL_VOLUME;
    m_VideoPool.SetProperties(&request, &actual);
    request.nSize  = sizeof(AVPacket);
    request.nCount = AUDIO_POOL_VOLUME;
    m_AudioPool.SetProperties(&request, &actual);
    
    memset(&m_video,  0, sizeof(VideoInfo));
    memset(&m_audio,  0, sizeof(AudioInfo));
    memset(&m_format, 0, sizeof(FormatInfo));

    avcodec_register_all();
    av_register_all();

//    avio_set_remoteprobe_cb(avio_is_remote);
//    avio_set_interrupt_cb(avio_interrupt_cb);
//    av_set_notify_cb(AV_NOTIFY_SEEK_POSITION, notify_seek_pos_cb, this);
//    av_set_notify_cb(AV_NOTIFY_BUFFER_SIZE, notify_buf_size_cb, this);
//    av_set_notify_cb(AV_NOTIFY_READ_INDEX, notify_read_index_cb, this);
}

CFFmpegDemuxer::~CFFmpegDemuxer()
{
    ReleaseResources();
}

// IFFmpegDemuxer
int CFFmpegDemuxer::InitialConfig(const char* szURL, double lfOffset, BOOL bRemote)
{
    if (strlen(szURL) <= 0) 
        return E_FAIL;
    
    m_strURL   = szURL;
    m_lfOffset = lfOffset;
    m_bRemote  = bRemote;
    
//    avio_set_remote(bRemote);
//    av_set_notify_cb(AV_NOTIFY_RECONNECT, m_bRemote ? notify_reconnect_cb : NULL, &m_format);
    
    return S_OK;
}

int CFFmpegDemuxer::ConnectedPeerNeedData(int nPeerType, BOOL bNeedData)
{
    if (nPeerType == CONNECTION_PEER_AUDIO) {
        m_bDiscard = bNeedData;
    }
    
    return S_OK;
}

int CFFmpegDemuxer::SetSeekPosition(double lfOffset)
{
    m_lfOffset = lfOffset;
    
    return S_OK;
}

int CFFmpegDemuxer::GetMediaDuration(double* pDuration)
{
    AssertValid(pDuration);
    double lfDuration = (double)m_video.llFormatDuration / AV_TIME_BASE;
    *pDuration = lfDuration;
    
//    if (lfDuration - *pDuration >= 0.5) {
//        *pDuration += 1;
//    }
    
    return S_OK;
}

int CFFmpegDemuxer::GetMediaStartTime(double* pTime)
{
    AssertValid(pTime);
    *pTime = m_llStartTime;
    
    return S_OK;
}

int CFFmpegDemuxer::GetMediaBitrate(int* pBitrate)
{
    AssertValid(pBitrate);
    *pBitrate = m_video.nBitrate + m_audio.nBitrate;
    
    int64_t llFileSize = avio_size(m_format.pFormatContext->pb);
    double lfDuration = (double)m_video.llFormatDuration / AV_TIME_BASE;
    int nBitrate = llFileSize / lfDuration * 8;
    
    if (FFABS(nBitrate - *pBitrate) > 200000) {
        *pBitrate = nBitrate;
    }
    
    return S_OK;
}

int CFFmpegDemuxer::GetMediaFormatName(char *pName)
{
    AssertValid(pName);
    AVInputFormat* pInputFormat = m_format.pFormatContext->iformat;
    
    if (!pInputFormat) {
        return E_FAIL;
    }
    
    strcpy(pName, pInputFormat->name);
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioChannelCount(int* pCount)
{
    if (!m_format.bDecodeAudio) {
        return E_FAIL;
    }
    
    AssertValid(pCount);
    *pCount = m_audio.nChannelsPerFrame;
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioSampleFormat(int* pFormat)
{
    AssertValid(pFormat);
    if (!pFormat) {
        return E_FAIL;
    }
    
    *pFormat = m_audio.nSampleFormat;
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioTrackCount(int* pCount)
{
    if (!m_format.bDecodeAudio) {
        return E_FAIL;
    }
    
    AssertValid(pCount);
    *pCount = m_audio.nTrackCount;
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioTimebase(double* lfTimebase)
{
    if (!m_format.bDecodeAudio) {
        return E_FAIL;
    }
    
    AssertValid(lfTimebase);
    *lfTimebase = m_audio.lfTimebase;
    
    return S_OK;
}

int CFFmpegDemuxer::GetVideoTimebase(double* lfTimebase)
{
    if (!m_format.bDecodeVideo) {
        return E_FAIL;
    }
    
    AssertValid(lfTimebase);
    *lfTimebase = m_video.lfTimebase;
    
    return S_OK;
}

int CFFmpegDemuxer::GetCurAudioTrack(int* pTrack)
{
    if (!m_format.bDecodeAudio) {
        return E_FAIL;
    }
    
    AssertValid(pTrack);
    *pTrack = m_audio.nCurTrack;
    
    return S_OK;
}

int CFFmpegDemuxer::SetCurAudioTrack(int nTrack)
{
    if (!m_format.bDecodeAudio) {
        return E_FAIL;
    }
    
    m_audio.nCurTrack = nTrack;

    return S_OK;
}

int CFFmpegDemuxer::GetAudioSampleRate(double* pSampleRate)
{
    if (!m_format.bDecodeAudio) {
        return E_FAIL;
    }
    
    AssertValid(pSampleRate);
    *pSampleRate = m_audio.nSampleRate;
    
    return S_OK;
}

int CFFmpegDemuxer::GetAudioFormatID(int* pFormatID)
{
    if (!m_format.bDecodeAudio) {
        return E_FAIL;
    }
    
    AssertValid(pFormatID);
    *pFormatID = m_audio.nFormatID;
    
    return S_OK;
}

int CFFmpegDemuxer::GetVideoFormatID(int* pFormatID)
{
    if (!m_format.bDecodeVideo) {
        return E_FAIL;
    }
    
    AssertValid(pFormatID);
    *pFormatID = m_video.nFormatID;
    
    return S_OK;
}

int CFFmpegDemuxer::GetVideoFPS(int* pFPS)
{
    if (!m_format.bDecodeVideo) {
        return E_FAIL;
    }
    
    AssertValid(pFPS);
    *pFPS = m_video.lfFPS;
    
    return S_OK;
}

// IBufferingProbe
BOOL CFFmpegDemuxer::IsProbing()
{
    return TRUE;
}

BOOL CFFmpegDemuxer::IsNeedBuffering()
{
    if (m_bEOS) {
        return FALSE;
    }
    
    int nAudioSize = m_AudioPool.Size();
    int nVideoSize = m_VideoPool.Size();
    
    //Log("audio: %d, video: %d\n", nAudioSize, nVideoSize);
    if (nAudioSize <= 45 || nVideoSize <= 30) {
        if (nVideoSize >= 150 || nAudioSize >= 210) {
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

void CFFmpegDemuxer::DiscardPackets(int nCount)
{
    NotifyEvent(EVENT_DISCARD_VIDEO_PACKET, nCount, 0, NULL);
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
    
    if (pPktFrom->stream_index == m_format.nVideoStreamIdx) {
        if (m_format.bDecodeVideo) {
            FillVideoPacketPool(pPktFrom);
        }
    } else if (pPktFrom->stream_index == m_format.nAudioStreamIdx) {
        if (m_format.bDecodeAudio) {
            FillAudioPacketPool(pPktFrom);
        }
    } 
    
    av_free_packet(pPktFrom);
    
    return TRUE;
}

inline
void CFFmpegDemuxer::FillVideoPacketPool(AVPacket* pPktFrom)
{
    AVPacket packet;
    CMediaSample sample;
    
    DuplicatePacket(&packet, pPktFrom);
    m_VideoPool.GetEmpty(sample); // no need to check here
    
    sample.m_Type = SAMPLE_PACKET;
    memcpy(sample.m_pBuf, &packet, sizeof(AVPacket));
    sample.m_pSpecs = &m_video;
    sample.m_pExten = m_format.pVideoContext;
    sample.m_llTimestamp = packet.pts != AV_NOPTS_VALUE ? packet.pts : m_llLastVideoTS + packet.duration;
    if (FFABS(sample.m_llTimestamp - m_llLastVideoTS) > m_llDisconThreshold) {
        if (m_llLastVideoTS != AV_NOPTS_VALUE) {
            sample.m_bDiscon = TRUE;
            m_llSyncPoint = sample.m_llTimestamp;
        }
    }
    sample.m_llSyncPoint = m_llSyncPoint;
    sample.m_bIgnore = m_lfOffset - (sample.m_llTimestamp - m_llStartTime) * m_video.lfTimebase > m_nJumpLimit;

    m_llLastVideoTS = sample.m_llTimestamp;
    //Log("video pts: %lld, syncpt: %lld, ignore: %d\n", sample.m_llTimestamp, sample.m_llSyncPoint, sample.m_bIgnore);
    m_VideoPool.Commit(sample);
}

inline
void CFFmpegDemuxer::FillAudioPacketPool(AVPacket* pPktFrom)
{
    AVPacket packet;
    CMediaSample sample;
    
    DuplicatePacket(&packet, pPktFrom);
    m_AudioPool.GetEmpty(sample);
    
    sample.m_Type = SAMPLE_PACKET;
    if (packet.duration == 0) packet.duration = AUDIO_FRAME_DURATION / m_audio.lfTimebase;
    memcpy(sample.m_pBuf, &packet, sizeof(AVPacket));
    sample.m_pSpecs = &m_audio;
    sample.m_pExten = m_format.pAudioContext[m_audio.nCurTrack];
    sample.m_llTimestamp = packet.pts != AV_NOPTS_VALUE ? packet.pts : m_llLastAudioTS + packet.duration;
    sample.m_llSyncPoint = m_format.bDecodeVideo ? m_llSyncPoint * m_lfConvert : m_llSyncPoint;
    double llStartTime = m_format.bDecodeVideo ? m_llStartTime * m_lfConvert : m_llStartTime;
    sample.m_bIgnore = m_lfOffset - (sample.m_llTimestamp - llStartTime) * m_audio.lfTimebase > m_nJumpLimit;
    
    m_llLastAudioTS = sample.m_llTimestamp;
    //Log("audio pts: %lld, syncpt: %lld, ignore: %d %lf\n", sample.m_llTimestamp, sample.m_llSyncPoint, sample.m_bIgnore, m_audio.lfTimebase);
    m_AudioPool.Commit(sample);
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
        
        int nAudioSize = m_AudioPool.Size(), nVideoSize = m_VideoPool.Size();
        if (nAudioSize < AUDIO_POOL_VOLUME && nVideoSize < VIDEO_POOL_VOLUME) {
            if (ReadPacket(m_format.pFormatContext, &packet)) {
                FillPacketPool(&packet);
                nWait = 0;
            } else {
                nWait = 50;
            }
        } else if (nAudioSize == 0 && nVideoSize == VIDEO_POOL_VOLUME) {
            if (m_format.bDecodeAudio && m_bDiscard) {
                DiscardPackets(VIDEO_POOL_VOLUME * 0.9);
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

void CFFmpegDemuxer::ReleaseResources()
{
    if (m_format.pVideoContext) {
        avcodec_close(m_format.pVideoContext);
    }
    for (int i = 0; i < MAX_AUDIO_TRACKS; ++i) {
        if (m_format.pAudioContext[i]) {
            avcodec_close(m_format.pAudioContext[i]);
        }
    }
    
    if (m_format.pFormatContext) {
        avformat_close_input(&m_format.pFormatContext);
    }
    
    memset(&m_format, 0, sizeof(FormatInfo));
    m_VideoPool.Flush();
    m_AudioPool.Flush();
}

int CFFmpegDemuxer::Load()
{
    Log("CFFmpegDemuxer::Load\n");
//    maintain_avio();
    ReleaseResources();

    if (avformat_open_input(&m_format.pFormatContext, m_strURL.c_str(), NULL, NULL) != 0) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_IO, 0, NULL);
        return E_FAIL;
    }
    if (avformat_find_stream_info(m_format.pFormatContext, NULL) < 0) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADSTREAM, 0, NULL);
        return E_FAIL;
    }
    if (!PrepareCodecs(m_format.pFormatContext)) { // audio/video codec都找不到
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_NOCODECS, 0, NULL);
        return E_FAIL;
    }
    m_format.bDecodeAudio = PrepareAudioData(m_format.pFormatContext);
    m_format.bDecodeVideo = PrepareVideoData(m_format.pFormatContext);
    if (m_format.bDecodeAudio && !m_format.bDecodeVideo) {
        m_llDisconThreshold = TS_JUMP_THRESHOLD / m_audio.lfTimebase;
        NotifyEvent(EVENT_AUDIO_ONLY, 0, 0, NULL);
    } else if (m_format.bDecodeVideo && !m_format.bDecodeAudio) {
        m_llDisconThreshold = TS_JUMP_THRESHOLD / m_video.lfTimebase;
        NotifyEvent(EVENT_VIDEO_ONLY, 0, 0, NULL);
    } else if (m_format.bDecodeAudio && m_format.bDecodeVideo) {
        m_llDisconThreshold = TS_JUMP_THRESHOLD / m_video.lfTimebase;
        m_lfConvert = m_video.lfTimebase / m_audio.lfTimebase;
    } else {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_BADSTREAM, 0, NULL);
        return E_FAIL;
    }
    if (m_format.bDecodeAudio) {
    	NotifyEvent(EVENT_CREATE_AUDIO, 0, 0, NULL);
    }
    if (m_format.bDecodeVideo) {
        NotifyEvent(EVENT_CREATE_VIDEO, 0, 0, NULL);
    }

    BOOL bSupport = TRUE;
    NotifyEvent(EVENT_CHECK_DEVICE, 0, 0, &bSupport);
    if (!bSupport) {
        NotifyEvent(EVENT_ENCOUNTER_ERROR, E_UNSUPPORTED, 0, NULL);
        return E_FAIL;
    }

    UpdateSyncPoint(0);
    m_llStartTime = m_llSyncPoint; // first key frame timestamp
    if (m_lfOffset > 0 && m_lfOffset <= m_video.llFormatDuration / AV_TIME_BASE) {
        Seek(m_lfOffset);
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
    
    if (m_bRemote) {
        Sleep(50);
    }
    
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
    m_AudioPool.Flush();
    m_VideoPool.Flush();
//    maintain_avio();
    Seek(m_lfOffset);
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
    
    memset(&m_video,  0, sizeof(VideoInfo));
    memset(&m_audio,  0, sizeof(AudioInfo));
    
//    av_set_notify_cb(AV_NOTIFY_RECONNECT, NULL, &m_format);
    
    m_llStartTime   = AV_NOPTS_VALUE;
    m_llLastAudioTS = AV_NOPTS_VALUE;
    m_llLastVideoTS = AV_NOPTS_VALUE;

    m_bDiscard = FALSE;
    
    CMediaObject::Unload();
    return S_OK;
}

int CFFmpegDemuxer::SetEOS()
{
    CMediaObject::SetEOS();

    
    return S_OK;
}

int CFFmpegDemuxer::Seek(double lfOffset)
{
    double lfTimebase = m_format.bDecodeVideo ? m_video.lfTimebase : m_audio.lfTimebase;
    int nStreamIdx = m_format.bDecodeVideo ? m_format.nVideoStreamIdx : m_format.nAudioStreamIdx;
    AVStream* pStream = m_format.pFormatContext->streams[nStreamIdx];
    if (!pStream) {
        return E_FAIL;
    }

    int64_t llStartTS = pStream->index_entries ? pStream->index_entries[0].timestamp : 0;
    int64_t llTargetStart = (lfOffset + llStartTS * lfTimebase) * AV_TIME_BASE;
    int64_t llSeekStart = av_rescale_q(llTargetStart, AV_TIME_BASE_Q, pStream->time_base);
    avformat_seek_file(m_format.pFormatContext, m_format.bDecodeVideo ? m_format.nVideoStreamIdx : m_format.nAudioStreamIdx, INT64_MIN, llSeekStart, INT64_MAX, AVSEEK_FLAG_BACKWARD);
    
    if (m_format.bDecodeAudio) {
        for (int i = 0; i < MAX_AUDIO_TRACKS; ++i) {
            if (m_format.pAudioContext[i]) {
                avcodec_flush_buffers(m_format.pAudioContext[i]);
            }
        }
    }
    if (m_format.bDecodeVideo) {
        avcodec_flush_buffers(m_format.pVideoContext);
    }

    UpdateSyncPoint2(llSeekStart);
    
    m_llLastAudioTS = m_format.bDecodeVideo ? 
        FFMAX(m_llSyncPoint * m_video.lfTimebase / m_audio.lfTimebase, 0) : FFMAX(m_llSyncPoint, 0);
    m_llLastVideoTS = FFMAX(m_llSyncPoint, 0);
    Log("m_llLastAudioTS = %lld\n", m_llLastAudioTS);
    Log("m_llLastVideoTS = %lld\n", m_llLastVideoTS);
    
    return S_OK;
}

int CFFmpegDemuxer::GetSamplePool(const GUID& guid, ISamplePool** ppPool)
{
    AssertValid(ppPool);
    
    if (!memcmp(&guid, &GUID_AUDIO_DECODER, sizeof(GUID))) {
        *ppPool = &m_AudioPool;
    } else if (!memcmp(&guid, &GUID_VIDEO_DECODER, sizeof(GUID))) {
        *ppPool = &m_VideoPool;
    } else {
        *ppPool = NULL;
    }
    
    return S_OK;
}

void CFFmpegDemuxer::UpdateSyncPoint(LONGLONG llTime)
{
    double lfTimebase = m_format.bDecodeVideo ? m_video.lfTimebase : m_audio.lfTimebase;
    int nStreamIdx = m_format.bDecodeVideo ? m_format.nVideoStreamIdx : m_format.nAudioStreamIdx;
    AVStream* pStream = m_format.pFormatContext->streams[nStreamIdx];
    if (!pStream) {
        return;
    }
    
    int64_t llStartTS = pStream->index_entries ? pStream->index_entries[0].timestamp : 0;
    int64_t llTargetStart = (llTime + llStartTS * lfTimebase) * AV_TIME_BASE;
    int64_t llStartTime = av_rescale_q(llTargetStart, AV_TIME_BASE_Q, pStream->time_base);
    
    UpdateSyncPoint2(llStartTime);
}

void CFFmpegDemuxer::UpdateSyncPoint2(LONGLONG llTime)
{
    AVFormatContext* pFmtCtx = m_format.pFormatContext;
    AVStream* pStream = pFmtCtx->streams[m_format.bDecodeVideo ? m_format.nVideoStreamIdx : m_format.nAudioStreamIdx];
    
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
    
    double lfTimebase = m_format.bDecodeVideo ? m_video.lfTimebase : m_audio.lfTimebase;
    m_nJumpLimit = m_bRemote ? 0x7FFFFFFF : m_nJumpLimit;
    m_lfJumpBack = m_lfOffset - (m_llSyncPoint - m_llStartTime) * lfTimebase;
    AssertValid(m_lfJumpBack >= 0);
    m_llSyncPoint = m_lfJumpBack <= m_nJumpLimit ? 
        m_llSyncPoint : (m_lfOffset - m_nJumpLimit) / lfTimebase + m_llStartTime;
}

BOOL CFFmpegDemuxer::PrepareCodecs(AVFormatContext* pFmtCtx)
{
    AVStream** pStreams = pFmtCtx->streams;
    AssertValid(m_audio.nTrackCount == 0);
    
    m_format.nAudioStreamIdx = -1;
    m_format.nVideoStreamIdx = -1;
    
    for (int i = 0; i < pFmtCtx->nb_streams; ++i) {
        if (!pStreams[i] || !pStreams[i]->codec)
            continue;
        
        AVCodec* pCodec = avcodec_find_decoder(pStreams[i]->codec->codec_id);
        // zero: successful, negative value: error occurs
        
        AVMediaType nCodecType = pStreams[i]->codec->codec_type;
        if (AVMEDIA_TYPE_VIDEO == nCodecType) {
            if (!pCodec || avcodec_open2(pStreams[i]->codec, pCodec, NULL) != 0)
                continue;
            m_format.pVideoCodec = pCodec;
            m_format.nVideoStreamIdx = i;
            m_format.pVideoContext = pStreams[i]->codec;
            m_format.pVideoContext->skip_loop_filter = AVDISCARD_ALL;
        } else if (AVMEDIA_TYPE_AUDIO == nCodecType) {
            pStreams[i]->codec->request_channel_layout = av_get_default_channel_layout(
                    pStreams[i]->codec->channels > 0 ? FFMIN(2, pStreams[i]->codec->channels) : 2);
            if (!pCodec || avcodec_open2(pStreams[i]->codec, pCodec, NULL) != 0)
                continue;
            pStreams[i]->codec->codec = pCodec;
            m_format.pAudioCodec[m_audio.nTrackCount] = pCodec;
            m_format.nAudioStreamIdx = i;
            m_format.pAudioContext[m_audio.nTrackCount++] = pStreams[i]->codec;
        } else if (AVMEDIA_TYPE_SUBTITLE == nCodecType) {
            if (!pCodec || avcodec_open2(pStreams[i]->codec, pCodec, NULL) != 0)
                continue;
            m_format.pSubtitleCodec = pCodec;
            m_format.nSubtitleStreamIdx = i;
            m_format.pSubtitleContext = pStreams[i]->codec;
        }
    }
    
    m_audio.nCurTrack = FFMAX(m_audio.nTrackCount - 1, 0);
    if (!m_format.pVideoCodec && !m_format.pAudioCodec) { // audio/video codec都找不到
        return FALSE;
    }
    
    return TRUE;
}

BOOL CFFmpegDemuxer::PrepareVideoData(AVFormatContext* pFmtCtx)
{
    if (!m_format.pVideoCodec) {
        return FALSE;
    }
    
    AVCodecContext* pVideoCtx = m_format.pVideoContext;
    if (!pVideoCtx) {
        Log("no video codec context found!\n");
        return FALSE;
    }
    
    AVStream* pVideoStream = pFmtCtx->streams[m_format.nVideoStreamIdx];
    if (!pVideoStream) {
        Log("no video stream found!\n");
        return FALSE;
    }
    
    m_video.nWidth  = pVideoCtx->width;
    m_video.nHeight = pVideoCtx->height;
    m_video.nFormatID = pVideoCtx->codec_id;
    m_video.nBitrate  = pVideoCtx->bit_rate;
    m_video.llFormatDuration = pFmtCtx->duration;
    m_video.llDuration       = pVideoStream->duration;
    if (pVideoStream->r_frame_rate.den) {
        m_video.lfFPS = (double)pVideoStream->r_frame_rate.num / pVideoStream->r_frame_rate.den;
    }
    if (pVideoStream->time_base.num) {
        m_video.lfTimebase = pVideoStream->time_base.num / (double)pVideoStream->time_base.den;
    }
    
    if (!m_bRemote) {
        RebuildIndexEntries(pFmtCtx, pVideoCtx, m_format.pAudioContext[0]);
    }
    int nDuration = FFMAX(m_video.llFormatDuration / AV_TIME_BASE, 0);
    m_nJumpLimit = nDuration <= 60 ? 2 : nDuration <= 270 ? 4 : 0x7FFFFFFF;
 
    return TRUE;
}

int CFFmpegDemuxer::RebuildIndexEntries(AVFormatContext* pFmtCtx, AVCodecContext* pVideoCtx, AVCodecContext* pAudioCtx)
{
    AVStream* pVideoStream = pFmtCtx->streams[m_format.nVideoStreamIdx];
    AVInputFormat* pInputFormat = pFmtCtx->iformat;
    BOOL bAddIndex = FALSE;
    int nTotalFrames = 0, nStartFrame = 0, nEndFrame = 0;
    double lfVideoDuration = 0;
    
    // flv files do not have and need index
    if (strcmp(pInputFormat->name, "flv") == 0) {
        return E_FAIL;
    }
    if (strcmp(pInputFormat->name, "qmv") == 0) {
        return E_FAIL;
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
            if (pkt.stream_index == m_format.nVideoStreamIdx) { // handle video stream is enough now
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
                    av_add_index_entry(pFmtCtx->streams[m_format.nVideoStreamIdx], pkt.pos, llTS,
                                       0, 0, pkt.flags & AV_PKT_FLAG_KEY ? AVINDEX_KEYFRAME : 0);
                } 
            }
            av_free_packet(&pkt);
        }
        
        // correct wrong fps or media duration if it is available
        if (llStartTS != AV_NOPTS_VALUE && llEndTS != AV_NOPTS_VALUE && nEndFrame > nStartFrame) {
            double fps = (nEndFrame - nStartFrame) / ((llEndTS - llStartTS) * m_video.lfTimebase);
            if (fps > 0 && FFABS(fps - m_video.lfFPS) >= 2) {
                m_video.lfFPS = fps;
            }
            
            lfVideoDuration = nTotalFrames / m_video.lfFPS;
            lfVideoDuration = FFMAX(lfVideoDuration, 0);
            if (lfVideoDuration > 0 && FFABS(lfVideoDuration - (double)m_video.llFormatDuration / AV_TIME_BASE) >= 8) {
                m_video.llFormatDuration = lfVideoDuration * AV_TIME_BASE;
            }
        }
        // return to the start position at last
        av_seek_frame(pFmtCtx, m_format.nVideoStreamIdx, FFMAX(llStartTS, 0), AVSEEK_FLAG_ANY);
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
            lfVideoDuration = (llMaxTS - llMinTS) * m_video.lfTimebase + (1000 / m_video.lfFPS) * nSkipFrame * 0.001;
            lfVideoDuration = FFMAX(lfVideoDuration, 0);
        }
        
        if (lfVideoDuration > 0 && FFABS(lfVideoDuration - (double)m_video.llFormatDuration / AV_TIME_BASE) >= 8) {
            int64_t llFileSize = avio_size(pFmtCtx->pb);
            if (pVideoCtx->bit_rate > 0 && pVideoCtx->bit_rate < MAX_BIT_RATE) { 
                int nBitrate = pVideoCtx->bit_rate + (m_format.bDecodeAudio ? pAudioCtx->bit_rate : 0);
                if (FFABS((nBitrate >> 3) * lfVideoDuration - llFileSize) < llFileSize * 0.2) {
                    m_video.llFormatDuration = lfVideoDuration * AV_TIME_BASE;
                } 
            } 
        }
    }
    
    return S_OK;
}

BOOL CFFmpegDemuxer::PrepareAudioData(AVFormatContext* pFmtCtx)
{
    if (!m_format.pAudioCodec) {
        return FALSE;
    }
    
    AVStream* pAudioStream = pFmtCtx->streams[m_format.nAudioStreamIdx];
    if (!pAudioStream) {
        Log("no audio stream found!\n");
        return FALSE;
    }
    
    AVCodecContext* pAudioCtx = m_format.pAudioContext[m_audio.nCurTrack];
    if (!pAudioCtx) {
        Log("no audio codec context found!\n")
        return FALSE;
    }
    
    m_audio.nSampleRate       = pAudioCtx->sample_rate;
    m_audio.nFormatID		  = pAudioCtx->codec_id;
    m_audio.nChannelsPerFrame = pAudioCtx->channels;
    m_audio.nFramesPerPacket  = pAudioCtx->frame_size;
    m_audio.nBitrate	      = pAudioCtx->bit_rate;
    m_audio.nSampleFormat     = pAudioCtx->sample_fmt;
    if (pAudioStream->time_base.den) {
        m_audio.lfTimebase = (double)pAudioStream->time_base.num / pAudioStream->time_base.den;
    }
    m_audio.avrTimebase = pAudioStream->time_base;

    return TRUE;
}

