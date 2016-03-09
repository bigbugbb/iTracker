//
//  BufferingManager.cpp
//  QVOD
//
//  Created by bigbug on 11-11-25.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include <ctime>
#include "BufferingManager.h"
//using ios_qvod_player::Sleep;
using ios_qvod_player::CAutoLock;

#include "Components/CompInterfaces.h"

const int DEFAULT_BUFFER_SIZE = 320 * 1024;

extern CCallbackManager* g_CallbackManager;

CBufferingManager::CBufferingManager()
{
    srand(time(NULL));
}

CBufferingManager::~CBufferingManager()
{
}

CBufferingManager* CBufferingManager::GetInstance()
{
    static CBufferingManager s_BufMgr;
    
    return &s_BufMgr;
}

void CBufferingManager::Initialize()
{
    m_nCurSize = 0;
    m_nBufSize = DEFAULT_BUFFER_SIZE;

    m_fProgress = 0;
    m_bBuffering = FALSE;
    
    m_cbdBeg = g_CallbackManager->GetCallbackData(CALLBACK_BEGIN_BUFFERING);
    m_cbdBuf = g_CallbackManager->GetCallbackData(CALLBACK_ON_BUFFERING);
    m_cbdEnd = g_CallbackManager->GetCallbackData(CALLBACK_END_BUFFERING);
    m_cbdSpd = g_CallbackManager->GetCallbackData(CALLBACK_GET_DOWNLOAD_SPEED);
}

void CBufferingManager::SetBufferSize(int nBufSize)
{
    AssertValid(nBufSize > 0);
    m_nBufSize = nBufSize;
}

void CBufferingManager::SetProbe(IBufferingProbe* pProbe)
{
    AssertValid(pProbe);
    m_vecBufProbes.push_back(pProbe);
}

void CBufferingManager::Reset()
{
    CAutoLock cObjectLock(&m_csBuffering);
    
    if (m_bBuffering) {
        NotifyEvent(EVENT_WAIT_FOR_RESOURCES, FALSE, TRUE/*FORCE CANCEL*/, NULL); 
        (*m_cbdEnd.pfnCallback)(m_cbdEnd.pUserData, m_cbdEnd.pReserved);
        m_bBuffering = FALSE;
    }
    
    m_nCurSize = 0;
    m_fProgress = 0;
}

void CBufferingManager::UpdateBuffering(int nRecvSize)
{
    m_nCurSize += nRecvSize;
}

inline 
BOOL CBufferingManager::IsNeedBuffering()
{
    for (int i = 0; i < m_vecBufProbes.size(); ++i) {
        IBufferingProbe* pProbe = m_vecBufProbes[i];
        
        if (pProbe->IsProbing()) {
            if (!pProbe->IsNeedBuffering()) {
                return FALSE;
            }
        }
    }
    
    return TRUE;
}

inline 
BOOL CBufferingManager::IsBuffering()
{
    CAutoLock cObjectLock(&m_csBuffering);
    
    return m_bBuffering;
}

THREAD_RETURN CBufferingManager::ThreadProc()
{
    m_nWait = 200000;

    while (m_bRun) {
        if (IsBuffering()) {
            OnBuffering();
            m_nWait = 1000000;
            
            if (!IsNeedBuffering()) {
                EndBuffering();
                m_nWait = 200000;
            }
        } else {
            if (IsNeedBuffering()) {
                BeginBuffering();
                m_nWait = 0;
            } 
        }
        
        Wait(m_nWait);
    }
    
    return 0;
}

int CBufferingManager::BeginBuffering()
{
    CAutoLock cObjectLock(&m_csBuffering);
    
    NotifyEvent(EVENT_WAIT_FOR_RESOURCES, TRUE, FALSE, NULL);
    
    m_bBuffering = TRUE;
    m_fProgress = 0;
    
    m_fPart1 = (25 + rand() % 36) * 0.01;
    m_fPart2 = (10 + rand() % 21) * 0.01;
    m_fPart3 = (90 + rand() % 10) * 0.01 - m_fPart1 - m_fPart2;
    
    m_fPart1Progress = 0;
    m_fPart2Progress = 0;
    m_fPart3Progress = 0;
    
    (*m_cbdBeg.pfnCallback)(m_cbdBeg.pUserData, m_cbdBeg.pReserved);
    
    gettimeofday(&m_tmLast, NULL);
    
    return S_OK;
}

int CBufferingManager::OnBuffering()
{
    AssertValid(m_nBufSize > 0);
    
    struct timeval tmNow;
    gettimeofday(&tmNow, NULL);
    float fInterval = tmNow.tv_sec - m_tmLast.tv_sec + (tmNow.tv_usec - m_tmLast.tv_usec) * 0.000001;
    if (fInterval >= 1) {
        m_tmLast = tmNow;
    } else {
        return S_OK;
    }
    
    int nSpeed = 0;
    (*m_cbdSpd.pfnCallback)(m_cbdBuf.pUserData, &nSpeed);
    if (nSpeed <= 0) {
        (*m_cbdBuf.pfnCallback)(m_cbdBuf.pUserData, &m_fProgress);
        return S_OK;
    }
    
    if (m_fPart1Progress < m_fPart1) {
        m_fPart1Progress += rand() % (nSpeed > 60 ? (nSpeed > 180 ? 11 : 6) : 3) * 0.01;
    }
    if (m_fPart2Progress < m_fPart2) {
        m_fPart2Progress = (float)m_nCurSize / m_nBufSize * m_fPart2;
    }
    if (m_fPart3Progress < m_fPart3) {
        m_fPart3Progress += rand() % (nSpeed > 60 ? (nSpeed > 200 ? 6 : 4) : 2) * 0.01; 
    }
    
    m_fPart1Progress = (m_fPart1Progress >= m_fPart1) ? m_fPart1 : m_fPart1Progress;
    m_fPart2Progress = (m_fPart2Progress >= m_fPart2) ? m_fPart2 : m_fPart2Progress;
    m_fPart3Progress = (m_fPart3Progress >= m_fPart3) ? m_fPart3 : m_fPart3Progress;
    
    m_fProgress = m_fPart1Progress + m_fPart2Progress + m_fPart3Progress;
    (*m_cbdBuf.pfnCallback)(m_cbdBuf.pUserData, &m_fProgress);

    return S_OK;
}

int CBufferingManager::EndBuffering()
{
    CAutoLock cObjectLock(&m_csBuffering);
    
    m_fProgress  = 1;
    m_bBuffering = FALSE;

    (*m_cbdBuf.pfnCallback)(m_cbdBuf.pUserData, &m_fProgress);
    (*m_cbdEnd.pfnCallback)(m_cbdEnd.pUserData, m_cbdEnd.pReserved);
    
    NotifyEvent(EVENT_WAIT_FOR_RESOURCES, FALSE, FALSE, NULL);
    
    Reset();
    
    return S_OK;
}




