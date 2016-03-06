//
//  Thread.cpp
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>

#include "String.h"
#include "Thread.h"


namespace ios_qvod_player
{

CThread::CThread()
{
    m_bCreate = FALSE;
    m_bRun    = FALSE;
}

CThread::~CThread()
{
    Close();
}
    
int CThread::Create()
{
    Reset();
    if (!m_bCreate) {
        if (CreateThread(&m_tid, InitialThreadProc, this)) {            
            return QVOD_ERROR;
        }
        m_bCreate = TRUE;
    }
    // 由于QVEvent默认为无信号，线程进入loop后将先在Wait处被挂起
    
    return QVOD_OK;
}

int CThread::Close()
{
    if (!m_bRun) {
        return QVOD_OK;
    }
    m_bRun = FALSE;
    
    if (m_bCreate) {
        Signal(); // make sure the thread is deblocked
        pthread_join(m_tid, NULL);
        m_bCreate = FALSE;
    }
    //Reset();
    
    return QVOD_OK;
}
    
int CThread::Start()
{
    if (m_bRun) {
        return QVOD_OK;
    }
    
    m_bRun = TRUE;
    Signal();
    
    return QVOD_OK;
}

THREAD_RETURN CThread::InitialThreadProc(void* arg)
{
    THREAD_RETURN ret;
    CThread* pThread = static_cast<CThread*>(arg);

    if (pThread) {
        pThread->Wait();
        ret = pThread->ThreadProc();
    }
    
    return ret;
}

/* tid and arg may be NULL */
int CreateThread(thread_t *tid, routine_pt routine, void *arg)
{
#ifdef WIN32 /* WIN32 */
    
	thread_t res;
    
	/* ignore tid */
	res = _beginthread(routine, 0, arg);
	if (res == -1) {
		/* error */
		return QVOD_ERROR;
	}
    
	/* ok */
	if (tid != NULL) {
		*tid = res;
	}
    
#else /* posix */
    
	int res;
	thread_t tid_tmp;
	pthread_attr_t tattr;
    
	res = pthread_attr_init(&tattr);
	if (res != 0) {
		/* error */
		QVOD_DEBUG("pthread_attr_init error\n");
		return QVOD_ERROR;
	}
    
//	res = pthread_attr_setdetachstate(&tattr, PTHREAD_CREATE_DETACHED);
//	if(res != 0) {
//		/* error */
//		QVOD_DEBUG("pthread_attr_setdetachstate error\n");
//		return QVOD_ERROR;
//	}
    
	res = pthread_create(&tid_tmp, &tattr, routine, arg);
	if (res != 0) {
		/* error */
		QVOD_DEBUG("QvodCreateThread return %d, error: %d\n", res, errno);
		return QVOD_ERROR;
	}
    
	res = pthread_attr_destroy(&tattr);
	if (res != 0) {
		/* error */
		QVOD_DEBUG("pthread_attr_destroy error\n");
		return QVOD_ERROR;
	}
    
	/* ok */
	if (tid != NULL) {
		*tid = tid_tmp;
	}
    
#endif /* posix end */
    
	return QVOD_OK;
}

int WaitForThread(thread_t tid, void **value_ptr)
{
	int res;
    
#ifdef WIN32
    
	/*
	 * win32 do not check the return value of the thread,
	 * so you should set value_ptr NULL.
	 */
	res = WaitForSingleObject(tid, INFINITE);
	if (res == WAIT_FAILED) {
		return QVOD_ERROR;
	}
    
#else /* posix */
    
	res = pthread_join(tid, value_ptr);
	if (res != 0) {
		return QVOD_ERROR;
	}
    
#endif
    
	return QVOD_OK;
}


int WaitForMultiThreads(int nCount, const thread_t *handles)
{
	int ret = QVOD_OK;
	//(void*)&ret;
    
#ifdef WIN32 /* WIN32 */
    
	int res;
    
	res = WaitForMultipleObjects(nCount, handles, true, INFINITE);
	if (res == WAIT_FAILED) {
		return QVOD_ERROR;
	}
    
#else /* posix */
    
	int res, i;
    
	for (i = 0; i < nCount; ++i) {
        
		res = pthread_join(handles[i], NULL);
		if (res != 0) {
			ret = QVOD_ERROR;
		}
	}
    
#endif /* posix end */
    
	return ret;
}

} /* end of namespace ios_qvod_player */