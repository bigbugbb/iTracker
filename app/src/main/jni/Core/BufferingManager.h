//
//  BufferingManager.h
//  QVOD
//
//  Created by bigbug on 11-11-25.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_BufferingManager_h
#define QVOD_BufferingManager_h

#include <vector>
using std::vector;

#include "DependencyObject.h"
#include "RefClock.h"
#include "CallbackManager.h"
using ios_qvod_player::CThread;
//using ios_qvod_player::CLock;

struct IBufferingProbe;

class CBufferingManager : public CThread,
                          public CDependencyObject
{
public:
    CBufferingManager();
    virtual ~CBufferingManager();
    
    static CBufferingManager* GetInstance();
    
    void Initialize();
    void Reset();
//    void ForceBuffering();
    void SetProbe(IBufferingProbe* pDetector);
    void SetBufferSize(int nBufSize);
    void UpdateBuffering(int nRecvSize);
    
protected:
    BOOL IsNeedBuffering();
    BOOL IsBuffering();
    
    int BeginBuffering();
    int OnBuffering();
    int EndBuffering();
    
    virtual THREAD_RETURN ThreadProc();
    
    int   m_nBufSize;
    int   m_nCurSize;
    
    float m_fProgress;
    float m_fPart1;
    float m_fPart1Progress;
    float m_fPart2;
    float m_fPart2Progress;
    float m_fPart3;
    float m_fPart3Progress;
    
    vector<IBufferingProbe*> m_vecBufProbes;
    
    struct timeval m_tmLast;

private:
    int   m_nWait;
    BOOL  m_bBuffering;
    CLock m_csBuffering;
    
    CallbackData  m_cbdBeg;
    CallbackData  m_cbdBuf;
    CallbackData  m_cbdEnd;
    CallbackData  m_cbdSpd;
};

#endif
