//
//  CallbackManager.h
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_CallbackManager_h
#define QVOD_CallbackManager_h

#include "DependencyObject.h"
#include <map>

typedef int (*PCallback)(void* pUserData, void* pReserved);

struct CallbackData 
{
    CallbackData()
    {
        nCallbackType = 0;
        pfnCallback   = 0;
        pUserData     = 0;
        pReserved     = 0;
    }
    
    int         nCallbackType;
    PCallback   pfnCallback;
    void*       pUserData;
    void*       pReserved;
};

class CCallbackManager : public CDependencyObject
{
    CCallbackManager();
    virtual ~CCallbackManager();
    
public:
    static CCallbackManager* GetInstance();
    
    int SetCallback(int nType, PCallback pfnCallback, void* pUserData, void* pReserved);
    CallbackData& GetCallbackData(int nType);
    
protected:
    std::map<int, CallbackData> m_mapCallbacks;
};

#endif
