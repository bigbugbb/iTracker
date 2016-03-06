//
//  Lock.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_Lock_h
#define QVOD_Lock_h

    
#include "BaseTypes.h"
    
    
#ifdef WIN32  /* WIN32 */

typedef	CRITICAL_SECTION thread_lock_t;
typedef	volatile long atom_t;

#else /* posix */

#include <pthread.h>

typedef pthread_mutex_t thread_lock_t;
typedef volatile long atom_t;

#endif /* posix end */

namespace ios_qvod_player
{
/* CCriticalSection */
class CCriticalSection
{
public:
    
	CCriticalSection(thread_lock_t *lock);
    virtual ~CCriticalSection();
	int Lock();
	int TryLock();
	int UnLock();
	bool IsLocked();
    
private:
    
	thread_lock_t *m_lock;
	bool m_blocked;
};


/* qvod CLock */
class CLock
{
public:
	CLock();
	virtual ~CLock();
    
	void Lock();
	void Unlock();
    
private:
	thread_lock_t  m_cs;
};

class CAutoLock
{
public:
	CAutoLock(CLock* pLock);
	virtual ~CAutoLock();
    
private:
	CLock*	m_pLock;
};


/* func */
int	InitializeCS(thread_lock_t *lock);
int DestroyCS(thread_lock_t *lock);
int CSLock(thread_lock_t *lock);
int CSTrylock(thread_lock_t *lock);
int CSUnlock(thread_lock_t *lock);


/* qvod atom operation */
int AtomAdd(atom_t *value);
int AtomDec(atom_t *value);

} /* end of namespace ios_qvod_player */

#endif
