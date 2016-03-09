//
//  BaseObject.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_BaseObject_h
#define QVOD_BaseObject_h

#include "Config.h"
#include "SysConsts.h"

#ifdef PLAYER_DEBUG
    #include <cassert>
    #ifdef ANDROID
        #include <android/log.h>
    #endif
#endif

//struct IUnknown
//{
//    virtual QueryInterface(REFIID riid, void **ppvObject) = 0;
//    virtual AddRef();
//    virtual Release();
//}

class CBaseObject
{
#ifdef PLAYER_DEBUG    
    #ifdef ANDROID
        #define Log(format, args...) \
        { \
            __android_log_print(ANDROID_LOG_INFO, "qplayer", format, ##args);\
        }
    #else
        #ifdef LOG_TO_FILE
            #define Log(format, args...) \
            { \
                extern std::string strPathLog; \
                FILE* fp = fopen(strPathLog.c_str(), "a+"); \
                fprintf(fp, format, ##args); \
                fclose(fp); \
            }
        #else
            #define Log(format, args...) \
            { \
                printf(format, ##args); \
            }
        #endif
    #endif \

    #define AssertValid(condition) \
    { \
        assert(condition); \
    }    
#else
    #define Log(format, args...)
    #define AssertValid(bCondition)
#endif
};

#endif
