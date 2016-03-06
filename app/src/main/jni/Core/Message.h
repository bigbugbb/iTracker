//
//  Message.h
//  QVOD
//
//  Created by bigbug on 11-11-11.
//  Copyright (c) 2011年 __MyCompanyName__. All rights reserved.
//

#ifndef QVOD_Message_h
#define QVOD_Message_h

#include "Utinities.h"

#include <list>
using namespace ios_qvod_player;

typedef enum 
{
    MSG_OPEN = 0,
    MSG_CLOSE,
    MSG_PLAY,
    MSG_PAUSE,
    MSG_SEEK,
    MSG_WAITFORRES,
    MSG_OTHER,
    MSG_NONE
} MSGID;

//typedef int PNotifyEvent(UINT nEventID, LPVOID pUserData, UINT* pParam1, UINT* pParam2);

struct Message
{
    Message()
    {
        nID       = MSG_NONE;
        bIgnore   = FALSE;
        dwParam1  = 0;
        dwParam2  = 0;
        pUserData = NULL;
    }
    
    Message(MSGID nID, BOOL bIgnore = FALSE, DWORD dwParam1 = 0, DWORD dwParam2 = 0, LPVOID pUserData = NULL)
    {
        this->nID       = nID;
        this->bIgnore   = bIgnore;
        this->dwParam1  = dwParam1;
        this->dwParam2  = dwParam2;
        this->pUserData = pUserData;
    }
    
    MSGID   nID;
    BOOL    bIgnore;
    DWORD   dwParam1;
    DWORD   dwParam2;
    LPVOID  pUserData;
};

struct Argument
{
    Argument()
    {
        dwParam1  = 0;
        dwParam2  = 0;
        pUserData = NULL;
        pReserved = NULL;
    }
    
    Argument(Message msg, DWORD dwParam1 = 0, DWORD dwParam2 = 0, LPVOID pUserData = NULL, LPVOID pReserved = NULL)
    {
        this->msg = msg;
        this->dwParam1  = dwParam1;
        this->dwParam2  = dwParam2;
        this->pUserData = pUserData;
        this->pReserved = pReserved;
    }
    
    Message  msg;
    DWORD    dwParam1;
    DWORD    dwParam2;
    LPVOID   pUserData;
    LPVOID   pReserved;
};

class CMessageQueue : public CLock
{
public:
    CMessageQueue();
    virtual ~CMessageQueue();
    
    int Size();
    
    int AddMessage(const Message& msg);
    int GetMessage(Message& msg);
    
    int Clear();
    int Shrink(BOOL bShrinkAll = FALSE); // remove messages that can be ignored in some cases
protected:
    std::list<Message> m_listMsgs;
};

#endif
