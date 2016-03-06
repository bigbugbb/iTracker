//
//  PlayerManager.cpp
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "MediaObject.h"
#include "GraphManager.h"
#include "Components/CompInterfaces.h"


CGraphManager::CGraphManager(IDependency* pDepend, int* pResult)
    : CDependencyObject(pDepend), m_pSource(NULL)
{
    int nResult;
    
    nResult = BuildGraph();
    if (nResult != S_OK) {
        if (pResult) {
            *pResult = nResult;
        }
        return;
    }
    
    m_pRefClock = GetReferenceClock();
    m_pRefClockCtrl = dynamic_cast<IReferenceClockControl*>(m_pRefClock);
    
    nResult = CThread::Create();
    if (nResult != S_OK) {
        if (pResult) {
            *pResult = nResult;
        }
        DestroyGraph();
        return;
    }
    
    m_pRefClockCtrl->Reset();
}

CGraphManager::~CGraphManager()
{
    CThread::Close();
    
    DestroyGraph();
}

int CGraphManager::BuildGraph()
{
    return S_OK;
}

int CGraphManager::DestroyGraph()
{
    return S_OK;
}

int CGraphManager::ShrinkMessage(BOOL bShrinkAll)
{
    m_MsgQueue.Shrink(bShrinkAll);
    
    return S_OK;
}

int CGraphManager::SendMessage(const Message& msg)
{
    m_MsgQueue.AddMessage(msg);
    
    Signal();
    
    return S_OK;
}

int CGraphManager::RecvMessage(Message& msg)
{
    if (!m_MsgQueue.Size()) {
        Wait();
    }
    m_MsgQueue.GetMessage(msg);
    
    return S_OK;
}

int CGraphManager::GetState()
{
    CMediaObject* pSink = m_Graph.GetSink();
    AssertValid(pSink);
    return pSink->GetState();
}

IReferenceClock* CGraphManager::GetReferenceClock()
{
    m_pRefClock = CSystemRefClock::GetInstance();
    
    return m_pRefClock;
}

THREAD_RETURN CGraphManager::ThreadProc()
{
    int nResult;
    Argument arg;

    while (m_bRun) {
        RecvMessage(arg.msg);
        
        switch (arg.msg.nID) {
        case MSG_OPEN:
            nResult = Open(arg);
            break;
        case MSG_CLOSE:
            nResult = Close(arg);
            break;
        case MSG_PLAY:
            nResult = Play(arg);
            break;
        case MSG_PAUSE:
            nResult = Pause(arg);
            break;
        case MSG_SEEK:
            nResult = Seek(arg);
            break;
        case MSG_WAITFORRES:
            nResult = WaitForResources(arg);
            break;
        case MSG_NONE:
            nResult = S_OK;
            break;
        default:
            nResult = S_OK;
            break;
        }
    }
    
    return 0;
}

int CGraphManager::Open(Argument& arg)
{
    Log("Open Graph\n");
    int nResult = S_OK;
    
    AssertValid(m_pSource);
    if (!m_pSource) {
        Log("Open Graph failed\n");
        return E_FAIL;
    }
    
    if (m_pSource->GetState() & STATE_LOADED) {
        Log("Graph is already opened\n");
        return S_OK;
    }
    
    OnInit(arg);
    m_Graph.Traversal(m_pSource, COMMAND_LOAD, NULL, nResult);

    if (nResult == S_OK) {
        OnLoaded(arg);
        BOOL bWait = TRUE;
        m_Graph.Traversal(m_pSource, COMMAND_WAITFORRESOURCES, &bWait, nResult);
        bWait = FALSE;
        m_Graph.Traversal(m_pSource, COMMAND_WAITFORRESOURCES, &bWait, nResult);
    }
    
    if (nResult == S_OK) {
        m_Graph.Traversal(m_pSource, COMMAND_IDLE, NULL, nResult);
        Log("Idle Graph\n");
    }
    
    if (nResult != S_OK) {
        OnInvalid(arg);
        m_Graph.Traversal(m_pSource, COMMAND_INVALID, NULL, nResult);
        Log("Invalid Graph\n");
        OnClose(arg);
        m_Graph.Traversal(m_pSource, COMMAND_UNLOAD, NULL, nResult);
        Log("Unload Graph\n");
    } else {
        OnOpened(arg);
    }
    
    Log("Open Graph End\n");
    return nResult;
}

