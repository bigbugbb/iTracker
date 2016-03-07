//
//  AudioRenderer.cpp
//  QVOD
//
//  Created by bigbug on 11-11-14.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "GUIDs.h"
#include "FFmpegData.h"
#include "AudioRenderer.h"

#define AUDIO_SYNC_MIN_TOP      35   // ms
#define AUDIO_SYNC_MAX_TOP      8000 // ms
#define AUDIO_SYNC_MIN_BOTTOM  -8000 // ms
#define AUDIO_SYNC_MAX_BOTTOM  -65   // ms

///////////////////////////////////////////////////////////////
CAudioRenderer::CAudioRenderer(const GUID& guid, IDependency* pDepend, int* pResult)
    : CSink(guid, pDepend), m_bEnable(TRUE), m_bSwitch(FALSE), m_bInterrupt(FALSE)
{
    m_fTimebase = 0;
    m_fSeekTime = 0;
    
    m_nSampleRate   = 0;
    m_nChannelCount = 0;
    m_nSampleFormat = 0;
    
    PrepareSeek(FALSE);
}

CAudioRenderer::~CAudioRenderer()
{
    
}

// IAudioRenderer
int CAudioRenderer::SetMediaStartTime(float fTime)
{
    m_llStartPTS = fTime;
    
    return S_OK;
}

int CAudioRenderer::SetMediaSeekTime(float fTime)
{
    PrepareSeek(TRUE);
    m_fSeekTime = fTime;
    m_llCurrentPTS = fTime / m_fTimebase;
    //Log("SetMediaSeekTime: %lf\n", m_fSeekTime);
    
    return S_OK;
}

int CAudioRenderer::GetMediaCurrentTime(float* pTime)
{
    AssertValid(pTime);
    *pTime = (m_llCurrentPTS - m_llStartPTS) * m_fTimebase;
    
    return S_OK;
}

int CAudioRenderer::SetTimebase(float fTimebase)
{
    m_fTimebase = fTimebase;
    
    return S_OK;
}

int CAudioRenderer::SetSampleRate(int nSampleRate)
{
    AssertValid(nSampleRate > 0);
    m_nSampleRate = nSampleRate;
    
    return S_OK;
}

int CAudioRenderer::SetChannelCount(int nChannelCount)
{
    m_nChannelCount = nChannelCount;
    
    return S_OK;
}

int CAudioRenderer::SetSampleFormat(int nSampleFormat)
{
    m_nSampleFormat = nSampleFormat;
    
    return S_OK;
}

void CAudioRenderer::PrepareSeek(BOOL bPrepare)
{
    CAutoLock cObjectLock(&m_csPreSeek);
    
    m_bPreSeek = bPrepare;
}

inline
BOOL CAudioRenderer::IsPreparingSeek()
{
    CAutoLock cObjectLock(&m_csPreSeek);
    
    return m_bPreSeek;
}

inline 
LONGLONG CAudioRenderer::EstimateTimestamp(const CMediaSample& sample) 
{
    LONGLONG llDelta = (sample.m_pCur - sample.m_pBuf) * m_fTSScale;
    
    if (!IsPreparingSeek()) {
        m_llCurrentPTS = sample.m_llTimestamp + llDelta;
    }
    
    return sample.m_llTimestamp + llDelta;
}

int CAudioRenderer::OutputAudio(BYTE* pData, UINT nDataByteSize)
{
    CAutoLock cObjectLock(this);
    
    if (GetState() & STATE_WAITFORRESOURCES) {
        memset(pData, 0, nDataByteSize);
        return S_OK;
    }
    
    //Log("nDataByteSize = %d\n", nDataByteSize);
    if (!(GetState() & STATE_EXECUTE)) {
        memset(pData, 0, nDataByteSize);
        //Log("audio 1\n");
        return E_FAIL;
    }
    
    if (m_vecInObjs[0]->IsEOS() && m_PcmPool.GetSize() < nDataByteSize) {
        SetEOS();
        memset(pData, 0, nDataByteSize);
        //Log("audio 2\n");
        return E_FAIL;
    }
    
    FillBuffer(pData, nDataByteSize);
    
    while (!m_pRefClock->IsStarted()) {
        m_ASync.Wait(10000);
        if (m_bFlush || m_bClose || m_bSwitch || m_bInterrupt) break;
    }
    
    if (m_bSwitch || m_bInterrupt) {
        memset(pData, 0, nDataByteSize);
    }

    //Log("audio 3\n");
    return S_OK;
}

