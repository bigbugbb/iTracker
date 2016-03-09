//
//  PlayerManager.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_GraphManager_h
#define QVOD_GraphManager_h

#include "Message.h"
#include "MediaGraph.h"
#include "RefClock.h"
#include "DependencyObject.h"

class CMediaObject;

class CGraphManager : public CThread,
                      public CDependencyObject
{
public:
    CGraphManager(IDependency* pDepend, int* pResult);
    virtual ~CGraphManager();
    
    int GetState();
    int ShrinkMessage(BOOL bShrinkAll = FALSE);
    int SendMessage(const Message& msg);
    
    virtual IReferenceClock* GetReferenceClock();

protected:
    // BuildGraph & DestroyGraph should be overrided
    int BuildGraph();
    int DestroyGraph();
    
    int RecvMessage(Message& msg);
    virtual THREAD_RETURN ThreadProc();
    
    virtual int Open(Argument& arg);
    virtual int Close(Argument& arg);
    virtual int Play(Argument& arg);
    virtual int Pause(Argument& arg);
    virtual int Seek(Argument& arg);
    virtual int WaitForResources(Argument& arg);

    virtual void OnInit(Argument& arg) = 0;
    virtual void OnLoaded(Argument& arg) = 0;
    virtual void OnOpened(Argument& arg) = 0;
    virtual void OnClose(Argument& arg) = 0;
    virtual void OnClosed(Argument& arg) = 0;
    virtual void OnPlay(Argument& arg) = 0;
    virtual void OnPlayed(Argument& arg) = 0;
    virtual void OnPause(Argument& arg) = 0;
    virtual void OnPaused(Argument& arg) = 0;
    virtual void OnFlush(Argument& arg) = 0;
    virtual void OnInvalid(Argument& arg) = 0;

    CMessageQueue     m_MsgQueue;
    
    CMediaGraph       m_Graph;
    CMediaObject*     m_pSource;
    
    IReferenceClock*        m_pRefClock;
    IReferenceClockControl* m_pRefClockCtrl;
};

#endif
