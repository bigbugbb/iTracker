//
//  MemRing.cpp
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "MemRing.h"
#include "SysConsts.h"
using namespace::ios_qvod_player;

CMemRing::CMemRing()
{  
    m_nHead = 0;
    m_nTail = 0;
    m_nUsed = 0;
    m_nCount = 0;
    
    m_pCells = NULL;
    
    m_pBuffer  = NULL;
    m_nBufSize = 0;
}

CMemRing::CMemRing(UINT nSize, UINT nCount)
{
    CMemRing();
    Alloc(nSize, nCount);
}

CMemRing::~CMemRing()
{   
    Free();
}

Cell* CMemRing::GetHead()
{
    if (m_nUsed >= m_nCount) {
        return NULL;
    }

    return m_pCells + m_nHead;
}

Cell* CMemRing::GetTail()
{
    if (m_nUsed == 0) {
        return NULL;
    }

    return m_pCells + m_nTail;
}

int CMemRing::Commit(Cell* pCell)
{
    int nResult = S_OK;
    
    if (!pCell) {
        return E_FAIL;
    }
    
    CAutoLock cObjectLock(&m_csLock);
    
    if (m_nHead == m_nTail) {
        if (m_nUsed > 0) {
            ++m_nTail;  --m_nUsed;
            m_nTail = (m_nTail < m_nCount) ? m_nTail : 0;
            return nResult;
        }
    }
    
    if (pCell->nID == m_nHead) {
        ++m_nHead;  ++m_nUsed;
        m_nHead = (m_nHead < m_nCount) ? m_nHead : 0;
    } else if (pCell->nID == m_nTail) {
        ++m_nTail;  --m_nUsed;
        m_nTail = (m_nTail < m_nCount) ? m_nTail : 0;
    } else {
        nResult = E_FAIL;
    }
    
    return nResult;
}

UINT CMemRing::GetSize()
{
    CAutoLock cObjectLock(&m_csLock);
    return m_nUsed;
}

UINT CMemRing::GetLength() const
{
    return m_nCount;
}

int CMemRing::Alloc(UINT nSize, UINT nCount)
{
    int nResult = S_OK;
    
    if (m_pCells || m_pBuffer) {
        Free();
    }
    
    m_nCount   = nCount;
    m_pCells   = new Cell[nCount];
    m_nBufSize = nSize * nCount;
    m_pBuffer  = new BYTE[m_nBufSize];
    memset(m_pBuffer, 0, m_nBufSize * sizeof(BYTE));
    
    for (int i = 0; i < m_nCount; ++i) {
        m_pCells[i].nID = i;
        m_pCells[i].nSize = nSize;
        m_pCells[i].pData = m_pBuffer + nSize * i;
    }
    
    return nResult;
}

int CMemRing::Free()
{
    int nResult = S_OK;
    
    if (m_pCells) {
        delete[] m_pCells;
        m_pCells = NULL;
    }
    
    if (m_pBuffer) {
        delete[] m_pBuffer;
        m_pBuffer = NULL;
    }
    
    m_nHead = 0;
    m_nTail = 0;
    m_nUsed = 0;
    
    return nResult;
}

