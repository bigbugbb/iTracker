//
//  RefClock.cpp
//  QVOD
//
//  Created by bigbug on 11-11-27.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "RefClock.h"
#ifdef ANDROID
    #include <sys/time.h>
#else
    #include <mach/mach_time.h>
    #include <mach/mach.h>
#endif
using namespace ios_qvod_player;

#define CLK_STATE_STARTED   (1)
#define CLK_STATE_PLAYING   (1 << 1)
#define CLK_STATE_PAUSED    (1 << 2)
#define CLK_STATE_WAITING   (1 << 3)
#define CLK_STATE_SEEKED    (1 << 4)
#define CLK_STATE_NONE      (1 << 5)

CReferenceClock::CReferenceClock()
{
    Reset();
}

CReferenceClock::~CReferenceClock()
{
    
}

inline BOOL CReferenceClock::IsStarted()
{
    CAutoLock cObjectLock(this);
    
    return GetState() & CLK_STATE_STARTED;
}

inline LONGLONG CReferenceClock::GetTime()
{
    CAutoLock cObjectLock(this);
    
    if (!(m_nState & CLK_STATE_STARTED)) {
        return 0;
    }
    
    if (m_nState & (CLK_STATE_PAUSED | CLK_STATE_WAITING)) {
        return m_llPausedTime - m_llStartTime;
    }
    
    return GetCurTime() - m_llStartTime;
}

void CReferenceClock::UpdateStartTime()
{
    SetStartTime(GetCurTime());
}

inline
int CReferenceClock::GetState()
{
    CAutoLock cObjectLock(this);
    
    return m_nState;
}

inline
int CReferenceClock::SetState(int nAdd, int nRemove)
{
    CAutoLock cObjectLock(this);
    
    m_nState &= ~nRemove;
    m_nState |= nAdd;
    
    return m_nState;
}

void CReferenceClock::Play()
{
    CAutoLock cObjectLock(this);
 
    if (!(m_nState & CLK_STATE_STARTED)) {
        SetStartTime(GetCurTime());
        SetState(CLK_STATE_STARTED | CLK_STATE_PLAYING, CLK_STATE_NONE);
        Log("Play when started\n");
        return;
    }
            
    if (m_nState & CLK_STATE_PAUSED) {
        if (m_nState & CLK_STATE_WAITING) {
            Log("Play when pause & waiting\n");
        } else {
            if (m_nState & CLK_STATE_SEEKED) {
                SetStartTime(GetCurTime());
                SetState(0, CLK_STATE_SEEKED);
                Log("Play when pause & seeked, start time: %lld\n", m_llStartTime);
            } else {
                SetStartTime(GetCurTime() - m_llPausedTime + m_llStartTime);
                Log("Play when pause, start time: %lld\n", m_llStartTime);
            }
        }
        SetState(CLK_STATE_PLAYING, CLK_STATE_PAUSED);
    }
}

void CReferenceClock::Pause()
{
    CAutoLock cObjectLock(this);
    
    if (!(m_nState & CLK_STATE_WAITING)) {
        m_llPausedTime = GetCurTime();
        Log("Pause when playing, pause time: %lld\n", m_llPausedTime);
    } else {
        Log("Pause when waiting\n");
    }
    
    SetState(CLK_STATE_PAUSED, CLK_STATE_PLAYING);
}

void CReferenceClock::BeginWaiting()
{
    CAutoLock cObjectLock(this);
    
    if (!(m_nState & CLK_STATE_STARTED)) {
        m_llPausedTime = GetCurTime();
        SetStartTime(m_llPausedTime);
        SetState(CLK_STATE_STARTED | CLK_STATE_WAITING, CLK_STATE_NONE);
        Log("Begin wait when not started\n");
        return;
    }
    
    if (!(m_nState & CLK_STATE_PAUSED)) {
        m_llPausedTime = GetCurTime();
        //SetStartTime(m_ullPausedTime); // for adjustment
        Log("Begin wait, pause & start time: %lld\n", m_llPausedTime);
    } else {
        Log("Begin wait when paused\n");
    }
    
    SetState(CLK_STATE_WAITING, 0);
}

void CReferenceClock::EndWaiting()
{
    CAutoLock cObjectLock(this);

    if (!(GetState() & CLK_STATE_WAITING)) {
        Log("End wait when no waiting\n");
        return;
    }
    
    if (m_nState & CLK_STATE_PAUSED) {
        Log("End wait when paused\n");
    } else {
        if (m_nState & CLK_STATE_SEEKED) {
            SetStartTime(GetCurTime());
            SetState(0, CLK_STATE_SEEKED);
            Log("End wait when seeked, start time: %lld\n", m_llStartTime);
        } else {
            SetStartTime(GetCurTime() - m_llPausedTime + m_llStartTime);
            Log("End wait when waiting, start time: %lld\n", m_llStartTime);
        }
    }
    
    SetState(0, CLK_STATE_WAITING);
}

void CReferenceClock::CancelWaiting()
{
    CAutoLock cObjectLock(this);
    
    Log("Cancel wait\n");
    
    SetState(0, CLK_STATE_WAITING);
}

void CReferenceClock::Seek()
{
    CAutoLock cObjectLock(this);
    
    if (m_nState & (CLK_STATE_PAUSED | CLK_STATE_WAITING)) {
        SetStartTime(m_llPausedTime);
        SetState(CLK_STATE_SEEKED, 0);
        Log("Seek when paused or waiting\n");
    } else {
        SetStartTime(GetCurTime());
        Log("Seek when playing, start time: %lld\n", m_llStartTime);
    }
}

void CReferenceClock::Reset()
{
    CAutoLock cObjectLock(this);
    
    m_nState = CLK_STATE_NONE;
    m_llStartTime  = 0;
    m_llPausedTime = 0;
    Log("reset reference clock!\n");
}

void CReferenceClock::SetStartTime(LONGLONG llStartTime)
{
    CAutoLock cObjectLock(this);
    
    m_llStartTime = llStartTime;
    SetState(CLK_STATE_STARTED, CLK_STATE_NONE);
    
    //std::cout << "set start time: " << m_llStartTime << std::endl;
}

LONGLONG CReferenceClock::GetStartTime()
{
    CAutoLock cObjectLock(this);
    
    return m_llStartTime;
}

//////////////////////////////////////////////////////
CSystemRefClock::CSystemRefClock() : CReferenceClock()
{
#ifdef ANDROID
    // do nothing now
#else
    int64_t llOneMillion = 1000000;
    mach_timebase_info_data_t timebase_info;
    
    mach_timebase_info(&timebase_info);
    AssertValid(timebase_info.denom != 0);
    m_fTimebase = (float)timebase_info.numer / (timebase_info.denom * llOneMillion);
#endif
}

CSystemRefClock::~CSystemRefClock()
{
    
}

CSystemRefClock* CSystemRefClock::GetInstance()
{
    static CSystemRefClock s_SysRefClock;
    
    return &s_SysRefClock;
}

inline LONGLONG CSystemRefClock::GetCurTime()
{
#ifdef ANDROID
    struct timeval tmNow;
    gettimeofday(&tmNow, NULL);
    return (LONGLONG)tmNow.tv_sec * 1000 + tmNow.tv_usec * 0.001;
#else
    return mach_absolute_time() * m_fTimebase; // milliseconds
#endif
}







