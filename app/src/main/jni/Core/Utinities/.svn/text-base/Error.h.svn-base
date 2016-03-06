//
//  Error.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_Error_h
#define QVOD_Error_h

//namespace ios_qvod_player
//{

#ifdef WIN32

#include <windows.h>
#include <assert.h>

#define WAIT_TIMEOUT	WAIT_TIMEOUT
#define WAIT_FAILED     WAIT_FAILED

typedef int err_t;

#else /* posix */

#include <errno.h>
#include <assert.h>

#define WAIT_TIMEOUT	ETIMEDOUT
#define WAIT_FAILED     -1

typedef int err_t;

//extern int errno;

#endif


/* func */
err_t GetLastError();
    
//} /* end of namespace ios_qvod_player */

#endif
