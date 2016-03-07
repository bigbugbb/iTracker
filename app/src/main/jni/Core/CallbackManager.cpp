//
//  CallbackManager.cpp
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "SysConsts.h"
#include "CallbackManager.h"
using namespace::std;


CCallbackManager::CCallbackManager()
{
    
}

CCallbackManager::~CCallbackManager()
{
    
}

CCallbackManager* CCallbackManager::GetInstance()
{
    static CCallbackManager s_CallbackManager;
    
    return &s_CallbackManager;
}

int CCallbackManager::SetCallback(int nType, PCallback pfnCallback, void* pUserData, void* pReserved)
{
    CallbackData cbd;
    
    cbd.nType       = nType;
    cbd.pfnCallback = pfnCallback;
    cbd.pUserData   = pUserData;
    cbd.pReserved   = pReserved;
    AssertValid(pfnCallback);
    
    m_mapCallbacks[nType] = cbd;
    
    return S_OK;
}

CallbackData& CCallbackManager::GetCallbackData(int nType)
{
    return m_mapCallbacks[nType];
}
