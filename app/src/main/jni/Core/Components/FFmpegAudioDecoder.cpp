//
//  FFmpegAudioDecoder.cpp
//  QVOD
//
//  Created by bigbug on 11-11-14.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "GUIDs.h"
#include "Global.h"
#include "FFmpegData.h"
#include "FFmpegAudioDecoder.h"

#ifdef LOG_AUDIO_PCM
    extern std::string strPathPCM;
    #define LOG_PCM(buf, size) \
    { \
        FILE* fp = fopen(strPathPCM.c_str(), "a+"); \
        fwrite(buf, 1, size, fp); \
        fclose(fp); \
    }
#else 
    #define LOG_PCM(buf, size)
#endif

CFFmpegAudioDecoder::CFFmpegAudioDecoder(const GUID& guid, IDependency* pDepend, int* pResult)
    : CMediaObject(guid, pDepend), m_pPcmPool(NULL)
{
    m_pAudio = NULL;
    m_llSwitchTime = AV_NOPTS_VALUE;

    m_pPCM = av_frame_alloc();
}

CFFmpegAudioDecoder::~CFFmpegAudioDecoder()
{
    if (m_pPCM) {
        av_frame_free(&m_pPCM);
    }
}

// IFFmpegAudioDecoder



int CFFmpegAudioDecoder::Load()
{
    Log("CFFmpegAudioDecoder::Load\n");
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
    pDemuxer->GetOutputPool(GetGUID(), &m_pAudioPool);
    if (!m_pAudioPool) {
        Log("No Audio Pool!\n");
    } else {
        for (int i = 0; i < m_vecOutObjs.size(); ++i) {
            const GUID& guid = m_vecOutObjs[i]->GetGUID();
            if (guid == GUID_AUDIO_RENDERER) {
                m_pRenderer = m_vecOutObjs[i];
            }
        }
        if (!m_pRenderer) {
            return E_FAIL;
        }
        m_pRenderer->GetInputPool(GetGUID(), &m_pPcmPool);
        if (!m_pPcmPool) {
            return E_FAIL;
        }
            
        Create();
    }
    m_sync.Signal();
    
    CMediaObject::Load();
    return S_OK;
}

int CFFmpegAudioDecoder::WaitForResources(BOOL bWait)
{
    Log("CFFmpegAudioDecoder::WaitForResources\n");
    CMediaObject::WaitForResources(bWait);
    
    return S_OK;
}

int CFFmpegAudioDecoder::Idle()
{
    Log("CFFmpegAudioDecoder::Idle\n");
    Start();
    
    CMediaObject::Idle();
    return S_OK;
}

int CFFmpegAudioDecoder::Execute()
{
    Log("CFFmpegAudioDecoder::Execute\n");
    
    CMediaObject::Execute();
    return S_OK;
}

int CFFmpegAudioDecoder::Pause()
{
    Log("CFFmpegAudioDecoder::Pause\n");
    
    CMediaObject::Pause();
    return S_OK;
}

int CFFmpegAudioDecoder::BeginFlush()
{
    Log("CFFmpegAudioDecoder::BeginFlush\n");
    CMediaObject::BeginFlush();
    
    m_sync.Wait();
    
    return S_OK;
}

int CFFmpegAudioDecoder::EndFlush()
{
    Log("CFFmpegAudioDecoder::EndFlush\n");
    m_sync.Signal();
    
    return CMediaObject::EndFlush();
}

int CFFmpegAudioDecoder::Invalid()
{
    Log("CFFmpegAudioDecoder::Invalid\n");
    CMediaObject::Invalid();
    
    Close();
    
    return S_OK;
}

int CFFmpegAudioDecoder::Unload()
{
    Log("CFFmpegAudioDecoder::Unload\n");
    Close();
    
    m_pRenderer  = NULL;
    m_pPcmPool   = NULL;
    m_pAudioPool = NULL;
    
    CMediaObject::Unload();
    return S_OK;
}

int CFFmpegAudioDecoder::SetEOS()
{
    if (m_bEOS) {
        return S_OK;
    }
    
    return CMediaObject::SetEOS();
}

int CFFmpegAudioDecoder::RespondDispatch(const GUID& sender, int nType, void* pUserData)
{
    if (nType == DISPATCH_SWITCH_AUDIO) {
        AudioTrack* pTrackA = (AudioTrack*)pUserData;
        AssertValid(pTrackA);
        
        m_sync.Wait();
        SwitchAudioTrack();
        Dispatch(GUID_AUDIO_RENDERER, nType, pTrackA);
        m_sync.Signal();
    }
    
    return S_OK;
}

int CFFmpegAudioDecoder::RespondFeedback(const GUID& sender, int nType, void* pUserData)
{
    if (nType == FEEDBACK_SWITCH_AUDIO) {
        m_llSwitchTime = *static_cast<LONGLONG*>(pUserData); // ms
    }
    
    return S_OK;
}

BOOL CFFmpegAudioDecoder::SwitchAudioTrack()
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
    
    ISamplePool* m_pAudioPool = NULL;
    pDemuxer->GetOutputPool(GetGUID(), &m_pAudioPool);
    if (!m_pAudioPool) {
        return FALSE;
    } else {
        m_pAudioPool = m_pAudioPool;
    }
    
    return TRUE;
}

THREAD_RETURN CFFmpegAudioDecoder::ThreadProc()
{
    int nWait = 0;

    while (m_bRun) {
        m_sync.Wait();
        
        if (m_pPcmPool->GetSize() != 0) {
            Feedback(GUID_DEMUXER, FEEDBACK_SUFFICIENT_DATA, NULL);
        } else {
            Feedback(GUID_DEMUXER, FEEDBACK_INSUFFICIENT_DATA, NULL);
        }
        
        if (Receive(m_pAudioPool) == E_RETRY) {
            nWait = 20;
        } else {
            nWait = 0;
        }
        
        m_sync.Signal();
        
        if (m_vecInObjs[0]->IsEOS() && !m_pAudioPool->GetSize()) {
            SetEOS();
        }
        
        Sleep(nWait);
    }
    
    return 0;
}

