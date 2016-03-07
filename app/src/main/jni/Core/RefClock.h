//
//  RefClock.h
//  QVOD
//
//  Created by bigbug on 11-11-27.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_RefClock_h
#define QVOD_RefClock_h

#include "DependencyObject.h"
#include "Utinities.h"
using ios_qvod_player::CLock;

struct IReferenceClock
{
    virtual BOOL IsStarted() = 0;
    virtual LONGLONG GetTime() = 0;
    virtual void UpdateStartTime() = 0;
};

struct IReferenceClockControl
{
    virtual void Play() = 0;
    virtual void Pause() = 0;
    virtual void BeginWaiting() = 0;
    virtual void EndWaiting() = 0;
    virtual void CancelWaiting() = 0;
    virtual void Seek() = 0;
    virtual void Reset() = 0;
};

class CReferenceClock : public IReferenceClock, 
                        public IReferenceClockControl,
                        public CLock, 
                        public CDependencyObject
{
public:
    CReferenceClock();
    virtual ~CReferenceClock();
    
    // IReferenceClock
    virtual BOOL IsStarted();
    virtual LONGLONG GetTime();
    virtual void UpdateStartTime();
    
    // IReferenceClockControl
    virtual void Play();
    virtual void Pause();
    virtual void BeginWaiting();
    virtual void EndWaiting();
    virtual void CancelWaiting();
    virtual void Seek();
    virtual void Reset();
    
protected:
    virtual LONGLONG GetCurTime() = 0;
    
    LONGLONG GetStartTime();
    void SetStartTime(LONGLONG llStartTime);
    
    int GetState();
    int SetState(int nAdd, int nRemove);
    
    UINT  m_nState;
    
    LONGLONG   m_llStartTime;
    LONGLONG   m_llPausedTime;
};

class CSystemRefClock : public CReferenceClock
{
    CSystemRefClock();
    virtual ~CSystemRefClock();
    
public:
    static CSystemRefClock* GetInstance();
    
protected:
    virtual LONGLONG GetCurTime();
    
    float  m_fTimebase;
};

#endif
