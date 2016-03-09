//
//  Pools.cpp
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include <iostream>
#include "Pools.h"
#include "FFmpegData.h"
#include "../MediaObject.h"

///////////////////////////////////////////////////////////////

int CPacketPool::Flush()
{
    CMediaSample sample;
    
    while (GetUnused(sample) == S_OK) {
        AVPacket* pPacket = (AVPacket*)sample.m_pBuf;
        align_free(pPacket->data);
        pPacket->data = NULL;
        Recycle(sample);
    }
    AssertValid(Size() == 0);
    
    return S_OK;
}

///////////////////////////////////////////////////////////////

CFramePool::CFramePool() 
    : CSamplePool()
{
    POOL_PROPERTIES request, actual;
    request.nSize  = sizeof(DWORD);
    request.nCount = FRAME_POOL_SIZE;
    CSamplePool::SetProperties(&request, &actual);
    
    CMediaSample sample;
    for (int i = 0; GetEmpty(sample) == S_OK; ++i) {
        sample.m_Type   = SAMPLE_FRAME;
        sample.m_pExten = &m_Frames[i];
        Commit(sample);
    }
    Flush();
}

CFramePool::~CFramePool()
{
}

int CFramePool::Flush()
{
    CSamplePool::Flush();
    
    for (int i = 0; i < FRAME_POOL_SIZE; ++i) {
        m_Frames[i].m_nType     = 0;
        m_Frames[i].m_bShow     = FALSE;
        m_Frames[i].m_nDuration = 0;
    }
    
    return S_OK;
}

int CFramePool::Reset()
{
    CFramePool::Flush();
    
    for (int i = 0; i < FRAME_POOL_SIZE; ++i) {
        m_Frames[i].m_nWidth  = 0;
        m_Frames[i].m_nHeight = 0;
    }
    
    return S_OK;
}

///////////////////////////////////////////////////////////////

CPcmPool::CPcmPool() 
    : CSamplePool(), m_pPCMs(NULL)
{
    POOL_PROPERTIES request, actual;
    request.nSize  = sizeof(void*);
    request.nCount = PCM_BUFFER_COUNT;
    CSamplePool::SetProperties(&request, &actual);
    
    m_pPCMs = (BYTE*)align_malloc(PCM_BUFFER_SIZE, DEFAULT_ALIGN_BYTE);
    AssertValid(m_pPCMs);
    
    CMediaSample sample;
    BYTE* pAligned = m_pPCMs;
    while (GetEmpty(sample) == S_OK) {
        sample.m_Type  = SAMPLE_PCM;
        sample.m_pBuf  = pAligned; // each pcm buffer starts at a 16-byte aligned location
        sample.m_nSize = AVCODEC_MAX_AUDIO_FRAME_SIZE * 3 / 2;
        pAligned += AVCODEC_MAX_AUDIO_FRAME_SIZE * 3 / 2;
        Commit(sample);
    }
    
    Flush();
}

CPcmPool::~CPcmPool()
{
    if (m_pPCMs) {
        align_free(m_pPCMs);
        m_pPCMs = NULL;
    }
}

int CPcmPool::Flush()
{
    CMediaSample sample;
    
    while (GetUnused(sample) == S_OK) {
        sample.m_pCur    = sample.m_pBuf;
        sample.m_nActual = 0;
        Recycle(sample);
    }
    m_nSize = 0;
    
    return S_OK;
}

int CPcmPool::GetSize()
{
    CAutoLock cObjectLock(&m_csSize);
    return m_nSize;
}

int CPcmPool::Commit(const CMediaSample& sample)
{
    CAutoLock cObjectLock(&m_csSize);
    
    int nResult = CSamplePool::Commit(sample);
    AssertValid(nResult == S_OK);
    m_nSize += sample.m_nActual;
    AssertValid(m_nSize <= PCM_BUFFER_SIZE);
    
    return nResult;
}

void CPcmPool::Consume(int nConsumed)
{
    CAutoLock cObjectLock(&m_csSize);
    m_nSize -= nConsumed;
    AssertValid(m_nSize >= 0);
}