int CFFmpegAudioDecoder::OnReceive(CMediaSample& sample)
{
    AssertValid(sample.m_nSize == sizeof(AVPacket));
    AVPacket* pPacket = (AVPacket*)sample.m_pBuf;
    AVCodecContext* pCodecCtx = (AVCodecContext*)sample.m_pExten;
    m_pAudio = (AudioTrack*)sample.m_pSpecs;
    
    int nResult = Decode(pPacket, pCodecCtx, sample);
    if (nResult != E_RETRY) {
        align_free(pPacket->data);
        pPacket->data = NULL;
    }
    
    return nResult;
}

inline
int CFFmpegAudioDecoder::Decode(AVPacket* pPacket, AVCodecContext* pCodecCtx, const CMediaSample& sampleIn)
{
    int nLength = 0;

    if (sampleIn.m_bIgnore) {
        return S_OK;
    }
    
    if (sampleIn.m_llTimestamp <= m_llSwitchTime) {
        return S_OK;
    } else {
        m_llSwitchTime = AV_NOPTS_VALUE;
    }
    
    CMediaSample sample;
    if (m_pPcmPool->GetEmpty(sample) != S_OK) {
        return E_RETRY;
    }

    BYTE* pData = pPacket->data;
    int  nTotal = pPacket->size;
    while (pPacket->size > 0) {
//        av_frame_unref(m_pPCM);

        int nGotFrm = 0;
        if ((nLength = avcodec_decode_audio4(pCodecCtx, m_pPCM, &nGotFrm, pPacket)) < 0) {
            pPacket->data = pData;
            pPacket->size = nTotal;
            Log("avcodec_decode_audio4 fails!\n");
            return E_FAIL;
        }

        if (nGotFrm) {
            int nSize = av_samples_get_buffer_size(NULL, pCodecCtx->channels, m_pPCM->nb_samples, pCodecCtx->sample_fmt, 1);
//            LOG_PCM(sample.m_pBuf[0], nSize);
            memcpy(sample.m_pBuf, m_pPCM->data[0], nSize);
            sample.m_pCur = sample.m_pBuf;
            sample.m_nActual     = nSize;
            sample.m_llTimestamp = sampleIn.m_llTimestamp;
            sample.m_llSyncPoint = sampleIn.m_llSyncPoint;
        }

        pPacket->data += nLength;
        pPacket->size -= nLength;
    }
    pPacket->data = pData;
    pPacket->size = nTotal;
    
    m_pPcmPool->Commit(sample);

    return S_OK;
}
//
//inline
//void CFFmpegAudioDecoder::ReSampleAudioData(AVCodecContext* pCodecCtx, int nDataSize)
//{
//    ULONGLONG llDecChannelLayout = 
//        (pCodecCtx->channel_layout && pCodecCtx->channels == av_get_channel_layout_nb_channels(pCodecCtx->channel_layout)) ? 
//        pCodecCtx->channel_layout : av_get_default_channel_layout(pCodecCtx->channels);
//    
//    if (pCodecCtx->sample_fmt != AV_SAMPLE_FMT_S16/*is->audio_src_fmt || 
//        dec_channel_layout != is->audio_src_channel_layout || 
//        pCodecCtx->sample_rate != is->audio_src_freq*/) 
//    {
//        if (m_pSwrCtx) swr_free(&m_pSwrCtx);
////        m_pSwrCtx = swr_alloc_set_opts(NULL, is->audio_tgt_channel_layout, is->audio_tgt_fmt, 
////                                       is->audio_tgt_freq, dec_channel_layout, pCodecCtx->sample_fmt, pCodecCtx->sample_rate, 0, NULL);
//        m_pSwrCtx = swr_alloc_set_opts(NULL, AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16, m_pAudio->nSampleRate, 
//                                       llDecChannelLayout, pCodecCtx->sample_fmt, m_pAudio->nSampleRate, 0, NULL);
//        if (!m_pSwrCtx || swr_init(m_pSwrCtx) < 0) {
//            Log("Cannot create sample rate converter\n");
//            return;
//        }
////        is->audio_src_channel_layout = dec_channel_layout;
////        is->audio_src_channels = dec->channels;
////        is->audio_src_freq = dec->sample_rate;
////        is->audio_src_fmt = dec->sample_fmt;
//    }
//    
//    int resampled_data_size = nDataSize;
//    if (m_pSwrCtx) {
//        const uint8_t *in[] = { is->frame->data[0] };
//        uint8_t *out[] = {is->audio_buf2};
//        int len2 = swr_convert(m_pSwrCtx, out, sizeof(is->audio_buf2) / is->audio_tgt_channels / av_get_bytes_per_sample(is->audio_tgt_fmt), in, data_size / pCodecCtx->channels / av_get_bytes_per_sample(pCodecCtx->sample_fmt));
//        if (len2 < 0) {
//            Log("audio_resample() failed\n");
//            return;
//        }
//        if (len2 == sizeof(is->audio_buf2) / is->audio_tgt_channels / av_get_bytes_per_sample(is->audio_tgt_fmt)) {
//            Log("warning: audio buffer is probably too small\n");
//            swr_init(m_pSwrCtx);
//        }
//        is->audio_buf = is->audio_buf2;
//        resampled_data_size = len2 * is->audio_tgt_channels * av_get_bytes_per_sample(is->audio_tgt_fmt);
//    } else {
//        is->audio_buf = is->frame->data[0];
//    }
//}







