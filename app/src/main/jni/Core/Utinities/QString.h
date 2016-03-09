//
//  QString.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_QString_h
#define QVOD_QString_h


#include "BaseTypes.h"

namespace ios_qvod_player
{

#ifdef WIN32 /***************** WIN32 *****************/


#include <string.h>
//#include <string>
#include <stdlib.h>
#include <tchar.h>

#define QVOD_ATOI			atoi
#define QVOD_ATOI64			_atoi64

/* TCHAR */
typedef LPVOID				LPVOID;
typedef LPCVOID 			LPCVOID;
typedef LPCSTR				LPCSTR;
typedef LPCTSTR				LPCTSTR;
typedef LPTSTR				LPTSTR;
typedef LPSTR				LPSTR;
typedef LPCWSTR				LPCWSTR;


#else /***************** posix *****************/


#include <string.h>
//#include <string>
#include <stdlib.h>
#include <wchar.h>
#include <ctype.h>
//#include <iconv.h>

#define QVOD_ATOI			atoi
#define QVOD_ATOI64			atoll

typedef const wchar_t*		LPCWSTR;
    
#ifdef _UNICODE  /* _UNICODE  */

typedef wchar_t 			TCHAR;
typedef void*				LPVOID;
typedef const void* 		LPCVOID;
typedef const char*			LPCSTR;
typedef const wchar_t*		LPCTSTR;
typedef wchar_t*			LPTSTR;
typedef char*				LPSTR;

#else /* non _UNICODE  */

typedef char 				TCHAR;
typedef void*				LPVOID;
typedef const void* 		LPCVOID;
typedef const char*			LPCSTR;
typedef const char*			LPCTSTR;
typedef char*				LPTSTR;
typedef char*				LPSTR;

#endif /* non _UNICODE  end */


typedef const wchar_t*		QVOD_LPCWSTR;


char* itoa(int value, char* str, int radix);
int memcpy_s(void *dest, size_t numberOfElements, const void *src, size_t count);
int strcpy_s(char *dest, size_t numberOfElements, const char *src);

#define sprintf_s snprintf /* need modify */


#endif /***************** posix end *****************/



int strupr(char *str);
int lstrlenW(LPCWSTR str);
int lstrlen(LPCTSTR str);
int WriteToOddAddress(unsigned char *oddAddress, unsigned char *data, int length);
int ReadFromOddAddress(unsigned char *oddAddress, unsigned char *data, int length);



/* INI file */
void LTruncate(char *pString, char *szFill);
void RTruncate(char *pString, char *szFill);
DWORD GetPrivateProfileString(LPCTSTR lpAppName,
									   LPCTSTR lpKeyName,
									   LPCTSTR lpDefault,
									   LPTSTR lpReturnedString,
									   DWORD nSize,
									   LPCTSTR lpFileName);
UINT GetPrivateProfileInt(LPCTSTR lpAppName,
								   LPCTSTR lpKeyName,
								   int nDefault,
								   LPCTSTR lpFileName);
BOOL WritePrivateProfileString(LPCTSTR lpAppName,
                                        LPCTSTR lpKeyName,
                                        LPCTSTR lpString,
                                        LPCTSTR lpFileName);

} /* end of namespace ios_qvod_player */

#endif
