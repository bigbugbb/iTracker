//
//  IQvodPlayer.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_IQvodPlayer_h
#define QVOD_IQvodPlayer_h

#include "Utinities.h"

typedef int (*PCallback)(void* pUserData, void* pReserved);

struct IQvodPlayer
{
    virtual int Open(const char* pszURL, double lfOffset, BOOL bRemote) = 0;
    virtual int Close() = 0;
    virtual int Play() = 0;
    virtual int Seek(double lfOffset) = 0;
    virtual int Pause() = 0;
    virtual int CaptureFrame() = 0;
    virtual int StartPreview(const char* pszURL, double lfOffset, int nFrameCount) = 0;
    virtual int StopPreview() = 0;
    virtual int SetParameter(int nParam, void* pValue) = 0;
    virtual int GetParameter(int nParam, void* pValue) = 0;
    virtual int SetCallback(int nType, PCallback pfnCallback, void* pUserData, void* pReserved) = 0;
    virtual int ReceiveRequest(int nType, int nParam1, int nParam2, void* pUserData, void* pReserved) = 0;
};

#endif
