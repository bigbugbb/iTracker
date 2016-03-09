//
//  Event.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_Event_h
#define QVOD_Event_h

#include <sys/time.h>
#include <errno.h>
#include <pthread.h>

#include "BaseTypes.h"

//namespace ios_qvod_player
//{
    
class CEvent
{
public:
    CEvent(BOOL bManualReset = FALSE)
    {
        m_bManualReset = bManualReset;
        pthread_cond_init(&m_cond, NULL);
        pthread_mutex_init(&m_mutex, NULL);
        m_bWait   = FALSE;
        m_bSignal = FALSE;
    }
    
    virtual ~CEvent()
    {
        pthread_mutex_destroy(&m_mutex);
        pthread_cond_destroy(&m_cond);
    }
    
    // Blocks until the event is signaled, or until a time-out occurs.
    BOOL Wait()
    {
        pthread_mutex_lock(&m_mutex);
        while (!m_bSignal) {
            m_bWait = TRUE;
            pthread_cond_wait(&m_cond, &m_mutex);
        }
        m_bWait = FALSE;
        if (!m_bManualReset)
            Reset(); // auto reset the event to nonsignal
        pthread_mutex_unlock(&m_mutex);
        return TRUE;
    }
    
    BOOL Wait(LONGLONG llMicroseconds) // 分开来写逻辑清楚一些
    {
        if (llMicroseconds < 0) {
            llMicroseconds = 0;
        }
        
        BOOL bInTime = TRUE;
        
        pthread_mutex_lock(&m_mutex);
        while (!m_bSignal) {
            struct timeval now;
            struct timespec timeout;
            
            gettimeofday(&now, NULL);
            
            LONG nsec = (now.tv_usec + (LONG)(llMicroseconds % 1000000)) * 1000;
            
            timeout.tv_sec = now.tv_sec + (LONG)(llMicroseconds / 1000000);
            if (nsec >= 1000000000) {
                ++timeout.tv_sec;
                timeout.tv_nsec = nsec - 1000000000;
            } else {
                timeout.tv_nsec = nsec;
            }
            
            m_bWait = TRUE;
            if (pthread_cond_timedwait(&m_cond, &m_mutex, &timeout) == ETIMEDOUT) {
                bInTime = FALSE;
            }
            m_bWait = FALSE;
            m_bSignal = TRUE;
        }
        
        if (!m_bManualReset)
            Reset(); // auto reset the event to nonsignal
        pthread_mutex_unlock(&m_mutex);
        return bInTime;
    }
    
    BOOL Check()
    {
        BOOL bWait;
        
        pthread_mutex_lock(&m_mutex);
        bWait = m_bWait;
        pthread_mutex_unlock(&m_mutex);
        
        return bWait;
    }
    
    // signal the event
    void Signal()
    {
        pthread_mutex_lock(&m_mutex);
        m_bSignal = TRUE;
        pthread_cond_signal(&m_cond);
        pthread_mutex_unlock(&m_mutex);
    }
    
protected:
    void Reset()
    {
        m_bSignal = FALSE;
    }
    
    BOOL            m_bWait;
    BOOL            m_bSignal;
    BOOL            m_bManualReset;
    
    pthread_cond_t  m_cond;
    pthread_mutex_t m_mutex;
};
    
//}

#endif