inline
void CAudioRenderer::FillBuffer(BYTE* pBuffer, UINT nDataByteSize)
{
    CMediaSample sample;
    BOOL bFirst = TRUE;
    LONGLONG llEarly = 0;
    
    while (nDataByteSize > 0 && !m_bFlush && !m_bClose) {
        int nResult = m_PcmPool.GetUnused(sample);
        if (nResult != S_OK) {
            if (m_vecInObjs[0]->IsEOS() || m_bSwitch || m_bInterrupt) break;
            m_ASync.Wait(3000); continue;
        }
        //Log("audio pts: %lld, syncpt: %lld, actual: %d, stream time: %lld\n", 
        //    sample.m_llTimestamp, sample.m_llSyncPoint, sample.m_nActual, m_pRefClock->GetTime());
        
        if (bFirst) {
            bFirst  = FALSE;
            llEarly = (EstimateTimestamp(sample) - sample.m_llSyncPoint) * m_fTimebase * 1000 - m_pRefClock->GetTime();
            //Log("llEarly = %lld\n", llEarly);
            
            if (llEarly > AUDIO_SYNC_MIN_TOP) {
                memset(pBuffer, 0, nDataByteSize);
                //Log("actual: %d, wait: %lld\n", sample.m_nActual, llEarly);
                if (llEarly > AUDIO_SYNC_MAX_TOP) {
                    m_PcmPool.Consume(sample.m_nActual);
                    m_PcmPool.Recycle(sample);
                    continue;
                }
                break;
            } else if (llEarly < AUDIO_SYNC_MAX_BOTTOM) {
                //Log("early = %lld\n", llEarly);
                if (sample.m_nActual <= nDataByteSize) {
                    m_PcmPool.Consume(sample.m_nActual);
                    m_PcmPool.Recycle(sample);
                } else { // sample.m_nActual > nDataByteSize
                    sample.m_pCur    += nDataByteSize;
                    sample.m_nActual -= nDataByteSize;
                    m_PcmPool.Consume(nDataByteSize);
                    m_PcmPool.Update(sample);
                }
                bFirst = TRUE;
                continue;
            }
        }
        AssertValid(llEarly >= AUDIO_SYNC_MAX_BOTTOM);
        
        if (sample.m_nActual > nDataByteSize) {
            memcpy(pBuffer, sample.m_pCur, nDataByteSize);
            sample.m_pCur    += nDataByteSize;
            sample.m_nActual -= nDataByteSize;
            m_PcmPool.Consume(nDataByteSize);
            m_PcmPool.Update(sample);
            nDataByteSize = 0;
        } else if (sample.m_nActual == nDataByteSize) {
            memcpy(pBuffer, sample.m_pCur, nDataByteSize);
            m_PcmPool.Consume(nDataByteSize);
            m_PcmPool.Recycle(sample);
            nDataByteSize = 0;
        } else { // sample.m_nActual < nDataByteSize
            memcpy(pBuffer, sample.m_pCur, sample.m_nActual);
            pBuffer += sample.m_nActual;
            nDataByteSize -= sample.m_nActual;
            m_PcmPool.Consume(sample.m_nActual);
            m_PcmPool.Recycle(sample);
        }
    }
}

int CAudioRenderer::Load()
{
    Log("CAudioRenderer::Load\n");
    m_nSampleRate   = 0;
    m_nChannelCount = 0;
    m_fTSScale = 0;
    m_bClose  = FALSE;
    m_bSwitch = FALSE;
    
    CMediaObject::Load();
    return S_OK;
}

int CAudioRenderer::WaitForResources(BOOL bWait)
{
    Log("CAudioRenderer::WaitForResources\n");
    CMediaObject::WaitForResources(bWait);
    
    
    
    return S_OK;
}

