//
//  SamplePool.h
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_SamplePool_h
#define QVOD_SamplePool_h

#include "ISamplePool.h"
#include "MemRing.h"
#include "DependencyObject.h"

class CMediaSample;

class CSamplePool : public ISamplePool,
                    public CDependencyObject
{
public:
    CSamplePool();
    virtual ~CSamplePool();
    
    int Size();
    int Flush();
    int Update(const CMediaSample& sample);
    
    // ISamplePool
    virtual int GetSize();
    virtual int GetEmpty(CMediaSample& sample);
    virtual int Commit(const CMediaSample& sample);
    virtual int GetUnused(CMediaSample& sample);
    virtual int Recycle(CMediaSample& sample);
    virtual int SetProperties(POOL_PROPERTIES *pRequest, 
                              POOL_PROPERTIES *pActual);
    
private:
    BYTE*     m_pBuf;
    CMemRing  m_ring;
};

#endif
