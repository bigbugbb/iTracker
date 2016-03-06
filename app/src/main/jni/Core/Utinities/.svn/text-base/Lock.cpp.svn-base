//
//  Lock.cpp
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "Lock.h"

namespace ios_qvod_player
{

CCriticalSection::CCriticalSection(thread_lock_t *lock)
{
	m_lock = lock;
	m_blocked =	false;
}

CCriticalSection::~CCriticalSection()
{
	if (m_lock != NULL && m_blocked) {
        
#ifdef WIN32
		LeaveCriticalSection(m_lock);
#else /* posix */
		pthread_mutex_unlock(m_lock);
#endif
        
		m_lock = NULL;
		m_blocked = false;
	}
}

int CCriticalSection::Lock()
{
	int res = QVOD_ERROR;
    
	if (m_lock != NULL) {
        
#ifdef WIN32 /* WIN32 */
        
		EnterCriticalSection(m_lock); /* return void */
		res = QVOD_OK;
		m_blocked = true;
        
#else /* posix */
        
		res = pthread_mutex_lock(m_lock);
		switch(res) {
        case 0:
            /* success */
            m_blocked = true;
            res = QVOD_OK;
            break; 
        default:
            /* failed */
            res = QVOD_ERROR;
		}
#endif /* posix end */
	}
    
	return res;
}

int CCriticalSection::UnLock()
{
	int res = QVOD_ERROR;
    
	if (m_lock != NULL) {
        
#ifdef WIN32 /* WIN32 */
        
		LeaveCriticalSection(m_lock); /* return void */
		res = QVOD_OK;
		m_blocked = false;
        
#else /* posix */
        
		res = pthread_mutex_unlock(m_lock);
		switch(res) {
        case 0:
            /* success */
            m_blocked = false;
            res = QVOD_OK;
            break;  
        default:
            /* failed */
            res = QVOD_ERROR;
		}
#endif /* posix end */
	}
    
	return res;
}


int CCriticalSection::TryLock()
{
	int res = QVOD_ERROR;
    
	if (m_lock != NULL) {
        
#ifdef WIN32 /* WIN32 */
        
		res = TryEnterCriticalSection(m_lock); /* return bool */
		switch (res) {
        case 0:
            /* another thread already owns */
            res = QVOD_ERROR;
            break;
        default:
            /* successfully entered */
            res = QVOD_OK;
            m_blocked = true;
		}
        
#else /* posix */
        
		res = pthread_mutex_trylock(m_lock);
		switch (res) {
        case 0:
            /* successfully entered */
            res = QVOD_OK;
            m_blocked = true;
            break;
        default:
            /* failed */
            res = QVOD_ERROR;
		}
        
#endif /* posix end */
	}
    
	return res;
}


bool CCriticalSection::IsLocked()
{
	return m_blocked;
}






/* func */
int	InitializeCS(thread_lock_t *lock)
{
#ifdef WIN32
    
	InitializeCriticalSection(lock); /* return void */
    
#else /* posix */
    
	int res;
	pthread_mutexattr_t lock_attr;
    
	res = pthread_mutexattr_init(&lock_attr);
	if (res != 0) {
		/* error */
		return QVOD_ERROR;
	}
    
	/* set lock recursion */
	res = pthread_mutexattr_settype(&lock_attr, PTHREAD_MUTEX_RECURSIVE);
	if (res != 0) {
		/* error */
		pthread_mutexattr_destroy(&lock_attr);
		return QVOD_ERROR;
	}
    
	res = pthread_mutex_init(lock, &lock_attr);
	if (res != 0) {
		/* error */
		pthread_mutexattr_destroy(&lock_attr);
		return QVOD_ERROR;
	}
    
	res = pthread_mutexattr_destroy(&lock_attr);
	if (res != 0) {
		/* error */
		return QVOD_ERROR;
	}
    
#endif
    
	return QVOD_OK;
}

int DestroyCS(thread_lock_t *lock)
{
#ifdef WIN32
    
	DeleteCriticalSection(lock); /* return void */
    
#else /* posix */
    
	int res;
	res = pthread_mutex_destroy(lock);
	if (res != 0) {
		/* error */
		return QVOD_ERROR;
	}
    
#endif
    
	return QVOD_OK;
}

int CSLock(thread_lock_t *lock)
{
	if (lock == NULL) {
		return QVOD_ERROR;
	}
    
	int res = QVOD_ERROR;
    
	if (lock != NULL) {
        
#ifdef WIN32 /* WIN32 */
        
		EnterCriticalSection(lock); /* return void */
		res = QVOD_OK;
        
#else /* posix */
        
		res = pthread_mutex_lock(lock);
		switch(res) {
        case 0:
            /* success */
            res = QVOD_OK;
            break;
        default:
            /* failed */
            res = QVOD_ERROR;
		}
#endif /* posix end */
	}
    
	return res;
}

int CSTrylock(thread_lock_t *lock)
{
	if(lock == NULL) {
		return QVOD_ERROR;
	}
    
	int res = QVOD_ERROR;
    
	if(lock != NULL) {
        
#ifdef WIN32 /* WIN32 */
        
		res = TryEnterCriticalSection(lock); /* return bool */
		switch(res) {
        case 0:
            /* another thread already owns */
            res = QVOD_ERROR;
            break;    
        default:
            /* successfully entered */
            res = QVOD_OK;
		}
        
#else /* posix */
        
		res = pthread_mutex_trylock(lock);
		switch(res) {
        case 0:
            /* successfully entered */
            res = QVOD_OK;
            break;
        default:
            /* failed */
            res = QVOD_ERROR;
		}
        
#endif /* posix end */
	}
    
	return res;
}

int CSUnlock(thread_lock_t *lock)
{
	if (lock == NULL) {
		return QVOD_ERROR;
	}
    
	int res = QVOD_ERROR;
    
	if (lock != NULL) {
        
#ifdef WIN32 /* WIN32 */
        
		LeaveCriticalSection(lock); /* return void */
		res = QVOD_OK;
        
#else /* posix */
        
		res = pthread_mutex_unlock(lock);
		switch(res) {
        case 0:
            /* success */
            res = QVOD_OK;
            break;  
        default:
            /* failed */
            res = QVOD_ERROR;
		}
#endif /* posix end */
	}
    
	return res;
}




CLock::CLock()
{
    InitializeCS(&m_cs);
}

CLock::~CLock()
{
	DestroyCS(&m_cs);
}

void CLock::Lock()
{
	CSLock(&m_cs);
}

void CLock::Unlock()
{
	CSUnlock(&m_cs);
}

///////////////////////////////////////////////////////////////////////////////
CAutoLock::CAutoLock(CLock *pLock) : m_pLock(pLock)
{
	m_pLock->Lock();
}

CAutoLock::~CAutoLock()
{
	m_pLock->Unlock();
}


#if 0

#ifdef EMBEDED /* EMBEDED */

int AtomAdd(atom_t *value)
{
	++(*value);
	return QVOD_OK;
}

int AtomDec(atom_t *value)
{
	--(*value);
	return QVOD_OK;
}

#else /* non-EMBEDED */

int AtomAdd(atom_t *value)
{
#ifdef WIN32 /* WIN32 */
    
	InterlockedIncrement(value);
    
#else /* posix */
    
	__sync_add_and_fetch(value, 1);
    
#endif /* posix end */
    
	return QVOD_OK;
}

int AtomDec(atom_t *value)
{
#ifdef WIN32 /* WIN32 */
    
	InterlockedDecrement(value);
    
#else /* posix */
    
	__sync_sub_and_fetch(value, 1);
    
#endif /* posix end */
    
	return QVOD_OK;
}

#endif /* non-EMBEDED end */

#endif /* if 0/1 */

} /* end of namespace ios_qvod_player */