int CAudioRenderer::Idle()
{
    Log("CAudioRenderer::Idle\n");

    CMediaObject::Idle();
    return S_OK;
}

int CAudioRenderer::Execute()
{
    Log("CAudioRenderer::Execute\n");
    AssertValid(m_nSampleRate > 0 && m_nChannelCount > 0);
    if (m_nSampleRate <= 0 || m_nChannelCount <= 0) {
        m_fTSScale = 0;
    } else {
        int nBytePerSample = av_get_bytes_per_sample((AVSampleFormat)m_nSampleFormat);
        m_fTSScale = (float)1 / (m_nSampleRate * nBytePerSample * m_nChannelCount) / m_fTimebase; // pts per byte
    }
    m_bInterrupt = FALSE;
    
    CMediaObject::Execute();
    return S_OK;
}

int CAudioRenderer::Pause()
{
    Log("CAudioRenderer::Pause\n");
    m_bInterrupt = TRUE;
    
    CMediaObject::Pause();
    return S_OK;
}

int CAudioRenderer::BeginFlush()
{
    Log("CAudioRenderer::BeginFlush\n");
    CMediaObject::BeginFlush();
    
    m_ASync.Signal();
    Lock();
    m_PcmPool.Flush();
    
    return S_OK;
}

int CAudioRenderer::EndFlush()
{
    Log("CAudioRenderer::EndFlush\n");
    PrepareSeek(FALSE);
    
    Unlock();
    return CMediaObject::EndFlush();
}

int CAudioRenderer::Invalid()
{
    Log("CAudioRenderer::Invalid\n");
    CMediaObject::Invalid();

    m_ASync.Signal();
    
    return S_OK;
}

int CAudioRenderer::Unload()
{
    Log("CAudioRenderer::Unload\n");
    m_bClose = TRUE;
    m_bInterrupt = FALSE;
    m_ASync.Signal();
    Lock();
    m_PcmPool.Flush();
    Unlock();
    PrepareSeek(FALSE);
    
    CMediaObject::Unload();
    return S_OK;
}

int CAudioRenderer::SetEOS()
{
    if (m_bEOS) {
        return S_OK;
    }
    m_bEOS = TRUE;
    
    NotifyEvent(EVENT_AUDIO_EOS, 0, 0, NULL);
    
    return S_OK;
}

int CAudioRenderer::GetInputPool(const GUID& requestor, ISamplePool** ppPool)
{
    AssertValid(ppPool);
    
    if (requestor == GUID_AUDIO_DECODER) {
        *ppPool = &m_PcmPool;
    } else {
        *ppPool = NULL;
    }
    
    return S_OK;
}

int CAudioRenderer::RespondDispatch(const GUID& sender, int nType, void* pUserData)
{
    if (nType == DISPATCH_SWITCH_AUDIO) {
        m_bSwitch = TRUE; // ensure we are not be blocked
        m_ASync.Signal(); // cancel any possible waiting
        
        { // do not delete '{' since the auto lock will keep locking until '}'
            CAutoLock cObjectLock(this);
            
            Feedback(GUID_AUDIO_DECODER, FEEDBACK_SWITCH_AUDIO, &m_llCurrentPTS);
            
            AudioTrack* pTrackA = (AudioTrack*)pUserData;
            AssertValid(pTrackA);
            SetTimebase(pTrackA->fTimebase);
            SetSampleFormat(pTrackA->nSampleRate);
            SetChannelCount(pTrackA->nChannelsPerFrame);
            SetSampleFormat(pTrackA->nSampleFormat);
            if (m_nSampleRate <= 0 || m_nChannelCount <= 0) {
                m_fTSScale = 0;
            } else {
                int nBytesPerSample = av_get_bytes_per_sample((AVSampleFormat)m_nSampleFormat);
                m_fTSScale = (float)1 / (m_nSampleRate * nBytesPerSample * m_nChannelCount) / m_fTimebase; // pts per byte
            }
        
            m_PcmPool.Flush();
        }
        
        NotifyEvent(EVENT_RESET_AUDIO, 0, 0, NULL);
        m_bSwitch = FALSE;
    }
    
    return S_OK;
}



