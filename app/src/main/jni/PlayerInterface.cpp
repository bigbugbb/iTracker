//
//  PlayerInterface.cpp
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "Core/QvodPlayer.h"
#include "PlayerInterface.h"


#ifdef LOG_AUDIO_PCM
std::string strPathPCM;
#endif

#ifdef LOG_VIDEO_RGB
std::string strPathRGB;
#endif

#ifdef LOG_TO_FILE
std::string strPathLog;
#endif

static IQvodPlayer* g_pPlayer = NULL;

int CreatePlayer(const char* szPath)
{
#ifdef LOG_AUDIO_PCM
    strPathPCM = szPath;
    strPathPCM += "/real";
#endif
    
#ifdef LOG_VIDEO_RGB
    strPathRGB = szPath;
    strPathRGB += "/rgb";
#endif 

#ifdef LOG_TO_FILE
    strPathLog = szPath;
    strPathLog += "/player_log.txt";
#endif    
    
    g_pPlayer = CQvodPlayer::GetInstance();
    if (!g_pPlayer) {
        return E_FAIL;
    }

    return S_OK;
}

int DestroyPlayer()
{
    // singleon model, do nothing here
    return S_OK;
}

int Open(const char* pszURL, double lfOffset, int nRemote)
{
    if (!g_pPlayer) {
        return E_FAIL;
    }

    int nResult = g_pPlayer->Open(pszURL, lfOffset, nRemote);

    return nResult;
}

int Close()
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->Close();
    
    return nResult;
}

int Play()
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->Play();
    
    return nResult;
}

int Pause()
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->Pause();
    
    return nResult;
}

int Seek(double lfTime)
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->Seek(lfTime);
    
    return nResult;
}

int StartPreview(const char* pszURL, double lfOffset, int nFrameCount)
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->StartPreview(pszURL, lfOffset, 1);
    
    return nResult;
}

int StopPreview()
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->StopPreview();
    
    return nResult;
}

int CaptureFrame()
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->CaptureFrame();
    
    return nResult;
}

int SetParameter(int nParam, void* pValue)
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->SetParameter(nParam, pValue);
    
    return nResult;
}

int GetParameter(int nParam, void* pValue)
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->GetParameter(nParam, pValue);
    
    return nResult;
}

int SetCallback(int nCallbackType, PCallback pfnCallback, void* pUserData, void* pReserved)
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->SetCallback(nCallbackType, pfnCallback, pUserData, pReserved);
    
    return nResult;
}

int SendRequest(int nType, int nParam1, int nParam2, void* pUserData, void* pReserved)
{
    if (!g_pPlayer) {
        return E_FAIL;
    }
    
    int nResult = g_pPlayer->ReceiveRequest(nType, nParam1, nParam2, pUserData, pReserved);
    
    return nResult;
}

