//
//  BaseTypes.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_BaseTypes_h
#define QVOD_BaseTypes_h

//namespace ios_qvod_player
//{

#ifdef WIN32 /* WIN32 */

#include <windows.h>
#include <stdio.h>


#define WINAPI			WINAPI
#define INFINITE		INFINITE
#define MAX_PATH		MAX_PATH
#define TRUE			TRUE
#define FALSE			FALSE
#define IN				IN
#define OUT             OUT

typedef BOOL				BOOL;
typedef BYTE				BYTE;
typedef unsigned char		UCHAR;
typedef int8				INT8;
typedef uint8				UINT8;
typedef int16				INT16;
typedef uint16				UINT16;
typedef WORD				WORD; /* 16-bit */
typedef USHORT				USHORT;
typedef DWORD				DWORD; /* 32-bit */
typedef int					INT;
typedef UINT				UINT;
typedef int32				INT32;
typedef uint32				UINT32;
tppedef LONG				LONG;
typedef ULONG				ULONG;
typedef __int64				INT64;
typedef uint64				UINT64;
typedef LONGLONG			LONGLONG;
typedef ULONGLONG			ULONGLONG;

typedef LARGE_INTEGER		LARGE_INTEGER;
typedef PLARGE_INTEGER		PLARGE_INTEGER;
typedef ULARGE_INTEGER		ULARGE_INTEGER;
typedef PULARGE_INTEGER		PULARGE_INTEGER;

typedef GUID				GUID;


#else /* posix */


#include <stdio.h>
#include <sys/types.h>
#include <inttypes.h>
#include <stdlib.h>


#define WINAPI
#define INFINITE		0
#define MAX_PATH		260
#define TRUE			1
#define FALSE			0
#define IN
#define OUT

typedef int					BOOL;
typedef unsigned char		BYTE;
typedef unsigned char		UCHAR;
typedef int8_t				INT8;
typedef uint8_t				UINT8;
typedef int16_t				INT16;
typedef uint16_t			UINT16;
typedef unsigned short		WORD; /* 16-bit */
typedef unsigned short		USHORT;
typedef unsigned long		DWORD; /* 32-bit */
typedef int					INT;
typedef unsigned int		UINT;
typedef int32_t				INT32;
typedef uint32_t			UINT32;
typedef long				LONG;
typedef unsigned long		ULONG;
typedef int64_t				INT64;
typedef int64_t				INT64_C;
typedef uint64_t			UINT64;
typedef uint64_t			UINT64_C;
typedef int64_t				LONGLONG;
typedef uint64_t			ULONGLONG;

typedef union _LARGE_INTEGER {
	struct {
		DWORD LowPart;
		LONG  HighPart;
	};
	struct {
		DWORD LowPart;
		LONG  HighPart;
	} u;
	LONGLONG QuadPart;
} LARGE_INTEGER, *PLARGE_INTEGER;

typedef union _ULARGE_INTEGER {
	struct {
		DWORD LowPart;
		DWORD HighPart;
	} ;
	struct {
		DWORD LowPart;
		DWORD HighPart;
	} u;
	ULONGLONG QuadPart;
} ULARGE_INTEGER, *PULARGE_INTEGER;


typedef struct _GUID {
    DWORD Data1;
    WORD  Data2;
    WORD  Data3;
    BYTE  Data4[8];
} GUID;

BOOL operator<(const GUID& left, const GUID& right);
BOOL operator>(const GUID& left, const GUID& right);
BOOL operator==(const GUID& left, const GUID& right);

#endif /* posix end */




#define QVOD_OK    		0
#define QVOD_ERROR 		-1
#define QVOD_FOUND 		-2
#define QVOD_NOFOUND 	-3
#define QVOD_EROFS		-4

#define MIN2(a,b) (((a)>(b))?(b):(a))
#define MAX2(a,b) (((a)>(b))?(a):(b))


/* QVOD_DEBUG */
#ifdef SDEBUG
#define QVOD_DEBUG(format,args...) \
printf(format,##args)
#else
#define QVOD_DEBUG(format,args...)
#endif


#ifdef SDEBUG1
#define QVOD_DEBUG1(format,args...) \
printf(format,##args)
#else
#define QVOD_DEBUG1(format,args...)
#endif

//}

#endif
