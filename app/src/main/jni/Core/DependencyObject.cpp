//
//  DependencyObject.cpp
//  QvodPlayer
//
//  Created by bigbug on 12-2-25.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include <iostream>
#include "DependencyObject.h"


CDependencyObject::CDependencyObject()
{
    m_pDepend = NULL;
}

CDependencyObject::CDependencyObject(IDependency* pDepend)
{
    m_pDepend = pDepend;
}

void CDependencyObject::SetDependency(IDependency* pDepend)
{
    m_pDepend = pDepend;
}

int CDependencyObject::NotifyEvent(int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData)
{
    if (!m_pDepend) {
        return E_FAIL;
    }
    
    if (InterceptEvent(nEvent, dwParam1, dwParam2, pUserData) == E_HANDLED) {
        return E_HANDLED;
    }
    
    return m_pDepend->ReceiveEvent(this, nEvent, dwParam1, dwParam2, pUserData);
}

int CDependencyObject::InterceptEvent(int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData)
{
    return S_OK;
}