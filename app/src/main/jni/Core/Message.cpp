//
//  Message.cpp
//  QVOD
//
//  Created by bigbug on 11-11-11.
//  Copyright (c) 2011年 __MyCompanyName__. All rights reserved.
//


#include <cassert>
#include <iostream>
#include "Message.h"
#include "SysConsts.h"
using namespace std;

CMessageQueue::CMessageQueue()
{
    m_listMsgs.clear();
}

CMessageQueue::~CMessageQueue()
{
    
}

int CMessageQueue::Size()
{
    CAutoLock cObjectLock(this);
    
    return m_listMsgs.size();
}

int CMessageQueue::AddMessage(const Message& msg)
{
    CAutoLock cObjectLock(this);
    
    m_listMsgs.push_back(msg);
    
    return S_OK;
}

int CMessageQueue::GetMessage(Message& msg)
{
    CAutoLock cObjectLock(this);
    
    if (m_listMsgs.empty()) {
        memset(&msg, 0, sizeof(Message));
        msg.eID = MSG_NONE;
    } else {
        msg = m_listMsgs.front();
        m_listMsgs.pop_front();
    }
    
    return S_OK;
}

int CMessageQueue::Clear()
{
    CAutoLock cObjectLock(this);
    
    m_listMsgs.clear();
    
    return S_OK;
}

int CMessageQueue::Shrink(BOOL bShrinkAll)
{
    CAutoLock cObjectLock(this);
    
    for (list<Message>::iterator iter = m_listMsgs.begin(); iter != m_listMsgs.end();) 
    {
        Message& msg = *iter;
        if (msg.bIgnore || bShrinkAll) {
            iter = m_listMsgs.erase(iter);
        } else {
            ++iter;
        }
    }
    
    return S_OK;
}

