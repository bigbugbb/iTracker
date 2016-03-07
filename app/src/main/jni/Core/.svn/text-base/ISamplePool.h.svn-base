//
//  ISamplePool.h
//  QVOD
//
//  Created by bigbug on 11-11-9.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_ISamplePool_h
#define QVOD_ISamplePool_h

#include "Utinities.h"

class CMediaSample;

struct POOL_PROPERTIES
{
    UINT    nSize;
    UINT    nCount;
    UINT    nAlign;
};

struct ISamplePool
{
    virtual int GetSize() = 0;
    virtual int GetEmpty(CMediaSample& sample) = 0;
    virtual int Commit(const CMediaSample& sample) = 0;
    virtual int GetUnused(CMediaSample& sample) = 0;
    virtual int Recycle(CMediaSample& sample) = 0;
    virtual int SetProperties(POOL_PROPERTIES *pRequest, 
                              POOL_PROPERTIES *pActual) = 0;
};



#endif
