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

#define AUDIO_SYNC_MIN_TOP      5    // ms
#define AUDIO_SYNC_MAX_TOP      8000 // ms
#define AUDIO_SYNC_MIN_BOTTOM  -8000 // ms
#define AUDIO_SYNC_MAX_BOTTOM  -40   // ms

///////////////////////////////////////////////////////////////
CAudioRenderer::CAudioRenderer(const GUID& guid, IDependency* pDepend, int* pResult)
    : CSink(guid, pDepend), m_bEnable(TRUE), m_bInterrupt(FALSE)
{
    m_lfTimebase = 0;
    m_lfSeekTime = 0;
    m_nSampleRate   = 0;
    m_nChannelCount = 0;
    m_nSampleFormat = 0;
}

CAudioRenderer::~CAudioRenderer()
{
    
}

// IAudioRenderer
int CAudioRenderer::SetMediaStartTime(double lfTime)
{
    m_llStartPTS = lfTime;
    
    return S_OK;
}

int CAudioRenderer::SetMediaSeekTime(double lfTime)
{
    m_lfSeekTime = lfTime;
    
    return S_OK;
}

int CAudioRenderer::GetMediaCurrentTime(double* pTime)
{
    AssertValid(pTime);
    
    return E_NOIMPL;
}

int CAudioRenderer::SetTimebase(double lfTimebase)
{
    m_lfTimebase = lfTimebase;
    
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

int CAudioRenderer::Interrupt(BOOL bInterrupt)
{
    m_bInterrupt = bInterrupt;
    
    return S_OK;
}

inline 
LONGLONG CAudioRenderer::EstimateTimestamp(const CMediaSample& sample) 
{
    LONGLONG llDelta = (sample.m_pCur - sample.m_pBuf) * m_lfTSScale;
    
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
        if (m_bFlush || m_bClose || m_bInterrupt) break;
    }
    
    if (m_bInterrupt) {
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
            if (m_vecInObjs[0]->IsEOS() || m_bInterrupt) break;
            m_ASync.Wait(3000); continue;
        }
        //Log("audio pts: %lld, syncpt: %lld, actual: %d, stream time: %lld\n", 
        //    sample.m_llTimestamp, sample.m_llSyncPoint, sample.m_nActual, m_pRefClock->GetTime());
        
        if (bFirst) {
            bFirst  = FALSE;
            llEarly = (EstimateTimestamp(sample) - sample.m_llSyncPoint) * m_lfTimebase * 1000 - m_pRefClock->GetTime();
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
    m_lfTSScale = 0;
    m_bClose = FALSE;
    
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
        m_lfTSScale = 0;
    } else {
        SampleFormat format = (SampleFormat)m_nSampleFormat;
        Log("format = %d\n", format);
        int nBitCount = 16;
        
        switch (format) {
        case AV_SAMPLE_FMT_U8:
            nBitCount = 8;
            break;
        case AV_SAMPLE_FMT_S16:
            nBitCount = 16;
            break;
        case AV_SAMPLE_FMT_S32:
            nBitCount = 32;
            break;
        case AV_SAMPLE_FMT_FLT:
            nBitCount = sizeof(float);
            break;
        case AV_SAMPLE_FMT_DBL:
            nBitCount = sizeof(double);
            break;
        default:
            break;
        }
        
        m_lfTSScale = (double)1 / (m_nSampleRate * nBitCount / 8 * m_nChannelCount) / m_lfTimebase; // pts per byte
    }
    
    CMediaObject::Execute();
    return S_OK;
}

int CAudioRenderer::Pause()
{
    Log("CAudioRenderer::Pause\n");

    
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

int CAudioRenderer::GetSamplePool(const GUID& guid, ISamplePool** ppPool)
{
    AssertValid(ppPool);
    
    if (!memcmp(&guid, &GUID_AUDIO_DECODER, sizeof(GUID))) {
        *ppPool = &m_PcmPool;
    } else {
        *ppPool = NULL;
    }
    
    return S_OK;
}



