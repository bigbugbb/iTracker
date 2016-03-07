//
//  Pools.h
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#ifndef QvodPlayer_Pools_h
#define QvodPlayer_Pools_h

#include "Frame.h"
#include "../SamplePool.h"

class CPacketPool : public CSamplePool
{
public:
    int Flush();
};

#include <map>
#include <list>

class CPacketPoolList
{
public:
    CPacketPoolList();
    
    void SetCurPool(int nTrackID);
    CPacketPool* GetCurPool();
    CPacketPool* GetPoolFromTrackID(int nTrackID);
    int  GetCurPoolSize();
    
    void Add(int nTrackID, CPacketPool* pPool);
    void Remove(int nTrackID);
    void Flush();
    void Clear();
    
    void EnableAutoRelease(BOOL bAutoRelease);
    
protected:
    BOOL         m_bAutoRelease;
    CLock        m_csPool;
    CPacketPool* m_pCurPool;
    
    std::list<CPacketPool*>     m_listPools;
    std::map<int, CPacketPool*> m_mapPools;
};

const int FRAME_POOL_SIZE = 6;

class CFramePool : public CSamplePool
{
public:
    CFramePool();
    virtual ~CFramePool();
    
    int Flush();
    int Reset();
protected:
    CFrame  m_Frames[FRAME_POOL_SIZE];
};

const int PCM_BUFFER_COUNT = 30;
const int PCM_BUFFER_SIZE = AVCODEC_MAX_AUDIO_FRAME_SIZE * PCM_BUFFER_COUNT * 2;

class CPcmPool : public CSamplePool
{
public:
    CPcmPool();
    virtual ~CPcmPool();
    
    int Flush();
    int GetSize();
    int Commit(const CMediaSample& sample);
    void Consume(int nConsumed);
    
protected:
    int     m_nSize;
    CLock   m_csSize;
    
    // I used to adopt the following DECLARE_ALIGNED to alloc a 16-byte-aligned buffer,
    // which works well on iOS. However, it has no effect on android, perhaps
    // this DECLARE_ALIGNED is not supported by the corresponding c++ compiler in NDK, 
    // or perhaps that I have the incorrect configuration in the makefile. After all, 
    // I decide to allocate the aliged memory dynamically all by myself.
//    DECLARE_ALIGNED(DEFAULT_ALIGN_BYTE, BYTE, m_PCMs)[PCM_BUFFER_SIZE];
    BYTE*   m_pPCMs;
};

#endif
