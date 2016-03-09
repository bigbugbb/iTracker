//
//  SamplePool.cpp
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "SysConsts.h"
#include "SamplePool.h"
#include "MediaObject.h"


CSamplePool::CSamplePool()
{
    m_pBuf = NULL;
}

CSamplePool::~CSamplePool()
{
    if (m_pBuf) {
        delete[] m_pBuf;
        m_pBuf = NULL;
    }
}

int CSamplePool::Size()
{
    int nSize = m_ring.GetSize();
    
    return nSize;
}

int CSamplePool::Flush()
{
    Cell* pCell = m_ring.GetTail();
    
    while (pCell) {
        m_ring.Commit(pCell);
        pCell = m_ring.GetTail();
    }
    
    return S_OK;
}

int CSamplePool::GetEmpty(CMediaSample& sample)
{
    Cell* pCell = m_ring.GetHead();
    
    if (!pCell) { // pool is full
        return E_RETRY;
    }
    
    CMediaSample* pSample = (CMediaSample*)pCell->pData;
    memcpy(&sample, pSample, sizeof(CMediaSample));
    
    return S_OK;
}

int CSamplePool::Commit(const CMediaSample& sample)
{
    AssertValid(sample.m_pOwner);
    Cell* pCell = (Cell*)sample.m_pOwner;
    
    if (!pCell) {
        AssertValid(0);
        return E_FAIL;
    }
    
    memcpy(pCell->pData, &sample, sizeof(CMediaSample));
    if (m_ring.Commit(pCell) != S_OK) {
        AssertValid(0);
        return E_FAIL; 
    }
    
    return S_OK;
}

int CSamplePool::Update(const CMediaSample& sample) 
{
    AssertValid(sample.m_pOwner);
    Cell* pCell = (Cell*)sample.m_pOwner;
    
    if (!pCell) {
        AssertValid(0);
        return E_FAIL;
    }
    
    memcpy(pCell->pData, &sample, sizeof(CMediaSample));
    
    return S_OK;
}

int CSamplePool::GetSize()
{
    return Size();
}

int CSamplePool::GetUnused(CMediaSample& sample)
{
    Cell* pCell = m_ring.GetTail();
    
    if (!pCell) { // pool is empty
        return E_RETRY;
    }
    
    memcpy(&sample, pCell->pData, sizeof(CMediaSample));
    
    return S_OK;
}

int CSamplePool::Recycle(CMediaSample& sample)
{    
    int nResult = m_ring.Commit((Cell*)sample.GetOwner());
    if (nResult != S_OK) {
        AssertValid(0);
        return E_FAIL;
    }
    
    return S_OK;
}

int CSamplePool::SetProperties(POOL_PROPERTIES *pRequest, 
                               POOL_PROPERTIES *pActual)
{
    int nResult;
    
    nResult = m_ring.Alloc(sizeof(CMediaSample), pRequest->nCount);
    if (nResult == S_OK) {
        if (pActual) {
            memcpy(pActual, pRequest, sizeof(POOL_PROPERTIES));
        }
    }
    
    if (m_pBuf) {
        delete[] m_pBuf;
        m_pBuf = NULL;
    }
    m_pBuf = new BYTE[pRequest->nSize * pRequest->nCount];
    if (!m_pBuf) {
        return E_FAIL;
    }
        
    int i = 0;
    Cell* pCell = m_ring.GetHead();
    while (pCell) {
        CMediaSample* pSample = (CMediaSample*)pCell->pData;
        *pSample = CMediaSample();
        
        pSample->m_pBuf    = m_pBuf;
        pSample->m_pCur    = m_pBuf;
        pSample->m_nSize   = pRequest->nSize;
        pSample->m_nActual = 0;
        pSample->m_pOwner  = pCell;
        m_pBuf += pRequest->nSize;
        m_ring.Commit(pCell);
        
        pCell = m_ring.GetHead();
        ++i;
    }
    m_pBuf -= pRequest->nSize * i;
    
    pCell = m_ring.GetTail();
    while (pCell) {
        m_ring.Commit(pCell);
        pCell = m_ring.GetTail();
    }
    
    return S_OK;
}