int CGraphManager::Close(Argument& arg)
{
    Log("Close Graph\n");
    int nResult = S_OK;
    
    AssertValid(m_pSource);
    if (!m_pSource) {
        nResult = E_FAIL;
        Log("Close Graph failed 1\n");
        goto end;
    }
    
    if (m_pSource->GetState() & STATE_UNLOADED) {
        nResult = S_OK;
        Log("Close Graph failed 2\n");
        goto end;
    }

    if (nResult != S_OK) {
        OnInvalid(arg);
        m_Graph.Traversal(m_pSource, COMMAND_INVALID, NULL, nResult);
    } 
    
    OnClose(arg);
    m_Graph.Traversal(m_pSource, COMMAND_UNLOAD, NULL, nResult);
    
    m_pRefClockCtrl->Reset();
    
    AssertValid(nResult == S_OK);
end:
    OnClosed(arg);

    Log("Close Graph End\n");
    return nResult;
}

int CGraphManager::Play(Argument& arg)
{
    Log("Play Graph\n");
    int nResult = S_OK;
    
    AssertValid(m_pSource);
    if (!m_pSource) {
        nResult = E_FAIL;
        Log("Play Graph failed 1\n");
        goto end;
    }
    
    if (!(m_pSource->GetState() & STATE_LOADED)) {
        nResult = E_FAIL;
        Log("Play Graph failed 2\n");
        goto end;
    }
    
    if (m_pSource->GetState() & STATE_EXECUTE) {
        nResult = S_OK;
        Log("Play Graph failed 3\n");
        goto end;
    }
    
    m_pRefClockCtrl->Play();

    OnPlay(arg);
    m_Graph.Traversal(m_pSource, COMMAND_EXECUTE, NULL, nResult);
    
    if (nResult != S_OK) {
        OnInvalid(arg);
        m_Graph.Traversal(m_pSource, COMMAND_INVALID, NULL, nResult);
        OnClose(arg);
        m_Graph.Traversal(m_pSource, COMMAND_UNLOAD, NULL, nResult);
    }
end:
    OnPlayed(arg);
    
    Log("Play Graph End\n");
    return nResult;
}

int CGraphManager::Pause(Argument& arg)
{
    Log("Pause Graph\n");
    int nResult = S_OK;
    
    AssertValid(m_pSource);
    if (!m_pSource) {
        nResult = E_FAIL;
        Log("Pause Graph failed 1\n");
        goto end;
    }
    
    if (!(m_pSource->GetState() & STATE_LOADED)) {
        nResult = E_FAIL;
        Log("Pause Graph failed 2\n");
        goto end;
    }
    
    if (m_pSource->GetState() & STATE_PAUSE) {
        nResult = S_OK;
        Log("Pause Graph failed 3\n");
        goto end;
    }
    
    OnPause(arg);
    m_Graph.Traversal(m_pSource, COMMAND_PAUSE, NULL, nResult);
    
    if (nResult != S_OK) {
        OnInvalid(arg);
        m_Graph.Traversal(m_pSource, COMMAND_INVALID, NULL, nResult);
        OnClose(arg);
        m_Graph.Traversal(m_pSource, COMMAND_UNLOAD, NULL, nResult);
    }
    
    m_pRefClockCtrl->Pause();
end:
    OnPaused(arg);
    
    Log("Pause Graph End\n");
    return nResult;
}

int CGraphManager::Seek(Argument& arg)
{
    int nResult = S_OK;
    
    AssertValid(m_pSource);
    if (!m_pSource) {
        return E_FAIL;
    }
    
    m_Graph.Traversal(m_pSource, COMMAND_BEGINFLUSH, NULL, nResult);
    Log("After Begin Flush\n");
    OnFlush(arg);
    m_pRefClockCtrl->Seek();
    Log("Before End Flush\n");
    m_Graph.Traversal(m_pSource, COMMAND_ENDFLUSH, NULL, nResult);
    
    if (nResult != S_OK) {
        OnInvalid(arg);
        m_Graph.Traversal(m_pSource, COMMAND_INVALID, NULL, nResult);
        OnClose(arg);
        m_Graph.Traversal(m_pSource, COMMAND_UNLOAD, NULL, nResult);
    }
    
    AssertValid(nResult == S_OK);
    
    return nResult;
}

int CGraphManager::WaitForResources(Argument& arg)
{
    int nResult = S_OK;
    
    AssertValid(m_pSource);
    if (!(m_pSource->GetState() & STATE_LOADED)) {
        return E_FAIL;
    }
    
    BOOL bWait   = (BOOL)arg.msg.dwParam1;
    BOOL bCancel = (BOOL)arg.msg.dwParam2;
    if (bWait) { // begin waiting
        m_pRefClockCtrl->BeginWaiting();
    } else { // end waiting
        bCancel ? m_pRefClockCtrl->CancelWaiting() : m_pRefClockCtrl->EndWaiting();
    }
    m_Graph.Traversal(m_pSource, COMMAND_WAITFORRESOURCES, &bWait, nResult);
    
    return nResult;
}




