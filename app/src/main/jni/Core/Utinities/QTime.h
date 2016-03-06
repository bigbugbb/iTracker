//
//  Time.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_Time_h
#define QVOD_Time_h

#include "BaseTypes.h"

//namespace ios_qvod_player
//{

#ifdef WIN32 /* WIN32 */

#include <windows.h>
#include <time.h>

#define	QVOD_SLEEP(a) Sleep(a)

#else /* posix */

#include <stdio.h>
#include <time.h>
#include <sys/time.h>
#include <sys/times.h>

#define	SLEEP(a) (usleep(1000*(a))) /* in this way, "a" should smaller than 1000 */

#endif /* posix end */


/* function */
int Sleep(unsigned int milliseconds);
LONGLONG GetTime();
int QueryPerformanceCounter(LARGE_INTEGER *lpPerformanceCount);
int GetTimeStr(char *dst_buf);
    
//} /* end of namespace ios_qvod_player */

#endif
