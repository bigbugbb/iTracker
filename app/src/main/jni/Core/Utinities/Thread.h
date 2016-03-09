//
//  Thread.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_Thread_h
#define QVOD_Thread_h

#include <pthread.h>

#include "Event.h"

namespace ios_qvod_player 
{

#ifdef WIN32 /* WIN32 */

#include <process.h>

#define THREAD_RETURN void
typedef	unsigned long thread_t;
typedef void(__cdecl *routine_pt)(void*);

#else /* posix */

#include <pthread.h>

#define THREAD_RETURN void*
typedef pthread_t thread_t;
typedef void *(*routine_pt)(void*);

#endif /* posix end */

class CThread : public CEvent
{
public:
    CThread();
    virtual ~CThread();
    
    int  Create();
    int  Close();
    int  Start();

protected:
    static THREAD_RETURN InitialThreadProc(void* arg);                
    virtual THREAD_RETURN ThreadProc() = 0;
    
    BOOL   m_bRun;
    BOOL   m_bCreate;
    thread_t m_tid;
};

/* func */
int CreateThread(thread_t *tid, routine_pt routine, void *arg);
int WaitForThread(thread_t tid, void **value_ptr);
int WaitForMultiThreads(int nCount, const thread_t *handles);
    
} /* end of namespace ios_qvod_player */

#endif
