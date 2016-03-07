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
    virtual int SetTimebase(float lfTimebase);
    virtual int SetSampleRate(int nSampleRate);
    virtual int SetChannelCount(int nChannelCount);
    virtual int SetSampleFormat(int nSampleFormat);
    virtual int SetMediaSeekTime(float lfTime);
    virtual int GetMediaCurrentTime(float* pTime);
    virtual int SetMediaStartTime(float lfTime);
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
    int GetInputPool(const GUID& requestor, ISamplePool** ppPool);
    int RespondDispatch(const GUID& sender, int nType, void* pUserData);

    void FillBuffer(BYTE* pBuffer, UINT nDataByteSize);
    LONGLONG EstimateTimestamp(const CMediaSample& sample);
    
    BOOL        m_bEnable;
    BOOL        m_bClose;
    BOOL        m_bSwitch;
    BOOL        m_bInterrupt;
    float       m_fTimebase;
    float       m_fSeekTime;
    
    CEvent      m_ASync;
    float       m_fTSScale;
    int         m_nSampleRate;
    int         m_nChannelCount;
    int         m_nSampleFormat;
    
    LONGLONG    m_llStartPTS;
    LONGLONG    m_llCurrentPTS;
    CPcmPool    m_PcmPool;
    
private:
    void PrepareSeek(BOOL bPrepare = TRUE);
    BOOL IsPreparingSeek();
    
    BOOL        m_bPreSeek;
    CLock       m_csPreSeek;
};

#endif
