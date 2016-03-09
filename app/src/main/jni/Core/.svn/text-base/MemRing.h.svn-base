//
//  MemRing.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_MemRing_h
#define QVOD_MemRing_h

#include "Utinities.h"
using ios_qvod_player::CLock;

struct Cell
{
    UINT  nID;
    BYTE* pData;
    UINT  nSize;
};

class CMemRing
{
public:
    CMemRing();
    CMemRing(UINT nSize, UINT nCount);
    virtual ~CMemRing();
    
    int Alloc(UINT nSize, UINT nCount);
    int Free();
    
    Cell* GetHead();
    Cell* GetTail();
    int Commit(Cell* pCell);
    
    UINT GetSize();
    UINT GetLength() const;
    
protected:
    UINT    m_nHead;
    UINT    m_nTail;
    UINT    m_nUsed;
    UINT    m_nCount;
    Cell*   m_pCells;
    
    BYTE*   m_pBuffer;
    UINT    m_nBufSize;
    
    CLock   m_csLock;
};

#endif
