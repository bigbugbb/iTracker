//
//  AudioRenderer.h
//  QVOD
//
//  Created by bigbug on 11-11-14.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_AudioRenderer_h
#define QVOD_AudioRenderer_h

#include "../MediaObject.h"
#include "Pools.h"
#include "CompInterfaces.h"

class CAudioRenderer : public CLock,
                       public CSink, 
                       public IAudioRenderer
{
public:
    CAudioRenderer(const GUID& guid, IDependency* pDepend, int* pResult);
    virtual ~CAudioRenderer();
    
    // IAudioRenderer
    virtual int Interrupt(BOOL bInterrupt);
    virtual int SetTimebase(double lfTimebase);
    virtual int SetSampleRate(int nSampleRate);
    virtual int SetChannelCount(int nChannelCount);
    virtual int SetSampleFormat(int nSampleFormat);
    virtual int SetMediaSeekTime(double lfTime);
    virtual int GetMediaCurrentTime(double* pTime);
    virtual int SetMediaStartTime(double lfTime);
    virtual int OutputAudio(BYTE* pData, UINT nDataByteSize);
    
protected:
    int Load();
    int WaitForResources(BOOL bWait);
    int Idle();
    int Execute();
    int Pause();
    int BeginFlush();
    int EndFlush();
    int Invalid();
    int Unload();
    int SetEOS();
    int GetSamplePool(const GUID& guid, ISamplePool** ppPool);

    void FillBuffer(BYTE* pBuffer, UINT nDataByteSize);
    LONGLONG EstimateTimestamp(const CMediaSample& sample);
    
    BOOL        m_bEnable;
    BOOL        m_bClose;
    BOOL        m_bInterrupt;
    double      m_lfTimebase;
    double      m_lfSeekTime;
    
    CEvent      m_ASync;
    double      m_lfTSScale;
    int         m_nSampleRate;
    int         m_nChannelCount;
    int         m_nSampleFormat;
    
    LONGLONG    m_llStartPTS;
    LONGLONG    m_llCurrentPTS;
    CPcmPool    m_PcmPool;
};

#endif
